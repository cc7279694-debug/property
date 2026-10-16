from capture_device import shell,O,S
import json,time
from pathlib import Path
R=Path.cwd();D=R/'docs/evidence/ui-c1/stage2/task8'
network=shell('dumpsys','connectivity')
default=[l.strip() for l in network.splitlines() if 'Active default network:' in l]
assert default==['Active default network: none'],default
assert shell('settings','get','global','airplane_mode_on')=='1' and shell('settings','get','global','wifi_on')=='0'
p=shell('pm','path','com.cooper.lithic.debug').split('package:')[1].splitlines()[0]
proof={'device':S,'api':shell('getprop','ro.build.version.sdk'),'activeDefaultNetwork':default,'airplaneMode':'1','wifiOn':'0','installedApkSha256':shell('sha256sum',p).split()[0],'guestUtc':shell('date','-u','+%Y-%m-%dT%H:%M:%SZ'),'observedJourneys':'Final normal/pressure MainActivity captures, cold force-stop/start, read/scroll/list/history/cover and original form validation/back/cancel were performed with airplane mode1 and wifi0. Full unfiltered Room/VM test journeys also ran in this offline state. No real QA payment/save/delete was confirmed.'}
(D/'offline-runtime.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
before=json.loads((O/'device-settings-before.json').read_text(encoding='utf8'))
for k,v in before.items():
 if '/' not in k:continue
 ns,key=k.split('/')
 if (ns,key) in [('global','airplane_mode_on')]:
  shell('cmd','connectivity','airplane-mode','enable' if v=='1' else 'disable')
 elif shell('settings','get',ns,key)!=v:
  if v=='null':shell('settings','delete',ns,key)
  else:shell('settings','put',ns,key,v)
shell('cmd','uimode','night','no')
shell('wm','density','reset')
shell('svc','wifi','enable' if before['wifi']=='1' else 'disable')
time.sleep(3)
print('Restored baseline settings; exact comparison is a separate final verification.')
