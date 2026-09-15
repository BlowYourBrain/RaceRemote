"""Two motor leads and explicit estimated terminal reserves, no physical qualification."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh, tube, intersect
from layout_zcar_battery import box
from build_xiao_power_harness import hits, service_check

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/motor-harness'


def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def record(path):
        sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest()
        return path
    def data(name):return json.loads(record(ROOT/name).read_text(encoding='utf-8'))
    def read(name):
        m=mesh(record(ROOT/name));assert m.is_volume,name
        return m
    previous=data('docs/evidence/direct-logic-harness-v01.json')
    obstacles={p:read(p) for p in previous['source_sha256'] if p.endswith('.stl') and p not in {
        'cad/motor-carrier-mount/tray.stl','cad/packaging-v05/inputs/adjustable-cavity.stl'}}
    for n in ['tray','5V','GND','tie-space','tie-head-space']:
        obstacles['direct-'+n]=read('cad/direct-logic-harness/'+n+'.stl')
    xiao=data('docs/evidence/xiao-mount-v01.json')
    for n,b in xiao['source_bounding_boxes_mm'].items():
        b=np.array(b);obstacles['xiao-'+n]=box(b[1]-b[0],b.mean(axis=0))
    old=data('docs/evidence/packaging-v05.json')['wire_corridors']|xiao['revised_corridor_definitions']
    for n in ['wire-power','wire-servo']:
        obstacles[n]=tube(old[n]['points_mm'],old[n]['radius_mm'])
    control=data('docs/evidence/control-harness-v01.json')['routes']
    for n in ['FI','BI']:obstacles['control-'+n]=tube(control[n]['points_mm'],control[n]['radius_mm'])
    geom=data('hardware/drive-power-carrier/geometry.json')
    targets={p['ref']:[25.75+p['xy_mm'][1],47+p['xy_mm'][0],31.1] for p in geom['pads'] if p['ref'] in ['H6','H7']}
    fit=data('docs/evidence/candidate-fit.json')
    cy,cz=fit['motor_shaft_axis_yz_mm']
    # Drawing gives rear extension4+/-0.4; lateral positions and lug sizes below
    # are explicit allocations from the drawing, NOT dimensioned terminal CAD.
    terminal_specs={
        'A':{'center_mm':[40.15,cy+8.4,cz+3.1],'extents_mm':[4.4,3,3]},
        'B':{'center_mm':[40.15,cy-8.4,cz-3.1],'extents_mm':[4.4,3,3]}}
    terminals={n:box(s['extents_mm'],s['center_mm']) for n,s in terminal_specs.items()}
    land={n:[s['center_mm'][0],s['center_mm'][1],s['center_mm'][2]+1.5] for n,s in terminal_specs.items()}
    routes={
        'FO':{'source':'H6','terminal':'A','radius_mm':.8,'points_mm':[
            targets['H6'],[37.75,49,36],[43.5,49,36],[43.5,32,36],[40.15,26,30.5],[40.15,24.5,28],[40.15,cy+8.4,25],land['A']]},
        'BO':{'source':'H7','terminal':'B','radius_mm':.8,'points_mm':[
            targets['H7'],[40.75,49,33],[40.75,51.5,33],[45.8,51.5,34.5],[46.5,40,35.8],[46.5,32,35.8],[44,26,30.5],[43,24,27],[43,10,25],[40.15,cy-8.4,23],land['B']]}}
    wires={n:tube(r['points_mm'],r['radius_mm']) for n,r in routes.items()}
    pcb=obstacles.pop('cad/drive-power-carrier/pcb.stl')
    obstacles['current-carrier-pcb']=trimesh.boolean.difference([pcb,*[box([1.9,1.9,1.8],p) for p in targets.values()]],engine='manifold')
    terminal_hits={n:hits(m,obstacles) for n,m in terminals.items()}
    collisions={}
    for n,m in wires.items():
        obs=obstacles.copy()
        for t,solid in terminals.items():
            obs['terminal-'+t]=trimesh.boolean.difference([solid,box([1.9,1.9,1.9],land[t])],engine='manifold') if t==routes[n]['terminal'] else solid
        collisions[n]=hits(m,obs)
    # Add upstream nearby mechanisms separately: open surfaces use conservative
    # bounding boxes and are never silently healed into supposed exact solids.
    reference=mesh(record(ROOT/'cad/reference/zcar-aligned.stl')).split(only_watertight=False,repair=False)
    region=box([20,51,33],[43,28,22])
    mechanical={};mechanical_modes={}
    for i,m in enumerate(reference):
        if i in {0,176,189,217,218}:continue
        if np.any(m.bounds[1]<=region.bounds[0]) or np.any(region.bounds[1]<=m.bounds[0]):continue
        if np.min(m.extents)<.001:continue
        mechanical['reference-'+str(i)]=m if m.is_volume else box(m.extents+.002,m.bounds.mean(axis=0))
        mechanical_modes[str(i)]='closed solid' if m.is_volume else 'conservative bounding box'
    alignments=data('cad/motor-harness/mechanism-alignment.json')
    for spec in alignments:
        src=mesh(record(ROOT/spec['source'])).split(only_watertight=False,repair=False)[spec['source_part']]
        src.apply_transform(spec['transform']);assert src.is_volume
        target=reference[spec['reference_part']]
        error=max(trimesh.proximity.closest_point_naive(target,src.vertices)[1].max(),trimesh.proximity.closest_point_naive(src,target.vertices)[1].max())
        assert error<.0001
        spec['verified_vertex_surface_max_mm']=float(error)
        name='reference-'+str(spec['reference_part'])
        mechanical[name]=src;mechanical_modes[str(spec['reference_part'])]='aligned closed upstream print solid'
        src.export(DEST/(name+'.stl'))
    mechanical_hits={n:hits(m,mechanical) for n,m in (wires|terminals).items()}
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume) for n,m in wires.items()}
    pair=intersect(wires['FO'],wires['BO'])
    old_motor=tube(old['wire-motor']['points_mm'],old['wire-motor']['radius_mm'])
    positive=intersect(old_motor,obstacles['direct-tray']);assert positive>1
    for n,r in routes.items():
        r['centerline_length_mm']=float(np.linalg.norm(np.diff(r['points_mm'],axis=0),axis=1).sum())
        assert next(p for p in geom['pads'] if p['ref']==r['source'])['net']==n
    result={'contract':'MOTOR-HARNESS-01 v0.1','date':'2026-09-15','pose_mm':[0,3],
        'routes':routes,'terminal_reserves':terminal_specs,'terminal_dimensions_estimated':True,
        'wire_hits_mm3':collisions,'terminal_hits_mm3':terminal_hits,'mechanical_hits_mm3':mechanical_hits,
        'mechanical_checks':mechanical_modes,'mechanism_alignments':alignments,'outside_cavity_mm3':outside,'pair_intersection_mm3':pair,
        'old_motor_route_tray_positive_mm3':positive,'physical_wiring':False}
    print(json.dumps({k:result[k] for k in ['wire_hits_mm3','terminal_hits_mm3','mechanical_hits_mm3','outside_cavity_mm3','pair_intersection_mm3']},indent=2),flush=True)
    clean=not any(collisions.values()) and not any(terminal_hits.values()) and not any(mechanical_hits.values()) and pair<.001 and max(outside.values())<.001
    for n,m in (wires|{'terminal-'+n:m for n,m in terminals.items()}).items():m.export(DEST/(n+'.stl'))
    result['nominal_complete_fit_passed']=clean
    result['wire_fit_passed']=not any(collisions.values()) and not any(mechanical_hits[n] for n in wires) and pair<.001 and max(outside.values())<.001
    # The moving shell/battery check reuses the previous fixed inventory plus new
    # leads/terminal reserves. Extra upstream mechanics above qualify LOCAL fit
    # only; their unclosed bounding boxes are not global service solids.
    result['service']=service_check(obstacles|{'terminal-'+n:m for n,m in terminals.items()},wires,read)
    result['limitations']=[
        'Terminal size/offsets are explicit estimates; A reserve overlaps the aligned upstream side piece',
        'Wire fit does not qualify terminal or motor installation, suspension travel, bends, strain relief or current',
        'Service uses prior fixed inventory plus new wires/terminals; local extra mechanics are not a whole moving-mechanism proof',
        'Existing approximate input-power route still intersects the tray; servo/input/charger wiring remains unfinished']
    for p in [Path(__file__),ROOT/'tools/build_xiao_power_harness.py',ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py',ROOT/'build/drive-research/f130.pdf']:record(p)
    result['source_sha256']=sources
    result['drawing_url']='https://static.chipdip.ru/lib/799/DOC059799826.pdf'
    result['artifact_sha256']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(DEST.glob('*.stl'))}
    (ROOT/'docs/evidence/motor-harness-v01.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Wire fit:',result['wire_fit_passed'],'complete fit:',clean,'service:',result['service']['passed'])


if __name__=='__main__':main()
