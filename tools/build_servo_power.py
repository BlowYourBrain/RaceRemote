"""Proposed independent Waveshare output pair to approximate servo cable ends."""
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh, tube, intersect
from layout_zcar_battery import box
from build_xiao_power_harness import hits, service_check

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-power'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--fit-only', action='store_true', help='Probe static fit without exporting final evidence')
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
    previous = data('docs/evidence/servo-wiring-v01.json')
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
    obstacles['servo-signal'] = read('cad/servo-wiring/signal.stl')
    reference = data('docs/evidence/direct-logic-harness-v01.json')
    pins = reference['pins']
    # Estimated pin geometry in packaging-v05/layout.scad: baseZ29.6, local
    # pin startsZ-1.5 and height11. This is the pin top39.1, not terminal top40.8.
    header_top = 29.6 - 1.5 + 11
    routes = {
        'VOUT': {'source_pin': '5', 'source_net': 'VOUT', 'color': [.85, .1, .1],
                 'points_mm': [[*pins['5']['xy_mm'], 39.1], [7.99, 48.1, 40.3], [7.99, 44.3, 40.3],
                               [48.4, 44.3, 40.3], [48.4, 76, 40.3], [47.5, 78, 40.3], [47.5, 92, 40.3],
                               [47.5, 95, 19], [43.2, 89, 16], [42.2, 79, 14]]},
        'GND': {'source_pin': '2', 'source_net': 'GND', 'color': [.15, .15, .16],
                'points_mm': [[*pins['2']['xy_mm'], 39.1], [15.61, 48.1, 42.1], [15.61, 46.2, 42.1],
                              [49.0, 46.2, 42.1], [49.0, 94, 42.1],
                              [49.0, 98, 17], [45.8, 90, 15], [45.8, 76.5, 15], [42.2, 76.5, 14]]}}
    occupied = {r['source'].split('.')[1].split()[0] for r in reference['routes'].values()}
    assert occupied == {'3', '4'}
    for name, route in routes.items():
        assert route['source_pin'] not in occupied
        assert pins[route['source_pin']]['net'] == route['source_net']
        assert abs(route['points_mm'][0][2] - header_top) < 1e-9
        route['source_contact'] = 'top of estimated existing header pin, soldered; no mating housing'
        route['radius_mm'] = .8
        route['centerline_length_mm'] = float(np.linalg.norm(np.diff(route['points_mm'], axis=0), axis=1).sum())
        route['target'] = 'approximate servo-side wiring point; no connector pin or splice assigned'
    wires = {n: tube(r['points_mm'], r['radius_mm']) for n, r in routes.items()}
    assert all(m.is_volume for m in wires.values())
    buck_name = 'cad/packaging-v05/buck.stl'
    full_buck = obstacles[buck_name]
    windows = [box([1.9, 1.9, 1.8], r['points_mm'][0]) for r in routes.values()]
    obstacles[buck_name] = trimesh.boolean.difference([full_buck, *windows], engine='manifold')
    collisions = {n: hits(m, obstacles) for n, m in wires.items()}
    pair = intersect(wires['VOUT'], wires['GND'])
    cavity = read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    outside = {n: float(trimesh.boolean.difference([m, cavity], engine='manifold').volume) for n, m in wires.items()}
    assert all(np.isfinite(v) and v >= -1e-7 for v in [pair, *outside.values()])
    checks = {'wire_obstacle_hits_mm3': collisions, 'pair_intersection_mm3': pair, 'outside_cavity_mm3': outside}
    print(json.dumps(checks, indent=2), flush=True)
    if args.fit_only and (any(collisions.values()) or pair > .001):
        for n, r in routes.items():
            for i, (a, b) in enumerate(zip(r['points_mm'], r['points_mm'][1:])):
                section_hits = hits(tube([a, b], r['radius_mm']),
                                    {k: obstacles[k] for k in collisions[n]} |
                                    {other: m for other, m in wires.items() if other != n})
                if section_hits:
                    print(n, i, section_hits, flush=True)
    assert not any(collisions.values()) and pair < .001 and max(outside.values()) < .001
    if args.fit_only:
        print('Static probe passed; no service or final evidence exported')
        return
    shortcuts = {n: hits(tube([r['points_mm'][0], r['points_mm'][-1]], r['radius_mm']), obstacles)
                 for n, r in routes.items()}
    assert all(shortcuts.values())
    obstacles[buck_name] = full_buck
    service = service_check(obstacles, wires, read)
    assert service['passed']
    DEST.mkdir(exist_ok=True)
    for n, m in wires.items():
        m.export(DEST / (n + '.stl'))
    for path in [Path(__file__), ROOT / 'tools/build_packaging_v05.py', ROOT / 'tools/layout_zcar_battery.py', ROOT / 'tools/build_xiao_power_harness.py', ROOT / 'cad/packaging-v05/layout.scad']:
        record(path)
    report = {'contract': 'SERVO-POWER-HARNESS-01 v0.1', 'date': '2026-09-15', 'pose_mm': [0, 3],
              'routes': routes, **checks, 'straight_shortcut_hits_mm3': shortcuts,
              'service': service, 'contact_window_bounds_mm': [w.bounds.tolist() for w in windows],
              'header_top_z_mm': header_top,
              'physical_pin_numbering_verified': False, 'wire_sku_selected': False, 'electrical_qualification': False,
              'complete_servo_harness': False, 'source_sha256': sources,
              'limitations': ['Independent pair from Waveshare P2.5/P2.2, not via XIAO or carrier logic traces',
                              'P2 groups sourced; numbering within same-net triples and tail height still estimated',
                              'Only two local buck pin contact windows; all other geometry retained; full buck restored for service',
                              'No actual connector/splice/strain relief, bending or current/thermal qualification',
                              'Nominal0/+3 only, hypothetical body and static steering; existing motor/input-power conflicts remain'],
              'artifact_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in DEST.glob('*.stl')}}
    (ROOT / 'docs/evidence/servo-power-v01.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('PASS: proposed power pair and nominal service')


if __name__ == '__main__':
    main()
