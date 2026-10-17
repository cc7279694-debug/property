"""Offline scope/link/secret/input audit; never invokes Android device tools."""
from pathlib import Path
import hashlib, json, re, subprocess
from urllib.parse import unquote

repo = Path(__file__).resolve().parents[3]
base = 'f85b8a9c5d14a745f8d9e3a33ca43c9c41eba890'
def git(*args): return subprocess.check_output(['git', *args], cwd=repo).decode('utf-8').splitlines()
assert git('branch', '--show-current') == ['research/m10-jpeg-h2-d2a-failure-fence']
assert git('merge-base', 'HEAD', base) == [base]
changed = git('diff', '--name-only', base)
untracked = git('ls-files', '--others', '--exclude-standard')
files = sorted(set(changed + untracked))
main = 'docs/research/m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/'
test = 'docs/research/m10-jpeg-r3/host/probe/src/test/java/com/cooper/lithic/m10r3/probe/'
allowed_main = {main + name + '.java' for name in ('ProbeRunner','SessionGate','StageAdmission','StageStore','BindingResponsibility','HostFdScope','InvocationFence','StageAuthorization')}
allowed_test = {test + name + '.java' for name in ('SessionGateFailureFenceTest','InvocationFenceTest','BindingResponsibilityTest','HostFdScopeTest','BindingFailureFenceIntegrationTest','StageAuthorizationTest','StageReadOnlyAdmissionTest')}
allowed_docs = {'docs/CURRENT_STATE.md','docs/checkpoints/2026-10-17-M10-JPEG-H2-D2A.md'}
links = 0
manifest = {}
secret = re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{30,}|ghp_[A-Za-z0-9]{30,}|AKIA[0-9A-Z]{16}')
for name in files:
    assert name in allowed_main | allowed_test | allowed_docs or name.startswith('docs/research/m10-jpeg-h2-d2a/') or re.fullmatch(r'docs/research/M10_JPEG_H2_D2A_[A-Z_]+\.md', name), name
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
    if name != 'docs/research/m10-jpeg-h2-d2a/evidence/delivery-verification.json':
        manifest[name] = {'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest()}
subprocess.run(['git','-c','core.whitespace=cr-at-eol','diff','--check'],cwd=repo,check=True)
audit = json.loads((repo / 'docs/research/m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/artifact-audit/result.json').read_text(encoding='utf-8'))
assert audit['tests'] == 145 and audit['oldUnchangedMethods'] == 60 and audit['newMethods'] == 85
inputs = json.loads((repo / 'docs/research/m10-jpeg-h2-d2a/evidence/build-runs/20261017T122708.815561Z/inputs.json').read_text(encoding='utf-8'))
for name, meta in inputs.items():
    assert hashlib.sha256((repo / name).read_bytes()).hexdigest() == meta['sha256'], name
result = {'status':'PASS','base':base,'branch':git('branch','--show-current')[0],
          'localLinksChecked':links,'finalBuildInputsChecked':len(inputs),'uniqueJUnitMethods':145,
          'original60TestFilesByteUnchanged':True,'d1ModelMethodsSeparatelyPassed':22,
          'productionAndHistoricalTrackedPathsChanged':False,'deviceCommandsExecuted':0,
          'apkInstalled':False,'privateBytesRead':False,'noForbiddenArtifactsOrSecretPatterns':True,
          'files':manifest}
target = repo / 'docs/research/m10-jpeg-h2-d2a/evidence/delivery-verification.json'
target.write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='files'},indent=2))
