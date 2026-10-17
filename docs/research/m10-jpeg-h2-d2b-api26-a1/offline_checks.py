"""Run only named offline Python suites into a fresh evidence directory."""
from pathlib import Path
import hashlib, json, os, re, subprocess, sys

repo=Path(__file__).resolve().parents[3]
label=sys.argv[1]
if label not in ('integration-red','offline-green','offline-final','offline-identities-final'):
    raise SystemExit('fixed label required')
out=repo/'docs/research/m10-jpeg-h2-d2b-api26-a1/evidence'/label
out.mkdir(parents=True,exist_ok=False)
suites=[('original-static',repo/'docs/research/m10-jpeg-h2-d2b','test_source_contract.py'),
        ('a1-integration',Path(__file__).parent,'test_a1_integration.py'),
        ('d1-models',repo/'docs/research/m10-jpeg-h2-d1','test_fence_model.py')]
results=[]
for name,root,pattern in suites:
    argv=[sys.executable,'-B','-m','unittest','discover','-s',str(root),'-p',pattern,'-v']
    result=subprocess.run(argv,cwd=repo,env=dict(os.environ,PYTHONDONTWRITEBYTECODE='1'),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=60)
    (out/(name+'.log')).write_bytes(result.stdout)
    text=result.stdout.decode('utf-8',errors='replace')
    identities=re.findall(r'^(test_\w+) \(([^)]+)\)',text,re.M)
    results.append({'suite':name,'argv':argv,'exitCode':result.returncode,'methods':len(identities),
        'identities':[kind if kind.endswith('.'+method) else kind+'.'+method for method,kind in identities],
        'logBytes':len(result.stdout),'logSHA256':hashlib.sha256(result.stdout).hexdigest()})
    print(name,result.returncode,text[-700:],flush=True)
(out/'result.json').write_text(json.dumps({'hostOnly':True,'deviceCommands':0,'suites':results},indent=2),encoding='utf-8')
raise SystemExit(0 if all(r['exitCode']==0 for r in results) else 1)
