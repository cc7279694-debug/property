"""Read-only end evidence, then one authorized console exit of this new M20 AVD."""
import ctypes,datetime,hashlib,json,os,pathlib,subprocess,time,sys,re
from environment import BASE,ROOT,SDK,NAME,SERIAL
assert sys.argv[1:] in ([],['final'])
OUT=ROOT/('docs/evidence/m20-android/end-final' if sys.argv[1:] else 'docs/evidence/m20-android/end');OUT.mkdir(exist_ok=False)
commands=[]
def save(name,value):
 with (OUT/name).open('x',encoding='utf-8') as f:json.dump(value,f,ensure_ascii=False,indent=2)
def run(name,args,timeout=30):
 r=subprocess.run(args,capture_output=True,timeout=timeout)
 for label,data in [('stdout',r.stdout),('stderr',r.stderr)]:
  (OUT/(name+'.'+label+'.txt')).write_bytes(data)
 commands.append(dict(name=name,command=args,exit_code=r.returncode,stdout_bytes=len(r.stdout),stdout_sha256=hashlib.sha256(r.stdout).hexdigest(),stderr_bytes=len(r.stderr),stderr_sha256=hashlib.sha256(r.stderr).hexdigest()))
 (OUT/'commands.json').write_text(json.dumps(commands,indent=2))
 assert r.returncode==0 and not r.stderr,(name,r.returncode,r.stderr)
 return r.stdout.decode('utf-8-sig')
def adb(name,*args):return run(name,[str(SDK/'platform-tools/adb.exe'),'-s',SERIAL,*args])
proof=json.loads((ROOT/'docs/evidence/m20-android/device/installed-platform-correction-proof.json').read_text())
original=json.loads((ROOT/'docs/evidence/m20-android/device/host-instance-proof.json').read_text())['processes']
host=json.loads(run('host-before-exit',['powershell','-NoProfile','-Command',"[ordered]@{processes=@(Get-CimInstance Win32_Process -Filter 'ProcessId=2100 OR ProcessId=9588'|ForEach-Object {[ordered]@{pid=$_.ProcessId;parent=$_.ParentProcessId;created=$_.CreationDate.ToString('o');path=$_.ExecutablePath;command=$_.CommandLine}});memory=@(Get-Process -Id 2100,9588|Select-Object Id,WorkingSet64,PeakWorkingSet64,PrivateMemorySize64,CPU);ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705|Select-Object LocalPort,OwningProcess)}|ConvertTo-Json -Depth 5"]))
assert sorted(host['processes'],key=lambda p:p['pid'])==sorted(original,key=lambda p:p['pid'])
assert {p['LocalPort'] for p in host['ports']}=={5704,5705} and all(p['OwningProcess']==9588 for p in host['ports'])
assert adb('boot','shell','cat /proc/sys/kernel/random/boot_id').strip()==proof['boot_id']
assert adb('name','emu','avd','name').splitlines()[0]==NAME
for label in ('app','test'):
 item=proof[label]
 assert adb(label+'-sha','shell','sha256sum',item['installed_path']).split()[0]==item['sha256']
 uid_rows=adb(label+'-uid','shell','cmd','package','list','packages','-U',item['package']).splitlines()
 matching=[re.fullmatch('package:'+re.escape(item['package'])+r' uid:(\d+)',line) for line in uid_rows]
 uids=[int(value.group(1)) for value in matching if value]
 assert uids==[item['uid']]
adb('synthetic-top-level','shell','run-as','com.cooper.lithic.debug','ls','-ln','no_backup')
assert adb('default-ledger-absent','shell',"run-as com.cooper.lithic.debug sh -c 'test ! -e no_backup/active-generation && test ! -e no_backup/generations && echo DEFAULT_LEDGER_ABSENT'").strip()=='DEFAULT_LEDGER_ABSENT'
assert adb('host-process-ended','shell','pidof com.cooper.lithic.debug || true').strip()==''
for name,query in [('font','settings get system font_scale'),('size','wm size'),('density','wm density'),('rotation','settings get system user_rotation')]:adb('final-'+name,'shell',query)
adb('cleanup-log','logcat','-d','-v','threadtime','LITHIC_M20_CLEANUP:I','*:S')
save('exit-claim.json',dict(serial=SERIAL,avd=NAME,processes=original,attempt=1,action='authorized emulator console exit; retain data and all UNKNOWN evidence'))
answer=adb('console-exit','emu','kill');assert 'OK' in answer
observations=[]
for index in range(13):
 value=json.loads(run(f'host-after-{index:02}',['powershell','-NoProfile','-Command',"[ordered]@{processes=@(Get-CimInstance Win32_Process -Filter 'ProcessId=2100 OR ProcessId=9588'|Select-Object ProcessId,ParentProcessId);ports=@(Get-NetTCPConnection -State Listen -LocalPort 5704,5705 -ErrorAction SilentlyContinue|Select-Object LocalPort,OwningProcess);memory=(Get-CimInstance Win32_OperatingSystem|Select-Object FreePhysicalMemory,FreeVirtualMemory)}|ConvertTo-Json -Depth 4"]))
 observations.append(value)
 if not value['processes'] and not value['ports']:break
 time.sleep(5)
assert not value['processes'] and not value['ports'],'Shutdown UNKNOWN; do not kill or retry'
save('exit-proof.json',dict(status='PASS',serial=SERIAL,avd=NAME,observations=observations,other_device_commands=0,avd_retained=BASE.is_dir(),retained_platform_run='eb140516-9807-45be-b01b-781a9b76f6c0',retained_platform_explicit_owner_close='UNKNOWN; not inferred from emulator exit'))
kernel=ctypes.WinDLL('kernel32',use_last_error=True);fn=kernel.GetCompressedFileSizeW;fn.argtypes=[ctypes.c_wchar_p,ctypes.POINTER(ctypes.c_uint32)];fn.restype=ctypes.c_uint32
logical=allocated=count=0
for directory,dirs,files in os.walk(BASE,followlinks=False):
 for name in dirs+files:
  p=pathlib.Path(directory)/name;assert not p.is_symlink() and not(p.stat().st_file_attributes&0x400)
 for name in files:
  p=pathlib.Path(directory)/name;high=ctypes.c_uint32();ctypes.set_last_error(0);low=fn(str(p),ctypes.byref(high));assert low!=0xffffffff or ctypes.get_last_error()==0
  allocated+=(high.value<<32)+low;logical+=p.stat().st_size;count+=1
save('resources.json',dict(base=str(BASE),new_directory_initial_bytes=0,files=count,logical_file_bytes=logical,ntfs_allocated_file_bytes=allocated,allocation_method='GetCompressedFileSizeW after confirmed emulator exit; directory metadata not included',process_lifetime_peak_working_sets=host['memory'],combined_simultaneous_process_peak='NOT_CAPTURED',other_system_disk_overhead='NOT_CAPTURED',guest_ram_mb=2048,guest_cores=2))
print('PASS: only M20 emulator exited; AVD and retained UNKNOWN preserved',allocated,'allocated bytes')
