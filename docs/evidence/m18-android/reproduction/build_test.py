"""Only the affected androidTest APK is rebuilt; production App bytes stay locked."""
import datetime, hashlib, json, os, pathlib, subprocess, sys, time
ROOT=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
OUT=ROOT/'docs/evidence/m18-android'/sys.argv[1];OUT.mkdir(exist_ok=False)
env=os.environ.copy();env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
app=ROOT/'app/build/outputs/apk/debug/app-debug.apk'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
before=sha(app);assert before=='a837fd4ae881efbf042587a53b87414a8a94cad935ba74ee99b87e7cce5b8ed9'
cmd=['gradlew.bat',':app:assembleDebugAndroidTest','-Pm13Isolated=true','--max-workers=2','--console=plain']
started=datetime.datetime.now(datetime.timezone.utc).isoformat();clock=time.monotonic()
with (OUT/'stdout.txt').open('xb') as out, (OUT/'stderr.txt').open('xb') as err:
 r=subprocess.run(cmd,cwd=ROOT,env=env,stdout=out,stderr=err,timeout=900)
result=dict(command=cmd,started_at=started,exit_code=r.returncode,seconds=round(time.monotonic()-clock,3),app_before=before,app_after=sha(app),test_sha256=sha(ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'),logs={n:dict(bytes=(OUT/n).stat().st_size,sha256=sha(OUT/n)) for n in ['stdout.txt','stderr.txt']})
(OUT/'result.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
assert r.returncode==0 and result['app_before']==result['app_after'];print(json.dumps(result))
