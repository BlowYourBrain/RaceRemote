"""A referenced D3 signal lead to the approximate servo-side packaging waypoint."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import mesh, tube
from layout_zcar_battery import box
from build_xiao_power_harness import hits, service_check

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-wiring'


def main():
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
    prior = data('docs/evidence/servo-bundle-v01.json')
    excluded = {'cad/xiao-mount/cradle.stl', 'cad/packaging-v05/inputs/adjustable-cavity.stl'}
    obstacles = {name: read(name) for name in prior['source_sha256']
                 if name.endswith('.stl') and name not in excluded}
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
    reference = data('cad/servo-wiring/pad-reference.json')
    pad = reference['pad']['candidate_xyz_mm']
    points = [pad, [pad[0], 85.4, pad[2]], [pad[0], 85.4, 21.6],
              [pad[0], 90.5, 21.6], [49.2, 90.5, 21.6],
              [49.2, 95, 19], [44.8, 89, 16], [43.8, 78, 14]]
    # Flat end at front PCB copper plane; clip only the local contact sphere.
    # The downstream wire may still return to Y78. All component boxes stay whole.
    full = tube(points, .4)
    contact = box([1.6, 1, 1.6], pad)
    rear_contact = trimesh.boolean.intersection([contact, box([200, 200, 200], [0, -16.75, 0])], engine='manifold')
    wire = trimesh.boolean.difference([full, rear_contact], engine='manifold')
    assert wire.is_volume
    cavity = read('cad/packaging-v05/inputs/adjustable-cavity.stl')
    collision = hits(wire, obstacles)
    outside = float(trimesh.boolean.difference([wire, cavity], engine='manifold').volume)
    assert np.isfinite(outside) and outside >= -1e-7
    shortcut = tube([pad, [pad[0], 90.5, pad[2]], [49.2, 90.5, pad[2]]], .4)
    shortcut_hits = hits(shortcut, {'xiao-71': obstacles['xiao-71']})
    assert shortcut_hits.get('xiao-71', 0) > .5
    result = {'contract': 'SERVO-WIRING-01 v0.1', 'date': '2026-09-15', 'pose_mm': [0, 3],
              'source': 'XIAO U9.4 / D3 / GPIO4', 'target': 'approximate servo cable waypoint; no connector pin assigned',
              'points_mm': points, 'radius_mm': .4,
              'centerline_length_mm': float(np.linalg.norm(np.diff(points, axis=0), axis=1).sum()),
              'new_hits_mm3': collision, 'outside_cavity_mm3': outside,
              'straight_front_exit_positive_control_mm3': shortcut_hits,
              'all_xiao_boxes_unmodified': True, 'physical_wiring': False,
              'complete_servo_harness': False,
              'limitations': ['OD0.8 is a space allocation, not a chosen wire',
                              'Signal ends near approximate servo exit; no connector, splice or strain relief modeled',
                              'No power pair, 3.3V input threshold, unpowered input or pulse calibration qualified',
                              'No dynamic steering/suspension, real body fit or other adjustment positions checked']}
    print(json.dumps(result, indent=2), flush=True)
    if collision or outside > .001:
        raise RuntimeError('Signal route is not clear')
    wire.export(DEST / 'signal.stl')
    # Rebuild from the checked definition: exported display STL has degenerate
    # junctions on reload. Do not silently repair it and call that checked volume.
    reserve = tube(prior['points_mm'], prior['radius_mm'])
    assert reserve.is_volume
    result['bundle_service_geometry'] = 'Rebuilt from servo-bundle-v01 points/radius; display STL not used as a solid'
    # A bundle reserve may contain its signal lead; it is not another solid wire.
    result['service'] = service_check(obstacles, {'servo-signal': wire, 'servo-bundle-reserve': reserve}, read)
    for p in [Path(__file__), ROOT / 'tools/build_packaging_v05.py', ROOT / 'tools/layout_zcar_battery.py', ROOT / 'tools/build_xiao_power_harness.py']:
        record(p)
    result['source_sha256'] = sources
    result['artifact_sha256'] = {'cad/servo-wiring/signal.stl': hashlib.sha256((DEST / 'signal.stl').read_bytes()).hexdigest()}
    (ROOT / 'docs/evidence/servo-wiring-v01.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    assert result['service']['passed']


if __name__ == '__main__':
    main()
