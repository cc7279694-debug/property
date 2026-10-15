from pathlib import Path
import json,hashlib,shutil,zipfile,xml.etree.ElementTree as ET
R=Path.cwd();O=R/'build/ui-c1-refinement';D=R/'docs/evidence/ui-c1/refinement';D.mkdir(parents=True,exist_ok=True)
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def copy(s,t):t.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(s,t);assert sha(s)==sha(t)
phases=[]
for f in sorted(O.glob('*-summary.json')):
 v=read(f)
 if 'sourceInputs' not in v:continue
 z=O/v['sourceZip'];assert sha(z)==v['sourceZipSha256']
 with zipfile.ZipFile(z) as archive:
  assert set(archive.namelist())=={x['path'] for x in v['sourceInputs']}
  for x in v['sourceInputs']:assert hashlib.sha256(archive.read(x['path'])).hexdigest()==x['sha256']
 for x in v['reports']:
  assert sha(O/x['path'])==x['sha256'];assert len(list(ET.parse(O/x['path']).getroot().iter('testcase')))==x['counts']['tests']
 for s in [f,O/(v['phase']+'.log'),z,*[O/x['path'] for x in v['reports']]]:copy(s,D/'tests'/s.name)
 phases.append({k:v[k] for k in ('phase','actualCommandExecutions','durationSeconds','exitCode','total','pass','fail','skipped','jvm','android','inputsStable')})
(D/'test-executions.json').write_text(json.dumps(phases,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for f in ('static-current.json','tool-versions.json','real-os-200-test-settings.json','boot-check.json'):
 if (O/f).exists():copy(O/f,D/f)
for f in (O/'apk').iterdir():
 if f.is_file():copy(f,D/'apk'/f.name)
copy(R/'app/build/reports/lint-results-debug.xml',D/'lint-results-debug.xml')
for f in ('run_checks.py','capture_device.py','manual_step.py','device_snapshot.py','final_device_proof.py','verify_apk.py','publish_evidence.py'):
 copy(O/f,D/'runners'/f)
for f in (O/'diagnostics').iterdir():
 if f.is_file() and f.suffix in ('.png','.txt','.xml','.json'):copy(f,D/'diagnostics'/f.name)
# Invalid images are retained only under diagnostics, never in the delivery gallery.
for f in (O/'first-green-synthetic').iterdir():
 if f.is_file():copy(f,D/'diagnostics/disqualified-anr-host'/f.name)
print(json.dumps({'archivedPhases':len(phases),'result':'PASS'},indent=2))
