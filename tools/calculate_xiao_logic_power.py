"""Conditional DC sensitivity model; not transient simulation or a device rating."""
import itertools
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/evidence/xiao-logic-power-v02'


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
        ppass = (vin - v) * iload  # Excludes U1/U2 own supply currents.
        assert abs(vin * iload - (ppass + v * iload)) < 1e-12
        rows.append({'ldo_v': v, 'input_v': vin, 'camera_load_a': load,
                     'preload_a': v / preload, 'preload_w': v * v / preload,
                     'ldo_load_a': iload, 'pass_loss_w': ppass,
                     'headroom_beyond_0_5v': vin - v - .5})
    def bounds(key):
        return {'min': min(r[key] for r in rows), 'max': max(r[key] for r in rows)}
    lo, hi = bounds('ldo_v').values()
    assert min(r['preload_a'] for r in rows) > .010
    assert min(r['headroom_beyond_0_5v'] for r in rows) > 0
    assert max(r['ldo_load_a'] for r in rows) < 1
    # Use the actual SOT23-5 table, not WLP headline values or an invented RON.
    # These maxima are specified at VDD=3.3V. Applying them at our ~4V is
    # explicitly a design-budget assumption, NOT a full operating bound.
    sot_forward_budget = []
    for load, drop in [(.1, .065), (.2, .090), (.5, .175)]:
        sot_forward_budget.append({'load_a': load, 'assumed_drop_v': drop,
                                   'bat_range_v_if_drop_between_zero_and_budget': [lo - drop, hi],
                                   'switch_loss_upper_w_if_drop_budget_holds': load * drop})
    # A lower 4.5V shared rail fails the same headroom requirement: guard
    # against interpreting a nominal "5V" label as evidence of regulation.
    assert 4.5 - hi - .5 < 0
    report = {
        'date': '2026-09-15', 'contract': 'XIAO-LOGIC-01 v0.2',
        'status': 'Conditional settled-DC calculation only', 'corners': len(rows),
        'nominal_ldo_v': 1.204 * (1 + 23200 / 10000),
        'ranges': {k: bounds(k) for k in rows[0] if k not in ('input_v', 'camera_load_a')},
        'external_service_vbus_connection': False,
        'sot23_forward_budget': sot_forward_budget,
        'switch_drop_at_0_6a_v': None,
        'reason_0_6a_drop_unknown': 'No specified 600mA table point; no interpolation claimed as a guarantee',
        'nominal_pass_loss_at_0_5a_w': (5 - 3.99728) * (.5 + 3.99728 / 360 + 3.99728 / 33200),
        'invalid_4_5v_rail_headroom_v': 4.5 - hi - .5,
        'conditions': [
            'Waveshare output measured 4.75..5.25V; not a supplier guarantee',
            'TPS737 legacy overall +/-3%, R1/R2 0.1%, signed IFB 0.6uA extra allowance',
            'R3 360ohm 1%; camera load 0..0.6A is exploration, not observed demand',
            'MAX40200AUK+T SOT23: 65/90/175mV max at 100/200/500mA at 3.3V; extension to ~4V is conditional',
            'No converter ripple, wire loss, Q1 loss, U1/U2 own supply currents, startup or source transition model',
            'MAX40200 EN connected locally to VDD; reverse blocking is internal, no external CE/USB wire',
            'No guaranteed reverse switching time inferred from typical EN timing or marketing text',
            'C3/C4 nominal1uF must retain >=0.33uF each; total effective OUT capacitance including XIAO <=100uF'
        ],
        'physical_tests': False, 'pcb_current_rating_a': None,
        'battery_charging_qualified': False, 'fabrication_released': False,
    }
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / 'dc-calculation.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
