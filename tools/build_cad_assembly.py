"""Build a portable packaging study and check added volumes against zcar.

Geometry budgets are explicit placeholders, not measured component models.
No reconstruction/repair of the non-watertight upstream assembly is attempted.
"""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

import numpy as np
import trimesh
from inspect_zcar import inspect
from layout_zcar_battery import broad_phase, box, overlap, unique_part


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main(openscad):
    root = Path(__file__).resolve().parents[1]
    cad = root / 'cad'
    checkout = root / 'build/upstream/zcar'
    source = inspect(checkout)
    out = root / 'build/cad-assembly'
    out.mkdir(parents=True, exist_ok=True)
    reference = cad / 'reference'
    reference.mkdir(exist_ok=True)
    original = trimesh.load_mesh(checkout / 'stl/zcar.stl')
    parts = original.split(only_watertight=False, repair=False)
    fi, frame_target = unique_part(parts, [49.5, 117.375, 23.25])
    ci, _ = unique_part(parts, [49.5, 47.05, 19])
    origin = frame_target.bounds[0].copy()
    original.apply_translation(-origin)
    for p in parts:
        p.apply_translation(-origin)
    original.export(reference / 'zcar-aligned.stl')
    shutil.copyfile(checkout / 'LICENSE', reference / 'LICENSE')
    if subprocess.check_output(['git','-C',str(checkout),'status','--porcelain','--','zcar.skp','README.md'],text=True).strip():
        raise ValueError('Upstream editable source has local changes')
    shutil.copyfile(checkout / 'zcar.skp',reference / 'zcar.skp')
    shutil.copyfile(checkout / 'README.md',reference / 'upstream-README.md')
    (reference / 'NOTICE.md').write_text(
        '# zcar reference geometry\n\n'
        f'Source: https://github.com/alexyu132/zcar/tree/{source["revision"]}\n\n'
        f'File: stl/zcar.stl; SHA256 {source["files"]["stl/zcar.stl"]["sha256"]}.\n\n'
        f'Change: rigid translation by {(-origin).tolist()} mm only; no mesh repair. '
        'Upstream mechanism and gearing retained, not validated against new parts.\n\n'
        'The unchanged editable SketchUp source zcar.skp and upstream-README.md are included. '
        f'SketchUp SHA256: {digest(reference / "zcar.skp")}.\n\n'
        'License: GPL-3.0; see LICENSE. The embedded reference in ../assembly-viewer.html '
        'comes from this same source. Rebuild with tools/build_cad_assembly.py.\n', encoding='utf-8',newline='\n')
    previous = json.loads((root / 'build/cad-layout/report.json').read_text())
    assert previous['source_sha256'] == {k:v['sha256'] for k,v in source['files'].items()}
    frame = trimesh.load_mesh(root / 'build/cad-layout/frame.stl')
    cover = trimesh.load_mesh(root / 'build/cad-layout/cover.stl')
    meshes = {}
    for name in ['carrier_installed','carrier_print','adhesive','battery','control','power','camera','driver','charge']:
        path = out / f'{name}.stl'
        subprocess.run([openscad,'-o',str(path),'-D',f'part="{name}"',str(cad/'assembly.scad')],
                       check=True,capture_output=True)
        meshes[name] = trimesh.load_mesh(path)
        assert meshes[name].is_volume, name
    for name, part_name in [('support','support_installed'),('guards','guards_installed'),('band','band_corridor')]:
        path = out / f'{name}.stl'
        subprocess.run([openscad,'-o',str(path),'-D',f'part="{part_name}"',str(cad/'battery-holder.scad')],
                       check=True,capture_output=True)
        meshes[name] = trimesh.load_mesh(path)
        assert meshes[name].is_volume, name
    added = ['carrier_installed','adhesive','control','power','camera','driver','charge']
    checks = {}
    for name in added:
        mesh = meshes[name]
        checks[name] = {
            'frame_overlap_mm3': overlap(mesh,frame),
            'cover_overlap_mm3': overlap(mesh,cover),
            'other_reference_aabb_candidates': broad_phase(parts,mesh.bounds,{fi,ci}),
            'battery_extraction_sweep_overlap_mm3': overlap(mesh,box([99,18,15],[-.25,55,10.25])),
        }
        assert checks[name]['frame_overlap_mm3'] < 1e-4, (name,checks[name])
        assert checks[name]['cover_overlap_mm3'] < 1e-4, (name,checks[name])
        assert checks[name]['battery_extraction_sweep_overlap_mm3'] < 1e-4, name
        # A broad hit may be inside the carrier's empty space between feet.
        # Refine against conservative boxes, never repair the open reference.
        # Pad each side by 0.001 mm so even planar fragments have volume.
        refined = {str(i): overlap(mesh,box(parts[i].extents+.002,parts[i].bounds.mean(axis=0)))
                   for i in checks[name]['other_reference_aabb_candidates']}
        checks[name]['reference_candidate_box_overlap_mm3'] = refined
        assert all(v<1e-4 for v in refined.values()), (name,refined)
    pairs = {}
    names = added + ['battery','support','guards','band']
    for i,a in enumerate(names):
        for b in names[i+1:]:
            pairs[f'{a}/{b}'] = overlap(meshes[a],meshes[b])
            assert pairs[f'{a}/{b}'] < 1e-4, (a,b,pairs[f'{a}/{b}'])
    # Four reserved adhesive pads must touch roof, not float in a gap.
    seating = []
    for x in [4.25,39.25]:
        for y in [45,70]:
            probe = box([6,6,.1],[x+3,y+3,19.70])
            area = overlap(probe,cover)/.1
            seating.append(area)
            assert np.isclose(area,36,atol=.01), area
    carrier = meshes['carrier_installed']
    lower = carrier.copy(); lower.apply_translation([0,0,-5])
    positive = overlap(lower,cover)
    assert positive > 1, positive
    assert len(meshes['carrier_print'].split(only_watertight=False,repair=False)) == 1
    assert np.allclose(meshes['carrier_print'].bounds[0],0,atol=1e-4)
    shutil.copyfile(out/'carrier_print.stl',cad/'electronics-carrier.stl')
    rows = [
        ('reference','Исходная механика zcar',original,[.57,.64,.69],0),
        ('battery','Батарея — макет 49×18×15',meshes['battery'],[1,.52,.1],0),
        ('holder','Опора и упоры батареи',trimesh.util.concatenate([meshes['support'],meshes['guards']]),[.35,.55,.32],0),
        ('band','Резерв ремешка',meshes['band'],[.2,.2,.25],0),
        ('carrier','Площадка и стойка — предложение',carrier,[.12,.65,.6],1),
        ('control','Резерв контроллера 22×18×10',meshes['control'],[.2,.45,.95],2),
        ('power','Резерв DC-DC 33×16×12',meshes['power'],[.95,.75,.15],2),
        ('camera','Резерв камеры 23×12×21',meshes['camera'],[.75,.2,.7],2),
        ('driver','Резерв драйвера 30×18×10',meshes['driver'],[.9,.3,.25],2),
        ('charge','Резерв зарядника 33×16×8',meshes['charge'],[.45,.35,.85],2),
    ]
    scene = []
    for name,label,mesh,color,level in rows:
        positions=mesh.triangles.reshape(-1,3)
        normals=np.repeat(mesh.face_normals,3,axis=0)
        packed=np.hstack([positions,normals]).astype('<f4')
        scene.append(dict(id=name,label=label,color=color,level=level,count=len(positions),
                          data=base64.b64encode(packed.tobytes()).decode()))
    template=(root/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    (cad/'assembly-viewer.html').write_text(template.replace('__SCENE_DATA__',json.dumps(scene,ensure_ascii=False)),encoding='utf-8',newline='\n')
    preview = root/'docs/evidence/assembly-overview.png'
    subprocess.run([openscad,'--preview','--imgsize=1400,1000','--autocenter','--viewall',
                    '-o',str(preview),str(cad/'assembly.scad')],check=True,capture_output=True)
    report = dict(revision=source['revision'],source_sha256=previous['source_sha256'],
                  design_sha256=digest(cad/'assembly.scad'),holder_sha256=digest(cad/'battery-holder.scad'),
                  checks=checks,pair_overlaps_mm3=pairs,foot_contact_areas_mm2=seating,
                  lowered_carrier_positive_control_mm3=positive,
                  assembly_bounds_mm=trimesh.util.concatenate([r[2] for r in rows]).bounds.tolist(),
                  component_bounds_mm={k:m.bounds.tolist() for k,m in meshes.items()},
                  exports={name:digest(cad/name) for name in ['assembly-viewer.html','electronics-carrier.stl','reference/zcar-aligned.stl']},
                  preview_sha256=digest(preview),
                  limitations=['Electronics are reserved envelopes, not exact purchasable component models',
                               'Original motor/servo/gearing retained; candidate fit and 9:43 not established',
                               'No wires, connectors, antenna, magnets or body shell',
                               'No suspension/steering sweep, strap removal path, load or print tests',
                               'Upper driver/charger blocks have no fastening yet; carrier requires print orientation/support design',
                               'Clearance checks cover static default geometry only'])
    (root/'docs/evidence/assembly-layout.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(dict(bounds=report['assembly_bounds_mm'],foot_contact_areas=seating,
                          positive_control_mm3=positive,checks=checks),indent=2))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--openscad',default='C:/Program Files/OpenSCAD/openscad.com')
    main(parser.parse_args().openscad)
