"""Waveshare output-pair packaging proposal, with explicit unselected mating housing."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,intersect,tube
from layout_zcar_battery import box
from build_xiao_power_harness import hits,service_check

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/buck-output-harness'


def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def record(path):sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest();return path
    def data(path):return json.loads(record(ROOT/path).read_text(encoding='utf-8'))
    def read(path):m=mesh(record(ROOT/path));assert m.is_volume,path;return m
    previous=data('docs/evidence/xiao-power-harness-v01.json')
    spec=data('hardware/drive-power-carrier/geometry.json')
    targets={p['ref']:[25.75+p['xy_mm'][1],47+p['xy_mm'][0],31.1] for p in spec['pads'] if p['ref'].startswith('H')}
    # Vendor top view: local X starts at the lower GND edge, local Y at P1 input end.
    # Three GND then three VOUT positions, 2.54 pitch. Global rotate Z180 and translate.
    pins={str(i+1):{'net':'GND' if i<3 else 'VOUT','xy_mm':[round(19.75-(1.6+2.54*i),3),48.1]} for i in range(6)}
    routes={
      '5V':{'source':'P2.4','target':'H8','net':'WAVE_5V','radius_mm':.6,
            'points_mm':[[10.53,48.1,43.3],[10.53,48.1,44.5],[10.53,50,44.5],[23.2,50,44.5],
                         [23.2,48.1,36],[23.2,75,36],[27.75,75,36],targets['H8']]},
      'GND':{'source':'P2.3','target':'H3','net':'DRIVE_GND','radius_mm':.6,
             'points_mm':[[13.07,48.1,43.3],[13.07,48.1,44],[13.07,46.8,44],
                          [34.75,46.8,44],[34.75,46.8,32],[34.75,49,32],targets['H3']]}}
    for r in routes.values():
        r['centerline_length_mm']=float(np.linalg.norm(np.diff(r['points_mm'],axis=0),axis=1).sum())
        assert next(p for p in spec['pads'] if p['ref']==r['target'])['net']==r['net']
        assert r['points_mm'][0][:2]==pins[r['source'].split('.')[1]]['xy_mm']
    wires={n:tube(r['points_mm'],r['radius_mm']) for n,r in routes.items()}
    # Flat ends at estimated housing top, not spherical caps inside the mating housing.
    for n,m in wires.items():
        point=routes[n]['points_mm'][0]
        cap=box([1.4,1.4,1.2],[point[0],point[1],42.7])
        wires[n]=trimesh.boolean.difference([m,cap],engine='manifold')
        assert wires[n].is_volume
    obstacles={}
    for p in previous['source_sha256']:
        if not p.endswith('.stl'):continue
        if p in ['cad/xiao-mount/cradle.stl','cad/motor-carrier/pcb.stl',
                 'cad/drive-power-carrier/H8.stl','cad/drive-power-carrier/H9.stl',
                 'cad/packaging-v05/inputs/adjustable-cavity.stl']:continue
        obstacles[p]=read(p)
    obstacles['new-cradle']=read('cad/xiao-power-harness/cradle.stl')
    for n in ['BAT','GND']:obstacles['xiao-power-'+n]=read('cad/xiao-power-harness/'+n+'.stl')
    xiao=data('docs/evidence/xiao-mount-v01.json')
    for n,b in xiao['source_bounding_boxes_mm'].items():
        b=np.array(b);obstacles['xiao-'+n]=box(b[1]-b[0],b.mean(axis=0))
    old=data('docs/evidence/packaging-v05.json')['wire_corridors']|xiao['revised_corridor_definitions']
    for n in ['wire-power','wire-servo','wire-motor']:obstacles[n]=tube(old[n]['points_mm'],old[n]['radius_mm'])
    control=data('docs/evidence/control-harness-v01.json')['routes']
    for n in ['FI','BI']:obstacles['control-'+n]=tube(control[n]['points_mm'],control[n]['radius_mm'])
    pcb=obstacles.pop('cad/drive-power-carrier/pcb.stl')
    obstacles['current-carrier-pcb']=trimesh.boolean.difference([pcb,*[box([1.6,1.6,1.4],r['points_mm'][-1]) for r in routes.values()]],engine='manifold')
    collisions={n:hits(m,obstacles) for n,m in wires.items()}
    pair=intersect(wires['5V'],wires['GND'])
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume) for n,m in wires.items()}
    wrong=tube([[13.07,48.1,43.3],[13.07,48.1,46],[34.75,48.1,46],[34.75,49,46],targets['H3']],.6)
    wrong_body=intersect(wrong,obstacles['cad/packaging-v05/inputs/usb-body.stl'])
    assert wrong_body>1
    result={'contract':'BUCK-OUTPUT-HARNESS-01 v0.1','date':'2026-09-15','pose_mm':[0,3],
            'pins':pins,'routes':routes,'wire_obstacle_hits_mm3':collisions,'pair_intersection_mm3':pair,
            'outside_cavity_mm3':outside,'too_high_ground_positive_control_mm3':wrong_body,
            'connector_selected':False,'physical_wiring':False,
            'notes':['P2 grouping from vendor schematic; physical pin order inferred from top-view polarity marks',
                     'Existing estimated 1x6 mating housing retained; no polarity key/retention/contact rating qualification',
                     'Wire OD1.2 is a reserve, not a selected current-rated wire or a qualified bend radius']}
    for n,m in wires.items():m.export(DEST/(n+'.stl'))
    print(json.dumps({k:result[k] for k in ['wire_obstacle_hits_mm3','pair_intersection_mm3','outside_cavity_mm3']},indent=2),flush=True)
    if not any(collisions.values()) and pair<.001 and max(outside.values())<.001:
        result['service']=service_check(obstacles,wires,read)
    for p in [Path(__file__),ROOT/'tools/build_xiao_power_harness.py',ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py',
              ROOT/'cad/packaging-v05/layout.scad',ROOT/'build/buck-output-research/schematic.pdf',
              ROOT/'build/packaging-research/waveshare-size.jpg',ROOT/'build/packaging-research/waveshare-details-1.jpg']:
        record(p)
    result['source_sha256']=sources
    result['manufacturer_urls']={
        'schematic':'https://files.waveshare.com/wiki/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-Schematic.pdf',
        'dimensions':'https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-size.jpg',
        'photo':'https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-details-1.jpg'}
    result['artifact_sha256']={str((DEST/(n+'.stl')).relative_to(ROOT)).replace('\\','/'):hashlib.sha256((DEST/(n+'.stl')).read_bytes()).hexdigest() for n in wires}
    (ROOT/'docs/evidence/buck-output-harness-v01.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    assert not any(collisions.values()) and pair<.001 and max(outside.values())<.001
    assert result['service']['passed']


if __name__=='__main__':main()
