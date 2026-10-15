import sys,re,json,time
from capture_device import adb,shell,dump,O
cmd=sys.argv[1]
if cmd in ('text','desc'):
 key='text' if cmd=='text' else 'content-desc';value=sys.argv[2];root=dump('manual-'+str(time.time_ns()))
 nodes=[n for n in root.iter('node') if n.get(key)==value and n.get('enabled')=='true'];assert nodes,(key,value)
 n=nodes[int(sys.argv[3]) if len(sys.argv)>3 else 0];x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')));assert x2>x1 and y2>y1
 shell('input','tap',str((x1+x2)//2),str((y1+y2)//2));print(key,value,n.get('bounds'))
elif cmd=='swipe':shell('input','swipe','540',sys.argv[2],'540',sys.argv[3],'450')
elif cmd=='restart':
 shell('am','force-stop','com.cooper.lithic.debug');print(shell('am','start','-W','-n','com.cooper.lithic.debug/com.cooper.lithic.app.MainActivity'))
elif cmd=='back':shell('input','keyevent','KEYCODE_BACK')
else:raise ValueError(cmd)
