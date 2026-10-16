from capture_device import shell,dump,O,P
from pathlib import Path
import sys,json,re
name=sys.argv[1];root=dump('manual-proof-'+name)
nodes=list(root.iter('node'))
ime=[{k:n.get(k) for k in ('package','class','text','content-desc','bounds')} for n in nodes if (n.get('package') or '').startswith('com.google.android.inputmethod') and (n.get('text') or n.get('content-desc'))]
inp=shell('dumpsys','input_method');window=shell('dumpsys','window')
item={'name':name,'observation':sys.argv[2:] or [],'guestUtc':shell('date','-u','+%Y-%m-%dT%H:%M:%SZ'),'fontScale':shell('settings','get','system','font_scale'),'density':shell('wm','density'),'size':shell('wm','size'),'night':shell('cmd','uimode','night'),'activityFocus':[l.strip() for l in window.splitlines() if 'mCurrentFocus=' in l or 'mFocusedApp=' in l],'imeDiagnosticLines':[l.strip() for l in inp.splitlines() if any(k in l for k in ('mInputShown=','mShowRequested=','mCurId='))],'imeInsets':[l.strip() for l in window.splitlines() if 'type=ime' in l and ('mVisible=' in l or 'frame=' in l.lower())],'visibleImeUiNodes':ime,'ownVisibleText':[n.get('text') for n in nodes if n.get('package')==P and n.get('text')],'nativeImeClaimRule':'Only assert actually visible IME when original screenshot visibly contains software keys and matching package nodes; dumpsys alone is insufficient.'}
p=O/'manual-observations.json';data=json.loads(p.read_text(encoding='utf8')) if p.exists() else [];data.append(item);p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'name':name,'imeNodes':len(ime),'ownText':item['ownVisibleText'],'imeLines':item['imeDiagnosticLines']},ensure_ascii=True))
