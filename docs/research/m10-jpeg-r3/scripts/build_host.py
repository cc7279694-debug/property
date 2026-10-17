"""Build only the independent research Gradle root; never the production :app."""
from pathlib import Path
import datetime, hashlib, json, os, shutil, subprocess, sys, time

BASE = Path(__file__).resolve().parents[1]
REPO = BASE.parents[2]
JDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1')
SDK = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
out = BASE / 'evidence' / 'build-runs' / stamp
out.mkdir(parents=True)
inputs = {}
projection=Path('E:/CODEX/.codex/research-build/m10-r3')/stamp
assert projection.resolve().is_relative_to(Path('E:/CODEX/.codex/research-build/m10-r3').resolve())
projection.mkdir(parents=True,exist_ok=False)
for path in (BASE / 'host').rglob('*'):
    if path.is_file() and not any(part in ('build', '.gradle') for part in path.relative_to(BASE/'host').parts):
        data=path.read_bytes(); inputs[str(path.relative_to(REPO))]={'sha256':hashlib.sha256(data).hexdigest(),'bytes':len(data)}
        target=projection/path.relative_to(BASE/'host');target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data)
        assert hashlib.sha256(target.read_bytes()).hexdigest()==hashlib.sha256(data).hexdigest()
(out/'inputs.json').write_text(json.dumps(inputs,indent=2),encoding='utf-8')
env=dict(os.environ, JAVA_HOME=str(JDK), ANDROID_HOME=str(SDK), ANDROID_SDK_ROOT=str(SDK))
argv=[str(REPO/'gradlew.bat'),'-p',str(projection),':probe:testDebugUnitTest',':probe:lintDebug',':probe:assembleDebug',':outsider:assembleDebug','--offline','--max-workers=2','--console=plain','--stacktrace']
if '--no-lint' in sys.argv: argv.remove(':probe:lintDebug')
start=time.monotonic()
print(str(out),flush=True)
with (out/'gradle.log').open('wb') as log:
    done=subprocess.run(argv,cwd=REPO,env=env,stdout=log,stderr=subprocess.STDOUT,timeout=1200)
record={'argv':argv,'exitCode':done.returncode,'wallSeconds':time.monotonic()-start,'jdk':str(JDK),'sdk':str(SDK),'productionAppBuilt':False,'utc':stamp,'asciiSourceProjection':str(projection),'projectionInputsByteIdentical':True}
if done.returncode==0:
    for module in ('probe','outsider'):
        source=projection/module/'build/outputs/apk/debug'/f'{module}-debug.apk'
        target=BASE/'host'/module/'build/outputs/apk/debug'/source.name
        target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
        record[module+'Apk']={'path':str(target),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'bytes':target.stat().st_size}
    for source in (projection/'probe/build').rglob('*'):
        if source.is_file() and (source.name.startswith('lint-results') or (source.suffix=='.xml' and 'test-results' in source.parts)):
            target=out/'reports'/source.relative_to(projection/'probe/build');target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source,target)
(out/'result.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
print(json.dumps(record),flush=True)
print((out/'gradle.log').read_text(encoding='utf-8',errors='replace')[-7000:])
sys.exit(done.returncode)
