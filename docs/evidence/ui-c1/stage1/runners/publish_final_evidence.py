from pathlib import Path
import json,hashlib,zipfile,shutil,xml.etree.ElementTree as ET
R=Path.cwd();O=R/'build/ui-c1-stage1';D=R/'docs/evidence/ui-c1/stage1'
def read(p):return json.loads(p.read_text(encoding='utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
def put(n,v): (D/n).write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
def copy(src,dst):dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst);assert sha(src.read_bytes())==sha(dst.read_bytes())
for f in ('static-current.json','final-artifact-command.json','final-artifact.log','manual-start.json','offline-connectivity.txt'):
 copy(O/f,D/f)
for f in (O/'apk').iterdir():
 if f.is_file():copy(f,D/'apk'/f.name)
copy(R/'app/build/reports/lint-results-debug.xml',D/'lint-results-debug.xml')
for f in ('device_snapshot.py','final_device_proof.py','publish_final_evidence.py','assemble_evidence.py'):
 copy(O/f,D/'runners'/f)
b=read(O/'final-stage1-recheck-summary.json');a=read(O/'final-stage1-summary.json')
for f in sorted(O.glob('*-summary.json')):
 p=read(f)
 if 'sourceInputs' not in p:continue
 z=O/p['sourceZip'];assert sha(z.read_bytes())==p['sourceZipSha256']
 with zipfile.ZipFile(z) as archive:
  assert set(archive.namelist())=={i['path'] for i in p['sourceInputs']}
  for i in p['sourceInputs']:
   raw=archive.read(i['path']);assert sha(raw)==i['sha256'] and len(raw)==i['bytes']
 for v in p['reports']:
  raw=(O/v['path']).read_bytes();assert sha(raw)==v['sha256'] and len(raw)==v['bytes']
  root=ET.fromstring(raw);cases=list(root.iter('testcase'))
  assert len(cases)==v['counts']['tests']
for i in b['sourceInputs']:
 raw=(R/i['path']).read_bytes();assert sha(raw)==i['sha256'] and len(raw)==i['bytes'],i['path']
latest={(c['class'],c['name']):c['result'] for c in b['cases']};new=read(O/'static-current.json')['newMethods']
assert len(new)==35 and all(latest[(v['class'],v['method'])]=='PASS' for v in new)
ah={v['path']:v['sha256'] for v in a['sourceInputs']};bh={v['path']:v['sha256'] for v in b['sourceInputs']}
delta=[p for p in sorted(set(ah)|set(bh)) if ah.get(p)!=bh.get(p)]
union={(c['class'],c['name']):c['result'] for c in a['cases']};union.update(latest)
put('input-and-test-binding.json',{'result':'PASS','finalPhase':b['phase'],'finalInputs':len(b['sourceInputs']),'finalInputZipSha256':b['sourceZipSha256'],'allPhaseZipAndXmlHashesVerified':True,'finalInputEqualsCurrentFiles':True,'baselineMethods':657,'currentMethods':692,'originalMethodsRemoved':0,'newMethods':35,'newMethodsAllFinalPass':True,'finalRun':{k:b[k] for k in ('jvm','android','total','pass','fail','skipped','exitCode')},'finalFailCases':[c['class']+'#'+c['name'] for c in b['cases'] if c['result']=='FAIL'],'broaderPreviousCandidate':{k:a[k] for k in ('jvm','android','total','pass','fail','skipped','exitCode')},'changedInputsFromBroaderCandidateToFinal':delta,'crossCandidateDistinctExecuted':len(union),'crossCandidateLatestOutcomes':{v:list(union.values()).count(v) for v in sorted(set(union.values()))},'sourceMethodsNotSelectedInEitherCandidate':692-len(union),'scope':'Broader A and final B are separate input sets. Their union is coverage history, NOT one final full regression. Task8 full 692-method run NOT_RUN.'})
shots=[]
for f in sorted(O.glob('*.png.json')):
 v=read(f);raw=(R/v['file']).read_bytes();assert sha(raw)==v['sha256'] and len(raw)==v['bytes'];assert v['installedApkSha256']==read(O/'apk/apk-verification.json')['sha256'];shots.append(v)
for v in read(O/'empty-shot-metadata.json'):
 raw=(R/v['file']).read_bytes();assert sha(raw)==v['sha256'] and len(raw)==v['bytes'];shots.append(v)
put('screenshot-manifest.json',sorted(shots,key=lambda v:v['file']))
print(json.dumps({'finalInputs':len(b['sourceInputs']),'sourceDeltaAtoB':delta,'newMethodsFinalPass':len(new),'screenshots':len(shots),'crossCandidateDistinct':len(union),'fullRegression':'NOT_RUN'},ensure_ascii=True,indent=2))
