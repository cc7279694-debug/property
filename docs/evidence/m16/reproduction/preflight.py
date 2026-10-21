import datetime, hashlib, json, os, pathlib, re, subprocess
ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
SDK = pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
ADB = str(SDK/'platform-tools/adb.exe')
OUT = ROOT/'docs/evidence/m16/device-preflight'
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
# Fresh read-only host gate before the first device update; original process identity only.
ps = "Get-CimInstance Win32_Process -Filter 'ProcessId=20744 OR ProcessId=7356' | ForEach-Object { [pscustomobject]@{ProcessId=$_.ProcessId;ParentProcessId=$_.ParentProcessId;CreationDate=$_.CreationDate.ToString('o');ExecutablePath=$_.ExecutablePath;CommandLine=$_.CommandLine} } | ConvertTo-Json -Depth 4"
observed=json.loads(run('host-instance',['powershell','-NoProfile','-Command',ps]))
original=json.loads((ROOT/'docs/evidence/m15/device-availability.json').read_text(encoding='utf-8-sig'))['processes']
assert len(observed)==len(original)==2
for old in original:
    current=next(x for x in observed if x['ProcessId']==old['ProcessId'])
    assert datetime.datetime.fromisoformat(current['CreationDate'])==datetime.datetime.fromisoformat(old['CreationDate'])
    for field in ['ProcessId','ParentProcessId','ExecutablePath','CommandLine']:
        assert current[field]==old[field]
assert hashlib.sha256(pathlib.Path('E:/CODEX/.codex/research-env/lithic-m14-android-20261021/Lithic_M14_API36_20261021.avd/hardware-qemu.ini').read_bytes()).hexdigest()=='22ca609f4cc2ffa18bd9cf87560a206ea5afc98089ab1397937d610d6d6d9c34'
assert adb('state','get-state').strip() == 'device'
assert adb('boot','shell','getprop sys.boot_completed').strip() == '1'
assert adb('api','shell','getprop ro.build.version.sdk').strip() == '36'
assert adb('boot-id','shell','cat /proc/sys/kernel/random/boot_id').strip() == '0b481eff-e49d-48f9-a740-2d2e9d3ce0e1'
assert adb('avd','emu','avd','name').splitlines()[0] == 'Lithic_M14_API36_20261021'
assert adb('before-no-backup','shell','run-as','com.cooper.lithic.debug','ls','-ln','no_backup').strip()=='total 0'

for name, package, expected in [
('app','com.cooper.lithic.debug','12e48456bcb6be51c1f47ea71776fa03e4fbd9f99185c8aa2285ff3199f35d57'),
('test','com.cooper.lithic.debug.test','fc1774b8b0e113d9191699c12559d08b3c2cf8a6c69044f7cd5cc2d4e5dc677f')]:
    paths=adb(name+'-path','shell','pm','path',package).strip().splitlines()
    assert len(paths)==1 and paths[0].startswith('package:/data/app/')
    assert adb(name+'-sha','shell','sha256sum',paths[0][8:]).split()[0]==expected
    identity=adb(name+'-uid','shell','cmd','package','list','packages','-U',package)
    assert ('package:'+package+' uid:'+('10216' if name=='app' else '10217')) in identity.splitlines()
print('PASS: original authorized instance, boot, isolation and M15 APK identities matched')
