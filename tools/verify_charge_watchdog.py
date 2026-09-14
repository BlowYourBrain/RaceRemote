"""Verify watchdog wiring and settled-rail fault/latch sequences, not hardware."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
from verify_usb_detector import ROOT, read_netlist, run
from verify_charge_permit import validate as validate_permit, IdealLatch, ideal_cd

DEST = ROOT / 'hardware/charge-watchdog'
EVIDENCE = ROOT / 'docs/evidence'
# TI SNVSB66A table 5-1; thermal pad has local logical number 9.
IC_NETS = {1: 'AUX_3V3', 2: 'WD_TIMEOUT_SELECT', 3: 'AUX_3V3',
           4: 'USB_GND', 5: 'AUX_3V3', 6: 'WD_HEARTBEAT',
           7: 'FAULT_BUS_N', 8: 'FAULT_BUS_N', 9: 'USB_GND'}
PAIRS = {'C501': ('AUX_3V3', 'USB_GND'),
         'R501': ('AUX_3V3', 'WD_TIMEOUT_SELECT'),
         'R502': ('WD_HEARTBEAT', 'USB_GND'),
         'J501': ('AUX_3V3', 'USB_GND'),
         'J502': ('WD_HEARTBEAT', 'USB_GND'),
         'J503': ('FAULT_BUS_N', 'USB_GND')}
VALUES = {'U501': 'TPS3431SDRBR', 'C501': '100n / 16V',
          'R501': '10k / 1%', 'R502': '47k / 1%', 'J501': 'FROM USB AUX',
          'J502': 'FROM MCU', 'J503': 'TO PERMIT J402'}


def validate(pins, values):
    expected = {('U501', str(p)): n for p, n in IC_NETS.items()}
    expected.update({(r, str(i + 1)): n for r, ns in PAIRS.items() for i, n in enumerate(ns)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'


class ReadyWatchdog:
    """1ms discrete model starting AFTER successful initialization, valid rails.

    No startup delay, analog thresholds, propagation or brownout simulation.
    Falling edges during an asserted fault are ignored (TI table 5-1).
    """
    def __init__(self, timeout, reset):
        self.timeout, self.reset = timeout, reset
        self.deadline, self.release, self.previous = timeout, None, 0

    def step(self, time, level):
        falling = self.previous == 1 and level == 0
        self.previous = level
        if self.release is not None:
            if time < self.release:
                return True
            self.release, self.deadline = None, time + self.timeout
            return False
        # Expiry wins over an edge exactly at the deadline.
        if time >= self.deadline:
            self.release = time + self.reset
            return True
        if falling:
            self.deadline = time + self.timeout
        return False


def traces(pins):
    rows = []
    for timeout in [170, 200, 230]:
        for reset in [170, 200, 230]:
            for mode in ['normal', 'missing', 'stuck_low', 'stuck_high', 'fast']:
                for frozen_arm in [0, 1]:
                    wd, latch = ReadyWatchdog(timeout, reset), IdealLatch(pins)
                    latch.step(arm=0, supervisor=True)
                    first_fault, releases, previous_fault = None, 0, False
                    for t in range(1001):
                        level = int(t % 50 == 10)
                        if mode == 'missing': level = 0
                        if mode.startswith('stuck') and t >= 121: level = int(mode == 'stuck_high')
                        if mode == 'fast': level = t % 2
                        fault = wd.step(t, level)
                        arm = int(t >= 5) if frozen_arm else int(t == 5)
                        q = latch.step(arm=arm, external=fault)
                        cd = ideal_cd(pins, q, int(not fault))
                        if fault and first_fault is None: first_fault = t
                        if previous_fault and not fault: releases += 1
                        if first_fault is not None:
                            assert q == 0 and cd == 1, (mode, t, q, cd)
                        elif t >= 5:
                            assert q == 1 and cd == 0
                        previous_fault = fault
                    expected = None if mode in ['normal', 'fast'] else timeout + (0 if mode == 'missing' else 111)
                    assert first_fault == expected, (mode, first_fault, expected)
                    if expected is not None: assert releases >= 1
                    rows.append({'timeout_ms': timeout, 'fault_pulse_ms': reset,
                                 'mode': mode, 'frozen_arm': frozen_arm,
                                 'first_fault_ms': first_fault, 'fault_releases': releases})
    # Deliberately demonstrate the boundary: faulty new ARM edges can re-enable.
    latch = IdealLatch(pins)
    assert latch.step(arm=0, external=True) == 0
    assert latch.step(arm=0) == 0
    assert latch.step(arm=1) == 1
    assert ideal_cd(pins, 1, 1, bias_valid=False) is None
    return rows


def main():
    sch = DEST / 'charge-watchdog.kicad_sch'
    erc = EVIDENCE / 'charge-watchdog-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    net = DEST / 'charge-watchdog.net'
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    pins, values = read_netlist(net)
    validate(pins, values)
    with (DEST / 'bom.csv').open(encoding='utf-8-sig', newline='') as f:
        rows = list(csv.DictReader(f))
    assert len(rows) == 7 and {r['reference']: r['value'] for r in rows} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST / 'charge-watchdog.pdf')
    # Export the current permit sheet afresh, without changing its old evidence.
    work = ROOT / 'build/charge-watchdog'
    work.mkdir(parents=True, exist_ok=True)
    permit_sch = ROOT / 'hardware/charge-permit/charge-permit.kicad_sch'
    permit_net = work / 'permit-current.net'
    run('sch', 'export', 'netlist', permit_sch, '--format', 'kicadxml', '-o', permit_net)
    pp, pv = read_netlist(permit_net)
    validate_permit(pp, pv)
    for pin in ['1', '2']: assert pins[('J503', pin)] == pp[('J402', pin)]
    assert pv['R401'] == '10k / 1%'
    original, faults = sch.read_text(encoding='utf-8'), []
    for name, old, new, position in [
        ('enable_disabled', 'AUX_3V3', 'USB_GND', '68.58 78.74 180'),
        ('set1_disabled', 'AUX_3V3', 'USB_GND', '68.58 88.9 180'),
        ('cwd_grounded', 'WD_TIMEOUT_SELECT', 'USB_GND', '134.62 88.9 0'),
        ('fault_connector_wrong', 'FAULT_BUS_N', 'WD_HEARTBEAT', '215.9 139.7 180'),
        ('enout_disconnected', 'FAULT_BUS_N', 'ISOLATED_ENOUT', '134.62 78.74 0')]:
        token = f'(global_label "{old}" (shape passive) (at {position})'
        assert original.count(token) == 1, (name, token)
        folder = work / 'negative-controls' / name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder / sch.name
        changed.write_text(original.replace(token, token.replace('"' + old + '"', '"' + new + '"', 1)), encoding='utf-8')
        for f in ['charge-watchdog.kicad_pro', 'RaceRemote_Watchdog.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(DEST / f, folder / f)
        exported = folder / 'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try: validate(*read_netlist(exported))
        except AssertionError as error:
            faults.append({'mutation': name, 'detected': True, 'failed_contract': str(error)})
        else: raise AssertionError('Undetected mutation: ' + name)
    sequences = traces(pp)
    files = [DEST / n for n in ['charge-watchdog.kicad_sch', 'charge-watchdog.kicad_pro',
             'RaceRemote_Watchdog.kicad_sym', 'charge-watchdog.net', 'charge-watchdog.pdf',
             'bom.csv', 'sym-lib-table', 'fp-lib-table']]
    files += [erc, permit_sch, ROOT / 'hardware/charge-permit/RaceRemote_Permit.kicad_sym',
              ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym']
    files += [ROOT / 'tools' / n for n in ['build_charge_watchdog.py', 'verify_charge_watchdog.py',
              'build_charge_permit.py', 'build_usb_port_schematic.py', 'verify_usb_detector.py', 'verify_charge_permit.py']]
    report = {'date': '2026-09-14', 'contract': 'CHARGE-WD-01 v0.1',
              'kicad_version': erc_data['kicad_version'], 'erc_violations': 0,
              'components': len(values), 'pins_including_ep': len(pins),
              'fault_injections': faults, 'settled_rail_sequences': sequences,
              'arithmetic': {'existing_fault_pullup_max_A': 3.6 / 9900,
                             'wdi_pulldown_high_load_max_A': 3.6 / (47000 * .99)},
              'not_verified': ['Physical timing, GPIO input/output margins and total AUX current',
                               'Startup/deep brownout, CD response and residual battery current',
                               'MCU firmware, source qualification and INA300 integration',
                               'PCB, assembly, complete charger and charge cycle'],
              'limitations': ['Model starts after explicit initialization on valid rails',
                              'Fast heartbeat does not prove correct firmware execution',
                              'A new erroneous ARM edge after fault release can rearm'],
              'sha256': {f.relative_to(ROOT).as_posix(): hashlib.sha256(f.read_bytes()).hexdigest() for f in files}}
    (EVIDENCE / 'charge-watchdog-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'erc': 0, 'components': len(values), 'pins': len(pins),
                      'detected_mutations': len(faults), 'settled_rail_sequences': len(sequences)}))


if __name__ == '__main__': main()
