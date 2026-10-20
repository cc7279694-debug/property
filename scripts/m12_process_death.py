"""Bounded supervisor for the three explicitly authorized synthetic M12 self-kills.

No AVD creation/start, App install, force-stop, arbitrary kill, or data cleanup.
The test runner substitutes plain Application so the default ledger is never opened.
"""
import argparse
import datetime as dt
import hashlib
import json
import re
import subprocess
import time
import uuid
from pathlib import Path

ADB = 'C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'
SERIAL = 'emulator-5686'
PKG = 'com.cooper.lithic.debug'
TEST = PKG + '.test'
APP_SHA = '3bb0d801300889d5c97f85dd768a14701f3a3f67aacd55dca929aa86e8233107'
OLD_TEST_SHA = 'd426c58ec36bb5505c6272c5b3f88037a55d9eb397b4a0ef55ba2070f82c098e'
BOOT = '837312dd-125c-41f8-92a9-eb2d96e7795e'
RUNNER = TEST + '/com.cooper.lithic.data.backup.M12ProcessDeathRunner'
CLASS = 'com.cooper.lithic.data.backup.M12ProcessDeathTest'
CASES = ('BEFORE_SWITCH', 'SWITCHING', 'STABLE')
ROOT = Path(__file__).resolve().parents[1] / 'build/m12-t04'


def digest(b):
    return hashlib.sha256(b).hexdigest()


def put(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('x', encoding='utf-8', newline='\n') as f:
        f.write(json.dumps(value, ensure_ascii=False, indent=2) + '\n')


def raw(path, b):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as f:
        f.write(b)


def record(out, name, argv, result, started, elapsed, timed_out=False):
    raw(out / (name + '.stdout'), result.stdout or b'')
    raw(out / (name + '.stderr'), result.stderr or b'')
    put(out / (name + '.json'), dict(argv=argv, utc=started, seconds=round(elapsed, 3),
        exitCode=result.returncode, timedOut=timed_out,
        stdoutBytes=len(result.stdout or b''), stdoutSha256=digest(result.stdout or b''),
        stderrBytes=len(result.stderr or b''), stderrSha256=digest(result.stderr or b'')))


def command(out, name, args, timeout=30, stdin=None, adb=True, allow_error=False):
    argv = [ADB, '-s', SERIAL, *args] if adb else args
    assert not (out / (name + '.json')).exists(), 'Evidence/operation already exists: ' + name
    started = dt.datetime.now(dt.timezone.utc).isoformat(); t = time.monotonic()
    try:
        r = subprocess.run(argv, input=stdin, capture_output=True, timeout=timeout)
    except subprocess.TimeoutExpired as e:
        r = subprocess.CompletedProcess(argv, None, e.stdout or b'', e.stderr or b'')
        record(out, name, argv, r, started, time.monotonic()-t, True)
        raise RuntimeError(name + ': TIMEOUT / UNKNOWN, no retry') from e
    record(out, name, argv, r, started, time.monotonic()-t)
    if not allow_error:
        assert r.returncode == 0, name + ': command failed ' + repr(r.stderr)
    return r


def shell(out, name, text, **kw):
    return command(out, name, ['shell', text], **kw).stdout


def private(out, name, text):
    # All call sites contain fixed or canonical UUID/enum paths, never user shell input.
    r=command(out,name,['shell',"run-as " + PKG + " sh -c '" + text + "'"])
    assert not r.stderr, name+': private read/write stderr UNKNOWN'
    return r.stdout


def windows_identity(out, name):
    ps = r'''$p=Get-CimInstance Win32_Process | Where-Object { $_.ProcessId -in @(1284,29348) }; $r=@($p | ForEach-Object { [pscustomobject]@{pid=$_.ProcessId;parent=$_.ParentProcessId;created=$_.CreationDate.ToString('o');exe=$_.ExecutablePath;cmd=$_.CommandLine} }); $ports=@(Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -in @(5686,5687) } | Select-Object LocalAddress,LocalPort,OwningProcess); @{processes=$r;ports=$ports}|ConvertTo-Json -Depth 5 -Compress'''
    b=command(out,name,['pwsh','-NoProfile','-Command',ps],adb=False).stdout
    j=json.loads(b.decode('utf-8-sig'))
    rows={r['pid']:r for r in j['processes']}
    assert set(rows)=={1284,29348}, 'Original emulator instance missing'
    for pid, parent, stamp in [(1284,27928,'2026-10-20T11:03:24.550494'),(29348,1284,'2026-10-20T11:03:25.115084')]:
        r=rows[pid]
        assert r['parent']==parent and r['created'].startswith(stamp), 'Instance creation conflict'
        expected='emulator.exe' if pid==1284 else 'qemu/windows-x86_64/qemu-system-x86_64-headless.exe'
        assert r['exe'].replace('\\','/').lower()==('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/emulator/'+expected).lower()
        for token in ['Lithic_M11B_Final_API36_20261020','-port 5686','lithic-m11b-final-20261020','-no-snapshot']:
            assert token in r['cmd'], 'Emulator command conflict'
    assert {r['LocalPort'] for r in j['ports']}=={5686,5687}
    assert all(r['OwningProcess']==29348 and r['LocalAddress'] in ('127.0.0.1','::1') for r in j['ports'])
    return j


def identity(out, name, package, expected_sha, expected_uid):
    b=shell(out,name+'-package',f'pm path {package}; cmd package list packages -U --user 0 {package}')
    lines=b.decode().splitlines()
    paths=[s.removeprefix('package:') for s in lines if s.startswith('package:/')]
    ids=[s for s in lines if s.startswith('package:'+package+' uid:')]
    assert len(paths)==1 and paths[0].endswith('/base.apk') and len(ids)==1
    assert ids[0]==f'package:{package} uid:{expected_uid}'
    h=shell(out,name+'-sha','sha256sum '+paths[0]).decode().split()[0]
    assert h==expected_sha, name+': APK hash mismatch'
    return dict(package=package,uid=expected_uid,sha256=h,path=paths[0])


def device(out, name):
    windows_identity(out,name+'-windows')
    b=shell(out,name+'-device','getprop sys.boot_completed; getprop ro.build.version.sdk; getprop ro.product.cpu.abi; cat /proc/sys/kernel/random/boot_id')
    assert b.decode().split()==['1','36','x86_64',BOOT], 'Device boot/API/ABI conflict'
    a=[s.strip() for s in command(out,name+'-avd',['emu','avd','name']).stdout.decode().splitlines() if s.strip()]
    assert a==['Lithic_M11B_Final_API36_20261020','OK'], 'AVD identity conflict'


def target_processes(out, name, require_absent=False):
    r=subprocess.run([ADB,'-s',SERIAL,'shell','ps -A -o UID,PID,NAME'],capture_output=True,timeout=30)
    assert r.returncode==0 and not r.stderr
    # Do not archive unrelated app names or a full process list.
    lines=r.stdout.decode().splitlines();assert lines[0].split()==['UID','PID','NAME']
    rows=[]
    for line in lines[1:]:
        cols=line.split();assert len(cols)==3
        if cols[0]=='10216' or cols[2].startswith(PKG):
            rows.append(dict(uid=cols[0],pid=int(cols[1]),name=cols[2]))
    put(out/(name+'.json'),dict(argv=[ADB,'-s',SERIAL,'shell','ps -A -o UID,PID,NAME'],exitCode=0,
        rawBytes=len(r.stdout),rawSha256=digest(r.stdout),rows=rows))
    app=[r for r in rows if r['uid']=='10216' or (r['name']==PKG or r['name'].startswith(PKG+':'))]
    if require_absent: assert not app, 'Active target App process: no instrumentation restart allowed'
    return app


def protected(out,name):
    # Hash/meta only: never copy ledger/image bytes, never open default Room/storage owner.
    text='set -e; set -o pipefail; for a in no_backup files; do test -d "$a"; test ! -L "$a"; stat -c "%n %u %d %i" "$a"; done; for d in no_backup/generations files/m11b-final-evidence files/m12-evidence; do test -d "$d"; find "$d" -print | sort | while IFS= read -r f; do test ! -L "$f"; if test -d "$f"; then stat -c "%n DIR %u %d %i %Y" "$f"; elif test -f "$f"; then stat -c "%n FILE %u %d %i %s %Y" "$f"; sha256sum "$f"; else exit 91; fi; done; done; test -f no_backup/active-generation; for f in no_backup/active-generation no_backup/active-generation.bak no_backup/active-generation.new; do test ! -L "$f"; if test -e "$f"; then test -f "$f"; stat -c "%n FILE %u %d %i %s %Y" "$f"; sha256sum "$f"; else echo "$f ABSENT"; fi; done'
    b=private(out,name,text)
    assert len(b)<128*1024 and b.count(b'no_backup/active-generation')==4
    return digest(b)


def baseline(resume=False):
    ROOT.mkdir(parents=True,exist_ok=True)
    if resume:
        c=json.loads((ROOT/'campaign.json').read_text(encoding='utf-8')); campaign=c['campaign'];out=Path(c['root'])
        assert not (out/'baseline.json').exists()
        # Preserve first strict-parser rejection. Re-evaluate the exact original bytes,
        # not a replacement device result or a repeated Android experiment.
        b=(out/'before-avd.stdout').read_bytes()
        assert [s.strip() for s in b.decode().splitlines() if s.strip()]==['Lithic_M11B_Final_API36_20261020','OK']
        assert json.loads((out/'before-avd.json').read_text(encoding='utf-8'))['exitCode']==0
        assert (out/'before-device.stdout').read_text(encoding='utf-8').split()==['1','36','x86_64',BOOT]
        put(out/'avd-line-ending-analysis.json',dict(rawSha256=digest(b),status='PASS',
            firstParser='REJECTED_CRCRLF_EMPTY_LINES',identityConflict=False))
        windows_identity(out,'baseline-continuation-windows')
    else:
        campaign=str(uuid.uuid4()); out=ROOT/campaign;out.mkdir()
        put(ROOT/'campaign.json',dict(campaign=campaign,root=str(out)))
        device(out,'before')
    app=identity(out,'before-app',PKG,APP_SHA,10216)
    test=identity(out,'before-test',TEST,OLD_TEST_SHA,10217)
    target_processes(out,'before-processes',True)
    h=protected(out,'before-protected')
    p=json.loads(private(out,'before-pointer','cat no_backup/active-generation'))
    assert p==dict(formatVersion=1,activeId='b3f76bf5-27f9-4cc4-b39c-d6feea2f2457',previousId='5886ffc8-317c-4342-a5fe-1ac5f7f85767',stage='STABLE')
    put(out/'baseline.json',dict(campaign=campaign,app=app,test=test,protectedSha256=h,pointer=p,status='PASS'))
    print(json.dumps(dict(campaign=campaign,status='PASS',target=SERIAL)))


def state():
    c=json.loads((ROOT/'campaign.json').read_text(encoding='utf-8'));out=Path(c['root'])
    assert out==ROOT/c['campaign'];assert str(uuid.UUID(c['campaign']))==c['campaign']
    base=json.loads((out/'baseline.json').read_text(encoding='utf-8'))
    if (out/'protected-v2-baseline.json').exists():
        base['protectedSha256']=json.loads((out/'protected-v2-baseline.json').read_text(encoding='utf-8'))['sha256']
    return c['campaign'],out,base


def upgrade_protection():
    _,out,_=state()
    assert not (out/'test-update.marker.json').exists(), 'Protection must precede any update/test'
    h=protected(out,'protected-v2-before')
    put(out/'protected-v2-baseline.json',dict(sha256=h,status='PASS',reason='Include directories, absence and fail-closed errors; original baseline preserved'))
    print('Stricter hash/meta baseline PASS')


def install(test_apk):
    campaign,out,base=state(); apk=Path(test_apk).resolve();h=digest(apk.read_bytes())
    expected=(Path(__file__).resolve().parents[1]/'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk').resolve()
    assert apk==expected and h!=OLD_TEST_SHA and apk.is_file(), 'Only this worktree Test output is permitted'
    sdk=Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/build-tools/36.0.0')
    audit=out/'final-artifact';audit.mkdir()
    badging=command(audit,'new-test-badging',[str(sdk/'aapt2.exe'),'dump','badging',str(apk)],adb=False).stdout.decode()
    assert re.search(r"^package: name='com\.cooper\.lithic\.debug\.test' ",badging,re.M)
    cert=command(audit,'new-test-certificate',[str(sdk/'apksigner.bat'),'verify','--print-certs',str(apk)],adb=False).stdout.decode()
    assert re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]+)',cert)==['29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9']
    xml=command(audit,'new-test-manifest',[str(sdk/'aapt2.exe'),'dump','xmltree',str(apk),'--file','AndroidManifest.xml'],adb=False).stdout.decode()
    assert 'com.cooper.lithic.data.backup.M12ProcessDeathRunner' in xml
    assert ':targetPackage' in xml and f'"{PKG}"' in xml
    assert 'android.permission.INTERNET' not in xml
    put(out/'new-test-audit.json',dict(path=str(apk),sha256=h,bytes=apk.stat().st_size,package=TEST,
        certificateSha256='29ded26bea44f1afe4fe383a302e4b507423d16223469d7da3fe578d9a7f83b9',status='PASS'))
    put(out/'test-update.marker.json',dict(campaign=campaign,testSha256=h,bytes=apk.stat().st_size,path=str(apk)))
    device(out,'preupdate')
    identity(out,'preupdate-app',PKG,APP_SHA,10216)
    identity(out,'preupdate-test',TEST,OLD_TEST_SHA,10217)
    target_processes(out,'preupdate-processes',True)
    assert protected(out,'preupdate-protected')==base['protectedSha256']
    r=command(out,'test-update',['install','-r',str(apk)],timeout=60)
    assert r.stdout.decode().split()==['Performing','Streamed','Install','Success'], 'Test update did not report Success'
    finish_install()


def finish_install():
    campaign,out,base=state()
    marker=json.loads((out/'test-update.marker.json').read_text(encoding='utf-8'));h=marker['testSha256']
    original=json.loads((out/'test-update.json').read_text(encoding='utf-8'))
    b=(out/'test-update.stdout').read_bytes()
    assert original['exitCode']==0 and original['stdoutSha256']==digest(b) and original['stderrBytes']==0
    assert b.decode().split()==['Performing','Streamed','Install','Success']
    assert not (out/'test-update-proof.json').exists()
    identity(out,'updated-app',PKG,APP_SHA,10216)
    identity(out,'updated-test',TEST,h,10217)
    components=shell(out,'updated-instrumentation','pm list instrumentation').decode().splitlines()
    assert f'instrumentation:{RUNNER} (target={PKG})' in components
    assert protected(out,'updated-protected')==base['protectedSha256']
    target_processes(out,'updated-processes',True)
    put(out/'test-update-proof.json',dict(campaign=campaign,testSha256=h,status='PASS'))
    print('Test-only update PASS; App/default ledger unchanged; no scenario started')


def instrument_args(campaign,case,h,method):
    return [ADB,'-s',SERIAL,'shell','am','instrument','-w','-r',
        '-e','class',CLASS+'#'+method,'-e','m12T04Campaign',campaign,
        '-e','m12T04Case',case,'-e','m12T04AppSha',APP_SHA,'-e','m12T04TestSha',h,RUNNER]


def process_identity(out,pid):
    assert isinstance(pid,int) and 1<pid<2**31
    b=private(out,'live-process-stat',f'cat /proc/{pid}/stat')
    tail=b.decode().rsplit(')',1)[1].split();start=int(tail[19])
    assert int(b.decode().split(' ',1)[0])==pid
    status=private(out,'live-process-uid',f'grep "^Uid:" /proc/{pid}/status').decode().split()
    assert status==['Uid:','10216','10216','10216','10216']
    cmd=private(out,'live-process-cmdline',f'cat /proc/{pid}/cmdline')
    assert cmd.rstrip(b'\x00').decode()==PKG
    return start


def grant(out,remote,checkpoint_bytes,checkpoint,campaign,case,h):
    cp=checkpoint
    assert cp['campaign']==campaign and cp['case']==case
    assert cp['uid']==10216 and cp['bootId']==BOOT
    assert cp['appSha256']==APP_SHA and cp['testSha256']==h
    assert str(uuid.UUID(cp['ownerToken']))==cp['ownerToken']
    start=process_identity(out,cp['pid']);assert start==cp['startTicks']
    rows=target_processes(out,'live-processes')
    assert len(rows)==1 and rows[0]['pid']==cp['pid']
    fields=dict(campaign=campaign,case=case,ownerToken=cp['ownerToken'],pid=cp['pid'],uid=10216,
        startTicks=start,bootId=BOOT,checkpointSha256=digest(checkpoint_bytes),
        appSha256=APP_SHA,testSha256=h,action='SELF_ONLY')
    b=(json.dumps(fields,separators=(',',':'))+'\n').encode()
    put(out/'external-self-only-permit.json',fields)
    # Exclusive temp + close + atomic same-directory rename. The test only watches final name.
    text=f'run-as {PKG} sh -c \'set -e; test ! -e {remote}/permit.json; set -C; cat > {remote}/permit.tmp; mv -n {remote}/permit.tmp {remote}/permit.json\''
    command(out,'permit-publication',['shell',text],stdin=b)
    # exec-out preserves exact bytes. ADB shell's PTY may convert LF to CRLF.
    r=command(out,'permit-readback',['exec-out','run-as',PKG,'cat',remote+'/permit.json'])
    assert not r.stderr
    actual=r.stdout
    assert actual==b


def run_case(case):
    campaign,out,base=state();assert case in CASES
    h=json.loads((out/'test-update-proof.json').read_text(encoding='utf-8'))['testSha256']
    for previous in CASES[:CASES.index(case)]:
        assert json.loads((out/previous/'result.json').read_text(encoding='utf-8'))['status']=='PASS'
    here=out/case;here.mkdir()
    device(here,'before')
    identity(here,'before-app',PKG,APP_SHA,10216)
    identity(here,'before-test',TEST,h,10217)
    target_processes(here,'before-processes',True)
    assert protected(here,'before-protected')==base['protectedSha256']
    remote=f'no_backup/m12-t04-{campaign}/{case}/evidence'
    present=private(here,'before-owned-case',f'if test -e no_backup/m12-t04-{campaign}/{case}; then echo PRESENT; else echo ABSENT; fi')
    assert present.strip()==b'ABSENT', 'Case already exists: no retry'
    argv=instrument_args(campaign,case,h,'execute_self_termination')
    put(here/'execute-once.marker.json',dict(argv=argv,campaign=campaign,case=case,maxSelfKills=1))
    t=time.monotonic();started=dt.datetime.now(dt.timezone.utc).isoformat()
    with (here/'execute.stdout').open('xb') as stdout,(here/'execute.stderr').open('xb') as stderr:
        proc=subprocess.Popen(argv,stdout=stdout,stderr=stderr)
        cp_bytes=None;polls=[]
        deadline=t+100
        while time.monotonic()<deadline:
            if proc.poll() is not None: break
            q=subprocess.run([ADB,'-s',SERIAL,'shell',f'run-as {PKG} sh -c \'if test -f {remote}/checkpoint.json; then cat {remote}/checkpoint.json; else echo NOT_READY; fi\''],capture_output=True,timeout=15)
            polls.append(dict(exitCode=q.returncode,bytes=len(q.stdout),sha256=digest(q.stdout)))
            assert q.returncode==0 and not q.stderr, 'Checkpoint observation UNKNOWN'
            if q.stdout.strip()!=b'NOT_READY': cp_bytes=q.stdout;break
            time.sleep(.4)
        put(here/'checkpoint-polls.json',polls)
        assert cp_bytes, 'No checkpoint / early failure; no kill authorized and no retry'
        raw(here/'checkpoint.json',cp_bytes);cp=json.loads(cp_bytes)
        grant(here,remote,cp_bytes,cp,campaign,case,h)
        try: proc.wait(timeout=30)
        except subprocess.TimeoutExpired as e:
            put(here/'execute-unknown.json',dict(reason='Self-kill result TIMEOUT',hostPid=proc.pid))
            raise RuntimeError('Self termination UNKNOWN; preserve process/scene') from e
    stdout=(here/'execute.stdout').read_bytes();stderr=(here/'execute.stderr').read_bytes()
    put(here/'execute-command.json',dict(argv=argv,utc=started,seconds=round(time.monotonic()-t,3),
        exitCode=proc.returncode,stdoutBytes=len(stdout),stdoutSha256=digest(stdout),
        stderrBytes=len(stderr),stderrSha256=digest(stderr),expectedInterrupted=True))
    verify_case(campaign,out,base,case,h,cp)


def verify_case(campaign,out,base,case,h,cp):
    here=out/case;remote=f'no_backup/m12-t04-{campaign}/{case}/evidence'
    intent=json.loads(private(here,'kill-intent',f'cat {remote}/kill-intent.json'))
    assert intent['pid']==cp['pid'] and intent['startTicks']==cp['startTicks']
    assert intent['campaign']==campaign and intent['case']==case
    target_processes(here,'after-self-kill-processes',True)
    missing=private(here,'old-process-ended',f'if test -e /proc/{cp["pid"]}; then cat /proc/{cp["pid"]}/stat; else echo ABSENT; fi')
    assert missing.strip()==b'ABSENT', 'Old PID not proven ended; no restart'
    assert protected(here,'after-kill-protected')==base['protectedSha256']
    verify=instrument_args(campaign,case,h,'verify_after_restart')
    put(here/'verify-once.marker.json',dict(argv=verify,campaign=campaign,case=case))
    r=command(here,'verify',verify,timeout=90,adb=False)
    output=r.stdout.decode()
    assert 'OK (1 test)' in output and 'INSTRUMENTATION_CODE: -1' in output
    assert 'FAILURES' not in output and 'INSTRUMENTATION_FAILED' not in output
    proof=json.loads(private(here,'verification',f'cat {remote}/verification.json'))
    assert proof['campaign']==campaign and proof['case']==case and proof['status']=='PASS'
    assert proof['pid']!=cp['pid'] and proof['bootId']==BOOT and proof['uid']==10216
    assert protected(here,'after-verify-protected')==base['protectedSha256']
    identity(here,'after-app',PKG,APP_SHA,10216)
    target_processes(here,'after-processes',True)
    put(here/'result.json',dict(case=case,status='PASS',actualSelfKills=1,executePid=cp['pid'],
        executeStartTicks=cp['startTicks'],verifyPid=proof['pid'],verification=proof))
    print(json.dumps(dict(case=case,status='PASS',executePid=cp['pid'],verifyPid=proof['pid'])))


def continue_after_permit_proof():
    campaign,out,base=state();case='BEFORE_SWITCH';here=out/case
    h=json.loads((out/'test-update-proof.json').read_text(encoding='utf-8'))['testSha256']
    analysis=json.loads((here/'permit-readback-analysis.json').read_text(encoding='utf-8'))
    assert analysis['exactRawMatch'] and analysis['shellOnlyCRLF'] and analysis['exitCode']==0
    assert analysis['expectedSha256']==analysis['deviceSha256']==analysis['execOutSha256']
    assert not (here/'verify-once.marker.json').exists()
    put(here/'continue-after-exact-raw-proof.marker.json',dict(reason='Exact device bytes/hash proven; no kill/execute replay',case=case))
    stdout=(here/'execute.stdout').read_bytes();stderr=(here/'execute.stderr').read_bytes()
    assert b'INSTRUMENTATION_RESULT: shortMsg=Process crashed.' in stdout and b'INSTRUMENTATION_CODE: 0' in stdout
    put(here/'execute-command.json',dict(argv=json.loads((here/'execute-once.marker.json').read_text(encoding='utf-8'))['argv'],
        exitCode=None,exitCodeStatus='NOT_CAPTURED_AFTER_SUPERVISOR_ASSERTION',androidInstrumentationCode=0,
        stdoutBytes=len(stdout),stdoutSha256=digest(stdout),stderrBytes=len(stderr),stderrSha256=digest(stderr),
        expectedInterrupted=True,diagnostic='Original shell read gained one CR; exact exec-out/device SHA equals original permit; no repeat'))
    cp=json.loads((here/'checkpoint.json').read_text(encoding='utf-8'))
    verify_case(campaign,out,base,case,h,cp)


def finish():
    campaign,out,base=state()
    results=[json.loads((out/c/'result.json').read_text(encoding='utf-8')) for c in CASES]
    assert all(r['status']=='PASS' and r['actualSelfKills']==1 for r in results)
    h=json.loads((out/'test-update-proof.json').read_text(encoding='utf-8'))['testSha256']
    device(out,'final')
    app=identity(out,'final-app',PKG,APP_SHA,10216)
    test=identity(out,'final-test',TEST,h,10217)
    target_processes(out,'final-processes',True)
    protected_hash=protected(out,'final-protected')
    assert protected_hash==base['protectedSha256']
    pointer=json.loads(private(out,'final-pointer','cat no_backup/active-generation'))
    assert pointer==base['pointer']
    put(out/'final.json',dict(campaign=campaign,status='PASS',device=SERIAL,bootId=BOOT,app=app,test=test,
        actualSelfKills=3,appInstalls=0,testUpdates=1,avdCreates=0,avdStarts=0,avdStops=0,
        activeAppProcesses=0,defaultLedgerUnchanged=True,protectedSha256=protected_hash,
        defaultPointer=pointer,results=results,independentAcceptance='PENDING',m12Frozen=False))
    print(json.dumps(dict(status='PASS',cases=3,selfKills=3,appUnchanged=True,defaultLedgerUnchanged=True)))


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['baseline','baseline-continue','protect','install','confirm-update','case','verify-exact-permit','finish']);p.add_argument('--test-apk');p.add_argument('--case',choices=CASES);a=p.parse_args()
    if a.action.startswith('baseline'): baseline(a.action=='baseline-continue')
    elif a.action=='install': install(a.test_apk)
    elif a.action=='case': run_case(a.case)
    elif a.action=='protect': upgrade_protection()
    elif a.action=='confirm-update': finish_install()
    elif a.action=='verify-exact-permit': continue_after_permit_proof()
    elif a.action=='finish': finish()
