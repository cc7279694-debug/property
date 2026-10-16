from pathlib import Path
import subprocess,sys,json,hashlib,time,re,xml.etree.ElementTree as ET
sys.stdout.reconfigure(encoding='utf-8',errors='replace')
R=Path.cwd(); O=R/'build/ui-c1-stage2'; A='C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'; S='emulator-5596'; P='com.cooper.lithic.debug'
def adb(*args):return subprocess.check_output([A,'-s',S,*args],timeout=30)
def shell(*args):return adb('shell',*args).decode('utf-8','replace').strip()
def dump(name='current'):
 for attempt in range(3):
  path='/data/local/tmp/ui-c1-'+str(time.time_ns())+'.xml'
  shell('uiautomator','dump',path)
  raw=adb('exec-out','cat',path)
  try:root=ET.fromstring(raw)
  except ET.ParseError:
   (O/(name+'-invalid-'+str(time.time_ns())+'.txt')).write_bytes(raw)
   if attempt==2:raise
   time.sleep(1)
   continue
  (O/(name+'.xml')).write_bytes(raw)
  return root
if __name__=='__main__':
 action=sys.argv[1]
 if action=='dump':
  root=dump(sys.argv[2] if len(sys.argv)>2 else 'current')
  print(json.dumps([{k:n.get(k) for k in ('text','content-desc','bounds','clickable','selected')} for n in root.iter('node') if n.get('text') or n.get('content-desc')],ensure_ascii=False,indent=2))
 elif action=='capture':
  name=sys.argv[2];out=R/'docs/evidence/ui-c1/stage2/screenshots'/name
  assert not out.exists();out.parent.mkdir(parents=True,exist_ok=True)
  time.sleep(1) # Allow the existing navigation transition to finish; no product timing/assertion changes.
  window=shell('dumpsys','window')
  focus=[line.strip() for line in window.splitlines() if 'mCurrentFocus=' in line or 'mFocusedApp=' in line]
  normalFocus=any('mCurrentFocus=' in line and P+'/' in line and 'MainActivity' in line for line in focus)
  popupProof=None
  if not normalFocus and '--popup' in sys.argv:
   current=next(line for line in focus if 'mCurrentFocus=' in line)
   wid=re.search(r'Window\{(\w+)',current)[1]
   match=re.search(r'Window #\d+ Window\{'+wid+r'[^\n]*\n([\s\S]*?)(?=  Window #|$)',window)
   assert match and 'Pop-Up Window' in current and any('mFocusedApp=' in line and P+'/' in line and 'MainActivity' in line for line in focus),focus
   block=match[0];owner=next(line.strip() for line in block.splitlines() if 'mOwnerUid=' in line)
   assert 'package='+P+' ' in owner and 'mParentWindow=Window{' in block and P+'/com.cooper.lithic.app.MainActivity}' in block,owner
   popupProof=owner
  assert normalFocus or popupProof,focus
  raw=adb('exec-out','screencap','-p');out.write_bytes(raw)
  package=shell('pm','path',P).split('package:')[1].splitlines()[0]
  proof={'file':out.relative_to(R).as_posix(),'sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw),'source':'unmodified adb screencap from installed production Debug MainActivity; existing synthetic QA ledger',
   'focusedWindow':focus,'popupOwner':popupProof,'device':S,'avd':shell('getprop','ro.boot.qemu.avd_name'),'api':shell('getprop','ro.build.version.sdk'),'fontScale':shell('settings','get','system','font_scale'),'size':shell('wm','size'),'density':shell('wm','density'),'night':shell('cmd','uimode','night'),'resumed':shell('dumpsys','activity','activities'), 'installedApkSha256':shell('sha256sum',package).split()[0], 'guestUtc':shell('date','-u','+%Y-%m-%dT%H:%M:%SZ'), 'networkSettings':{'airplaneMode':shell('settings','get','global','airplane_mode_on'),'wifiEnabled':shell('settings','get','global','wifi_on')}}
  # Keep complete dumpsys private; publish the actual focused MainActivity line only.
  focus=[line.strip() for line in proof['resumed'].splitlines() if 'ResumedActivity' in line or 'topResumedActivity' in line]
  proof['resumed']=focus;assert any(P+'/' in line and 'MainActivity' in line for line in focus)
  (O/(name+'.json')).write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  print(json.dumps(proof,ensure_ascii=False,indent=2))
