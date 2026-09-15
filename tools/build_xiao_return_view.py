"""Current motor scene with PCB0.3 and the dedicated XIAO return."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/xiao-return-h10'


def main():
    source=ROOT/'cad/motor-harness/viewer.html'
    report=ROOT/'docs/evidence/xiao-return-h10-v01.json'
    evidence=json.loads(report.read_text());assert evidence['service']['passed']
    page=source.read_text(encoding='utf-8')
    data=page.split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0]
    rows=json.loads(data);removed={'power-pcb','harness-GND'}
    assert removed.issubset({r['id'] for r in rows})
    rows=[r for r in rows if r['id'] not in removed]
    specs=[('power-pcb','cad/drive-power-carrier/pcb.stl','Плата 0.3 с отверстием H10',[.13,.48,.28]),
           ('xiao-return','cad/xiao-return-h10/GND.stl','GND0 XIAO → H10 снизу; H5 свободна для питания',[.45,.2,.65])]
    inputs=[source,report,Path(__file__)]
    for name,relative,label,color in specs:
        path=ROOT/relative;inputs.append(path);m=trimesh.load_mesh(path)
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id=name,label=label,color=color,slide=None,level=0,wire=name!='power-pcb',
                         hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page=page.replace(data,json.dumps(rows,ensure_ascii=False),1).replace('Проводка мотора: примерка','Отдельный общий провод XIAO')
    start=page.index('<p class="note">');end=page.index('</p>',start)+4
    page=page[:start]+'''<p class="note">Плата 0.3: общий провод XIAO идёт снизу к H10. Площадка H5 освобождена для силового возврата. Новая трасса и обслуживание проверены при 0/+3. Питание от аккумулятора ещё не разведено; прежний конфликт резерва клеммы мотора A с подвеской остаётся открытым. Печать и монтаж не выполнялись.</p>'''+page[end:]
    page=page.replace('../../tools/build_motor_harness.py','../../tools/build_xiao_return_h10.py')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    result={'scene_objects':len(rows),'removed_ids':sorted(removed),
            'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
            'artifact_sha256':{'viewer.html':hashlib.sha256((DEST/'viewer.html').read_bytes()).hexdigest()}}
    (DEST/'view-evidence.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Current scene:',len(rows),'objects')


if __name__=='__main__':main()
