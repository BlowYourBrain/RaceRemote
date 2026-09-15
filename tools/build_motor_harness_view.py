"""Show the two motor wires and the unresolved terminal reserve collision."""
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
DEST=ROOT/'cad/motor-harness'


def main():
    source=ROOT/'cad/direct-logic-harness/viewer.html'
    report=ROOT/'docs/evidence/motor-harness-v01.json'
    result=json.loads(report.read_text())
    original=source.read_text(encoding='utf-8')
    data=original.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0]
    rows=json.loads(data)
    assert sum(r['id']=='wire-motor' for r in rows)==1
    rows=[r for r in rows if r['id']!='wire-motor']
    specs=[('FO','H6 / FO → клемма A: резерв провода',[.95,.5,.05]),
           ('BO','H7 / BO → клемма B: резерв провода',[.2,.65,.25]),
           ('terminal-A','Клемма A: оценка, пересечение с подвеской',[.95,.15,.1]),
           ('terminal-B','Клемма B: оценка габарита',[.7,.55,.15])]
    inputs=[source,report,Path(__file__)];meshes={}
    for name,label,color in specs:
        path=DEST/(name+'.stl');inputs.append(path);m=trimesh.load_mesh(path);meshes[name]=m
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='motor-'+name,label=label,color=color,slide=None,level=0,
                         wire=name in ['FO','BO'],hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=original.replace(data,json.dumps(rows,ensure_ascii=False),1).replace('Припаянное питание логики','Проводка мотора: примерка')
    start=page.index('<p class="note">');end=page.index('</p>',start)+4
    page=page[:start]+'''<p class="note">Два провода от H6/H7, резерв Ø1,6 мм. Провода проходят локальную примерку. Красный резерв клеммы A пересекает боковую деталь подвески на 3,124 мм³: посадка мотора НЕ подтверждена. Клеммы оценены по изображению, нужен образец. Старый входной коридор питания тоже требует доработки. Печать и физическая сборка не выполнялись.</p>'''+page[end:]
    page=page.replace('../../tools/build_direct_logic_harness.py','../../tools/build_motor_harness.py')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    fig=plt.figure(figsize=(11,6));ax=fig.add_subplot(111,projection='3d')
    for name,color in [('reference-246',[.4,.5,.65]),('reference-281',[.55,.65,.75])]:
        p=DEST/(name+'.stl');inputs.append(p);m=trimesh.load_mesh(p)
        ax.add_collection3d(Poly3DCollection(m.triangles,facecolor=color,edgecolor='none',alpha=.35))
    for name,label,color in specs:
        ax.add_collection3d(Poly3DCollection(meshes[name].triangles,facecolor=color,edgecolor='none'))
    ax.set(xlim=(35,50),ylim=(4,54),zlim=(10,39),xlabel='X (mm)',ylabel='Y (mm)',zlabel='Z (mm)',
           title='Motor lead study: red terminal reserve overlaps side piece; not assembly approval')
    ax.set_box_aspect((15,50,29));ax.view_init(28,25)
    fig.tight_layout();fig.savefig(DEST/'closeup.png',dpi=140);plt.close(fig)
    (DEST/'view-evidence.json').write_text(json.dumps({
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        'artifact_sha256':{n:hashlib.sha256((DEST/n).read_bytes()).hexdigest() for n in ['viewer.html','closeup.png']},
        'scene_objects':len(rows),'wire_fit_passed':result['wire_fit_passed'],
        'complete_fit_passed':result['nominal_complete_fit_passed']},indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Scene objects:',len(rows),'complete fit:',result['nominal_complete_fit_passed'])


if __name__=='__main__':main()
