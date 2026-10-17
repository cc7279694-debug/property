"""D2A research-only offline build, byte-checked ASCII projection. Never installs."""
from pathlib import Path
import datetime, hashlib, json, os, shutil, subprocess

repo = Path(__file__).resolve().parents[3]
base = repo/'docs/research/m10-jpeg-r3'
jdk = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
sdk = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
out = repo/'docs/research/m10-jpeg-h2-d2a/evidence/build-runs'/stamp
out.mkdir(parents=True, exist_ok=False)
projection = Path('E:/CODEX/.codex/research-build/m10-h2-d2a')/stamp
projection.mkdir(parents=True, exist_ok=False)
inputs = {}
for f in (base/'host').rglob('*'):
    if f.is_file() and not any(x in ('build','.gradle') for x in f.relative_to(base/'host').parts):
        b=f.read_bytes(); target=projection/f.relative_to(base/'host')
        target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(b)
        assert target.read_bytes()==b
        inputs[f.relative_to(repo).as_posix()]={'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()}
(out/'inputs.json').write_text(json.dumps(inputs,indent=2),encoding='utf-8')
env=dict(os.environ,JAVA_HOME=str(jdk),ANDROID_HOME=str(sdk),ANDROID_SDK_ROOT=str(sdk))
argv=[str(repo/'gradlew.bat'),'-p',str(projection),':probe:testDebugUnitTest',':outsider:testDebugUnitTest',':probe:lintDebug',':outsider:lintDebug',':probe:assembleDebug',':outsider:assembleDebug','--offline','--max-workers=2','--console=plain','--stacktrace']
print(out,flush=True)
start=datetime.datetime.now(datetime.timezone.utc)
with (out/'gradle.log').open('wb') as log:
    done=subprocess.run(argv,cwd=repo,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=1200)
record={'argv':argv,'exitCode':done.returncode,'utc':stamp,'wallSeconds':(datetime.datetime.now(datetime.timezone.utc)-start).total_seconds(),'productionAppBuilt':False,'apkInstalled':False,'androidScenariosRun':False,'asciiSourceProjection':str(projection),'projectionInputsByteIdentical':True,'jdk':str(jdk),'sdk':str(sdk)}
for module in ('probe','outsider'):
    for f in (projection/module/'build').rglob('*'):
        if f.is_file() and (f.name.startswith('lint-results') or (f.suffix=='.xml' and 'test-results' in f.parts)):
            target=out/'reports'/module/f.relative_to(projection/module/'build')
            target.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(f,target)
    source=projection/module/'build/outputs/apk/debug'/f'{module}-debug.apk'
    if done.returncode==0 and source.is_file():
        target=repo/'build/d2a-artifacts'/stamp/source.name
        target.parent.mkdir(parents=True,exist_ok=True); shutil.copyfile(source,target)
        record[module+'Apk']={'path':str(target),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
(out/'result.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
print(json.dumps(record),flush=True)
print((out/'gradle.log').read_text(encoding='utf-8',errors='replace')[-5000:])
raise SystemExit(done.returncode)
