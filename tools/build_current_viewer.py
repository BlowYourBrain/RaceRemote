"""Portable current CAD viewer: updated meshes, independently checked stage poses."""
import base64
import hashlib
import json
import re
from pathlib import Path
import numpy as np
from build_packaging_v05 import mesh,shifted

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/current-layout'

def main():
    DEST.mkdir(exist_ok=True)
    old=ROOT/'cad/packaging-v05/viewer.html'
    text=old.read_text(encoding='utf-8')
    rows=json.loads(text.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    removed={'removed','servo_clash','camera_mount','tray','camera_fasteners','service_usb','wire-servo','wire-antenna','wire-control'}
    rows=[p for p in rows if p['id']not in removed]
    source_paths=[old,ROOT/'tools/cad_viewer_template.html',Path(__file__)]
    def add(name,label,path,color,slide=None,wire=False,hidden=False):
        source_paths.append(path)
        m=mesh(path)
        if slide=='camera':m=shifted(m,-3)
        if wire:
            edges=m.face_adjacency_edges[m.face_adjacency_angles>.15]
            pos=m.vertices[edges].reshape(-1,3);norm=np.tile([0,0,1],(len(pos),1))
        else:pos=m.triangles.reshape(-1,3);norm=np.repeat(m.face_normals,3,axis=0)
        rows.append({'id':name,'label':label,'color':color,'slide':slide,'level':0,'wire':wire,
                     'hidden':hidden,'count':len(pos),'data':base64.b64encode(np.hstack([pos,norm]).astype('<f4').tobytes()).decode()})
    add('tray','Поддон с направляющей силового провода',ROOT/'cad/harness-guides/tray.stl',[.35,.65,.6],'electronics')
    add('cradle','Съёмная опора XIAO',ROOT/'cad/xiao-mount/cradle.stl',[.25,.5,.8],'camera')
    add('cap','Прижим с направляющей серво',ROOT/'cad/harness-guides/cap.stl',[1,.6,.1],'camera')
    add('clamp_screws','Крепёж прижима — резерв M2',ROOT/'cad/xiao-mount/screws.stl',[.6,.6,.63],'camera')
    add('stage_screws','Крепёж камеры в пазах',ROOT/'cad/xiao-mount/stage_screws.stl',[.6,.6,.63],'camera')
    add('service_usb','Правый доступ к USB XIAO — оценка штекера',ROOT/'cad/xiao-mount/usb_access.stl',[.65,.4,.15],'camera',True,True)
    for n,col in [('wire-servo',[.1,.3,.8]),('wire-antenna',[.1,.1,.12]),('wire-control',[.5,.25,.75])]:
        add(n,{'wire-servo':'Трасса серво','wire-antenna':'Трасса от U.FL','wire-control':'Трасса управления'}[n],ROOT/f'cad/xiao-mount/{n}.stl',col)
    report_path=ROOT/'docs/evidence/current-adjustment-v06.json';source_paths.append(report_path)
    report=json.loads(report_path.read_text())
    html=(ROOT/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    html=html.replace('3D-компоновка 0.1','Текущая компоновка 0.6')
    html=html.replace('assembly.scad','../harness-guides-assembly.scad').replace('assembly.md','README.md').replace('reference/NOTICE.md','../reference/NOTICE.md')
    html=html.replace('Габаритная проработка, не готовая машинка. Цветные блоки электроники — резерв места. Их крепления, провода, кузов и ход подвески ещё не проверены.',
                      f"Новые держатель XIAO и направляющие. {report['feasible_pose_count']} из {len(report['poses'])} сочетаний проходят проверку твёрдых тел и условного кузова. Физическая посадка не проверена.")
    html=html.replace('slides={electronics:0,camera:0}','slides={electronics:0,camera:3}')
    html=html.replace('id="camera-value">0','id="camera-value">3').replace('id="camera-slide" type="range" min="-3" max="8" step="1" value="0"','id="camera-slide" type="range" min="-3" max="8" step="1" value="3"')
    html=html.replace('part.visible=true;','part.visible=!part.hidden;').replace('input.checked=true;','input.checked=!part.hidden;')
    html=html.replace('if(!part.visible)continue;',"if(!part.visible || (part.id.startsWith('wire-') && (slides.electronics!==0 || slides.camera!==3)))continue;")
    html=html.replace("!row.mechanical_clear?'Есть пересечение креплений или механики. Такое положение не подходит.'", "!row.slot_constraints_clear?'Крепёж выходит за проверенные пределы пазов.':!row.mechanical_clear?'Есть пересечение деталей. Положение не подходит.'")
    html=html.replace("'В этом положении пересечений макетов не найдено; они внутри условного кузова.'", "!row.service_usb_solids_clear?'Детали помещаются, но доступ к сервисному USB перекрыт.':row.wire_routing_verified?'Начальное положение: проверены детали и трассы проводов; физической проверки нет.':'Детали помещаются. Для этого положения провода и обслуживание ещё нужно проверить.'")
    html=html.replace('Провода, допуски, реальный кузов и ход подвески не проверены.','Провода показаны только при 0/+3 мм. Перестановка — при выключенном питании, с освобождением фиксаторов жгута. Это не доказательство непрерывного свободного хода.')
    html=html.replace('Перетаскивание — вращение; колесо — масштаб. Разнесение деталей служит только для просмотра.','Перетаскивание — вращение; колесо — масштаб. Кузов условный, размеры реальных плат ещё нужно сверить.')
    # Keep adjustment and views visible above the long component inventory.
    controls=re.search(r'<div id="adjustment" hidden>.*?</div>',html,re.S).group()
    html=html.replace(controls,'')
    views='<button id="iso">Общий вид</button><button id="top">Сверху</button><button id="side">Сбоку</button>'
    html=html.replace(views,'')
    presets='<p><button data-pose="0,3">Начальное</button><button data-pose="2,5">Платы +2 / камера +5</button><button data-pose="5,8">Платы +5 / камера +8</button></p>'
    html=html.replace('<div id="parts"></div>',controls+presets+views+'<details><summary>Детали и видимость</summary><div id="parts"></div></details>')
    html=html.replace('__SCENE_DATA__',json.dumps(rows,ensure_ascii=False)).replace('__ADJUSTMENT_DATA__',json.dumps({'poses':report['poses']}))
    extra="""document.querySelector('#explode').closest('label').hidden=true;
document.querySelectorAll('[data-pose]').forEach(b=>b.onclick=()=>{const [e,c]=b.dataset.pose.split(',').map(Number);for(const [n,v]of [['electronics',e],['camera',c]]){const i=document.getElementById(n+'-slide');i.value=v;i.dispatchEvent(new Event('input'));}});
"""
    html=html.replace('</script></html>',extra+'</script></html>')
    (DEST/'viewer.html').write_text(html,encoding='utf-8',newline='\n')
    manifest={'version':'0.6','date':'2026-09-15','parts':len(rows),'physical_fit_verified':False,
              'sources_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in source_paths},
              'viewer_sha256':hashlib.sha256((DEST/'viewer.html').read_bytes()).hexdigest()}
    (ROOT/'docs/evidence/current-viewer-v06.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'parts':len(rows),'feasible_pairs':report['feasible_pose_count']}))

if __name__=='__main__':main()
