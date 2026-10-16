"""Check research record consistency, not production acceptance or a new run."""
from __future__ import annotations
import ast, hashlib, json, re, subprocess
from pathlib import Path
from urllib.parse import unquote

ROOT=Path(__file__).resolve().parents[4]
RESEARCH=ROOT/'docs/research/m10-jpeg-r1'
E=RESEARCH/'evidence'

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(name):return json.loads((E/name).read_text(encoding='utf-8-sig'))
def lines(name):return [json.loads(x) for x in (E/name).read_text(encoding='utf-8').splitlines()]

def main():
    result={'scope':'RESEARCH_RECORD_ONLY_NOT_PRODUCT_TESTS','researchGate':'BLOCKED',
            'originalProductFailuresUnchanged':True,'checks':[]}
    def check(name,condition):
        result['checks'].append({'name':name,'pass':bool(condition)})
        if not condition:raise ValueError(name)
    for name,expected in {
      'complete.jpg':'2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907',
      'entropy_cut_eoi_retained.jpg':'f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a'}.items():
        check('locked-'+name,sha(ROOT/'app/src/androidTest/assets/m10_jpeg_integrity'/name)==expected)
    check('harness-stable',sha(RESEARCH/'native/strictjpeg_probe.c')=='954938adc6c2fcea91a5469d28ed864b3230abe520ac99561e88f69eee10ba58')
    minimal=read('minimal.json')['cases']
    expanded=lines('expanded.jsonl')
    single=read('single-444.json')
    supplement=read('supplement.json')['cases']
    edges=read('runtime-edges.json')['cases']
    all_cases=minimal+expanded+single['cases']+supplement+[c for c in edges if c['label'].startswith('SHARED_INVOKE')]+[read('late-cancel-recheck.json')]
    counts=read('result-counts.json')
    check('record-counts',len(all_cases)==counts['registeredDecoderCalls']==122 and len(expanded)==108)
    check('one-timeout-retained',sum(c.get('supervisorStatus')=='TIMEOUT' for c in all_cases)==1)
    check('reported-completed-iterations',sum(c.get('native',{}).get('iterationsCompleted',0) for c in all_cases)==519)
    codes={'ACCEPT':0,'REJECT':10,'RESOURCE_REJECT':20,'CANCELLED':30,'INFRA_ERROR':40}
    structured=[c for c in all_cases if 'native'in c]
    check('structured-exits',all(c['exitCode']==codes[c['native']['status']] for c in structured))
    check('caller-and-fd-balance',all(c['native']['trackedCallerBytesAfter']==0 and c['native']['openFdsBefore']==c['native']['openFdsAfter'] for c in structured))
    check('minimal-lock-gate',all(c['supervisorStatus']==('STRUCTURED_ACCEPT' if c['input']=='complete.jpg' else 'STRUCTURED_REJECT') for c in minimal))
    healthy=[c for c in expanded if c['category']=='AUTHORED_HEALTHY' and c['input']!='large_40mp_progressive_444.jpg']+single['cases']
    check('22-complete-authored-fixtures-44-calls',len(healthy)==44 and all(c['native']['status']=='ACCEPT' and c['native']['finishComplete'] and c['native']['warningCount']==0 for c in healthy))
    check('catalogue-incomplete-not-misclassified',single['firstBytesArePrefixOfCompletedFile'] and single['firstMissingEoi'])
    check('four-abi-audit',read('build-audit.json')['status']=='PASS')
    scripts=list((RESEARCH/'scripts').glob('*.py'))
    for p in scripts:ast.parse(p.read_text(encoding='utf-8'))
    check('python-ast',bool(scripts))
    json_count=0
    for p in E.rglob('*.json'):
        json.loads(p.read_text(encoding='utf-8-sig'));json_count+=1
    result['jsonFilesParsed']=json_count
    docs=list(RESEARCH.glob('*.md'))+list((ROOT/'docs/research').glob('M10_JPEG_R1_*.md'))+[ROOT/'docs/CURRENT_STATE.md',ROOT/'docs/checkpoints/2026-10-16-M10-JPEG-R1.md']
    links=[]
    for p in docs:
        text=p.read_text(encoding='utf-8-sig')
        for raw in re.findall(r'\[[^\]]*\]\(([^)]+)\)',text):
            target=raw.strip().strip('<>')
            if re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*:',target) or target.startswith('#'):continue
            target=unquote(target.split('#',1)[0])
            links.append((p,target))
    check('local-markdown-links',all((p.parent/target).exists() for p,target in links))
    result['localLinksChecked']=len(links)
    scope=['app','gradle','gradlew','gradlew.bat','build.gradle.kts','settings.gradle.kts','gradle.properties','AGENTS.md','PROJECT.md','docs/ACCEPTANCE_MATRIX.md','docs/planning/original-v1','docs/research/m10-jpeg-r0','docs/research/M10_JPEG_R0_FEASIBILITY.md','docs/research/M10_JPEG_R0_TEST_MATRIX.md','docs/research/M10_JPEG_R0_PRODUCTION_PROPOSAL.md']
    cmd=['git','diff','--exit-code','c4cd90b148cce99ba497d864be1176c90408a4da','--',*scope]
    p=subprocess.run(cmd,cwd=ROOT,capture_output=True)
    result['protectedDiffCommand']=cmd
    result['protectedDiffExit']=p.returncode
    check('protected-history-and-production-no-diff',p.returncode==0)
    candidates=[p for p in RESEARCH.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.pyc']+docs+[ROOT/'docs/research/.gitattributes']
    patterns=[r'gh[pousr]_[A-Za-z0-9]{20,}',r'github_pat_[A-Za-z0-9_]{30,}',r'sk-[A-Za-z0-9]{24,}',r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',r'AIza[0-9A-Za-z_-]{30,}']
    findings=[]
    for p in set(candidates):
        if p.name in ('verification.json','file-manifest.json'):continue
        raw=p.read_bytes()
        check('no-binary-'+str(p.relative_to(ROOT)),b'\x00' not in raw)
        text=raw.decode('utf-8-sig',errors='replace')
        if any(re.search(pattern,text) for pattern in patterns):findings.append(str(p.relative_to(ROOT)))
    result['secretPatternFindings']=findings
    check('sensitive-pattern-scan',not findings)
    result['verificationStatus']='PASS_FOR_RESEARCH_RECORD_ONLY'
    (E/'verification.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    inventory=[]
    for p in sorted(set(candidates)):
        if p.name in ('verification.json','file-manifest.json'):continue
        entry={'path':p.relative_to(ROOT).as_posix(),'bytes':p.stat().st_size,'sha256':sha(p)}
        if entry['path'] in ('docs/CURRENT_STATE.md','docs/checkpoints/2026-10-16-M10-JPEG-R1.md'):
            # Existing repository text attributes normalize these memory docs;
            # research sources/evidence themselves are -text byte-preserved.
            staged=subprocess.run(['git','show',':'+entry['path']],cwd=ROOT,capture_output=True)
            if staged.returncode==0:
                entry['gitIndexSha256']=hashlib.sha256(staged.stdout).hexdigest()
                entry['lineEndingNormalizationOnly']=staged.stdout==p.read_bytes().replace(b'\r\n',b'\n')
        inventory.append(entry)
    (E/'file-manifest.json').write_text(json.dumps({'selfAndVerificationExcluded':True,'sha256Origin':'WORKTREE_BYTES','memoryDocsMayHaveSeparateIndexLfHash':True,'files':inventory},indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'recordVerification':result['verificationStatus'],'researchGate':result['researchGate'],'files':len(inventory),'links':len(links),'jsonFiles':json_count}))
    return 0

if __name__=='__main__':raise SystemExit(main())
