"""Check MCU package/allocation/interfaces and conditional DC logic, not firmware."""
import csv
import hashlib
import itertools
import json
from pathlib import Path
import shutil
from verify_usb_detector import ROOT, read_netlist, run, validate as validate_bc
from verify_charge_permit import validate as validate_permit, IdealLatch
from verify_charge_watchdog import validate as validate_wd

DEST = ROOT / 'hardware/charger-mcu'
EVIDENCE = ROOT / 'docs/evidence'
# WCH table 2-1, TSSOP20 column; independent of generator/CSV.
PORTS = ['PD4', 'PD5', 'PD6', 'PD7', 'PA1', 'PA2', 'VSS', 'PD0', 'VDD',
         'PC0', 'PC1', 'PC2', 'PC3', 'PC4', 'PC5', 'PC6', 'PC7', 'PD1', 'PD2', 'PD3']
SIGNALS = ['PERMIT_Q', 'ARM_N', 'WD_HEARTBEAT_N', 'RESET_N', 'FAULT_BUS_N',
           'CHARGER_FAULT', 'USB_GND', 'SOURCE_REQUEST_N', 'AUX_3V3', 'BC_INT_N',
           'BC_SDA', 'BC_SCL', 'BC_EN_N', 'AUX_DIAGNOSTIC', 'CC_OUT1', 'CC_OUT2',
           'INPUT_FAULT_N', 'SWIO', 'VBUS_SENSE', 'CELL1_SENSE']
# TI SCES367J, DCT diagram: 1->7, 3->5, 6->2 inverting; VCC8/GND4.
BUFFER = {1: 'ARM_N', 7: 'ARM', 3: 'WD_HEARTBEAT_N', 5: 'WD_HEARTBEAT',
          6: 'SOURCE_REQUEST_N', 2: 'SOURCE_REQUEST', 8: 'AUX_3V3', 4: 'USB_GND'}
PAIRS = {'R601': ('AUX_3V3', 'ARM_N'), 'R602': ('AUX_3V3', 'WD_HEARTBEAT_N'),
         'R603': ('AUX_3V3', 'SOURCE_REQUEST_N'), 'R604': ('AUX_3V3', 'RESET_N'),
         'C601': ('AUX_3V3', 'USB_GND'), 'C602': ('AUX_3V3', 'USB_GND'), 'C603': ('AUX_3V3', 'USB_GND'),
         'J601': ('AUX_3V3', 'USB_GND'), 'J602': ('ARM', 'FAULT_BUS_N', 'PERMIT_Q', 'USB_GND'),
         'J603': ('WD_HEARTBEAT', 'USB_GND'),
         'J604': ('AUX_3V3', 'SOURCE_REQUEST', 'INPUT_FAULT_N', 'USB_GND'),
         'J605': ('BC_SCL', 'BC_SDA', 'BC_INT_N', 'BC_EN_N'),
         'J606': ('CC_OUT1', 'CC_OUT2', 'USB_GND', 'USB_GND'),
         'J607': ('SWIO', 'RESET_N', 'AUX_3V3', 'USB_GND'),
         'J608': ('CHARGER_FAULT', 'AUX_DIAGNOSTIC', 'VBUS_SENSE', 'CELL1_SENSE')}
VALUES = {'U601': 'CH32V003F4P6', 'U602': 'SN74LVC3G14DCTR',
          'R601': '10k / 1%', 'R602': '10k / 1%', 'R603': '10k / 1%', 'R604': '10k / 1%',
          'C601': '100n / 16V', 'C602': '1u / 16V', 'C603': '100n / 16V',
          'J601': 'FROM USB AUX', 'J602': 'TO PERMIT J403', 'J603': 'TO WATCHDOG J502',
          'J604': 'SOURCE POLICY', 'J605': 'TO BC J303', 'J606': 'CC RESERVE',
          'J607': 'DEBUG LOGICAL', 'J608': 'DIAGNOSTIC RESERVE'}


def validate(pins, values):
    expected = {('U601', str(i + 1)): n for i, n in enumerate(SIGNALS)}
    expected.update({('U602', str(i)): n for i, n in BUFFER.items()})
    expected.update({(r, str(i + 1)): n for r, ns in PAIRS.items() for i, n in enumerate(ns)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'


def validate_allocation(rows):
    assert [int(r['pin']) for r in rows] == list(range(1, 21))
    assert [r['port'] for r in rows] == PORTS
    assert [r['proposed_signal'] for r in rows] == SIGNALS
    for p in [2, 3, 8, 13]: assert rows[p - 1]['mode'] == 'open_drain'
    assert rows[4]['mode'] == 'open_drain_readback'
    assert rows[0]['mode'] == 'input' and rows[17]['mode'] == 'debug'
    assert rows[3]['mode'] == 'reserved'


def logical_cases(pins, permit):
    cases = []
    # State 0 sinks GPIO, 1 releases it. Valid power, settled Schmitt state only.
    for states in itertools.product([0, 1], repeat=3):
        levels = dict(zip(['ARM_N', 'WD_HEARTBEAT_N', 'SOURCE_REQUEST_N'], states))
        for inp, out in [(1, 7), (3, 5), (6, 2)]:
            levels[pins[('U602', str(out))]] = 1 - levels[pins[('U602', str(inp))]]
        assert [levels[n] for n in ['ARM', 'WD_HEARTBEAT', 'SOURCE_REQUEST']] == [1-s for s in states]
        cases.append({'gpio_released': list(states), 'outputs': {n: levels[n] for n in ['ARM', 'WD_HEARTBEAT', 'SOURCE_REQUEST']}})
    assert all(v == 0 for v in cases[-1]['outputs'].values())
    latch = IdealLatch(permit)
    assert latch.step(arm=0, supervisor=True) == 0
    assert latch.step(arm=0) == 0  # all released
    assert latch.step(arm=1) == 1  # MCU sinks ARM_N
    assert latch.step(arm=0) == 1  # release does not clear latched charge permit
    assert latch.step(arm=0, revoke=True) == 0
    assert latch.step(arm=0) == 0
    return cases


def main():
    allocation = ROOT / 'docs/charger-mcu-pins.csv'
    with allocation.open(encoding='utf-8-sig') as f: rows = list(csv.DictReader(f))
    validate_allocation(rows)
    sch, net, erc = DEST / 'charger-mcu.kicad_sch', DEST / 'charger-mcu.net', EVIDENCE / 'charger-mcu-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    pins, values = read_netlist(net)
    validate(pins, values)
    with (DEST / 'bom.csv').open(encoding='utf-8-sig') as f: bom = list(csv.DictReader(f))
    assert len(bom) == 17 and {r['reference']: r['value'] for r in bom} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST / 'charger-mcu.pdf')
    work = ROOT / 'build/charger-mcu'
    work.mkdir(parents=True, exist_ok=True)
    interface_inputs, external = [], {}
    for module, validator, source, target in [('charge-permit', validate_permit, 'J602', 'J403'),
            ('charge-watchdog', validate_wd, 'J603', 'J502'), ('usb-detector', validate_bc, 'J605', 'J303')]:
        path = ROOT / 'hardware' / module / (module + '.kicad_sch')
        exported = work / (module + '.net')
        run('sch', 'export', 'netlist', path, '--format', 'kicadxml', '-o', exported)
        pp, pv = read_netlist(exported)
        validator(pp, pv)
        external[module] = pp
        for i in range(len(PAIRS[source])): assert pins[(source, str(i + 1))] == pp[(target, str(i + 1))]
        interface_inputs.append(path)
        if module == 'charge-permit': assert pv['R402'] == '47k / 1%'
    original, faults = sch.read_text(encoding='utf-8'), []
    for name, old, new, position in [
        ('arm_bypasses_inverter', 'ARM', 'ARM_N', '302.26 55.88 0'),
        ('watchdog_uses_source', 'WD_HEARTBEAT', 'SOURCE_REQUEST', '302.26 66.04 0'),
        ('default_arm_input_grounded', 'AUX_3V3', 'USB_GND', '228.6 123.19 0'),
        ('readback_is_arm', 'PERMIT_Q', 'ARM', '68.58 55.88 180'),
        ('fault_bus_driven_by_buffer', 'SOURCE_REQUEST', 'FAULT_BUS_N', '302.26 76.2 0'),
        ('swio_repurposed_as_i2c', 'SWIO', 'BC_SCL', '134.62 127 0')]:
        token = f'(global_label "{old}" (shape passive) (at {position})'
        assert original.count(token) == 1, (name, token)
        folder = work / 'negative-controls' / name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder / sch.name
        changed.write_text(original.replace(token, token.replace('"' + old + '"', '"' + new + '"', 1)), encoding='utf-8')
        for f in ['charger-mcu.kicad_pro', LIB_FILE, 'sym-lib-table', 'fp-lib-table']: shutil.copyfile(DEST / f, folder / f)
        exported = folder / 'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try: validate(*read_netlist(exported))
        except AssertionError as error: faults.append({'mutation': name, 'detected': True, 'reason': str(error)})
        else: raise AssertionError('Undetected fault: ' + name)
    changed = [dict(r) for r in rows]; changed[4]['mode'] = 'push_pull'
    try: validate_allocation(changed)
    except AssertionError: faults.append({'mutation': 'fault_gpio_push_pull_mode', 'detected': True})
    else: raise AssertionError('Unsafe GPIO mode accepted')
    arithmetic = {'scope': 'Conditional DC arithmetic, not full GPIO/rail/edge qualification',
        'direct_cmos_high_min_V': 2.3, 'watchdog_high_required_at_3V6_V': .8 * 3.6,
        'each_request_gpio_sink_upper_A': 3.6 / 9900 + 5e-6,
        'inverter_input_released_min_at_3V_with_6uA_leak_V': 3.0 - 6e-6 * 10100,
        'inverter_input_high_margin_at_datasheet_3V_point_V': 3.0 - 6e-6 * 10100 - 2.2,
        'inverter_input_low_margin_at_datasheet_3V_point_V': .6 - .4,
        'arm_output_load_upper_A': 3.6 / (47000 * .99) + 5e-6,
        'wd_output_load_with_assumed_20uA_input_A': 3.6 / (47000 * .99) + 20e-6,
        'source_output_external_load_budget_A': 100e-6,
        'buffer_output_high_at_3V_100uA_V': 2.9,
        'buffer_output_low_100uA_V': .1,
        'watchdog_high_min_margin_over_3V_to_3V6_V': .2 * 3.0 - .1,
        'source_high_margin_to_proposed_2V8_V': 2.9 - 2.8}
    assert arithmetic['direct_cmos_high_min_V'] < arithmetic['watchdog_high_required_at_3V6_V']
    assert arithmetic['arm_output_load_upper_A'] < 100e-6
    assert arithmetic['wd_output_load_with_assumed_20uA_input_A'] < 100e-6
    assert arithmetic['each_request_gpio_sink_upper_A'] < .008
    cases = logical_cases(pins, external['charge-permit'])
    files = list(DEST.glob('*')) + [allocation, erc] + interface_inputs
    files = [p for p in files if p.is_file() and p.suffix != '.md']
    files += [ROOT / 'tools' / n for n in ['build_charger_mcu.py', 'verify_charger_mcu.py', 'build_charge_permit.py',
              'build_usb_port_schematic.py', 'verify_charge_permit.py', 'verify_charge_watchdog.py', 'verify_usb_detector.py']]
    files.append(ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym')
    report = {'date': '2026-09-14', 'contracts': ['CHARGE-MCU-01 v0.2', 'CHARGE-IO-01 v0.1'],
              'erc_violations': 0, 'kicad_version': erc_data['kicad_version'],
              'components': len(values), 'pins': len(pins), 'fault_injections': faults,
              'truth_table': cases, 'arithmetic': arithmetic,
              'not_verified': ['MCU firmware, GPIO reset/configuration/locking and programming',
                               'Input Schmitt thresholds between tabulated VCC points, analog ramps and edges',
                               'Actual WDI/source loading, total AUX current and fault-bus leakage',
                               'Source permission policy, INA300 clear/latch, battery monitoring/protection',
                               'PCB, assembly, CD/current transient and physical charging'],
              'sha256': {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}}
    (EVIDENCE / 'charger-mcu-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'erc': 0, 'components': len(values), 'pins': len(pins), 'faults_detected': len(faults), 'truth_cases': len(cases)}))


LIB_FILE = 'RaceRemote_ChargerMCU.kicad_sym'
if __name__ == '__main__': main()
