"""Archive one verified Preview17, without touching historical APKs or devices."""
from pathlib import Path
import datetime, hashlib, json, subprocess

root = Path.cwd()
e = root / 'docs/evidence/v1-rc0-20261103/desktop-1'
def load(path): return json.loads((e / path).read_text(encoding='utf-8'))
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
audit = load('artifact-1/identity.json')
build = load('build-1/command.json')
config = load('config-1/command.json')
lint = load('lint/result.json')
source = load('source-checks.json')
prep = load('preparation.json')
assert audit['status'] == 'DESKTOP_PASS' and build['exitCode'] == config['exitCode'] == 0
assert lint['status'] == 'PASS' and not any(lint['counts'].get(x, 0) for x in ('Error','Fatal'))
assert source['status'] == 'VERSION_ONLY_SOURCE_CHECK_PASS'
assert subprocess.check_output(['git','rev-parse','HEAD:app'],text=True).strip() == source['appGitTree']
assert not subprocess.check_output(['git','status','--porcelain','--untracked-files=all','--','app'],text=True).strip()
app = audit['app']
assert app['applicationId'] == 'com.cooper.lithic.preview.debug' and app['versionCode'] == '33'
assert app['versionName'] == '0.1.0-preview.17-debug'
assert app['certificateSha256'] == '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9'
src = root / app['path']; assert src.stat().st_size == app['bytes'] and sha(src) == app['sha256']
for item in prep['protectedApks']:
    p = root / item['path']; assert p.stat().st_size == item['bytes'] and sha(p) == item['sha256'], str(p)
assert len(prep['protectedApks']) == 32
directory = root / 'deliveries/lithic-preview-0.1.0-preview.17'
directory.mkdir()  # Never replace a previously existing candidate or unknown contents.
dst = directory / 'Lithic-0.1.0-preview.17-debug.apk'
with dst.open('xb') as output: output.write(src.read_bytes())
assert dst.read_bytes() == src.read_bytes() and sha(dst) == app['sha256']
receipt = dict(status='PREVIEW17_DESKTOP_DELIVERY_READY',absolutePath=str(dst.resolve()),
    relativePath=dst.relative_to(root).as_posix(),bytes=dst.stat().st_size,sha256=sha(dst),
    applicationId=app['applicationId'],versionCode=33,versionName=app['versionName'],
    certificateSha256=app['certificateSha256'],sourceCommit=source['sourceCommit'],appGitTree=source['appGitTree'],
    baseline=source['baseline'],copiedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    copyByteIdentical=True,protectedApks=prep['protectedApks'],
    evidence={p:sha(e/p) for p in ('artifact-1/identity.json','config-1/command.json','build-1/command.json','lint/result.json','source-checks.json')},
    userManual='USER_REPORTED_MANUAL_PASS_WITH_NOTES',androidExecution='NOT_RUN',deviceOperations=0)
with (e/'archive.json').open('x',encoding='utf-8') as out: out.write(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({k:receipt[k] for k in ('status','absolutePath','bytes','sha256')}))
