"""H2 only: one fixed normal launch, bounded synthetic evidence; no retries or recovery."""
import hashlib, importlib.util, json, os, re, shlex, subprocess, sys, time, uuid
from pathlib import Path
sys.dont_write_bytecode=True
HERE=Path(__file__).resolve().parent
REPO=HERE.parents[2]
OUT=HERE.parent/'m10-jpeg-r3-f1b/evidence/h2-20261017-01'
spec=importlib.util.spec_from_file_location('h2_locked_verify',HERE.parent/'m10-jpeg-r3-f1b/verify.py')
V=importlib.util.module_from_spec(spec);spec.loader.exec_module(V)
BUFFERS=('main','system','crash','events')
LINE=re.compile(r'^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+([A-Z])\s+([^:]+):\s?(.*)$')

def need(ok,why):
    if not ok: raise RuntimeError(why)
def load(name):return json.loads((OUT/name).read_text(encoding='utf-8'),object_pairs_hook=V.no_duplicate)
def save(name,value):
    with (OUT/name).open('x',encoding='utf-8',newline='\n') as h:json.dump(value,h,indent=2);h.write('\n');h.flush();os.fsync(h.fileno())
def digest(raw):return hashlib.sha256(raw).hexdigest()

class NormalOnly(V.Bounded):
    def command(self,args,**kw):
        if args[3:5]!=['shell','am']:return super().command(args,**kw)
        marker=load('normal-launch-issued.json')
        need(args==marker['argv'],'only exact persisted normal command is allowed')
        need(not getattr(self,'launchSent',False),'no second launch')
        self.launchSent=True
        return V.D.Diagnostic.command(self,args,**kw)

def epoch(d,label):
    value=d.text('shell','date','+%s.%N',label=label)
    need(re.fullmatch(r'[0-9]+\.[0-9]{9}',value),'unknown device time')
    return value

def buffer(d,name,phase,start=None,end=None,pids=()):
    r=d.adb('logcat','-d','-b',name,'-v','epoch',label=phase+'-full-'+name,private_metadata=True)
    need(r.returncode==0 and not r.stderr and len(r.stdout)<=32*1024*1024,'incomplete/oversize log capture')
    text=r.stdout.decode('utf-8','strict');need(not text or text.endswith('\n'),'missing log newline')
    times=[];events=[];kept=[];bad=0;foreign=0
    for line in text.splitlines():
        if not line.strip() or line.startswith('--------- '):continue
        m=LINE.fullmatch(line)
        if not m:bad+=1;continue
        stamp,pid,tid,level,tag,msg=m.groups();stamp=float(stamp);pid=int(pid);tag=tag.strip();times.append(stamp)
        if start is None or not float(start)<=stamp<=float(end):continue
        related=(pid in pids and tag in ('LITHIC_R3','ActivityThread','AndroidRuntime','libc','DEBUG')) or (tag in ('ActivityManager','ActivityTaskManager','am_proc_start','am_proc_bound','am_proc_died','am_kill','wm_create_activity','wm_on_create_called','wm_destroy_activity','wm_on_destroy_called','Zygote','zygote','zygote64') and (V.S.PROBE in msg or any(re.search(r'(?<!\d)'+str(p)+r'(?!\d)',msg) for p in pids)))
        if not related:continue
        packages=re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+',msg)
        if any(not s.startswith((V.S.PROBE,V.S.OUTSIDER,'com.android.internal.','com.android.server.','org.json.')) for s in packages):foreign+=1;continue
        events.append({'epoch':stamp,'pid':pid,'tag':tag,'message':msg});kept.append(line)
    value={'buffer':name,'bytes':len(r.stdout),'sha256':digest(r.stdout),'exitCode':r.returncode,'stderrBytes':len(r.stderr),'oldestEpoch':min(times) if times else None,'latestEpoch':max(times) if times else None,'parsedEntries':len(times),'unparsedLines':bad,'scopedEntries':len(kept),'foreignMixedLinesWithheld':foreign,'fullOutputArchived':False}
    save(phase+'-buffer-'+name+'.json',value)
    if kept:(OUT/('normal-window-'+name+'.log')).write_text('\n'.join(kept)+'\n',encoding='utf-8',newline='\n')
    need(bad==0,'unknown log format; no complete-window claim')
    return value,events

def read_json(d,path,label):
    need(re.fullmatch(r'files/r3-reports/[A-Za-z0-9_-]{1,80}\.json',path),'unexpected synthetic report path')
    script='if [ -f '+shlex.quote(path)+' ]; then stat -c "%s" '+shlex.quote(path)+'; cat '+shlex.quote(path)+'; else printf "ABSENT\\n"; fi'
    r=d.adb('exec-out','run-as',V.S.PROBE,'sh','-c',shlex.quote(script),label=label)
    need(r.returncode==0 and not r.stderr,'synthetic report collection failed')
    if r.stdout.strip()==b'ABSENT':return None
    head,raw=r.stdout.split(b'\n',1);need(head.strip().isdigit() and int(head)==len(raw)<=16384,'report length bound/identity')
    obj=json.loads(raw.decode('utf-8'),object_pairs_hook=V.no_duplicate)
    (OUT/(label+'.json')).write_bytes(raw)
    save(label+'-integrity.json',{'path':path,'bytes':len(raw),'sha256':digest(raw)})
    return obj

def read_trace(d,pid):
    need(type(pid) is int and pid>0,'trace PID invalid')
    path='files/r3-traces/trace-'+str(pid)+'.jsonl'
    r=d.adb('exec-out','run-as',V.S.PROBE,'cat',path,label='normal-trace-'+str(pid))
    need(r.returncode==0 and not r.stderr and 0<len(r.stdout)<=262144 and r.stdout.endswith(b'\n'),'incomplete synthetic trace')
    rows=[json.loads(x,object_pairs_hook=V.no_duplicate) for x in r.stdout.decode('utf-8','strict').splitlines()]
    need(all(x.get('pid')==pid and x.get('uid')==10218 and x.get('process') in (V.S.PROBE,V.S.PROBE+':jpeg_worker') for x in rows),'trace identity conflict')
    (OUT/('normal-trace-'+str(pid)+'.jsonl')).write_bytes(r.stdout)
    save('normal-trace-'+str(pid)+'-integrity.json',{'path':path,'bytes':len(r.stdout),'sha256':digest(r.stdout),'rows':len(rows)})
    return rows

def main():
    contract=load('h2-contract.json')
    for p,f in contract['lockedScripts'].items():need(digest((REPO/p).read_bytes())==f['sha256'],'locked script changed')
    need(load('h2-preflight-execution.json')['entryReturnCode']==0 and load('preflight-gate.json')['classification']=='PREFLIGHT_ELIGIBLE','fresh complete preflight required')
    need(not (OUT/'blocked.json').exists(),'preflight blocked')
    run='h2_'+uuid.uuid4().hex
    save('normal-supervision-attempt.json',{'atUtc':V.S.utc_now(),'runId':run,'maxActivityCommands':1,'noRetry':True,'scriptSHA256':digest(Path(__file__).read_bytes())})
    d=NormalOnly(OUT,'h2-normal'); real_run=subprocess.run;command_count=0
    def observe(args,*a,**k):
        nonlocal command_count
        need(str(args[0]).replace('\\','/')==str(V.S.ADB).replace('\\','/') and args[1:3]==['-s',V.S.SERIAL],'unexpected subprocess')
        command_count+=1; t=time.monotonic();item={'index':command_count,'startedAtUtc':V.S.utc_now(),'argv':args}
        try:
            r=real_run(args,*a,**k);item.update(exitCode=r.returncode,timedOut=False,stdoutBytes=len(r.stdout),stdoutSHA256=digest(r.stdout),stderrBytes=len(r.stderr),stderrSHA256=digest(r.stderr));return r
        except subprocess.TimeoutExpired as e:
            item.update(exitCode=None,timedOut=True,stdoutBytes=len(e.stdout or b''),stdoutSHA256=digest(e.stdout or b''),stderrBytes=len(e.stderr or b''),stderrSHA256=digest(e.stderr or b''));raise
        finally:
            item.update(finishedAtUtc=V.S.utc_now(),wallMs=round((time.monotonic()-t)*1000,3))
            with (OUT/'h2-normal-command-integrity.jsonl').open('a',encoding='utf-8',newline='\n') as h:h.write(json.dumps(item)+'\n')
    baseline={};start=None;end=None;report=None;error=None;traces={};launched=False
    try:
        subprocess.run=observe
        # One immediate read-only readiness sample, using unchanged G1/U1 rules.
        packages={p:d.package(p) for p in (V.S.PRODUCTION,V.S.PROBE,V.S.OUTSIDER)}
        need({p:v['uid'] for p,v in packages.items()}=={V.S.PRODUCTION:10216,V.S.PROBE:10218,V.S.OUTSIDER:10219},'H1 installed UID continuity mismatch')
        for p,v in packages.items():need(len(v['apks'])==1 and v['apks'][0]['sha256']==V.EXPECTED[p],'installed locked APK mismatch')
        save('normal-installed-identities.json',packages);d.research_uids={p:packages[p]['uid'] for p in (V.S.PROBE,V.S.OUTSIDER)}
        cap=V.G.capture(d.adb('shell','ps','-A','-o','USER,PID,PPID,NAME',label='normal-ready-processes',private_metadata=True),'ps-user-pid-ppid-name-v1')
        rows=V.G.process_rows(cap,d.research_uids);save('normal-ready-processes.json',{'rows':rows,'capture':{k:v for k,v in cap.items() if k!='stdout'}})
        cap=V.G.capture(d.adb('shell','dumpsys','activity','services',V.S.PROBE,label='normal-ready-services',private_metadata=True),'api36-f1b-v1')
        service=V.G.SERVICE.classify_capture(cap,packages[V.S.PROBE]['uid'],packages[V.S.OUTSIDER]['uid']);save('normal-ready-services.json',{'classification':service,'capture':{k:v for k,v in cap.items() if k!='stdout'}})
        need(service['classification']!='BLOCKED','service readiness blocked')
        inventory=d.inventory('normal-ready-files');stage=V.validate_stages(inventory);save('normal-ready-stage.json',stage);need(stage['status']=='PASS','stage readiness blocked')
        for name in BUFFERS:baseline[name],_=buffer(d,name,'before-normal')
        start=epoch(d,'normal-window-start')
        argv=[str(V.S.ADB),'-s',V.S.SERIAL,'shell','am','start','-W','-n',V.S.PROBE+'/.ProbeActivity','--es','command','run','--es','case','normal','--es','runId',run]
        # Exclusive fsync marker exists before the sole Activity command, including uncertain returns.
        save('normal-launch-issued.json',{'atUtc':V.S.utc_now(),'runId':run,'argv':argv,'deviceEpochStart':start,'noRetry':True,'maximumLaunches':1})
        launched=True
        r=d.command(argv,label='only-normal-launch',timeout=30)
        need(r.returncode==0 and b'Status: ok' in r.stdout and not r.stderr,'normal launch failed/unknown; never repeat')
        deadline=time.monotonic()+20; index=0
        while time.monotonic()<deadline:
            index+=1;report=read_json(d,'files/r3-reports/'+run+'.json','normal-observed-result-'+str(index))
            if report is not None:break
            time.sleep(0.4)
        need(report is not None,'normal report timeout UNKNOWN; no retry')
        save('normal-result.json',report)
        need(report.get('status')=='PASS' and report.get('runId')==run and report.get('case')=='normal','normal report FAIL/identity conflict')
        need(type(report.get('hostPid')) is int and type(report.get('workerPid')) is int and report['hostPid']!=report['workerPid'],'host/worker identity unknown')
        for pid in (report['hostPid'],report['workerPid']):traces[str(pid)]=read_trace(d,pid)
        time.sleep(2) # observe OS death/log completion, no new App operation
    except Exception as stopped:error={'type':type(stopped).__name__,'detail':str(stopped)}
    if launched:
        try:
            end=epoch(d,'normal-window-end');pids=[int(p) for p in traces]
            if report:
                pids=list({p for p in (*pids,report.get('hostPid'),report.get('workerPid')) if type(p) is int and p>0})
            coverage={};events=[]
            for name in BUFFERS:
                after,e=buffer(d,name,'after-normal',start,end,pids);events+=e
                retained=(after['oldestEpoch'] is not None and after['oldestEpoch']<=float(start)) or (baseline[name]['parsedEntries']==after['parsedEntries']==0)
                coverage[name]={'status':'PASS' if retained else 'UNKNOWN','retainedStart':retained,'rawBytes':after['bytes'],'rawSHA256':after['sha256']}
                need(retained,'log retention incomplete '+name)
            save('normal-log-window.json',{'deviceEpochStart':start,'deviceEpochEnd':end,'pids':pids,'coverage':coverage,'events':events,'fullUnrelatedLogsArchived':False})
        except Exception as stopped:error=error or {'type':type(stopped).__name__,'detail':str(stopped)}
    subprocess.run=real_run
    save('normal-supervision-result.json',{'status':'BLOCKED' if error else 'COLLECTED_REQUIRES_LIFECYCLE_AND_ENDING_REVIEW','runId':run,'activityCommandsIssued':int(launched),'installCount':0,'adbCommands':command_count,'error':error,'noRetry':True,'atUtc':V.S.utc_now()})
    if error:save('h2-blocked.json',{'phase':'single-normal','detail':error,'activityCommandsIssued':int(launched),'noRetry':True});print('H2 BLOCKED: '+error['detail']);return 2
    print('One normal collected; lifecycle and ending assertions still required');return 0
if __name__=='__main__':raise SystemExit(main())
