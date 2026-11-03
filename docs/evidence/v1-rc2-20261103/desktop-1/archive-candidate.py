"""Archive one previously verified RC2 Preview18, without building or device access."""
from pathlib import Path
import argparse, datetime, hashlib, json, subprocess, xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[4]
e = root / 'docs/evidence/v1-rc2-20261103/desktop-1'
parser = argparse.ArgumentParser()
parser.add_argument('--audit', type=Path, required=True)
parser.add_argument('--build-receipt', type=Path, required=True)
parser.add_argument('--lint-xml', type=Path, required=True)
args = parser.parse_args()
def local(path):
    path = path.resolve()
    assert path.is_relative_to(root), f'Evidence must stay in project tree: {path}'
    return path

def load(path): return json.loads(local(path).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def git(*args): return subprocess.check_output(['git', *args], cwd=root, text=True).strip()
audit_path, build_path, lint_path = map(local, (args.audit, args.build_receipt, args.lint_xml))
assert audit_path.is_relative_to(e.resolve())
audit, build, prep = load(audit_path), load(build_path), load(e / 'preparation.json')
assert audit['status'] == 'DESKTOP_PASS' and audit['scope'] == 'RC2_PRIVATE_TRIAL_STATIC_AUDIT_ONLY'
assert audit['deviceOperations'] == 0 and audit['androidExecution'] == 'NOT_RUN'
assert audit['archivedByThisScript'] is False and audit['newTestApk'] is False
assert build['exitCode'] == 0 and build['deviceOperations'] == 0 and build['androidExecution'] == 'NOT_RUN'
assert '-PlithicPrivateTrial=true' in build['argv'] and ':app:assembleDebug' in build['argv'] and ':app:lintDebug' in build['argv']
assert audit['buildReceipt']['sha256'] == sha(build_path)
assert audit['buildSourceCommit'] == build['sourceCommit']
assert audit['buildAppGitTree'] == build['sourceAppGitTree'] == git('rev-parse', 'HEAD:app')
assert git('branch', '--show-current') == 'codex/v1-rc2-reminder-integration'
assert not git('status', '--porcelain=v1', '--untracked-files=all', '--', 'app')
assert audit['acceptedBaseline'] == prep['baseline'] == '8c79671960119bd836e95a45e30b4b1bde226eaf'
assert audit['onlyTwoPrivateTrialVersionValuesChanged'] and audit['allMainTestSchemaSourcesUnchanged']
assert audit['testFrameworkAndInstrumentationAbsent'] and audit['normalApplicationAndLauncher']
assert audit['permissionsAndNativeUnchangedAfterApplicationIdNormalization']
assert audit['r1Equivalence']['status'] == 'UNCHANGED_FROM_ACCEPTED_PREVIEW16' and audit['rc1ReminderDexEvidence']['androidRuntime'] == 'NOT_RUN'
app = audit['app']
assert app['applicationId'] == 'com.cooper.lithic.preview.debug'
assert app['versionCode'] == '34' and app['versionName'] == '0.1.0-preview.18-debug'
assert app['certificateSha256'] == '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9'
assert app['path'] == 'app/build/rc2-preview18/outputs/apk/debug/app-debug.apk'
src = root / app['path']; assert src.is_file() and src.stat().st_size == app['bytes'] and sha(src) == app['sha256']
issues = ET.parse(lint_path).getroot().findall('issue')
counts = {level: sum(i.get('severity') == level for i in issues) for level in ('Fatal','Error','Warning')}
assert counts['Fatal'] == 0 and counts['Error'] == 0, counts
rows = prep['protectedApks']; assert len(rows) == 35 and len({x['path'] for x in rows}) == 35
for row in rows:
    p = root / row['path']; assert p.is_file() and p.stat().st_size == row['bytes'] and sha(p) == row['sha256'],str(p)
assert len(audit['protectedLocalApks']) == len(rows)
assert all({k: x[k] for k in ('path', 'bytes', 'sha256')} == {k: y[k] for k in ('path', 'bytes', 'sha256')} for x,y in zip(audit['protectedLocalApks'],rows))
directory = root / 'deliveries/lithic-preview-0.1.0-preview.18'
directory.mkdir()  # Existing path, including empty or unknown contents, is a hard stop.
dst = directory / 'Lithic-0.1.0-preview.18-debug.apk'
with dst.open('xb') as output: output.write(src.read_bytes())
assert dst.read_bytes() == src.read_bytes() and sha(dst) == app['sha256']
for row in rows:
    p = root / row['path']; assert p.is_file() and p.stat().st_size == row['bytes'] and sha(p) == row['sha256'],str(p)
receipt = dict(status='PREVIEW18_DESKTOP_DELIVERY_READY', absolutePath=str(dst.resolve()),
    relativePath=dst.relative_to(root).as_posix(), bytes=dst.stat().st_size, sha256=sha(dst),
    applicationId=app['applicationId'], versionCode=34, versionName=app['versionName'],
    certificateSha256=app['certificateSha256'], sourceCommit=build['sourceCommit'], appGitTree=build['sourceAppGitTree'],
    baseline=prep['baseline'], copiedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    copyByteIdentical=True, protectedApks=rows, lintCounts=counts,
    evidence={str(p.relative_to(root)):sha(p) for p in (audit_path,build_path,lint_path)},
    androidExecution='NOT_RUN',deviceOperations=0)
with (e/'archive.json').open('x', encoding='utf-8') as out: out.write(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({k:receipt[k] for k in ('status','absolutePath','bytes','sha256')}))
