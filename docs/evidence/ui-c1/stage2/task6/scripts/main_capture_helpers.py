from pathlib import Path
import subprocess,sys,time,re
from capture_device import shell,dump
R=Path.cwd();O=R/'build/ui-c1-stage2'
def rect(n):return tuple(map(int,re.findall(r'\d+',n.get('bounds'))))
def tap(n):
 x1,y1,x2,y2=rect(n);assert x2>x1 and y2>y1;shell('input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.8)
def node(value,key='text',scroll=False):
 for attempt in range(9 if scroll else 1):
  ns=[n for n in dump('driver-'+str(time.time_ns())).iter('node') if n.get(key)==value and n.get('enabled')=='true']
  if ns:return ns[0]
  shell('input','swipe','540','1430','540','650','400');time.sleep(.5)
 raise AssertionError((key,value))
def text(value,scroll=False):tap(node(value,scroll=scroll))
def desc(value):tap(node(value,'content-desc'))
def restart():
 shell('am','force-stop','com.cooper.lithic.debug');shell('am','start','-W','-n','com.cooper.lithic.debug/com.cooper.lithic.app.MainActivity');time.sleep(1)
def open_list(title):
 for attempt in range(9):
  root=dump('driver-list-'+str(time.time_ns()));head=[n for n in root.iter('node') if n.get('text')==title]
  if head:
   h=rect(head[0]);matches=[n for n in root.iter('node') if n.get('text')=='查看全部' and abs(rect(n)[1]-h[1])<90 and rect(n)[3]<1750]
   if matches:tap(matches[0]);return
  shell('input','swipe','540','1450','540','650','400');time.sleep(.5)
 raise AssertionError('No real list action '+title)
def capture(name):
 dump('before-'+name)
 subprocess.run([sys.executable,str(O/'capture_device.py'),'capture',name+'.png'],check=True,stdout=subprocess.DEVNULL)
 print('CAPTURED '+name,flush=True)
if __name__=='__main__':
 phase=sys.argv[1]
 for theme in ('light','dark'):
  shell('cmd','uimode','night','no' if theme=='light' else 'yes');restart()
  text('回看');capture(phase+'-review-'+theme)
  text('历史档案');capture(phase+'-project-history-'+theme)
  desc('返回回看');text('已停止订阅');capture(phase+'-subscription-history-'+theme)
  desc('返回回看');text('设置');capture(phase+'-settings-'+theme)
  restart();open_list('我的订阅');capture(phase+'-subscription-list-'+theme)
  text('M10 QA Subscription',scroll=True);capture(phase+'-subscription-detail-'+theme)
 shell('cmd','uimode','night','no');restart()
