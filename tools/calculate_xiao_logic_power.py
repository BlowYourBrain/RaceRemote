"""Conditional DC sensitivity model; not transient simulation or a device rating."""
import itertools
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/evidence/xiao-logic-power-v01'


def main():
    rows = []
    # Overall legacy accuracy plus resistor tolerance and an extra signed IFB
    # allowance. Deliberately pessimistic; not independent statistical errors.
    for rt, rb, accuracy, ifb, preload, vin, load in itertools.product(
        [23200 * .999, 23200 * 1.001], [10000 * .999, 10000 * 1.001],
        [-.03, .03], [-.6e-6, .6e-6], [360 * .99, 360 * 1.01],
        [4.75, 5.25], [0, .1, .2, .5, .6]
    ):
        v = 1.204 * (1 + rt / rb) * (1 + accuracy) + ifb * rt
        # Exact divider current at OUT with signed input-bias current:
        vfb = (v - ifb * rt) / (1 + rt / rb)
        idiv = (v - vfb) / rt
        iload = load + v / preload + idiv
        ppass = (vin - v) * iload  # Excludes U1 ground-pin current.
        assert abs(vin * iload - (ppass + v * iload)) < 1e-12
        rows.append({'ldo_v': v, 'input_v': vin, 'camera_load_a': load,
                     'preload_a': v / preload, 'preload_w': v * v / preload,
                     'ldo_load_a': iload, 'pass_loss_w': ppass,
                     'headroom_beyond_0_5v': vin - v - .5,
                     # 0.14 ohm is a WHAT-IF extension of a 3.6V/200mA
                     # datasheet point, NOT a guaranteed bound at 600mA.
                     'bat_v_if_ron_0_14': v - .14 * load,
                     'switch_loss_w_if_ron_0_14': .14 * load * load})
    def bounds(key):
        return {'min': min(r[key] for r in rows), 'max': max(r[key] for r in rows)}
    lo, hi = bounds('ldo_v').values()
    assert min(r['preload_a'] for r in rows) > .010
    assert min(r['headroom_beyond_0_5v'] for r in rows) > 0
    assert max(r['ldo_load_a'] for r in rows) < 1
    # Steady service USB at least 4.75V vs maximum calculated LDO output.
    ce_off_margin = 4.75 - hi - .080
    assert ce_off_margin > 0
    # Disconnected native VBUS assumed to have no source other than leakage;
    # 300nA * 10.1k is only the CE pin contribution, not the whole XIAO board.
    ce_on_local_margin = lo - 300e-9 * 10100 - .250
    assert ce_on_local_margin > 0
    # A lower 4.5V shared rail fails the same headroom requirement: guard
    # against interpreting a nominal "5V" label as evidence of regulation.
    assert 4.5 - hi - .5 < 0
    report = {
        'date': '2026-09-15', 'contract': 'XIAO-LOGIC-01 v0.1',
        'status': 'Conditional settled-DC calculation only', 'corners': len(rows),
        'nominal_ldo_v': 1.204 * (1 + 23200 / 10000),
        'ranges': {k: bounds(k) for k in rows[0] if k not in ('input_v', 'camera_load_a')},
        'service_usb_off_margin_v': ce_off_margin,
        'ce_pin_only_on_margin_v': ce_on_local_margin,
        'nominal_pass_loss_at_0_5a_w': (5 - 3.99728) * (.5 + 3.99728 / 360 + 3.99728 / 33200),
        'invalid_4_5v_rail_headroom_v': 4.5 - hi - .5,
        'conditions': [
            'Waveshare output measured 4.75..5.25V; not a supplier guarantee',
            'Native USB when present 4.75..5.25V at CE, common valid ground',
            'TPS737 legacy overall +/-3%, R1/R2 0.1%, signed IFB 0.6uA extra allowance',
            'R3 360ohm 1%; camera load 0..0.6A is exploration, not observed demand',
            'LM RON 0.14ohm extrapolated beyond specified 3.6V/200mA point',
            'No converter ripple, wire loss, Q1 loss, ground current, startup or source transition model',
            'Steady CE state assumes intact J4 and no unexpected VBUS source; no single-fault claim'
        ],
        'physical_tests': False, 'pcb_current_rating_a': None,
        'battery_charging_qualified': False, 'fabrication_released': False,
    }
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'dc-calculation.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
