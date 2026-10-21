"""M19: update only the reverified original5692 instance; no create/start/clear entry."""
import datetime, hashlib, json, os, pathlib, re, subprocess, sys
ROOT=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
SDK=pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk'); ADB=str(SDK/'platform-tools/adb.exe')
phase=sys.argv[1];assert phase in ('initial','test-fix','stale-red','stale-green')
OUT=ROOT/f'docs/evidence/m19/install-{phase}';OUT.mkdir(exist_ok=False)
env=os.environ.copy();env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
commands=[]
def run(name,cmd,allowed=(0,),timeout=120):
    r=subprocess.run(cmd,capture_output=True,env=env,timeout=timeout)
    for suffix,b in [('stdout',r.stdout),('stderr',r.stderr)]: (OUT/f'{name}.{suffix}.txt').write_bytes(b)
    commands.append(dict(name=name,command=cmd,exit_code=r.returncode,at=datetime.datetime.now(datetime.timezone.utc).isoformat(),stdout_bytes=len(r.stdout),stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stderr_bytes=len(r.stderr),stderr_sha256=hashlib.sha256(r.stderr).hexdigest()))
    (OUT/'commands.json').write_text(json.dumps(commands,indent=2),encoding='utf-8')
    assert r.returncode in allowed,(name,r.returncode)
    return r
def adb(name,*args,allowed=(0,)): return run(name,[ADB,'-s','emulator-5692',*args],allowed)
def text(r): return r.stdout.decode('utf-8-sig').strip()
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
old=json.loads((ROOT/'docs/evidence/m18-android/device/boot-proof.json').read_text())
ps="Get-CimInstance Win32_Process -Filter 'ProcessId=5688 OR ProcessId=34920' | ForEach-Object { [pscustomobject]@{ProcessId=$_.ProcessId;ParentProcessId=$_.ParentProcessId;CreationDate=$_.CreationDate.ToString('o');ExecutablePath=$_.ExecutablePath;CommandLine=$_.CommandLine} } | ConvertTo-Json -Depth 4"
assert json.loads(text(run('host-instance',['powershell','-NoProfile','-Command',ps])))==old['host_processes']
assert text(adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id'))==old['boot_id']
assert text(adb('boot','shell','getprop sys.boot_completed'))=='1'
assert text(adb('api','shell','getprop ro.build.version.sdk'))=='36'
assert text(adb('avd','emu','avd','name')).splitlines()[0]==old['avd']
expected=json.loads((ROOT/'docs/evidence/m18/apk/identities.json').read_text(encoding='utf-8'))['artifacts']
previous_phase = 'stale-red' if phase=='stale-green' else 'initial'
prior=json.loads((ROOT/('docs/evidence/m18-android/install-driver-fix/artifacts.json' if phase=='initial' else f'docs/evidence/m19/install-{previous_phase}/artifacts.json')).read_text(encoding='utf-8'))
identities=[]
for label,item in expected.items():
    path=pathlib.Path(item['path']); actual=sha(path)
    previous=next(p for p in prior['applications'] if p['label']==label)
    assert not path.is_symlink()
    cert=text(run(label+'-certificate',[str(SDK/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs',str(path)]))
    assert item['certificate_sha256'] in cert
    manifest=text(run(label+'-manifest',[str(SDK/'build-tools/36.0.0/aapt2.exe'),'dump','xmltree',str(path),'--file','AndroidManifest.xml']))
    pkg=item['application_id'];assert f'package="{pkg}"' in manifest
    if label=='test':assert 'com.cooper.lithic.app.M13IsolatedRunner' in manifest
    else:assert 'android.permission.INTERNET' not in manifest
    before=adb(label+'-before-path','shell','pm','path',pkg,allowed=(0,1))
    uid_rows=text(adb(label+'-before-list','shell','cmd','package','list','packages','-U',pkg)).splitlines()
    matches=[x for x in uid_rows if x.startswith('package:'+pkg+' uid:')]
    absent=False
    assert before.returncode==0 and len(matches)==1
    already=False
    if not absent:
        assert before.returncode==0 and len(matches)==1
        paths=text(before).splitlines();assert len(paths)==1 and paths[0].startswith('package:/data/app/')
        installed=text(adb(label+'-before-sha','shell','sha256sum',paths[0][8:])).split()[0]
        assert installed==previous['sha256']
        already=installed==actual
    assert not absent
    if not already:
        claim=OUT/(label+'-install-claim.json')
        with claim.open('x',encoding='utf-8') as f:json.dump(dict(serial='emulator-5692',package=pkg,sha256=actual,attempt=1,update=not absent),f)
        args=['install','-t']+([] if absent else ['-r'])+[str(path)]
        assert 'Success' in text(adb(label+'-install',*args))
    paths=text(adb(label+'-installed-path','shell','pm','path',pkg)).splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/') and paths[0].endswith('/base.apk')
    assert text(adb(label+'-installed-sha','shell','sha256sum',paths[0][8:])).split()[0]==actual
    uid_rows=text(adb(label+'-installed-uid','shell','cmd','package','list','packages','-U',pkg)).splitlines()
    uids=[int(m.group(1)) for line in uid_rows if (m:=re.fullmatch('package:'+re.escape(pkg)+r' uid:(\d+)',line))]
    assert len(uids)==1 and uids[0]==previous['uid']
    identities.append(dict(label=label,package=pkg,uid=uids[0],bytes=path.stat().st_size,sha256=actual,
        certificate_sha256=item['certificate_sha256'],certificate_basis='Installed APK SHA equals the actual locally signature-verified APK byte for byte',installed_path=paths[0][8:],install_skipped=already))
assert len({p['uid'] for p in identities})==2
assert 'com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner (target=com.cooper.lithic.debug)' in text(adb('instrumentation','shell','pm','list','instrumentation','com.cooper.lithic.debug'))
assert text(adb('no-backup','shell','run-as','com.cooper.lithic.debug','ls','-ln','no_backup'))=='total 0'
for label,cmd in [('font','settings get system font_scale'),('size','wm size'),('density','wm density'),('rotation','settings get system user_rotation')]:adb(label,'shell',cmd)
(OUT/'artifacts.json').write_text(json.dumps(dict(serial='emulator-5692',avd=old['avd'],boot_id=old['boot_id'],applications=identities,old_device_commands=0,default_ledger_opened=False),indent=2),encoding='utf-8')
print('PASS: two installed Debug byte identities, certificate equality, distinct UIDs and isolated Runner')
