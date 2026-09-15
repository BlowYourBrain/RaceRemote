"""Offline scene and geometric close-up of the proposed soldered logic pair."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/direct-logic-harness'


def main():
    source=ROOT/'cad/buck-output-harness/viewer.html'
    report=ROOT/'docs/evidence/direct-logic-harness-v01.json'
    evidence=json.loads(report.read_text());assert evidence['service']['passed']
    original=source.read_text(encoding='utf-8')
    data=original.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0]
    rows=json.loads(data);removed={'buck_plug','buck-output-5V','buck-output-GND','mount-tray'}
    assert removed.issubset({r['id'] for r in rows});rows=[r for r in rows if r['id'] not in removed]
    specs=[('tray','Поддон с креплением пары питания',[.3,.6,.48]),
           ('5V','Waveshare → H8: припаянный провод 5 В',[.95,.35,.08]),
           ('GND','Waveshare → H3: припаянный общий провод',[.24,.21,.18]),
           ('tie-space','Место под вязку шнуром Ø0,8 мм',[.35,.3,.8]),
           ('tie-head-space','Место под узел шнура 3×3×2 мм',[.62,.32,.8])]
    inputs=[source,report,Path(__file__)];meshes={}
    for name,label,color in specs:
        path=DEST/(name+'.stl');inputs.append(path);m=trimesh.load_mesh(path);meshes[name]=m
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='direct-'+name,label=label,color=color,slide=None,level=0,wire=name!='tray',
                         hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=original.replace(data,json.dumps(rows,ensure_ascii=False),1).replace('Выход Waveshare и питание XIAO','Припаянное питание логики')
    start=page.index('<p class="note">');end=page.index('</p>',start)+4
    page=page[:start]+'''<p class="note">Пара от нижних хвостов Waveshare, резерв Ø1,6 мм. Между платами — печатное седло с проходом для шнура. Примерка и снятие условного кузова/макета батареи проверены в CAD. Пайка, изгибы, материал шнура и удержание не испытаны. Прежние приблизительные трассы мотора и входного питания ещё требуют замены: у них есть пересечения с поддоном.</p>'''+page[end:]
    page=page.replace('../../tools/build_buck_output_harness.py','../../tools/build_direct_logic_harness.py')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    # Cutaway used only for the illustration. The viewer contains full geometry.
    crop=trimesh.creation.box(extents=[9,12,11],transform=trimesh.transformations.translation_matrix([23.5,49.7,30.5]))
    fig=plt.figure(figsize=(10,6));ax=fig.add_subplot(111,projection='3d')
    for name,label,color in specs:
        m=trimesh.boolean.intersection([meshes[name],crop],engine='manifold')
        ax.add_collection3d(Poly3DCollection(m.triangles,facecolor=color,edgecolor='none'))
    ax.set(xlim=(19,28),ylim=(43.7,55.7),zlim=(25,36),xlabel='Car X (mm)',ylabel='Car Y (mm)',zlabel='Z (mm)',
           title='Saddle and lacing space — cropped CAD, no pull/print qualification')
    ax.set_box_aspect((9,12,11));ax.view_init(18,-125)
    fig.tight_layout();fig.savefig(DEST/'closeup.png',dpi=150);plt.close(fig)
    (DEST/'view-evidence.json').write_text(json.dumps({
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        'artifact_sha256':{n:hashlib.sha256((DEST/n).read_bytes()).hexdigest() for n in ['viewer.html','closeup.png']},
        'scene_objects':len(rows),'removed_ids':sorted(removed)},indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Generated scene:',len(rows),'objects and cropped close-up.')


if __name__=='__main__':main()
