"""Bounded desktop checks: reuse accepted evidence; never touch an Android device."""
from pathlib import Path
import datetime, hashlib, json, subprocess, xml.etree.ElementTree as ET

root = Path.cwd()
out = root / 'docs/evidence/v1-b2-20261102/desktop-1'
baseline = 'cf0512bbac1c4155efac7b8186ff573e49f97746'
def git(*args):
    return subprocess.check_output(['git', *args], text=True, encoding='utf-8').strip()
def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

assert git('branch', '--show-current') == 'codex/v1-b2-saf-acceptance'
assert not git('status', '--porcelain', '--untracked-files=all', '--', 'app')
changed = git('diff', '--name-only', baseline, 'HEAD', '--', 'app').splitlines()
assert set(changed) == {'app/build.gradle.kts', 'app/src/b1Qa/AndroidManifest.xml'}
assert not git('diff', baseline, 'HEAD', '--', 'app/src/main', 'app/src/test', 'app/src/androidTest',
               'app/schemas', 'gradle', 'gradle.properties', 'settings.gradle.kts', 'build.gradle.kts')
source = (root / 'app/build.gradle.kts').read_text(encoding='utf-8')
guard = source.split('if (b1QaVerification) {', 1)[1].split('layout.buildDirectory.set', 1)[0]
for mode in ('lithicPrivateTrial', 'm22DeviceVerification', 'c2VisualVerification',
             'c6IsolationVerification', 'v1AcceptanceVerification', 'photoLiteExperiment'):
    assert '!' + mode in guard
assert 'variant.enableAndroidTest = false' in source
assert 'if (b1QaVerification || photoLiteExperiment' in source

prepared = json.loads((out / 'preparation.json').read_text(encoding='utf-8'))
protected = []
for item in prepared['protectedApks']:
    p = root / item['path']
    observed = dict(path=item['path'], bytes=p.stat().st_size, sha256=sha(p))
    assert observed['bytes'] == item['bytes'] and observed['sha256'] == item['sha256'], str(p)
    protected.append(observed)
assert len(protected) == 18
for item in prepared['preview16']['gateEvidence']:
    p = root / item['path']
    assert p.stat().st_size == item['bytes'] and sha(p) == item['sha256'], str(p)

r1 = root / 'docs/evidence/v1-b1-r1-20261102/desktop-1'
result = json.loads((r1 / 'test-2/result.json').read_text(encoding='utf-8'))
counts = dict(tests=0, failures=0, errors=0, skipped=0)
for suite in result['suites']:
    p = r1 / 'test-2/junit' / ('TEST-' + suite['name'] + '.xml')
    assert sha(p) == suite['sha256']
    doc = ET.parse(p).getroot()
    for name in counts:
        counts[name] += int(doc.attrib[name])
assert counts == dict(tests=168, failures=0, errors=0, skipped=0)
lint = json.loads((r1 / 'lint/result.json').read_text(encoding='utf-8'))
assert sha(r1 / 'lint/lint-results-debug.xml') == lint['sha256']
assert lint['exitCode'] == 0 and lint['counts'] == {'Warning': 18}
receipt = dict(status='DESKTOP_SOURCE_AND_INHERITED_EVIDENCE_PASS',
    observedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(), baseline=baseline,
    sourceCommit=git('rev-parse', 'HEAD'), appGitTree=git('rev-parse', 'HEAD:app'),
    onlyAppChanges=changed, productionSourcesTestsSchemasAndDependenciesUnchanged=True,
    inheritedJVM=counts, inheritedJunitFiles=len(result['suites']),
    inheritedLint=dict(errors=0, warnings=18), newJvmExecution='NOT_RUN_REUSED_ACCEPTED_R1',
    protectedApks=protected, preview16AuditEvidenceUnchanged=True,
    androidExecution='NOT_RUN', deviceOperations=0)
(out / 'source-checks.json').write_text(json.dumps(receipt, indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(status=receipt['status'], protectedApks=len(protected), inheritedJVM=counts)))
