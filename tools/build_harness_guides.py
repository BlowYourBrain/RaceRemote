"""Routing saddles and nominal service audit for the current XIAO holder."""
import hashlib
import json
import subprocess
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import intersect,mesh,tube
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/harness-guides'
BASE=ROOT/'cad/packaging-v05'
XIAO=ROOT/'cad/xiao-mount'

def main():
    DEST.mkdir(exist_ok=True)
    sources={}
    def read(path):
        sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest()
        return mesh(path)
    parts={}
    for n in ['power_guide','servo_guide','tray','cap','tray_print','cap_print']:
        path=DEST/f'{n}.stl'
        r=subprocess.run(['C:/Program Files/OpenSCAD/openscad.com','-o',str(path),'-D',f'part="{n}"',str(ROOT/'cad/harness-guides.scad')],capture_output=True,text=True,check=True)
        assert 'WARNING'not in r.stderr and 'ERROR'not in r.stderr,r.stderr
        parts[n]=mesh(path);assert parts[n].is_volume,n
        assert len(parts[n].split())==1,n
    fixed={n:read(BASE/f'{n}.stl')for n in ['frame-relief-proposal','servo-installed','buck','buck_plug','pads','driver','charge','antenna','antenna_support']}
    for n in ['cover','adjustable-support','adjustable-main_fasteners','usb-base','usb-pcb','usb-connector','usb-components']:
        fixed[n]=read(BASE/f'inputs/{n}.stl')
    for n in ['cradle','screws','stage_screws']:fixed[n]=read(XIAO/f'{n}.stl')
    fixed.update({n:parts[n]for n in ['tray','cap']})
    fixed['motor']=read(ROOT/'cad/components/motor-installed.stl')
    a=json.loads((ROOT/'docs/evidence/packaging-v05.json').read_text())
    b=json.loads((ROOT/'docs/evidence/xiao-mount-v01.json').read_text())
    routes=a['wire_corridors']|b['revised_corridor_definitions']
    fixed.update({n:tube(p['points_mm'],p['radius_mm'])for n,p in routes.items()})
    component_boxes={f'component-{n}':box(np.array(v)[1]-np.array(v)[0],np.array(v).mean(axis=0))for n,v in b['source_bounding_boxes_mm'].items()}
    fixed.update(component_boxes)
    battery=read(BASE/'inputs/adjustable-battery.stl');guards=read(BASE/'inputs/adjustable-guards.stl');band=read(BASE/'inputs/adjustable-band.stl')
    cavity=read(BASE/'inputs/adjustable-cavity.stl');body=read(BASE/'inputs/usb-body.stl');lid=read(BASE/'inputs/usb-cover.stl')
    usb=read(XIAO/'usb_access.stl')
    collisions={}
    for n in ['power_guide','servo_guide']:
        # Integral union with its own printed host is deliberate, not a clearance error.
        host='tray'if n=='power_guide'else'cap'
        hits={k:intersect(parts[n],m)for k,m in (fixed|{'usb_access':usb}).items()if k!=host}
        collisions[n]={k:v for k,v in hits.items()if v>.001}
    outside={n:float(trimesh.boolean.difference([parts[n],cavity],engine='manifold').volume)for n in ['tray','cap']}
    left,right=sorted(guards.split(),key=lambda m:m.bounds.mean(axis=0)[0])
    swept=box(battery.extents+[50,0,0],battery.bounds.mean(axis=0)-[25,0,0])
    battery_hits={k:intersect(swept,m)for k,m in (fixed|{'right_guard':right}).items()}
    guard_path=[]
    for d in np.arange(0,6.01,.25):
        moved=left.copy();moved.apply_translation([-float(d),0,0])
        h={k:intersect(moved,m)for k,m in (fixed|{'battery':battery,'right_guard':right}).items()}
        guard_path.append({'outward_mm':float(d),'hits':{k:v for k,v in h.items()if v>.001}})
    body_path=[]
    poses=[[0,0,float(z)]for z in np.arange(0,2.01,.25)]+[[float(x),0,2]for x in np.arange(.25,1.01,.25)]+[[1,0,float(z)]for z in range(3,66)]
    for xyz in poses:
        h={}
        for name,m in [('body',body),('lid',lid)]:
            moved=m.copy();moved.apply_translation(xyz)
            for k,v in (fixed|{'battery':battery,'guards':guards,'band':band}).items():
                amount=intersect(moved,v)
                if amount>.001:h[f'{name}/{k}']=amount
        body_path.append({'offset_mm':xyz,'hits':h})
    wrong=body.copy();wrong.apply_translation([0,0,7])
    controls={'retained_left_guard':intersect(swept,left),'straight_lift_usb':intersect(wrong,fixed['usb-connector'])}
    # To service the cap, first release the tie and lift the servo lead out of its saddle.
    # Its new free shape is not simulated. Clamp screws are removed before moving the cap.
    cap_path=[]
    cap_obstacles={k:m for k,m in fixed.items()if k not in ['cap','screws','wire-servo']}
    for xyz in [[0,0,float(z)]for z in np.arange(0,1.101,.1)]+[[0,float(y),1.1]for y in np.arange(.5,8.01,.5)]:
        moved=parts['cap'].copy();moved.apply_translation(xyz)
        h={k:intersect(moved,m)for k,m in cap_obstacles.items()}
        cap_path.append({'offset_mm':xyz,'hits':{k:v for k,v in h.items()if v>.001}})
    for p in [Path(__file__),ROOT/'cad/harness-guides.scad',ROOT/'cad/harness-guides-assembly.scad',ROOT/'cad/xiao-mount.scad',ROOT/'docs/evidence/packaging-v05.json',ROOT/'docs/evidence/xiao-mount-v01.json']:
        sources[p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
    report={'version':'0.1','date':'2026-09-15','physical_fit_verified':False,'camera_electronics_mm':[3,0],
            'guide_collisions_mm3':collisions,'outside_body_mm3':outside,
            'battery_sweep_mm3':battery_hits,'left_guard_samples':guard_path,'body_samples':body_path,
            'cap_samples':cap_path,'cap_service_requires':['Remove clamp screws','Release tie and move servo lead out of saddle; free wire shape not simulated'],
            'positive_controls_mm3':controls,'source_sha256':sources,
            'print_bounds_mm':{n:parts[n].bounds.tolist()for n in ['tray_print','cap_print']},
            'limits':['Open saddles need removable ties/lacing; no elastic retention or cable squeeze qualification',
                      'Saddle slots are size allocations, no purchased tie SKU',
                      'Nominal pose only; sampled body and guard motions, continuous battery box sweep',
                      'Battery leads, fingers, magnets, some suspension parts and real cable plugs omitted',
                      'Cap sampled with servo lead moved aside and screws removed; no flexible-wire reach/strain proof',
                      'No print, electrical connection, real OV3660 size, RF or thermal validation']}
    (ROOT/'docs/evidence/harness-guides-v01.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'collisions':collisions,'outside':outside,'battery_hits':{k:v for k,v in battery_hits.items()if v>.001},'guard_blocked':[p for p in guard_path if p['hits']],'body_blocked':[p for p in body_path if p['hits']]},indent=2),flush=True)
    assert all(v>1 for v in controls.values())
    assert all(not v for v in collisions.values()) and max(outside.values())<.001
    assert max(battery_hits.values())<.001 and all(not p['hits']for p in guard_path+body_path+cap_path)

if __name__=='__main__':main()
