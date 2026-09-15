"""Display the relocated servo power pair and removable lacing in the existing cap."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'cad/servo-lacing'


def main():
    source = ROOT / 'cad/servo-power/viewer.html'
    evidence = ROOT / 'docs/evidence/servo-lacing-v01.json'
    report = json.loads(evidence.read_text())
    assert report['service']['passed'] and not report['cord_hits_mm3'] and not report['knot_hits_mm3']
    page = source.read_text(encoding='utf-8')
    encoded = page.split('const parts=', 1)[1].split(';\nconst adjustmentChecks=', 1)[0]
    rows = json.loads(encoded)
    inputs = [source, evidence, Path(__file__)]
    def geometry(file):
        path = DEST / file
        inputs.append(path)
        solid = trimesh.load_mesh(path)
        raw = np.hstack([solid.triangles.reshape(-1, 3), np.repeat(solid.face_normals, 3, axis=0)]).astype('<f4')
        return {'count': len(raw), 'data': base64.b64encode(raw.tobytes()).decode()}
    for name in ('VOUT', 'GND'):
        row = next(p for p in rows if p['id'] == 'servo-power-' + name)
        row.update(geometry(name + '.stl'))
        row['label'] += ' — через седло прижима'
    for name, label, color in [('cord', 'Съёмная вязка Ø0,8 мм; натяжение не проверено', [.6, .15, .75]),
                               ('knot-reserve', 'Место под узел 3×3×2 мм; форма узла не задана', [.4, .05, .55])]:
        rows.append(dict(id='servo-lacing-' + name, label=label, color=color, slide=None, level=0,
                         wire=False, hidden=False, **geometry(name + '.stl')))
    assert len(rows) == len({p['id'] for p in rows})
    page = page.replace(encoded, json.dumps(rows, ensure_ascii=False), 1)
    page = page.replace('Питание и сигнал сервопривода', 'Фиксация проводов сервопривода')
    start = page.index('<p class="note">'); end = page.index('</p>', start) + 4
    page = page[:start] + '''<p class="note">Пара питания проходит через существующее седло верхнего прижима. Фиолетовая петля — съёмный шнур, кубик — место под узел. Печатные детали не изменены. Геометрия и обслуживание проверены при 0/+3; удержание при рывке, затяжка, изоляция и осевое скольжение не проверены. Перед снятием прижима вязку нужно освободить. Конфликт клеммы мотора и незавершённое питание сохраняются. Печать отложена.</p>''' + page[end:]
    page = page.replace('../../tools/build_servo_power.py', '../../tools/build_servo_lacing.py')
    output = DEST / 'viewer.html'
    output.write_text(page, encoding='utf-8', newline='\n')
    result = {'scene_objects': len(rows), 'replaced_wire_ids': ['servo-power-VOUT', 'servo-power-GND'],
              'source_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
              'artifact_sha256': {'viewer.html': hashlib.sha256(output.read_bytes()).hexdigest()}}
    (DEST / 'view-evidence.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(f'Current scene: {len(rows)} objects')


if __name__ == '__main__':
    main()
