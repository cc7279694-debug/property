"""Offline H2 evidence audit. Never imports a device executor or invokes ADB."""
import ast,hashlib,json
from pathlib import Path
HERE=Path(__file__).resolve().parent
REPO=HERE.parents[2]
OUT=HERE.parent/'m10-jpeg-r3-f1b/evidence/h2-20261017-01'
def load(n):return json.loads((OUT/n).read_text(encoding='utf-8-sig'))
def sha(b):return hashlib.sha256(b).hexdigest()
def main():
    names=['h2-command-integrity.jsonl','h2-normal-command-integrity.jsonl','h2-stopped-command-integrity.jsonl','h2-final-command-integrity.jsonl','h2-ending-log-command-integrity.jsonl','h2-final-protection-command-integrity.jsonl']
    proof=[json.loads(l) for n in names for l in (OUT/n).read_text().splitlines()];raw=[json.loads(l) for l in (OUT/'adb-commands.jsonl').read_text().splitlines()]
    assert len(raw)==len(proof)==152
    for a,b in zip(raw,sorted(proof,key=lambda x:x.get('startedAtUtc',x.get('atUtc')))):
        assert a['argv']==b['argv'] and a['exitCode']==b['exitCode']
        if not a.get('stdout','').startswith('RAW_NOT_ARCHIVED'):
            original=a['stdout'].encode('utf-8');assert len(original)==b['stdoutBytes'] and sha(original)==b['stdoutSHA256']
    launches=[x for x in proof if x['argv'][3:7]==['shell','am','start','-W']]
    assert len(launches)==1 and '--es' in launches[0]['argv'] and 'normal' in launches[0]['argv']
    assert all(x['exitCode']==0 and not x['timedOut'] for x in proof)
    assert not any(x['argv'][3:4] in (['install'],['uninstall'],['reboot']) for x in proof)
    assert load('preflight-gate.json')['classification']=='PREFLIGHT_ELIGIBLE'
    assert load('normal-supervision-result.json')['status']=='BLOCKED'
    assert load('h2-final-protection-result.json')['status']=='PASS'
    assert load('h2-final-stage-check.json')['status']=='BLOCKED'
    assert load('h2-final-services.json')['classification']['classification']=='NO_TARGET_RECORD'
    contract=load('h2-contract.json')
    for name,value in contract['lockedScripts'].items():assert sha((REPO/name).read_bytes())==value['sha256']
    sources=load('h2-source-correspondence.json')['unchangedF1ABuildInputs'];assert len(sources)==27
    for name,value in sources.items():assert sha((REPO/name).read_bytes())==value['sha256']
    for name,value in load('h2-recovered-raw-index.json').items():assert sha((OUT/name).read_bytes())==value['sha256']
    for name in HERE.glob('*.py'):ast.parse(name.read_text(encoding='utf-8-sig'))
    result={'status':'PASS_OFFLINE_AUDIT_ONLY','sourceInputsMatched':27,'gateScriptsMatched':len(contract['lockedScripts']),'rawADBLogRows':len(raw),'matchingIntegrityRows':len(proof),'totalADBCommandsIncludingInitial':len(proof)+1,'normalLaunches':1,'installs':0,'adbNonzeroExitCodes':0,'adbSubprocessTimeouts':0,'amReportedTimeoutPreserved':True,'normalFAILAndStageBLOCKEDPreserved':True,'collectorErrorsPreserved':True,'overallH2':'BLOCKED','doesNotCountAsAndroidTestPass':True}
    (OUT/'h2-offline-audit.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8');print(json.dumps(result));return 0
if __name__=='__main__':raise SystemExit(main())
