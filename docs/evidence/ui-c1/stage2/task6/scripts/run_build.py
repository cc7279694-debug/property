from pathlib import Path
import subprocess,os,json,time,hashlib,sys
r=Path.cwd();o=r/'build/ui-c1-stage2';phase=sys.argv[1];ref=sys.argv[2];m=json.loads((o/(ref+'-summary.json')).read_text(encoding='utf8'));inputs=m['sourceInputs'];sha=lambda b:hashlib.sha256(b).hexdigest()
for p in inputs:assert sha((r/p['path']).read_bytes())==p['sha256']
cmd=[str(r/'gradlew.bat'),':app:lintDebug',':app:assembleDebug','--max-workers=2','--console=plain'];env=os.environ.copy();env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1';start=time.time();log=o/(phase+'.log');assert not log.exists()
with log.open('xb') as f:code=subprocess.run(cmd,env=env,stdout=f,stderr=subprocess.STDOUT).returncode
for p in inputs:assert sha((r/p['path']).read_bytes())==p['sha256']
xml=r/'app/build/reports/lint-results-debug.xml';(o/(phase+'-lint.xml')).write_bytes(xml.read_bytes())
v={'phase':phase,'command':cmd,'exitCode':code,'seconds':time.time()-start,'inputsReference':ref+'-summary.json','inputsStable':True,'logSha256':sha(log.read_bytes()),'lintSha256':sha(xml.read_bytes()),'apkSha256':sha((r/'app/build/outputs/apk/debug/app-debug.apk').read_bytes())};(o/(phase+'-summary.json')).write_text(json.dumps(v,indent=2)+'\n',encoding='utf8');print(json.dumps(v))
