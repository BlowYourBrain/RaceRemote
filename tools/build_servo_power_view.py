"""Show the two proposed servo power wires and the existing D3 signal."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-power'


def main():
    source = ROOT / 'cad/servo-wiring/viewer.html'
    evidence = ROOT / 'docs/evidence/servo-power-v01.json'
    report = json.loads(evidence.read_text())
    assert report['service']['passed'] and not any(report['wire_obstacle_hits_mm3'].values())
    page = source.read_text(encoding='utf-8')
    encoded = page.split('const parts=', 1)[1].split(';\nconst adjustmentChecks=', 1)[0]
    original = json.loads(encoded)
    removed = {'servo-bundle', 'servo-old-clash'}
    assert {p['id'] for p in original} >= removed
    rows = [p for p in original if p['id'] not in removed]
    inputs = [source, evidence, Path(__file__)]
    for name, route in report['routes'].items():
        path = DEST / (name + '.stl')
        inputs.append(path)
        solid = trimesh.load_mesh(path)
        raw = np.hstack([solid.triangles.reshape(-1, 3), np.repeat(solid.face_normals, 3, axis=0)]).astype('<f4')
        rows.append(dict(id='servo-power-' + name,
                         label=f"Серво {name}: Waveshare P2.{route['source_pin']} → приблизительный конец кабеля",
                         color=route['color'], slide=None, level=0, wire=False, hidden=False,
                         count=len(raw), data=base64.b64encode(raw.tobytes()).decode()))
    assert len(rows) == len({p['id'] for p in rows})
    page = page.replace(encoded, json.dumps(rows, ensure_ascii=False), 1)
    page = page.replace('Сигнальный провод сервопривода', 'Питание и сигнал сервопривода')
    start = page.index('<p class="note">'); end = page.index('</p>', start) + 4
    page = page[:start] + '''<p class="note">Красный и тёмный провода Ø1,6 мм идут от верхних концов P2.5/P2.2 Waveshare к серво. Жёлтый — сигнал D3. Проверены препятствия и обслуживание при 0/+3. Реальные концы кабеля, разъём, крепление, ток и работа входа серво ещё не проверены. Старый общий резерв жгута заменён отдельными проводами. Конфликт клеммы мотора и незавершённый силовой вход сохраняются. Печать отложена.</p>''' + page[end:]
    page = page.replace('../../tools/build_servo_wiring.py', '../../tools/build_servo_power.py')
    output = DEST / 'viewer.html'
    output.write_text(page, encoding='utf-8', newline='\n')
    result = {'scene_objects': len(rows), 'removed_ids': sorted(removed),
              'source_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
              'artifact_sha256': {'viewer.html': hashlib.sha256(output.read_bytes()).hexdigest()}}
    (DEST / 'view-evidence.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f'Current scene: {len(rows)} objects')


if __name__ == '__main__':
    main()
