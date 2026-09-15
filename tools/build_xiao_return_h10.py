"""Move XIAO GND0 to dedicated H10, leaving H5 for traction supply return."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,tube,intersect
from layout_zcar_battery import box
from build_xiao_power_harness import hits,service_check

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/xiao-return-h10'


def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def record(p):sources[p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest();return p
    def data(n):return json.loads(record(ROOT/n).read_text(encoding='utf-8'))
    def read(n):
        m=mesh(record(ROOT/n));assert m.is_volume,n
        return m
    previous=data('docs/evidence/motor-harness-v01.json')
    omit={'cad/xiao-power-harness/GND.stl','cad/packaging-v05/inputs/adjustable-cavity.stl'}
    obstacles={n:read(n) for n in previous['source_sha256'] if n.endswith('.stl') and n not in omit and not n.startswith('cad/reference/')}
    for n in ['FO','BO','terminal-A','terminal-B']:obstacles['motor-'+n]=read('cad/motor-harness/'+n+'.stl')
    xiao=data('docs/evidence/xiao-mount-v01.json')
    for n,b in xiao['source_bounding_boxes_mm'].items():
        b=np.array(b);obstacles['xiao-'+n]=box(b[1]-b[0],b.mean(axis=0))
    old=data('docs/evidence/packaging-v05.json')['wire_corridors']|xiao['revised_corridor_definitions']
    for n in ['wire-power','wire-servo']:obstacles[n]=tube(old[n]['points_mm'],old[n]['radius_mm'])
    control=data('docs/evidence/control-harness-v01.json')['routes']
    for n in ['FI','BI']:obstacles['control-'+n]=tube(control[n]['points_mm'],control[n]['radius_mm'])
    spec=data('hardware/drive-power-carrier/geometry.json')
    pad=next(p for p in spec['pads'] if p['ref']=='H10');assert pad['net']=='DRIVE_GND'
    target=[25.75+pad['xy_mm'][1],47+pad['xy_mm'][0],29.5]
    source=data('cad/xiao-power-harness/pad-reference.json')['pads']['GND0']['candidate_xyz_mm']
    route={'source':'GND0','target':'H10','net':'DRIVE_GND','radius_mm':.6,
           'points_mm':[source,[25.63183,79.5,26.445],[25.63183,79.5,28.3],[25.63183,78.1,28.3],[25.63183,67.5,28.3],[27.45,67.5,28.3],[27.45,65,28.3],target]}
    route['centerline_length_mm']=float(np.linalg.norm(np.diff(route['points_mm'],axis=0),axis=1).sum())
    wire=trimesh.boolean.intersection([tube(route['points_mm'],route['radius_mm']),box([200,200,200],[0,-18,0])],engine='manifold')
    pcb=obstacles.pop('cad/drive-power-carrier/pcb.stl')
    obstacles['current-carrier-pcb']=trimesh.boolean.difference([pcb,box([1.6,1.6,1.6],target)],engine='manifold')
    found=hits(wire,obstacles)
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside=float(trimesh.boolean.difference([wire,cavity],engine='manifold').volume)
    print('New return hits:',found,'outside:',outside,flush=True)
    # Uncut old cradle remains a meaningful check of the source contact passage.
    positive=intersect(wire,read('cad/xiao-mount/cradle.stl'));assert positive>.1
    result={'contract':'XIAO-RETURN-H10-01 v0.1','date':'2026-09-15','route':route,'pose_mm':[0,3],
            'wire_hits_mm3':found,'outside_cavity_mm3':outside,'old_cradle_positive_mm3':positive,
            'H5_role':'traction supply return; no XIAO wire','physical_wiring':False,
            'inherited_terminal_A_collision_unresolved':True,'input_power_routing_complete':False}
    wire.export(DEST/'GND.stl')
    if not found and outside<.001:result['service']=service_check(obstacles,{'xiao-GND':wire},read)
    for p in [Path(__file__),ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py',ROOT/'tools/build_xiao_power_harness.py']:record(p)
    result['source_sha256']=sources
    result['artifact_sha256']={'cad/xiao-return-h10/GND.stl':hashlib.sha256((DEST/'GND.stl').read_bytes()).hexdigest()}
    (ROOT/'docs/evidence/xiao-return-h10-v01.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    assert not found and outside<.001
    assert result['service']['passed']


if __name__=='__main__':main()
