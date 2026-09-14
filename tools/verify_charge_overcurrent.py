"""Check INA300 wiring and a window/latch model, not analog charge protection."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
from verify_usb_detector import ROOT, read_netlist, run
from verify_charge_permit import validate as validate_permit, IdealLatch, ideal_cd

DEST = ROOT / 'hardware/charge-overcurrent'
EVIDENCE = ROOT / 'docs/evidence'
# SBOS613C table 5-1 DSQ. Thermal pad assigned local logical number 11.
IC_NETS = {1: 'SHUNT_UPSTREAM_P', 2: 'SHUNT_DOWNSTREAM_P', 3: 'OC_LIMIT',
           4: 'AUX_3V3', 5: 'FAULT_BUS_N', 6: 'AUX_3V3', 7: 'USB_GND',
           8: 'USB_GND', 9: 'AUX_3V3', 10: 'unconnected-(U701-HYS-Pad10)', 11: 'USB_GND'}
PAIRS = {'C701': ('AUX_3V3', 'USB_GND'), 'R701': ('OC_LIMIT', 'USB_GND'),
         'R702': ('SHUNT_UPSTREAM_P', 'SHUNT_DOWNSTREAM_P'),
         'J701': ('AUX_3V3', 'USB_GND'),
         'J702': ('SHUNT_UPSTREAM_P', 'SHUNT_DOWNSTREAM_P'), 'J703': ('FAULT_BUS_N', 'USB_GND')}
VALUES = {'U701': 'INA300AIDSQR', 'C701': '100n / 16V', 'R701': '1.74k / 1%',
          'R702': 'RL2512FK-070R1L', 'J701': 'FROM USB AUX',
          'J702': 'BENCH SERIES PATH', 'J703': 'TO PERMIT J402'}


def validate(pins, values):
    expected = {('U701', str(p)): n for p, n in IC_NETS.items()}
    expected.update({(r, str(i + 1)): n for r, ns in PAIRS.items() for i, n in enumerate(ns)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'


class WindowLatch:
    """Ideal 1us input samples, fixed 10us averaging windows, five consecutive.

    Starts AFTER initialization with no fault. No delay tolerance, hysteresis,
    noise, analog startup, real filter or supply-cycle reset simulation.
    Input in microvolts; nominal threshold 34800uV. LATCH fixed HIGH.
    """
    def __init__(self):
        self.total, self.samples, self.consecutive, self.fault = 0, 0, 0, False

    def step(self, differential_uV):
        self.total += differential_uV
        self.samples += 1
        if self.samples == 10:
            self.consecutive = self.consecutive + 1 if self.total > 34800 * 10 else 0
            self.fault |= self.consecutive >= 5
            self.total, self.samples = 0, 0
        return self.fault


def sequences(permit):
    definitions = [
        ('nominal_250mA', [25000] * 200, None),
        ('continuous_above_threshold', [50000] * 100, 50),
        ('one_low_window_restarts_counter', [50000] * 40 + [0] * 10 + [50000] * 50, 100),
        ('limitation_repeated_40us_bursts_missed', ([150000] * 40 + [0] * 60) * 10, None),
        ('within_window_average_above_threshold', ([150000] * 6 + [0] * 4) * 10, 50),
        ('limitation_reversed_sense_not_detected', [-150000] * 200, None)]
    rows = []
    for name, samples, expected in definitions:
        detector, latch = WindowLatch(), IdealLatch(permit)
        latch.step(arm=0, supervisor=True)
        latch.step(arm=1)
        first = None
        for time, voltage in enumerate(samples, 1):
            fault = detector.step(voltage)
            if fault and first is None: first = time
            q = latch.step(arm=time % 2, external=fault)
            if fault: assert q == 0 and ideal_cd(permit, q, 0) == 1
        assert first == expected, (name, first, expected)
        if first is not None:
            # Even malicious/repeated ARM edges cannot overcome a held OC fault.
            for arm_mode in ['low', 'high', 'toggling']:
                for time in range(100):
                    assert detector.step(0)
                    arm = int(arm_mode == 'high') if arm_mode != 'toggling' else time % 2
                    q = latch.step(arm=arm, external=True)
                    assert q == 0 and ideal_cd(permit, q, 0) == 1
        rows.append({'name': name, 'samples_1us': len(samples), 'first_alert_us': first,
                     'postfault_hold_with_all_ARM_modes': first is not None})
    # Boundary: a lost/released detector fault plus fresh ARM is not blocked forever.
    latch = IdealLatch(permit)
    assert latch.step(arm=0, external=True) == 0
    assert latch.step(arm=0) == 0
    assert latch.step(arm=1) == 1
    assert ideal_cd(permit, 0, 0, bias_valid=False) is None
    return rows


def main():
    sch, net = DEST / 'charge-overcurrent.kicad_sch', DEST / 'charge-overcurrent.net'
    erc = EVIDENCE / 'charge-overcurrent-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    pins, values = read_netlist(net)
    validate(pins, values)
    with (DEST / 'bom.csv').open(encoding='utf-8-sig') as f: rows = list(csv.DictReader(f))
    assert len(rows) == 7 and {r['reference']: r['value'] for r in rows} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST / 'charge-overcurrent.pdf')
    work = ROOT / 'build/charge-overcurrent'
    work.mkdir(parents=True, exist_ok=True)
    permit_sch = ROOT / 'hardware/charge-permit/charge-permit.kicad_sch'
    permit_net = work / 'permit-current.net'
    run('sch', 'export', 'netlist', permit_sch, '--format', 'kicadxml', '-o', permit_net)
    pp, pv = read_netlist(permit_net)
    validate_permit(pp, pv)
    for pin in ['1', '2']: assert pins[('J703', pin)] == pp[('J402', pin)]
    assert pv['R401'] == '10k / 1%'
    original, faults = sch.read_text(encoding='utf-8'), []
    for name, old, new, pos in [
        ('sense_positive_swapped', 'SHUNT_UPSTREAM_P', 'SHUNT_DOWNSTREAM_P', '68.58 68.58 180'),
        ('latch_transparent', 'AUX_3V3', 'USB_GND', '68.58 99.06 180'),
        ('monitor_disabled', 'AUX_3V3', 'USB_GND', '68.58 88.9 180'),
        ('wrong_100us_delay', 'USB_GND', 'AUX_3V3', '134.62 88.9 0'),
        ('fault_wrong_output', 'FAULT_BUS_N', 'PERMIT_Q', '215.9 139.7 180'),
        ('shunt_bypassed', 'SHUNT_DOWNSTREAM_P', 'SHUNT_UPSTREAM_P', '144.78 166.37 0')]:
        token = f'(global_label "{old}" (shape passive) (at {pos})'
        assert original.count(token) == 1, (name, token)
        folder = work / 'negative-controls' / name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder / sch.name
        changed.write_text(original.replace(token, token.replace('"' + old + '"', '"' + new + '"', 1)), encoding='utf-8')
        for f in ['charge-overcurrent.kicad_pro', 'RaceRemote_Overcurrent.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(DEST / f, folder / f)
        exported = folder / 'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try: validate(*read_netlist(exported))
        except AssertionError as error: faults.append({'mutation': name, 'detected': True, 'reason': str(error)})
        else: raise AssertionError('Undetected fault: ' + name)
    current = 3.6 / 9900
    assert current < .003
    files = [DEST / n for n in ['charge-overcurrent.kicad_sch', 'charge-overcurrent.kicad_pro',
             'RaceRemote_Overcurrent.kicad_sym', 'charge-overcurrent.net', 'charge-overcurrent.pdf', 'bom.csv', 'sym-lib-table', 'fp-lib-table']]
    files += [erc, permit_sch, ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym']
    files += [ROOT / 'tools' / n for n in ['build_charge_overcurrent.py', 'verify_charge_overcurrent.py',
              'build_charge_permit.py', 'build_usb_port_schematic.py', 'verify_charge_permit.py', 'verify_usb_detector.py']]
    report = {'date': '2026-09-14', 'contract': 'CHARGE-OC-01 v0.2',
              'kicad_version': erc_data['kicad_version'], 'erc_violations': 0,
              'components': len(values), 'pins_including_ep_and_nc': len(pins), 'fault_injections': faults,
              'window_model_sequences': sequences(pp), 'existing_pullup_sink_max_A': current,
              'arithmetic_scope': 'Pull-up only; complete bus leakage and transient sink current not measured',
              'not_verified': ['Approved LW charge and pulse limits; complete shutdown latency/peak/energy',
                               'Final shunt placement relative to BAT/MID/CBSET and BMS; reverse/balance paths',
                               'Shunt PCB Kelvin layout, temperature, pulse rating and LIMIT resistor TCR',
                               'INA startup/brownout, power-cycle fault recovery and no-backfeed discharge of AUX',
                               'MCU/source policy, common PCB, assembly and actual charging'],
              'sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}}
    (EVIDENCE / 'charge-overcurrent-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'erc': 0, 'components': len(values), 'pins': len(pins), 'faults': len(faults), 'sequences': len(report['window_model_sequences'])}))


if __name__ == '__main__': main()
