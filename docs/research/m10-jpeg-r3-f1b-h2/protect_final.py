"""H2 final protection after observed late Worker death. Read-only, no eligibility."""
import importlib.util,json,subprocess,sys,time
from pathlib import Path
sys.dont_write_bytecode=True
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('h2_protect_helpers',HERE/'normal_once.py');N=importlib.util.module_from_spec(spec);spec.loader.exec_module(N)
V,OUT=N.V,N.OUT

def main():
    N.need((OUT/'h2-blocked.json').exists(),'stopped H2 only');N.save('h2-final-protection-attempt.json',{'atUtc':V.S.utc_now(),'reason':'LATE_WORKER_AFTER_FIRST_ENDING_REQUIRES_FINAL_READ_ONLY_PROTECTION_NO_RETRY'})
    d=V.Bounded(OUT,'h2-final-protection');real=subprocess.run;records=[]
    def observe(args,*a,**k):
        N.need(str(args[0])==str(V.S.ADB) and args[1:3]==['-s',V.S.SERIAL],'unexpected command');item={'startedAtUtc':V.S.utc_now(),'argv':args};t=time.monotonic()
        try:
            r=real(args,*a,**k);item.update(exitCode=r.returncode,timedOut=False,stdoutBytes=len(r.stdout),stdoutSHA256=N.digest(r.stdout),stderrBytes=len(r.stderr),stderrSHA256=N.digest(r.stderr));return r
        except subprocess.TimeoutExpired as e:item.update(exitCode=None,timedOut=True,stdoutBytes=len(e.stdout or b''),stdoutSHA256=N.digest(e.stdout or b''),stderrBytes=len(e.stderr or b''),stderrSHA256=N.digest(e.stderr or b''));raise
        finally:
            item['wallMs']=round((time.monotonic()-t)*1000,3);records.append(item)
            with (OUT/'h2-final-protection-command-integrity.jsonl').open('a',encoding='utf-8') as h:h.write(json.dumps(item)+'\n')
    try:
        subprocess.run=observe;after=V.protection(d,'final');before=N.load('snapshot-before.json');packages={p:d.package(p) for p in (V.S.PROBE,V.S.OUTSIDER)};d.research_uids={p:x['uid'] for p,x in packages.items()}
        inv=d.inventory('h2-final-files');stage=V.validate_stages(inv,expected_current='586d0904-eaac-403a-895a-48c34cd98810');N.save('h2-final-stage-check.json',stage)
        cap=V.G.capture(d.adb('shell','ps','-A','-o','USER,PID,PPID,NAME',label='h2-final-protection-processes',private_metadata=True),'ps-user-pid-ppid-name-v1')
        try:process={'status':'PASS','rows':V.G.process_rows(cap,d.research_uids)}
        except ValueError as e:process={'status':'BLOCKED','reason':str(e)}
        process['capture']={k:v for k,v in cap.items() if k!='stdout'};N.save('h2-final-protection-processes.json',process)
        cap=V.G.capture(d.adb('shell','dumpsys','activity','services',V.S.PROBE,label='h2-final-protection-services',private_metadata=True),'api36-f1b-v1');classification=V.G.SERVICE.classify_capture(cap,packages[V.S.PROBE]['uid'],packages[V.S.OUTSIDER]['uid']);N.save('h2-final-services.json',{'classification':classification,'capture':{k:v for k,v in cap.items() if k!='stdout'},'rawFullArchived':False})
        checks={'formalAPKAndUIDUnchanged':before['productionPackage']==after['productionPackage'],'formalPrivateMetadataUnchanged':before['productionPrivateMetadata']==after['productionPrivateMetadata'],'deviceSettingsUnchanged':before['settings']==after['settings'] and N.load('before-extra-settings.json')==N.load('final-extra-settings.json'),'researchAPKAndUIDUnchanged':packages==N.load('normal-installed-identities.json') if False else packages=={p:N.load('normal-installed-identities.json')[p] for p in packages}}
        N.save('h2-final-protection-result.json',{'status':'PASS' if all(checks.values()) else 'BLOCKED','atUtc':V.S.utc_now(),'checks':checks,'packages':packages,'formalBefore':before['productionPrivateMetadata'],'formalAfter':after['productionPrivateMetadata'],'privateByteIntegrity':'NOT_RUN','stageStatus':stage['status'],'processStatus':process['status'],'serviceClassification':classification['classification'],'adbCommands':len(records),'installCount':0,'activityLaunchCount':0,'originalBlockersPreserved':True});print(json.dumps({'protection':'PASS' if all(checks.values()) else 'BLOCKED','stage':stage,'process':process['status'],'service':classification['classification'],'commands':len(records)}));return 0
    except Exception as e:N.save('h2-final-protection-error.json',{'status':'UNKNOWN','detail':str(e),'atUtc':V.S.utc_now()});print('Final protection UNKNOWN: '+str(e));return 2
    finally:subprocess.run=real
if __name__=='__main__':raise SystemExit(main())
