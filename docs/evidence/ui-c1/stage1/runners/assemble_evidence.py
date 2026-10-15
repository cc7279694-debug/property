from pathlib import Path
import json,hashlib,shutil,re,subprocess,xml.etree.ElementTree as ET
R=Path.cwd();O=R/'build/ui-c1-stage1';D=R/'docs/evidence/ui-c1/stage1';D.mkdir(parents=True,exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
phases=[]
for f in sorted(O.glob('*-summary.json')):
 d=json.loads(f.read_text(encoding='utf-8'))
 if 'phase' not in d or 'cases' not in d:continue
 phase=d['phase'];directory=D/'tests';directory.mkdir(exist_ok=True)
 files=[f,O/(phase+'.log'),O/d['sourceZip'],*[O/r['path'] for r in d['reports']]]
 for src in files:
  target=directory/src.name;shutil.copyfile(src,target);assert sha(src)==sha(target)
 phases.append({k:d[k] for k in ('phase','actualCommandExecutions','durationSeconds','exitCode','total','pass','fail','skipped','jvm','android','inputsStable')})
for name in ('tool-versions.json',):shutil.copyfile(O/name,D/name)
runner=D/'runners';runner.mkdir(exist_ok=True)
for name in ('run_phase.py','run_stage1_checks.py','capture_device.py'):
 shutil.copyfile(O/name,runner/name)
(D/'test-executions.json').write_text(json.dumps(phases,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(phases,ensure_ascii=True,indent=2))
