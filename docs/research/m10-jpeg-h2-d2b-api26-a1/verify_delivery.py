"""Offline A1 scope/link/secret/source/artifact audit; never contacts a device."""
from pathlib import Path
import hashlib,json,re,subprocess
from urllib.parse import unquote

repo=Path(__file__).resolve().parents[3]
base='3792917c7938c2ec78789df0a53a27215f4ee099'
prefix='docs/research/m10-jpeg-h2-d2b-api26-a1/'
run=repo/(prefix+'evidence/build-runs/20261017T142151.230105Z')
def git(*args):
    return subprocess.check_output(['git','-c','core.quotePath=false',*args],cwd=repo).decode('utf-8').splitlines()
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
assert git('branch','--show-current')==['research/m10-jpeg-h2-d2b-api26-a1']
assert git('merge-base','HEAD',base)==[base]
main='docs/research/m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/'
test='docs/research/m10-jpeg-r3/host/probe/src/test/java/com/cooper/lithic/m10r3/probe/'
allowed={main+n+'.java' for n in ('ProbeSupport','ProbeApplication','ProcessIdentityGate')}
allowed|={test+'ProcessIdentityGateTest.java','docs/CURRENT_STATE.md','docs/checkpoints/2026-10-17-M10-JPEG-H2-D2B-A1.md'}
files=sorted(set(git('diff','--name-only',base)+git('ls-files','--others','--exclude-standard')))
index_objects={}
for entry in subprocess.check_output(['git','ls-files','--stage','-z'],cwd=repo).split(b'\0'):
    if entry:
        meta,path=entry.split(b'\t',1)
        index_objects[path.decode('utf-8')]=meta.split()[1].decode('ascii')
manifest={};links=0
secret=re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{30,}|ghp_[A-Za-z0-9]{30,}|AKIA[0-9A-Z]{16}')
target=prefix+'evidence/delivery-verification.json'
for name in files:
    assert name in allowed or name.startswith(prefix) or re.fullmatch(r'docs/research/M10_JPEG_H2_D2B_A1_[A-Z0-9_]+\.md',name),name
    p=repo/name;assert p.is_file(),('deletion',name)
    assert p.suffix.lower() not in ('.class','.apk','.aab','.jar','.keystore','.jks','.db','.sqlite','.pem','.key','.p12'),name
    data=p.read_bytes();assert not secret.search(data),('secret',name)
    if p.suffix=='.json':json.loads(data.decode('utf-8'))
    if p.suffix=='.md':
        for raw in re.findall(r'\[[^\]]*\]\(([^)]+)\)',data.decode('utf-8')):
            raw=unquote(raw.strip('<>')).split('#')[0]
            if not raw or re.match(r'\w+://',raw):continue
            resolved=(p.parent/raw).resolve()
            assert resolved.is_relative_to(repo),('outside repo link',name,raw)
            assert resolved.exists() or resolved==repo/target,(name,raw)
            links+=1
    if name!=target:
        # Read by object ID so a long Windows evidence path is never treated as a
        # filesystem argument by revision/path disambiguation in `git show`.
        indexed=subprocess.check_output(['git','cat-file','blob',index_objects[name]],cwd=repo)
        assert indexed==data or indexed==data.replace(b'\r\n',b'\n'),('unexpected staged content difference',name)
        manifest[name]={'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),
            'gitIndexBytes':len(indexed),'gitIndexSHA256':hashlib.sha256(indexed).hexdigest()}
# Compare the entire staged candidate with the locked source, including new files.
subprocess.run(['git','-c','core.whitespace=cr-at-eol','diff','--check',base],cwd=repo,check=True)
audit=json.loads((run/'artifact-audit/result.json').read_text(encoding='utf-8'))
assert audit['status']=='PASS'
t=audit['tests'];assert (t['uniquePassMethods'],t['oldUnchangedMethods'],t['newMethodCount'])==(295,229,66)
assert len(t['oldUnchangedTestFiles'])==16 and len(t['xmlFiles'])==17
assert audit['inputCount']==48
inputs=json.loads((run/'inputs.json').read_text(encoding='utf-8'))
for name,meta in inputs.items():assert sha(repo/name)==meta['sha256'],name
for meta in t['oldUnchangedTestFiles'].values():assert meta['sha256']
for name,meta in t['oldUnchangedTestFiles'].items():assert sha(repo/name)==meta['sha256'],name
for meta in audit['apk'].values():assert sha(Path(meta['path']))==meta['sha256']
for stage in audit['historicalApksUnchanged'].values():
    for meta in stage['artifacts'].values():assert sha(Path(meta['path']))==meta['sha256']
supplement=json.loads((repo/(prefix+'evidence/offline-identities-final/result.json')).read_text(encoding='utf-8'))
assert [(s['suite'],s['methods'],s['exitCode']) for s in supplement['suites']]==[
    ('original-static',8,0),('a1-integration',4,0),('d1-models',22,0)]
for suite in supplement['suites']:assert len(set(suite['identities']))==suite['methods']
result={'verificationStatus':'PASS','taskStatus':'READY_FOR_REVIEW','scope':'A1 research offline only',
    'base':base,'branch':git('branch','--show-current')[0],
    'localLinksChecked':links,'finalBuildInputsChecked':len(inputs),
    'uniqueJUnitMethods':295,'original229MethodsAnd16SourceFilesUnchanged':True,'newJUnitMethods':66,
    'd1ModelMethodsSeparatelyPassed':22,'originalStaticChecksSeparatelyPassed':8,'newStaticChecksSeparatelyPassed':4,
    'originalReportsEvidenceAndNotesChanged':False,'productionCodeChanged':False,
    'workerAidlManifestPermissionsGradleDependenciesAndStageStoreChanged':False,
    'deviceCommandsExecuted':0,'apkInstalled':False,'h2RecoveredOrModified':False,'privateBytesRead':False,
    'noForbiddenArtifactsOrSecretPatterns':True,'historicalSixApksStillUnchanged':True,
    'worktreeAndGitNormalizedTextHashesRecorded':True,'rawEvidenceWhitespacePreserved':True,
    'realApi26Api27StartupMainLatencyAndAnr':'NOT_RUN','notesClosed':[],
    'files':manifest}
(repo/target).write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k!='files'},indent=2))
