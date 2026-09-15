"""Update the last full offline CAD scene with the Waveshare output wire pair."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/buck-output-harness'


def main():
    source=ROOT/'cad/xiao-power-harness/viewer.html'
    report=ROOT/'docs/evidence/buck-output-harness-v01.json'
    r=json.loads(report.read_text());assert r['service']['passed'] and not any(r['wire_obstacle_hits_mm3'].values())
    original=source.read_text(encoding='utf-8')
    data=original.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0]
    rows=json.loads(data);assert sum(p['id']=='power-H8' for p in rows)==1
    rows=[p for p in rows if p['id']!='power-H8']
    inputs=[source,report,Path(__file__)]
    for name,label,color in [('5V','Waveshare VOUT → H8: 5 В',[.95,.38,.08]),
                             ('GND','Waveshare GND → H3: общий провод',[.22,.19,.16])]:
        path=DEST/(name+'.stl');inputs.append(path);m=trimesh.load_mesh(path)
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='buck-output-'+name,label=label,color=color,slide=None,level=0,wire=True,hidden=False,
                         count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=original.replace(data,json.dumps(rows,ensure_ascii=False),1).replace('Проводка питания XIAO','Выход Waveshare и питание XIAO')
    begin=page.index('<p class="note">');end=page.index('</p>',begin)+4
    page=page[:begin]+'''<p class="note">Добавлена пара от выходного гребня Waveshare к общей плате; питание XIAO сохранено. Только положение 0/+3. Снятие условного кузова и выход макета батареи проверены в CAD. Ответный разъём, защита от переполюсовки, фиксация и реальные провода ещё не выбраны. Для обзора скройте кузов флажком.</p>'''+page[end:]
    page=page.replace('../../tools/build_xiao_power_harness.py','../../tools/build_buck_output_harness.py')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    (DEST/'view-evidence.json').write_text(json.dumps({
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        'artifact_sha256':{'viewer.html':hashlib.sha256((DEST/'viewer.html').read_bytes()).hexdigest()},
        'scene_objects':len(rows),'removed_ids':['power-H8'],'added_ids':['buck-output-5V','buck-output-GND']},indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Generated full scene:',len(rows),'objects')


if __name__=='__main__':main()
