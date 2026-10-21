import hashlib, json, pathlib, re, struct, subprocess
ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
ADB='C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'
result=ROOT/'docs/evidence/m18-android/android-final/M18TotalCostTrendJourneyTest.result.json'
assert json.loads(result.read_text(encoding='utf-8'))['status']=='PASS'
text=result.with_name('M18TotalCostTrendJourneyTest.stdout.txt').read_text(encoding='utf-8')
paths=sorted(set(re.findall(r'M18_COMPONENT_ACTIVITY_SCREENSHOT=(/[^\r\n]+\.png)',text)))
assert len(paths)==10, paths
out=ROOT/'docs/evidence/m18-android/screenshots';out.mkdir(exist_ok=True); assert not any(out.iterdir())
directory_cmd=[ADB,'-s','emulator-5692','shell','run-as','com.cooper.lithic.debug','stat','-c','%u:%F','/data/data/com.cooper.lithic.debug/files/m18-evidence']
directory=subprocess.run(directory_cmd,capture_output=True,timeout=30)
assert directory.returncode==0 and not directory.stderr and directory.stdout.strip()==b'10216:directory'
records=[]
for source in paths:
    assert source.startswith('/data/data/com.cooper.lithic.debug/files/m18-evidence/')
    name=pathlib.PurePosixPath(source).name
    match=re.fullmatch(r'(\d{2}[a-z]?-[a-z0-9-]+)-([a-f0-9-]{36})\.png',name)
    assert match and '..' not in source
    cmd=[ADB,'-s','emulator-5692','exec-out','run-as','com.cooper.lithic.debug','cat',source]
    r=subprocess.run(cmd,capture_output=True,timeout=30)
    assert r.returncode==0 and not r.stderr and r.stdout.startswith(b'\x89PNG\r\n\x1a\n')
    label=match.group(1)+'.png'
    with (out/label).open('xb') as f: f.write(r.stdout)
    records.append(dict(file=label,source=source,command=cmd,exit_code=r.returncode,bytes=len(r.stdout),sha256=hashlib.sha256(r.stdout).hexdigest(),size=struct.unpack('>II',r.stdout[16:24])))
with (out/'sources.json').open('x',encoding='utf-8') as f: json.dump(records,f,indent=2)
print('Captured',len(records),'real isolated ComponentActivity images; no MainActivity or system font modification')

with (out/'directory-identity.json').open('x',encoding='utf-8') as f: json.dump(dict(command=directory_cmd,exit_code=directory.returncode,stdout=directory.stdout.decode('utf-8'),sha256=hashlib.sha256(directory.stdout).hexdigest()),f,indent=2)
