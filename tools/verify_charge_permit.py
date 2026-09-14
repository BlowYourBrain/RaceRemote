"""Verify exported package wiring and ideal latch sequences, not analog safety."""
import csv
import hashlib
import json
from pathlib import Path
import shutil
from verify_usb_detector import ROOT, read_netlist, run

DEST = ROOT / 'hardware/charge-permit'
EVIDENCE = ROOT / 'docs/evidence'
# Independently transcribed numeric package tables: SCES794G p4, SCES381N p3,
# SBVS050N p4, SCDS409A p3, SBVS186H p3. 1G74 pin3 is inverted, pin5 true output.
IC_NETS = {
    'U401': {1: 'CLK', 2: 'AUX_3V3', 3: 'unconnected-(U401-~{Q}-Pad3)', 4: 'USB_GND',
             5: 'PERMIT_Q', 6: 'CLR_N', 7: 'AUX_3V3', 8: 'AUX_3V3'},
    'U402': {1: 'FAULT_BUS_N', 2: 'USB_GND', 3: 'ARM', 4: 'CLK', 5: 'AUX_3V3', 6: 'CLR_N'},
    'U403': {1: 'FAULT_BUS_N', 2: 'USB_GND', 3: 'CHG_BIAS_3V3',
             4: 'unconnected-(U403-CT-Pad4)', 5: 'AUX_3V3', 6: 'CHG_BIAS_3V3'},
    'U404': {1: 'PERMIT_Q', 2: 'CHG_BIAS_3V3', 3: 'USB_GND', 4: 'CHG_BIAS_3V3', 5: 'CD_REQUEST', 6: 'USB_GND'},
    'U405': {1: 'FAULT_BUS_N', 2: 'CHG_BIAS_3V3', 3: 'USB_GND', 4: 'CHG_BIAS_3V3', 5: 'CHARGER_CD', 6: 'CD_REQUEST'},
    'U406': {1: 'CHG_SUPPLY_INPUT', 2: 'USB_GND', 3: 'unconnected-(U406-EN-Pad3)',
             4: 'unconnected-(U406-NC-Pad4)', 5: 'CHG_BIAS_3V3'}}
CONNECTIONS = {
    'C401': ('AUX_3V3', 'USB_GND'), 'C402': ('AUX_3V3', 'USB_GND'), 'C403': ('CHG_BIAS_3V3', 'USB_GND'),
    'C404': ('CHG_BIAS_3V3', 'USB_GND'), 'C405': ('CHG_BIAS_3V3', 'USB_GND'),
    'C406': ('CHG_SUPPLY_INPUT', 'USB_GND'), 'C407': ('CHG_BIAS_3V3', 'USB_GND'),
    'R401': ('AUX_3V3', 'FAULT_BUS_N'), 'R402': ('ARM', 'USB_GND'), 'R403': ('PERMIT_Q', 'USB_GND'),
    'R404': ('CHG_BIAS_3V3', 'CHARGER_CD'), 'R405': ('FAULT_BUS_N', 'USB_GND'),
    'R406': ('CHG_BIAS_3V3', 'USB_GND'),
    'J401': ('AUX_3V3', 'USB_GND', 'CHG_SUPPLY_INPUT', 'USB_GND'), 'J402': ('FAULT_BUS_N', 'USB_GND'),
    'J403': ('ARM', 'FAULT_BUS_N', 'PERMIT_Q', 'USB_GND'), 'J404': ('CHARGER_CD', 'USB_GND')}
VALUES = {
    'U401': 'SN74LVC1G74DCTR', 'U402': 'SN74LVC2G17DCKR', 'U403': 'TPS3808G33DBVR',
    'U404': 'TMUX1219DBVR', 'U405': 'TMUX1219DBVR',
    'U406': 'TPS70933DBVR',
    'C401': '100n / 16V', 'C402': '100n / 16V', 'C403': '100n / 16V',
    'C404': '100n / 16V', 'C405': '100n / 16V',
    'C406': '1u / 50V', 'C407': '4.7u / 16V X7R',
    'R401': '10k / 1%', 'R402': '47k / 1%', 'R403': '47k / 1%',
    'R404': '47k / 1%', 'R405': '1M / 1%',
    'R406': '3.3k / 1%',
    'J401': 'AUX AND CHARGER INPUT', 'J402': 'FAULT SOURCES', 'J403': 'MCU INTERFACE', 'J404': 'TO BQ25887 CD'}


def validate(pins, values):
    expected = {(ref, str(pin)): net for ref, pp in IC_NETS.items() for pin, net in pp.items()}
    for ref, nets in CONNECTIONS.items():
        expected.update({(ref, str(i + 1)): net for i, net in enumerate(nets)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'


class IdealLatch:
    """Numeric-pin digital propagation using the exported nets.

    Valid rails and settled logic only. Supervisor reset is an injected input,
    NOT computed from an analog supply waveform or the nominal 20ms setting.
    No metastability, propagation delay, noise, leakage or brownout simulation.
    """
    def __init__(self, pins, initial=None):
        self.pins, self.q, self.clock = pins, initial, 0

    def step(self, arm, supervisor=False, external=False, revoke=False):
        def net(ref, pin):
            return self.pins[(ref, str(pin))]
        signals = {net('J401', 1): 1, net('J401', 2): 0, net('J401', 3): 1, net('J403', 1): int(arm)}
        # Shared wired-AND open-drain bus: any LOW wins; no HIGH driver.
        signals[net('U403', 1)] = int(not (supervisor or external or revoke))
        assert signals[net('U402', 5)] == 1 and signals[net('U402', 2)] == 0
        for inp, out in [(1, 6), (3, 4)]:
            signals[net('U402', out)] = signals[net('U402', inp)]
        assert signals[net('U401', 8)] == 1 and signals[net('U401', 4)] == 0
        clock, clear, preset, data = [signals[net('U401', p)] for p in [1, 6, 7, 2]]
        assert preset == 1, 'PRE must never request asynchronous set'
        if clear == 0:
            self.q = 0
        elif self.clock == 0 and clock == 1:
            self.q = data
        self.clock = clock
        signals[net('U401', 5)] = self.q
        return signals[net('J403', 3)]


def ideal_cd(pins, q, fault_bus_n, bias_valid=True):
    """Settled mux truth table, including unknown Q; not a supply-ramp model."""
    if not bias_valid:
        return None  # No claim of driven HIGH without a qualified bias rail.
    def net(ref, pin):
        return pins[(ref, str(pin))]
    signals = {net('J401', 3): 1, net('U406', 5): 1, net('J401', 2): 0,
               net('U401', 5): q, net('U403', 1): fault_bus_n}
    for ref in ['U404', 'U405']:
        assert signals[net(ref, 2)] == 1 and signals[net(ref, 3)] == 0
        sel, s1, s2 = [signals[net(ref, pin)] for pin in [1, 4, 6]]
        signals[net(ref, 5)] = s1 if sel == 0 else s2 if sel == 1 else s1 if s1 == s2 else None
    return signals[net('J404', 1)]


def actuator_cases(pins):
    rows = []
    for bias in [True, False]:
        for q in [0, 1, None]:
            for fault in [0, 1, None]:
                observed = ideal_cd(pins, q, fault, bias)
                if not bias:
                    expected = None
                elif fault == 0 or q == 0:
                    expected = 1
                elif q == 1 and fault == 1:
                    expected = 0
                else:
                    expected = None
                assert observed == expected, (bias, q, fault, observed, expected)
                rows.append({'bias_valid': bias, 'q': q, 'fault_bus_n': fault, 'cd': observed})
    return rows


def sequences(pins):
    traces = []
    def check(name, events, expected, initial=None):
        latch = IdealLatch(pins, initial)
        observed = [latch.step(**event) for event in events]
        assert observed == expected, (name, observed, expected)
        traces.append({'scenario': name, 'initial_q': initial, 'events': events, 'observed_q': observed})
    check('unknown_without_startup_reset', [{'arm': False}], [None])
    for initial in [None, 0, 1]:
        check('startup_reset_from_' + str(initial),
              [{'arm': True, 'supervisor': True}, {'arm': True}, {'arm': False}, {'arm': True}],
              [0, 0, 0, 1], initial)
    for source in ['supervisor', 'external', 'revoke']:
        check(source + '_clears_and_release_with_stuck_high_stays_off',
              [{'arm': False, 'supervisor': True}, {'arm': False}, {'arm': True},
               {'arm': True, source: True}, {'arm': True}, {'arm': True}], [0, 0, 1, 0, 0, 0])
    check('fault_wins_over_simultaneous_arm',
          [{'arm': False, 'supervisor': True}, {'arm': True, 'external': True},
           {'arm': True}, {'arm': False}, {'arm': True}], [0, 0, 0, 0, 1])
    check('stuck_low_cannot_rearm',
          [{'arm': False, 'external': True}, {'arm': False}, {'arm': False}], [0, 0, 0])
    check('one_fault_released_while_another_holds',
          [{'arm': False, 'external': True, 'revoke': True}, {'arm': False, 'external': True},
           {'arm': True, 'external': True}, {'arm': True}], [0, 0, 0, 0])
    check('limitation_new_edges_can_rearm_after_all_faults_release',
          [{'arm': False, 'external': True}, {'arm': False}, {'arm': True},
           {'arm': False, 'external': True}, {'arm': False}, {'arm': True}], [0, 0, 1, 0, 0, 1])
    return traces


def main():
    sch = DEST / 'charge-permit.kicad_sch'
    erc = EVIDENCE / 'charge-permit-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    net = DEST / 'charge-permit.net'
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    pins, values = read_netlist(net)
    validate(pins, values)
    with (DEST / 'bom.csv').open(encoding='utf-8-sig', newline='') as f:
        rows = list(csv.DictReader(f))
    assert len(rows) == len(VALUES) and {r['reference']: r['value'] for r in rows} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST / 'charge-permit.pdf')
    original, faults = sch.read_text(encoding='utf-8'), []
    for name, old, new, position in [
        ('clear_bypassed_to_aux', 'CLR_N', 'AUX_3V3', '297.18 88.9 180'),
        ('data_tied_low', 'AUX_3V3', 'USB_GND', '297.18 78.74 180'),
        ('supervisor_senses_ground', 'AUX_3V3', 'USB_GND', '43.18 68.58 180'),
        ('output_connector_uses_clock', 'CHARGER_CD', 'CLK', '368.3 246.38 180'),
        ('supervisor_supply_back_on_aux', 'CHG_BIAS_3V3', 'AUX_3V3', '109.22 88.9 0'),
        ('fault_mux_inhibit_branch_grounded', 'CHG_BIAS_3V3', 'USB_GND', '490.22 170.18 0'),
        ('fault_mux_select_bypassed_by_q', 'FAULT_BUS_N', 'PERMIT_Q', '424.18 170.18 180'),
        ('bias_ldo_fed_from_monitored_aux', 'CHG_SUPPLY_INPUT', 'AUX_3V3', '144.78 320.04 180'),
        ('mux_supply_bypasses_bias_ldo', 'CHG_BIAS_3V3', 'CHG_SUPPLY_INPUT', '424.18 180.34 180'),
        ('bias_output_cap_on_input', 'CHG_BIAS_3V3', 'CHG_SUPPLY_INPUT', '279.4 339.09 0')]:
        token = f'(global_label "{old}" (shape passive) (at {position})'
        assert original.count(token) == 1, (name, token)
        folder = ROOT / 'build/charge-permit/negative-controls' / name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder / sch.name
        changed.write_text(original.replace(token, token.replace('"' + old + '"', '"' + new + '"', 1)), encoding='utf-8')
        for file in ['charge-permit.kicad_pro', 'RaceRemote_Permit.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(DEST / file, folder / file)
        exported = folder / 'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try:
            validate(*read_netlist(exported))
        except AssertionError as error:
            faults.append({'mutation': name, 'detected': True, 'failed_contract': str(error)})
        else:
            raise AssertionError('Undetected fault: ' + name)
    # Static bounds under individual TI test conditions, not a dynamic proof.
    arithmetic = {
        'fault_pullup_max_A': 3.6 / (10000 * .99),
        'arm_pulldown_max_A': 3.6 / (47000 * .99),
        'arm_load_max_A_with_5uA_schmitt_input': 3.6 / (47000 * .99) + 5e-6,
        'arm_float_V_with_assumed_15uA_leakage': 15e-6 * 47000 * 1.01,
        'q_load_max_A_with_proposed_20uA_external': 3.6 / (47000 * .99) + 20e-6,
        'q_voltage_from_10uA_Ioff_at_VCC_zero_V': 10e-6 * 47000 * 1.01,
        'supervisor_falling_threshold_full_temperature_min_V': 3.07 * .985,
        'supervisor_falling_threshold_full_temperature_max_V': 3.07 * 1.015,
        'supervisor_ct_open_release_ms_min_typ_max': [12, 20, 28],
        'scope': 'Separate datasheet conditions and assumed resistor tolerance. No analog simulation, '
                 'whole AUX budget, output threshold acceptance, fast supply-fall or total turnoff proof.'}
    assert arithmetic['fault_pullup_max_A'] < .001
    assert arithmetic['arm_load_max_A_with_5uA_schmitt_input'] < 100e-6
    assert arithmetic['arm_float_V_with_assumed_15uA_leakage'] < .8
    assert arithmetic['q_load_max_A_with_proposed_20uA_external'] < 100e-6
    # Conditional DC model: specified table at bias 3.3V +/-10%. 100uA is a
    # proposed external load budget, not a measured or BQ-guaranteed maximum.
    cd_model = {
        'bias_range_V': [3.0, 3.6], 'ron_each_ohm': 12, 'assumed_external_load_A': 100e-6,
        'low_V': (3.6 / (47000 * .99) + 100e-6) * 24,
        'high_V': 3.0 - 100e-6 * 24,
        'supervisor_LOW_V_max_at_specified_sink': .4, 'mux_VIL_max_V': .8,
        'mux_VIH_min_V': 1.35,
        'fault_high_V_with_1M_and_assumed_10uA_leakage':
            (3.0 / 10100 - 10e-6) / (1 / 10100 + 1 / 990000),
        'input_gate_conditional_clamp_V_max': 5.55,
        'direct_mux_supply_from_input_allowed': False,
        'bias_regulator_input_recommended_max_V': 30,
        'input_gate_clamp_within_regulator_input_rating': True,
        'complete_input_integration_qualified': False,
        'ldo_dc_nominal_V': 3.3,
        'ldo_dc_estimate_V_under_separate_table_conditions': [3.3 * .99 - .01 - .05, 3.3 * 1.01 + .01 + .05],
        'ldo_preload_A_range': [3.0 / (3300 * 1.01), 3.6 / (3300 * .99)],
        'bias_total_load_proposal_A': .002,
        'ldo_series_loss_W_at_5_55V_2mA': (5.55 - 3.0) * .002,
        'ldo_estimate_scope': 'Sum of accuracy, line/load regulation under separate TI conditions; '
                              'not a guaranteed combined envelope. Near-dropout, total load and capacitor behavior require qualification.',
        'scope': 'DC budget only at stated TI test conditions; no interpolation into supply ramps, '
                 'switch charge-injection, BQ timing, residual current or all-state safety proof.'}
    assert cd_model['low_V'] < .4 and cd_model['high_V'] > 1.3
    assert cd_model['supervisor_LOW_V_max_at_specified_sink'] < cd_model['mux_VIL_max_V']
    assert cd_model['fault_high_V_with_1M_and_assumed_10uA_leakage'] > cd_model['mux_VIH_min_V']
    assert cd_model['input_gate_conditional_clamp_V_max'] < cd_model['bias_regulator_input_recommended_max_V']
    assert 2.97 <= cd_model['bias_range_V'][0] < cd_model['bias_range_V'][1] <= 3.63
    assert cd_model['ldo_preload_A_range'][0] > 100e-6
    assert cd_model['ldo_preload_A_range'][1] < cd_model['bias_total_load_proposal_A']
    traces = sequences(pins)
    files = [DEST / name for name in ['charge-permit.kicad_sch', 'charge-permit.kicad_pro',
             'RaceRemote_Permit.kicad_sym', 'charge-permit.net', 'charge-permit.pdf', 'bom.csv']]
    files += [erc, ROOT / 'tools/build_charge_permit.py', Path(__file__),
              ROOT / 'tools/verify_usb_detector.py', ROOT / 'tools/build_usb_port_schematic.py']
    report = {'date': '2026-09-14', 'contract': 'CHARGE-PERMIT-01 v0.4',
              'kicad_version': erc_data['kicad_version'], 'erc_violations': 0,
              'ic_pins_checked': 37, 'all_pins_checked': len(pins), 'logical_components': len(values),
              'fault_injections': faults, 'ideal_logic_sequences': traces, 'arithmetic': arithmetic,
              'ideal_actuator_cases': actuator_cases(pins), 'conditional_cd_model': cd_model,
              'sha256': {f.relative_to(ROOT).as_posix(): hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
              'not_verified': ['Startup/deep brownout/metastability/pulse widths and real fault timing',
                               'MCU firmware, independent watchdog and INA300 circuits',
                               'Persistent error versus incorrectly repeated MCU ARM edges',
                               'CHG_BIAS_3V3 compatibility with TPS25200 and charger VBUS across all states',
                               'CD physical switching, charge current interruption and residual energy',
                               'Footprints, PCB, assembly, complete assembled cost and battery acceptance']}
    (EVIDENCE / 'charge-permit-verification.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'erc': 0, 'ic_pins': 37, 'all_pins': len(pins), 'components': len(values),
                      'mutations_detected': len(faults), 'ideal_sequences': len(traces),
                      'actuator_cases': len(report['ideal_actuator_cases']), 'cd_model': cd_model}))


if __name__ == '__main__':
    main()
