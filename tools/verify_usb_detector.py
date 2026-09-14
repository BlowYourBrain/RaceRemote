"""Check PI3USB9201 interface wiring, not USB classification or analog behavior."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/usb-detector'
EVIDENCE = ROOT / 'docs/evidence'
CLI = Path(sys.executable).with_name('kicad-cli.exe' if sys.platform == 'win32' else 'kicad-cli')

# Independently transcribed DS41358 Rev3-2 p2 numeric package pins.
IC_NETS = {1: 'unconnected-(U301-USB+-Pad1)', 2: 'unconnected-(U301-USB--Pad2)',
           3: 'BC_SCL', 4: 'BC_SDA', 5: 'BC_INT_N', 6: 'unconnected-(U301-NC-Pad6)',
           7: 'PORT_DM', 8: 'PORT_DP', 9: 'USB_GND', 10: 'USB_GND',
           11: 'BC_EN_N', 12: 'AUX_3V3'}
CONNECTIONS = {'C301': ('AUX_3V3', 'USB_GND'), 'R301': ('AUX_3V3', 'BC_EN_N'),
               'R302': ('AUX_3V3', 'BC_SCL'), 'R303': ('AUX_3V3', 'BC_SDA'),
               'R304': ('AUX_3V3', 'BC_INT_N'), 'J301': ('AUX_3V3', 'USB_GND'),
               'J302': ('PORT_DP', 'PORT_DM'),
               'J303': ('BC_SCL', 'BC_SDA', 'BC_INT_N', 'BC_EN_N')}
VALUES = {'U301': 'PI3USB9201ZTAEX', 'C301': '100n / 16V', 'R301': '47k / 1%',
          'R302': '4.7k / 1%', 'R303': '4.7k / 1%', 'R304': '10k / 1%',
          'J301': 'FROM USB AUX', 'J302': 'USB PORT DATA', 'J303': 'TO SOURCE MCU'}


def run(*args):
    result = subprocess.run([str(CLI), *map(str, args)], cwd=ROOT, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stdout + result.stderr)


def read_netlist(path):
    root = ET.parse(path).getroot()
    pins = {(p.attrib['ref'], p.attrib['pin']): n.attrib['name']
            for n in root.findall('./nets/net') for p in n.findall('node')}
    values = {c.attrib['ref']: c.findtext('value') for c in root.findall('./components/comp')}
    return pins, values


def validate(pins, values):
    expected = {('U301', str(pin)): net for pin, net in IC_NETS.items()}
    for ref, nets in CONNECTIONS.items():
        expected.update({(ref, str(i + 1)): net for i, net in enumerate(nets)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'
    # No external participant on the unused USB transceiver pins; no charger-core path.
    for net, nodes in [('PORT_DP', {('U301', '8'), ('J302', '1')}),
                       ('PORT_DM', {('U301', '7'), ('J302', '2')})]:
        assert {pin for pin, name in pins.items() if name == net} == nodes


def main():
    sch = DEST / 'usb-detector.kicad_sch'
    erc = EVIDENCE / 'usb-detector-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    net = DEST / 'usb-detector.net'
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    validate(*read_netlist(net))
    with (DEST / 'bom.csv').open(encoding='utf-8-sig', newline='') as f:
        rows = list(csv.DictReader(f))
    assert len(rows) == len(VALUES)
    assert {r['reference']: r['value'] for r in rows} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST / 'usb-detector.pdf')

    original = sch.read_text(encoding='utf-8')
    faults = []
    for name, old, new, position in [
        ('connector_dp_swapped_to_dm', 'PORT_DP', 'PORT_DM', '165.1 243.84 180'),
        ('enable_pullup_to_ground', 'AUX_3V3', 'USB_GND', '50.8 168.91 0'),
        ('supply_connector_raw_vbus', 'AUX_3V3', 'USB_RAW_VBUS', '50.8 243.84 180')]:
        token = f'(global_label "{old}" (shape passive) (at {position})'
        assert original.count(token) == 1, (name, token)
        folder = ROOT / 'build/usb-detector/negative-controls' / name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder / sch.name
        changed.write_text(original.replace(token, token.replace('"' + old + '"', '"' + new + '"', 1)), encoding='utf-8')
        for file in ['usb-detector.kicad_pro', 'RaceRemote_Detector.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(DEST / file, folder / file)
        exported = folder / 'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try:
            validate(*read_netlist(exported))
        except AssertionError as error:
            faults.append({'mutation': name, 'detected': True, 'failed_contract': str(error)})
        else:
            raise AssertionError('Undetected fault: ' + name)

    # Datasheet p4 ENB leakage/threshold, pp5-6 I2C/INT. Static resistor arithmetic only.
    en_high_min = 3.0 - 5e-6 * 47000 * 1.01
    int_sink_max = 3.6 / (10000 * .99)
    bus_sink_max = 3.6 / (4700 * .99)
    assert en_high_min > 1.05 and int_sink_max < .003 and bus_sink_max < .020
    arithmetic = {'en_high_min_V': en_high_min, 'int_pullup_max_A': int_sink_max,
                  'each_i2c_pullup_max_A': bus_sink_max,
                  'illustrative_rise_time_100pF_ns': .8473 * 4700 * 1.01 * 100e-12 * 1e9,
                  'scope': 'Static 3.0..3.6V, 1% resistor model at datasheet leakage conditions; '
                           '100pF is assumed, not measured; no brownout/timing or total AUX budget proof'}
    files = [DEST / n for n in ['usb-detector.kicad_sch', 'usb-detector.kicad_pro',
             'RaceRemote_Detector.kicad_sym', 'usb-detector.net', 'usb-detector.pdf', 'bom.csv']]
    files += [erc, ROOT / 'tools/build_usb_detector.py', Path(__file__)]
    report = {'date': '2026-09-14', 'contract': 'USB-BC-01 v0.1',
              'kicad_version': erc_data['kicad_version'], 'erc_violations': 0,
              'ic_pins_checked': 12, 'logical_components': len(VALUES),
              'fault_injections': faults, 'arithmetic': arithmetic,
              'sha256': {f.relative_to(ROOT).as_posix(): hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
              'not_verified': ['MCU selection/firmware and autonomous charge permission',
                               'CC attachment, VBUS qualification and detach/reset/brownout handling',
                               'Real source classification, timeout and total AUX consumption',
                               'ESD, footprints, PCB, assembly and full charger integration']}
    (EVIDENCE / 'usb-detector-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: report[k] for k in ['erc_violations', 'ic_pins_checked', 'logical_components', 'fault_injections', 'arithmetic']}))


if __name__ == '__main__':
    main()
