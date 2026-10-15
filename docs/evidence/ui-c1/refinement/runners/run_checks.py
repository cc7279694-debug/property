from pathlib import Path
import subprocess,os,sys,json,time,hashlib,zipfile,xml.etree.ElementTree as ET
from datetime import datetime,timezone
sys.stdout.reconfigure(encoding='utf-8',errors='replace')
R=Path.cwd();O=R/'build/ui-c1-refinement';phase=sys.argv[1];extra=sys.argv[2:]
assert not (O/(phase+'.log')).exists(),'Use a new phase name; original output is immutable'
J=Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1');ADB=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe')
serial='emulator-5596'
def sha(b):return hashlib.sha256(b).hexdigest()
def adb(*a):return subprocess.check_output([str(ADB),'-s',serial,*a]).decode('utf-8').strip()
assert adb('get-state')=='device' and adb('shell','getprop','ro.boot.qemu.avd_name')=='Lithic_M01_API_36'
assert adb('shell','getprop','ro.build.version.sdk')=='36'
inputs=[];sources=[]
for prefix in ('app/src/main','app/src/debug','app/src/test','app/src/androidTest','app/schemas'):
 for p in sorted((R/prefix).rglob('*')):
  if p.is_file():inputs.append({'path':p.relative_to(R).as_posix(),'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size})
  if p.is_file() and p.suffix=='.kt':sources.append(p)
for n in ('gradlew','gradlew.bat','app/build.gradle.kts','build.gradle.kts','settings.gradle.kts','gradle/libs.versions.toml','gradle.properties','gradle/wrapper/gradle-wrapper.properties','gradle/wrapper/gradle-wrapper.jar'):
 p=R/n;inputs.append({'path':n,'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size})
with zipfile.ZipFile(O/(phase+'-source.zip'),'x',zipfile.ZIP_DEFLATED) as z:
 for q in inputs:z.write(R/q['path'],q['path'])
tasks=[':app:connectedDebugAndroidTest']
if '--checks' in extra:extra.remove('--checks');tasks=[':app:testDebugUnitTest','--rerun',':app:connectedDebugAndroidTest',':app:lintDebug',':app:assembleDebug']
cmd=[str(R/'gradlew.bat'),*tasks,*extra,'--max-workers=2','--console=plain']
env=os.environ.copy();env['JAVA_HOME']=str(J);env['ANDROID_SERIAL']=serial
start=time.time();started=datetime.now(timezone.utc).isoformat()
commands=[cmd]
if ':app:testDebugUnitTest' in tasks:
 # Execute JVM first; a connected-test failure must not leave it unexecuted.
 commands=[[str(R/'gradlew.bat'),':app:testDebugUnitTest','--rerun-tasks','--max-workers=2','--console=plain'],
           [str(R/'gradlew.bat'),':app:connectedDebugAndroidTest',':app:lintDebug',':app:assembleDebug',*extra,'--continue','--max-workers=2','--console=plain']]
commandExecutions=[]
with (O/(phase+'.log')).open('wb') as log:
 for actualCommand in commands:
  began=time.time();status=subprocess.run(actualCommand,cwd=R,env=env,stdout=log,stderr=subprocess.STDOUT).returncode
  commandExecutions.append({'command':actualCommand,'exitCode':status,'durationSeconds':round(time.time()-began,3)})
code=next((row['exitCode'] for row in commandExecutions if row['exitCode']),0)
end=time.time();changed=[q['path'] for q in inputs if sha((R/q['path']).read_bytes())!=q['sha256']]
assert not changed,'Inputs changed during Gradle: '+str(changed)
reports=[];cases=[]
for kind,folder in [('android',R/'app/build/outputs/androidTest-results/connected/debug'),('jvm',R/'app/build/test-results/testDebugUnitTest')]:
 if kind=='jvm' and ':app:testDebugUnitTest' not in tasks:continue
 for i,p in enumerate(sorted(folder.rglob('*.xml'))):
  if p.stat().st_mtime<start-1:continue
  raw=p.read_bytes();t=ET.fromstring(raw);name=phase+'-'+kind+'-'+str(i)+'.xml';(O/name).write_bytes(raw)
  reports.append({'kind':kind,'path':name,'sha256':sha(raw),'bytes':len(raw),'counts':{k:int(t.get(k,'0')) for k in ('tests','failures','errors','skipped')},'devices':[q.get('value') for q in t.findall('./properties/property') if q.get('name')=='device']})
  for c in t.iter('testcase'):
   failure=c.find('failure');error=c.find('error');bad=failure if failure is not None else error
   cases.append({'kind':kind,'class':c.get('classname'),'name':c.get('name'),'result':'FAIL' if bad is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASS','failureType':bad.get('type') if bad is not None else None,'message':(bad.get('message') or bad.text or '')[:2000] if bad is not None else None})
baseline=[]
for p in sorted((R/'docs/evidence/m09-tests').glob('green-full-verified*.xml')):
 for c in ET.parse(p).getroot().iter('testcase'):baseline.append((c.get('classname'),c.get('name')))
assert len(set(baseline))==len(baseline)==549
actual={(c['class'],c['name']) for c in cases}
result={'phase':phase,'command':cmd,'actualCommandExecutions':commandExecutions,'startedUtc':started,'durationSeconds':round(end-start,3),'exitCode':code,'sourceInputs':inputs,'sourceZip':phase+'-source.zip','sourceZipSha256':sha((O/(phase+'-source.zip')).read_bytes()),'inputsStable':'PASS','reports':reports,'cases':cases,'total':len(cases),'pass':sum(c['result']=='PASS' for c in cases),'fail':sum(c['result']=='FAIL' for c in cases),'skipped':sum(c['result']=='SKIPPED' for c in cases),'jvm':sum(c['kind']=='jvm' for c in cases),'android':sum(c['kind']=='android' for c in cases),'original549Present':'NOT_CHECKED_PARTIAL_STAGE1','newDistinctTests':None}
(O/(phase+'-summary.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps({k:result[k] for k in ('phase','durationSeconds','exitCode','inputsStable','total','pass','fail','skipped','jvm','android','original549Present','newDistinctTests')},ensure_ascii=True))
print((O/(phase+'.log')).read_bytes().decode('utf-8',errors='replace')[-6000:])
