"""Check exported native connectivity independently of the drawing generator."""
import csv
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
HW = ROOT / 'hardware/xiao-logic-power'
OUT = ROOT / 'docs/evidence/xiao-logic-power-v02'
CLI = ROOT / 'build/tooling/kicad-10.0.6/bin/kicad-cli.exe'

# Separate review oracle: DRB assignment, MAX40200 SOT23 assignment, divider,
# preload before isolation, local enable and no external service-VBUS connection.
EXPECTED = {
    'U1': {'1': 'LDO_4V', '2': 'NC', '3': 'FB', '4': 'DRIVE_GND',
           '5': 'WAVE_5V', '6': 'NC', '7': 'NC', '8': 'WAVE_5V', '9': 'DRIVE_GND'},
    'U2': {'1': 'LDO_4V', '2': 'DRIVE_GND', '3': 'LDO_4V',
           '4': 'NC', '5': 'XIAO_BAT'},
    'R1': {'1': 'LDO_4V', '2': 'FB'}, 'R2': {'1': 'FB', '2': 'DRIVE_GND'},
    'R3': {'1': 'LDO_4V', '2': 'DRIVE_GND'},
    'C1': {'1': 'WAVE_5V', '2': 'DRIVE_GND'}, 'C2': {'1': 'LDO_4V', '2': 'DRIVE_GND'},
    'C3': {'1': 'LDO_4V', '2': 'DRIVE_GND'}, 'C4': {'1': 'XIAO_BAT', '2': 'DRIVE_GND'},
    'J1': {'1': 'WAVE_5V'}, 'J2': {'1': 'DRIVE_GND'}, 'J3': {'1': 'XIAO_BAT'},
    'J5': {'1': 'DRIVE_GND'},
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    subprocess.run([sys.executable, str(ROOT / 'tools/calculate_xiao_logic_power.py')], check=True, stdout=subprocess.DEVNULL)
    sch = str(HW / 'xiao-logic-power.kicad_sch')
    for args in [
        ['sch', 'erc', sch, '--format', 'json', '-o', str(OUT / 'erc.json')],
        ['sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', str(OUT / 'netlist.xml')],
        ['sch', 'export', 'pdf', sch, '-o', str(HW / 'schematic.pdf')],
    ]:
        subprocess.run([str(CLI), *args], check=True)
    actual = {}
    for net in ET.parse(OUT / 'netlist.xml').findall('.//nets/net'):
        name = net.attrib['name']
        nodes = net.findall('node')
        for node in nodes:
            ref, pin = node.attrib['ref'], node.attrib['pin']
            if ref.startswith('#'):
                continue
            if name.startswith('unconnected-'):
                assert len(nodes) == 1 and '+no_connect' in node.attrib['pintype']
                value = 'NC'
            else:
                value = name
            assert pin not in actual.setdefault(ref, {})
            actual[ref][pin] = value
    assert actual == EXPECTED, ('Native netlist differs from reviewed mapping', actual)
    with (HW / 'connections.csv').open(encoding='utf-8', newline='') as f:
        exported = {}
        for row in csv.DictReader(f):
            exported.setdefault(row['reference'], {})[row['pin']] = row['net']
    assert exported == EXPECTED, 'Human connection list differs from native schematic'
    erc = json.loads((OUT / 'erc.json').read_text())
    violations = [v for sheet in erc['sheets'] for v in sheet['violations']]
    assert not violations, violations
    artifacts = sorted(p for p in HW.iterdir() if p.suffix in ('.kicad_sch', '.kicad_sym', '.csv', '.kicad_pro') or p.name == 'sym-lib-table')
    artifacts += [ROOT / 'tools' / n for n in ('build_xiao_logic_power.py', 'calculate_xiao_logic_power.py', 'verify_xiao_logic_power.py')]
    artifacts += [OUT / 'dc-calculation.json']
    report = {
        'date': '2026-09-15', 'contract': 'XIAO-LOGIC-01 v0.2',
        'positions': len(actual), 'pins_including_nc': sum(map(len, actual.values())),
        'connected_pins': sum(n != 'NC' for pins in actual.values() for n in pins.values()),
        'named_nets': sorted({n for pins in actual.values() for n in pins.values()} - {'NC'}),
        'native_netlist_and_csv_match_review_oracle': True,
        'kicad_version': erc['kicad_version'], 'erc_violations': 0,
        'reviewed_erc_exception': None,
        'unexpected_erc_violations': 0, 'ignored_checks': erc['ignored_checks'],
        'physical_tests': False, 'pcb_exists': False, 'fabrication_released': False,
        'artifact_sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in artifacts},
    }
    (OUT / 'summary.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('Verified 13 positions, 32 pins including 4 NC, 5 named nets. ERC: 0; no USB sense wire. No physical proof.')


if __name__ == '__main__':
    main()
