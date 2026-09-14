"""INA300 bench proposal arithmetic. No waveform, battery or circuit validation."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    shunt = .1
    limit_r = 1740
    resistor_tolerance = .01
    # SBOS613C, 50us mode (no 500uV NAF used in this mode).
    current_min, current_nom, current_max = 19.85e-6, 20e-6, 20.15e-6
    offset_allowance = .0005
    # Illustrative conservative addition of datasheet rows, not a combined guaranteed spec.
    drift_allowance = 100 * .5e-6  # 25C to 125C, worst distance in -40..125C
    psr_allowance = .3 * 150e-6  # AUX 3.0..3.6V relative to 3.3V
    cmr_allowance = 12 * 10**(-100/20)  # assumed common mode 0..10V, relative to 12V
    error = offset_allowance + drift_allowance + psr_allowance + cmr_allowance
    trip_nom = current_nom * limit_r / shunt
    trip_low = (current_min * limit_r * (1-resistor_tolerance)-error)/(shunt*(1+resistor_tolerance))
    trip_high = (current_max * limit_r * (1+resistor_tolerance)+error)/(shunt*(1-resistor_tolerance))
    # Selected shunt Yageo RL2512FK-070R1L: +/-600ppm/C. Body temperature,
    # not ambient. LIMIT resistor TCR is still unknown, so these are partial estimates.
    temperature_cases = []
    for body_c in [-40, 25, 70, 125]:
        drift = abs(body_c - 25) * 600e-6
        r_low = shunt * (1-resistor_tolerance) * (1-drift)
        r_high = shunt * (1+resistor_tolerance) * (1+drift)
        temperature_cases.append({'assumed_shunt_body_C': body_c,
            'shunt_ohm_min': r_low, 'shunt_ohm_max': r_high,
            'partial_trip_min_A': (current_min*limit_r*(1-resistor_tolerance)-error)/r_high,
            'partial_trip_max_A': (current_max*limit_r*(1+resistor_tolerance)+error)/r_low,
            'shunt_power_at_2p2A_max_W': 2.2**2*r_high})
    samples = [{'current_A': i, 'shunt_drop_V': i*shunt, 'shunt_loss_W': i*i*shunt}
               for i in [.25, trip_nom, 1.5, 2.2]]
    # Charge/energy exposure of a hypothetical rectangular pulse, NOT a predicted peak or delay.
    pulses = [{'hypothetical_peak_A': i, 'hypothetical_duration_us': us,
               'charge_C': i*us*1e-6, 'battery_energy_at_8p4V_J': 8.4*i*us*1e-6}
              for i in [1.5, 2.2] for us in [50, 100, 1000]]
    assert abs(.25*.1-.025)<1e-12 and abs(.25**2*.1-.00625)<1e-12
    report = {'date': '2026-09-14', 'contract': 'CHARGE-OC-01 v0.2',
              'status': 'Candidate schematic exists; this file is arithmetic only, no PCB/protection acceptance',
              'source': 'https://www.ti.com/lit/ds/symlink/ina300.pdf',
              'datasheet': 'SBOS613C, June 2021, sections 6.5, 7.3, 7.4',
              'shunt_ohm': shunt, 'limit_resistor_ohm': limit_r,
              'resistor_tolerance_fraction': resistor_tolerance,
              'shunt_part': 'RL2512FK-070R1L',
              'shunt_source': 'https://yageogroup.com/component-documentation/download/specsheet/RL2512FK-070R1L',
              'shunt_rated_power_W_at_70C': 1,
              'shunt_temperature_partial_estimates': temperature_cases,
              'temperature_scope': 'Only shunt TCR added. Body temperature, LIMIT TCR, aging, derating and pulse rating are not qualified.',
              'delay_setting_us': 50, 'delay_scope': 'Comparator setting, not complete turn-off bound',
              'trip_nom_A': trip_nom, 'illustrative_trip_low_A': trip_low,
              'illustrative_trip_high_A': trip_high, 'input_error_allowance_V': error,
              'error_scope': 'Base trip pair omits resistor TCR; separate temperature cases add only shunt TCR. '
                             'Sum of separate datasheet rows, not a combined-condition guarantee; PCB errors/ripple remain open',
              'shunt_samples': samples, 'hypothetical_pulses': pulses,
              'guard_acceptance_proven': False,
              'missing': ['Approved LW continuous and transient charge envelope',
                          'Independent hardware path from fault to charge interruption and startup/brownout inhibit',
                          'Whole-path measured peak/duration including output capacitors',
                          'Shunt effect on cell measurement/balancing and Kelvin routing',
                          'Reverse/balancing currents, middle-tap path and traction separation',
                          'Latched restart behavior with MCU frozen and detector supply cycled']}
    (ROOT/'docs/evidence/charge-current-guard.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['trip_nom_A','illustrative_trip_low_A','illustrative_trip_high_A','input_error_allowance_V','guard_acceptance_proven']}))


if __name__ == '__main__':
    main()
