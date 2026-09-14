"""Validate BQ29209 pin/filter/domain wiring; no cell/charger dynamics model."""
import csv
import hashlib
import json
import shutil
from verify_usb_detector import ROOT, read_netlist, run

DEST = ROOT/'hardware/pack-balancer'
EVIDENCE = ROOT/'docs/evidence'
# Independent transcription of SLUSA52C pin table and Figure9.
IC_NETS = {1: 'BAL_VC2', 2: 'BAL_VC1', 3: 'BAL_VC1_CB', 4: 'BAL_DELAY',
           5: 'BAT_RAW_N', 6: 'BAL_EN_N', 7: 'BAL_VDD', 8: 'BAL_OVP_RAW', 9: 'BAT_RAW_N'}
PAIRS = {'R801': ('BAT_RAW_P', 'BAL_VDD'), 'R802': ('BAT_RAW_P', 'BAL_VC2'),
         'R803': ('BAT_MID', 'BAL_VC1'), 'R804': ('BAT_MID', 'BAL_VC1_CB'),
         'R805': ('BAL_VDD', 'BAL_EN_N'), 'C801': ('BAL_VDD', 'BAT_RAW_N'),
         'C802': ('BAL_VC2', 'BAL_VC1'), 'C803': ('BAL_VC1', 'BAT_RAW_N'),
         'C804': ('BAL_DELAY', 'BAT_RAW_N'),
         'J801': ('BAT_RAW_P', 'BAT_MID', 'BAT_RAW_N', 'BAT_RAW_N'),
         'J802': ('BAL_EN_N', 'BAL_OVP_RAW', 'BAL_VDD', 'BAT_RAW_N')}
VALUES = {'U801': 'BQ29209DRBR', 'R801': '100 / 1%', 'R802': '100 / 1%',
          'R803': '100 / 1%', 'R804': '120 / 1%', 'R805': '100k / 1%',
          'C801': '100n / 16V', 'C802': '100n / 16V', 'C803': '100n / 16V',
          'C804': '100n / 16V / 10%', 'J801': 'CELL-SIDE BENCH NODES',
          'J802': 'BATTERY-DOMAIN IO ONLY'}


def validate(pins, values):
    expected = {('U801', str(p)): n for p, n in IC_NETS.items()}
    expected.update({(r, str(p)): n for r, ns in PAIRS.items() for p, n in enumerate(ns, 1)})
    assert values == VALUES, 'Component inventory/value mismatch'
    assert pins == expected, f'Connectivity mismatch: {set(pins.items()) ^ set(expected.items())}'
    forbidden = {'USB_GND', 'CHG_GND', 'PACK_MINUS', 'AUX_3V3', 'FAULT_BUS_N'}
    assert not (set(pins.values()) & forbidden), 'Unqualified cross-domain connection'


def main():
    sch, net = DEST/'pack-balancer.kicad_sch', DEST/'pack-balancer.net'
    erc = EVIDENCE/'pack-balancer-erc.json'
    run('sch', 'erc', sch, '--format', 'json', '--severity-all', '--exit-code-violations', '-o', erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    run('sch', 'export', 'netlist', sch, '--format', 'kicadxml', '-o', net)
    pins, values = read_netlist(net)
    validate(pins, values)
    with (DEST/'bom.csv').open(encoding='utf-8-sig') as f: rows = list(csv.DictReader(f))
    assert {r['reference']: r['value'] for r in rows} == VALUES
    run('sch', 'export', 'pdf', sch, '-o', DEST/'pack-balancer.pdf')
    original, faults = sch.read_text(encoding='utf-8'), []
    for name, old, new in [
        ('sense_cells_swapped', '(global_label "BAL_VC2" (shape passive) (at 86.36 66.04 180)', 'BAL_VC1'),
        ('ground_after_cutoff', '(global_label "BAT_RAW_N" (shape passive) (at 157.48 96.52 0)', 'USB_GND'),
        ('top_filter_to_ground', '(global_label "BAL_VC1" (shape passive) (at 111.76 245.11 0)', 'BAT_RAW_N'),
        ('enable_pulldown', '(global_label "BAL_VDD" (shape passive) (at 325.12 161.29 0)', 'BAT_RAW_N'),
        ('raw_ovp_into_permit', '(global_label "BAL_OVP_RAW" (shape passive) (at 157.48 66.04 0)', 'FAULT_BUS_N')]:
        assert original.count(old) == 1, (name, old)
        old_net = old.split('"')[1]
        folder = ROOT/'build/pack-balancer/negative-controls'/name
        folder.mkdir(parents=True, exist_ok=True)
        changed = folder/sch.name
        changed.write_text(original.replace(old, old.replace('"'+old_net+'"', '"'+new+'"', 1)), encoding='utf-8')
        for filename in ['pack-balancer.kicad_pro', 'RaceRemote_Balancer.kicad_sym', 'sym-lib-table', 'fp-lib-table']:
            shutil.copyfile(DEST/filename, folder/filename)
        exported = folder/'fault.net'
        run('sch', 'export', 'netlist', changed, '--format', 'kicadxml', '-o', exported)
        try: validate(*read_netlist(exported))
        except AssertionError as error: faults.append(dict(mutation=name, detected=True, reason=str(error)))
        else: raise AssertionError('Undetected fault: '+name)
    files = [DEST/n for n in ['pack-balancer.kicad_sch', 'pack-balancer.kicad_pro', 'RaceRemote_Balancer.kicad_sym',
                             'pack-balancer.net', 'pack-balancer.pdf', 'bom.csv', 'sym-lib-table', 'fp-lib-table']]
    files += [erc, ROOT/'hardware/charge-core/RaceRemote_Charge.kicad_sym']
    files += [ROOT/'tools'/n for n in ['build_pack_balancer.py', 'verify_pack_balancer.py',
                                     'build_charge_permit.py', 'build_usb_port_schematic.py', 'verify_usb_detector.py']]
    report = dict(date='2026-09-14', contract='PACK-BAL-01 v0.1', kicad_version=erc_data['kicad_version'],
                  erc_violations=0, components=len(values), pins_including_ep=len(pins), fault_injections=faults,
                  source=dict(url='https://www.ti.com/lit/gpn/bq29209', revision='SLUSA52C March2016',
                              inspected_pages=[4, 6, 7, 10, 13],
                              mirror_url='https://static.chipdip.ru/lib/799/DOC059799688.pdf',
                              mirror_sha256='5d78b2ee2f7e9e8a1e3e1a45afaf8842c88b30fa5ff7b72a738fd8188a1fce7f'),
                  ovp_nominal_V_per_cell=4.30,
                  delay_estimate_s=dict(nominal=.1*9, min=.1*.9*5.5, max=.1*1.1*13.5),
                  delay_scope='Datasheet scale -40..110C times assumed effective capacitance +/-10%; not full shutdown latency',
                  ground_reference_example=dict(
                      assumption='Two ideal3.7V cells; open low-side protector; load pulls protected negative to pack positive',
                      raw_negative_V=0., midpoint_V=3.7, raw_positive_V=7.4,
                      protected_negative_V=7.4,
                      midpoint_relative_to_raw_negative_V=3.7,
                      midpoint_relative_to_protected_negative_V=-3.7,
                      input_absolute_minimum_V=-.3,
                      scope='DC voltage reference counterexample, not a predicted transient or current'),
                  not_verified=['Primary cell voltage regulation and undervoltage/overcurrent protection',
                                'Actual balance current and correction time; datasheet resistor-name ambiguity',
                                'Effective capacitor tolerance under bias and temperature; exact purchased passives',
                                'Battery-referenced enable policy below6V and OUT level/domain translation',
                                'Ground topology, cell disconnect order, storage drain, full charger/BMS integration',
                                'LW charge limits, PCB, assembly or physical balancing/charge'],
                  sha256={p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files})
    (EVIDENCE/'pack-balancer-verification.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
    print(json.dumps(dict(erc=0, components=len(values), pins=len(pins), detected_faults=len(faults))))


if __name__ == '__main__': main()
