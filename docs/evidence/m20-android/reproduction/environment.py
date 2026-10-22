"""One new low-resource M20 AVD. No legacy-device or retry entry point."""
import datetime,hashlib,json,os,pathlib,subprocess,sys
ROOT=next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
SDK=pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
BASE=pathlib.Path('E:/CODEX/.codex/research-env/lithic-m20-android-20261022')
OUT=ROOT/'docs/evidence/m20-android/device'
NAME='Lithic_M20_API36_20261022';SERIAL='emulator-5704';PORT=5704
ENV=os.environ.copy()
for key in ('ANDROID_SDK_HOME','ANDROID_PREFS_ROOT'):ENV.pop(key,None)
ENV.update(JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1',ANDROID_HOME=str(SDK),ANDROID_SDK_ROOT=str(SDK),ANDROID_USER_HOME=str(BASE/'android-user'),ANDROID_AVD_HOME=str(BASE/'avd-registration'),ANDROID_EMULATOR_HOME=str(BASE/'emulator-home'),TEMP=str(BASE/'tmp'),TMP=str(BASE/'tmp'))
ENV['PATH']=ENV['JAVA_HOME']+'/bin;'+ENV['PATH']
def sha(p):
 h=hashlib.sha256()
 with pathlib.Path(p).open('rb') as f:
  for b in iter(lambda:f.read(4*1024*1024),b''):h.update(b)
 return h.hexdigest()
def save(name,value):
 with (OUT/name).open('x',encoding='utf-8') as f:json.dump(value,f,ensure_ascii=False,indent=2)
def run(name,args,stdin=None,timeout=90):
 r=subprocess.run(args,input=stdin,capture_output=True,env=ENV,timeout=timeout)
 for label,b in [('stdout',r.stdout),('stderr',r.stderr)]:
  with (OUT/f'{name}.{label}.txt').open('xb') as f:f.write(b)
 save(name+'.command.json',dict(command=args,exit_code=r.returncode,at=datetime.datetime.now(datetime.timezone.utc).isoformat(),stdout_bytes=len(r.stdout),stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stderr_bytes=len(r.stderr),stderr_sha256=hashlib.sha256(r.stderr).hexdigest()))
 assert r.returncode==0,(name,r.returncode,r.stderr[-1500:])
 return r.stdout.decode('utf-8-sig')
def safe(path):
 p=pathlib.Path(path).absolute()
 assert p.resolve().is_relative_to(BASE.resolve())
 for a in (p,*p.parents):
  if a.exists():assert not a.is_symlink() and not(a.stat().st_file_attributes&0x400),a
 return p

def fields(text):
 result={}
 for line in text.splitlines():
  if '=' in line and not line.lstrip().startswith('#'):
   key,value=line.split('=',1);key=key.strip();value=value.strip()
   assert key not in result,('duplicate key',key)
   result[key]=value
 return result
if __name__=='__main__':
 action=sys.argv[1];assert action in ('create','start')
 if action=='create':
  assert not BASE.exists(),'New root already exists; do not overwrite'
  safe(BASE)
  OUT.mkdir(exist_ok=False)
  # Environment roots are new; no historical AVD config is opened.
  resource=json.loads(subprocess.check_output(['powershell','-NoProfile','-Command',r"[ordered]@{memory=(Get-CimInstance Win32_OperatingSystem|Select-Object FreePhysicalMemory,FreeVirtualMemory);emulators=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match '^(emulator|qemu-system.*)\.exe$'}|Select-Object Name,ProcessId,ParentProcessId);ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705 -ErrorAction SilentlyContinue|Select-Object LocalPort,OwningProcess)}|ConvertTo-Json -Depth 5"]))
  save('resource-before.json',resource)
  assert resource['memory']['FreePhysicalMemory']>3*1024*1024,'Need 3GiB free for 2GiB guest plus headroom'
  assert not resource['emulators'] and not resource['ports'],'No second emulator or occupied target port allowed'
  assert not (pathlib.Path.home()/'.android/avd'/f'{NAME}.ini').exists(),'Name collision'
  image=SDK/'system-images/android-36/google_apis/x86_64';props=fields((image/'source.properties').read_text())
  for k,v in {'AndroidVersion.ApiLevel':'36','SystemImage.Abi':'x86_64','SystemImage.TagId':'google_apis','Pkg.Revision':'7'}.items():assert props[k]==v
  save('tools-image.json',dict(sdk=str(SDK),image=str(image),files={n:dict(bytes=(image/n).stat().st_size,sha256=sha(image/n)) for n in ('source.properties','package.xml','kernel-ranchu','ramdisk.img','system.img','vendor.img')},emulator_sha256=sha(SDK/'emulator/emulator.exe'),environment={k:ENV[k] for k in ('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME','TEMP','TMP')},removed=['ANDROID_SDK_HOME','ANDROID_PREFS_ROOT']))
  BASE.mkdir()
  for n in ('android-user','avd-registration','emulator-home','runtime-data','snapshots','logs','tmp'):(BASE/n).mkdir()
  target=BASE/(NAME+'.avd');save('create-claim.json',dict(name=NAME,serial=SERIAL,path=str(target),attempt=1))
  run('create',[str(SDK/'cmdline-tools/latest/bin/avdmanager.bat'),'create','avd','--name',NAME,'--package','system-images;android-36;google_apis;x86_64','--path',str(target),'--device','pixel_5'],stdin=b'no\n',timeout=180)
  registration=BASE/'avd-registration'/(NAME+'.ini');registered=fields(registration.read_text())
  assert registered['target']=='android-36' and safe(registered['path']).resolve()==target.resolve()
  config=safe(target/'config.ini');values={}
  for l in config.read_text().splitlines():
   if '=' in l:
    k,v=l.split('=',1);assert k not in values;values[k]=v
  assert values['abi.type']=='x86_64' and values['tag.id']=='google_apis'
  assert values['image.sysdir.1'].replace('\\','/').rstrip('/')=='system-images/android-36/google_apis/x86_64'
  values.update({'hw.ramSize':'2048','hw.cpu.ncore':'2','disk.dataPartition.size':'2G'})
  config.write_text(''.join(k+'='+v+'\n' for k,v in values.items()),encoding='utf-8')
  save('config-proof.json',dict(name=NAME,serial=SERIAL,registration=str(registration),registration_sha256=sha(registration),avd_directory=str(target),config=str(config),config_sha256=sha(config),base=str(BASE),ram_mb=2048,cpu=2))
  (OUT/'generated-config.ini').write_bytes(config.read_bytes());(OUT/'registration.ini').write_bytes(registration.read_bytes())
  print('PASS: fresh isolated M20 AVD created',flush=True)
 else:
  proof=json.loads((OUT/'config-proof.json').read_text(encoding='utf-8'));assert sha(proof['config'])==proof['config_sha256'] and sha(proof['registration'])==proof['registration_sha256']
  for p in (BASE,BASE/(NAME+'.avd')/'config.ini',BASE/'avd-registration'/(NAME+'.ini'),BASE/'runtime-data',BASE/'snapshots',BASE/'logs'):safe(p)
  resource=json.loads(subprocess.check_output(['powershell','-NoProfile','-Command',r"[ordered]@{memory=(Get-CimInstance Win32_OperatingSystem|Select-Object FreePhysicalMemory,FreeVirtualMemory);emulators=@(Get-CimInstance Win32_Process|Where-Object {$_.Name -match '^(emulator|qemu-system.*)\.exe$'}|Select-Object Name,ProcessId,ParentProcessId);ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705 -ErrorAction SilentlyContinue|Select-Object LocalPort,OwningProcess)}|ConvertTo-Json -Depth 5"]))
  save('resource-before-start.json',resource)
  assert resource['memory']['FreePhysicalMemory']>3*1024*1024 and not resource['emulators'] and not resource['ports'],'Start requires free memory, no other emulator and free ports'
  args=['-avd',NAME,'-port',str(PORT),'-datadir',str(BASE/'runtime-data'),'-cache',str(BASE/'runtime-data/cache.img'),'-snapstorage',str(BASE/'snapshots/snapshots.img'),'-no-snapshot','-no-window','-no-boot-anim','-no-audio','-gpu','swiftshader','-memory','2048','-cores','2']
  save('start-claim.json',dict(name=NAME,serial=SERIAL,args=args,attempt=1))
  quoted=','.join("'"+x+"'" for x in args)
  ps=f"$m20process=Start-Process -FilePath '{SDK/'emulator/emulator.exe'}' -ArgumentList @({quoted}) -WindowStyle Hidden -PassThru -RedirectStandardOutput '{BASE/'logs/emulator.stdout.txt'}' -RedirectStandardError '{BASE/'logs/emulator.stderr.txt'}'; $m20process.Id"
  pid=int(run('start',['powershell','-NoProfile','-Command',ps]).strip());save('launcher.json',dict(pid=pid,name=NAME,serial=SERIAL));print('M20 launcher PID',pid,flush=True)
