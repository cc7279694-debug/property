"""F1-B stopped at the safety gate. Read-only preflight/final evidence only; no install or launch entry."""
from pathlib import Path
import argparse, datetime, hashlib, importlib.util, json, os, re, shlex, subprocess, sys, time, uuid, xml.etree.ElementTree as ET

sys.dont_write_bytecode = True
HERE=Path(__file__).resolve().parent
REPO=HERE.parents[2]
spec=importlib.util.spec_from_file_location('f0_helpers',REPO/'docs/research/m10-jpeg-r3-f0/diagnose.py')
D=importlib.util.module_from_spec(spec);spec.loader.exec_module(D)
S=D.S;S.DEVICE_ROOT=HERE/'evidence'
os.environ['JAVA_HOME']='C:/Users/CDD/AppData/Local/MirraAndroid/jdk17/jdk-17.0.20.1+1'
APK_ROOT=Path('E:/CODEX/.codex/worktrees/m10-jpeg-strict-r3-f1/砺石新APP/docs/research/m10-jpeg-r3/host')
EXPECTED={S.PRODUCTION:'4bfd57231a9028ed5c2cd6961e2c59756936b688c6bf90159d58743301889c46',S.PROBE:'a3eea12dfe05736a69c473fa812d0b52a43c8100bbed2cffd0ba9c8f5f0ea684',S.OUTSIDER:'a51c4984f07986c8df52b3dbbf7f946523e4c259aa67ab5e8f9b3feffdf41879'}
OLD={S.PROBE:'230407d8bc98aeac6fcda632b935551e8ca3322cbdf55cb73baf8bc258cb47d0',S.OUTSIDER:'214a6e98085d77d8fffb0c5be9f9aff70700c0f1e6365a3042b6ccf72385b358'}
def save(p,x):S.save_json(p,x)
def load(p):return json.loads(p.read_text(encoding='utf-8'))
def require(ok,why):
    if not ok:raise S.StopExperiment(why)
def no_duplicate(pairs):
    result={}
    for key,value in pairs:
        if key in result:raise ValueError('duplicate JSON key: '+key)
        result[key]=value
    return result

class Bounded(D.Diagnostic):
    def __init__(self,out,phase):
        super().__init__(out);self.phase=phase
    def command(self,args,**kw):
        if str(args[0])==str(S.ADB):
            require(args[1:3]==['-s',S.SERIAL],'exact authorized serial required')
            tail=args[3:]
            if tail and tail[0]=='install':raise S.StopExperiment('F1-B is blocked; installation entry removed')
            elif tail[:2]==['shell','am']:raise S.StopExperiment('F1-B is blocked; Activity launch entry removed')
            else:
                require(tail and tail[0] in ('shell','exec-out','emu','get-state','logcat'),'unexpected ADB command')
                require(not any(x in tail for x in ('clear','uninstall','reboot','force-stop','kill','kill_host','kill-server','start-server','wipe-data','put','delete')),'device mutation forbidden')
                if tail[0]=='emu':require(tail==['emu','avd','name'],'emulator control forbidden')
        return super().command(args,**kw)
    def processes(self,label):
        result=self.adb('shell','ps','-A','-o','USER,PID,PPID,NAME',label=label,private_metadata=True)
        rows=[]
        for line in result.stdout.decode().splitlines()[1:]:
            fields=line.split()
            if len(fields)<4:continue
            user=fields[0];match=re.fullmatch(r'u(\d+)_a(\d+)',user)
            uid=int(user) if user.isdigit() else (int(match[1])*100000+10000+int(match[2]) if match else None)
            target=fields[-1] in (S.PROBE,S.PROBE+':jpeg_worker',S.OUTSIDER)
            if target:require(uid is not None,'research UID cannot be resolved from ps')
            if uid in self.research_uids.values() or target:
                rows.append({'uid':uid,'pid':int(fields[1]),'ppid':int(fields[2]),'name':fields[-1]})
        value={'atUtc':S.utc_now(),'rows':rows,'unrelatedProcessRowsArchived':False}
        save(self.output/(label+'.json'),value);return value
    def inventory(self,label):
        script="if [ -e files ]; then find files -exec stat -c '%f|%d|%i|%s|%Y|%Z|%n' {} +; else printf 'ABSENT\\n'; fi"
        p=self.adb('shell','run-as',S.PROBE,'sh','-c',shlex.quote(script),label=label+'-stat')
        require(not p.stderr.strip(),'research inventory diagnostics')
        entries=[];objects={};hashes={}
        for line in p.stdout.decode().splitlines():
            if line=='ABSENT':continue
            fields=line.split('|');require(len(fields)==7,'malformed research stat')
            mode=int(fields[0],16);kind={0x8000:'regular',0x4000:'directory',0xa000:'symlink'}.get(mode&0xf000,'other')
            path=fields[6];require(path=='files' or (path.startswith('files/') and '..' not in path.split('/')),'unexpected research path')
            item={'path':path,'type':kind,'device':int(fields[1]),'inode':int(fields[2]),'size':int(fields[3]),'mode':fields[0],'mtime':int(fields[4]),'ctime':int(fields[5])};entries.append(item)
            known=re.fullmatch(r'files/synthetic-stages/(?:current\.json|[0-9a-f-]{36}/(?:owner|state|receipt|cleanup|revoked|output-owner)\.json)',path)
            if known and kind=='regular':
                require(item['size']<=16384,'bounded synthetic JSON exceeded')
                raw=self.adb('exec-out','run-as',S.PROBE,'cat',path,label=label+'-read-'+path.replace('/','_')).stdout
                require(len(raw)==item['size'],'synthetic JSON changed during read')
                target=self.output/'synthetic-json'/label/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
                objects[path]=json.loads(raw.decode('utf-8'),object_pairs_hook=no_duplicate);hashes[path]=hashlib.sha256(raw).hexdigest()
        value={'atUtc':S.utc_now(),'entries':entries,'json':objects,'sha256':hashes}
        save(self.output/(label+'.json'),value);return value

def validate_stages(snapshot,expected_current=None):
    spec=importlib.util.spec_from_file_location('f1b_stage_check',HERE/'stage_check.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
    return m.validate_snapshot(snapshot,expected_current=expected_current)

def safe_idle(d,label):
    packages={p:d.package(p) for p in (S.PROBE,S.OUTSIDER)}
    save(d.output/(label+'-packages.json'),packages)
    d.research_uids={p:packages[p]['uid'] for p in packages}
    require(packages[S.PROBE]['uid']!=packages[S.OUTSIDER]['uid'],'Outsider does not have a distinct UID')
    ps=d.processes(label+'-processes')
    require(not any(x['name']==S.PROBE+':jpeg_worker' for x in ps['rows']),'active old worker: stop without install/cleanup')
    require(all(x['name'] in (S.PROBE,S.OUTSIDER) for x in ps['rows']),'unrecognized process under research UID')
    services=d.adb('shell','dumpsys','activity','services',S.PROBE,label=label+'-services',private_metadata=True)
    text=services.stdout.decode('utf-8','replace')
    selected=[x for x in text.splitlines() if S.PROBE in x]
    save(d.output/(label+'-services.json'),{'matchingLines':selected,'unrelatedLinesArchived':False,'workerMentioned':'.WorkerService' in text})
    require('.WorkerService' not in text,'existing Worker service or pending binding: stop')
    snapshot=d.inventory(label+'-files');result=validate_stages(snapshot)
    save(d.output/(label+'-stage-check.json'),result)
    require(result['status']=='PASS','unsafe or unknown prior research stage: '+str(result['reasons']))
    return {'packages':packages,'processes':ps,'inventory':snapshot,'stageCheck':result}

def protection(d,label):
    value=d.snapshot(label)
    require(len(value['productionPackage']['apks'])==1 and value['productionPackage']['apks'][0]['sha256']==EXPECTED[S.PRODUCTION],'formal APK bytes mismatch')
    save(d.output/(label+'-extra-settings.json'),{k:d.text('shell',*v,label=label+'-'+k) for k,v in [('auto_time',('settings','get','global','auto_time')),('auto_time_zone',('settings','get','global','auto_time_zone')),('timezone',('getprop','persist.sys.timezone'))]})
    return value

def preflight(d):
    d.verify_device();require(d.text('shell','getprop','sys.boot_completed',label='boot-complete')=='1','boot incomplete')
    before=protection(d,'before');idle=safe_idle(d,'preflight')
    require(before['productionPackage']['uid'] not in d.research_uids.values(),'research/formal UID collision')
    for p,v in idle['packages'].items():require(len(v['apks'])==1 and v['apks'][0]['sha256'] in (OLD[p],EXPECTED[p]),'unrecognized installed research APK')
    save(d.output/'preflight-pass.json',{'status':'PASS','installed':idle['packages'],'productionUID':before['productionPackage']['uid'],'n01':'OPEN_NOT_TESTED','atUtc':S.utc_now()})

def finish(d):
    # After a blocking gate, collect only read-only residual/protection evidence.
    d.verify_device()
    packages={p:d.package(p) for p in (S.PROBE,S.OUTSIDER)}
    d.research_uids={p:packages[p]['uid'] for p in packages}
    ps=d.processes('after-processes')
    raw=d.adb('shell','dumpsys','activity','services',S.PROBE,label='blocked-service-record-detail',private_metadata=True).stdout.decode('utf-8','replace')
    all_lines=raw.splitlines()
    start=next((i for i,x in enumerate(all_lines) if '* ServiceRecord{' in x and S.PROBE+'/.WorkerService' in x),None)
    selected=[] if start is None else all_lines[start:]
    lines=[];dropped=0
    for line in selected:
        names=re.findall(r'\b(?:com|org|net)\.[A-Za-z0-9_.]+',line)
        if any(not x.startswith((S.PROBE,S.OUTSIDER,'com.android.','org.json.')) for x in names):dropped+=1;continue
        lines.append(line)
    (d.output/'blocked-service-record-detail.txt').write_text('\n'.join(lines)+'\n',encoding='utf-8')
    save(d.output/'blocked-service-detail-scope.json',{'targetPackage':S.PROBE,'rawBytes':len(raw.encode()),'keptLines':len(lines),'excludedNonResearchPrefixLines':start,'foreignPackageLinesDropped':dropped,'noUnrestrictedOutputArchived':True})
    inventory=d.inventory('after-files');stage=validate_stages(inventory)
    save(d.output/'after-stage-check.json',stage)
    after=protection(d,'after');before=load(d.output/'snapshot-before.json')
    checks={'formalAPKAndUIDUnchanged':before['productionPackage']==after['productionPackage'],'formalPrivateMetadataUnchanged':before['productionPrivateMetadata']==after['productionPrivateMetadata'],'deviceSettingsUnchanged':before['settings']==after['settings'] and load(d.output/'before-extra-settings.json')==load(d.output/'after-extra-settings.json'),'researchPackagesUnchanged':packages=={p:load(d.output/'snapshot-research-before.json')[p] for p in packages},'privateByteIntegrity':'NOT_RUN'}
    checks['status']='PASS' if all(checks[k] for k in ('formalAPKAndUIDUnchanged','formalPrivateMetadataUnchanged','deviceSettingsUnchanged','researchPackagesUnchanged')) else 'BLOCKED'
    save(d.output/'protection-comparison.json',checks)
    save(d.output/'finish-readonly.json',{'status':'READ_ONLY_COLLECTION_COMPLETE','packages':packages,'processes':ps,'stage':stage,'experimentsExecuted':0,'existingBlockerPreserved':load(d.output/'blocked.json') if (d.output/'blocked.json').exists() else None})

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('phase',choices=('preflight','finish'));p.add_argument('--output',required=True,type=Path);args=p.parse_args()
    d=Bounded(args.output,args.phase)
    if args.phase!='finish':
        require(not (d.output/'blocked.json').exists(),'previous failure: mutation/retry prohibited')
        with (d.output/(args.phase+'-attempt.json')).open('x',encoding='utf-8') as f:json.dump({'phase':args.phase,'utc':S.utc_now(),'scriptSHA256':S.sha256_file(Path(__file__))},f)
        (d.output/(args.phase+'-script.py.txt')).write_bytes(Path(__file__).read_bytes())
        save(d.output/(args.phase+'-input-sha.json'),{str(f.relative_to(REPO)):{'bytes':f.stat().st_size,'sha256':S.sha256_file(f)} for f in (Path(__file__),HERE/'stage_check.py',REPO/'docs/research/m10-jpeg-r3-f0/diagnose.py',REPO/'docs/research/m10-jpeg-r3/scripts/run_device.py')})
    else:
        with (d.output/'finish-attempt.json').open('x',encoding='utf-8') as f:json.dump({'phase':'finish','utc':S.utc_now(),'scriptSHA256':S.sha256_file(Path(__file__))},f)
        (d.output/'finish-script.py.txt').write_bytes(Path(__file__).read_bytes())
    try:
        if args.phase=='preflight':preflight(d)
        elif args.phase=='finish':finish(d)
        else:raise S.StopExperiment('phase not implemented; no implicit suite entry')
        print(json.dumps({'phase':args.phase,'status':'PASS','output':str(d.output)},ensure_ascii=True))
        return 0
    except Exception as e:
        error_file=d.output/('finish-collection-error.json' if args.phase=='finish' else 'blocked.json')
        save(error_file,{'phase':args.phase,'status':'BLOCKED','detail':str(e),'utc':S.utc_now(),'noAutomaticRetry':True})
        print(json.dumps({'phase':args.phase,'status':'BLOCKED','detail':str(e)},ensure_ascii=True));return 2

if __name__=='__main__':raise SystemExit(main())
