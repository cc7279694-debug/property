from pathlib import Path
import subprocess,os,sys,json,time,hashlib,zipfile,xml.etree.ElementTree as ET
from datetime import datetime,timezone
sys.stdout.reconfigure(encoding='utf8',errors='replace')
r=Path.cwd();o=r/'build/ui-c1-stage2';phase=sys.argv[1]
assert not (o/(phase+'-summary.json')).exists()
j='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1';env=os.environ.copy();env.update(JAVA_HOME=j,ANDROID_SERIAL='emulator-5596')
def sha(b):return hashlib.sha256(b).hexdigest()
def snapshot():
 paths=[p for prefix in ('app/src/main','app/src/debug','app/src/test','app/src/androidTest','app/schemas') for p in sorted((r/prefix).rglob('*')) if p.is_file()]
 paths.extend(r/p for p in ('gradlew','gradlew.bat','app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle/libs.versions.toml','gradle.properties','gradle/wrapper/gradle-wrapper.properties','gradle/wrapper/gradle-wrapper.jar'))
 return [{'path':p.relative_to(r).as_posix(),'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size} for p in paths]
frozenHead=subprocess.check_output(['git','rev-parse','HEAD']).decode().strip()
assert subprocess.check_output(['git','branch','--show-current']).decode().strip()=='feat/ui-c1-visual-redesign'
inputs=snapshot();fingerprint=sha(json.dumps(inputs,sort_keys=True,separators=(',',':')).encode());archive=phase+'-source.zip'
with zipfile.ZipFile(o/archive,'x',zipfile.ZIP_DEFLATED) as z:
 for p in inputs:z.write(r/p['path'],p['path'])
commands=[[':app:testDebugUnitTest'],[':app:connectedDebugAndroidTest'],[':app:lintDebug',':app:assembleDebug']]
prep=[str(r/'gradlew.bat'),':app:cleanTestDebugUnitTest','--max-workers=2','--console=plain'];began=time.time()
with (o/(phase+'-jvm-clean.log')).open('xb') as f:prepCode=subprocess.run(prep,cwd=r,env=env,stdout=f,stderr=subprocess.STDOUT).returncode
assert prepCode==0 and snapshot()==inputs
preflight={'command':prep,'exitCode':prepCode,'durationSeconds':round(time.time()-began,3),'log':phase+'-jvm-clean.log','purpose':'Clean only rebuildable JVM test outputs to ensure actual fresh test execution; no application/device data cleanup'}
executions=[];cases=[];reports=[]
for i,tasks in enumerate(commands):
 assert snapshot()==inputs,'Input change before command'
 cmd=[str(r/'gradlew.bat'),*tasks,'--max-workers=2','--console=plain'];log=phase+'-'+str(i)+'.log';start=time.time();print('START '+log,flush=True)
 with (o/log).open('xb') as f:code=subprocess.run(cmd,cwd=r,env=env,stdout=f,stderr=subprocess.STDOUT).returncode
 changed=snapshot()!=inputs
 executions.append({'command':cmd,'exitCode':code,'durationSeconds':round(time.time()-start,3),'log':log,'logSha256':sha((o/log).read_bytes()),'startedUtc':datetime.fromtimestamp(start,timezone.utc).isoformat(),'inputsStable':not changed})
 assert not changed,'Input change during command: final full run invalid'
 if i<2:
  kind='jvm' if i==0 else 'android';folder=r/('app/build/test-results/testDebugUnitTest' if i==0 else 'app/build/outputs/androidTest-results/connected/debug')
  for n,p in enumerate(sorted(folder.rglob('*.xml'))):
   if p.stat().st_mtime<start-1:continue
   raw=p.read_bytes();tree=ET.fromstring(raw);name=f'{phase}-{kind}-{n}.xml';(o/name).write_bytes(raw)
   reports.append({'kind':kind,'path':name,'sha256':sha(raw),'bytes':len(raw),'counts':{k:int(tree.get(k,'0')) for k in ('tests','failures','errors','skipped')}})
   for c in tree.iter('testcase'):
    bad=c.find('failure');bad=c.find('error') if bad is None else bad
    cases.append({'kind':kind,'class':c.get('classname'),'name':c.get('name'),'result':'FAIL' if bad is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASS','failureType':bad.get('type') if bad is not None else None,'message':(bad.get('message') or bad.text or '')[:4000] if bad is not None else None})
 print('END '+log+' exit='+str(code)+' methods='+str(len(cases)),flush=True)
 baseline=json.loads((o/'baseline-methods.json').read_text(encoding='utf8'));actual={(c['class'],c['name']) for c in cases}
 result={'phase':phase,'frozenSourceHead':frozenHead,'sourceInputsSha256':fingerprint,'preflight':preflight,'actualCommandExecutions':executions,'sourceInputs':inputs,'sourceZip':archive,'sourceZipSha256':sha((o/archive).read_bytes()),'inputsStable':True,'reports':reports,'cases':cases,'total':len(cases),'jvm':sum(c['kind']=='jvm' for c in cases),'android':sum(c['kind']=='android' for c in cases),'pass':sum(c['result']=='PASS' for c in cases),'fail':sum(c['result']=='FAIL' for c in cases),'skipped':sum(c['result']=='SKIPPED' for c in cases),'missingOriginal701':[c for c in baseline if (c['class'],c['name']) not in actual],'overall':'FAIL' if any(e['exitCode'] for e in executions) else 'PASS','scope':'Fresh per-command outputs only; no class filter; UP-TO-DATE is not counted as executed tests.'}
 (o/(phase+'-summary.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({k:result[k] for k in ('phase','total','jvm','android','pass','fail','skipped','overall')},indent=2))
