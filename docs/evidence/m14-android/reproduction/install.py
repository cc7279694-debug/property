import datetime, hashlib, json, os, pathlib, re, subprocess
ROOT = pathlib.Path(__file__).resolve().parents[2]
SDK = pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
ADB = str(SDK/'platform-tools/adb.exe')
OUT = ROOT/'build/m14-android/install-final'
OUT.mkdir(exist_ok=False)
commands = []
def run(name, cmd):
    result = subprocess.run(cmd, capture_output=True, timeout=90)
    (OUT/(name+'.stdout.txt')).write_bytes(result.stdout)
    (OUT/(name+'.stderr.txt')).write_bytes(result.stderr)
    commands.append(dict(name=name,command=cmd,exit_code=result.returncode,
        utc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        stdout_bytes=len(result.stdout),stdout_sha256=hashlib.sha256(result.stdout).hexdigest(),
        stderr_bytes=len(result.stderr),stderr_sha256=hashlib.sha256(result.stderr).hexdigest()))
    (OUT/'commands.json').write_text(json.dumps(commands,indent=2),encoding='utf-8')
    if result.returncode: raise RuntimeError(name+' failed')
    return result.stdout.decode('utf-8')
def adb(name, *args): return run(name,[ADB,'-s','emulator-5672',*args])
assert adb('state','get-state').strip() == 'device'
assert adb('boot','shell','getprop sys.boot_completed').strip() == '1'
assert adb('api','shell','getprop ro.build.version.sdk').strip() == '36'
assert adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip() == '0b481eff-e49d-48f9-a740-2d2e9d3ce0e1'
assert adb('avd','emu','avd','name').splitlines()[0] == 'Lithic_M14_API36_20261021'
identity=[]
for name, relative, package in [
 ('app','app/build/outputs/apk/debug/app-debug.apk','com.cooper.lithic.debug'),
 ('test','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk','com.cooper.lithic.debug.test')]:
    path=ROOT/relative
    sha=hashlib.sha256(path.read_bytes()).hexdigest()
    if name=='app': assert sha=='59d45ad38b467e5c4bb895c71feab3315bb872d670177ab7457033179c4b35b9'
    cert=run(name+'-certificate',['cmd','/c',str(SDK/'build-tools/36.0.0/apksigner.bat'),'verify','--verbose','--print-certs',str(path)])
    assert '29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9' in cert
    manifest=run(name+'-manifest',[str(SDK/'build-tools/36.0.0/aapt2.exe'),'dump','xmltree',str(path),'--file','AndroidManifest.xml'])
    assert 'package="'+package+'"' in manifest
    if name=='test':
        assert 'com.cooper.lithic.app.M13IsolatedRunner' in manifest
        assert 'android:targetPackage(0x01010021)="com.cooper.lithic.debug"' in manifest
    else:
        assert 'android.permission.INTERNET' not in manifest
    identity.append(dict(name=name,path=relative,bytes=path.stat().st_size,sha256=sha,package=package))
(OUT/'artifacts.json').write_text(json.dumps(identity,indent=2),encoding='utf-8')
for item in identity:
    name=item['name']; package=item['package']
    listed = adb(name+'-before-packages','shell','pm','list','packages',package).splitlines()
    assert 'package:'+package not in listed
    marker=OUT/(name+'-install-claim.json')
    with marker.open('x',encoding='utf-8') as f: json.dump(item,f,indent=2)
    install=adb(name+'-install','install','-t',str(ROOT/item['path']))
    assert 'Success' in install
    paths=adb(name+'-installed-path','shell','pm','path',package).strip().splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/') and paths[0].endswith('/base.apk')
    installed_sha=adb(name+'-installed-sha','shell','sha256sum',paths[0][8:]).split()[0]
    assert installed_sha==item['sha256']
    dump=adb(name+'-installed-package','shell','dumpsys','package',package)
    assert 'Package ['+package+']' in dump and 'User 0:' in dump and 'installed=true' in dump
    print(name, item['sha256'], 'installed and byte matched',flush=True)
print(adb('instrumentation','shell','pm','list','instrumentation'),flush=True)
