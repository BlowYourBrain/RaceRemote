"""Recheck body and battery service with the motor carrier and control harness."""
import hashlib
import json
from pathlib import Path

import numpy as np
import trimesh

from build_packaging_v05 import intersect, mesh, tube
from layout_zcar_battery import box

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "cad/packaging-v05"


def main():
    sources = {}

    def record(path):
        sources[path.relative_to(ROOT).as_posix()] = hashlib.sha256(path.read_bytes()).hexdigest()
        return path

    def read(path):
        solid = mesh(record(path))
        assert solid.is_volume, path
        assert np.isfinite(solid.vertices).all(), path
        return solid

    def data(name):
        return json.loads(record(ROOT / "docs/evidence" / name).read_text(encoding="utf-8"))

    fixed = {n: read(BASE / f"{n}.stl") for n in
             ["frame-relief-proposal", "servo-installed", "buck", "buck_plug",
              "charge", "antenna", "antenna_support"]}
    for n in ["cover", "adjustable-support", "adjustable-main_fasteners",
              "usb-base", "usb-pcb", "usb-connector", "usb-components"]:
        fixed[n] = read(BASE / "inputs" / f"{n}.stl")
    for n in ["cradle", "screws", "stage_screws"]:
        fixed["xiao-" + n] = read(ROOT / "cad/xiao-mount" / f"{n}.stl")
    fixed["xiao-cap"] = read(ROOT / "cad/harness-guides/cap.stl")
    for n in ["tray", "pads", "clamp", "screws", "nuts"]:
        fixed["mount-" + n] = read(ROOT / "cad/motor-carrier-mount" / f"{n}.stl")
    carrier_names = ["pcb", "ta6586", "R1", "R2", "R3", "R4", "C1", "C2",
                     "C2lead1", "C2lead2", *[f"lead{i}" for i in range(1, 9)]]
    for n in carrier_names:
        fixed["carrier-" + n] = read(ROOT / "cad/motor-carrier" / f"{n}.stl")
    fixed["motor"] = read(ROOT / "cad/components/motor-installed.stl")
    fixed["wire-antenna"] = read(ROOT / "cad/motor-carrier/wire-antenna.stl")
    old = data("packaging-v05.json")
    xiao = data("xiao-mount-v01.json")
    harness = data("control-harness-v01.json")
    routes = old["wire_corridors"] | xiao["revised_corridor_definitions"]
    for n in ["wire-power", "wire-servo", "wire-motor"]:
        r = routes[n]
        fixed[n] = tube(r["points_mm"], r["radius_mm"])
    assert set(harness["routes"]) == {"FI", "BI", "GND"}
    for n, r in harness["routes"].items():
        fixed["control-" + n] = tube(r["points_mm"], r["radius_mm"])
    # Full boxes, with no solder-contact exceptions: none is needed for a moving body.
    assert len(xiao["source_bounding_boxes_mm"]) == 103
    for n, bounds in xiao["source_bounding_boxes_mm"].items():
        bounds = np.array(bounds)
        fixed["xiao-component-" + n] = box(bounds[1] - bounds[0], bounds.mean(axis=0))
    assert all(m.is_volume and np.isfinite(m.vertices).all() for m in fixed.values())
    assert "driver" not in fixed and "wire-control" not in fixed

    battery = read(BASE / "inputs/adjustable-battery.stl")
    guards = read(BASE / "inputs/adjustable-guards.stl")
    band = read(BASE / "inputs/adjustable-band.stl")
    bodies = {"body": read(BASE / "inputs/usb-body.stl"),
              "lid": read(BASE / "inputs/usb-cover.stl")}
    sides = sorted(guards.split(), key=lambda m: m.bounds.mean(axis=0)[0])
    assert len(sides) == 2
    left, right = sides

    def hits(moving, obstacles):
        result = {}
        for n, m in obstacles.items():
            volume = intersect(moving, m)
            assert np.isfinite(volume) and volume >= -1e-7, n
            if volume > .001:
                result[n] = round(volume, 6)
        return result

    # Seller-sized battery is only a rectangular allocation; verify that assumption.
    battery_box = box(battery.extents, battery.bounds.mean(axis=0))
    assert abs(battery_box.volume - battery.volume) < .001
    swept = box(battery.extents + [50, 0, 0], battery.bounds.mean(axis=0) - [25, 0, 0])
    battery_hits = hits(swept, fixed | {"right_guard": right})
    guard_path = []
    for distance in np.arange(0, 6.01, .25):
        moved = left.copy()
        moved.apply_translation([-float(distance), 0, 0])
        guard_path.append({"outward_mm": float(distance),
                           "hits_mm3": hits(moved, fixed | {"battery": battery, "right_guard": right})})
    print("Battery and left guard checked", flush=True)
    poses = [[0, 0, float(z)] for z in np.arange(0, 2.01, .25)]
    poses += [[float(x), 0, 2] for x in np.arange(.25, 1.01, .25)]
    poses += [[1, 0, float(z)] for z in np.arange(2.25, 65.01, .25)]
    body_path = []
    obstacles = fixed | {"battery": battery, "guards": guards, "band": band}
    for i, xyz in enumerate(poses):
        found = {}
        for name, body in bodies.items():
            moved = body.copy()
            moved.apply_translation(xyz)
            found.update({name + "/" + k: v for k, v in hits(moved, obstacles).items()})
        body_path.append({"offset_mm": xyz, "hits_mm3": found})
        if i % 50 == 0:
            print(f"Body pose {i + 1}/{len(poses)}; colliding poses so far: "
                  f"{sum(bool(p['hits_mm3']) for p in body_path)}", flush=True)

    wrong = bodies["body"].copy()
    wrong.apply_translation([0, 0, 7])
    controls = {"battery_with_left_guard_mm3": intersect(swept, left),
                "straight_body_lift_usb_mm3": intersect(wrong, fixed["usb-connector"])}
    assert all(np.isfinite(v) and v > 1 for v in controls.values()), controls
    for p in [Path(__file__), ROOT / "tools/build_packaging_v05.py",
              ROOT / "tools/layout_zcar_battery.py", ROOT / "tools/inspect_zcar.py"]:
        record(p)
    report = {"contract": "CURRENT-SERVICE-01", "version": "0.1", "date": "2026-09-15",
              "nominal_electronics_camera_mm": [0, 3], "fixed_solid_count": len(fixed),
              "fixed_inventory": sorted(fixed), "battery_sweep_mm3": battery_hits,
              "battery_exit_mm": [-50, 0, 0], "battery_sweep_continuous": True,
              "left_guard_samples": guard_path, "body_samples": body_path,
              "positive_controls": controls, "source_sha256": sources,
              "physical_service_verified": False,
              "preconditions": ["Power off; external USB cables unplugged",
                                "Body and USB lid move together; remaining car stays fixed",
                                "Before guard/battery removal, body and battery band removed",
                                "Battery electrically disconnected; loose pack leads not modeled"],
              "limitations": ["Hypothetical body, not selected or printed shell",
                              "Battery is seller-sized box; connectors and pack tail absent",
                              "Body and guard motion sampled every .25mm, not continuous proof",
                              "No finger, magnet, tie, force or cable-flexibility qualification",
                              "Some original suspension/gears omitted; charger still an allocation",
                              "Three control leads retained during body service; carrier/camera removal not checked",
                              "Only nominal 0/+3 pose; no approval of other adjustment positions",
                              "Power/motor/servo corridors remain approximate; no full electrical wiring proof"]}
    destination = ROOT / "docs/evidence/current-service-v01.json"
    destination.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    blocked = [p for p in body_path if p["hits_mm3"]]
    print(json.dumps({"battery_hits": battery_hits, "body_blocked": blocked,
                      "guard_blocked": [p for p in guard_path if p["hits_mm3"]],
                      "positive_controls": controls}, indent=2), flush=True)
    assert not battery_hits and not blocked and not any(p["hits_mm3"] for p in guard_path)


if __name__ == "__main__":
    main()
