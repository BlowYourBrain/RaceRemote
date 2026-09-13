"""Native KiCad ERC/DRC/parity, exported netlist and fault-injection checks.

Run with KiCad Python. Mutated boards are written only to ignored build/.
No hardware is powered by this tool.
"""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

import pcbnew as p

ROOT = Path(__file__).resolve().parents[1]
PORT = ROOT / 'hardware/usb-port'
EVIDENCE = ROOT / 'docs/evidence'
CLI = Path(sys.executable).with_name('kicad-cli.exe' if sys.platform == 'win32' else 'kicad-cli')


def run(*args, allow_violations=False):
    result = subprocess.run([str(CLI), *map(str, args)], cwd=ROOT,
                            capture_output=True, text=True)
    if result.returncode and not allow_violations:
        raise RuntimeError(result.stdout + result.stderr)
    return result


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


def main():
    erc = EVIDENCE / 'usb-port-erc.json'
    drc = EVIDENCE / 'usb-port-pcb-drc.json'
    netlist = PORT / 'usb-port.net'
    run('sch', 'erc', PORT / 'usb-port.kicad_sch', '--format', 'json',
        '--severity-all', '--exit-code-violations', '-o', erc)
    run('pcb', 'drc', PORT / 'usb-port.kicad_pcb', '--schematic-parity',
        '--format', 'json', '--all-track-errors', '--refill-zones',
        '--exit-code-violations', '-o', drc)
    run('sch', 'export', 'netlist', PORT / 'usb-port.kicad_sch', '--format', 'kicadxml', '-o', netlist)
    # Do not rely only on CLI exit code; inspect every report section.
    assert not any(s['violations'] for s in read(erc)['sheets']), 'ERC violations'
    for section in ['violations', 'unconnected_items', 'schematic_parity']:
        assert not read(drc)[section], section

    b = p.LoadBoard(str(PORT / 'usb-port.kicad_pcb'))
    actual = {}
    for f in b.GetFootprints():
        actual[f.GetReference()] = sorted(
            [{'pad': q.GetNumber(), 'net': q.GetNetname()} for q in f.Pads()],
            key=lambda a: (a['pad'], a['net']))
    xml = ET.parse(netlist).getroot()
    expected = {}
    for net in xml.findall('./nets/net'):
        for node in net.findall('node'):
            expected[(node.attrib['ref'], node.attrib['pin'])] = net.attrib['name']
    assert len(expected) == 27, 'Unexpected logical pin count'
    seen = set()
    for ref, pads in actual.items():
        for pad in pads:
            if pad['pad']:
                key = (ref, pad['pad'])
                assert expected[key] == pad['net'], (key, expected[key], pad['net'])
                seen.add(key)
    assert seen == set(expected), 'Missing PCB pin'
    assert {c.attrib['ref'] for c in xml.findall('./components/comp')} == set(actual)

    # Independent USB-PORT-01 terminal contract, not copied from either generator.
    for ref, net in [('H1', 'VBUS'), ('H2', 'DP'), ('H3', 'CC1'),
                     ('H4', 'GND'), ('H5', 'DM'), ('H6', 'CC2')]:
        assert expected[(ref, '1')] == net, (ref, net)
    assert {expected[('R1', '1')], expected[('R1', '2')]} == {'CC1', 'GND'}
    assert {expected[('R2', '1')], expected[('R2', '2')]} == {'CC2', 'GND'}

    # Retained CAD geometry remains applicable: electrical metadata changes must
    # not move pads/drills or change the conservative exported dimensions.
    geometry = read(PORT / 'geometry.json')
    checks = 0
    for record in geometry['holes'] + geometry['copper']:
        f = b.FindFootprintByReference(record['reference'])
        matches = []
        for q in f.Pads():
            xy = [55.425 + p.ToMM(q.GetPosition().y) - 100,
                  59 + p.ToMM(q.GetPosition().x) - 100]
            if q.GetNumber() == record['pad'] and all(abs(a-c) < 1e-6 for a, c in zip(xy, record['center_mm'])):
                matches.append(q)
        assert matches, record
        for q in matches:
            for key, v in [('size_mm', q.GetSize()), ('drill_mm', q.GetDrillSize())]:
                assert all(abs(a-c) < 1e-6 for a, c in zip([p.ToMM(v.y), p.ToMM(v.x)], record[key])), record
        checks += 1

    faults = []
    scratch = ROOT / 'build/usb-port-pcb/negative-controls'
    for case in ['swapped_cc_terminal', 'missing_rd']:
        folder = scratch / case
        folder.mkdir(parents=True, exist_ok=True)
        for name in ['usb-port.kicad_sch', 'usb-port.kicad_pro', 'RaceRemote_USB.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(PORT / name, folder / name)
        shutil.copytree(PORT / 'RaceRemote_USB.pretty', folder / 'RaceRemote_USB.pretty', dirs_exist_ok=True)
        mutated = p.LoadBoard(str(PORT / 'usb-port.kicad_pcb'))
        if case == 'swapped_cc_terminal':
            next(iter(mutated.FindFootprintByReference('H3').Pads())).SetNet(mutated.FindNet('CC2'))
            wanted = 'net_conflict'
        else:
            mutated.Remove(mutated.FindFootprintByReference('R1'))
            wanted = 'missing_footprint'
        board_path = folder / 'usb-port.kicad_pcb'
        p.SaveBoard(str(board_path), mutated)
        report = folder / 'drc.json'
        report.unlink(missing_ok=True)  # A failed CLI must not reuse an old success.
        run('pcb', 'drc', board_path, '--schematic-parity', '--format', 'json',
            '--exit-code-violations', '-o', report, allow_violations=True)
        issues = read(report)['schematic_parity']
        assert any(v['type'] == wanted for v in issues), (case, issues)
        faults.append({'mutation': case, 'detected': True, 'parity_types': sorted({v['type'] for v in issues}),
                       'report_sha256': hashlib.sha256(report.read_bytes()).hexdigest()})

    paths = [PORT / n for n in ['usb-port.kicad_pcb', 'usb-port.kicad_pro', 'usb-port.kicad_sch',
                               'RaceRemote_USB.kicad_sym', 'usb-port.net', 'geometry.json']]
    paths += [erc, drc, ROOT / 'cad/components/usb-port-pcb.scad', Path(__file__)]
    paths += [ROOT / 'tools' / n for n in ['usb_port_identity.py', 'build_usb_port_pcb.py',
                                          'build_usb_port_schematic.py']]
    paths += sorted((PORT / 'RaceRemote_USB.pretty').glob('*.kicad_mod'))
    report = {'kicad_version': p.Version(),
              'checks': {'erc_violations': 0, 'drc_violations': 0, 'unconnected_items': 0,
                         'schematic_parity_issues': 0, 'logical_pins_compared': len(expected),
                         'separate_cc_resistors': True, 'cad_pad_drill_records_matched': checks},
              'actual_pad_nets': dict(sorted(actual.items())), 'negative_controls': faults,
              'sha256': {str(path.relative_to(ROOT)).replace('\\', '/'): hashlib.sha256(path.read_bytes()).hexdigest()
                         for path in paths},
              'not_verified': ['Actual soldered components', 'USB source recognition by charger',
                               'Current and temperature', 'ESD and harness strain relief',
                               'Complete 2S charger circuit']}
    (EVIDENCE / 'usb-port-pcb-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report['checks']))
    print(json.dumps(faults))


if __name__ == '__main__':
    main()
