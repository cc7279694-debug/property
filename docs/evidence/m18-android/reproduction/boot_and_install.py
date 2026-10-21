"""Read-only admission, then install only the two approved M18 Debug packages on 5692."""
import datetime, hashlib, json, os, pathlib, re, subprocess, time

ROOT=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
BASE=pathlib.Path('E:/CODEX/.codex/research-env/lithic-m18-android-20261021')
SDK=pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
OUT=ROOT/'docs/evidence/m18-android/device'
ADB=str(SDK/'platform-tools/adb.exe'); SERIAL='emulator-5692'; NAME='Lithic_M18_API36_20261021'
env=os.environ.copy(); env['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
commands=[]
def save(name,data):
    with (OUT/name).open('x',encoding='utf-8') as f: json.dump(data,f,ensure_ascii=False,indent=2)
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def run(name,cmd,timeout=90):
    r=subprocess.run(cmd,capture_output=True,env=env,timeout=timeout)
    for label,b in [('stdout',r.stdout),('stderr',r.stderr)]:
        with (OUT/f'{name}.{label}.txt').open('xb') as f:f.write(b)
    commands.append(dict(name=name,command=cmd,exit_code=r.returncode,
        at=datetime.datetime.now(datetime.timezone.utc).isoformat(),stdout_bytes=len(r.stdout),
        stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stderr_bytes=len(r.stderr),stderr_sha256=hashlib.sha256(r.stderr).hexdigest()))
    (OUT/'admission-commands.json').write_text(json.dumps(commands,indent=2),encoding='utf-8')
    assert r.returncode==0,(name,r.returncode)
    return r.stdout.decode('utf-8-sig')
def adb(name,*args,timeout=90):return run(name,[ADB,'-s',SERIAL,*args],timeout)

proof=json.loads((OUT/'config-proof.json').read_text())
registration=pathlib.Path(proof['registration'])
assert sha(registration)==proof['registration_sha256']
registration_fields=dict(l.split('=',1) for l in registration.read_text().splitlines() if '=' in l)
assert pathlib.Path(registration_fields['path']).resolve()==pathlib.Path(proof['actual_avd']).resolve()
# Check lexical ancestors, not just the resolved final path, for links/reparse points.
for path in [BASE,registration,pathlib.Path(proof['actual_avd'])]:
    for part in (path,*path.parents):
        assert not part.is_symlink() and not (part.stat().st_file_attributes & 0x400),part
        if part==BASE.parent:break
ps="Get-CimInstance Win32_Process -Filter 'ProcessId=5688 OR ProcessId=34920' | ForEach-Object { [pscustomobject]@{ProcessId=$_.ProcessId;ParentProcessId=$_.ParentProcessId;CreationDate=$_.CreationDate.ToString('o');ExecutablePath=$_.ExecutablePath;CommandLine=$_.CommandLine} } | ConvertTo-Json -Depth 4"
processes=json.loads(run('host-instance',['powershell','-NoProfile','-Command',ps]))
assert len(processes)==2
launcher=next(p for p in processes if p['ProcessId']==5688)
qemu=next(p for p in processes if p['ProcessId']==34920)
assert qemu['ParentProcessId']==launcher['ProcessId']
assert launcher['CreationDate']=='2026-10-21T19:40:18.0643680+08:00'
assert qemu['CreationDate']=='2026-10-21T19:40:18.2920070+08:00'
assert pathlib.Path(launcher['ExecutablePath']).resolve()==(SDK/'emulator/emulator.exe').resolve()
assert pathlib.Path(qemu['ExecutablePath']).resolve()==(SDK/'emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe').resolve()
for p in processes:
    assert NAME in p['CommandLine'] and '-port 5692' in p['CommandLine'] and '-no-snapshot' in p['CommandLine']
ports=json.loads(run('target-ports',['powershell','-NoProfile','-Command',
    "@(Get-NetTCPConnection -State Listen -LocalPort 5692,5693 | Select-Object LocalPort,OwningProcess) | ConvertTo-Json"]))
assert {p['LocalPort'] for p in ports}=={5692,5693} and all(p['OwningProcess']==qemu['ProcessId'] for p in ports)
hardware=pathlib.Path(proof['actual_avd'])/'hardware-qemu.ini'
text=hardware.read_text(); fields=dict(l.split(' = ',1) for l in text.splitlines() if ' = ' in l)
assert fields['avd.name']==NAME and fields['avd.id']==NAME
writable={key:value for key,value in fields.items() if key in ('disk.cachePartition.path','disk.dataPartition.path','disk.encryptionKeyPartition.path')}
assert len(writable)==3
for path in writable.values():assert pathlib.Path(path).resolve().is_relative_to(BASE.resolve())
save('host-instance-proof.json',dict(processes=processes,ports=ports,hardware=str(hardware),hardware_sha256=sha(hardware),writable_paths=writable,
    start_operation_count=1,start_outer_receipt='NOT_CAPTURED at admission; actual instance independently verified',old_device_commands=0))
assert adb('state','get-state').strip()=='device'
deadline=time.monotonic()+120
for index in range(25):
    boot=adb(f'boot-{index:02d}','shell','getprop sys.boot_completed').strip()
    if boot=='1':break
    assert time.monotonic()<deadline,'Boot incomplete; no restart permitted'
    time.sleep(5)
assert boot=='1'
assert adb('api','shell','getprop ro.build.version.sdk').strip()=='36'
assert adb('abi','shell','getprop ro.product.cpu.abi').strip()=='x86_64'
assert adb('avd','emu','avd','name').splitlines()[0]==NAME
boot_id=adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip()
assert re.fullmatch(r'[a-f0-9-]{36}',boot_id)
save('boot-proof.json',dict(serial=SERIAL,avd=NAME,api=36,abi='x86_64',boot_completed=1,boot_id=boot_id,host_processes=processes))
expected=json.loads((ROOT/'docs/evidence/m18/apk/identities.json').read_text(encoding='utf-8'))['artifacts']
identities=[]
for label,item in expected.items():
    path=pathlib.Path(item['path']);assert sha(path)==item['sha256'] and path.stat().st_size==item['bytes']
    cert=run(label+'-certificate',[str(SDK/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs',str(path)])
    assert item['certificate_sha256'] in cert
    pkg=item['application_id']
    assert not adb(label+'-before-path','shell','pm','path',pkg).strip(),'Fresh AVD package must be absent'
    save(label+'-install-claim.json',dict(package=pkg,sha256=item['sha256'],serial=SERIAL,operation='install',attempt=1))
    assert 'Success' in adb(label+'-install','install','-t',str(path),timeout=120)
    paths=adb(label+'-installed-path','shell','pm','path',pkg).strip().splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/') and paths[0].endswith('/base.apk')
    assert adb(label+'-installed-sha','shell','sha256sum',paths[0][8:]).split()[0]==item['sha256']
    rows=adb(label+'-uid','shell','cmd','package','list','packages','-U',pkg).splitlines()
    matching=[re.fullmatch('package:'+re.escape(pkg)+r' uid:(\d+)',row) for row in rows]
    uid=[int(m.group(1)) for m in matching if m];assert len(uid)==1
    # Read installed APK bytes only, never any app ledger or media content.
    data=subprocess.run([ADB,'-s',SERIAL,'exec-out','cat',paths[0][8:]],capture_output=True,timeout=90)
    assert data.returncode==0 and not data.stderr and hashlib.sha256(data.stdout).hexdigest()==item['sha256']
    local=ROOT/'build'/f'm18-installed-{label}.apk';local.parent.mkdir(exist_ok=True);local.write_bytes(data.stdout)
    installed_cert=run(label+'-installed-certificate',[str(SDK/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs',str(local)])
    assert item['certificate_sha256'] in installed_cert
    identities.append(dict(label=label,package=pkg,uid=uid[0],sha256=item['sha256'],certificate_sha256=item['certificate_sha256'],installed_path=paths[0][8:]))
assert len({i['uid'] for i in identities})==2
instrumentation=adb('instrumentation','shell','pm','list','instrumentation','com.cooper.lithic.debug')
assert 'com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner (target=com.cooper.lithic.debug)' in instrumentation
assert adb('before-no-backup','shell','run-as','com.cooper.lithic.debug','ls','-ln','no_backup').strip()=='total 0'
for name,command in [('font','settings get system font_scale'),('size','wm size'),('density','wm density'),('rotation','settings get system user_rotation')]:adb(name,'shell',command)
save('installed-identities.json',dict(serial=SERIAL,avd=NAME,boot_id=boot_id,applications=identities,old_device_commands=0,default_ledger_opened=False))
print('PASS: fresh isolated boot and both installed APK bytes/certificates/UIDs verified')
