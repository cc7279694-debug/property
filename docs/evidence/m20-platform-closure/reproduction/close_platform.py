"""Exact second M20 run only. One cold start, one closure, one normal exit."""
import datetime,hashlib,json,os,pathlib,subprocess,sys,time,re
ROOT=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
OLD=ROOT/'docs/evidence/m20-android';OUT=ROOT/'docs/evidence/m20-platform-closure'
SDK=pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
BASE=pathlib.Path('E:/CODEX/.codex/research-env/lithic-m20-android-20261022')
NAME='Lithic_M20_API36_20261022'; SERIAL='emulator-5704'
ENV=os.environ.copy()
for k in ('ANDROID_SDK_HOME','ANDROID_PREFS_ROOT'):ENV.pop(k,None)
ENV.update(JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1',ANDROID_HOME=str(SDK),ANDROID_SDK_ROOT=str(SDK),ANDROID_USER_HOME=str(BASE/'android-user'),ANDROID_AVD_HOME=str(BASE/'avd-registration'),ANDROID_EMULATOR_HOME=str(BASE/'emulator-home'),TEMP=str(BASE/'tmp'),TMP=str(BASE/'tmp'))
ENV['PATH']=ENV['JAVA_HOME']+'/bin;'+ENV['PATH']
def sha(p):
 h=hashlib.sha256()
 with pathlib.Path(p).open('rb') as f:
  for b in iter(lambda:f.read(4*1024*1024),b''):h.update(b)
 return h.hexdigest()
def save(p,v):
 with pathlib.Path(p).open('x',encoding='utf-8') as f:json.dump(v,f,ensure_ascii=False,indent=2)
def safe(p):
 p=pathlib.Path(p).absolute();assert p.resolve().is_relative_to(BASE.resolve())
 for a in (p,*p.parents):
  if a.exists():assert not a.is_symlink() and not(a.stat().st_file_attributes&0x400),str(a)
 return p
COMMANDS=[];PHASE=None
def run(name,args,timeout=45,allow_nonzero=False):
 started=datetime.datetime.now(datetime.timezone.utc).isoformat();timed=False
 try:r=subprocess.run(args,capture_output=True,env=ENV,cwd=ROOT,timeout=timeout);a,b,code=r.stdout,r.stderr,r.returncode
 except subprocess.TimeoutExpired as e:a,b,code=e.stdout or b'',e.stderr or b'',None;timed=True
 for label,data in [('stdout',a),('stderr',b)]:
  with (PHASE/(name+'.'+label+'.txt')).open('xb') as f:f.write(data)
 rec=dict(name=name,command=args,started=started,exit_code=code,timed_out=timed,stdout_bytes=len(a),stdout_sha256=hashlib.sha256(a).hexdigest(),stderr_bytes=len(b),stderr_sha256=hashlib.sha256(b).hexdigest())
 COMMANDS.append(rec);(PHASE/'commands.json').write_text(json.dumps(COMMANDS,indent=2),encoding='utf-8')
 assert allow_nonzero or code==0,(name,code,b[-800:])
 assert not timed,(name,'UNKNOWN_TIMEOUT')
 return a.decode('utf-8-sig')
def adb(name,*args,timeout=45):return run(name,[str(SDK/'platform-tools/adb.exe'),'-s',SERIAL,*args],timeout)
def host(name):
 return json.loads(run(name,['powershell','-NoProfile','-Command',r"[ordered]@{processes=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match '^(emulator|qemu-system.*)\.exe$'}|ForEach-Object {[ordered]@{pid=$_.ProcessId;parent=$_.ParentProcessId;created=$_.CreationDate.ToString('o');path=$_.ExecutablePath;command=$_.CommandLine}});ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705 -ErrorAction SilentlyContinue|Select-Object LocalPort,OwningProcess);memory=(Get-CimInstance Win32_OperatingSystem|Select-Object FreePhysicalMemory,FreeVirtualMemory)}|ConvertTo-Json -Depth 6"]))
def fields(text):
 d={}
 for line in text.splitlines():
  if '=' in line and not line.lstrip().startswith('#'):
   k,v=line.split('=',1);k=k.strip();assert k not in d;d[k]=v.strip()
 return d
if __name__=='__main__':
 action=sys.argv[1];assert action in ('start','observe')
 PHASE=OUT/('start-authorized' if action=='start' else action);PHASE.mkdir(exist_ok=False)
 if action=='start':
  before=host('host-before');assert not before['processes'] and not before['ports'],'No second emulator permitted'
  assert before['memory']['FreePhysicalMemory']>3*1024*1024,'Insufficient memory'
  p=json.loads((OLD/'device/config-proof.json').read_text());image=json.loads((OLD/'device/tools-image.json').read_text())
  assert not (OUT/'start/attempt-claim.json').exists(), 'Earlier start must not have been issued'
  assert sha(safe(p['registration']))==p['registration_sha256']
  archived=fields((OLD/'device/generated-config.ini').read_text());actual=fields(safe(p['config']).read_text())
  delta={k:(archived.get(k),actual.get(k)) for k in archived.keys()|actual.keys() if archived.get(k)!=actual.get(k)}
  hardware=safe(BASE/(NAME+'.avd')/'hardware-qemu.ini');old_host=json.loads((OLD/'device/host-instance-proof.json').read_text())
  assert sha(hardware)==old_host['hardware_sha256'], 'Prior runtime identity changed'
  assert delta=={'disk.dataPartition.size':('2G','6442450944')},delta
  assert fields(hardware.read_text())['disk.dataPartition.size']=='6g'
  save(PHASE/'config-provenance.json',dict(status='PASS',previous_config_byte_sha=p['config_sha256'],current_config_byte_sha=sha(p['config']),semantic_delta=delta,archived_runtime_hardware_sha=sha(hardware),note='Only formatting and persisted 6GiB data size differ; exact prior runtime hardware byte SHA authenticates the existing size. No config was modified.'))
  for path in (BASE,BASE/(NAME+'.avd'),BASE/'runtime-data',BASE/'snapshots',BASE/'logs',BASE/'android-user',BASE/'emulator-home',BASE/'avd-registration'):assert safe(path).is_dir()
  props=fields(pathlib.Path(p['config']).read_text());assert props['hw.ramSize']=='2048' and props['hw.cpu.ncore']=='2'
  assert props['image.sysdir.1'].replace('\\','/').rstrip('/')=='system-images/android-36/google_apis/x86_64'
  for name,expected in image['files'].items():
   f=pathlib.Path(image['image'])/name;assert f.stat().st_size==expected['bytes'] and sha(f)==expected['sha256'],name
  assert sha(SDK/'emulator/emulator.exe')==image['emulator_sha256']
  save(PHASE/'static-proof.json',dict(status='PASS',config=p,official_image_files_hash_match=True,environment={k:ENV[k] for k in ('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME')},historical_result_sha256=sha(OLD/'platform-final-observation/retained-result.json')))
  args=[str(SDK/'emulator/emulator.exe'),'-avd',NAME,'-port','5704','-datadir',str(BASE/'runtime-data'),'-cache',str(BASE/'runtime-data/cache.img'),'-snapstorage',str(BASE/'snapshots/snapshots.img'),'-no-snapshot','-no-window','-no-boot-anim','-no-audio','-gpu','swiftshader','-memory','2048','-cores','2']
  save(PHASE/'attempt-claim.json',dict(action='one authorized original M20 cold start',command=args,at=datetime.datetime.now(datetime.timezone.utc).isoformat(),attempt=1))
  with (PHASE/'emulator.stdout.txt').open('xb') as out,(PHASE/'emulator.stderr.txt').open('xb') as err:
   proc=subprocess.Popen(args,stdout=out,stderr=err,env=ENV,cwd=ROOT,creationflags=subprocess.CREATE_NO_WINDOW)
  save(PHASE/'launcher.json',dict(pid=proc.pid,args=args,at=datetime.datetime.now(datetime.timezone.utc).isoformat()))
  print('M20 original AVD started once; launcher',proc.pid,flush=True)
 else:
  launcher=json.loads((OUT/'start-authorized/launcher.json').read_text())['pid'];deadline=time.monotonic()+60
  for i in range(13):
   value=host('identity-'+str(i));rows=value['processes'];p=next((x for x in rows if x['pid']==launcher),None);qs=[x for x in rows if x['parent']==launcher]
   if p and p['path'] and len(qs)==1 and qs[0]['path'] and value['ports']:break
   assert time.monotonic()<deadline,'Incomplete host identity';time.sleep(4)
  assert len(rows)==2 and p and len(qs)==1
  q=qs[0]
  for row,expected in [(p,SDK/'emulator/emulator.exe'),(q,SDK/'emulator/qemu/windows-x86_64/qemu-system-x86_64-headless.exe')]:
   assert pathlib.Path(row['path']).resolve()==expected.resolve()
   for arg in (NAME,'-port 5704','-no-snapshot','-no-window','-memory 2048','-cores 2',str(BASE/'runtime-data')):assert arg in row['command'],arg
  assert {x['LocalPort'] for x in value['ports']}=={5704,5705} and all(x['OwningProcess']==q['pid'] for x in value['ports'])
  hardware=safe(BASE/(NAME+'.avd')/'hardware-qemu.ini');hf=fields(hardware.read_text());assert hf['avd.name']==NAME and hf['avd.id']==NAME
  paths={k:hf[k] for k in ('disk.cachePartition.path','disk.dataPartition.path','disk.encryptionKeyPartition.path')}
  for path in paths.values():safe(path)
  save(PHASE/'host-proof.json',dict(status='PASS',processes=rows,ports=value['ports'],writable_paths=paths,hardware_sha256=sha(hardware)))
  deadline=time.monotonic()+120
  for i in range(25):
   state=run('state-'+str(i),[str(SDK/'platform-tools/adb.exe'),'-s',SERIAL,'get-state'],15,True).strip()
   if state=='device':break
   assert time.monotonic()<deadline;time.sleep(4)
  assert state=='device'
  deadline=time.monotonic()+240
  for i in range(49):
   boot=adb('boot-'+str(i),'shell','getprop sys.boot_completed').strip()
   if boot=='1':break
   assert time.monotonic()<deadline;time.sleep(5)
  assert boot=='1'
  assert adb('api','shell','getprop ro.build.version.sdk').strip()=='36'
  assert adb('abi','shell','getprop ro.product.cpu.abi').strip()=='x86_64'
  assert adb('avd','emu','avd','name').splitlines()[0]==NAME
  bid=adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip();assert re.fullmatch('[0-9a-f-]{36}',bid)
  save(PHASE/'device-proof.json',dict(status='PASS',serial=SERIAL,avd=NAME,boot_id=bid,old_boot_id='476456f9-76a7-45d6-b048-51cc1fc2edb5',cold_boot=True))
  print('PASS: only original M20 host and new boot verified',bid,flush=True)
