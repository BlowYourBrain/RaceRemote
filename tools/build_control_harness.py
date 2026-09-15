"""Route three proposed insulated control leads from referenced copper pads."""
import base64
import csv
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,intersect,tube
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/control-harness'
MOUNT=ROOT/'cad/motor-carrier-mount'


def main():
    reference=DEST/'xiao-pad-reference.json';pinref=json.loads(reference.read_text())
    pads={k:v['candidate_xyz_mm'] for k,v in pinref['pads'].items()}
    boardpath=ROOT/'hardware/motor-carrier/geometry.json';board=json.loads(boardpath.read_text())
    end={p['ref']:[25.75+p['xy_mm'][1],47+p['xy_mm'][0],31.1]for p in board['pads']if p['ref'].startswith('H')}
    routes={
      'FI':{'xiao':'D0','gpio':1,'terminal':'H1','net':'CMD_FI','color':[.9,.5,.05],
        'points_mm':[pads['D0'],[30.96583,87.1,23.17],[30.96583,87.1,37.3],[37.5,87.1,37.3],
                     [37.5,64,37.3],[28.75,64,37.3],[28.75,48.8,37.3],end['H1']]},
      'BI':{'xiao':'D1','gpio':2,'terminal':'H2','net':'CMD_BI','color':[.3,.3,.85],
        'points_mm':[pads['D1'],[28.42583,88.2,23.27],[28.42583,88.2,37.3],[38.6,88.2,37.3],
                     [38.6,63,37.3],[31.75,63,37.3],[31.75,48.8,37.3],end['H2']]},
      'GND':{'xiao':'GND','gpio':None,'terminal':'H3','net':'DRIVE_GND','color':[.18,.18,.2],
        # Shift solder target .25mm inward within the1.4mm ground pad, below clamp finger.
        'points_mm':[[pads['GND'][0],83.25,38.26],[28.42583,85,38.26],[28.42583,85.8,38.7],[38,89.5,38.7],
                     [39.7,89.5,38.7],[39.7,79,37.3],[39.7,62,37.3],
                     [34.75,62,37.3],[34.75,49,37.3],end['H3']]}}
    for r in routes.values():
        r['radius_mm']=.4
        r['centerline_length_mm']=round(float(np.linalg.norm(np.diff(r['points_mm'],axis=0),axis=1).sum()),3)
        assert next(p for p in board['pads']if p['ref']==r['terminal'])['net']==r['net']
    wires={n:tube(r['points_mm'],r['radius_mm'])for n,r in routes.items()}
    for n,m in wires.items():assert m.is_volume,n
    sources=[Path(__file__),reference,boardpath,ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py']
    previous_path=ROOT/'docs/evidence/motor-carrier-mount-v01.json';sources.append(previous_path)
    previous=json.loads(previous_path.read_text())
    obstacles={}
    def add(path):
        sources.append(path);obstacles[path.relative_to(ROOT).as_posix()]=mesh(path)
        assert obstacles[path.relative_to(ROOT).as_posix()].is_volume,path
    for p in previous['source_sha256']:
        path=ROOT/p
        if path.suffix=='.stl' and path.name not in ['tray.stl','pads.stl','adjustable-cavity.stl']:
            add(path)
    for n in ['tray','clamp','nuts','screws','pads']:add(MOUNT/(n+'.stl'))
    xiao_path=ROOT/'docs/evidence/xiao-mount-v01.json';sources.append(xiao_path)
    boxes=json.loads(xiao_path.read_text())['source_bounding_boxes_mm']
    contact_masks=[box([1.6,1.0,1.6],r['points_mm'][0])for r in routes.values()]
    for n,b in boxes.items():
        m=box(np.diff(b,axis=0)[0],np.mean(b,axis=0))
        if n=='37':
            # Only local contact windows on the unperforated reference PCB box are omitted.
            m=trimesh.boolean.difference([m,*contact_masks],engine='manifold')
        obstacles['xiao-solid-'+n]=m
    cavity_path=ROOT/'cad/packaging-v05/inputs/adjustable-cavity.stl';sources.append(cavity_path);cavity=mesh(cavity_path)
    collisions={n:{o:intersect(m,om)for o,om in obstacles.items()}for n,m in wires.items()}
    pairs={n+'/'+o:intersect(wires[n],wires[o])for i,n in enumerate(wires)for o in list(wires)[i+1:]}
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume)for n,m in wires.items()}
    assert all(np.isfinite(v)for group in collisions.values()for v in group.values())
    assert all(np.isfinite(v)for v in [*pairs.values(),*outside.values()])
    # Positive control: a straight shortcut from D0 toH1 crosses solid structure.
    shortcut=tube([routes['FI']['points_mm'][0],end['H1']],.4)
    shortcut_hits={n:intersect(shortcut,m)for n,m in obstacles.items()}
    report={'version':'0.1','date':'2026-09-15','pose_mm':[0,3],'routes':routes,
      'obstacle_hits_mm3':{n:{o:round(v,5)for o,v in group.items()if v>.001}for n,group in collisions.items()},
      'wire_pair_hits_mm3':{n:round(v,5)for n,v in pairs.items()if v>.001},
      'outside_body_mm3':{n:round(v,5)for n,v in outside.items()if v>.001},
      'straight_shortcut_positive_control':{n:round(v,5)for n,v in shortcut_hits.items()if v>.001},
      'contact_exception':'Only XIAO PCB solid37 within1.6x1x1.6mm boxes at the three proposed solder points',
      'physical_wiring':False,'power_domain_integration_verified':False,
      'strain_relief_designed':False,'connector_selected':False,'service_motion_with_wires_verified':False,
      'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in sources}}
    out=ROOT/'docs/evidence/control-harness-v01.json'
    out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:report[k]for k in ['obstacle_hits_mm3','wire_pair_hits_mm3','outside_body_mm3','straight_shortcut_positive_control']},indent=2),flush=True)
    assert not any(report['obstacle_hits_mm3'].values())and not report['wire_pair_hits_mm3']and not report['outside_body_mm3']
    assert report['straight_shortcut_positive_control']
    for n,m in wires.items():m.export(DEST/(n+'.stl'))
    source=MOUNT/'viewer.html';text=source.read_text(encoding='utf-8')
    rows=json.loads(text.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    rows=[r for r in rows if r['id']!='wire-control']
    for n,m in wires.items():
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='control-'+n,label=n+' — XIAO '+routes[n]['xiao']+' → '+routes[n]['terminal'],
          color=routes[n]['color'],slide=None,level=0,wire=False,hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    start=text.index('const parts=');endpos=text.index(';\nconst adjustmentChecks=',start)
    text=text[:start]+'const parts='+json.dumps(rows,ensure_ascii=False)+text[endpos:]
    text=text.replace('Крепление платы привода','Проводка управления')
    a=text.index('<p class="note">');b=text.index('</p>',a)
    text=text[:a]+'<p class="note">Три отдельных провода управления Ø0,8 мм: D0 → H1, D1 → H2, GND → H3. Координаты XIAO выведены из PCB1.5 и перенесены в STEP2023; ревизия полученной платы ещё неизвестна. Проверена только начальная геометрия 0/+3. Фиксация жгута, разъём и обслуживание с проводами ещё открыты; питание привода не подключено.'+text[b:]
    (DEST/'viewer.html').write_text(text,encoding='utf-8',newline='\n')
    with (DEST/'wiring.csv').open('w',encoding='utf-8-sig',newline='')as f:
        writer=csv.writer(f);writer.writerow(['signal','xiao_pad','gpio','carrier_pad','net','path_mm','wire_outer_diameter_mm','status'])
        for n,r in routes.items():writer.writerow([n,r['xiao'],r['gpio'],r['terminal'],r['net'],r['centerline_length_mm'],.8,'Proposal; not a cut length or verified wire SKU'])
    report['source_sha256'][source.relative_to(ROOT).as_posix()]=hashlib.sha256(source.read_bytes()).hexdigest()
    report['artifact_sha256']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in [*sorted(DEST.glob('*.stl')),DEST/'viewer.html',DEST/'wiring.csv']}
    out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('PASS: three proposed control paths; lengths', {n:r['centerline_length_mm']for n,r in routes.items()})


if __name__=='__main__':main()
