"""Offline audit of one frozen research build. No device commands or product build."""
from pathlib import Path
import hashlib, json, os, re, subprocess, sys, zipfile
import xml.etree.ElementTree as ET

repo = Path(__file__).resolve().parents[3]
run = repo / sys.argv[1]
record = json.loads((run / 'result.json').read_text(encoding='utf-8'))
assert record['exitCode'] == 0
sdk = Path(record['sdk'])
env = dict(os.environ, JAVA_HOME=record['jdk'])
out = run / 'artifact-audit'
out.mkdir(exist_ok=False)
def sha(data): return hashlib.sha256(data).hexdigest()
def command(label, argv):
    p = subprocess.run([str(a) for a in argv], env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=120)
    (out / (label + '.log')).write_bytes(p.stdout)
    assert p.returncode == 0, (label, p.returncode)
    commands.append({'label': label, 'argv': [str(a) for a in argv], 'exitCode': p.returncode,
                     'bytes': len(p.stdout), 'sha256': sha(p.stdout)})
    return p.stdout.decode('utf-8', errors='strict')

commands = []
inputs = json.loads((run / 'inputs.json').read_text(encoding='utf-8'))
for name, meta in inputs.items():
    assert sha((repo / name).read_bytes()) == meta['sha256'], name

test_sources = {}
for module in ('probe', 'outsider'):
    root = repo / 'docs/research/m10-jpeg-r3/host' / module / 'src/test/java'
    for file in root.rglob('*.java'):
        text = file.read_text(encoding='utf-8')
        package = re.search(r'package\s+([^;]+);', text).group(1)
        for name in re.findall(r'@Test(?:\([^)]*\))?\s+public\s+void\s+(\w+)\s*\(', text):
            identity = package + '.' + file.stem + '#' + name
            assert identity not in test_sources
            test_sources[identity] = file.relative_to(repo).as_posix()
results = {}
for file in (run / 'reports').rglob('TEST-*.xml'):
    for case in ET.parse(file).getroot().iter('testcase'):
        identity = case.attrib['classname'] + '#' + case.attrib['name']
        assert identity not in results
        assert not list(case), (identity, 'not PASS')
        results[identity] = 'PASS'
assert results.keys() == test_sources.keys(), (results.keys() - test_sources.keys(), test_sources.keys() - results.keys())
old_tests = []
for name in test_sources.values():
    if name in old_tests: continue
    old = subprocess.run(['git', 'show', 'f85b8a9c5d14a745f8d9e3a33ca43c9c41eba890:' + name], cwd=repo, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if old.returncode == 0:
        assert old.stdout == (repo / name).read_bytes(), name
        old_tests.append(name)
old_methods = [identity for identity, name in test_sources.items() if name in old_tests]
assert len(old_methods) == 60

lint = {}
for module in ('probe', 'outsider'):
    issues = list(ET.parse(run / 'reports' / module / 'reports/lint-results-debug.xml').getroot().iter('issue'))
    lint[module] = {'errors': sum(i.get('severity') in ('Error', 'Fatal') for i in issues),
                    'warnings': sum(i.get('severity') == 'Warning' for i in issues),
                    'ids': [i.get('id') for i in issues]}
    assert lint[module]['errors'] == 0

android = '{http://schemas.android.com/apk/res/android}'
apk_info = {}
for module in ('probe', 'outsider'):
    apk = Path(record[module + 'Apk']['path'])
    assert sha(apk.read_bytes()) == record[module + 'Apk']['sha256']
    manifest = command(module + '-manifest', [sdk / 'cmdline-tools/latest/bin/apkanalyzer.bat', 'manifest', 'print', apk])
    root = ET.fromstring(manifest)
    assert root.get('package') == 'com.cooper.lithic.m10r3.' + module
    assert not list(root.iter('uses-permission'))
    uses = root.find('uses-sdk')
    assert uses.get(android + 'minSdkVersion') == '26'
    assert uses.get(android + 'targetSdkVersion') == '36'
    if module == 'probe':
        worker = root.find('application/service')
        assert worker.get(android + 'exported') == 'false'
        assert worker.get(android + 'process') == ':jpeg_worker'
    signing = command(module + '-signature', [sdk / 'build-tools/36.0.0/apksigner.bat', 'verify', '--verbose', '--print-certs', apk])
    cert = re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', signing).group(1)
    assert cert == '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9'
    command(module + '-badging', [sdk / 'build-tools/36.0.0/aapt2.exe', 'dump', 'badging', apk])
    packages = command(module + '-dex', [sdk / 'cmdline-tools/latest/bin/apkanalyzer.bat', 'dex', 'packages', '--defined-only', apk])
    assert 'org.junit' not in packages and 'org.hamcrest' not in packages
    with zipfile.ZipFile(apk) as z:
        assert not any(n.startswith('lib/') for n in z.namelist())
    apk_info[module] = dict(record[module + 'Apk'], certificateSHA256=cert, permissions=[], minSdk=26, targetSdk=36, nativeLibraries=False)

old_apks = {}
old_root = Path('E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP/docs/research/m10-jpeg-r3/host')
expected = {'probe':'a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684', 'outsider':'a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879'}
for module in expected:
    file = old_root / module / 'build/outputs/apk/debug' / (module + '-debug.apk')
    old_apks[module] = {'path': str(file), 'sha256': sha(file.read_bytes()), 'bytes': file.stat().st_size}
    assert old_apks[module]['sha256'] == expected[module]
    old_sign = command('old-' + module + '-signature', [sdk / 'build-tools/36.0.0/apksigner.bat', 'verify', '--print-certs', file])
    assert re.search(r'certificate SHA-256 digest: ([0-9a-f]+)', old_sign).group(1) == apk_info[module]['certificateSHA256']

summary = {'hostOnly':True, 'androidRun':False, 'inputCount':len(inputs),
           'tests':len(results), 'oldUnchangedMethods':len(old_methods), 'newMethods':len(results)-len(old_methods),
           'results':results, 'sourceMethods':test_sources, 'lint':lint, 'apk':apk_info,
           'originalF1AApksUnchanged':old_apks, 'commands':commands}
(out / 'result.json').write_text(json.dumps(summary, indent=2), encoding='utf-8')
print(json.dumps({k:v for k,v in summary.items() if k not in ('results','sourceMethods','commands')}, indent=2))
