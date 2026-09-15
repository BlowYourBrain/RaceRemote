"""Detailed packaging study. No hardware qualification or electrical pin assignment.

Uses pinned FreeCAD SG90 STEP, existing vendor XIAO STEP, dimensioned Waveshare
PCB plus explicitly estimated -M terminals, and independent closed-volume checks.
"""
import base64
import hashlib
import json
from pathlib import Path
import subprocess
from itertools import combinations

import numpy as np
import trimesh
from OCP.STEPControl import STEPControl_Reader
from OCP.IFSelect import IFSelect_RetDone
from OCP.BRepMesh import BRepMesh_IncrementalMesh
from OCP.StlAPI import StlAPI_Writer

from layout_zcar_battery import box

ROOT = Path(__file__).resolve().parents[1]
CAD = ROOT / 'cad/packaging-v05'
OUT = ROOT / 'build/packaging-v05'
SCAD = 'C:/Program Files/OpenSCAD/openscad.com'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def intersect(a, b):
    if np.any(a.bounds[1] <= b.bounds[0]) or np.any(b.bounds[1] <= a.bounds[0]):
        return 0.0
    m = trimesh.boolean.intersection([a, b], engine='manifold')
    return 0.0 if m.is_empty else float(m.volume)


def shifted(m, y):
    n = m.copy()
    n.apply_translation([0, y, 0])
    return n


def mesh(path):
    return trimesh.load_mesh(path)


def scad_mesh(name):
    path = CAD / f'{name}.stl'
    run = subprocess.run([SCAD, '-o', str(path), '-D', f'part="{name}"',
                          str(CAD / 'layout.scad')], capture_output=True, text=True, check=True)
    if 'WARNING' in run.stderr or 'ERROR' in run.stderr:
        raise RuntimeError(run.stderr)
    m = mesh(path)
    if not m.is_volume:
        raise ValueError(f'Non-solid generated model: {name}')
    return m


def tube(points, radius):
    # Swept corridor with rounded elbows, an allocation rather than a cable specification.
    pieces = []
    for a, b in zip(points, points[1:]):
        pieces.append(trimesh.creation.cylinder(radius=radius, segment=[a, b], sections=12))
    for p in points:
        sphere = trimesh.creation.icosphere(subdivisions=1, radius=radius)
        sphere.apply_translation(p)
        pieces.append(sphere)
    return trimesh.boolean.union(pieces, engine='manifold')


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    reader = STEPControl_Reader()
    if reader.ReadFile(str(CAD / 'sg90-library.step')) != IFSelect_RetDone:
        raise RuntimeError('Cannot import SG90 STEP')
    reader.TransferRoots()
    shape = reader.OneShape()
    BRepMesh_IncrementalMesh(shape, .03, False, .15, True).Perform()
    writer = StlAPI_Writer()
    writer.ASCIIMode = False
    writer.Write(shape, str(OUT / 'sg90-source.stl'))
    servo = mesh(OUT / 'sg90-source.stl')
    source_bounds = servo.bounds.copy()
    # Preserve output shaft centre X=24.75/Z=8.1 and horn face Y=101.275.
    # No scaling to make the model fit. Reference thickness is 11.8, supplier says 12.2.
    transform = np.eye(4)
    transform[:3, :3] = [[0, -1, 0], [0, 0, 1], [-1, 0, 0]]
    transform[:3, 3] = [41.35, 71.375, 8.1]
    servo.apply_transform(transform)
    servo.export(CAD / 'servo-installed.stl')
    generated = {name: scad_mesh(name) for name in
                 ['buck', 'buck_pcb', 'buck_components', 'buck_terminals', 'buck_plug',
                  'tray', 'pads', 'driver', 'charge', 'antenna', 'antenna_support', 'service_usb', 'camera_support']}
    # Pinned prior-stage exports make this study reproducible without an ignored build cache.
    base = CAD / 'inputs'
    base_meshes = {n: mesh(base / f'adjustable-{n}.stl') for n in
                   ['camera_mount', 'main_fasteners', 'camera_fasteners',
                    'battery', 'support', 'guards', 'band', 'cavity']}
    # The old bracket is translated with the new camera default, without altering its size.
    camera = mesh(ROOT / 'cad/components/xiao-sense-2023-installed.stl')
    camera_box = box(camera.extents, camera.bounds.mean(axis=0))
    frame = mesh(base / 'frame.stl')
    cover = mesh(base / 'cover.stl')
    motor = mesh(ROOT / 'cad/components/motor-installed.stl')
    # Explicit local relief proposal at the four body-width stops; original frame is preserved.
    # New passage 23.4 mm provides 0.2 mm/side for a centred 23 mm candidate body.
    cutters = [box([.302, length, 10.6], [x, y + length/2, 7.2])
               for x in [18.30, 41.40] for y, length in [(71.2, 2.3), (89.5, 3.0)]]
    relief = trimesh.boolean.union(cutters, engine='manifold')
    revised_frame = trimesh.boolean.difference([frame, relief], engine='manifold')
    assert revised_frame.is_volume
    revised_frame.export(CAD / 'frame-relief-proposal.stl')
    removed = trimesh.boolean.intersection([frame, relief], engine='manifold')
    removed.export(CAD / 'frame-removed-material.stl')
    original_parts = mesh(ROOT / 'cad/reference/zcar-aligned.stl').split(only_watertight=False, repair=False)
    assert np.allclose(original_parts[0].bounds, frame.bounds, atol=.001)
    reference = trimesh.util.concatenate([p for i,p in enumerate(original_parts) if i not in {0,176,189,217,218}])
    reference.export(CAD / 'mechanics-without-frame.stl')
    usb = {n: mesh(base / f'usb-{n}.stl') for n in ['base','pcb','connector','components','cover','body']}
    assert np.allclose(generated['camera_support'].bounds, shifted(base_meshes['camera_mount'],3).bounds, atol=.001), 'SCAD/Python camera mount transforms differ'

    # Nominal routes only; exact electrical terminals/connectors are still unassigned.
    routes = {
        'wire-power': (1.0, [[50.5,54,16],[51.5,40,16],[51.5,30,26],[45,27,28],
                            [0,27,28],[0,75,35],[7,82,35],[7,79,35]]),
        'wire-servo': (.9, [[35, 68, 38.6], [44, 68, 39.5], [44, 92, 34],
                           [45, 95, 19], [44, 89, 16], [43, 78, 14]]),
        'wire-antenna': (.65, [[37, 86, 34], [41, 86, 37], [47.5, 83, 42], [47.5, 63, 42]]),
        'wire-motor': (.9, [[35,49,38.6],[44,44,35],[44,32,28],[41,29,25],[39,24,22]]),
        'wire-control': (.8, [[37,85,35],[40.5,82,37],[41,79,40],[36,74,39]]),
    }
    wires = {}
    wire_results = {}
    for name, (radius, points) in routes.items():
        m = tube(points, radius)
        m.export(CAD / f'{name}.stl')
        wires[name] = m
        wire_results[name] = {
            'radius_mm': radius, 'points_mm': points,
            'polyline_length_mm': float(np.linalg.norm(np.diff(points, axis=0), axis=1).sum()),
            'frame_overlap_mm3': intersect(m, frame),
            'cover_overlap_mm3': intersect(m, cover),
            'battery_overlap_mm3': intersect(m, base_meshes['battery']),
            'motor_overlap_mm3': intersect(m, motor),
            'port_base_overlap_mm3': intersect(m, usb['base']),
            'outside_hypothetical_body_mm3': float(trimesh.boolean.difference([m, base_meshes['cavity']], engine='manifold').volume),
            'note': 'Nominal corridor only; endpoints unassigned, not a wire cutting length or bend-radius qualification',
        }

    # High -M terminals in the OLD pose demonstrate why stacked charger space must change.
    old_m = generated['buck'].copy()
    mat = np.eye(4)
    mat[:3, :3] = [[0, 1, 0], [-1, 0, 0], [0, 0, 1]]
    mat[:3, 3] = [-38.25, 79.75, -2]
    old_m.apply_transform(mat)
    old_charge = mesh(base / 'adjustable-charge.stl')
    servo_interference = trimesh.boolean.intersection([servo, frame], engine='manifold')
    if not servo_interference.is_empty:
        servo_interference.export(CAD / 'servo-frame-interference.stl')

    # New independent 12x12 sweep. Covers packaging groups, not moving wheels/linkages.
    group = {n: generated[n] for n in ['buck', 'buck_plug', 'tray', 'pads', 'driver', 'charge']}
    # Contacting plug/pins and dielectric support pads are deliberate interfaces.
    internal_overlaps = {f'{a}/{b}': intersect(group[a], group[b])
                         for a,b in combinations(['buck','tray','driver','charge'],2)}
    fixed = {'frame': revised_frame, 'cover': cover, 'motor': motor, 'servo': servo,
             'battery': base_meshes['battery'], 'base': usb['base'],
             'antenna': generated['antenna'], 'antenna_support': generated['antenna_support'],
             'usb_pcb':usb['pcb'], 'usb_connector':usb['connector']}
    poses = []
    fixed_body = {n: float(trimesh.boolean.difference([generated[n], base_meshes['cavity']], engine='manifold').volume)
                  for n in ['antenna', 'antenna_support']}
    for e in range(-3, 9):
        moving = {n: shifted(m, e) for n, m in group.items()}
        electronics_errors = {f'{n}/{f}': intersect(m, fm) for n, m in moving.items() for f, fm in fixed.items()}
        electronics_body = {n: float(trimesh.boolean.difference([m, base_meshes['cavity']], engine='manifold').volume)
                            for n, m in moving.items()}
        for c in range(-3, 9):
            cb = shifted(camera_box, c)
            cm = shifted(base_meshes['camera_mount'], c)
            # Pads intentionally contact boards/tray. Camera stage and fixed base have designed contacts.
            errors = dict(electronics_errors, **internal_overlaps)
            for n, m in moving.items():
                errors[f'{n}/camera'] = intersect(m, cb)
                errors[f'{n}/camera_mount'] = intersect(m, cm)
            for n in ['frame', 'cover', 'motor', 'servo', 'battery', 'antenna', 'antenna_support', 'usb_pcb', 'usb_connector']:
                errors[f'camera/{n}'] = intersect(cb, fixed[n])
                errors[f'camera_mount/{n}'] = intersect(cm, fixed[n])
            outside = dict(electronics_body, **fixed_body)
            outside['camera'] = float(trimesh.boolean.difference([cb, base_meshes['cavity']], engine='manifold').volume)
            outside['camera_mount'] = float(trimesh.boolean.difference([cm, base_meshes['cavity']], engine='manifold').volume)
            poses.append({'electronics_mm': e, 'camera_mm': c,
                          'mechanical_clear': max(errors.values()) < .001,
                          'hypothetical_body_clear': max(outside.values()) < .001,
                          'overlaps_mm3': {k: round(v, 5) for k, v in errors.items() if v > .001},
                          'outside_body_mm3': {k: round(v, 5) for k, v in outside.items() if v > .001}})
    nominal = next(p for p in poses if p['electronics_mm'] == 0 and p['camera_mm'] == 3)
    feasible = [p for p in poses if p['mechanical_clear'] and p['hypothetical_body_clear']]

    rows = []
    def row(name, label, m, color, slide=None, wire=False, hidden=False):
        if wire:
            edges = m.face_adjacency_edges[m.face_adjacency_angles > .15]
            pos = m.vertices[edges].reshape(-1, 3)
            norm = np.tile([0, 0, 1], (len(pos), 1))
        else:
            pos = m.triangles.reshape(-1, 3)
            norm = np.repeat(m.face_normals, 3, axis=0)
        rows.append(dict(id=name, label=label, color=color, level=0, wire=wire,
                         hidden=hidden, slide=slide, count=len(pos),
                         data=base64.b64encode(np.hstack([pos, norm]).astype('<f4').tobytes()).decode()))
    row('reference', 'Механика zcar; исходный привод', reference, [.62, .66, .7])
    row('frame', 'Рама: предложены выемки под SG90', revised_frame, [.52, .63, .7])
    row('removed', 'Материал для удаления из старой рамы', removed, [1,.12,.1], hidden=True)
    row('motor', 'F130: корпус и вал по чертежу', motor, [.67, .69, .72])
    row('servo', 'SG90: STEP FreeCAD, справочный образец', servo, [.15, .37, .8])
    if not servo_interference.is_empty:
        row('servo_clash', 'Красное: SG90 пересекает исходную раму', servo_interference, [1, .1, .1], hidden=True)
    row('camera', 'XIAO Sense: STEP Seeed 2023', camera, [.1, .55, .3], 'camera')
    row('camera_mount', 'Регулируемая опора камеры', base_meshes['camera_mount'], [.4, .68, .6], 'camera')
    for name, label, color in [('buck_pcb', 'Waveshare -M: PCB по чертежу', [.12, .28, .22]),
                               ('buck_components', 'Waveshare: компоненты по фото', [.55, .57, .59]),
                               ('buck_terminals', 'Клеммы и штырьки -M: оценка размеров', [.4, .7, .2]),
                               ('tray', 'Удлинённый регулируемый поддон', [.35, .65, .6]),
                               ('pads', 'Диэлектрические опоры плат — предложение', [.7, .75, .7])]:
        row(name, label, generated[name], color, 'electronics')
    row('buck_plug', 'Резерв ответного разъёма -M', generated['buck_plug'], [.9, .55, .15], 'electronics', True)
    row('driver', 'Место драйвера: плата пока не разведена', generated['driver'], [.65, .2, .65], 'electronics', True)
    row('charge', 'Место зарядки: габарит пока не подтверждён', generated['charge'], [.45, .25, .7], 'electronics', True)
    row('antenna', 'Место FPC-антенны, размер уточнить при получении', generated['antenna'], [.1, .1, .12])
    row('antenna_support', 'Опора антенны — предложение', generated['antenna_support'], [.4,.68,.6])
    # Service reserve is already generated at c=3; de-offset before assigning camera slide.
    row('service_usb', 'Доступ к USB XIAO при снятом кузове — оценка', shifted(generated['service_usb'], -3), [.7, .4, .1], 'camera', True, True)
    for name, color in [('wire-power', [.9, .2, .1]), ('wire-servo', [.1, .3, .8]), ('wire-antenna', [.12, .12, .12]), ('wire-motor',[.8,.45,.1]), ('wire-control',[.5,.25,.75])]:
        row(name, name.replace('wire-', 'Трасса: '), wires[name], color)
    row('port_base','Основание с креплением зарядного USB-C',usb['base'],[.45,.65,.58])
    for name in ['pcb','connector','components','cover']:
        row('usb_'+name,'Зарядный USB-C: '+name,usb[name],[.2,.4,.65] if name=='cover' else [.5,.6,.5])
    for name in ['battery', 'support', 'guards', 'band', 'main_fasteners', 'camera_fasteners']:
        color = [1, .5, .1] if name == 'battery' else [.45, .65, .58]
        row(name, {'carrier_installed': 'Основание с пазами', 'battery': 'LW: макет 49×18×15',
                   'support': 'Опора батареи', 'guards': 'Упоры батареи', 'band': 'Место ремешка',
                   'main_fasteners': 'Крепёж поддона — резерв', 'camera_fasteners': 'Крепёж камеры — резерв'}[name],
            base_meshes[name], color, 'electronics' if name == 'main_fasteners' else 'camera' if name == 'camera_fasteners' else None)
    row('body', 'Контур условного кузова с крышкой USB', usb['body'], [.18, .3, .5], wire=True)
    template = (ROOT / 'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    template = template.replace('3D-компоновка 0.1', 'Детальная компоновка 0.5')
    template = template.replace('assembly.scad', 'layout.scad').replace('assembly.md', 'README.md').replace('reference/NOTICE.md', '../reference/NOTICE.md')
    template = template.replace('Габаритная проработка, не готовая машинка. Цветные блоки электроники — резерв места. Их крепления, провода, кузов и ход подвески ещё не проверены.',
                                'XIAO и SG90 — импортированные STEP. Waveshare — PCB по чертежу, разъёмы оценочные. Зарядка и драйвер — резерв. В раме предложены выемки; красным показано старое пересечение.')
    template = template.replace('slides={electronics:0,camera:0}', 'slides={electronics:0,camera:3}')
    template = template.replace('id="camera-value">0', 'id="camera-value">3').replace('id="camera-slide" type="range" min="-3" max="8" step="1" value="0"', 'id="camera-slide" type="range" min="-3" max="8" step="1" value="3"')
    template = template.replace('part.visible=true;', 'part.visible=!part.hidden;').replace('input.checked=true;', 'input.checked=!part.hidden;')
    template = template.replace("if(!part.visible)continue;", "if(!part.visible || (part.id.startsWith('wire-') && (slides.electronics!==0 || slides.camera!==3)))continue;")
    template = template.replace('В этом положении пересечений макетов не найдено; они внутри условного кузова.', 'Электроника проходит геометрические проверки. Выемки рамы под SG90 — предложение; физической проверки нет.')
    template = template.replace('Провода, допуски, реальный кузов и ход подвески не проверены.', 'Трассы проводов показаны только при 0/+3 мм; при регулировке скрыты. Допуски и ход подвески не проверены.')
    template = template.replace('__ADJUSTMENT_DATA__', json.dumps({'poses': poses})).replace('__SCENE_DATA__', json.dumps(rows, ensure_ascii=False))
    template = template.replace('</script></html>', "document.querySelector('#explode').closest('label').hidden=true;</script></html>")
    (CAD / 'viewer.html').write_text(template, encoding='utf-8', newline='\n')

    previews = {}
    for name, extra in [('overview', []), ('top', ['--camera=24.75,58,25,0,0,0,190']),
                        ('side', ['--camera=24.75,58,25,90,0,90,190'])]:
        dest = ROOT / f'docs/evidence/packaging-v05-{name}.png'
        run = subprocess.run([SCAD, '--preview', '--imgsize=1400,1000', '--viewall', '--autocenter',
                              *extra, '-o', str(dest), str(CAD / 'layout.scad')], capture_output=True, text=True, check=True)
        if 'WARNING' in run.stderr or 'ERROR' in run.stderr:
            raise RuntimeError(run.stderr)
        previews[dest.relative_to(ROOT).as_posix()] = sha(dest)
    report = {
        'version': '0.5', 'date': '2026-09-15', 'physical_fit_verified': False,
        'servo_source': {'url': 'https://github.com/FreeCAD/FreeCAD-library/tree/6f071a4bab37fff7784092548e6dbff21499b751/Electrical%20Parts/Servos/SG-90',
                         'sha256': sha(CAD / 'sg90-library.step'), 'native_bounds_mm': source_bounds.tolist(),
                         'transform': transform.tolist(), 'scaled': False, 'sample_match_confirmed': False},
        'buck_sources': ['https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-size.jpg',
                        'https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-details-1.jpg'],
        'buck_terminal_dimensions_estimated': True,
        'old_stacked_charge_with_M_module_overlap_mm3': intersect(old_m, old_charge),
        'servo_frame_overlap_mm3': float(servo_interference.volume),
        'servo_revised_frame_overlap_mm3': intersect(servo, revised_frame),
        'frame_removed_volume_mm3': float(removed.volume),
        'frame_connected_components_before':len(frame.split(only_watertight=False,repair=False)),
        'frame_connected_components_after':len(revised_frame.split(only_watertight=False,repair=False)),
        'servo_cover_overlap_mm3': intersect(servo, cover),
        'internal_electronics_overlaps_mm3': internal_overlaps,
        'fixed_antenna_outside_body_mm3': fixed_body,
        'nominal_pose': nominal, 'feasible_pose_count': len(feasible), 'poses': poses,
        'wire_corridors': wire_results,
        'bounds_mm': {n: m.bounds.tolist() for n, m in generated.items()},
        'sources_and_artifacts_sha256': {p.relative_to(ROOT).as_posix(): sha(p) for p in sorted(CAD.rglob('*')) if p.is_file()},
        'previews_sha256': previews,
        'limitations': ['XIAO vendor STEP 2023 not confirmed as OV3660 revision',
                        'Servo library model is not the ordered supplier model; source 11.8mm thickness versus seller 12.2mm',
                        'Static electronics sweep, no wheel/linkage motion or continuous travel qualification',
                        'Waveshare -M terminal height, pin tails, plugs and antenna dimensions are estimates',
                        'Existing camera support contact and printed tray cantilever are not validated',
                        'Driver and charger are unqualified volume allocations, not completed populated boards',
                        'Wires are geometric corridors at nominal pose only, not wiring instructions',
                        'No received parts, printed assembly, thermal or radio evidence'],
    }
    (ROOT / 'docs/evidence/packaging-v05.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8', newline='\n')
    print(json.dumps({'nominal': nominal, 'feasible_poses': len(feasible),
                      'servo_frame_mm3': report['servo_frame_overlap_mm3'],
                      'old_stack_mm3': report['old_stacked_charge_with_M_module_overlap_mm3'],
                      'wire_corridors': wire_results}, indent=2), flush=True)


if __name__ == '__main__':
    main()
