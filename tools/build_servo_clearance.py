"""Locate SG90 envelope interference without changing candidate or chassis.

The upstream open servo surface is a visual reference, never a solid operand.
Run after candidate-fit prerequisites, with tools/cad-requirements.txt.
"""
import base64
import hashlib
import json
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.collections import LineCollection
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def box(low, high):
    m = trimesh.creation.box(extents=np.array(high)-low)
    m.apply_translation((np.array(high)+low)/2)
    return m

def main():
    out = ROOT/'cad/servo-clearance'
    sources = [ROOT/'build/cad-layout/frame.stl', ROOT/'cad/reference/zcar-aligned.stl',
               ROOT/'cad/components/servo-case-installed.stl']
    before = {str(p.relative_to(ROOT)):sha(p) for p in sources}
    frame, reference, candidate = [trimesh.load_mesh(p) for p in sources]
    assert frame.is_volume and candidate.is_volume
    upstream = reference.split(only_watertight=False, repair=False)[176]
    assert np.allclose(upstream.bounds, [[13.55,71.675,2],[46.15,101.275,14.2]], atol=.001)
    rear = upstream.vertices[np.isclose(upstream.vertices[:,1],71.675,atol=.001)]
    assert len(rear)==4
    interference = trimesh.boolean.intersection([candidate,frame],engine='manifold')
    pieces = interference.split(only_watertight=False,repair=False)
    assert len(pieces)==5 and abs(interference.volume-43.81498)<.001
    side = [p for p in pieces if p.extents[2]>9]
    front = [p for p in pieces if p.extents[2]<1.01]
    assert len(side)==4 and len(front)==1
    left = [p for p in side if p.bounds.mean(axis=0)[0]<30]
    right = [p for p in side if p.bounds.mean(axis=0)[0]>30]
    left_wall = max(p.bounds[1,0] for p in left)
    right_wall = min(p.bounds[0,0] for p in right)
    passage = right_wall-left_wall
    assert abs(passage-22.8)<.001
    cropped = trimesh.boolean.intersection([frame,box([10,69,0],[48.5,103,16])],engine='manifold')
    assert cropped.is_volume
    for name,m in [('frame',cropped),('upstream-servo',upstream),('interference',interference)]:
        m.export(out/f'{name}.stl')
    rows = [('frame','Участок неизменённой рамы zcar',cropped,[.5,.55,.6],True),
            ('upstream','Серво из zcar: справочная поверхность, не купленный SG90',upstream,[.2,.45,.8],False),
            ('candidate','Брусок кандидата 23×29×12,2, без изменения размера',candidate,[1,.6,.1],True),
            ('interference','Пять пересечений бруска с рамой',interference,[.9,.1,.15],False)]
    scene=[]
    for name,label,m,color,wire in rows:
        if wire:
            edges=m.face_adjacency_edges[m.face_adjacency_angles>.1]
            pos=m.vertices[edges].reshape(-1,3); normal=np.tile([0,0,1],(len(pos),1))
        else:
            pos=m.triangles.reshape(-1,3); normal=np.repeat(m.face_normals,3,axis=0)
        scene.append(dict(id=name,label=label,color=color,wire=wire,level=0,slide=None,count=len(pos),
                          data=base64.b64encode(np.hstack([pos,normal]).astype('<f4').tobytes()).decode()))
    html=(ROOT/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    html=html.replace('3D-компоновка 0.1','Зазоры серво 0.1').replace('assembly.scad','study.scad').replace('assembly.md','README.md')
    html=html.replace('reference/NOTICE.md','../reference/NOTICE.md').replace('span=178','span=55').replace('center=[24.75,58,20+explosion]','center=[29.85,86.5,8+explosion]')
    start=html.index('<p class="note">'); end=html.index('</p>',start)+4
    html=html[:start]+'<p class="note">Диагностика пересечений, не новая рама. Проход 22,8 мм, брусок 23 мм; образец ещё не измерен. Синяя форма взята из zcar, не из чертежа товара.</p>'+html[end:]
    html=html.replace('__ADJUSTMENT_DATA__','null').replace('__SCENE_DATA__',json.dumps(scene,ensure_ascii=False))
    html=html.replace('</script></html>',"document.querySelector('#explode').closest('label').hidden=true;</script></html>")
    (out/'viewer.html').write_text(html,encoding='utf-8',newline='\n')
    fig,axes=plt.subplots(1,2,figsize=(12,7))
    for ax,z in zip(axes,[8.1,2.5]):
        for name,label,m,color,wire in rows:
            lines=trimesh.intersections.mesh_plane(m,plane_normal=[0,0,1],plane_origin=[0,0,z])
            ax.add_collection(LineCollection(lines[:,:,:2],colors=[color],linewidths=2.5 if name=='interference' else 1.2,
                                            label={'frame':'Рама','upstream':'Справочное серво zcar','candidate':'Брусок кандидата','interference':'Пересечения'}[name]))
        ax.set(xlim=(12,48),ylim=(70,103),xlabel='X, мм',ylabel='Y, мм — вперёд',title=f'Сечение Z = {z} мм')
        ax.set_aspect('equal'); ax.grid(alpha=.2)
    axes[0].annotate('',xy=(left_wall,90.9),xytext=(right_wall,90.9),arrowprops={'arrowstyle':'<->','color':'black'})
    axes[0].text(29.85,89.2,'Проход 22,8 мм',ha='center',fontsize=9)
    axes[0].legend(loc='lower center',fontsize=8)
    fig.suptitle('SG90: проверка бруска и посадочного места, без изменения геометрии',fontsize=13)
    fig.tight_layout()
    preview=ROOT/'docs/evidence/servo-clearance.png';fig.savefig(preview,dpi=160);plt.close(fig)
    report={'version':'0.1','date':'2026-09-14','upstreamCommit':'4b714e63fa30ed2030a8a48ab15f1e4a5fa380c4',
            'sourceHashes':before,'candidateDimensionsXYZmm':candidate.extents.tolist(),
            'candidateBoundsMm':candidate.bounds.tolist(),'upstreamRearFaceVerticesMm':rear.tolist(),
            'upstreamRearFaceWidthMm':float(np.ptp(rear[:,0])),'frameSidePassageMm':float(passage),
            'leftPassageXmm':float(left_wall),'rightPassageXmm':float(right_wall),
            'interferenceVolumeMm3':float(interference.volume),'sideInterferenceMm3':sum(float(p.volume) for p in side),
            'frontLowerInterferenceMm3':float(front[0].volume),
            'pieces':[{'boundsMm':p.bounds.tolist(),'volumeMm3':float(p.volume)} for p in pieces],
            'upstreamReferenceWatertight':upstream.is_watertight,
            'unchangedSourceGeometry':True,'physicalFitVerified':False,
            'limitations':['Nominal CAD dimensions, no manufacturing tolerances',
                           'Upstream servo is an open reference surface, not the selected supplier geometry',
                           'Front-lower interference may overstate actual shaped servo volume; exact sample unknown',
                           'Width 23 mm exceeds the current 22.8 mm passage; do not declare it a drop-in fit',
                           'No shaft/ear/lead calibration, frame relief, new servo choice or hardware test'],
            'artifacts':{str(p.relative_to(ROOT)):sha(p) for p in [out/'frame.stl',out/'upstream-servo.stl',out/'interference.stl',out/'viewer.html',out/'study.scad',preview]},
            'scriptSha256':sha(Path(__file__))}
    assert before=={str(p.relative_to(ROOT)):sha(p) for p in sources}
    (ROOT/'docs/evidence/servo-clearance.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:report[k] for k in ['upstreamRearFaceWidthMm','frameSidePassageMm','interferenceVolumeMm3','sideInterferenceMm3','frontLowerInterferenceMm3']},indent=2))

if __name__=='__main__': main()
