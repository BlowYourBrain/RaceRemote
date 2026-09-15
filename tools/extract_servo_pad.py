"""Locate D3 in the pinned Seeed PCB and cross-check the existing CAD transform."""
import hashlib
import json
from pathlib import Path
import re
import pcbnew as pcb

ROOT = Path(__file__).resolve().parents[1]


def main():
    prior_path = ROOT / 'cad/control-harness/xiao-pad-reference.json'
    prior = json.loads(prior_path.read_text(encoding='utf-8'))
    vendor = next((ROOT / 'build/xiao-control-harness/vendor').rglob('*.kicad_pcb'))
    assert hashlib.sha256(vendor.read_bytes()).hexdigest() == prior['vendor_pcb_sha256']
    board = pcb.LoadBoard(str(vendor))
    edge = next(f for f in board.GetFootprints() if f.GetReference() == 'U9')
    mount_path = ROOT / 'docs/evidence/xiao-mount-v01.json'
    bounds = json.loads(mount_path.read_text())['source_bounding_boxes_mm']['37']
    cx, cz = [(bounds[0][i] + bounds[1][i]) / 2 for i in (0, 2)]
    ox, oy = pcb.ToMM(edge.GetPosition().x), pcb.ToMM(edge.GetPosition().y)
    def extract(number, suffix):
        pads = [a for a in edge.Pads() if a.GetNumber() == number
                and a.GetAttribute() == pcb.PAD_ATTRIB_PTH
                and abs(pcb.ToMM(a.GetSize().x) - 1.4) < .001]
        assert len(pads) == 1
        pad = pads[0]
        assert pad.GetNetname().endswith(suffix)
        x, y = pcb.ToMM(pad.GetPosition().x), pcb.ToMM(pad.GetPosition().y)
        return {'vendor_ref': 'U9', 'pin': number, 'net': pad.GetNetname(),
                'vendor_xy_mm': [x, y], 'pad_diameter_mm': 1.4,
                'candidate_xyz_mm': [round(cx + oy - y, 5), 83.25, round(cz + x - ox, 5)]}
    for name, old in prior['pads'].items():
        assert extract(old['pin'], old['net']) == old, name
    pad = extract('4', 'D3{slash}A3')
    firmware = ROOT.parent / 'RC_CAR_ESP32/board/xiao_sense.h'
    variant = Path('C:/Users/Evgeny/.platformio/packages/framework-arduinoespressif32/variants/XIAO_ESP32S3/pins_arduino.h')
    assert re.search(r'\bservo_signal\s*=\s*D3\s*;', firmware.read_text())
    assert re.search(r'\bD3\s*=\s*4\s*;', variant.read_text())
    result = {'contract': 'SERVO-WIRING-01 v0.1', 'date': '2026-09-15',
              'source_label': 'D3', 'gpio': 4, 'pad': pad,
              'physical_pad_identification': False,
              'vendor_url': prior['vendor_url'], 'vendor_pcb_sha256': prior['vendor_pcb_sha256'],
              'limitations': [prior['archive_label_mismatch'],
                             'PCB1.5 coordinates transferred to STEP2023; sample revision unverified',
                             'Signal only; 3.3V input threshold and unpowered-servo behavior unqualified'],
              'source_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                                for p in [Path(__file__), prior_path, mount_path]},
              'firmware_profile_sha256': hashlib.sha256(firmware.read_bytes()).hexdigest(),
              'arduino_variant_sha256': hashlib.sha256(variant.read_bytes()).hexdigest()}
    dest = ROOT / 'cad/servo-wiring'
    dest.mkdir(exist_ok=True)
    (dest / 'pad-reference.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(pad, indent=2))


if __name__ == '__main__':
    main()
