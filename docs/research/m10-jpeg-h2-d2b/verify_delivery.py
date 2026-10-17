"""Offline scope/link/secret/input audit; never invokes Android device tools."""
from pathlib import Path
import hashlib, json, re, subprocess
from urllib.parse import unquote

repo = Path(__file__).resolve().parents[3]
base = '59e61cd856b4039c693eb0f280eaf1f2ac838b6b'
def git(*args): return subprocess.check_output(['git', *args], cwd=repo).decode('utf-8').splitlines()
assert git('branch', '--show-current') == ['research/m10-jpeg-h2-d2b-host-scheduling']
assert git('merge-base', 'HEAD', base) == [base]
changed = git('diff', '--name-only', base)
untracked = git('ls-files', '--others', '--exclude-standard')
files = sorted(set(changed + untracked))
main = 'docs/research/m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/'
test = 'docs/research/m10-jpeg-r3/host/probe/src/test/java/com/cooper/lithic/m10r3/probe/'
allowed_main = {main + name + '.java' for name in ('ProbeActivity','ProbeApplication','ProbeRunner','ProbeSupport','HostCommandQueue','BoundedTraceWriter','BindingTimeline','DeadlineWatchdog')}
allowed_test = {test + name + '.java' for name in ('HostCommandQueueTest','BoundedTraceWriterTest','BindingTimelineTest','DeadlineWatchdogTest')}
allowed_docs = {'docs/CURRENT_STATE.md','docs/checkpoints/2026-10-17-M10-JPEG-H2-D2B.md'}
links = 0
manifest = {}
secret = re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{30,}|ghp_[A-Za-z0-9]{30,}|AKIA[0-9A-Z]{16}')
for name in files:
    assert name in allowed_main | allowed_test | allowed_docs or name.startswith('docs/research/m10-jpeg-h2-d2b/') or re.fullmatch(r'docs/research/M10_JPEG_H2_D2B_[A-Z0-9_]+\.md', name), name
    file = repo / name
    assert file.is_file(), ('deletion', name)
    assert file.suffix.lower() not in ('.class','.apk','.aab','.jar','.keystore','.jks','.db','.sqlite','.pem','.key','.p12'), name
    data = file.read_bytes()
    assert not secret.search(data), ('secret pattern', name)
    if file.suffix == '.md':
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', data.decode('utf-8')):
            target = unquote(target.strip('<>')).split('#')[0]
            if not target or re.match(r'\w+://',target): continue
            assert (file.parent / target).exists(), (name, target)
            links += 1
    if name != 'docs/research/m10-jpeg-h2-d2b/evidence/delivery-verification.json':
        manifest[name] = {'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
subprocess.run(['git','-c','core.whitespace=cr-at-eol','diff','--check'],cwd=repo,check=True)
audit = json.loads((repo / 'docs/research/m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/artifact-audit/result.json').read_text(encoding='utf-8'))
assert audit['tests'] == 229 and audit['oldUnchangedMethods'] == 145 and audit['newMethods'] == 84
inputs = json.loads((repo / 'docs/research/m10-jpeg-h2-d2b/evidence/build-runs/20261017T132201.765744Z/inputs.json').read_text(encoding='utf-8'))
for name, meta in inputs.items():
    assert hashlib.sha256((repo / name).read_bytes()).hexdigest() == meta['sha256'], name
result = {'verificationStatus':'PASS','taskStatus':'BLOCKED','blockingReasons':['API26/27 async identity versus synchronous Worker onBind','D2A numbered notes original not provided'],'base':base,'branch':git('branch','--show-current')[0],
          'localLinksChecked':links,'finalBuildInputsChecked':len(inputs),'uniqueJUnitMethods':229,
          'original145TestFilesByteUnchanged':True,'d1ModelMethodsSeparatelyPassed':22,'sourceContractChecksSeparatelyPassed':8,
          'productionAndHistoricalTrackedPathsChanged':False,'deviceCommandsExecuted':0,
          'apkInstalled':False,'privateBytesRead':False,'noForbiddenArtifactsOrSecretPatterns':True,
          'files':manifest}
target = repo / 'docs/research/m10-jpeg-h2-d2b/evidence/delivery-verification.json'
target.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='files'},indent=2))
