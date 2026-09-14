"""DC current-path study, not a charger simulation or battery qualification.

Topology follows TI SLUA938 sections 2 and 3.1. External shunts are our
proposals. Cells are ideal voltage sources; only one balancing FET is on.
No converter dynamics, capacitance, protection FETs, loads or leakage.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def solve(current, top_v, bottom_v, balancing, top_shunt=.1,
          return_shunt=0., balance_r=41., fet_r=1.):
    """Positive cell current = charge; positive return flows PACK_N -> GND.

    KCL: I0 = Itop + IH; Ibottom = I0 - IL.
    KVL: IH*(Rbal+Ron) = Vtop + Itop*Rtop (when H on).
         IL*(Rbal+Ron) = Vbottom + Ibottom*Rreturn (when L on).
    """
    if balancing not in ('none', 'top', 'bottom'):
        raise ValueError('Only none or one balancing FET is modelled')
    if min(current, top_v, bottom_v, top_shunt, return_shunt) < 0:
        raise ValueError('Inputs must be non-negative')
    if balance_r <= 0 or fet_r < 0:
        raise ValueError('Balance resistance must be positive')
    path_r = balance_r + fet_r
    high = ((top_v + top_shunt*current)/(path_r+top_shunt)
            if balancing == 'top' else 0.)
    low = ((bottom_v + return_shunt*current)/(path_r+return_shunt)
           if balancing == 'bottom' else 0.)
    top = current-high
    bottom = current-low
    top_error = top_shunt*top
    bottom_error = return_shunt*bottom
    # Independent node/loop residuals: catches sign or wrong-branch errors.
    residuals = [current-top-high, top+high-bottom-low]
    if balancing == 'top':
        residuals.append(high*path_r-top_v-top_error)
    if balancing == 'bottom':
        residuals.append(low*path_r-bottom_v-bottom_error)
    return dict(source_A=current, top_cell_V=top_v, bottom_cell_V=bottom_v,
                balancing=balancing, top_shunt_ohm=top_shunt,
                return_shunt_ohm=return_shunt, balance_resistor_ohm=balance_r,
                assumed_fet_ohm=fet_r, top_bypass_A=high, bottom_bypass_A=low,
                top_cell_A=top, bottom_cell_A=bottom,
                positive_cell_max_A=max(0., top, bottom),
                top_sense_error_V=top_error, bottom_sense_error_V=bottom_error,
                max_kcl_kvl_residual=max(abs(x) for x in residuals))


def main():
    trip = .348  # Existing NOMINAL candidate; not a battery limit.
    cases = []
    for label, upper, lower, mode, current, low_shunt in [
        ('normal_no_balance', 4., 4., 'none', .25, 0.),
        ('missed_lower_cell', 4.1, 3.9, 'top', .4, 0.),
        ('lower_balance_top_seen', 3.9, 4.1, 'bottom', .4, 0.),
        ('reset_1p5A_seen', 4.1, 3.9, 'top', 1.5, 0.),
        ('setting_2p2A_seen', 4.1, 3.9, 'top', 2.2, 0.),
        ('top_discharge_not_positive_charge', 4.1, 3.9, 'top', 0., 0.),
        ('two_shunts_top_balance', 4.1, 3.9, 'top', .4, .1),
        ('two_shunts_bottom_balance', 3.9, 4.1, 'bottom', .4, .1),
    ]:
        row = solve(current, upper, lower, mode, return_shunt=low_shunt)
        row.update(name=label, nominal_top_threshold_exceeded=row['top_cell_A'] > trip,
                   nominal_return_threshold_exceeded=(row['bottom_cell_A'] > trip
                                                       if low_shunt else None))
        cases.append(row)
    counter = cases[1]
    assert counter['top_cell_A'] < trip < counter['bottom_cell_A']
    assert cases[3]['nominal_top_threshold_exceeded']
    assert cases[4]['nominal_top_threshold_exceeded']
    assert cases[5]['top_cell_A'] < 0 and cases[5]['bottom_cell_A'] == 0

    # Check conservation and the ideal two-sensor coverage across a finite grid.
    # These are forced switch states, not an implementation of the TI algorithm.
    count = 0
    max_residual = 0.
    for current in [0., .05, .25, .348, .4, 1.5, 2.2]:
        for upper in [3., 3.9, 4.2]:
            for lower in [3., 3.9, 4.2]:
                for mode in ['none', 'top', 'bottom']:
                    for return_r in [0., .1]:
                        row = solve(current, upper, lower, mode, return_shunt=return_r)
                        max_residual = max(max_residual, row['max_kcl_kvl_residual'])
                        assert row['positive_cell_max_A'] <= current+1e-12
                        if return_r:
                            either = row['top_cell_A'] > trip or row['bottom_cell_A'] > trip
                            assert either == (row['positive_cell_max_A'] > trip)
                        count += 1
    assert max_residual < 1e-12
    # At the upper-cell threshold, bypass current is still added to lower cell.
    at_trip = trip + (4.1+.1*trip)/(41.+1.)
    report = dict(date='2026-09-14', contract='CHARGE-SENSE-01 v0.1',
                  status='DC analysis only; no final shunt placement selected',
                  sources=[
                      dict(url='https://www.ti.com/lit/an/slua938/slua938.pdf',
                           revision='SLUA938 March 2019', sections='2, 3.1; Figures 1, 3'),
                      dict(url='https://www.ti.com/lit/gpn/bq25887',
                           revision='SLUSD89B November 2019', sections='6, 8.3.4.3')],
                  script_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
                  nominal_detector_threshold_A=trip, cases=cases,
                  lower_cell_A_when_top_reaches_nominal_trip=at_trip,
                  grid_cases=count, maximum_grid_equation_residual=max_residual,
                  assumptions=['Ideal fixed-voltage cells, no traction or logic load',
                               'One balancing FET at most, forced state, positive source current',
                               '41 ohm resistor and 1 ohm FET are illustrative, not selected parts',
                               'MID input leakage and 300 ohm sense drop omitted',
                               'PACK_N and charger GND connected only by the proposed return shunt',
                               'Ideal instantaneous threshold; INA300 window/delay not simulated'],
                  excludes=['Switching, capacitor discharge, startup/brownout',
                            'Open/short faults, battery protector, USB/debug ground bypass',
                            'Actual temperature, tolerances, charge regulation loop and termination',
                            'Cell model, LW limits, heat, physical measurements'],
                  single_top_shunt_covers_both_cells_at_same_threshold=False,
                  two_shunts_are_hardware_qualified=False,
                  physical_charge_performed=False)
    out = ROOT/'docs/evidence/charge-sense-paths.json'
    out.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8', newline='\n')
    print(json.dumps(dict(grid_cases=count, counterexample_top_A=counter['top_cell_A'],
                         counterexample_bottom_A=counter['bottom_cell_A'],
                         lower_A_at_nominal_trip=at_trip, max_residual=max_residual)))


if __name__ == '__main__':
    main()
