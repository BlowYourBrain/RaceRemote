"""Low soldered logic pair with an integral tray saddle and lacing passages."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,intersect,tube
from layout_zcar_battery import box
from build_xiao_power_harness import hits,service_check

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/direct-logic-harness'


def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def record(path):sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest();return path
    def data(path):return json.loads(record(ROOT/path).read_text(encoding='utf-8'))
    def read(path):m=mesh(record(ROOT/path));assert m.is_volume,path;return m
    previous=data('docs/evidence/xiao-power-harness-v01.json')
    spec=data('hardware/drive-power-carrier/geometry.json')
    targets={p['ref']:[25.75+p['xy_mm'][1],47+p['xy_mm'][0],29.5] for p in spec['pads'] if p['ref'].startswith('H')}
    targets['H8'][2]=31.1  # Positive lead lands on top; ground reaches the underside.
    # Vendor top view: local X starts at the lower GND edge, local Y at P1 input end.
    # Three GND then three VOUT positions, 2.54 pitch. Global rotate Z180 and translate.
    pins={str(i+1):{'net':'GND' if i<3 else 'VOUT','xy_mm':[round(19.75-(1.6+2.54*i),3),48.1]} for i in range(6)}
    routes={
      '5V':{'source':'P2.4 underside tail','target':'H8','net':'WAVE_5V','radius_mm':.8,
            'points_mm':[[10.53,48.1,28.6],[10.53,51.2,28.6],[20.8,51.2,28.6],
                         [21.6,51.2,30],[24.4,51.2,30],[24.9,51.2,28.6],
                         [24.9,51.2,34.6],[26.5,51.2,34.6],[26.5,75,34.6],
                         [27.75,75,34.6],targets['H8']]},
      'GND':{'source':'P2.3 underside tail','target':'H3','net':'DRIVE_GND','radius_mm':.8,
             'points_mm':[[13.07,48.1,28.6],[13.07,46.9,28.6],[20.8,46.9,28.6],
                          [20.8,48.2,28.6],[21.6,48.2,30],[24.4,48.2,30],
                          [24.9,48.2,28.6],[34.75,48.2,28.6],[34.75,49,28.6],targets['H3']]}}
    for r in routes.values():
        r['centerline_length_mm']=float(np.linalg.norm(np.diff(r['points_mm'],axis=0),axis=1).sum())
        assert next(p for p in spec['pads'] if p['ref']==r['target'])['net']==r['net']
        assert r['points_mm'][0][:2]==pins[r['source'].split('.')[1].split()[0]]['xy_mm']
    wires={n:tube(r['points_mm'],r['radius_mm']) for n,r in routes.items()}
    obstacles={}
    for p in previous['source_sha256']:
        if not p.endswith('.stl'):continue
        if p in ['cad/xiao-mount/cradle.stl','cad/motor-carrier/pcb.stl',
                 'cad/drive-power-carrier/H8.stl','cad/drive-power-carrier/H9.stl',
                 'cad/packaging-v05/inputs/adjustable-cavity.stl','cad/packaging-v05/buck_plug.stl']:continue
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
    obstacles['current-carrier-pcb']=trimesh.boolean.difference([pcb,*[box([1.9,1.9,1.8],r['points_mm'][-1]) for r in routes.values()]],engine='manifold')
    buck=obstacles['cad/packaging-v05/buck.stl']
    solder_windows=[box([1.9,1.9,1.8],r['points_mm'][0]) for r in routes.values()]
    obstacles['cad/packaging-v05/buck.stl']=trimesh.boolean.difference([buck,*solder_windows],engine='manifold')
    oldtray=obstacles.pop('cad/motor-carrier-mount/tray.stl')
    saddle=box([3.6,5.6,2.6],[23,49.7,28.4])
    grooves=[tube(r['points_mm'],.9) for r in routes.values()]
    slots=[box([2.8,8,1.0],[23,49.7,27.9])]
    tray=trimesh.boolean.difference([trimesh.boolean.union([oldtray,saddle],engine='manifold'),*grooves,*slots],engine='manifold')
    assert tray.is_volume and len(tray.split())==1
    obstacles['new-tray']=tray
    # Cord lacing avoids a large cable-tie head in the narrow space between boards.
    # These are occupancy reservations, not a selected cord or knot/force qualification.
    tie=tube([[22.5,46.5,27.9],[22.5,52.9,27.9],[22.5,52.9,31.3],
              [22.5,46.5,31.3],[22.5,46.5,27.9]],.4)
    tie_head=box([3,3,2],[22.5,49.7,32.7])
    added=trimesh.boolean.difference([tray,oldtray],engine='manifold')
    guide_hits=hits(added,{n:m for n,m in obstacles.items() if n!='new-tray'})
    tie_hits=hits(tie,obstacles|wires)
    head_hits=hits(tie_head,obstacles|wires)
    collisions={n:hits(m,obstacles) for n,m in wires.items()}
    pair=intersect(wires['5V'],wires['GND'])
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume) for n,m in wires.items()}
    positive=intersect(tie,trimesh.boolean.union([oldtray,saddle],engine='manifold'));assert positive>.1
    result={'contract':'DIRECT-LOGIC-HARNESS-01 v0.1','date':'2026-09-15','pose_mm':[0,3],
            'pins':pins,'routes':routes,'wire_obstacle_hits_mm3':collisions,'pair_intersection_mm3':pair,
            'outside_cavity_mm3':outside,'tie_against_solid_saddle_positive_mm3':positive,
            'guide_hits_mm3':guide_hits,'tie_hits_mm3':tie_hits,'tie_head_hits_mm3':head_hits,
            'internal_connector':'none; proposed soldered pair','physical_wiring':False,
            'saddle':{'bounds_mm':saddle.bounds.tolist(),'channel_centres_y_mm':[48.2,51.2],'channel_radius_mm':.9,
                      'wire_z_at_crown_mm':30,'tunnel_center_mm':[23,49.7,27.9],'tunnel_xz_mm':[2.8,1.0]},
            'inherited_old_tray_approximate_corridor_hits_mm3':hits(oldtray,{n:obstacles[n] for n in ['wire-power','wire-motor']}),
            'notes':['Source soldering windows only around two existing underside header tails; PCB pads and pin groups must be verified on received module',
                     'OD1.6 wire space includes0.2mm over the documented1.4mm cable example; current, bends and insulation not qualified',
                     'Lacing cord OD0.8 and knot3x3x2 are allocations, not selected supplies; tightening, creep and pull resistance untested',
                     'SM remains an optional service alternative; quick battery connector is unchanged']}
    for n,m in wires.items():m.export(DEST/(n+'.stl'))
    for n,m in [('tray',tray),('tie-space',tie),('tie-head-space',tie_head)]:m.export(DEST/(n+'.stl'))
    print(json.dumps({k:result[k] for k in ['wire_obstacle_hits_mm3','pair_intersection_mm3','outside_cavity_mm3','guide_hits_mm3','tie_hits_mm3','tie_head_hits_mm3']},indent=2),flush=True)
    clean=not any(collisions.values()) and pair<.001 and max(outside.values())<.001 and not guide_hits and not tie_hits and not head_hits
    if clean:
        obstacles['cad/packaging-v05/buck.stl']=buck
        obstacles['tie-space']=tie;obstacles['tie-head-space']=tie_head
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
    result['artifact_sha256']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(DEST.glob('*.stl'))}
    (ROOT/'docs/evidence/direct-logic-harness-v01.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    assert clean
    assert result['service']['passed']


if __name__=='__main__':main()
