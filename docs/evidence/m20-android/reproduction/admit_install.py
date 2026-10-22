"""Only the newly claimed M20 emulator; no legacy serial or restart entry point."""
import datetime,hashlib,json,pathlib,re,subprocess,time,sys
from environment import ROOT,BASE,SDK,OUT,NAME,SERIAL,ENV,safe,sha,save,fields
ADB=str(SDK/'platform-tools/adb.exe')
COMMANDS=[]
PHASE=sys.argv[1] if sys.argv[1:] else "initial"
assert PHASE in ("initial","install-only","install-final")
INSTALL_ONLY=PHASE!="initial"
def command(name,args,timeout=60,check=True):
 name=(PHASE+'-'+name) if INSTALL_ONLY else name
 start=datetime.datetime.now(datetime.timezone.utc).isoformat()
 try:r=subprocess.run(args,capture_output=True,timeout=timeout,env=ENV)
 except subprocess.TimeoutExpired as e:
  a,b=e.stdout or b'',e.stderr or b'';code=None
 else:a,b,code=r.stdout,r.stderr,r.returncode
 for label,data in [('stdout',a),('stderr',b)]:
  with (OUT/f'{name}.{label}.txt').open('xb') as f:f.write(data)
 COMMANDS.append(dict(name=name,command=args,started=start,exit_code=code,stdout_bytes=len(a),stdout_sha256=hashlib.sha256(a).hexdigest(),stderr_bytes=len(b),stderr_sha256=hashlib.sha256(b).hexdigest()))
 (OUT/(PHASE+'-command-index.json' if INSTALL_ONLY else 'admission-command-index.json')).write_text(json.dumps(COMMANDS,indent=2))
 if check:assert code==0,(name,code,b[-1000:])
 elif code!=0:return None
 return a.decode('utf-8-sig')
def adb(name,*args,timeout=60,check=True):return command(name,[ADB,'-s',SERIAL,*args],timeout,check)
def host():
 text=subprocess.check_output(['powershell','-NoProfile','-Command',r"[ordered]@{processes=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match '^(emulator|qemu-system.*)\.exe$'}|ForEach-Object {[ordered]@{pid=$_.ProcessId;parent=$_.ParentProcessId;created=$_.CreationDate.ToString('o');path=$_.ExecutablePath;command=$_.CommandLine}});ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705 -ErrorAction SilentlyContinue|Select-Object LocalPort,OwningProcess)}|ConvertTo-Json -Depth 6"])
 return json.loads(text)

if not INSTALL_ONLY:
 launcher=json.loads((OUT/'launcher.json').read_text())['pid']
 deadline=time.monotonic()+60
 observations=[]
 while True:
  value=host();observations.append(value)
  rows=value['processes'];p=next((p for p in rows if p['pid']==launcher),None)
  q=[p for p in rows if p['parent']==launcher]
  if p and p['path'] and len(q)==1 and q[0]['path'] and len(value['ports'])>=2:break
  assert time.monotonic()<deadline,'Host identity UNKNOWN; do not send ADB'
  time.sleep(3)
 save('host-observations.json',observations)
 assert len(rows)==2 and p and len(q)==1
 q=q[0]
 assert pathlib.Path(p['path']).resolve()==(SDK/'emulator/emulator.exe').resolve()
 assert pathlib.Path(q['path']).resolve()==(SDK/'emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe').resolve()
 for row in rows:
  assert NAME in row['command'] and '-port 5704' in row['command'] and '-no-snapshot' in row['command'] and '-no-window' in row['command']
 assert {p['LocalPort'] for p in value['ports']}=={5704,5705} and all(p['OwningProcess']==q['pid'] for p in value['ports'])
 hardware=safe(BASE/(NAME+'.avd')/'hardware-qemu.ini')
 hardware_fields=fields(hardware.read_text())
 assert hardware_fields['avd.name']==NAME and hardware_fields['avd.id']==NAME
 writable={k:hardware_fields[k] for k in ('disk.cachePartition.path','disk.dataPartition.path','disk.encryptionKeyPartition.path')}
 for path in writable.values():safe(path)
 save('host-instance-proof.json',dict(processes=rows,ports=value['ports'],hardware_sha256=sha(hardware),writable_paths=writable))
 deadline=time.monotonic()+120
 for index in range(25):
  state=adb(f'state-{index:02}','get-state',timeout=15,check=False)
  if state is not None and state.strip()=='device':break
  assert time.monotonic()<deadline,'ADB not online; no second start'
  time.sleep(5)
 assert state is not None and state.strip()=='device'
 deadline=time.monotonic()+300
 for i in range(61):
  boot=adb(f'boot-{i:02}','shell','getprop sys.boot_completed',timeout=20).strip()
  if boot=='1':break
  assert time.monotonic()<deadline,'Boot incomplete; no retry start'
  time.sleep(5)
 assert boot=='1'
 assert adb('api','shell','getprop ro.build.version.sdk').strip()=='36'
 assert adb('abi','shell','getprop ro.product.cpu.abi').strip()=='x86_64'
 assert adb('name','emu','avd','name').splitlines()[0]==NAME
 boot_id=adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip();assert re.fullmatch(r'[a-f0-9-]{36}',boot_id)
 common=dict(status='PASS',serial=SERIAL,avd=NAME,api=36,boot_id=boot_id)
 save('device-admission.json',dict(**common,host=rows))
else:
 common=json.loads((OUT/'device-admission.json').read_text())
 assert common['status']=='PASS' and common['serial']==SERIAL and common['avd']==NAME
 assert adb('resumed-state','get-state').strip()=='device'
 assert adb('resumed-boot','shell','getprop sys.boot_completed').strip()=='1'
 assert adb('resumed-boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip()==common['boot_id']
 boot_id=common['boot_id']
 common={k:common[k] for k in ('status','serial','avd','api','boot_id')}
artifacts={
 'app':('com.cooper.lithic.debug',ROOT/'app/build/outputs/apk/debug/app-debug.apk'),
 'test':('com.cooper.lithic.debug.test',ROOT/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')}
identities={}
for label,(package,path) in artifacts.items():
 digest=sha(path)
 if label=='app':assert digest=='776c00167dd856d39791fca4c9331fdfaf313dab096b2835df6e40b3a66fda38'
 cert=command(label+'-certificate',[str(SDK/'build-tools/36.0.0/apksigner.bat'),'verify','--print-certs',str(path)])
 certificate='29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9';assert certificate in cert
 manifest=command(label+'-manifest',[str(SDK/'build-tools/36.0.0/aapt.exe'),'dump','badging',str(path.relative_to(ROOT))])
 assert f"package: name='{package}'" in manifest
 assert not adb(label+'-package-absent','shell','cmd','package','list','packages',package).strip()
 absent=adb(label+'-absent','shell','pm','path',package,check=False)
 assert absent in (None,'') and COMMANDS[-1]['exit_code'] in (0,1) and COMMANDS[-1]['stderr_bytes']==0
 save(label+'-install-claim.json',dict(serial=SERIAL,package=package,sha256=digest,attempt=1))
 assert 'Success' in adb(label+'-install','install','-t',str(path),timeout=150)
 paths=adb(label+'-path','shell','pm','path',package).strip().splitlines()
 assert len(paths)==1 and paths[0].startswith('package:/data/app/') and paths[0].endswith('/base.apk')
 assert adb(label+'-readback-sha','shell','sha256sum',paths[0][8:]).split()[0]==digest
 uidtext=adb(label+'-uid','shell','cmd','package','list','packages','-U',package)
 uids=re.findall(r'^package:'+re.escape(package)+r' uid:(\d+)\s*$',uidtext,re.M);assert len(uids)==1
 uid=int(uids[0]);assert 10000<=uid<100000
 identities[label]=dict(package=package,sha256=digest,bytes=path.stat().st_size,uid=uid,certificate_sha256=certificate,installed_path=paths[0][8:])
assert identities['app']['uid']!=identities['test']['uid']
component='com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner'
assert f'{component} (target=com.cooper.lithic.debug)' in adb('instrumentation','shell','pm','list','instrumentation','com.cooper.lithic.debug')
assert adb('fresh-no-backup','shell',"run-as com.cooper.lithic.debug sh -c 'if [ ! -e no_backup ]; then echo ABSENT; else ls -ln no_backup; fi'").strip() in ('ABSENT','total 0')
for name,query in [('font','settings get system font_scale'),('size','wm size'),('density','wm density'),('rotation','settings get system user_rotation')]:adb('initial-'+name,'shell',query)
save('installed-proof.json',dict(**common,**identities,instrumentation=component,old_device_commands=0,default_ledger_opened=False))
print('PASS: new isolated M20 device and installed App/Test identities')
