"""Show the expanded servo cable reserve and its inherited C2 counterexample."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-bundle-study'


def main():
    source = ROOT / 'cad/xiao-return-h10/viewer.html'
    evidence = ROOT / 'docs/evidence/servo-bundle-v01.json'
    report = json.loads(evidence.read_text())
    assert report['service']['passed'] and not report['new_hits_mm3']
    page = source.read_text(encoding='utf-8')
    encoded = page.split('const parts=', 1)[1].split(';\nconst adjustmentChecks=', 1)[0]
    rows = json.loads(encoded)
    assert sum(r['id'] == 'wire-servo' for r in rows) == 1
    rows = [r for r in rows if r['id'] != 'wire-servo']
    inputs = [source, evidence, Path(__file__)]
    specs = [('servo-bundle', 'bundle.stl', 'Резерв жгута серво Ø3,3 мм; концы пока не назначены', [.1, .4, .85], False),
             ('servo-old-clash', 'old-bundle.stl', 'Старый коридор Ø1,8 мм: проходит сквозь C2', [1, .05, .05], True)]
    for name, file, label, color, hidden in specs:
        path = DEST / file
        inputs.append(path)
        mesh = trimesh.load_mesh(path)
        raw = np.hstack([mesh.triangles.reshape(-1, 3), np.repeat(mesh.face_normals, 3, axis=0)]).astype('<f4')
        rows.append(dict(id=name, label=label, color=color, slide=None, level=0, wire=False,
                         hidden=hidden, count=len(raw), data=base64.b64encode(raw.tobytes()).decode()))
    page = page.replace(encoded, json.dumps(rows, ensure_ascii=False), 1)
    page = page.replace('Отдельный общий провод XIAO', 'Резерв жгута сервопривода')
    start = page.index('<p class="note">'); end = page.index('</p>', start) + 4
    page = page[:start] + '''<p class="note">Три провода серво: резерв Ø3,3 мм идёт вдоль правого края, обходя C2 и крышку. Проверены неподвижные детали и снятие кузова/батареи при 0/+3. Это место для жгута: выводы, разъём, изгибы и реальный кабель ещё не определены. Красный переключатель показывает прежнее пересечение с C2. Конфликт клеммы мотора A и незавершённый силовой вход сохраняются. Печать отложена.</p>''' + page[end:]
    page = page.replace('../../tools/build_xiao_return_h10.py', '../../tools/build_servo_bundle_study.py')
    (DEST / 'viewer.html').write_text(page, encoding='utf-8', newline='\n')
    result = {'scene_objects': len(rows), 'removed_ids': ['wire-servo'],
              'source_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
              'artifact_sha256': {'viewer.html': hashlib.sha256((DEST / 'viewer.html').read_bytes()).hexdigest()}}
    (DEST / 'view-evidence.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f'Current scene: {len(rows)} objects')


if __name__ == '__main__':
    main()
