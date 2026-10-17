"""H2 final bounded ending observation. Fixed read-only calls, no launch/control entry."""
import hashlib,importlib.util,json,re,subprocess,sys,time
from pathlib import Path
sys.dont_write_bytecode=True
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('h2_ending_helpers',HERE/'normal_once.py');N=importlib.util.module_from_spec(spec);spec.loader.exec_module(N)
V,OUT=N.V,N.OUT

def main():
    N.need((OUT/'h2-blocked.json').exists(),'stopped H2 only');N.save('h2-ending-logs-attempt.json',{'atUtc':V.S.utc_now(),'scope':'FINAL_READ_ONLY_LOGS_AND_SCOPED_PROCESS_NO_MORE_EXPERIMENTS'})
    d=V.Bounded(OUT,'h2-ending-logs');real=subprocess.run;records=[]
    def observe(args,*a,**k):
        N.need(str(args[0])==str(V.S.ADB) and args[1:3]==['-s',V.S.SERIAL],'unexpected command');item={'startedAtUtc':V.S.utc_now(),'argv':args};t=time.monotonic()
        try:
            r=real(args,*a,**k);item.update(exitCode=r.returncode,timedOut=False,stdoutBytes=len(r.stdout),stdoutSHA256=N.digest(r.stdout),stderrBytes=len(r.stderr),stderrSHA256=N.digest(r.stderr));return r
        except subprocess.TimeoutExpired as e:item.update(exitCode=None,timedOut=True,stdoutBytes=len(e.stdout or b''),stdoutSHA256=N.digest(e.stdout or b''),stderrBytes=len(e.stderr or b''),stderrSHA256=N.digest(e.stderr or b''));raise
        finally:
            item['wallMs']=round((time.monotonic()-t)*1000,3);records.append(item)
            with (OUT/'h2-ending-log-command-integrity.jsonl').open('a',encoding='utf-8') as h:h.write(json.dumps(item)+'\n')
    try:
        subprocess.run=observe;cap=V.G.capture(d.adb('shell','ps','-A','-o','USER,PID,PPID,NAME',label='h2-ending-scoped-processes',private_metadata=True),'ps-user-pid-ppid-name-v1');lines=V.G.complete_text(cap,'ps-user-pid-ppid-name-v1').splitlines();N.need(lines[0].split()==['USER','PID','PPID','NAME'],'unknown ps header');scoped=[]
        for line in lines[1:]:
            f=line.split();N.need(len(f)==4,'unknown ps row');uid=V.G.resolve_ps_uid(f[0])
            if uid in (10218,10219) or f[3].startswith((V.S.PROBE,V.S.OUTSIDER)):scoped.append({'uid':uid,'pid':int(f[1]),'ppid':int(f[2]),'name':f[3]})
        try:V.G.process_rows(cap,{V.S.PROBE:10218,V.S.OUTSIDER:10219});state='PASS';reason=None
        except ValueError as e:state='BLOCKED';reason=str(e)
        N.save('h2-ending-scoped-processes.json',{'status':state,'reason':reason,'rows':scoped,'capture':{k:v for k,v in cap.items() if k!='stdout'},'unrelatedRowsArchived':False,'earlierBlockerUnchanged':True})
        start=N.load('normal-launch-issued.json')['deviceEpochStart'];end=N.epoch(d,'h2-ending-log-window-end');buffers={};events=[];pids={6423};captures={}
        for name in N.BUFFERS:
            r=d.adb('logcat','-d','-b',name,'-v','epoch',label='h2-ending-full-'+name,private_metadata=True);N.need(r.returncode==0 and not r.stderr and len(r.stdout)<=32*1024*1024,'log incomplete/oversize');text=r.stdout.decode('utf-8','strict');N.need(not text or text.endswith('\n'),'missing log newline');parsed=[];bad=0
            for line in text.splitlines():
                if not line.strip() or line.startswith('--------- '):continue
                m=N.LINE.fullmatch(line)
                if not m:bad+=1;continue
                stamp,pid,tid,level,tag,msg=m.groups();stamp=float(stamp);pid=int(pid);tag=tag.strip();parsed.append((stamp,pid,tag,msg,line))
                if float(start)<=stamp<=float(end) and tag=='am_proc_start':
                    match=re.match(r'\[0,(\d+),10218,'+re.escape(V.S.PROBE)+r'(?::jpeg_worker)?,',msg)
                    if match:pids.add(int(match[1]))
            captures[name]=(r,parsed,bad)
        for name,(r,parsed,bad) in captures.items():
            times=[x[0] for x in parsed];kept=[];foreign=0
            for stamp,pid,tag,msg,line in parsed:
                if not float(start)<=stamp<=float(end):continue
                related=(pid in pids and tag in ('LITHIC_R3','ActivityThread','AndroidRuntime','libc','DEBUG')) or (tag in ('ActivityManager','ActivityTaskManager','am_proc_start','am_proc_bound','am_proc_died','am_kill','wm_create_activity','wm_on_create_called','wm_destroy_activity','wm_on_destroy_called','Zygote','zygote','zygote64') and (V.S.PROBE in msg or any(re.search(r'(?<!\d)'+str(p)+r'(?!\d)',msg) for p in pids)))
                if not related:continue
                pkgs=re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+',msg)
                if any(not x.startswith((V.S.PROBE,V.S.OUTSIDER,'com.android.internal.','com.android.server.','org.json.')) for x in pkgs):foreign+=1;continue
                kept.append(line);events.append({'epoch':stamp,'pid':pid,'tag':tag,'message':msg})
            (OUT/('h2-ending-window-'+name+'.log')).write_text('\n'.join(kept)+ ('\n' if kept else ''),encoding='utf-8',newline='\n');baseline=N.load('before-normal-buffer-'+name+'.json');retained=(bool(times) and min(times)<=float(start)) or (not times and baseline['parsedEntries']==0)
            buffers[name]={'exitCode':r.returncode,'bytes':len(r.stdout),'sha256':N.digest(r.stdout),'oldestEpoch':min(times) if times else None,'latestEpoch':max(times) if times else None,'unparsedLines':bad,'scopedEntries':len(kept),'foreignMixedLinesWithheld':foreign,'windowCoverage':'PASS' if retained and bad==0 else 'UNKNOWN','fullRawArchived':False}
        # Only PID traces identified by actual research process-start events; old traces remain unread.
        for pid in sorted(pids-{6423}):
            raw=d.adb('exec-out','run-as',V.S.PROBE,'cat','files/r3-traces/trace-'+str(pid)+'.jsonl',label='h2-ending-worker-trace-'+str(pid))
            N.need(raw.returncode==0 and not raw.stderr and 0<len(raw.stdout)<=262144,'worker synthetic trace read failed')
            rows=[json.loads(x,object_pairs_hook=V.no_duplicate) for x in raw.stdout.decode('utf-8','strict').splitlines()];N.need(all(x.get('pid')==pid and x.get('uid')==10218 and x.get('process')==V.S.PROBE+':jpeg_worker' for x in rows),'worker trace identity mismatch')
            (OUT/('h2-ending-worker-trace-'+str(pid)+'.jsonl')).write_bytes(raw.stdout)
        N.save('h2-ending-log-result.json',{'status':'BLOCKED_PRESERVED','atUtc':V.S.utc_now(),'researchPids':sorted(pids),'deviceEpochStart':start,'deviceEpochEnd':end,'buffers':buffers,'events':events,'commands':len(records),'launches':0,'privateContentsRead':False,'doesNotClearEarlierUnknowns':True});print(json.dumps({'status':'BLOCKED_PRESERVED','researchRows':scoped,'researchPids':sorted(pids),'commands':len(records)}));return 0
    except Exception as e:N.save('h2-ending-log-error.json',{'status':'UNKNOWN','detail':str(e),'atUtc':V.S.utc_now()});print('Ending logs UNKNOWN: '+str(e));return 2
    finally:subprocess.run=real
if __name__=='__main__':raise SystemExit(main())
