"""Check service motions against the committed 0.5 geometry; no physical validation."""
import hashlib
import json
from pathlib import Path

import numpy as np
import trimesh

from build_packaging_v05 import intersect, mesh, shifted, tube
from layout_zcar_battery import box

ROOT = Path(__file__).resolve().parents[1]
CAD = ROOT / 'cad/packaging-v05'


def main():
    files = {'checker':Path(__file__).relative_to(ROOT).as_posix()}

    def read(name, path):
        files[name] = path.relative_to(ROOT).as_posix()
        return mesh(path)

    fixed = {n: read(n, CAD/f'{n}.stl') for n in
             ['frame-relief-proposal','servo-installed','buck','buck_plug','tray','pads',
              'driver','charge','antenna','antenna_support','camera_support',
              'wire-power','wire-servo','wire-antenna','wire-motor','wire-control']}
    for n in ['cover','adjustable-support','adjustable-main_fasteners','adjustable-camera_fasteners',
              'usb-base','usb-pcb','usb-connector','usb-components']:
        fixed[n] = read(n, CAD/f'inputs/{n}.stl')
    # Recreate analytic corridor inputs: float32 STL of wire-power loses watertightness.
    # Preserve the original display mesh; do not silently repair or use it as a solid.
    corridor_file = ROOT/'docs/evidence/packaging-v05.json'
    files['corridor_definitions'] = corridor_file.relative_to(ROOT).as_posix()
    corridors = json.loads(corridor_file.read_text(encoding='utf-8'))['wire_corridors']
    non_solid_wire_exports = [n for n in corridors if not fixed[n].is_volume]
    for n,p in corridors.items():
        fixed[n] = tube(p['points_mm'],p['radius_mm'])
    fixed['adjustable-camera_fasteners'] = shifted(fixed['adjustable-camera_fasteners'],3)
    fixed['motor'] = read('motor', ROOT/'cad/components/motor-installed.stl')
    camera = read('camera', ROOT/'cad/components/xiao-sense-2023-installed.stl')
    fixed['camera'] = shifted(box(camera.extents,camera.bounds.mean(axis=0)),3)
    battery = read('battery', CAD/'inputs/adjustable-battery.stl')
    guards = read('guards', CAD/'inputs/adjustable-guards.stl')
    band = read('band', CAD/'inputs/adjustable-band.stl')
    bodies = {'body':read('body',CAD/'inputs/usb-body.stl')}
    bodies['lid'] = read('lid',CAD/'inputs/usb-cover.stl')
    invalid = [n for n,m in {**fixed,'battery':battery,'guards':guards,'band':band,**bodies}.items() if not m.is_volume]
    assert not invalid, invalid
    sides = sorted(guards.split(),key=lambda m:m.bounds.mean(axis=0)[0])
    assert len(sides) == 2
    left,right = sides
    # A translating box has this exact swept solid for the whole straight path.
    sweep = box(battery.extents+[50,0,0], battery.bounds.mean(axis=0)-[25,0,0])
    battery_hits = {n:intersect(sweep,m) for n,m in {**fixed,'right_guard':right}.items()}
    left_guard_path = []
    for distance in np.arange(0,6.01,.25):
        moved = left.copy()
        moved.apply_translation([-float(distance),0,0])
        hits = {n:intersect(moved,m) for n,m in {**fixed,'battery':battery,'right_guard':right}.items()}
        left_guard_path.append({'outward_mm':float(distance),'overlaps_mm3':{n:v for n,v in hits.items() if v>.001}})

    # Retain battery, guards and band while lifting the body. User cable unplugged;
    # removable USB lid travels with the body. Magnets/fingers are not represented.
    obstacles = {**fixed,'battery':battery,'guards':guards,'band':band}
    xyzs = [[0,0,float(z)] for z in np.arange(0,2.01,.25)]
    xyzs += [[float(x),0,2] for x in np.arange(.25,1.01,.25)]
    xyzs += [[1,0,float(z)] for z in range(3,66)]
    removal = []
    for xyz in xyzs:
        hits = {}
        for bn,b in bodies.items():
            moved = b.copy()
            moved.apply_translation(xyz)
            for n,m in obstacles.items():
                v = intersect(moved,m)
                if v>.001:
                    hits[f'{bn}/{n}'] = v
        removal.append({'translation_mm':xyz,'overlaps_mm3':hits})
    wrong_body = bodies['body'].copy()
    wrong_body.apply_translation([0,0,7])
    controls = {'battery_exit_with_left_guard_mm3':intersect(sweep,left),
                'straight_body_lift_7mm_usb_collision_mm3':intersect(wrong_body,fixed['usb-connector'])}
    assert all(v > 1 for v in controls.values()), controls
    report = {
        'version':'0.1','date':'2026-09-15','packaging_version':'0.5',
        'nominal_electronics_camera_mm':[0,3],'physical_service_verified':False,
        'battery_exit':{'direction':'-X','distance_mm':50,'continuous_box_sweep':True,
                        'body_removed':True,'band_removed':True,'left_guard_removed':True,
                        'right_guard_retained':True,'overlaps_mm3':battery_hits},
        'left_guard_removal_samples':left_guard_path,'body_removal_samples':removal,
        'positive_controls':controls,
        'wire_check_geometry':'Rebuilt from pinned radius/points, original display STL unchanged',
        'non_solid_wire_display_exports':non_solid_wire_exports,
        'source_hashes':{path:hashlib.sha256((ROOT/path).read_bytes()).hexdigest() for path in files.values()},
        'limitations':['Battery is seller-size box without its connectors or wires',
                       'Wire corridors are rigid estimates; disconnected battery lead must be handled separately',
                       'Body/guard paths sampled, not continuous collision proof',
                       'Only listed closed solids checked; remaining reference suspension/gears omitted',
                       'No finger clearance, magnet retention, strap elasticity, forces or physical fit checks',
                       'Only nominal 0/+3mm pose, not all accepted adjustment pairs',
                       'Real body, full populated charger/driver and service USB plug remain open'],
    }
    out = ROOT/'docs/evidence/packaging-service-v01.json'
    out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'battery_collisions':{n:v for n,v in battery_hits.items() if v>.001},
                      'left_guard_blocked_samples':[p for p in left_guard_path if p['overlaps_mm3']],
                      'body_blocked_samples':[p for p in removal if p['overlaps_mm3']],
                      'positive_controls':controls},indent=2))
    assert max(battery_hits.values()) < .001, 'Battery path blocked; inspect report'
    assert all(not p['overlaps_mm3'] for p in left_guard_path), 'Left guard path blocked'
    assert all(not p['overlaps_mm3'] for p in removal), 'Body path blocked'


if __name__ == '__main__':
    main()
