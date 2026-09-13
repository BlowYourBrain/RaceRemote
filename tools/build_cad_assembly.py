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


def main(openscad, body_study=False, adjustable=False):
    root = Path(__file__).resolve().parents[1]
    cad = root / 'cad'
    checkout = root / 'build/upstream/zcar'
    source = inspect(checkout)
    body_study = body_study or adjustable
    stem = 'adjustable-layout' if adjustable else ('body-study' if body_study else 'assembly')
    design = cad / f'{stem}.scad'
    viewer_name = f'{stem}-viewer.html'
    carrier_name = f'{stem}-carrier.stl' if body_study else 'electronics-carrier.stl'
    out = root / f'build/cad-{stem}'
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
    export_names = ['carrier_installed','carrier_print','adhesive','battery','power','camera','driver','charge']
    export_names += ['body','cavity'] if body_study else ['control']
    extras = ['tray','camera_mount','main_fasteners','camera_fasteners'] if adjustable else []
    export_names += extras
    for name in export_names:
        path = out / f'{name}.stl'
        subprocess.run([openscad,'-o',str(path),'-D',f'part="{name}"',str(design)],
                       check=True,capture_output=True)
        meshes[name] = trimesh.load_mesh(path)
        assert meshes[name].is_volume, name
    for name, part_name in [('support','support_installed'),('guards','guards_installed'),('band','band_corridor')]:
        path = out / f'{name}.stl'
        subprocess.run([openscad,'-o',str(path),'-D',f'part="{part_name}"',str(cad/'battery-holder.scad')],
                       check=True,capture_output=True)
        meshes[name] = trimesh.load_mesh(path)
        assert meshes[name].is_volume, name
    added = ['carrier_installed','adhesive','power','camera','driver','charge']
    added += extras
    if not body_study:
        added.append('control')
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
    shutil.copyfile(out/'carrier_print.stl',cad/carrier_name)
    rows = [
        ('reference','Исходная механика zcar',original,[.57,.64,.69],0),
        ('battery','Батарея — макет 49×18×15',meshes['battery'],[1,.52,.1],0),
        ('holder','Опора и упоры батареи',trimesh.util.concatenate([meshes['support'],meshes['guards']]),[.35,.55,.32],0),
        ('band','Резерв ремешка',meshes['band'],[.2,.2,.25],0),
        ('carrier','Площадка и стойка — предложение',carrier,[.12,.65,.6],1),
        ('power','Резерв DC-DC 33×16×5,7' if body_study else 'Резерв DC-DC 33×16×12',meshes['power'],[.95,.75,.15],2),
        ('camera','Общий ESP32 + камера: резерв 23×12×21' if body_study else 'Резерв камеры 23×12×21',meshes['camera'],[.75,.2,.7],2),
        ('driver','Резерв драйвера 30×18×10',meshes['driver'],[.9,.3,.25],2),
        ('charge','Резерв зарядника 33×16×8',meshes['charge'],[.45,.35,.85],2),
    ]
    if adjustable:
        rows += [('tray','Сдвижной поддон силовых плат',meshes['tray'],[.4,.8,.6],1),
                 ('camera_mount','Независимый держатель камеры',meshes['camera_mount'],[.25,.6,.4],1),
                 ('main_fasteners','Четыре винта/гайки — резерв M2',meshes['main_fasteners'],[.4,.4,.45],1),
                 ('camera_fasteners','Два винта камеры — резерв M2',meshes['camera_fasteners'],[.4,.4,.45],1)]
    body_checks = None
    body_positive = None
    if body_study:
        # This cavity belongs to our hypothetical shell, not a commercial body.
        body_checks = {}
        for name in added + ['battery','support','guards','band']:
            m = meshes[name]
            outside = trimesh.boolean.difference([m,meshes['cavity']],engine='manifold').volume
            shell_hit = overlap(m,meshes['body'])
            body_checks[name] = dict(outside_hypothetical_cavity_mm3=float(outside),
                                     hypothetical_shell_overlap_mm3=shell_hit)
            assert outside < 1e-3 and shell_hit < 1e-4, (name,body_checks[name])
        raised = meshes['camera'].copy(); raised.apply_translation([0,0,8])
        body_positive = overlap(raised,meshes['body'])
        assert body_positive > 1, 'Body collision positive control'
        rows.append(('body','Условный кузов — контур, не покупная модель',meshes['body'],[.15,.3,.5],3))
    else:
        rows.append(('control','Резерв контроллера 22×18×10',meshes['control'],[.2,.45,.95],2))
    scene = []
    for name,label,mesh,color,level in rows:
        positions=mesh.triangles.reshape(-1,3)
        normals=np.repeat(mesh.face_normals,3,axis=0)
        wire = name == 'body'
        if wire:
            edges = mesh.face_adjacency_edges[mesh.face_adjacency_angles > .15]
            positions = mesh.vertices[edges].reshape(-1,3)
            normals = np.tile([0,0,1],(len(positions),1))
        packed=np.hstack([positions,normals]).astype('<f4')
        scene.append(dict(id=name,label=label,color=color,level=level,wire=wire,count=len(positions),
                          slide=('electronics' if name in ['tray','power','driver','charge','main_fasteners'] else
                                 'camera' if name in ['camera','camera_mount','camera_fasteners'] else None) if adjustable else None,
                          data=base64.b64encode(packed.tobytes()).decode()))
    template=(root/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    if body_study:
        template = template.replace('3D-компоновка 0.1','Кузов и компоновка 0.2').replace('assembly.scad','body-study.scad').replace('assembly.md','body-study.md')
        template = template.replace('Габаритная проработка, не готовая машинка.',
            'Условный кузов 70×155×54 мм над колёсами. Это наш предложенный контур, не готовый кузов Mini-Z.')
    if adjustable:
        template=template.replace('Кузов и компоновка 0.2','Регулируемая компоновка 0.3').replace('body-study.scad','adjustable-layout.scad').replace('body-study.md','adjustable-layout.md')
    adjustments = None
    if adjustable:
        from adjustable_checks import check_positions
        adjustments=check_positions(meshes,parts,frame,cover,{fi,ci})
    template=template.replace('__ADJUSTMENT_DATA__',json.dumps(adjustments,ensure_ascii=False))
    (cad/viewer_name).write_text(template.replace('__SCENE_DATA__',json.dumps(scene,ensure_ascii=False)),encoding='utf-8',newline='\n')
    preview = root/f'docs/evidence/{stem}-overview.png'
    subprocess.run([openscad,'--preview','--imgsize=1400,1000','--autocenter','--viewall',
                    '-o',str(preview),str(design)],check=True,capture_output=True)
    report = dict(revision=source['revision'],source_sha256=previous['source_sha256'],
                  design_sha256=digest(design),holder_sha256=digest(cad/'battery-holder.scad'),
                  assembly_helpers_sha256=digest(cad/'assembly.scad'),
                  template_sha256=digest(root/'tools/cad_viewer_template.html'),
                  body_helpers_sha256=digest(cad/'body-study.scad') if adjustable else None,
                  adjustment_checks=adjustments,
                  orientation={'front':'+Y','rear_axle_y_mm':21.125,'front_axle_y_mm':111.125},
                  hypothetical_body_checks=body_checks,
                  raised_camera_body_positive_control_mm3=body_positive,
                  checks=checks,pair_overlaps_mm3=pairs,foot_contact_areas_mm2=seating,
                  lowered_carrier_positive_control_mm3=positive,
                  assembly_bounds_mm=trimesh.util.concatenate([r[2] for r in rows]).bounds.tolist(),
                  component_bounds_mm={k:m.bounds.tolist() for k,m in meshes.items()},
                  exports={name:digest(cad/name) for name in [viewer_name,carrier_name,'reference/zcar-aligned.stl']},
                  preview_sha256=digest(preview),
                  limitations=['Electronics are reserved envelopes, not exact purchasable component models',
                               'Original motor/servo/gearing retained; candidate fit and 9:43 not established',
                               'No wires, connectors, antenna, magnets or body shell',
                               'No suspension/steering sweep, strap removal path, load or print tests',
                               'Upper driver/charger blocks have no fastening yet; carrier requires print orientation/support design',
                               'Clearance checks cover static default geometry only'])
    if body_study:
        report['limitations'].remove('Upper driver/charger blocks have no fastening yet; carrier requires print orientation/support design')
        report['limitations'] += ['Hypothetical shell, not purchased Mini-Z geometry; static wheel arches only',
                                 'Combined camera/control board and bare 5.7 mm buck are architecture proposals',
                                 'Charger standoffs, camera fasteners, body windows and magnets not designed',
                                 'Shell containment checked for added volumes only, not every upstream mechanism part']
        report['limitations'].remove('No wires, connectors, antenna, magnets or body shell')
        report['limitations'].append('No wires, connectors, antenna or magnets; proposed shell only')
        # Technical side view: projected meshes, no imagined commercial body.
        import matplotlib
        matplotlib.use('Agg')
        import matplotlib.pyplot as plt
        from matplotlib.collections import LineCollection, PolyCollection
        fig,ax=plt.subplots(figsize=(13,6))
        ax.add_collection(PolyCollection(original.triangles[:,:,[1,2]],facecolor='#85929e',edgecolor='none',alpha=.12))
        for name,label,m,color,level in rows[1:]:
            if name=='body':
                edges=m.face_adjacency_edges[m.face_adjacency_angles>.15]
                ax.add_collection(LineCollection(m.vertices[edges][:,:,[1,2]],colors='#285078',linewidths=.6))
            else:
                lo,hi=m.bounds
                ax.add_patch(plt.Rectangle((lo[1],lo[2]),hi[1]-lo[1],hi[2]-lo[2],
                                          facecolor=color,edgecolor='white',alpha=.65))
        for name,label in [('battery','Батарея'),('driver','Драйвер'),('power','5 В'),
                           ('charge','Зарядка'),('camera','ESP32 +\nкамера')]:
            y,z=meshes[name].bounds.mean(axis=0)[1:]
            ax.text(y,z,label,ha='center',va='center',fontsize=9)
        ax.axhline(-7,color='#526170',linewidth=1)
        ax.annotate('Перед →',xy=(110,-13),ha='center')
        ax.annotate('База 90 мм',xy=(66.125,-12),ha='center')
        ax.annotate('',xy=(21.125,-9),xytext=(111.125,-9),arrowprops=dict(arrowstyle='<->'))
        ax.annotate('54 мм',xy=(143,20),rotation=90,va='center')
        ax.annotate('',xy=(141,-7),xytext=(141,47),arrowprops=dict(arrowstyle='<->'))
        ax.set(xlim=(-25,150),ylim=(-17,56),xlabel='Y, мм',ylabel='Z, мм',
               title=('Предложение 0.3: регулируемые крепления' if adjustable else 'Предложение 0.2: условный кузов и габаритные резервы')+'\nНе модель покупного Mini-Z; провода, крепёж и ход подвески не проверены')
        ax.set_aspect('equal'); ax.grid(alpha=.15)
        fig.tight_layout()
        side=root/f'docs/evidence/{stem}-side.png'
        fig.savefig(side,dpi=160); plt.close(fig)
        report['side_preview_sha256']=digest(side)
    if adjustable:
        report['limitations'].remove('Clearance checks cover static default geometry only')
        report['limitations'].append('Adjustment sampled on a 1 mm grid; no continuous motion or tolerance verification')
        for name in ['tray','camera_mount']:
            printable=meshes[name].copy()
            printable.apply_translation(-printable.bounds[0])
            target=cad/f'adjustable-{name}.stl'
            printable.export(target)
            report['exports'][target.name]=digest(target)
        report_name='adjustable-layout.json'
    else:
        report_name=f'{stem}-layout.json'
    (root/'docs/evidence'/report_name).write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(dict(bounds=report['assembly_bounds_mm'],foot_contact_areas=seating,
                          positive_control_mm3=positive,checks=checks),indent=2))


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--openscad',default='C:/Program Files/OpenSCAD/openscad.com')
    parser.add_argument('--body-study',action='store_true',help='Build hypothetical body-constrained alternative')
    parser.add_argument('--adjustable',action='store_true',help='Build slotted adjustable stages')
    args=parser.parse_args()
    main(args.openscad,args.body_study,args.adjustable)
