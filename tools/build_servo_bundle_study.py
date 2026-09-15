"""Reserve a three-wire servo bundle around the current electronics, not through C2.

Both ends are packaging waypoints, not assigned electrical terminals. No physical
cable diameter, bend radius, connector, soldering or current rating is qualified.
"""
import hashlib
import json
import math
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh, tube
from layout_zcar_battery import box
from build_xiao_power_harness import hits, service_check

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-bundle-study'


def main():
    DEST.mkdir(exist_ok=True)
    sources = {}
    def record(path):
        sources[path.relative_to(ROOT).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
        return path
    def data(name):
        return json.loads(record(ROOT / name).read_text(encoding='utf-8'))
    def read(name):
        solid = mesh(record(ROOT / name))
        assert solid.is_volume, name
        return solid
    previous = data('docs/evidence/xiao-return-h10-v01.json')
    omitted = {'cad/xiao-mount/cradle.stl', 'cad/packaging-v05/inputs/adjustable-cavity.stl'}
    obstacles = {n: read(n) for n in previous['source_sha256'] if n.endswith('.stl') and n not in omitted}
    obstacles['current-carrier-pcb'] = obstacles.pop('cad/drive-power-carrier/pcb.stl')
    obstacles['xiao-GND'] = read('cad/xiao-return-h10/GND.stl')
    xiao = data('docs/evidence/xiao-mount-v01.json')
    for name, bounds in xiao['source_bounding_boxes_mm'].items():
        bounds = np.array(bounds)
        obstacles['xiao-' + name] = box(bounds[1] - bounds[0], bounds.mean(axis=0))
    for name, route in data('docs/evidence/control-harness-v01.json')['routes'].items():
        if name in ['FI', 'BI']:
            obstacles['control-' + name] = tube(route['points_mm'], route['radius_mm'])
    old = data('docs/evidence/packaging-v05.json')['wire_corridors'] | xiao['revised_corridor_definitions']
    obstacles['wire-power'] = tube(old['wire-power']['points_mm'], old['wire-power']['radius_mm'])
    points = [[42, 78, 38], [48.4, 78, 38], [48.4, 92, 34],
              [48.4, 95, 19], [44, 89, 16], [43, 78, 14]]
    # Three assumed OD1.4 wires, centre spacing1.6 in an equilateral section.
    required_radius = .7 + 1.6 / math.sqrt(3)
    radius = 1.65
    assert required_radius < radius
    old_thin = tube(old['wire-servo']['points_mm'], .9)
    old_wide = tube(old['wire-servo']['points_mm'], radius)
    bundle = tube(points, radius)
    assert bundle.is_volume and old_thin.is_volume and old_wide.is_volume
    cavity = read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside = float(trimesh.boolean.difference([bundle, cavity], engine='manifold').volume)
    report = {
        'contract': 'SERVO-BUNDLE-01 v0.1', 'date': '2026-09-15', 'pose_mm': [0, 3],
        'points_mm': points, 'radius_mm': radius,
        'assumed_wire_od_mm': 1.4, 'assumed_centre_spacing_mm': 1.6,
        'minimum_enclosing_radius_for_assumed_section_mm': required_radius,
        'centerline_length_mm': float(np.linalg.norm(np.diff(points, axis=0), axis=1).sum()),
        'old_radius_0_9_hits_mm3': hits(old_thin, obstacles),
        'old_radius_1_65_hits_mm3': hits(old_wide, obstacles),
        'new_hits_mm3': hits(bundle, obstacles), 'outside_cavity_mm3': outside,
        'electrical_endpoints_assigned': False, 'physical_cable_verified': False,
        'complete_servo_harness': False,
        'limitations': ['Triangular loose-wire bundle assumption; not a measured factory ribbon cable',
                       'No connector/strain relief, bend-radius, pinout or current qualification',
                       'Electronic-side waypoint moved because old one was inside C2',
                       'SG90-side waypoint remains approximate, not a verified cable exit',
                       'Service covers existing fixed assembly and hypothetical body, not moving steering/suspension',
                       'Inherited motor-terminal-A and input-power corridor conflicts remain'],
    }
    # The old path is an independent counterexample, not an artificial notch.
    assert report['old_radius_0_9_hits_mm3']['cad/motor-carrier/C2.stl'] > 2
    print(json.dumps({k: report[k] for k in ['old_radius_0_9_hits_mm3', 'old_radius_1_65_hits_mm3', 'new_hits_mm3', 'outside_cavity_mm3']}, indent=2), flush=True)
    bundle.export(DEST / 'bundle.stl')
    old_thin.export(DEST / 'old-bundle.stl')
    if not report['new_hits_mm3'] and outside < .001:
        report['service'] = service_check(obstacles, {'servo-bundle': bundle}, read)
    for path in [Path(__file__), ROOT / 'tools/build_packaging_v05.py', ROOT / 'tools/layout_zcar_battery.py', ROOT / 'tools/build_xiao_power_harness.py']:
        record(path)
    report['source_sha256'] = sources
    report['artifact_sha256'] = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in DEST.glob('*.stl')}
    (ROOT / 'docs/evidence/servo-bundle-v01.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    assert not report['new_hits_mm3'] and outside < .001
    assert report['service']['passed']


if __name__ == '__main__':
    main()
