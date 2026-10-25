"""Package the recorded JADX output; --check verifies the published recovery artifacts."""
import hashlib
import json
from pathlib import Path
import re
import sys
import zipfile

HERE = Path(__file__).resolve().parent
FIELD = re.compile(r'^\s*private static (?:final )?(byte|short|int|long|float|double|boolean|char|String) ([\w$]+)(?: = ("(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[^;\n]+))?;', re.M)
GETTER = re.compile(r'public final (?:byte|short|int|long|float|double|boolean|char|String) ([\w$]+)\(\) \{\s*if \(!LiveLiteralKt\.isLiveLiteralsEnabled\(\)\) \{\s*return ([\w$]+);')
CALL = re.compile(r'LiveLiterals\$[\w$]+\.INSTANCE\.[\w$]+\(\)')
QUOTED = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/', re.S)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def literal_values(text, class_name):
    # shortcut: recognizes JADX1.5.6 LiveLiterals output; review this parser if the decompiler changes.
    fields = {}
    defaults = {'boolean': 'false', 'String': 'null', 'char': r"'\u0000'", 'long': '0L',
                'float': '0.0f', 'double': '0.0d'}
    for kind, name, value in FIELD.findall(text):
        fields[name] = value.strip() if value else defaults.get(kind, '0')
    return {class_name + '.INSTANCE.' + method + '()': fields[field]
            for method, field in GETTER.findall(text) if field in fields}


def readable(text, values):
    text = '\n'.join(line.rstrip() for line in text.splitlines()
                     if not line.lstrip().startswith(('@Metadata(', '@SourceDebugExtension('))) + '\n'
    text = re.sub(r'(?m)^[ \t]+', lambda m: m[0].replace('\t', '    '), text)
    result, start = [], 0
    for token in QUOTED.finditer(text):
        result.append(CALL.sub(lambda m: values.get(m[0], m[0]), text[start:token.start()]))
        result.append(token[0])
        start = token.end()
    result.append(CALL.sub(lambda m: values.get(m[0], m[0]), text[start:]))
    return ''.join(result)


def self_test():
    sample = '''private static int zero;
private static String text = "a;b";
private static float fraction;
public final int getZero() { if (!LiveLiteralKt.isLiveLiteralsEnabled()) { return zero; } }
public final String getText() { if (!LiveLiteralKt.isLiveLiteralsEnabled()) { return text; } }
public final float getFraction() { if (!LiveLiteralKt.isLiveLiteralsEnabled()) { return fraction; } }
'''
    values = literal_values(sample, 'LiveLiterals$Test')
    assert values['LiveLiterals$Test.INSTANCE.getZero()'] == '0'
    assert values['LiveLiterals$Test.INSTANCE.getText()'] == '"a;b"'
    assert values['LiveLiterals$Test.INSTANCE.getFraction()'] == '0.0f'
    call = 'LiveLiterals$Test.INSTANCE.getZero()'
    text = f'int n = {call}; String s = "{call}"; // {call}\n'
    assert readable(text, values) == f'int n = 0; String s = "{call}"; // {call}\n'


def check():
    manifest = json.loads((HERE / 'manifest.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(HERE / 'sources.zip') as archive:
        assert archive.testzip() is None
        actual = set(archive.namelist())
        assert actual == set(manifest['archive_entries_sha256'])
        for name, digest in manifest['archive_entries_sha256'].items():
            assert sha(archive.read(name)) == digest, name
        for gap in manifest['explicit_structured_gaps']:
            name = 'fallback-' + gap['apk'] + '/sources/' + gap['path']
            assert name in actual, name
            assert b'Method not decompiled:' not in archive.read(name), name
    assert sha((HERE / 'sources.zip').read_bytes()) == manifest['archive_sha256']
    for name, digest in manifest['readable_sha256'].items():
        assert sha((HERE / name).read_bytes()) == digest, name
    assert manifest['original_firmware']['image_sha256'] == '6605db3054c823e6c1be2b5c62d9cd90b5471d442fd5b102a8f6e122f38b4126'
    print(f"Verified {len(actual)} archived files, readable views and fallback coverage")


def package(work):
    entries, gaps, inventories = {}, [], {}
    for group in ['app', 'tests', 'fallback-app', 'fallback-tests']:
        root = work / group
        files = sorted((root / 'sources').rglob('*.java'))
        assert files, group
        original_names = set()
        for path in files:
            relative = path.relative_to(root).as_posix()
            assert relative.startswith('sources/vea/raceremote/'), relative
            data = path.read_bytes()
            entries[group + '/' + relative] = data
            text = data.decode('utf-8')
            original_names.update(re.findall(r'compiled from: ([^\r\n*]+)', text))
            if group in ['app', 'tests'] and 'Method not decompiled:' in text:
                gaps.append({'apk': group, 'path': path.relative_to(root / 'sources').as_posix(),
                             'methods': re.findall(r'Method not decompiled: ([^"\r\n]+)', text)})
        inventories[group] = {'java_files': len(files), 'original_source_names': sorted(original_names)}
        if (root / 'AndroidManifest.xml').is_file():
            entries[group + '/AndroidManifest.xml'] = (root / 'AndroidManifest.xml').read_bytes()
    assert inventories['app']['java_files'] == 172
    assert inventories['tests']['java_files'] == 27
    assert inventories['fallback-app']['java_files'] == 13
    assert inventories['fallback-tests']['java_files'] == 5
    views = {}
    for group in ['app', 'tests']:
        root = work / group / 'sources'
        values = {}
        for path in root.rglob('LiveLiterals$*.java'):
            values.update(literal_values(path.read_text(encoding='utf-8'), path.stem))
        selected = [root / 'vea/raceremote/control' / (name + '.java') for name in
                    ['UdpControlPacket', 'UdpControlSession', 'UdpControlClient', 'ControlEndpoint',
                     'ControlEndpointProbe', 'CarControlConnection', 'DiscoveryPacket',
                     'DiscoveredCar', 'CarDiscovery']] if group == 'app' else [
            root / 'vea/raceremote' / (name + '.java') for name in
            ['ControlEndpointProbeTest', 'ControlStatusFixtureKt', 'UdpControlDeviceBenchTest',
             'UdpControlScreenDeviceTest', 'UdpDiscoveryDeviceTest']]
        for path in selected:
            if path.stem.startswith('LiveLiterals$'):
                continue
            content = readable(path.read_text(encoding='utf-8'), values).encode('utf-8')
            name = 'readable/' + group + '/' + path.relative_to(root).as_posix()
            target = HERE / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(content)
            views[name] = sha(content)
    with zipfile.ZipFile(HERE / 'sources.zip', 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, date_time=(2026, 9, 26, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, data)
    manifest = json.loads((HERE / 'manifest.json').read_text(encoding='utf-8'))
    manifest.update(inventories=inventories, explicit_structured_gaps=gaps,
                    archive_entries_sha256={name: sha(data) for name, data in sorted(entries.items())},
                    archive_sha256=sha((HERE / 'sources.zip').read_bytes()), readable_sha256=views)
    (HERE / 'manifest.json').write_bytes((json.dumps(manifest, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    check()


if __name__ == '__main__':
    self_test()
    if sys.argv[1:] == ['--check']:
        check()
    elif len(sys.argv) == 2:
        package(Path(sys.argv[1]).resolve())
    else:
        raise SystemExit('Supply the recorded JADX working directory or --check')
