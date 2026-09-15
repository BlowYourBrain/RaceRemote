"""Build an offline assembled view and a close-up from the actual candidate meshes."""
import base64
import hashlib
import json
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
import numpy as np
import trimesh

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/xiao-power-harness'


def main():
    source=ROOT/'cad/drive-power-carrier/viewer.html'
    template=ROOT/'tools/cad_viewer_template.html'
    report=ROOT/'docs/evidence/xiao-power-harness-v01.json'
    evidence=json.loads(report.read_text())
    assert evidence['service']['passed'] and not any(evidence['wire_obstacle_hits_mm3'].values())
    rows=json.loads(source.read_text(encoding='utf-8').split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    removed={'cradle','control-GND','power-H9'}
    assert removed.issubset({r['id'] for r in rows})
    rows=[r for r in rows if r['id'] not in removed]
    inputs=[source,template,report,Path(__file__)]
    for name,label,color in [('cradle','Опора XIAO: проход питания',[.65,.7,.76]),
                             ('BAT','BAT0 → H9: питание XIAO',[.85,.12,.12]),
                             ('GND','GND0 → H5: общий провод',[.17,.17,.18])]:
        path=DEST/(name+'.stl');inputs.append(path);m=trimesh.load_mesh(path)
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='harness-'+name,label=label,color=color,slide=None,level=0,
                         wire=name!='cradle',hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=template.read_text(encoding='utf-8').replace('3D-компоновка 0.1','Проводка питания XIAO')
    begin=page.index('<p class="note">');end=page.index('</p>',begin)+4
    page=page[:begin]+'''<p class="note">Красный и чёрный провода идут к задним площадкам XIAO через окно в держателе. Начальная компоновка 0/+3; снятие условного кузова и выход макета батареи проверены в CAD. Изгибы, фиксация и реальные детали ещё требуют проверки. Для обзора проводов скройте кузов и камеру флажками.</p>'''+page[end:]
    page=page.replace('assembly.scad','../../tools/build_xiao_power_harness.py').replace('Редактировать в OpenSCAD','Генератор геометрии')
    page=page.replace('assembly.md','README.md').replace('reference/NOTICE.md','../packaging-v05/NOTICE.md')
    page=page.replace('part.visible=true;','part.visible=!part.hidden;').replace('input.checked=true;','input.checked=!part.hidden;')
    page=page.replace('__SCENE_DATA__',json.dumps(rows,ensure_ascii=False)).replace('__ADJUSTMENT_DATA__','null')
    page=page.replace('</script></html>',"document.querySelector('#explode').closest('label').hidden=true;\n</script></html>")
    page=page.replace('Разнесение деталей служит только для просмотра.','Детали можно скрывать флажками слева.')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    wanted={'power-pcb','control-FI','control-BI','harness-cradle','harness-BAT','harness-GND'}
    # This illustration is a cropped section; the interactive scene retains full geometry.
    crop=trimesh.creation.box(extents=[24,14,19],transform=trimesh.transformations.translation_matrix([32,80,32.5]))
    shown={}
    for row in rows:
        if row['id'] not in wanted:continue
        vertices=np.frombuffer(base64.b64decode(row['data']),dtype='<f4').reshape(-1,6)[:,:3]
        solid=trimesh.Trimesh(vertices=vertices,faces=np.arange(len(vertices)).reshape(-1,3),process=True)
        assert solid.is_volume,row['id']
        shown[row['id']]=trimesh.boolean.intersection([solid,crop],engine='manifold')
    fig=plt.figure(figsize=(13,6))
    for i in range(2):
        ax=fig.add_subplot(1,2,i+1,projection='3d')
        for row in rows:
            if row['id'] not in wanted or (i==1 and row['id']=='harness-cradle'):continue
            triangles=shown[row['id']].triangles
            ax.add_collection3d(Poly3DCollection(triangles,facecolor=row['color'],edgecolor='none'))
        ax.set(xlim=(20,44),ylim=(73,87),zlim=(23,42),xlabel='Car X (mm)',ylabel='Car Y (mm)',zlabel='Z (mm)',
               title='Cropped passage (XIAO hidden)' if i==0 else 'Wire routing (XIAO and cradle hidden)')
        ax.set_box_aspect((24,14,19));ax.view_init(20,-65 if i==0 else -110)
    fig.tight_layout();fig.savefig(DEST/'preview.png',dpi=140);plt.close(fig)
    (DEST/'view-evidence.json').write_text(json.dumps({
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        'artifact_sha256':{n:hashlib.sha256((DEST/n).read_bytes()).hexdigest() for n in ['viewer.html','preview.png']},
        'removed_scene_ids':sorted(removed),'nominal_pose_mm':[0,3]},indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Generated offline viewer and mesh close-up.')


if __name__=='__main__':main()
