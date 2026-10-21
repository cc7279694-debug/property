import hashlib, json, os, pathlib, subprocess, datetime
root=pathlib.Path(__file__).resolve().parents[2]
sdk=pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
out=root/'build/m14-android/test-update02'; out.mkdir(exist_ok=False)
records=[]
def run(name,args):
 r=subprocess.run(args,capture_output=True,timeout=90)
 (out/(name+'.stdout.txt')).write_bytes(r.stdout);(out/(name+'.stderr.txt')).write_bytes(r.stderr)
 records.append(dict(name=name,command=args,exit_code=r.returncode,utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stdout_bytes=len(r.stdout),stderr_bytes=len(r.stderr)))
 (out/'commands.json').write_text(json.dumps(records,indent=2),encoding='utf-8')
 assert r.returncode==0,name
 return r.stdout.decode('utf-8')
def adb(name,*args):return run(name,[str(sdk/'platform-tools/adb.exe'),'-s','emulator-5672',*args])
assert adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip()=='0b481eff-e49d-48f9-a740-2d2e9d3ce0e1'
for kind,pkg,expected in [('app','com.cooper.lithic.debug','59d45ad38b467e5c4bb895c71feab3315bb872d670177ab7457033179c4b35b9'),('old-test','com.cooper.lithic.debug.test','f1e1b85dcf704fdaaf6cce5259b28e6a8b778123b4aadeb95e6d4ed3a9497257')]:
 p=adb(kind+'-path','shell','pm','path',pkg).strip().splitlines();assert len(p)==1
 assert adb(kind+'-sha','shell','sha256sum',p[0][8:]).split()[0]==expected
test=root/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
sha=hashlib.sha256(test.read_bytes()).hexdigest()
cert=run('new-test-certificate',['cmd','/c',str(sdk/'build-tools/36.0.0/apksigner.bat'),'verify','--verbose','--print-certs',str(test)])
assert '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9' in cert
manifest=run('new-test-manifest',[str(sdk/'build-tools/36.0.0/aapt2.exe'),'dump','xmltree',str(test),'--file','AndroidManifest.xml'])
assert 'com.cooper.lithic.app.M13IsolatedRunner' in manifest
assert 'package="com.cooper.lithic.debug.test"' in manifest
artifact=dict(bytes=test.stat().st_size,sha256=sha,applicationId='com.cooper.lithic.debug.test')
(out/'artifact.json').write_text(json.dumps(artifact,indent=2),encoding='utf-8')
with (out/'update-claim.json').open('x',encoding='utf-8') as f:json.dump(artifact,f)
assert 'Success' in adb('update','install','-r','-t',str(test))
p=adb('new-test-path','shell','pm','path','com.cooper.lithic.debug.test').strip().splitlines();assert len(p)==1
assert adb('new-test-sha','shell','sha256sum',p[0][8:]).split()[0]==sha
print(json.dumps(artifact),flush=True)
