"""Run six Android control clients against the firmware's actual C++ Control core.

Only host processes and loopback sockets are used. No USB, flash, GPIO or camera.
The dedicated JUnit test is skipped in ordinary runs without RC_CONTROL_BRIDGE.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
TEST = 'vea.raceremote.control.FirmwareFleetTest'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--firmware', type=Path, default=ROOT.parent / 'RC_CAR_ESP32')
    parser.add_argument('--output', type=Path, default=ROOT / 'build/firmware-fleet-v01')
    args = parser.parse_args()
    firmware = args.firmware.resolve()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    compiler = shutil.which('g++')
    if not compiler:
        parser.error('g++ with C++17 support must be on PATH')
    header = firmware / 'prototype/control.h'
    original = header.read_text(encoding='utf-8')
    source_hash = sha(header)
    mutation_from = 'if (active || session == 0) return false;'
    if original.count(mutation_from) != 1:
        raise RuntimeError('Firmware changed: review the ownership negative control before running')
    variant = output / 'allow-takeover'
    variant.mkdir(exist_ok=True)
    (variant / 'control.h').write_text(original.replace(mutation_from, 'if (session == 0) return false;'), encoding='utf-8')
    report = {
        'scope': 'JVM Android client and actual C++ control.h via host transport adapter; not ESP32 main/RTOS/HAL, RF or physical cars',
        'firmware_commit': subprocess.check_output(['git', '-C', str(firmware), 'rev-parse', 'HEAD'], text=True).strip(),
        'firmware_control_sha256': source_hash,
        'compiler': subprocess.check_output([compiler, '--version'], text=True).splitlines()[0],
        'source_sha256': {p: sha(ROOT / p) for p in [
            'tools/firmware_control_bridge.cpp', 'tools/run_firmware_fleet.py',
            'app/src/main/java/vea/raceremote/control/CarControlClient.kt',
            'app/src/main/java/vea/raceremote/control/ControlPacket.kt',
            'app/src/test/java/vea/raceremote/control/FirmwareFleetTest.kt']},
        'runs': {},
    }
    try:
        for name, include in [('baseline', header.parent), ('allow-takeover', variant)]:
            executable = output / f'{name}.exe'
            compile_command = [compiler, '-std=c++17', '-O2', '-Wall', '-Wextra', '-Werror', '-pedantic',
                               '-I', str(include), str(ROOT / 'tools/firmware_control_bridge.cpp'), '-o', str(executable)]
            compiled = subprocess.run(compile_command, capture_output=True, text=True, timeout=60)
            (output / f'{name}-compile.txt').write_text(compiled.stdout + compiled.stderr, encoding='utf-8')
            compiled.check_returncode()
            env = os.environ.copy()
            env['RC_CONTROL_BRIDGE'] = str(executable)
            command = [str(ROOT / 'gradlew.bat'), ':app:testDebugUnitTest', '--tests', TEST,
                       '--rerun-tasks', '--console=plain']
            print(f'Running {name}: six clients and six native control instances', flush=True)
            run = subprocess.run(command, cwd=ROOT, env=env, capture_output=True, text=True,
                                 encoding='utf-8', errors='replace', timeout=240)
            (output / f'{name}-gradle.txt').write_text(run.stdout + run.stderr, encoding='utf-8')
            junit = ROOT / f'app/build/test-results/testDebugUnitTest/TEST-{TEST}.xml'
            if not junit.exists():
                raise RuntimeError(f'{name}: no JUnit report; inspect Gradle output')
            shutil.copyfile(junit, output / f'{name}-junit.xml')
            result = ET.parse(junit).getroot()
            counts = {k: int(result.get(k, 0)) for k in ['tests', 'failures', 'errors', 'skipped']}
            entry = {'gradle_exit_code': run.returncode, **counts, 'bridge_sha256': sha(executable),
                     'junit_sha256': sha(junit), 'control_header_sha256': sha(include / 'control.h')}
            report['runs'][name] = entry
            if name == 'baseline':
                if run.returncode or counts != {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}:
                    raise RuntimeError('Native baseline failed; inspect baseline-junit.xml and baseline-gradle.txt')
            else:
                failures = result.findall('.//failure')
                detected = run.returncode != 0 and counts['tests'] == 1 and counts['skipped'] == 0 and any(
                    'Second connection stole car' in failure.get('message', '') for failure in failures)
                entry['ownership_mutation_detected'] = detected
                if not detected:
                    raise RuntimeError('Ownership mutation was not rejected by the expected assertion')
            print(json.dumps({name: entry}), flush=True)
        if sha(header) != source_hash:
            raise RuntimeError('Firmware source changed during the run')
        report['passed'] = True
    finally:
        (output / 'summary.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(f'Evidence: {output / "summary.json"}', flush=True)


if __name__ == '__main__':
    main()
