"""Recompute the priced subset of charger candidates; this is not a full BOM."""
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'docs/charge-parts.csv'
OUT = ROOT / 'docs/evidence/charger-cost-comparison.json'


def main():
    with SOURCE.open(encoding='utf-8-sig', newline='') as f:
        rows = list(csv.reader(f, delimiter=';'))
    assert all(len(r) == 22 for r in rows)
    parts = {r[0]: r for r in rows[1:]}
    assert len(parts) == len(rows) - 1

    def offer(part, index):
        r = parts[part]
        need, purchase, price, cost = (int(r[start + index]) for start in [5, 8, 11, 14])
        assert purchase >= need and purchase >= int(r[4])
        if part in ['CHG-CD-LOT', 'CHG-OC-SHUNT']:
            assert purchase % 5 == 0
        assert cost == purchase * price
        return {'id': part, 'url': r[3], 'needed': need, 'purchase': purchase,
                'unit_RUB': price, 'spend_RUB': cost, 'stock': int(r[17]),
                'stock_sufficient': int(r[17]) >= purchase, 'checked': r[21]}

    def cheapest(ids, index):
        choices = [offer(part, index) for part in ids]
        return min((o for o in choices if o['stock_sufficient']), key=lambda o: o['spend_RUB'])

    common_ids = ['USB-SRC-PI3', 'USB-SRC-CC', 'USB-INPUT-GATE', 'USB-AUX-LDO',
                  'CHG-MCU', 'WD-3431', 'PROT-HY-CB']
    scenarios = []
    for index, count in enumerate([1, 4, 6]):
        common = [offer(part, index) for part in common_ids]
        common.append(cheapest(['USB-GCT-ONE', 'USB-GCT-TWO', 'USB-GCT-FOUR'], index))
        variant_a = [offer('CHG-886', index), offer('BAL-29209', index)]
        variant_b = [cheapest(['CHG-887-ONE', 'CHG-887-LOT'], index)]
        b_guard = cheapest(['CHG-OC-ONE', 'CHG-OC-LOT'], index)
        b_cd = cheapest(['CHG-CD-ONE', 'CHG-CD-LOT'], index)
        b_bias = offer('CHG-BIAS-LDO', index)
        mcu_io = offer('CHG-MCU-IO', index)
        oc_shunt = offer('CHG-OC-SHUNT', index)
        permit = [offer(part, index) for part in ['CHG-PERMIT-FF', 'CHG-PERMIT-SUP', 'CHG-PERMIT-SCHMITT']]
        permit_cost = sum(o['spend_RUB'] for o in permit)
        subtotal = sum(o['spend_RUB'] for o in common)
        a = subtotal + sum(o['spend_RUB'] for o in variant_a)
        b = subtotal + sum(o['spend_RUB'] for o in variant_b)
        scenarios.append({'cars': count, 'common': common, 'A_only': variant_a, 'B_only': variant_b,
                          'common_subtotal_RUB': subtotal, 'A_priced_subset_RUB': a,
                          'B_priced_subset_RUB': b, 'A_minus_B_RUB': a-b,
                          'B_current_detector_candidate': b_guard,
                          'B_subset_plus_detector_RUB': b+b_guard['spend_RUB'],
                          'A_minus_B_with_detector_RUB': a-b-b_guard['spend_RUB'],
                          'permit_latch_candidate': permit,
                          'permit_latch_ICs_RUB': permit_cost,
                          'B_cd_actuator_candidate': b_cd,
                          'cd_actuator_ICs_RUB': b_cd['spend_RUB'],
                          'B_subset_with_CD_candidate_RUB': b+b_guard['spend_RUB']+permit_cost+b_cd['spend_RUB'],
                          'B_bias_regulator_candidate': b_bias,
                          'MCU_IO_candidate': mcu_io,
                          'OC_shunt_candidate': oc_shunt,
                          'B_latest_priced_subset_RUB': b+b_guard['spend_RUB']+permit_cost+b_cd['spend_RUB']+b_bias['spend_RUB']+mcu_io['spend_RUB']+oc_shunt['spend_RUB'],
                          'B_subset_with_MCU_IO_candidate_RUB': b+b_guard['spend_RUB']+permit_cost+b_cd['spend_RUB']+b_bias['spend_RUB']+mcu_io['spend_RUB'],
                          'B_subset_with_regulated_CD_candidate_RUB': b+b_guard['spend_RUB']+permit_cost+b_cd['spend_RUB']+b_bias['spend_RUB'],
                          'A_subset_plus_one_permit_latch_RUB': a+permit_cost,
                          'B_subset_plus_detector_and_one_permit_latch_RUB': b+b_guard['spend_RUB']+permit_cost,
                          'stock_shortages': [o['id'] for o in common+variant_a+variant_b+[b_guard, b_cd, b_bias, mcu_io, oc_shunt]+permit
                                              if not o['stock_sufficient']],
                          'complete_purchase_ready': False})
    report = {'date': '2026-09-14', 'scope': 'Conditional priced subset, not full BOM or authorization to buy',
              'source_sha256': hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
              'scenarios': scenarios,
              'missing_costs': ['PCB fabrication/assembly, footprints, shipping and spares',
                                'Inductor, capacitors, resistors, thermistor, harness and power MOSFETs',
                                'Permit/CD/regulator passives, qualified supply integration and source-policy circuit',
                                'A: equivalent actuator not priced; B: CD switches do not close supply-transition qualification',
                                'A: complete per-cell charge control; B: remaining OC passives, interruption integration and reset circuit',
                                'Common programming tool and MCU board/debug access'],
              'limitations': ['Current-limit and battery protection candidates are not approved complete circuits',
                              'Use B_latest_priced_subset_RUB for current B; MCU_IO field omits the selected shunt and earlier fields omit the IO inverter',
                              'B_subset_with_CD_candidate_RUB retains the earlier subset without U406; use regulated_CD field for current candidate',
                              'USB-AUX-LDO and CHG-BIAS-LDO are distinct placements of one SKU; aggregate them when ordering',
                              'Permit supplement assumes one identical latch per A/B car; full integration/count is not qualified',
                              'TPS3431 stock is insufficient for 4/6; substituted watchdog pricing is unknown',
                              'BQ25887 single-unit offer lacks exact ordering suffix confirmation',
                              'Prices are recorded card snapshots; this script does not check live stock',
                              'No cost advantage or area/thermal fit of the completed board has been proven']}
    OUT.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps([{k: s[k] for k in ['cars', 'common_subtotal_RUB', 'A_priced_subset_RUB',
                      'B_priced_subset_RUB', 'B_subset_plus_detector_RUB', 'A_minus_B_with_detector_RUB',
                      'permit_latch_ICs_RUB', 'A_subset_plus_one_permit_latch_RUB',
                      'B_subset_plus_detector_and_one_permit_latch_RUB',
                      'B_subset_with_regulated_CD_candidate_RUB',
                      'B_subset_with_MCU_IO_candidate_RUB',
                      'B_latest_priced_subset_RUB',
                      'stock_shortages']} for s in scenarios]))


if __name__ == '__main__':
    main()
