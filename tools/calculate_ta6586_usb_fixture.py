"""DC feasibility estimate for the proposed USB-only static fixture; no hardware proof."""
import itertools
import json
from pathlib import Path


def calculate():
    states = {"00": (None, None), "10": (1, 0), "01": (0, 1), "11": (0, 0)}
    rows = []
    max_kcl_error = 0.0
    max_power_error = 0.0
    for usb, series, loads, icc in itertools.product(
        (4.75, 5.25), (47 * .95, 47 * 1.05),
        itertools.product((990.0, 1010.0), repeat=4), (0.0, .007),
    ):
        for state, levels in states.items():
            conductance = 0.0
            for level, upper, lower in zip(levels, loads[::2], loads[1::2]):
                conductance += (1 / (upper + lower) if level is None
                                else 1 / (lower if level else upper))
            vm = (usb - series * icc) / (1 + series * conductance)
            outputs = [vm * (lower / (upper + lower) if level is None else level)
                       for level, upper, lower in zip(levels, loads[::2], loads[1::2])]
            # Independently sum the four resistor losses and current in the ideal bridge.
            upper_i = [(vm - out) / r for out, r in zip(outputs, loads[::2])]
            lower_i = [out / r for out, r in zip(outputs, loads[1::2])]
            bridge_i = sum(low - up for level, up, low in zip(levels, upper_i, lower_i)
                           if level == 1)
            source_i = (usb - vm) / series
            resistor_p = [(vm - out) ** 2 / r for out, r in zip(outputs, loads[::2])]
            resistor_p += [out ** 2 / r for out, r in zip(outputs, loads[1::2])]
            series_p = source_i ** 2 * series
            max_kcl_error = max(max_kcl_error, abs(source_i - sum(upper_i) - bridge_i - icc))
            max_power_error = max(max_power_error,
                                  abs(usb * source_i - series_p - sum(resistor_p) - vm * icc))
            rows.append(dict(state=state, vm_V=vm, source_mA=source_i * 1000,
                             series_W=series_p, load_max_W=max(resistor_p)))
    assert max_kcl_error < 1e-12 and max_power_error < 1e-12
    r_min = 47 * .95
    return {
        "contract": "TA-USB-STATIC-01 v0.1", "evidence": "ideal DC calculation only",
        "assumptions": {
            "USB_measured_V": [4.75, 5.25], "series_operating_ohm": [r_min, 47 * 1.05],
            "four_load_resistors_ohm": [990, 1010], "assumed_ICC_mA": [0, 7],
            "ICC_caveat": "7 mA is specified at VCC=6 V, not guaranteed at fixture voltage",
            "output_model": "ideal switches or open; no transient, leakage or dropout model",
            "resistor_caveat": "operating resistance bounds assumed; no thermal/drift qualification",
        },
        "corner_count": len(rows),
        "states": {state: {key: [min(r[key] for r in rows if r['state'] == state),
                                     max(r[key] for r in rows if r['state'] == state)]
                           for key in ("vm_V", "source_mA", "series_W", "load_max_W")}
                   for state in states},
        "hard_short_source_mA": 5.25 / r_min * 1000,
        "hard_short_series_W": 5.25 ** 2 / r_min,
        "downstream_470uF_plus20percent_energy_mJ": .5 * 470e-6 * 1.2 * 5.25 ** 2 * 1000,
        "capacitor_caveat": "downstream capacitor discharge bypasses the series resistor",
        "max_KCL_residual_A": max_kcl_error, "max_power_residual_W": max_power_error,
        "physical_measurements": None,
    }


if __name__ == "__main__":
    destination = Path(__file__).resolve().parents[1] / "docs/evidence/ta6586-usb-calculation.json"
    result = calculate()
    destination.write_text(json.dumps(result, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2, ensure_ascii=False))
