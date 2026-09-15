"""Run the five loopback video/control UI tests on an explicitly selected Android.

Build app-debug.apk and app-debug-androidTest.apk first. This installs debug APKs;
it never selects a device implicitly, unlocks a phone, or contacts a physical car.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
CLASSES = "vea.raceremote.VideoControlIsolationTest,vea.raceremote.DrivingModeTest,vea.raceremote.CarVideoSelectionTest"
EXPECTED_TESTS = 5


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb", type=Path, default=Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools/adb.exe")
    parser.add_argument("--output", type=Path, default=ROOT / "build/video-android-smoke")
    args = parser.parse_args()
    if not args.adb.is_file():
        parser.error("Supply --adb or set ANDROID_HOME")

    def adb(*command, timeout=30):
        result = subprocess.run([str(args.adb), "-s", args.serial, *command],
                                capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
        if result.returncode:
            raise RuntimeError(f"adb {command[0]} failed: {result.stdout}{result.stderr}")
        return result.stdout

    if adb("get-state").strip() != "device":
        raise RuntimeError("Selected device is not ready")
    sdk = int(adb("shell", "getprop", "ro.build.version.sdk").strip())
    if sdk < 31:
        raise RuntimeError("RaceRemote currently requires Android API31 or newer")
    policy = adb("shell", "dumpsys", "window", "policy")
    if re.search(r"\b(?:showing|mIsShowing)=true\b", policy):
        raise RuntimeError("Selected device is keyguard-locked; no UI tests started")
    display = {"size": adb("shell", "wm", "size").strip(),
               "density": adb("shell", "wm", "density").strip(),
               "emulator": adb("shell", "getprop", "ro.kernel.qemu").strip() == "1"}
    apk_paths = [ROOT / "app/build/outputs/apk/debug/app-debug.apk",
                 ROOT / "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"]
    hashes = {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in apk_paths}
    args.output.mkdir(parents=True, exist_ok=True)
    for apk in apk_paths:
        installed = adb("install", "-r", str(apk), timeout=120)
        if "Success" not in installed:
            raise RuntimeError(f"Install failed for {apk.name}: {installed}")
    print(f"Running {EXPECTED_TESTS} loopback tests on {args.serial}, API{sdk}", flush=True)
    process = subprocess.run([str(args.adb), "-s", args.serial, "shell", "am", "instrument", "-w", "-r",
                              "-e", "class", CLASSES, "vea.raceremote.test/androidx.test.runner.AndroidJUnitRunner"],
                             capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=300)
    transcript = process.stdout + process.stderr
    log_path = args.output / "instrumentation.txt"
    log_path.write_text(transcript, encoding="utf-8", newline="\n")
    passed = process.returncode == 0 and bool(re.search(rf"\bOK \({EXPECTED_TESTS} tests\)", transcript)) and not re.search(
        r"INSTRUMENTATION_STATUS_CODE: -[1-9]|FAILURES!!!|INSTRUMENTATION_FAILED", transcript)
    report = {"serial": args.serial, "api": sdk, "display": display, "classes": CLASSES.split(","), "expected_tests": EXPECTED_TESTS,
              "passed": passed, "adb_exit_code": process.returncode, "apk_sha256": hashes,
              "transcript_sha256": hashlib.sha256(log_path.read_bytes()).hexdigest(),
              "runner_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
              "scope": "Android UI and software loopback; no physical camera, RF, motor or optical latency"}
    (args.output / "result.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(report, indent=2), flush=True)
    if not passed:
        raise RuntimeError(f"Android test run did not pass; inspect {log_path}")


if __name__ == "__main__":
    main()
