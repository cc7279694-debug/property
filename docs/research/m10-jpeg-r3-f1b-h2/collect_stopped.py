"""H2 stopped: one bounded read-only residual collection, no execution/recovery entry."""
import hashlib,importlib.util,json,re,subprocess,sys,time
from pathlib import Path
sys.dont_write_bytecode=True
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('h2_once_helpers',HERE/'normal_once.py');N=importlib.util.module_from_spec(spec);spec.loader.exec_module(N)
V,OUT=N.V,N.OUT

def main():
    N.need((OUT/'h2-blocked.json').exists(),'only after recorded blocker')
    N.save('h2-stopped-collection-attempt.json',{'atUtc':V.S.utc_now(),'purpose':'READ_ONLY_RESIDUAL_AFTER_UNKNOWN_NO_RETRY'})
    d=V.Bounded(OUT,'h2-stopped-residual');real=subprocess.run;records=[]
    def observe(args,*a,**k):
        N.need(args[1:3]==['-s',V.S.SERIAL] and str(args[0])==str(V.S.ADB),'unexpected command')
        item={'atUtc':V.S.utc_now(),'argv':args};t=time.monotonic()
        try:
            r=real(args,*a,**k);item.update(exitCode=r.returncode,timedOut=False,stdoutBytes=len(r.stdout),stdoutSHA256=N.digest(r.stdout),stderrBytes=len(r.stderr),stderrSHA256=N.digest(r.stderr));return r
        except subprocess.TimeoutExpired as e:item.update(exitCode=None,timedOut=True,stdoutBytes=len(e.stdout or b''),stdoutSHA256=N.digest(e.stdout or b''),stderrBytes=len(e.stderr or b''),stderrSHA256=N.digest(e.stderr or b''));raise
        finally:
            item['wallMs']=round((time.monotonic()-t)*1000,3);records.append(item)
            with (OUT/'h2-stopped-command-integrity.jsonl').open('a',encoding='utf-8') as h:h.write(json.dumps(item)+'\n')
    report=None;traces={};errors=[];pids={6423};result={}
    try:
        subprocess.run=observe
        launch=N.load('normal-launch-issued.json');run=launch['runId']
        # One read of only the exact report claimed by the already-issued Intent.
        report=N.read_json(d,'files/r3-reports/'+run+'.json','normal-ending-observed-report')
        N.save('normal-ending-report-summary.json',{'present':report is not None,'report':report,'classification':'RESIDUAL_ONLY_DOES_NOT_CLEAR_LAUNCH_UNKNOWN'})
        try:
            trace=N.read_trace(d,6423);traces['6423']=trace
            for row in trace:
                if row.get('event')=='SYNC_HELLO' and row.get('accepted') is True and row.get('callingUid')==10218 and type(row.get('callingPid')) is int:pids.add(row['callingPid'])
        except Exception as e:errors.append({'scope':'host trace','detail':str(e)})
        if report and type(report.get('workerPid')) is int and report['workerPid']>0:pids.add(report['workerPid'])
        for pid in sorted(pids-{6423}):
            try:traces[str(pid)]=N.read_trace(d,pid)
            except Exception as e:errors.append({'scope':'worker trace','detail':str(e)})
        cap=V.G.capture(d.adb('shell','ps','-A','-o','USER,PID,PPID,NAME',label='h2-stopped-research-processes',private_metadata=True),'ps-user-pid-ppid-name-v1')
        lines=V.G.complete_text(cap,'ps-user-pid-ppid-name-v1').splitlines();N.need(lines[0].split()==['USER','PID','PPID','NAME'],'unknown ps header')
        rows=[]
        for line in lines[1:]:
            fields=line.split();N.need(len(fields)==4,'malformed ps row')
            uid=V.G.resolve_ps_uid(fields[0])
            if uid in (10218,10219) or fields[-1].startswith((V.S.PROBE,V.S.OUTSIDER)):
                rows.append({'uid':uid,'pid':int(fields[1]),'ppid':int(fields[2]),'name':fields[3]})
        try:V.G.process_rows(cap,{V.S.PROBE:10218,V.S.OUTSIDER:10219});classification='PASS';reason=None
        except ValueError as e:classification='BLOCKED';reason=str(e)
        N.save('h2-stopped-scoped-processes.json',{'classification':classification,'reason':reason,'rows':rows,'capture':{k:v for k,v in cap.items() if k!='stdout'},'originalEndingUnknownUnchanged':True,'unrelatedRowsArchived':False})
        end=N.epoch(d,'h2-stopped-window-end');start=launch['deviceEpochStart'];buffers={};events=[]
        for name in N.BUFFERS:
            r=d.adb('logcat','-d','-b',name,'-v','epoch',label='h2-stopped-full-'+name,private_metadata=True)
            N.need(r.returncode==0 and not r.stderr and len(r.stdout)<=32*1024*1024,'incomplete stopped log capture')
            text=r.stdout.decode('utf-8','strict');N.need(not text or text.endswith('\n'),'incomplete stopped log text')
            times=[];kept=[];bad=0;foreign=0
            for line in text.splitlines():
                if not line.strip() or line.startswith('--------- '):continue
                m=N.LINE.fullmatch(line)
                if not m:bad+=1;continue
                stamp,pid,tid,level,tag,msg=m.groups();stamp=float(stamp);pid=int(pid);tag=tag.strip();times.append(stamp)
                if not float(start)<=stamp<=float(end):continue
                related=(pid in pids and tag in ('LITHIC_R3','ActivityThread','AndroidRuntime','libc','DEBUG')) or (tag in ('ActivityManager','ActivityTaskManager','am_proc_start','am_proc_bound','am_proc_died','am_kill','wm_create_activity','wm_on_create_called','wm_destroy_activity','wm_on_destroy_called','Zygote','zygote','zygote64') and (V.S.PROBE in msg or any(re.search(r'(?<!\d)'+str(p)+r'(?!\d)',msg) for p in pids)))
                if not related:continue
                packages=re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+',msg)
                if any(not s.startswith((V.S.PROBE,V.S.OUTSIDER,'com.android.internal.','com.android.server.','org.json.')) for s in packages):foreign+=1;continue
                kept.append(line);events.append({'epoch':stamp,'pid':pid,'tag':tag,'message':msg})
            (OUT/('h2-stopped-window-'+name+'.log')).write_text('\n'.join(kept)+ ('\n' if kept else ''),encoding='utf-8',newline='\n')
            old=N.load('before-normal-buffer-'+name+'.json')
            retained=(bool(times) and min(times)<=float(start)) or (not times and old['parsedEntries']==0)
            buffers[name]={'exitCode':r.returncode,'bytes':len(r.stdout),'sha256':N.digest(r.stdout),'oldestEpoch':min(times) if times else None,'latestEpoch':max(times) if times else None,'unparsedLines':bad,'scopedEntries':len(kept),'foreignMixedLinesWithheld':foreign,'completeWindow':'PASS' if retained and bad==0 else 'UNKNOWN','fullRawArchived':False}
        result={'status':'BLOCKED_PRESERVED','atUtc':V.S.utc_now(),'runId':run,'deviceEpochStart':start,'deviceEpochEnd':end,'researchPids':sorted(pids),'buffers':buffers,'events':events,'errors':errors,'reportPresent':report is not None,'tracesCollected':list(traces),'scope':'ONE_ENDING_READ_ONLY_COLLECTION_NO_RETRY_NO_RECOVERY'}
        N.save('h2-stopped-residual-result.json',result)
        print(json.dumps({'status':'BLOCKED_PRESERVED','report':report,'researchRows':rows,'traces':list(traces),'errors':errors},ensure_ascii=True))
    except Exception as e:N.save('h2-stopped-collection-error.json',{'status':'UNKNOWN','detail':str(e),'atUtc':V.S.utc_now()});print('Read-only residual collection UNKNOWN: '+str(e));return 2
    finally:subprocess.run=real
    return 0
if __name__=='__main__':raise SystemExit(main())
