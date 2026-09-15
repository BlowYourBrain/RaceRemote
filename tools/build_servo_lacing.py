"""Route the servo pair through the existing cap saddle and reserve removable lacing."""
import argparse
import copy
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh, tube, intersect
from layout_zcar_battery import box
from build_xiao_power_harness import hits, service_check

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-lacing'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fit-only', action='store_true')
    args = parser.parse_args()
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
    previous = data('docs/evidence/servo-power-v01.json')
    excluded = {'cad/xiao-mount/cradle.stl', 'cad/packaging-v05/inputs/adjustable-cavity.stl'}
    obstacles = {n: read(n) for n in previous['source_sha256'] if n.endswith('.stl') and n not in excluded}
    obstacles['current-carrier-pcb'] = obstacles.pop('cad/drive-power-carrier/pcb.stl')
    xiao = data('docs/evidence/xiao-mount-v01.json')
    for name, bounds in xiao['source_bounding_boxes_mm'].items():
        bounds = np.array(bounds)
        obstacles['xiao-' + name] = box(bounds[1] - bounds[0], bounds.mean(axis=0))
    for name, route in data('docs/evidence/control-harness-v01.json')['routes'].items():
        if name in ('FI', 'BI'):
            obstacles['control-' + name] = tube(route['points_mm'], route['radius_mm'])
    old = data('docs/evidence/packaging-v05.json')['wire_corridors'] | xiao['revised_corridor_definitions']
    obstacles['wire-power'] = tube(old['wire-power']['points_mm'], old['wire-power']['radius_mm'])
    routes = copy.deepcopy(previous['routes'])
    p = routes['VOUT']['points_mm']
    assert p[4:7] == [[48.4, 76, 40.3], [47.5, 78, 40.3], [47.5, 92, 40.3]]
    routes['VOUT']['points_mm'] = p[:5] + [[44, 80, 39.15], [44, 86, 39.15], [47.5, 90, 40.3]] + p[6:]
    p = routes['GND']['points_mm']
    assert p[3:5] == [[49, 46.2, 42.1], [49, 94, 42.1]]
    routes['GND']['points_mm'] = p[:4] + [[49, 77, 42.1], [44, 80, 40.95], [44, 86, 40.95], [49, 90, 42.1]] + p[4:]
    for n, r in routes.items():
        assert r['points_mm'][0] == previous['routes'][n]['points_mm'][0]
        assert r['points_mm'][-1] == previous['routes'][n]['points_mm'][-1]
        r['centerline_length_mm'] = float(np.linalg.norm(np.diff(r['points_mm'], axis=0), axis=1).sum())
    wires = {n: tube(r['points_mm'], r['radius_mm']) for n, r in routes.items()}
    cord_points = [[40.15, 84, 36.8], [40.15, 84, 42.7], [47, 84, 42.7],
                   [47, 84, 36.8], [40.15, 84, 36.8]]
    cord = tube(cord_points, .4)
    knot = box([3, 3, 2], [40.15, 84, 43.8])
    assert all(m.is_volume for m in [*wires.values(), cord, knot])
    buck_name = 'cad/packaging-v05/buck.stl'
    full_buck = obstacles[buck_name]
    windows = [box([1.9, 1.9, 1.8], r['points_mm'][0]) for r in routes.values()]
    obstacles[buck_name] = trimesh.boolean.difference([full_buck, *windows], engine='manifold')
    wire_hits = {n: hits(m, obstacles) for n, m in wires.items()}
    pair = intersect(wires['VOUT'], wires['GND'])
    obstacles[buck_name] = full_buck
    cord_hits = hits(cord, obstacles | wires)
    knot_hits = hits(knot, obstacles | wires)
    cavity = read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    additions = wires | {'cord': cord, 'knot-reserve': knot}
    outside = {n: float(trimesh.boolean.difference([m, cavity], engine='manifold').volume) for n, m in additions.items()}
    assert all(np.isfinite(v) and v >= -1e-7 for v in [pair, *outside.values()])
    checks = {'wire_hits_mm3': wire_hits, 'pair_intersection_mm3': pair,
              'cord_hits_mm3': cord_hits, 'knot_hits_mm3': knot_hits, 'outside_cavity_mm3': outside}
    print(json.dumps(checks, indent=2), flush=True)
    assert not any(wire_hits.values()) and pair < .001 and not cord_hits and not knot_hits and max(outside.values()) < .001
    if args.fit_only:
        print('Static fit passed; no service evidence exported')
        return
    # The aperture already exists in the cap: filling it must obstruct the cord.
    aperture_fill = box([1.3, 2.8, 1.6], [40.15, 84, 41.6])
    positive = intersect(cord, aperture_fill)
    assert positive > .1
    # A local upward displacement of the upper wire meets the cord, while the
    # same position is clear of the open cap. This is geometry, not a pull test.
    lifted_upper = tube([[44, 82.5, 41.95], [44, 85.5, 41.95]], .8)
    lift_check = {'upward_mm': 1.0, 'against_cord_mm3': intersect(lifted_upper, cord),
                  'against_open_cap_mm3': intersect(lifted_upper, obstacles['cad/harness-guides/cap.stl'])}
    assert lift_check['against_cord_mm3'] > .1 and lift_check['against_open_cap_mm3'] < .001
    service = service_check(obstacles, additions, read)
    assert service['passed']
    DEST.mkdir(exist_ok=True)
    for n, m in additions.items():
        m.export(DEST / (n + '.stl'))
    for path in [Path(__file__), ROOT / 'tools/build_packaging_v05.py', ROOT / 'tools/layout_zcar_battery.py',
                 ROOT / 'tools/build_xiao_power_harness.py', ROOT / 'cad/harness-guides.scad']:
        record(path)
    result = {'contract': 'SERVO-LACING-01 v0.1', 'date': '2026-09-15', 'pose_mm': [0, 3],
              'routes': routes, 'cord_points_mm': cord_points, 'cord_radius_mm': .4,
              'knot_bounds_mm': knot.bounds.tolist(), 'printed_parts_changed': False,
              **checks, 'filled_aperture_positive_control_mm3': positive,
              'local_upper_wire_lift_check': lift_check, 'service': service,
              'physical_retention_verified': False, 'complete_servo_harness': False,
              'limitations': ['One proposed removable cord loop; knot volume is a reserve, not tied topology',
                              'Captures the pair in the existing open saddle, does not prove axial strain relief',
                              'Cord material, knot, tension, insulation pressure, bends and impact retention unqualified',
                              'Cap extraction requires releasing lacing and moving wires; not simulated in this study',
                              'Nominal fixed assembly only; electrical, RF, servo endpoints and other adjustment poses remain open'],
              'source_sha256': sources,
              'artifact_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in DEST.glob('*.stl')}}
    (ROOT / 'docs/evidence/servo-lacing-v01.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print('PASS: proposed lacing and nominal service')


if __name__ == '__main__':
    main()
