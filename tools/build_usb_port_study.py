"""Build the side USB-C packaging proposal and measure geometric clearances.

Requires prior adjustable/candidate exports; never substitutes these checks for
PCB design, cable measurements, electrical verification or print testing.
"""
import base64
import hashlib
import json
from pathlib import Path
import subprocess
import urllib.request

import numpy as np
import trimesh

from layout_zcar_battery import box, overlap, broad_phase

ROOT = Path(__file__).resolve().parents[1]
OPENSCAD = 'C:/Program Files/OpenSCAD/openscad.com'
DRAWING_URL = 'https://gct.co/files/drawings/usb4105.pdf'
DRAWING_SHA = 'fb331fbabee8392ed2937ed757c1610cb0f174b84625147c0b580a18eea8c0e5'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def export_mesh(mesh, path):
    # CGAL may reorder unchanged triangles. Preserve prior STL bytes when
    # both solids and bounds are equivalent, instead of churning the PCB.
    # Compare the float32 coordinates that binary STL will actually store.
    mesh = mesh.copy()
    mesh.vertices = mesh.vertices.astype(np.float32)
    if path.exists():
        old = trimesh.load_mesh(path)
        if old.is_volume and np.allclose(old.bounds, mesh.bounds, atol=1e-6, rtol=0):
            common = overlap(old, mesh)
            if abs(old.volume-common) < 1e-5 and abs(mesh.volume-common) < 1e-5:
                return
    mesh.export(path)


def main():
    cad = ROOT / 'cad'
    out = ROOT / 'build/usb-port'
    out.mkdir(parents=True, exist_ok=True)
    drawing = out / 'usb4105.pdf'
    if not drawing.exists():
        drawing.write_bytes(urllib.request.urlopen(DRAWING_URL, timeout=40).read())
    assert drawing.read_bytes().startswith(b'%PDF') and sha(drawing) == DRAWING_SHA, 'Connector drawing changed; review dimensions'
    design = cad / 'usb-port-study.scad'
    meshes = {}
    for name in ['base', 'pcb', 'connector', 'components', 'copper', 'fasteners', 'body', 'cover',
                 'bezel_magnets', 'cover_magnets', 'plug', 'fingers', 'cable']:
        path = out / f'{name}.stl'
        result = subprocess.run([OPENSCAD, '-o', str(path), '-D', f'part="{name}"', str(design)],
                                check=True, capture_output=True, text=True)
        assert 'WARNING' not in result.stderr, result.stderr
        mesh = trimesh.load_mesh(path)
        assert mesh.is_volume, name
        meshes[name] = mesh
    assert len(meshes['body'].split()) == 1, 'Surround must join the body'
    assert len(meshes['cover'].split()) == 1, 'Lid must be a single printable part'
    fixed = {}
    previous = ROOT / 'build/cad-adjustable-layout'
    for name in ['tray', 'camera_mount', 'main_fasteners', 'camera_fasteners', 'battery', 'support', 'guards', 'band', 'driver', 'charge', 'cavity']:
        fixed[name] = trimesh.load_mesh(previous / f'{name}.stl')
    for name, filename in [('motor', 'motor-installed.stl'), ('servo', 'servo-case-installed.stl'),
                           ('camera', 'xiao-sense-2023-installed.stl'), ('power', 'buck-installed.stl')]:
        fixed[name] = trimesh.load_mesh(cad / 'components' / filename)
    # Vendor STEP has open surfaces: check its complete bounding box instead.
    fixed['camera_envelope'] = box(fixed['camera'].extents, fixed['camera'].bounds.mean(axis=0))
    for name in ['frame', 'cover']:
        fixed['zcar_' + name] = trimesh.load_mesh(ROOT / f'build/cad-layout/{name}.stl')

    external = ['pcb', 'connector', 'components', 'copper', 'fasteners', 'plug', 'fingers', 'cable']
    check_names = [n for n in fixed if n not in ['cavity', 'camera']]
    checks = {name: {other: overlap(meshes[name], fixed[other]) for other in check_names}
              for name in external}
    # New base intentionally replaces/overlaps the previous base.
    checks['base'] = {n: overlap(meshes['base'], fixed[n]) for n in check_names}
    body_checks = {n: overlap(meshes[n], meshes['body']) for n in ['base'] + external}
    internal = {n: overlap(meshes[n], meshes['base']) for n in external}
    # Screws pass through clearance holes and meet PCB/base/nut faces only.
    for name, values in checks.items():
        assert max(values.values()) < 1e-3, (name, values)
    assert max(body_checks.values()) < 1e-3, body_checks
    assert max(internal.values()) < 1e-3, internal
    inside = {n: float(trimesh.boolean.difference([meshes[n], fixed['cavity']], engine='manifold').volume)
              for n in ['base', 'pcb', 'connector', 'components', 'fasteners']}
    # Connector/head may occupy the removed body-wall region, checked above.
    assert max(inside[n] for n in ['base', 'pcb', 'components']) < 1e-3, inside
    closed_cover = {n: overlap(meshes[n], meshes['cover']) for n in
                    ['base', 'pcb', 'connector', 'components', 'copper', 'fasteners', 'body', 'bezel_magnets', 'cover_magnets']}
    assert max(closed_cover.values()) < 1e-3, closed_cover

    # Four separate physical discs; pockets are clear, not interference fits.
    for name in ['bezel_magnets', 'cover_magnets']:
        assert len(meshes[name].split()) == 2, name
        for other in ['body', 'cover', 'base', 'pcb', 'connector', 'copper', 'components', 'fasteners']:
            assert overlap(meshes[name], meshes[other]) < 1e-3, (name, other)
    # Rigid straight pull: discrete samples, not a contact/force simulation.
    lid_motion = {}
    lid_obstacles = ['body', 'base', 'pcb', 'connector', 'components', 'copper', 'fasteners', 'bezel_magnets']
    for distance in np.arange(0, 8.01, .25):
        moved = meshes['cover'].copy()
        moved.apply_translation([distance, 0, 0])
        values = {n: overlap(moved, meshes[n]) for n in lid_obstacles}
        assert max(values.values()) < 1e-3, (distance, values)
        lid_motion[str(float(distance))] = values
    # Hard stops resist excess inward travel and rim arrests lateral travel.
    cap_controls = {}
    for label, movement in [('inward_stop', [-.1, 0, 0]), ('lateral_guide', [0, .4, 0])]:
        moved = meshes['cover'].copy()
        moved.apply_translation(movement)
        cap_controls[label] = overlap(moved, meshes['body'])
        assert cap_controls[label] > .01, (label, cap_controls[label])

    # A straight vertical body lift catches the receptacle. First raise by
    # 2 mm to clear the camera screw head, shift 1 mm toward the USB side,
    # then lift away. This checks neutral stage positions and rigid solids.
    removal_parts = {n: meshes[n] for n in ['base', 'pcb', 'connector', 'components', 'copper', 'fasteners']}
    removal_parts.update({n: fixed[n] for n in check_names})
    body_removal = {}
    poses = [[0, 0, float(z)] for z in np.arange(0, 2.01, .25)]
    poses += [[float(x), 0, 2] for x in np.arange(.25, 1.01, .25)]
    poses += [[1, 0, float(z)] for z in range(3, 66)]
    moving_body = trimesh.util.concatenate([meshes[n] for n in ['body', 'cover', 'bezel_magnets', 'cover_magnets']])
    for xyz in poses:
        moved = moving_body.copy()
        moved.apply_translation(xyz)
        values = {n: overlap(moved, m) for n, m in removal_parts.items()}
        assert max(values.values()) < 1e-3, ('body_removal', xyz, values)
        body_removal[str(xyz)] = values
    incorrect_lift = meshes['body'].copy()
    incorrect_lift.apply_translation([0, 0, 7])
    vertical_lift_collision = overlap(incorrect_lift, meshes['connector'])
    assert vertical_lift_collision > 1, vertical_lift_collision

    # Verify the added assembly does not block the full straight battery exit.
    battery_sweep = box(fixed['battery'].extents + [50, 0, 0], fixed['battery'].bounds.mean(axis=0) - [25, 0, 0])
    swap = {n: overlap(battery_sweep, meshes[n]) for n in ['base', 'pcb', 'connector', 'components', 'fasteners']}
    assert max(swap.values()) < 1e-3, swap
    # Cable insertion is straight outward; cover must be removed first.
    insertion_sweep = box([44, 18, 9], [81.1, 59, 24.88])
    insertion = {n: overlap(insertion_sweep, meshes[n]) for n in ['base', 'pcb', 'connector', 'components', 'fasteners', 'body']}
    assert max(insertion.values()) < 1e-3, insertion
    cover_block = overlap(meshes['cover'], meshes['plug'])
    assert cover_block > 1, 'Closed cover must obstruct the assumed plug'

    # Added fixed geometry versus all 12 discrete existing stage positions.
    slides = {}
    obstacle = trimesh.util.concatenate([meshes[n] for n in ['pcb', 'connector', 'components', 'copper', 'fasteners']])
    # Base tabs included as the whole new base; original stage/base contacts are zero-volume.
    for mm in range(-3, 9):
        values = {}
        for n in ['tray', 'main_fasteners', 'camera_mount', 'camera_fasteners', 'driver', 'charge', 'camera_envelope', 'power']:
            moved = fixed[n].copy()
            moved.apply_translation([0, mm, 0])
            values[n] = overlap(moved, obstacle) + overlap(moved, meshes['base'])
        slides[str(mm)] = values
        assert max(values.values()) < 1e-3, (mm, values)

    reference = trimesh.load_mesh(cad / 'components/zcar-without-reference-actuators.stl')
    # Report conservative boxes for other source parts: open source surfaces
    # are not silently repaired or treated as solid intersection evidence.
    parts = reference.split(only_watertight=False, repair=False)
    reference_hits = {}
    for n in external:
        hits = {}
        for i in broad_phase(parts, meshes[n].bounds, set()):
            candidate = parts[i]
            volume = overlap(meshes[n], box(candidate.extents + .002, candidate.bounds.mean(axis=0)))
            if volume > 1e-3:
                hits[str(i)] = {'box_overlap_mm3': volume, 'source_bounds_mm': candidate.bounds.tolist()}
        reference_hits[n] = hits

    rows = [('reference', 'Механика zcar: статическая модель', reference, [.57, .64, .69], False)]
    labels = {'base': 'Основание с опорами USB', 'pcb': 'Плата 7,6×26×1: разведена, не изготовлена',
              'connector': 'USB4105-GF-A: габарит и монтажные выводы', 'components': 'Резисторы и резерв пайки жгута',
              'copper': 'Площадки из KiCad; дорожки показаны в редакторе PCB',
              'fasteners': 'Резерв двух винтов M2 с гайками', 'body': 'Условный кузов с окном',
              'cover': 'Съёмная крышка: бортик, упоры и выступ для пальца',
              'bezel_magnets': 'Два магнита Ø3×1 в рамке — предложение',
              'cover_magnets': 'Два магнита Ø3×1 в крышке — предложение',
              'plug': 'Штекер: условный корпус 18×9×24', 'fingers': 'Резерв для пальцев — проверить руками',
              'cable': 'Прямой участок кабеля Ø6 — условный'}
    for n, mesh in meshes.items():
        shown = mesh.copy()
        color = [.9, .15, .15] if n in ['plug', 'fingers', 'cable'] else [.2, .6, .45]
        if n == 'connector': color = [.75, .75, .8]
        if n == 'copper': color = [.9, .65, .1]
        if n in ['body', 'cover']: color = [.2, .35, .7]
        rows.append((n, labels[n], shown, color, n in ['body', 'fingers', 'plug', 'cable']))
    fixed_labels = {'tray': 'Подвижный поддон', 'camera_mount': 'Крепление камеры',
                    'main_fasteners': 'Крепёж поддона — резерв', 'camera_fasteners': 'Крепёж камеры — резерв',
                    'battery': 'Аккумулятор LW — габарит продавца', 'support': 'Опора аккумулятора',
                    'guards': 'Упоры аккумулятора', 'band': 'Резерв ремешка', 'driver': 'Драйвер — прежний резерв',
                    'charge': 'Зарядная плата — прежний резерв', 'motor': 'F130 — модель по чертежу',
                    'servo': 'SG90 — габарит по карточке', 'camera': 'XIAO — STEP 2023', 'power': 'Waveshare — низкий габарит'}
    for n, m in fixed.items():
        if n in ['cavity', 'camera_envelope', 'zcar_frame', 'zcar_cover']: continue
        rows.append((n, fixed_labels[n], m,
                     [.95, .45, .1] if n == 'battery' else [.6, .5, .65], False))
    scene = []
    for name, label, mesh, color, wire in rows:
        if wire:
            edges = mesh.face_adjacency_edges[mesh.face_adjacency_angles > .15]
            pos = mesh.vertices[edges].reshape(-1, 3)
            norm = np.tile([0, 0, 1], (len(pos), 1))
        else:
            pos = mesh.triangles.reshape(-1, 3)
            norm = np.repeat(mesh.face_normals, 3, axis=0)
        packed = np.hstack([pos, norm]).astype('<f4')
        scene.append(dict(id=name, label=label, color=color, level=0, wire=wire, slide=None,
                          count=len(pos), data=base64.b64encode(packed.tobytes()).decode()))
    template = (ROOT / 'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    template = template.replace('3D-компоновка 0.1', 'Зарядный порт 0.3').replace('assembly.scad', 'usb-port-study.scad').replace('assembly.md', 'usb-port-study.md')
    start = template.index('<p class="note">'); end = template.index('</p>', start)
    template = template[:start] + '<p class="note">Боковой USB-C на шасси, магнитная крышка на условном печатном кузове. Плата разведена в KiCad. Крышка снимается за нижний выступ. Геометрия проверена; удержание, кабели и печать ещё не испытаны.' + template[end:]
    template = template.replace('<div id="parts">', '<button id="port-closed">Порт скрыт</button><button id="port-open">Подключение кабеля</button><div id="parts">')
    template = template.replace('__ADJUSTMENT_DATA__', 'null').replace('__SCENE_DATA__', json.dumps(scene, ensure_ascii=False))
    mode_js = """
document.querySelector('#explode').closest('label').hidden=true;
function portMode(charging) {
  for(const p of parts) {
    if(['cover','cover_magnets'].includes(p.id)) p.visible=!charging;
    if(['plug','cable'].includes(p.id)) p.visible=charging;
    if(p.id==='fingers') p.visible=false;
    document.querySelector(`input[data-part="${p.id}"]`).checked=p.visible;
  }
  render();
}
document.querySelector('#port-closed').onclick=()=>portMode(false);
document.querySelector('#port-open').onclick=()=>portMode(true);
portMode(true);
"""
    template = template.replace('</script></html>', mode_js + '</script></html>')
    (cad / 'usb-port-study-viewer.html').write_text(template, encoding='utf-8', newline='\n')
    for n in ['base', 'pcb', 'cover']:
        export_mesh(meshes[n], cad / f'usb-port-{n}.stl')
    # Print the lid on its flat outer face, pocket openings upward. Only
    # rigid transforms are applied; retain assembly coordinates separately.
    printed = meshes['cover'].copy()
    printed.apply_transform(trimesh.transformations.rotation_matrix(np.pi/2, [0, 1, 0]))
    printed.apply_translation(-printed.bounds[0])
    assert np.allclose(printed.bounds[0], 0, atol=1e-5)
    assert len(printed.split()) == 1 and printed.is_volume
    export_mesh(printed, cad / 'usb-port-cover-print.stl')
    for n in ['bezel_coupon', 'magnet_coupon']:
        path = out / f'{n}.stl'
        subprocess.run([OPENSCAD, '-o', str(path), '-D', f'part="{n}"', str(design)], check=True, capture_output=True)
        item = trimesh.load_mesh(path)
        assert item.is_volume and len(item.split()) == 1, n
        if n == 'bezel_coupon':
            # Front magnet pockets upward for the loose fit/retention coupon.
            item.apply_transform(trimesh.transformations.rotation_matrix(-np.pi/2, [0, 1, 0]))
            item.apply_translation(-item.bounds[0])
        export_mesh(item, cad / f'usb-port-{n.replace("_", "-")}-print.stl')
    preview = ROOT / 'docs/evidence/usb-port-overview.png'
    subprocess.run([OPENSCAD, '--preview', '--imgsize=1400,1000', '--autocenter', '--viewall',
                    '-o', str(preview), str(design)], check=True, capture_output=True)
    closed_preview = ROOT / 'docs/evidence/usb-port-closed.png'
    subprocess.run([OPENSCAD, '--preview', '--imgsize=1400,1000', '--autocenter', '--viewall',
                    '-D', 'part="assembly_closed"', '-o', str(closed_preview), str(design)], check=True, capture_output=True)
    lid_preview = ROOT / 'docs/evidence/usb-port-lid-detail.png'
    subprocess.run([OPENSCAD, '--preview', '--imgsize=1200,800', '--autocenter', '--viewall',
                    '--camera=0,0,0,60,0,145,100', '-D', 'part="lid_detail"',
                    '-o', str(lid_preview), str(design)], check=True, capture_output=True)
    report = dict(version='0.3', mount_contract='USB-MOUNT-01 v0.4', units='mm', design_sha256=sha(design),
                  builder_sha256=sha(Path(__file__)),
                  pad_geometry_sha256=sha(cad/'components/usb-port-pcb.scad'),
                  connector_source=DRAWING_URL,
                  connector_drawing_revision='B4 2023-12-18',
                  connector_drawing_sha256=sha(drawing),
                  bounds_mm={n: m.bounds.tolist() for n, m in meshes.items()},
                  fixed_checks_mm3=checks, body_overlap_mm3=body_checks, base_overlap_mm3=internal,
                  outside_cavity_mm3=inside, closed_cover_hardware_overlap_mm3=closed_cover, battery_exit_sweep_mm3=swap,
                  insertion_sweep_mm3=insertion, closed_cover_positive_control_mm3=cover_block,
                  discrete_slide_checks_mm3=slides, other_reference_box_hits=reference_hits,
                  lid_pull_samples_mm3=lid_motion, lid_contact_positive_controls_mm3=cap_controls,
                  body_removal_samples_mm3=body_removal,
                  incorrect_vertical_lift_positive_control_mm3=vertical_lift_collision,
                  proposed_lid=dict(pocket_diameter_mm=3.3, magnet_diameter_mm=3, magnet_thickness_mm=1,
                                    magnet_count=4, magnet_face_gap_mm=.7, radial_guide_gap_mm=.3,
                                    retention_force_measured=False, printed=False),
                  single_piece_body_and_lid=True,
                  limitations=['No physical cable/print/strain tests', 'PCB routed, no assembly or thermal measurements',
                               'Magnetic lid is a proposal, no chosen magnet SKU, adhesive or retention measurement',
                               'Bezel integrated with hypothetical printed body, no arbitrary purchased-body compatibility',
                               'Body removal sampled with neutral stages only; real body mounts, clearances and dynamic suspension remain unverified',
                               'No wires or suspension travel',
                               'Old SG90 collision and OV3660 revision uncertainty remain open'],
                  outputs_sha256={p.name: sha(p) for p in [cad / 'usb-port-base.stl', cad / 'usb-port-pcb.stl', cad / 'usb-port-cover.stl',
                                  cad / 'usb-port-cover-print.stl', cad / 'usb-port-bezel-coupon-print.stl', cad / 'usb-port-magnet-coupon-print.stl',
                                  cad / 'usb-port-study-viewer.html', preview, closed_preview, lid_preview]})
    (ROOT / 'docs/evidence/usb-port-study.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'solid_checks': 'passed', 'reference_box_hits': reference_hits, 'closed_cover_overlap_mm3': cover_block}, ensure_ascii=False))


if __name__ == '__main__':
    main()
