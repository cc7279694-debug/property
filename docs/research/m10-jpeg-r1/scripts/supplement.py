"""Remaining isolated cases after expanded-driver timeout; never rewrites it."""
import json
from run_android import EVIDENCE, REMOTE, BASE, LOCKS, adb, checked, remote_hash, sha, run, save

def main():
    records=[]
    journal=EVIDENCE/'supplement.jsonl'
    with journal.open('x',encoding='utf-8',newline='\n') as f:
        for label, mode, name, extra in (
            ('A1_CANCEL_UNSUPPORTED','a1','complete.jpg',['--cancel-after-ms','0']),
            ('MISSING_INPUT','a2','intentionally_absent.jpg',[]),
            ('VALID_444_REPEAT3','a2','valid_40mp_progressive_444.jpg',['--repeat','3']),
            ('VALID_444_LATE_CANCEL','a2','valid_40mp_progressive_444.jpg',['--cancel-after-progress','1000']),
            ('VALID_444_DEADLINE300','a2','valid_40mp_progressive_444.jpg',['--cancel-after-ms','300']),
        ):
            r={'label':label,**run(mode,name,extra,timeout=120)}
            records.append(r); f.write(json.dumps(r,ensure_ascii=False)+'\n'); f.flush()
            print(json.dumps({'label':label,'status':r.get('supervisorStatus')}),flush=True)
            if r.get('supervisorStatus')=='TIMEOUT': break
    digests=checked(['shell','sha256sum '+REMOTE+'/complete.jpg '+REMOTE+'/entropy_cut_eoi_retained.jpg '+REMOTE+'/strictjpeg_probe'])
    after=checked(['shell',"ps -A -o PID,ARGS | grep -E '^[[:space:]]*[0-9]* (strictjpeg_probe|generate_fixtures|generate_single_444|runtime_edges)( |$)' || true"])
    save('supplement.json',{'cases':records,'lockedRemoteSha256Readback':digests,
        'ownedNativeProcessesAfter':after,'lateTimeoutNotClosedByLaterPass':True,
        'resourceScope':'CLI caller balance and RSS observations only; no full allocator tracking, JNI or Kotlin Job test'})
    return 0

if __name__=='__main__':raise SystemExit(main())
