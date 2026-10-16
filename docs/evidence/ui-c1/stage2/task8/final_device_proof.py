from pathlib import Path
import json, hashlib, subprocess
R=Path.cwd();O=R/'build/ui-c1-stage2';D=R/'docs/evidence/ui-c1/stage2'
A='C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe';S='emulator-5596';P='com.cooper.lithic.debug'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(b):return hashlib.sha256(b).hexdigest()
def adb(*a):return subprocess.check_output([A,'-s',S,*a],timeout=30)
def shell(*a):return adb('shell',*a).decode('utf8','replace').strip()
before=read(O/'ledger-before.json');after=read(O/'ledger-after.json')
assert before['facts']==after['facts'],'Any fact row changed or added'
assert before['storage']==after['storage'],'Storage generation/settings changed'
settings={}
for k in read(O/'device-settings-before.json'):
 if '/' in k:settings[k]=shell('settings','get',*k.split('/'))
 elif k in ('size','density'):settings[k]=shell('wm',k)
 elif k=='night':settings[k]=shell('cmd','uimode','night')
 elif k=='wifi':settings[k]=shell('settings','get','global','wifi_on')
assert settings==read(O/'device-settings-before.json'),settings
(O/'device-settings-after.json').write_text(json.dumps(settings,indent=2)+'\n',encoding='utf8')
root='no_backup/generations/'+after['storage']['pointer']['activeId']+'/'
media=read(O/'private-media-before.json')
for m in media:
 b=adb('exec-out','run-as',P,'cat',root+m['relativePath'])
 assert sha(b)==m['sha256'] and len(b)==m['bytes']
avd=read(O/'avd-before.json')
for v in avd:assert sha(Path(v['path']).read_bytes())==v['sha256']
result={'result':'PASS','device':S,'avd':'Lithic_M01_API_36','schemaVersion':after['schemaVersion'],'roomIdentity':after['roomIdentity'],'tableCounts':{t:len(v) for t,v in after['facts'].items()},'allSevenTablesExactRowEquality':True,'noNewActiveQaRows':True,'factsSha256':sha(json.dumps(after['facts'],sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()),'sameGenerationPointerReadyAndSettings':True,'integrityCheck':after['integrityCheck'],'foreignKeyCheck':after['foreignKeyCheck'],'privateMedia':media,'avdDescriptorAndConfigUnchanged':avd,'deviceSettingsRestoredExactly':settings,'installedApkSha256':after['installedApkSha256'],'scope':'Owned existing API36 emulator only; read-only DB/WAL copies after controlled force-stop; raw database/private originals remain ignored locally. No physical/API26/OEM/Release/LMK or restore claim.'}
(D/'device-preservation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps(result,ensure_ascii=True,indent=2))
