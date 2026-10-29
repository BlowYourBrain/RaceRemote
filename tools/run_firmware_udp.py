"""Run the recovered Kotlin UDP client against the actual reconstructed C++ core (host only)."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
TEST = 'vea.raceremote.control.FirmwareUdpInteropTest'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--firmware', type=Path, required=True)
    parser.add_argument('--gradle', type=Path, default=ROOT / 'gradlew.bat')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/firmware-udp-interop')
    args = parser.parse_args()
    firmware, output = args.firmware.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    (output / 'summary.json').unlink(missing_ok=True)
    executable = output / 'udp-bridge.exe'
    compiled = subprocess.run([sys.executable, '-m', 'ziglang', 'c++', '-std=c++17', '-O0', '-UNDEBUG',
                               '-Wall', '-Wextra', '-Werror', '-pedantic', '-I', str(firmware / 'prototype'),
                               str(firmware / 'tools/udp_bridge.cpp'), '-o', str(executable)],
                              cwd=ROOT, capture_output=True, text=True, encoding='utf-8', errors='replace', timeout=120)
    (output / 'compile.txt').write_text(compiled.stdout + compiled.stderr, encoding='utf-8')
    compiled.check_returncode()
    env = os.environ.copy()
    env['RC_UDP_BRIDGE'] = str(executable)
    junit = ROOT / f'app/build/test-results/testDebugUnitTest/TEST-{TEST}.xml'
    junit.unlink(missing_ok=True)
    run = subprocess.run([str(args.gradle), '--no-daemon', '-PrecoveryInstall=true', ':app:testDebugUnitTest',
                          '--tests', TEST, '--rerun-tasks', '--console=plain'], cwd=ROOT, env=env, capture_output=True,
                         text=True, encoding='utf-8', errors='replace', timeout=240)
    (output / 'gradle.txt').write_text(run.stdout + run.stderr, encoding='utf-8')
    if not junit.exists():
        raise RuntimeError('No JUnit report; inspect gradle.txt')
    target = output / 'junit.xml'
    target.write_bytes(junit.read_bytes())
    result = ET.parse(junit).getroot()
    counts = {k: int(result.get(k, 0)) for k in ('tests', 'failures', 'errors', 'skipped')}
    sources = [firmware / 'prototype/udp_control.h', firmware / 'prototype/control.h',
               firmware / 'prototype/drive_plan.h', firmware / 'prototype/drive_outputs.h', firmware / 'tools/udp_bridge.cpp']
    report = {'scope': 'Actual Kotlin UDP client/socket and C++ core/planner/null backend on host; not ESP32 main/HAL/RF',
              'firmware_commit': subprocess.check_output(['git', '-C', str(firmware), 'rev-parse', 'HEAD'], text=True).strip(),
              'source_sha256': {p.relative_to(firmware).as_posix(): sha(p) for p in sources},
              'android_source_sha256': {p: sha(ROOT / p) for p in ('app/src/main/java/vea/raceremote/control/UdpControlClient.kt',
                  'app/src/main/java/vea/raceremote/control/UdpControlSession.kt',
                  'app/src/test/java/vea/raceremote/control/FirmwareUdpInteropTest.kt')},
              'bridge_sha256': sha(executable), 'junit_sha256': sha(junit), 'gradle_exit_code': run.returncode,
              **counts, 'passed': run.returncode == 0 and counts == {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}}
    (output / 'summary.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({k: v for k, v in report.items() if k not in ('source_sha256', 'android_source_sha256')}, indent=2))
    if not report['passed']:
        raise RuntimeError('Native UDP interop failed; inspect junit.xml/gradle.txt')


if __name__ == '__main__':
    main()
