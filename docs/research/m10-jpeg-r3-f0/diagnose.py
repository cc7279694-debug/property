#!/usr/bin/env python3
"""F0 only: read-only preflight, one bare Host launch, one guarded comparator.

No installs/builds/force-stop/signals/settings writes or R3 case commands.
Unrestricted log/process output is held in memory, scoped, then discarded.
"""
from __future__ import annotations
import argparse
import concurrent.futures
import datetime as dt
import hashlib
import importlib.util
import json
import re
import shlex
import subprocess
import sys
import time
from pathlib import Path

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[2]
OLD = REPO / 'docs/research/m10-jpeg-r3'
spec = importlib.util.spec_from_file_location('r3_read_only_helpers', OLD / 'scripts/run_device.py')
S = importlib.util.module_from_spec(spec)
spec.loader.exec_module(S)
S.DEVICE_ROOT = ROOT / 'evidence'
EXPECTED = {S.PROBE:'230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0',
            S.OUTSIDER:'214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358',
            S.PRODUCTION:'4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46'}
SYSTEM_NAMES = {'zygote','zygote64','system_server','artd','logd','lmkd','surfaceflinger','adbd'}

class Diagnostic(S.Supervisor):
    def command(self,args,*,label,timeout=45,private_metadata=False):
        if not 0 < timeout <= 45: raise S.StopExperiment('Bounded command timeout required')
        start=time.monotonic(); expired=False
        try: r=subprocess.run(args,capture_output=True,timeout=timeout,check=False)
        except subprocess.TimeoutExpired as e:
            expired=True; r=subprocess.CompletedProcess(args,124,e.stdout or b'',e.stderr or b'')
        record={'atUtc':S.utc_now(),'label':label,'argv':args,'exitCode':r.returncode,
                'wallMs':round((time.monotonic()-start)*1000,3),'timeoutSeconds':timeout,'timedOut':expired}
        if private_metadata:
            record.update(stdout='RAW_NOT_ARCHIVED: aggregate or scoped derivatives only',
                          stdoutBytes=len(r.stdout),stderr='RAW_NOT_ARCHIVED',stderrBytes=len(r.stderr))
        else: record.update(stdout=r.stdout.decode('utf-8','replace'),stderr=r.stderr.decode('utf-8','replace'))
        with self.command_log.open('a',encoding='utf-8',newline='\n') as f:f.write(json.dumps(record,ensure_ascii=True)+'\n')
        return r

    def processes(self,label):
        r=self.adb('shell','ps','-A','-o','USER,PID,PPID,VSZ,RSS,WCHAN,S,NAME',label=label,
                   private_metadata=True,required=False,timeout=10)
        lines=r.stdout.decode('utf-8','replace').splitlines(); rows=[]
        for line in lines[1:]:
            fields=line.split()
            if fields and (fields[-1] in SYSTEM_NAMES or fields[-1] in (S.PROBE,S.PROBE+':jpeg_worker',S.OUTSIDER)):
                rows.append(line)
        value={'atUtc':S.utc_now(),'exitCode':r.returncode,'allProcessCount':max(0,len(lines)-1),
               'header':lines[0] if lines else '', 'scopedRows':rows,'otherAppRowsArchived':False}
        S.save_json(self.output/(label+'.json'),value)
        return value

    def health(self,label):
        result={}
        for key,args in [('kernel',('shell','cat','/proc/loadavg','/proc/meminfo','/proc/pressure/cpu','/proc/pressure/memory')),
                         ('space',('shell','df','-k','/data')),('boot',('shell','getprop','sys.boot_completed')),
                         ('debug_app',('shell','settings','get','global','debug_app')),
                         ('wait_for_debugger',('shell','settings','get','global','wait_for_debugger')),
                         ('start_count',('shell','getprop','ro.boottime.zygote'))]:
            r=self.adb(*args,label=label+'-'+key,required=False,timeout=15)
            result[key]={'exitCode':r.returncode,'stdout':r.stdout.decode('utf-8','replace').strip()}
        S.save_json(self.output/(label+'.json'),result)
        return result

    def scoped_logs(self,label,start_epoch,packages,pids):
        raw=self.adb('logcat','-d','-b','main','-b','system','-b','crash','-b','events','-v','epoch','-t','4000',
                     label=label+'-read-window',private_metadata=True,required=False,timeout=20)
        selected=[]; dropped=0; seen=[]
        pattern=re.compile(r'^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s?(.*)$')
        for line in raw.stdout.decode('utf-8','replace').splitlines():
            m=pattern.match(line)
            if not m: continue
            epoch=float(m[1]); pid=int(m[2]); tag=m[5].strip(); msg=m[6]
            if epoch < start_epoch-0.25:continue
            # Discover target child IDs from package-bearing system events before PID filtering.
            if any(pkg in msg for pkg in packages):
                for q in re.findall(r'(?:Start proc |Killing |ProcessRecord\{\w+ )(\d+)',msg):pids.add(int(q))
                if pid not in [4251] and tag in ('LITHIC_R3','AndroidRuntime'):pids.add(pid)
        for line in raw.stdout.decode('utf-8','replace').splitlines():
            m=pattern.match(line)
            if not m:continue
            epoch=float(m[1]); pid=int(m[2]); tag=m[5].strip(); msg=m[6]
            if epoch < start_epoch-0.25:continue
            keep=pid in pids or any(pkg in msg for pkg in packages)
            if not keep and tag in ('Zygote','zygote','zygote64','art','libc','DEBUG','am_proc_start','am_proc_died','am_kill'):
                keep=any(re.search(r'(?<!\d)'+str(q)+r'(?!\d)',msg) for q in pids)
            if keep:
                # Never export a line that also names another Android application package.
                other=re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+',msg)
                if any(not any(x.startswith(pkg) for pkg in packages) for x in other):
                    dropped+=1;continue
                selected.append(line);seen.append(epoch)
        text='\n'.join(selected)+ ('\n' if selected else '')
        (self.output/(label+'.log')).write_text(text,encoding='utf-8',newline='\n')
        meta={'exitCode':raw.returncode,'requestedStartEpoch':start_epoch,'targetPackages':packages,
              'targetPids':sorted(pids),'keptLines':len(selected),'mixedOtherAppLinesDropped':dropped,
              'firstRetainedEpoch':min(seen) if seen else None,'lastRetainedEpoch':max(seen) if seen else None,
              'boundedTailLines':4000,'unrestrictedRawArchived':False,
              'limitation':'Bounded log buffer and explicit PID/package rules; missing lines do not prove code was not reached'}
        S.save_json(self.output/(label+'.json'),meta)
        return meta

def save(path,value):S.save_json(path,value)
def load(path):return json.loads(path.read_text())

def preflight(d):
    d.verify_device()
    before=d.snapshot('before')
    pkgs={pkg:d.package(pkg) for pkg in (S.PROBE,S.OUTSIDER)}
    pkgs[S.PRODUCTION]=before['productionPackage']
    for pkg,x in pkgs.items():
        if len(x['apks'])!=1 or x['apks'][0]['sha256']!=EXPECTED[pkg]:raise S.StopExperiment('Installed APK baseline mismatch: '+pkg)
    if len({x['uid'] for x in pkgs.values()})!=3:raise S.StopExperiment('Distinct installed UIDs not established')
    save(d.output/'installed-packages.json',pkgs)
    # Read only the known small research APK into local ignored scratch for actual Manifest audit.
    code=pkgs[S.PROBE]['apks'][0]['codePath']
    raw=d.adb('exec-out','cat',code,label='read-installed-probe-APK-for-manifest',private_metadata=True)
    if hashlib.sha256(raw.stdout).hexdigest()!=EXPECTED[S.PROBE]:raise S.StopExperiment('APK read identity mismatch')
    scratch=Path('E:/CODEX/.codex/research-build/m10-r3-f0')/d.output.name
    scratch.mkdir(parents=True,exist_ok=False); apk=scratch/'installed-probe.apk';apk.write_bytes(raw.stdout)
    aapt=S.SDK/'build-tools/36.0.0/aapt2.exe'
    m=d.command([str(aapt),'dump','xmltree',str(apk),'--file','AndroidManifest.xml'],label='actual-installed-manifest')
    text=m.stdout.decode('utf-8','replace');(d.output/'installed-probe-manifest.txt').write_text(text,encoding='utf-8')
    service=text[text.find('E: service'):]
    private=m.returncode==0 and 'WorkerService' in service and re.search(r'exported[^\n]*=false',service) is not None and ':jpeg_worker' in service
    if not private:raise S.StopExperiment('Actual installed private worker Manifest not confirmed')
    health=d.health('health-before'); ps=d.processes('processes-before')
    rows=ps['scopedRows']; oldworker=any(S.PROBE+':jpeg_worker' in x for x in rows)
    oldhost=any(x.split()[-1]==S.PROBE for x in rows if x.split())
    value={'status':'PASS' if not oldworker and not oldhost and health['boot']['stdout']=='1' else 'BLOCKED',
           'existingResearchWorker':oldworker,'existingResearchHost':oldhost,'workerPrivateManifestVerified':bool(private),
           'distinctUIDsVerified':True,'apkHashesVerified':True,'beforeSnapshot':before['atUtc'],
           'outcomeMeaning':'Preflight only, not R3 lifecycle acceptance'}
    save(d.output/'preflight.json',value)
    if value['status']!='PASS':raise S.StopExperiment('Existing process or boot state requires review; no cleanup permitted')

def launch(d,kind):
    p=load(d.output/'preflight.json')
    if p['status']!='PASS':raise S.StopExperiment('Successful F0 preflight required')
    pkg=S.PROBE if kind=='host' else S.OUTSIDER
    if kind=='outsider' and not(p['workerPrivateManifestVerified'] and p['distinctUIDsVerified']):raise S.StopExperiment('Comparator gates absent')
    # Exclusive persisted claim prevents this script from issuing a second launch.
    with (d.output/(kind+'-launch-claim.json')).open('x',encoding='utf-8') as f:
        json.dump({'atUtc':S.utc_now(),'package':pkg,'extras':[],'limit':1},f)
    start=float(d.text('shell','date','+%s.%N',label=kind+'-device-start-epoch'))
    component=pkg+('/.ProbeActivity' if kind=='host' else '/.OutsiderActivity')
    samples=[]
    with concurrent.futures.ThreadPoolExecutor(max_workers=1) as pool:
        pending=pool.submit(d.adb,'shell','am','start','-W','-n',component,label=kind+'-bare-launch',required=False,timeout=45)
        for i in range(4):
            time.sleep(1 if i==0 else 3)
            samples.append(d.processes(kind+'-process-'+str(i)))
            if pending.done():break
        result=pending.result()
    time.sleep(1)
    samples.append(d.processes(kind+'-process-end'))
    pids=set()
    for x in samples:
        for row in x['scopedRows']:
            fields=row.split()
            if fields[-1]==pkg or fields[-1]==S.PROBE+':jpeg_worker':pids.add(int(fields[1]))
    meta=d.scoped_logs(kind+'-scoped-system',start,[pkg,S.PROBE] if kind=='outsider' else [pkg],pids)
    # Only synthetic research files are read. No formal app contents are accessed.
    if kind=='host':
        r=d.adb('shell','run-as',pkg,'sh','-c',shlex.quote('if [ -d files/r3-traces ]; then find files/r3-traces -type f -name "trace-*.jsonl" -exec cat {} +; else echo NO_RESEARCH_TRACE; fi'),label='host-research-traces',required=False)
        (d.output/'host-research-traces.log').write_bytes(r.stdout)
        info={'traceExitCode':r.returncode,'traceBytes':len(r.stdout)}
    else:
        r=d.adb('shell','run-as',pkg,'cat','files/result.json',label='outsider-research-result',required=False)
        (d.output/'outsider-result-read.log').write_bytes(r.stdout);info={'resultExitCode':r.returncode,'resultBytes':len(r.stdout)}
    save(d.output/(kind+'-launch.json'),{'atUtc':S.utc_now(),'commandExitCode':result.returncode,'deviceStartEpoch':start,
         'component':component,'intentExtras':[],'pidsObserved':sorted(pids),'logSelection':meta,**info,
         'r3LifecycleScenarioExecuted':False,'classification':'REQUIRES_EVIDENCE_REVIEW'})

def finish(d):
    after=d.snapshot('after');before=load(d.output/'snapshot-before.json')
    comparison={'productionAPKAndUIDUnchanged':before['productionPackage']==after['productionPackage'],
                'productionPrivateMetadataUnchanged':before['productionPrivateMetadata']==after['productionPrivateMetadata'],
                'deviceSettingsUnchanged':before['settings']==after['settings'],'privateByteIntegrity':'NOT_RUN'}
    comparison['status']='PASS' if all(comparison[k] for k in list(comparison)[:3]) else 'BLOCKED'
    save(d.output/'protection-comparison.json',comparison);d.health('health-after');d.processes('processes-after')
    pkgs={pkg:d.package(pkg) for pkg in (S.PROBE,S.OUTSIDER)};save(d.output/'installed-research-after.json',pkgs)

if __name__=='__main__':
    a=argparse.ArgumentParser(description=__doc__);a.add_argument('step',choices=['preflight','host','outsider','finish']);a.add_argument('--run',required=True);args=a.parse_args()
    out=(ROOT/'evidence'/args.run).resolve()
    if not re.fullmatch(r'[0-9TZ.]+',args.run):raise SystemExit('Timestamp run name only')
    d=Diagnostic(out)
    try:
        if args.step=='preflight':preflight(d)
        else:
            d.verify_device()
            if args.step=='finish':finish(d)
            else:launch(d,args.step)
        print(json.dumps({'step':args.step,'commandExecution':'FINISHED_REVIEW_EVIDENCE','evidence':str(out)},ensure_ascii=True))
    except Exception as e:
        save(out/(args.step+'-blocked.json'),{'atUtc':S.utc_now(),'status':'BLOCKED','detail':str(e),'noAutomaticRetry':True})
        print(json.dumps({'step':args.step,'status':'BLOCKED','detail':str(e)},ensure_ascii=True));sys.exit(1)
