"""Package current electronics holders for later fit printing, without printer actions."""
import json
from itertools import combinations
from pathlib import Path
import xml.etree.ElementTree as ET
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED
import numpy as np
import trimesh
from matplotlib.collections import PolyCollection
from build_motor_print_kit import CORE, CONTENT, REL, digest, xml, plt

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'cad/current-fit-kit'
ASSEMBLY = ROOT / 'docs/evidence/servo-lacing-v01.json'
SPECS = [
    ('tray', 'cad/direct-logic-harness/tray.stl', False, [20, 30]),
    ('xiao_cradle', 'cad/xiao-power-harness/cradle.stl', True, [90, 30]),
    ('xiao_cap', 'cad/harness-guides/cap.stl', True, [90, 75]),
    ('pcb_clamp', 'cad/motor-carrier-mount/clamp.stl', False, [135, 75]),
    ('pcb_gauge', 'cad/motor-carrier-mount/pcb_gauge_print.stl', False, [160, 30]),
]


def main():
    OUT.mkdir(exist_ok=True)
    assembly = json.loads(ASSEMBLY.read_text())
    assert assembly['service']['passed']
    profile = Path('C:/Program Files/Creality/Creality Print 7.0/resources/profiles/Creality/machine/Creality K1C 0.4 nozzle.json')
    settings = json.loads(profile.read_text(encoding='utf-8'))
    assert settings['printable_area'] == '0x0,220x0,220x220,0x220'
    assert float(settings['printable_height']) == 250
    # Conservative reserved rear strip, also present as color_bed_exclude_area.
    assert settings['color_bed_exclude_area'] == '0x215,220x215,220x220,0x220'
    board_path = ROOT / 'hardware/drive-power-carrier/geometry.json'
    board = json.loads(board_path.read_text())
    assert board['board_size_mm'] == [30, 18, 1.6]
    model = ET.Element('model', {'xmlns': CORE, 'unit': 'millimeter', 'xml:lang': 'en-US'})
    ET.SubElement(model, 'metadata', {'name': 'Title'}).text = 'RaceRemote current electronics fit kit 0.1 - not sliced'
    resources = ET.SubElement(model, 'resources'); build = ET.SubElement(model, 'build')
    placed = {}; rows = []; sources = {ASSEMBLY.relative_to(ROOT).as_posix(): digest(ASSEMBLY),
                                      board_path.relative_to(ROOT).as_posix(): digest(board_path)}
    for oid, (name, relative, rotate, xy) in enumerate(SPECS, 1):
        source = ROOT / relative
        source_hash = digest(source)
        if name != 'pcb_gauge':
            assert assembly['source_sha256'][relative] == source_hash, f'Stale assembly evidence: {relative}'
        sources[relative] = source_hash
        original = trimesh.load_mesh(source)
        assert original.is_volume and len(original.split()) == 1 and np.isfinite(original.vertices).all(), name
        if name == 'pcb_gauge':
            assert np.allclose(original.extents, [18, 30, 1.6], atol=1e-6, rtol=0)
            assert np.isclose(original.volume, np.prod(board['board_size_mm']), atol=.001, rtol=0)
        transform = np.eye(4)
        if rotate:
            # Exact +90deg X, preserving the established cradle/cap orientation.
            transform[:3, :3] = [[1, 0, 0], [0, 0, -1], [0, 1, 0]]
        solid = original.copy(); solid.apply_transform(transform)
        transform[:3, 3] = -solid.bounds[0]
        solid.apply_translation(transform[:3, 3])
        assert np.allclose(solid.bounds[0], 0, atol=1e-9, rtol=0)
        assert np.isclose(solid.volume, original.volume, atol=1e-7, rtol=1e-9)
        output = OUT / f'{name}.stl'
        solid.export(output)
        reloaded = trimesh.load_mesh(output)
        assert reloaded.is_volume and len(reloaded.split()) == 1
        assert np.allclose(reloaded.triangles, solid.triangles, atol=1e-5, rtol=0), name
        plate = solid.copy(); plate.apply_translation([*xy, 0])
        assert (plate.bounds[0] >= 0).all() and (plate.bounds[1] <= [220, 215, 250]).all()
        placed[name] = plate
        full_transform = transform.copy(); full_transform[:2, 3] += xy
        obj = ET.SubElement(resources, 'object', {'id': str(oid), 'name': name, 'type': 'model'})
        m = ET.SubElement(obj, 'mesh'); vertices = ET.SubElement(m, 'vertices')
        for v in plate.vertices:
            ET.SubElement(vertices, 'vertex', dict(zip('xyz', (format(a, '.12g') for a in v))))
        triangles = ET.SubElement(m, 'triangles')
        for f in plate.faces:
            ET.SubElement(triangles, 'triangle', dict(zip(('v1', 'v2', 'v3'), map(str, f))))
        ET.SubElement(build, 'item', {'objectid': str(oid)})
        rows.append({'name': name, 'quantity': 1, 'role': 'bare PCB dimensional gauge' if name == 'pcb_gauge' else 'current holder',
                     'source': relative, 'source_sha256': source_hash, 'source_is_in_current_assembly': name != 'pcb_gauge',
                     'source_to_stl_matrix': transform.tolist(), 'source_to_plate_matrix': full_transform.tolist(),
                     'size_mm': solid.extents.tolist(), 'plate_bounds_mm': plate.bounds.tolist(),
                     'volume_mm3': float(solid.volume), 'triangles': len(solid.faces), 'output': output.name})
    gaps = []
    for (a, ma), (b, mb) in combinations(placed.items(), 2):
        sep = np.maximum(np.maximum(ma.bounds[0, :2] - mb.bounds[1, :2], mb.bounds[0, :2] - ma.bounds[1, :2]), 0)
        gap = float(np.linalg.norm(sep)); assert gap >= 9.9999, (a, b, gap)
        gaps.append({'parts': [a, b], 'xy_bounding_box_gap_mm': gap})
    types = ET.Element('Types', {'xmlns': CONTENT})
    for ext, mime in [('rels', 'application/vnd.openxmlformats-package.relationships+xml'),
                      ('model', 'application/vnd.ms-package.3dmanufacturing-3dmodel+xml')]:
        ET.SubElement(types, 'Default', {'Extension': ext, 'ContentType': mime})
    rels = ET.Element('Relationships', {'xmlns': REL})
    ET.SubElement(rels, 'Relationship', {'Id': 'rel0', 'Target': '/3D/3dmodel.model',
                                       'Type': 'http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel'})
    package = OUT / 'electronics-fit.3mf'
    with ZipFile(package, 'w') as archive:
        for name, payload in [('[Content_Types].xml', xml(types)), ('_rels/.rels', xml(rels)), ('3D/3dmodel.model', xml(model))]:
            info = ZipInfo(name, date_time=(2026, 9, 15, 0, 0, 0)); info.compress_type = ZIP_DEFLATED
            archive.writestr(info, payload)
    loaded = trimesh.load_scene(package).to_geometry().split()
    assert len(loaded) == len(SPECS)
    unmatched = list(loaded)
    for name, solid in placed.items():
        matches = [m for m in unmatched if np.allclose(m.bounds, solid.bounds, atol=1e-7, rtol=0)]
        assert len(matches) == 1, name
        actual = matches[0]
        assert actual.is_volume and len(actual.faces) == len(solid.faces)
        assert np.isclose(actual.volume, solid.volume, rtol=1e-9, atol=1e-7)
        unmatched.remove(actual)
    fig, ax = plt.subplots(figsize=(11, 5))
    for (name, solid), color in zip(placed.items(), ['#58a58a', '#528bd1', '#f0a329', '#bf6983', '#bbc2c8']):
        faces = solid.triangles[solid.face_normals[:, 2] > .01]
        faces = faces[np.argsort(faces[:, :, 2].mean(axis=1))]
        ax.add_collection(PolyCollection(faces[:, :, :2], facecolors=color, edgecolors='#273641', linewidths=.15))
        ax.text(solid.bounds[:, 0].mean(), solid.bounds[1, 1] + 3, name, ha='center', fontsize=10)
    ax.set(xlim=(10, 190), ylim=(20, 105), aspect='equal', xlabel='X (mm)', ylabel='Y (mm)',
           title='Current electronics holders: 4 parts + bare PCB gauge\nGeometry only; scale 1:1; not sliced or printed')
    ax.grid(alpha=.2); fig.tight_layout(); fig.savefig(OUT / 'layout.png', dpi=160); plt.close(fig)
    for path in [Path(__file__), ROOT / 'tools/build_motor_print_kit.py', ROOT / 'tools/print-kit-requirements.txt', ROOT / 'tools/cad-requirements.txt']:
        sources[path.relative_to(ROOT).as_posix()] = digest(path)
    report = {'contract': 'CURRENT-FIT-KIT-01 v0.1', 'date': '2026-09-15', 'unit': 'millimeter',
              'parts': rows, 'xy_gaps': gaps, 'source_sha256': sources,
              'profile_reference': {'path': str(profile), 'sha256': digest(profile), 'usable_box_mm': [220, 215, 250],
                                    'nozzle_material_or_process_selected': False},
              'checks': {'assembly_source_hashes': True, 'single_closed_solids': True, 'rigid_transforms_only': True,
                         'stl_reload_triangles': True, 'five_object_3mf_roundtrip': True, 'bed_and_spacing': True},
              'slicer_import_verified': False, 'sliced': False, 'printed': False,
              'limitations': ['Electronics holder fit kit, not complete chassis or finished assembly',
                              'Gauge has no components/holes and is not an electrically functional PCB',
                              'No process/nozzle/material, support, brim, sequential clearance or physical tolerance validation',
                              'Current cradle window and tray lacing saddle included; source CAD remains unchanged'],
              'artifacts': {p.relative_to(ROOT).as_posix(): digest(p) for p in [package, OUT / 'layout.png', *sorted(OUT.glob('*.stl'))]}}
    (ROOT / 'docs/evidence/current-fit-kit-v01.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'parts': len(loaded), 'checks': report['checks'], 'minimum_gap_mm': min(g['xy_bounding_box_gap_mm'] for g in gaps)}, indent=2))


if __name__ == '__main__':
    main()
