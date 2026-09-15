"""Nominal rear-pad power pair and local cradle passage; no hardware actions."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh,intersect,tube
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/xiao-power-harness'
OUT=ROOT/'docs/evidence/xiao-power-harness-v01.json'


def hits(moving, obstacles):
    result={}
    for name,solid in obstacles.items():
        volume=intersect(moving,solid)
        assert np.isfinite(volume) and volume>=-1e-7,name
        if volume>.001:result[name]=round(volume,6)
    return result


def service_check(obstacles,wires,read):
    prefix='cad/packaging-v05/inputs/'
    moving_names=['adjustable-battery','adjustable-guards','adjustable-band','usb-body','usb-cover']
    moving={n:obstacles[prefix+n+'.stl'] for n in moving_names}
    fixed={n:m for n,m in obstacles.items() if n not in {prefix+s+'.stl' for s in moving_names}}
    fixed.update({'power-wire-'+n:m for n,m in wires.items()})
    # Contact exceptions are for fit only; service sees the complete carrier PCB.
    fixed['current-carrier-pcb']=read('cad/drive-power-carrier/pcb.stl')
    battery=moving['adjustable-battery'];guards=moving['adjustable-guards']
    sides=sorted(guards.split(),key=lambda m:m.centroid[0]);assert len(sides)==2
    left,right=sides
    assert abs(np.prod(battery.extents)-battery.volume)<.001
    swept=box(battery.extents+[50,0,0],battery.bounds.mean(axis=0)-[25,0,0])
    battery_hits=hits(swept,fixed|{'right-guard':right})
    guard_results=[]
    for d in np.arange(0,6.01,.25):
        m=left.copy();m.apply_translation([-d,0,0])
        guard_results.append({'outward_mm':float(d),'hits_mm3':hits(m,fixed|{'battery':battery,'right-guard':right})})
    poses=[[0,0,float(z)] for z in np.arange(0,2.01,.25)]
    poses += [[float(x),0,2] for x in np.arange(.25,1.01,.25)]
    poses += [[1,0,float(z)] for z in np.arange(2.25,65.01,.25)]
    body_results=[]
    for i,xyz in enumerate(poses):
        found={}
        for name in ['usb-body','usb-cover']:
            m=moving[name].copy();m.apply_translation(xyz)
            found.update({name+'/'+n:v for n,v in hits(m,fixed|{'battery':battery,'guards':guards,'band':moving['adjustable-band']}).items()})
        body_results.append({'offset_mm':xyz,'hits_mm3':found})
        if i%100==0:print(f'Service body pose {i+1}/{len(poses)}',flush=True)
    wrong=moving['usb-body'].copy();wrong.apply_translation([0,0,7])
    controls={'retained_left_guard_mm3':intersect(swept,left),
              'straight_body_lift_usb_mm3':intersect(wrong,fixed[prefix+'usb-connector.stl'])}
    assert min(controls.values())>1
    return {'fixed_inventory':sorted(fixed),'fixed_solid_count':len(fixed),'battery_sweep_hits_mm3':battery_hits,
            'battery_continuous_exit_mm':[-50,0,0],'body_samples':body_results,'left_guard_samples':guard_results,
            'positive_controls':controls,'passed':not battery_hits and not any(r['hits_mm3'] for r in body_results+guard_results),
            'limitations':['Only nominal 0/+3 pose and hypothetical shell; body/guard sampled every .25mm',
                           'Battery box only; pack tails, connectors, fingers, ties, magnets and some moving mechanism absent',
                           'Power off and external USB unplugged; remove body/band and electrically disconnect battery before extraction',
                           'No PCB/camera extraction or strain, flexibility, current, heat or physical qualification']}


def main():
    sources={}
    def record(path):sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest();return path
    def data(path):return json.loads(record(ROOT/path).read_text(encoding='utf-8'))
    def read(path):m=mesh(record(ROOT/path));assert m.is_volume,path;return m
    pads=data('cad/xiao-power-harness/pad-reference.json')['pads']
    spec=data('hardware/drive-power-carrier/geometry.json')
    targets={v['ref']:[25.75+v['xy_mm'][1],47+v['xy_mm'][0],31.1] for v in spec['pads'] if v['ref'].startswith('H')}
    routes={
      'BAT':{'source':'BAT0','target':'H9','net':'XIAO_BAT','color':[.85,.12,.12],
        'points_mm':[pads['BAT0']['candidate_xyz_mm'],[23.72683,79.5,26.445],[23.72683,79.5,38.6],
                     [35.25,79.5,38.6],[35.25,75.2,38.6],targets['H9']]},
      'GND':{'source':'GND0','target':'H5','net':'DRIVE_GND','color':[.17,.17,.18],
        'points_mm':[pads['GND0']['candidate_xyz_mm'],[25.63183,79.5,26.445],[25.63183,79.5,28.3],
                     [25.63183,78.1,28.3],[25.63183,78.1,40],
                     [41.75,78.1,40],[41.75,75,40],targets['H5']]}}
    for r in routes.values():
        r['radius_mm']=.6
        r['centerline_length_mm']=float(np.linalg.norm(np.diff(r['points_mm'],axis=0),axis=1).sum())
        assert next(v for v in spec['pads'] if v['ref']==r['target'])['net']==r['net']
    # Flat solder contact at the back PCB plane, not a spherical cap through the PCB.
    rear_halfspace=box([200,200,200],[0,-18,0])  # maximum Y=82
    wires={n:trimesh.boolean.intersection([tube(r['points_mm'],r['radius_mm']),rear_halfspace],engine='manifold')
           for n,r in routes.items()}
    assert all(m.is_volume for m in wires.values())
    oldcradle=read('cad/xiao-mount/cradle.stl')
    # Shared rectangular window around the two rear solder pads, through rear plate only.
    window=box([5.2,2.0,2.8],[24.68,81.05,26.445])
    cradle=trimesh.boolean.difference([oldcradle,window],engine='manifold');assert cradle.is_volume
    original_hits={n:intersect(m,oldcradle) for n,m in wires.items()}
    obstacles={}
    previous=data('docs/evidence/current-service-v01.json')
    for filename in previous['source_sha256']:
        if not filename.endswith('.stl') or filename in ('cad/motor-carrier/pcb.stl','cad/xiao-mount/cradle.stl'):continue
        obstacles[filename]=read(filename)
    obstacles['new-cradle']=cradle
    # Replace the old carrier with the current PCB and all added component reserves.
    for name in ['pcb',*spec['new_component_positions']]:
        if name=='H9':continue  # Its lead reserve is replaced by the new wire, not an obstacle.
        obstacles['current-carrier-'+name]=read(f'cad/drive-power-carrier/{name}.stl')
    xiao=data('docs/evidence/xiao-mount-v01.json')
    for name,b in xiao['source_bounding_boxes_mm'].items():
        b=np.array(b);m=box(b[1]-b[0],b.mean(axis=0))
        obstacles['xiao-'+name]=m
    pcb_masks=[box([1.6,1.6,1.4],r['points_mm'][-1]) for r in routes.values()]
    obstacles['current-carrier-pcb']=trimesh.boolean.difference([obstacles['current-carrier-pcb'],*pcb_masks],engine='manifold')
    oldroutes=data('docs/evidence/packaging-v05.json')['wire_corridors']|xiao['revised_corridor_definitions']
    for n in ('wire-power','wire-servo','wire-motor'):
        r=oldroutes[n];obstacles[n]=tube(r['points_mm'],r['radius_mm'])
    control=data('docs/evidence/control-harness-v01.json')
    for n in ('FI','BI'):
        r=control['routes'][n];obstacles['control-'+n]=tube(r['points_mm'],r['radius_mm'])
    # The new GND0 return replaces the prior thin U9.13-to-H3 wire.
    cavity=read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    collisions={n:hits(m,obstacles) for n,m in wires.items()}
    pairs=intersect(wires['BAT'],wires['GND'])
    outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume) for n,m in wires.items()}
    report={'date':'2026-09-15','contract':'XIAO-POWER-HARNESS-01 v0.1','pose_mm':[0,3],
            'routes':routes,'wire_obstacle_hits_mm3':collisions,'pair_intersection_mm3':pairs,'outside_cavity_mm3':outside,
            'old_cradle_positive_control_mm3':original_hits,'cradle_removed_mm3':oldcradle.volume-cradle.volume,
            'cradle_window_bounds_mm':window.bounds.tolist(),'replaced_wire':'control-GND U9.13 to H3',
            'contact_exceptions':'Wire ends flat at XIAO rear plane Y82; all XIAO boxes unmodified. H9/H5 windows on new carrier PCB; H9 dummy lead replaced',
            'physical_wiring':False,'wire_sku_selected':False,'strain_relief_qualified':False,'power_domain_verified':False}
    for n,m in wires.items():m.export(DEST/(n+'.stl'))
    cradle.export(DEST/'cradle.stl');window.export(DEST/'window.stl')
    report['service']=service_check(obstacles,wires,read)
    record(Path(__file__));record(ROOT/'tools/build_packaging_v05.py');record(ROOT/'tools/layout_zcar_battery.py')
    report['source_sha256']=sources
    OUT.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:report[k] for k in ('wire_obstacle_hits_mm3','pair_intersection_mm3','outside_cavity_mm3','old_cradle_positive_control_mm3')},indent=2),flush=True)
    assert not any(collisions.values()) and pairs<.001 and max(outside.values())<.001
    assert min(original_hits.values())>1
    assert report['service']['passed']


if __name__=='__main__':main()
