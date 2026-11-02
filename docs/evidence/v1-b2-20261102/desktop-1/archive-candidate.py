"""Archive one reviewed QA App, exclusively; no clean/overwrite/device operations."""
from pathlib import Path
import datetime, hashlib, json, subprocess

root = Path.cwd()
evidence = root / 'docs/evidence/v1-b2-20261102/desktop-1'
def read(name):
    return json.loads((evidence / name).read_text(encoding='utf-8'))
def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()
audit = read('artifact-1/identity.json')
configuration = read('configuration.json')
build = read('build-3/command.json')
lint = read('lint/result.json')
assert audit['status'] == 'DESKTOP_PASS' and configuration['status'] == 'DESKTOP_CONFIGURATION_PASS'
assert build['exitCode'] == lint['exitCode'] == 0
assert not any(lint['counts'].get(x, 0) for x in ('Error', 'Fatal'))
assert subprocess.check_output(['git', 'rev-parse', 'HEAD:app'], text=True).strip() == build['sourceAppGitTree']
assert not subprocess.check_output(['git', 'status', '--porcelain', '--untracked-files=all', '--', 'app'], text=True).strip()
app = audit['app']
assert app['applicationId'] == 'com.cooper.lithic.b1qa.debug'
assert app['versionCode'] == '1' and app['versionName'] == '0.1.0-b1qa.1-debug'
assert app['certificateSha256'] == '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9'
source = root / app['path']
assert source.stat().st_size == app['bytes'] and sha(source) == app['sha256']
protected = read('preparation.json')['protectedApks']
for item in protected:
    p = root / item['path']
    assert p.stat().st_size == item['bytes'] and sha(p) == item['sha256'], str(p)

directory = root / 'verification-candidates/v1-b2-b1qa'
directory.mkdir()  # Refuse any preexisting unknown candidate directory.
destination = directory / 'Lithic-B1-QA-0.1.0-b1qa.1-debug.apk'
with destination.open('xb') as output:
    output.write(source.read_bytes())
assert destination.read_bytes() == source.read_bytes()
assert sha(destination) == app['sha256']
with (directory / 'README.txt').open('x', encoding='utf-8') as output:
    output.write('LITHIC B1-QA: independent acceptance candidate, not a Preview update.\n'
                 'Desktop static checks only. Installation/SAF/overwrite restore need new authorization.\n'
                 'Use only disposable synthetic records. No Test APK or plain Application host.\n')
record = dict(status='DESKTOP_PASS', purpose='INDEPENDENT_NORMAL_APP_ACCEPTANCE_ONLY',
    absolutePath=str(destination.resolve()), relativePath=destination.relative_to(root).as_posix(),
    bytes=destination.stat().st_size, sha256=sha(destination), applicationId=app['applicationId'],
    versionCode=1, versionName=app['versionName'], certificateSha256=app['certificateSha256'],
    copiedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(), copyByteIdentical=True,
    sourceCommit=build['sourceCommit'], appGitTree=build['sourceAppGitTree'],
    acceptedBaseline='cf0512bbac1c4155efac7b8186ff573e49f97746',
    evidence={name:sha(evidence / name) for name in ('artifact-1/identity.json', 'configuration.json',
        'build-3/command.json', 'lint/result.json', 'source-checks.json')},
    protectedApks=protected, androidExecution='NOT_RUN', deviceOperations=0,
    runtimeUidAndPrivateDataIsolation='UNKNOWN_NO_DEVICE_OBSERVATION', installationAuthorized=False)
with (evidence / 'archive.json').open('x', encoding='utf-8') as output:
    output.write(json.dumps(record, indent=2)+'\n')
print(json.dumps({k:record[k] for k in ('status','absolutePath','bytes','sha256')}))
