"""Import the published XIAO STEP and place dimensioned candidate models.

The 2023 vendor reference is not a promise about the current OV3660 revision.
No mesh repair, geometric scaling or invented PCB component placement.
"""
import base64
import hashlib
import io
import json
from pathlib import Path
import subprocess
import urllib.request
import zipfile

import numpy as np
import trimesh
from OCP.STEPControl import STEPControl_Reader
from OCP.IFSelect import IFSelect_RetDone
from OCP.BRepMesh import BRepMesh_IncrementalMesh
from OCP.StlAPI import StlAPI_Writer
from layout_zcar_battery import box,overlap,broad_phase

ROOT=Path(__file__).resolve().parents[1]
URL='https://files.seeedstudio.com/wiki/SeeedStudio-XIAO-ESP32S3/res/seeed-studio-xiao-esp32s3-sense-3d_model.zip'
ARCHIVE_SHA='773c16cb7518a3a18df926979f24c7e224f19309aad69962ec8a54454e288c59'

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    cad=ROOT/'cad'; dest=cad/'components'; dest.mkdir(exist_ok=True)
    out=ROOT/'build/candidate-models'; out.mkdir(parents=True,exist_ok=True)
    archive=ROOT/'build/component-research/xiao-sense.zip'
    if not archive.exists():
        archive.parent.mkdir(parents=True,exist_ok=True)
        archive.write_bytes(urllib.request.urlopen(URL,timeout=45).read())
    assert sha(archive)==ARCHIVE_SHA,'Published archive changed; review before importing'
    with zipfile.ZipFile(io.BytesIO(archive.read_bytes())) as z:
        member=next(n for n in z.namelist() if n.lower().endswith('.step'))
        step=dest/'xiao-sense-2023.step'; step.write_bytes(z.read(member))
    reader=STEPControl_Reader(); assert reader.ReadFile(str(step))==IFSelect_RetDone
    reader.TransferRoots(); shape=reader.OneShape()
    BRepMesh_IncrementalMesh(shape,.03,False,.15,True).Perform()
    writer=StlAPI_Writer(); writer.ASCIIMode=False
    raw=out/'xiao-source.stl'; assert writer.Write(shape,str(raw))
    xiao=trimesh.load_mesh(raw)
    original_bounds=xiao.bounds.copy()
    # Vendor board normal/lens direction is +Y; USB points sideways in car.
    # Centre full imported outline laterally, seat its bottom at Z=22.
    shift=np.array([24.75-xiao.bounds.mean(axis=0)[0],79-xiao.bounds[0,1],22-xiao.bounds[0,2]])
    xiao.apply_translation(shift)
    xiao.export(dest/'xiao-sense-2023-installed.stl')
    candidates={}
    for name in ['motor','motor_upper_case','servo_case','buck','bearing']:
        path=dest/f'{name}.stl'
        subprocess.run(['C:/Program Files/OpenSCAD/openscad.com','-o',str(path),'-D',f'part="{name}"',str(dest/'candidates.scad')],check=True,capture_output=True)
        candidates[name]=trimesh.load_mesh(path)
        assert candidates[name].is_volume,name
    reference=trimesh.load_mesh(cad/'reference/zcar-aligned.stl')
    parts=reference.split(only_watertight=False,repair=False)
    # Recover the existing motor shaft axis from its 24 end-circle vertices.
    end=parts[217].vertices
    end=end[np.isclose(end[:,0],4.25,atol=.001),1:]
    fit=np.linalg.lstsq(np.column_stack([2*end,np.ones(len(end))]),(end*end).sum(axis=1),rcond=None)[0]
    radius=np.sqrt(fit[2]+sum(fit[:2]**2))
    residual=float(np.max(np.abs(np.linalg.norm(end-fit[:2],axis=1)-radius)))
    assert len(end)==24 and residual<1e-4,(len(end),residual)
    motor=candidates['motor'].copy(); motor.apply_translation([12.65,*fit[:2]])
    servo=candidates['servo_case'].copy(); servo.apply_translation([18.35,71.975,2])
    buck=candidates['buck'].copy(); buck.apply_translation([8.25,60,27.6])
    motor.export(dest/'motor-installed.stl');servo.export(dest/'servo-case-installed.stl');buck.export(dest/'buck-installed.stl')
    # Omit original motor and servo bodies for viewing; keep original mounts,
    # gears and servo horn. Their compatibility is still not established.
    without=trimesh.util.concatenate([p for i,p in enumerate(parts) if i not in {176,189,217,218}])
    without.export(dest/'zcar-without-reference-actuators.stl')
    scene_rows=[('reference','Механика zcar; исходные корпуса мотора/серво скрыты',without,[.57,.64,.69]),
                ('motor','F130-08450: корпус и вал по чертежу',motor,[.75,.75,.8]),
                ('servo','SG90: габарит по карточке, не точная форма корпуса',servo,[.2,.45,.8]),
                ('camera','XIAO Sense: STEP производителя 2023, ревизию OV3660 сверить',xiao,[.2,.65,.45]),
                ('power','Waveshare: опубликованный низкий габарит 33×16×5,7',buck,[.95,.75,.15])]
    base=ROOT/'build/cad-adjustable-layout'
    meshes={n:trimesh.load_mesh(base/f'{n}.stl') for n in ['carrier_installed','tray','camera_mount','main_fasteners','camera_fasteners','battery','support','guards','band','driver','charge','body','cavity']}
    for n in ['carrier_installed','tray','camera_mount','main_fasteners','camera_fasteners','battery','support','guards','band','driver','charge','body']:
        color=[.15,.3,.5] if n=='body' else [.95,.45,.1] if n=='battery' else [.6,.45,.6] if n in ['driver','charge'] else [.4,.65,.6]
        labels={'driver':'Драйвер: пока бюджет будущей платы','charge':'Зарядка: пока бюджет, модуль не выбран','body':'Условный кузов, не покупная модель',
                'carrier_installed':'Основание с пазами','tray':'Силовой поддон','camera_mount':'Держатель камеры',
                'main_fasteners':'Крепёж силового поддона — резерв','camera_fasteners':'Крепёж камеры — резерв',
                'battery':'Батарея 49×18×15','support':'Опора батареи','guards':'Упоры батареи','band':'Резерв ремешка'}
        scene_rows.append((n,labels.get(n,n),meshes[n],color))
    # Imported vendor surface includes open faces: use the complete bounding
    # box for conservative external checks, never pretend it is a solid PCB.
    camera_box=box(xiao.extents,xiao.bounds.mean(axis=0))
    checks={n:overlap(camera_box,m) for n,m in meshes.items() if n not in ['body','cavity']}
    checks['frame']=overlap(camera_box,trimesh.load_mesh(ROOT/'build/cad-layout/frame.stl'))
    checks['cover']=overlap(camera_box,trimesh.load_mesh(ROOT/'build/cad-layout/cover.stl'))
    checks['candidate_buck']=overlap(camera_box,buck)
    camera_reference_boxes={str(i):overlap(camera_box,box(parts[i].extents+.002,parts[i].bounds.mean(axis=0)))
                            for i in broad_phase(parts,camera_box.bounds,{0,367,176,189,217,218})}
    checks['outside_hypothetical_body']=float(trimesh.boolean.difference([camera_box,meshes['cavity']],engine='manifold').volume)
    scene=[]
    for name,label,m,color in scene_rows:
        wire=name=='body'
        if wire:
            edges=m.face_adjacency_edges[m.face_adjacency_angles>.15]
            pos=m.vertices[edges].reshape(-1,3);norm=np.tile([0,0,1],(len(pos),1))
        else:
            pos=m.triangles.reshape(-1,3);norm=np.repeat(m.face_normals,3,axis=0)
        packed=np.hstack([pos,norm]).astype('<f4')
        scene.append(dict(id=name,label=label,color=color,level=0,wire=wire,slide=None,count=len(pos),data=base64.b64encode(packed.tobytes()).decode()))
    template=(ROOT/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    template=template.replace('3D-компоновка 0.1','Примерка кандидатов 0.4').replace('assembly.scad','candidate-fit.scad').replace('assembly.md','candidate-fit.md')
    template=template.replace('Габаритная проработка, не готовая машинка.','XIAO — официальный STEP 2023, мотор — чертёж. Серво/DC-DC — опубликованные габариты. Зарядка и драйвер ещё условные.')
    template=template.replace('__ADJUSTMENT_DATA__','null').replace('__SCENE_DATA__',json.dumps(scene,ensure_ascii=False))
    template=template.replace('</script></html>',"document.querySelector('#explode').closest('label').hidden=true;</script></html>")
    (cad/'candidate-fit-viewer.html').write_text(template,encoding='utf-8',newline='\n')
    preview=ROOT/'docs/evidence/candidate-fit-overview.png'
    render=subprocess.run(['C:/Program Files/OpenSCAD/openscad.com','--preview','--imgsize=1400,1000','--autocenter','--viewall',
                           '-o',str(preview),str(cad/'candidate-fit.scad')],check=True,capture_output=True,text=True)
    assert 'WARNING' not in render.stderr,render.stderr
    report=dict(source_url=URL,source_archive_sha256=ARCHIVE_SHA,source_step_sha256=sha(step),step_date='2023-05-27',
                native_bounds_mm=original_bounds.tolist(),installed_bounds_mm=xiao.bounds.tolist(),translation_mm=shift.tolist(),
                xiao_mesh_watertight=xiao.is_watertight,xiao_faces=len(xiao.faces),
                motor_shaft_axis_yz_mm=fit[:2].tolist(),axis_fit_max_residual_mm=residual,
                camera_external_envelope_checks_mm3=checks,
                camera_other_reference_box_checks_mm3=camera_reference_boxes,
                motor_frame_cover_overlap_mm3={n:overlap(motor,trimesh.load_mesh(ROOT/f'build/cad-layout/{n}.stl')) for n in ['frame','cover']},
                servo_body_frame_cover_overlap_mm3={n:overlap(servo,trimesh.load_mesh(ROOT/f'build/cad-layout/{n}.stl')) for n in ['frame','cover']},
                component_bounds_mm={n:m.bounds.tolist() for n,m in [('motor',motor),('servo_body',servo),('buck',buck)]},
                artifacts_sha256={p.relative_to(cad).as_posix():sha(p) for p in dest.glob('*') if p.is_file()},
                viewer_sha256=sha(cad/'candidate-fit-viewer.html'),
                preview_sha256=sha(preview),
                source_models_sha256=sha(dest/'candidates.scad'),
                limitations=['2023 XIAO reference not confirmed as current OV3660 hardware; antenna/heatsink absent',
                             'XIAO box checks prove external geometric clearance only; mount contact not established',
                             'Motor boss/terminals, servo ears/spline/lead and buck terminals unknown',
                             'Motor clamp and pinion bore/engagement not validated; original mounting parts retained',
                             'No new adjustment sweep for actual candidate parts; viewer shows nominal pose only',
                             'Hypothetical body only, no motion/wiring/load/thermal/physical evidence'])
    (ROOT/'docs/evidence/candidate-fit.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:report[k] for k in ['native_bounds_mm','camera_external_envelope_checks_mm3','motor_frame_cover_overlap_mm3','servo_body_frame_cover_overlap_mm3']},indent=2))

if __name__=='__main__':main()
