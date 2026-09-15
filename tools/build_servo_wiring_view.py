"""Add the referenced servo signal to the existing nominal assembly view."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-wiring'


def main():
    source = ROOT / 'cad/servo-bundle-study/viewer.html'
    evidence = ROOT / 'docs/evidence/servo-wiring-v01.json'
    report = json.loads(evidence.read_text())
    assert report['service']['passed'] and not report['new_hits_mm3']
    page = source.read_text(encoding='utf-8')
    encoded = page.split('const parts=', 1)[1].split(';\nconst adjustmentChecks=', 1)[0]
    rows = json.loads(encoded)
    reserve = next(r for r in rows if r['id'] == 'servo-bundle')
    reserve['hidden'] = True
    path = DEST / 'signal.stl'
    solid = trimesh.load_mesh(path)
    raw = np.hstack([solid.triangles.reshape(-1, 3), np.repeat(solid.face_normals, 3, axis=0)]).astype('<f4')
    rows.append(dict(id='servo-signal', label='Сигнал D3/GPIO4 → приблизительный выход кабеля серво',
                     color=[1, .65, .04], slide=None, level=0, wire=False, hidden=False,
                     count=len(raw), data=base64.b64encode(raw.tobytes()).decode()))
    assert len({r['id'] for r in rows}) == len(rows)
    page = page.replace(encoded, json.dumps(rows, ensure_ascii=False), 1)
    page = page.replace('Резерв жгута сервопривода', 'Сигнальный провод сервопривода')
    start = page.index('<p class="note">'); end = page.index('</p>', start) + 4
    page = page[:start] + '''<p class="note">Жёлтый провод Ø0,8 мм начинается на D3/GPIO4 и проходит под расширением камеры. Конец возле серво приблизительный; разъём, питание и крепление ещё не определены. Проверены препятствия и обслуживание при 0/+3. Синий резерв всего жгута можно включить отдельно. Конфликт клеммы мотора и незавершённый силовой вход сохраняются. Печать отложена.</p>''' + page[end:]
    page = page.replace('../../tools/build_servo_bundle_study.py', '../../tools/build_servo_wiring.py')
    output = DEST / 'viewer.html'
    output.write_text(page, encoding='utf-8', newline='\n')
    result = {'scene_objects': len(rows), 'source_sha256': {
        p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
        for p in [Path(__file__), source, evidence, path]},
        'artifact_sha256': {'viewer.html': hashlib.sha256(output.read_bytes()).hexdigest()}}
    (DEST / 'view-evidence.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f'Current scene: {len(rows)} objects')


if __name__ == '__main__':
    main()
