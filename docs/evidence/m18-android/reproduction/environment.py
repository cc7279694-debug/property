"""One fresh M18 AVD only. No download, legacy AVD, reset or shutdown entry point."""
import datetime, hashlib, json, os, pathlib, subprocess, sys

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
SDK = pathlib.Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk')
BASE = pathlib.Path('E:/CODEX/.codex/research-env/lithic-m18-android-20261021')
OUT = ROOT/'docs/evidence/m18-android/device'
NAME = 'Lithic_M18_API36_20261021'
SERIAL = 'emulator-5692'
ENV = os.environ.copy()
for key in ('ANDROID_SDK_HOME', 'ANDROID_PREFS_ROOT'): ENV.pop(key, None)
ENV.update(JAVA_HOME='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1',
    ANDROID_HOME=str(SDK), ANDROID_SDK_ROOT=str(SDK), ANDROID_USER_HOME=str(BASE/'android-user'),
    ANDROID_AVD_HOME=str(BASE/'avd-registration'), ANDROID_EMULATOR_HOME=str(BASE/'emulator-home'))
ENV['PATH'] = ENV['JAVA_HOME']+'/bin;'+ENV['PATH']

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(4*1024*1024), b''): h.update(block)
    return h.hexdigest()

def save(name, value):
    with (OUT/name).open('x', encoding='utf-8') as f: json.dump(value, f, ensure_ascii=False, indent=2)

def run(name, command, *, input=None, timeout=90):
    result = subprocess.run(command, input=input, capture_output=True, env=ENV, timeout=timeout)
    for suffix, data in [('stdout', result.stdout), ('stderr', result.stderr)]:
        with (OUT/f'{name}.{suffix}.txt').open('xb') as f: f.write(data)
    save(name+'.command.json', dict(command=command, exit_code=result.returncode,
        at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        stdout_bytes=len(result.stdout),stdout_sha256=hashlib.sha256(result.stdout).hexdigest(),
        stderr_bytes=len(result.stderr),stderr_sha256=hashlib.sha256(result.stderr).hexdigest()))
    assert result.returncode == 0, (name, result.returncode)
    return result.stdout.decode('utf-8-sig')

def child(path):
    path = path.resolve(strict=True)
    assert path.is_relative_to(BASE.resolve()) and not path.is_symlink()
    for part in (path, *path.parents):
        if part == BASE.parent: break
        assert not part.is_symlink() and not (part.stat().st_file_attributes & 0x400), part
    return path

if sys.argv[1] == 'create':
    assert not BASE.exists(), 'Never reuse a prior environment or marker'
    OUT.mkdir(exist_ok=False)
    resources=json.loads(run('resources',['powershell','-NoProfile','-Command',
        "Get-CimInstance Win32_OperatingSystem | Select-Object FreePhysicalMemory,TotalVisibleMemorySize | ConvertTo-Json"]))
    assert resources['FreePhysicalMemory'] > 3*1024*1024, 'Need at least 3 GiB free before launch'
    ports=run('ports-before',['powershell','-NoProfile','-Command',
        "@(Get-NetTCPConnection -State Listen -LocalPort 5692,5693 -ErrorAction SilentlyContinue | Select-Object LocalPort,OwningProcess) | ConvertTo-Json"])
    assert not ports.strip() or json.loads(ports)==[], 'Fixed new ports are occupied'
    image=SDK/'system-images/android-36/google_apis/x86_64'
    props=(image/'source.properties').read_text(encoding='utf-8')
    for value in ('AndroidVersion.ApiLevel=36','SystemImage.Abi=x86_64','SystemImage.TagId=google_apis','Pkg.Revision=7'): assert value in props
    save('tools-image.json', dict(sdk=str(SDK), image=str(image), package='system-images;android-36;google_apis;x86_64',
        files={name:dict(bytes=(image/name).stat().st_size,sha256=sha(image/name)) for name in
            ('source.properties','package.xml','kernel-ranchu','ramdisk.img','system.img','vendor.img')},
        emulator_sha256=sha(SDK/'emulator/emulator.exe'),
        environment={k:ENV[k] for k in ('ANDROID_HOME','ANDROID_SDK_ROOT','ANDROID_USER_HOME','ANDROID_AVD_HOME','ANDROID_EMULATOR_HOME')},
        removed_environment=['ANDROID_SDK_HOME','ANDROID_PREFS_ROOT']))
    BASE.mkdir()
    for folder in ('android-user','avd-registration','emulator-home','runtime-data','snapshots','logs'): (BASE/folder).mkdir()
    target=BASE/(NAME+'.avd')
    save('create-claim.json',dict(avd=NAME,path=str(target),serial=SERIAL,attempt=1))
    command=[str(SDK/'cmdline-tools/latest/bin/avdmanager.bat'),'create','avd','--name',NAME,
        '--package','system-images;android-36;google_apis;x86_64','--path',str(target),'--device','pixel_5']
    run('create',command,input=b'no\n',timeout=180)
    registration=BASE/'avd-registration'/(NAME+'.ini')
    assert registration.is_file()
    fields=dict(line.split('=',1) for line in registration.read_text().splitlines() if '=' in line)
    actual=child(pathlib.Path(fields['path']))
    config=child(actual/'config.ini')
    rows=config.read_text().splitlines(); values={}
    for line in rows:
        if '=' in line:
            key,value=line.split('=',1); assert key not in values; values[key]=value
    assert values.get('abi.type')=='x86_64' and values.get('tag.id')=='google_apis'
    assert values.get('image.sysdir.1','').replace('\\','/').rstrip('/')=='system-images/android-36/google_apis/x86_64'
    assert fields.get('target')=='android-36'
    save('config-proof.json',dict(avd=NAME,serial=SERIAL,registration=str(registration),actual_avd=str(actual),
        config=str(config),registration_sha256=sha(registration),config_sha256=sha(config),
        snapshot_directory=str(BASE/'snapshots'),runtime_data=str(BASE/'runtime-data'),all_paths_inside_new_root=True))
    with (OUT/'generated-config.ini').open('xb') as f: f.write(config.read_bytes())
    with (OUT/'registration.ini').open('xb') as f: f.write(registration.read_bytes())
    print('PASS: one fresh official API36 AVD created and isolated')
elif sys.argv[1] == 'start':
    proof=json.loads((OUT/'config-proof.json').read_text())
    assert sha(pathlib.Path(proof['config']))==proof['config_sha256']
    args=['-avd',NAME,'-port','5692','-datadir',str(BASE/'runtime-data'),'-snapstorage',str(BASE/'snapshots/snapshots.img'),
        '-no-snapshot','-no-boot-anim','-no-audio','-no-window','-gpu','swiftshader','-memory','2048','-cores','2']
    save('start-claim.json',dict(avd=NAME,serial=SERIAL,arguments=args,attempt=1))
    quoted=','.join("'"+a+"'" for a in args)
    ps=f"$m18process=Start-Process -FilePath '{SDK/'emulator/emulator.exe'}' -ArgumentList @({quoted}) -WindowStyle Hidden -PassThru -RedirectStandardOutput '{BASE/'logs/emulator.stdout.txt'}' -RedirectStandardError '{BASE/'logs/emulator.stderr.txt'}'; $m18process.Id"
    pid=int(run('start',['powershell','-NoProfile','-Command',ps]).strip())
    save('launcher.json',dict(pid=pid,serial=SERIAL,avd=NAME))
    print('Started new isolated launcher PID',pid)
else:
    raise SystemExit('Only create/start are supported; no retry/cleanup/shutdown operation')
