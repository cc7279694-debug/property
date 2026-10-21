import datetime, hashlib, json, os, pathlib, re, subprocess
ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
SDK = pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
ADB = str(SDK/'platform-tools/adb.exe')
OUT = ROOT/'docs/evidence/m18-android/device-end'
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
def adb(name, *args): return run(name,[ADB,'-s','emulator-5692',*args])
# Fresh read-only host gate before the first device update; original process identity only.
ps = "Get-CimInstance Win32_Process -Filter 'ProcessId=5688 OR ProcessId=34920' | ForEach-Object { [pscustomobject]@{ProcessId=$_.ProcessId;ParentProcessId=$_.ParentProcessId;CreationDate=$_.CreationDate.ToString('o');ExecutablePath=$_.ExecutablePath;CommandLine=$_.CommandLine} } | ConvertTo-Json -Depth 4"
observed=json.loads(run('host-instance',['powershell','-NoProfile','-Command',ps]))
original=json.loads((ROOT/'docs/evidence/m18-android/device/boot-proof.json').read_text(encoding='utf-8'))['host_processes']
assert len(observed)==len(original)==2
for old in original:
    current=next(x for x in observed if x['ProcessId']==old['ProcessId'])
    assert datetime.datetime.fromisoformat(current['CreationDate'])==datetime.datetime.fromisoformat(old['CreationDate'])
    for field in ['ProcessId','ParentProcessId','ExecutablePath','CommandLine']:
        assert current[field]==old[field]
proof=json.loads((ROOT/'docs/evidence/m18-android/device/host-instance-proof.json').read_text(encoding='utf-8'))
assert hashlib.sha256(pathlib.Path(proof['hardware']).read_bytes()).hexdigest()==proof['hardware_sha256']
assert adb('state','get-state').strip() == 'device'
assert adb('boot','shell','getprop sys.boot_completed').strip() == '1'
assert adb('api','shell','getprop ro.build.version.sdk').strip() == '36'
assert adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip() == 'f9583e05-77d0-4308-9ef8-7b7b5656ee7e'
assert adb('avd','emu','avd','name').splitlines()[0] == 'Lithic_M18_API36_20261021'
assert adb('before-no-backup','shell','run-as','com.cooper.lithic.debug','ls','-ln','no_backup').strip()=='total 0'

for item in json.loads((ROOT/'docs/evidence/m18-android/install-driver-fix/artifacts.json').read_text(encoding='utf-8'))['applications']:
    name, package, expected = item['label'], item['package'], item['sha256']
    paths=adb(name+'-path','shell','pm','path',package).strip().splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/')
    assert adb(name+'-sha','shell','sha256sum',paths[0][8:]).split()[0]==expected
    identity=adb(name+'-uid','shell','cmd','package','list','packages','-U',package)
    assert ('package:'+package+' uid:'+('10216' if name=='app' else '10217')) in identity.splitlines()
assert adb('abi','shell','getprop ro.product.cpu.abi').strip()=='x86_64'
settings=[adb(name,'shell',command).strip() for name,command in [
    ('font','settings get system font_scale'),('density','wm density'),('size','wm size'),
    ('rotation','settings get system user_rotation'),('auto-rotation','settings get system accelerometer_rotation')]]
previous=[(ROOT/f'docs/evidence/m18-android/install-driver-fix/{name}.stdout.txt').read_text(encoding='utf-8').strip() for name in ('font','density','size','rotation')]
settings=settings[:4]
assert settings==previous
ports=json.loads(run('target-ports',['powershell','-NoProfile','-Command',"Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in @(5692,5693) } | Select-Object LocalAddress,LocalPort,OwningProcess | ConvertTo-Json"]))
assert {row['LocalPort'] for row in ports}=={5692,5693} and all(row['OwningProcess']==34920 for row in ports)
adb('target-processes','shell',"ps -A -o PID,UID,NAME | awk 'NR == 1 || $3 == \"com.cooper.lithic.debug\" || $3 == \"com.cooper.lithic.debug.test\"'")
(OUT/'summary.json').write_text(json.dumps(dict(status='PASS',serial='emulator-5692',avd='Lithic_M18_API36_20261021',
    original_instance_and_boot=True,apk_and_uid_matched=True,no_backup_empty=True,settings_match_before_final_tests=settings==previous,
    avd_left_online=True,private_ledger_or_image_bytes_read=False,private_byte_comparison='NOT_RUN'),indent=2),encoding='utf-8')
print('PASS: original instance retained online, final APK/UID matched, no_backup empty and settings unchanged')
