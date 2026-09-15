"""Check an inline keyed SM envelope above the buck; do not transfer old wire proof."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,tube,intersect
from layout_zcar_battery import box
from build_xiao_power_harness import hits

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/logic-connector-study'


def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def record(p):sources[p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest();return p
    def data(p):return json.loads(record(ROOT/p).read_text(encoding='utf-8'))
    def read(p):m=mesh(record(ROOT/p));assert m.is_volume,p;return m
    previous=data('docs/evidence/buck-output-harness-v01.json')
    removed={'cad/xiao-mount/cradle.stl','cad/motor-carrier/pcb.stl',
             'cad/drive-power-carrier/H8.stl','cad/drive-power-carrier/H9.stl',
             'cad/packaging-v05/inputs/adjustable-cavity.stl',
             'cad/packaging-v05/buck_plug.stl'}
    obstacles={p:read(p) for p in previous['source_sha256'] if p.endswith('.stl') and p not in removed}
    obstacles['cradle']=read('cad/xiao-power-harness/cradle.stl')
    for n in ['BAT','GND']:obstacles['xiao-power-'+n]=read('cad/xiao-power-harness/'+n+'.stl')
    xiao=data('docs/evidence/xiao-mount-v01.json')
    for n,b in xiao['source_bounding_boxes_mm'].items():
        b=np.array(b);obstacles['xiao-'+n]=box(b[1]-b[0],b.mean(axis=0))
    old=data('docs/evidence/packaging-v05.json')['wire_corridors']|xiao['revised_corridor_definitions']
    for n in ['wire-power','wire-servo','wire-motor']:obstacles[n]=tube(old[n]['points_mm'],old[n]['radius_mm'])
    controls=data('docs/evidence/control-harness-v01.json')['routes']
    for n in ['FI','BI']:obstacles['control-'+n]=tube(controls[n]['points_mm'],controls[n]['radius_mm'])
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    extent=[8.5,23.4,9.2] # catalog mated 22.9, width8, height8.7 plus0.5 total allowance
    candidates=[]
    poses=[([12,61.5,40.3],extent),([12,62.1,40.3],extent),
           *[(xyz,[23.4,8.5,9.2]) for xyz in [[15,61,40.3],[15,64,40.3],[15,58,40.3],[16,61,40.3]]]]
    for xyz,size in poses:
        m=box(size,xyz);h=hits(m,obstacles)
        outside=float(trimesh.boolean.difference([m,cavity],engine='manifold').volume)
        candidates.append({'center_mm':xyz,'extent_mm':size,'hits_mm3':h,'outside_cavity_mm3':outside,'fits':not h and outside<.001})
    feasible=[p for p in candidates if p['fits']]
    result={'contract':'LOGIC-CONNECTOR-01 v0.1','date':'2026-09-15','pose_mm':[0,3],
            'housing_pair':['SMR-02V-B','SMP-02V-BC'],'extent_mm':extent,'candidates':candidates,
            'selected_pose':feasible[0] if feasible else None,'source_sha256':sources,
            'old_buck_output_wires_removed':True,'old_mating_housing_removed':True,
            'mount_wires_service_verified':False,'physical_verified':False}
    record(ROOT/'build/connector-research/jst-sm.pdf');record(Path(__file__))
    for p in ['tools/build_packaging_v05.py','tools/layout_zcar_battery.py','tools/build_xiao_power_harness.py']:record(ROOT/p)
    if feasible:box(feasible[0]['extent_mm'],feasible[0]['center_mm']).export(DEST/'sm-envelope.stl')
    (ROOT/'docs/evidence/logic-connector-fit-v01.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(candidates,indent=2),flush=True)
    assert feasible,'No sampled pose fits; retain failed candidates as evidence.'


if __name__=='__main__':main()
