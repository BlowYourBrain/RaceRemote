"""Build removable carrier retention and check nominal assembly/service geometry."""
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import numpy as np
import trimesh
from build_packaging_v05 import mesh,intersect
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/motor-carrier-mount'
PREV=ROOT/'cad/motor-carrier'
SCAD=ROOT/'cad/motor-carrier-mount.scad'


def main():
    DEST.mkdir(exist_ok=True)
    inputs=[SCAD,Path(__file__),ROOT/'cad/harness-guides.scad',ROOT/'cad/xiao-mount.scad',ROOT/'cad/packaging-v05/tray.stl',ROOT/'cad/packaging-v05/pads.stl',ROOT/'cad/adjustable-layout.scad',PREV/'viewer.html',ROOT/'docs/evidence/motor-carrier-fit-v01.json',ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py',ROOT/'tools/inspect_zcar.py']
    parts={}
    for name in ['tray','clamp','screws','nuts','pads','tray_print','clamp_print','pcb_gauge_print']:
        subprocess.run(['C:/Program Files/OpenSCAD/openscad.com','-o',str(DEST/(name+'.stl')),'-D',f'part="{name}"',str(SCAD)],check=True,capture_output=True,text=True)
        parts[name]=mesh(DEST/(name+'.stl'));assert parts[name].is_volume,name
    assert len(parts['tray'].split())==1,'Tray and holder must be one connected printable body'
    assert len(parts['clamp'].split())==1
    for name in ['tray_print','clamp_print','pcb_gauge_print']:
        assert abs(parts[name].bounds[0,2])<.001,(name,parts[name].bounds)
    previous=json.loads((ROOT/'docs/evidence/motor-carrier-fit-v01.json').read_text())
    obstacles={}
    # Reuse the exact input inventory, not the previous result, to check new geometry.
    for path in previous['sources_sha256']:
        q=ROOT/path
        if q.suffix=='.stl' and q.name not in ['tray.stl','pads.stl','adjustable-cavity.stl','wire-antenna.stl']:
            inputs.append(q);obstacles[path]=mesh(q);assert obstacles[path].is_volume,path
    inputs.append(ROOT/'docs/evidence/xiao-mount-v01.json')
    bounds=json.loads((ROOT/'docs/evidence/xiao-mount-v01.json').read_text())['source_bounding_boxes_mm']
    obstacles['xiao']=trimesh.boolean.union([box(np.diff(v,axis=0)[0],np.mean(v,axis=0))for v in bounds.values()],engine='manifold')
    inputs.append(PREV/'wire-antenna.stl');obstacles['antenna-wire']=mesh(PREV/'wire-antenna.stl')
    obstacles['pads']=parts['pads']
    carrier={}
    for p in PREV.glob('*.stl'):
        if p.name in ['carrier.stl','wire-antenna.stl']:continue
        inputs.append(p);carrier[p.stem]=mesh(p)
    cavity_path=ROOT/'cad/packaging-v05/inputs/adjustable-cavity.stl';inputs.append(cavity_path);cavity=mesh(cavity_path)
    assembly={n:parts[n]for n in ['tray','clamp','screws','nuts']}
    hits={f'{n}/{o}':intersect(m,om)for n,m in assembly.items()for o,om in (obstacles|carrier).items()}
    # Hardware internal tangencies are permitted; geometric threads are omitted.
    internal={f'{n}/{o}':intersect(assembly[n],assembly[o])for i,n in enumerate(assembly)for o in list(assembly)[i+1:]}
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume)for n,m in assembly.items()}
    # Body is removed, power off and carrier wires freed for this service sequence.
    # Remove clamp/screws; tilt6deg about the right underside, slide left1.4mm,
    # lift8mm below the antenna cable, move rearward8mm, then lift clear.
    fixed=obstacles|{'tray':parts['tray'],'nuts':parts['nuts']}
    poses=[(float(a),[0,0,0])for a in np.arange(0,6.01,.25)]
    poses += [(6,[-float(x),0,0])for x in np.arange(.1,1.401,.1)]
    poses += [(6,[-1.4,0,float(z)])for z in np.arange(.25,8.01,.25)]
    poses += [(6,[-1.4,-float(y),8])for y in np.arange(.25,8.01,.25)]
    poses += [(6,[-1.4,-8,float(z)])for z in np.arange(8.25,25.01,.25)]
    def moved_hits(delta,angle=0):
        h={}
        for n,m in carrier.items():
            moved=m.copy();moved.apply_transform(trimesh.transformations.rotation_matrix(np.radians(angle),[0,1,0],[43.75,0,29.5]));moved.apply_translation(delta)
            for o,om in fixed.items():
                v=intersect(moved,om)
                assert np.isfinite(v),(n,o,delta,angle)
                if v>.001:h[n+'/'+o]=round(v,5)
        return h
    service=[{'angle_deg':a,'translation_mm':d,'intersections_mm3':moved_hits(d,a)}for a,d in poses]
    vertical_control=moved_hits([0,0,1])
    clamp_service=[]
    for dz in np.arange(0,20.01,.25):
        moved=parts['clamp'].copy();moved.apply_translation([0,0,float(dz)])
        h={n:intersect(moved,m)for n,m in (fixed|carrier).items()}
        assert all(np.isfinite(v)for v in h.values())
        clamp_service.append({'lift_mm':float(dz),'intersections_mm3':{n:round(v,5)for n,v in h.items()if v>.001}})
    gauge=parts['pcb_gauge_print'].copy();gauge.apply_translation([25.75,47,29.5])
    gauge_hits={n:intersect(gauge,m)for n,m in assembly.items()}
    nut_insertion=[]
    for dx in np.arange(-6,.001,.25):
        moved=parts['nuts'].copy();moved.apply_translation([float(dx),0,0])
        v=intersect(moved,parts['tray']);assert np.isfinite(v)
        nut_insertion.append({'x_shift_mm':float(dx),'tray_intersection_mm3':round(v,5)})
    tool=trimesh.util.concatenate([trimesh.creation.cylinder(radius=1.1,height=20,transform=trimesh.transformations.translation_matrix([23,y,44.7]))for y in [57,63]])
    tool_hits={n:intersect(tool,m)for n,m in (obstacles|carrier).items()}
    report={'version':'0.1','date':'2026-09-15','pose_mm':[0,3],
        'assembly_intersections_mm3':{k:round(v,5)for k,v in hits.items()if v>.001},
        'internal_intersections_mm3':{k:round(v,5)for k,v in internal.items()if v>.001},
        'outside_body_mm3':{k:round(v,5)for k,v in outside.items()if v>.001},
        'service_poses':service,'straight_up_positive_control':vertical_control,
        'clamp_removal_poses':clamp_service,'gauge_intersections_mm3':{n:round(v,5)for n,v in gauge_hits.items()if v>.001},
        'nut_insertion_before_buck_and_pcb':nut_insertion,
        'hex_key_corridor_intersections_mm3':{k:round(v,5)for k,v in tool_hits.items()if v>.001},
        'printed_part_bounds_mm':{n:parts[n].bounds.tolist()for n in ['tray_print','clamp_print','pcb_gauge_print']},
        'physical_print':False,'retention_force_verified':False,
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}}
    (ROOT/'docs/evidence/motor-carrier-mount-v01.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:v for k,v in report.items()if k not in ['source_sha256','service_poses','clamp_removal_poses','nut_insertion_before_buck_and_pcb']},indent=2),flush=True)
    print('Service collisions:',[(r['translation_mm'],r['intersections_mm3'])for r in service if r['intersections_mm3']],flush=True)
    # A candidate that fails the mechanical checks must not silently produce a passed viewer.
    assert all(np.isfinite(v)for v in [*hits.values(),*internal.values(),*outside.values(),*tool_hits.values()])
    assert not report['assembly_intersections_mm3'] and not report['internal_intersections_mm3'] and not report['outside_body_mm3']
    assert not any(r['intersections_mm3']for r in service)
    assert not any(r['intersections_mm3']for r in clamp_service)
    assert all(np.isfinite(v)for v in gauge_hits.values()) and not report['gauge_intersections_mm3']
    assert not any(r['tray_intersection_mm3']>.001 for r in nut_insertion)
    assert vertical_control,'Positive control must catch fixed-lip retention'
    assert not report['hex_key_corridor_intersections_mm3']
    source=PREV/'viewer.html';text=source.read_text(encoding='utf-8')
    rows=json.loads(text.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    rows=[r for r in rows if r['id']not in ['tray','pads']]
    for n in ['tray','clamp','screws','nuts','pads']:
        m=parts[n];raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='mount-'+n,label='Крепление привода: '+n,color=[1,.55,.15]if n=='clamp' else [.7,.7,.73]if n in ['screws','nuts']else [.35,.65,.6],slide=None,level=0,wire=False,hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    begin=text.index('const parts=');end=text.index(';\nconst adjustmentChecks=',begin)
    text=text[:begin]+'const parts='+json.dumps(rows,ensure_ascii=False)+text[end:]
    text=text.replace('Плата привода — примерка','Крепление платы привода')
    text=text.replace('Кузов условный; крепление и реальная посадка ещё открыты.','Слева съёмный прижим, справа неподвижный паз. Проверены начальное положение и снятие отключённой платы при снятом кузове и освобождённых проводах; физическая посадка ещё открыта.')
    text=text.replace('href="carrier.step"','href="../motor-carrier/carrier.step"')
    (DEST/'viewer.html').write_text(text,encoding='utf-8',newline='\n')
    report['artifact_sha256']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in [*sorted(DEST.glob('*.stl')),DEST/'viewer.html']}
    (ROOT/'docs/evidence/motor-carrier-mount-v01.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')


if __name__=='__main__':main()
