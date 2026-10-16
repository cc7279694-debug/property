from main_capture_helpers import *
from capture_device import P
window=shell('dumpsys','window')
ime_tops=[int(m[1]) for m in re.finditer(r'type=ime frame=\[0,(\d+)\][^\n]* visible=true',window)]
assert ime_tops,'Need actual visible native IME boundary'
ime_top=min(ime_tops)
found=None
for attempt in range(18):
 root=dump('native-scroll-save-actual-ime-'+str(attempt))
 found=next((n for n in root.iter('node') if n.get('text')=='保存项目' and n.get('enabled')=='true' and rect(n)[3]<=ime_top and rect(n)[1]>80),None)
 if found is not None:break
 # Stay inside the actual app region above the visible native keyboard.
 shell('input','swipe','540','900','540','350','450');time.sleep(.4)
assert found is not None,'Save did not become visible above IME in bounded18 content swipes'
tap(found)
capture('final-native-ime-submit-error-light')
print('Save target '+found.get('bounds')+'; actual IME top '+str(ime_top),flush=True)
