"""Show the SM space study without pretending the earlier output wires still apply."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/logic-connector-study'


def main():
    source=ROOT/'cad/buck-output-harness/viewer.html'
    report=ROOT/'docs/evidence/logic-connector-fit-v01.json'
    r=json.loads(report.read_text());assert r['selected_pose']['fits']
    original=source.read_text(encoding='utf-8')
    data=original.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0]
    rows=json.loads(data);removed={'buck_plug','buck-output-5V','buck-output-GND'}
    assert removed.issubset({p['id'] for p in rows})
    rows=[p for p in rows if p['id'] not in removed]
    path=DEST/'sm-envelope.stl';m=trimesh.load_mesh(path)
    raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
    rows.append(dict(id='sm-envelope',label='Габарит пары SM с запасом: 23,4 × 8,5 × 9,2 мм',
                     color=[.7,.18,.56],slide=None,level=0,wire=False,hidden=False,
                     count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=original.replace(data,json.dumps(rows,ensure_ascii=False),1).replace('Выход Waveshare и питание XIAO','Место под разъём SM')
    begin=page.index('<p class="note">');end=page.index('</p>',begin)+4
    page=page[:begin]+'''<p class="note">Отдельное исследование места: фиолетовый блок — габарит разъёма SM по каталогу JST с запасом. Он помещается поперёк платы Waveshare. Корпус держателя, защита от натяжения, новые провода и обслуживание ещё не проверены. Прежние выходная пара и разъём на гребень удалены из этой сцены. Это не готовая разводка и не проверка покупки Scondar.</p>'''+page[end:]
    page=page.replace('../../tools/build_buck_output_harness.py','../../tools/check_logic_connector_fit.py')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    inputs=[source,report,path,Path(__file__)]
    (DEST/'view-evidence.json').write_text(json.dumps({
        'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
        'artifact_sha256':{'viewer.html':hashlib.sha256((DEST/'viewer.html').read_bytes()).hexdigest()},
        'scene_objects':len(rows),'removed_ids':sorted(removed)},indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Generated connector-study scene:',len(rows),'objects')


if __name__=='__main__':main()
