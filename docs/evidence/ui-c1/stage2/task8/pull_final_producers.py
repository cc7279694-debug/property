from pathlib import Path
import subprocess,json,hashlib
R=Path.cwd();O=R/'build/ui-c1-stage2'
s=json.loads((O/'stage2-final-restored-full-summary.json').read_text(encoding='utf8'))
assert len(s['actualCommandExecutions'])==3
A='C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe';P='com.cooper.lithic.debug'
def adb(*a):return subprocess.check_output([A,'-s','emulator-5596',*a],timeout=30)
for part,folder,count,cls in [('subscriptions','ui-c1-stage2-visual',14,'UIC1Stage2EvidenceTest'),('empty','ui-c1-stage2-empty-visual',2,'UIC1Stage2EmptyEvidenceTest')]:
 cases=[c for c in s['cases'] if c['class'].endswith('.'+cls)]
 assert len(cases)==1 and cases[0]['result']=='PASS',cases
 dest=O/'final-producers'/part;dest.mkdir(parents=True,exist_ok=False)
 names=adb('shell','run-as',P,'ls','no_backup/'+folder).decode().splitlines()
 assert len([n for n in names if n.endswith('.png')])==count and 'fixture-source.json' in names
 for n in names:
  assert '/' not in n and '\\' not in n and '..' not in n
  if n.endswith('.png') or n=='fixture-source.json':
   (dest/n).write_bytes(adb('exec-out','run-as',P,'cat','no_backup/'+folder+'/'+n))
 print(part,len(names),flush=True)
