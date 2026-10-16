"""Isolated native CLI runner on the existing API36 emulator; no App access.

Runs only within a new, fixed /data/local/tmp research directory. Does not
install APKs, clear data, alter settings, or hide process failures as rejects.
"""
from __future__ import annotations
import argparse, datetime as dt, hashlib, json, re, shlex, subprocess, time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
ADB = Path('C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe')
SERIAL = 'emulator-5596'
REMOTE = '/data/local/tmp/lithic-m10-r1-20261016'
BASE = ROOT / 'build/m10-jpeg-r1'
EVIDENCE = ROOT / 'docs/research/m10-jpeg-r1/evidence'
LOCKS = {
    'complete.jpg': '2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907',
    'entropy_cut_eoi_retained.jpg': 'f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a',
}

def sha(p):
    h = hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda: f.read(1024 * 1024), b''): h.update(b)
    return h.hexdigest()

def adb(args, timeout=120):
    start = time.perf_counter()
    cmd = [str(ADB), '-s', SERIAL, *args]
    try:
        p = subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=timeout, check=False)
        return {'argv': cmd, 'exitCode': p.returncode, 'stdout': p.stdout.decode('utf-8', 'replace'),
                'stderr': p.stderr.decode('utf-8', 'replace'), 'hostWallSeconds': time.perf_counter()-start}
    except subprocess.TimeoutExpired as e:
        return {'argv': cmd, 'exitCode': None, 'supervisorStatus': 'TIMEOUT',
                'stdout': (e.stdout or b'').decode('utf-8', 'replace'), 'stderr': (e.stderr or b'').decode('utf-8', 'replace'),
                'hostWallSeconds': time.perf_counter()-start,
                'note': 'ADB timeout does not prove remote native termination; inspect owned process before continuing.'}

def checked(args, timeout=120):
    r = adb(args, timeout)
    if r['exitCode'] != 0: raise RuntimeError(json.dumps(r))
    return r

def remote_hash(name):
    return checked(['shell', 'sha256sum ' + shlex.quote(REMOTE + '/' + name)])['stdout'].split()[0]

def push(p, name):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', name): raise ValueError('Unsafe remote name')
    result = checked(['push', str(p), REMOTE + '/' + name])
    expected = sha(p)
    actual = remote_hash(name)
    if actual != expected: raise ValueError('Remote bytes differ')
    return {'localPath': str(p), 'remoteName': name, 'bytes': p.stat().st_size, 'sha256': expected, 'remoteSha256Readback':actual, 'push': result}

def classify(r):
    if r.get('supervisorStatus') == 'TIMEOUT': return r
    try:
        lines = r['stdout'].strip().splitlines()
        parsed = json.loads(lines[-1])
        expected = {'ACCEPT':0,'REJECT':10,'RESOURCE_REJECT':20,'CANCELLED':30,'INFRA_ERROR':40}
        if expected.get(parsed.get('status')) != r['exitCode']:
            r['supervisorStatus'] = 'INCONSISTENT_RESULT'
        else: r['supervisorStatus'] = 'STRUCTURED_' + parsed['status']
        r['native'] = parsed
    except (ValueError, IndexError):
        code = r.get('exitCode')
        if code is not None and code >= 128: r['supervisorStatus'] = 'CRASH_OR_SIGNAL_NO_JSON'
        else: r['supervisorStatus'] = 'LOAD_OR_DRIVER_ERROR_NO_JSON'
    return r

def run(mode, name, extra=(), timeout=120):
    argv = [REMOTE + '/strictjpeg_probe', '--mode', mode, *extra, REMOTE + '/' + name]
    return classify(adb(['shell', ' '.join(shlex.quote(x) for x in argv)], timeout))

def save(name, obj):
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    (EVIDENCE / name).write_text(json.dumps(obj, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', choices=['minimal','fixtures','collect-fixtures','expanded'])
    args = parser.parse_args()
    if args.stage == 'minimal':
        props = checked(['shell', 'getprop ro.build.version.sdk; getprop ro.product.cpu.abilist; getprop ro.kernel.qemu; id; getconf PAGESIZE; getprop ro.boot.qemu.avd_name'])
        lines = props['stdout'].splitlines()
        if lines[0] != '36' or 'x86_64' not in lines[1] or lines[2] != '1': raise RuntimeError('Device differs from approved API36 emulator')
        save('device-before.json', props)
        checked(['shell', 'mkdir ' + REMOTE]) # refuse silently reusing a previous directory
        binary = BASE / 'native/x86_64/probe/strictjpeg_probe'
        inputs = [push(binary, 'strictjpeg_probe')]
        checked(['shell', 'chmod 700 ' + REMOTE + '/strictjpeg_probe'])
        for name, expected in LOCKS.items():
            p = ROOT / 'app/src/androidTest/assets/m10_jpeg_integrity' / name
            if sha(p) != expected: raise ValueError('Locked fixture mismatch')
            inputs.append(push(p, name))
        cases = [{'mode':m,'input':n,**run(m,n)} for m in ('a2','a1') for n in LOCKS]
        save('minimal.json', {'inputs':inputs,'cases':cases})
        def matches(c):
            n = c.get('native', {})
            status = 'ACCEPT' if c['input'] == 'complete.jpg' else 'REJECT'
            if c.get('supervisorStatus') != 'STRUCTURED_' + status: return False
            if status == 'ACCEPT':
                return n.get('finishComplete') is True and n.get('warningCount') == 0
            return True
        good = all(matches(c) for c in cases)
        print(json.dumps({'minimalGate': 'PASS' if good else 'FAIL', 'cases': cases}, ensure_ascii=True))
        return 0 if good else 1
    if args.stage in ('fixtures','collect-fixtures'):
        generator = BASE / 'native/x86_64/probe/generate_fixtures'
        identity = push(generator, 'generate_fixtures')
        if args.stage == 'fixtures':
            checked(['shell', 'chmod 700 ' + REMOTE + '/generate_fixtures; mkdir ' + REMOTE + '/generated'])
            generation = adb(['shell', REMOTE + '/generate_fixtures ' + REMOTE + '/generated --large'], timeout=240)
            save('generation-command.json', generation)
            if generation['exitCode'] != 0: raise RuntimeError('Generation driver failed; retained generation-command.json; inspect remote process')
        else:
            no_process = checked(['shell', "ps -A -o PID,ARGS | grep '^[[:space:]]*[0-9]* generate_fixtures /data/local/tmp/lithic-m10-r1-20261016/generated' || true"])
            if no_process['stdout'].strip(): raise RuntimeError('Generator still owns output files')
            generation = {'exitCode':None, 'status':'OUTPUTS_RECOVERED_AFTER_DRIVER_TIMEOUT',
                          'originalSupervisorTimeoutSeconds':240,
                          'originalGeneratorPid':21974,'processNoLongerRunning':no_process,
                          'note':'Original generator exit unknown. Validate each complete file through decoders; do not claim generator exit 0.'}
        outdir = BASE / 'fixtures'
        outdir.mkdir(exist_ok=True)
        pulled = checked(['pull', REMOTE + '/generated/.', str(outdir)])
        manifest = [{'name':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(outdir.iterdir()) if p.is_file()]
        save('generated.json', {'generator':identity,'generation':generation,'pull':pulled,'outputs':manifest})
        print(json.dumps({'generatedCount':len(manifest)})); return 0
    # The minimal gate was already recorded before any expanded cases exist.
    minimal = json.loads((EVIDENCE / 'minimal.json').read_text(encoding='utf-8'))
    if len(minimal['cases']) != 4 or any(c['supervisorStatus'] != ('STRUCTURED_ACCEPT' if c['input']=='complete.jpg' else 'STRUCTURED_REJECT') for c in minimal['cases']):
        raise RuntimeError('Do not expand a failed minimal gate')
    journal = EVIDENCE / 'expanded.jsonl'
    cases, uploads = [], []
    with journal.open('x', encoding='utf-8', newline='\n') as output:
        def add(category, mode, name, extra=(), expectation='EXPLORATORY', timeout=120):
            c = {'category':category, 'mode':mode, 'input':name, 'options':list(extra),
                 'expectation':expectation, **run(mode,name,extra,timeout)}
            cases.append(c)
            output.write(json.dumps(c,ensure_ascii=False)+'\n'); output.flush()
            print(json.dumps({'category':category,'mode':mode,'input':name,'status':c['supervisorStatus']},ensure_ascii=True),flush=True)
            if c.get('supervisorStatus')=='TIMEOUT': raise RuntimeError('Remote timeout requires process investigation')
        # Generated originals already exist on device, copied to flat names only
        # in this owned research directory, and verified against host SHA.
        for p in sorted((BASE/'fixtures').glob('*.jpg')):
            uploads.append(push(p, p.name))
            for mode in ('a2','a1'): add('AUTHORED_HEALTHY',mode,p.name,expectation='ACCEPT')
        manifest = json.loads((BASE/'derived/mutation-manifest.json').read_text(encoding='utf-8'))
        for p in sorted((BASE/'derived').glob('*.jpg')):
            uploads.append(push(p,p.name))
            for mode in ('a2','a1'): add('DERIVED_BYTES',mode,p.name)
        for mode in ('a2','a1'):
            for name in ('complete.jpg','entropy_cut_eoi_retained.jpg'):
                add('REPEAT_100',mode,name,['--repeat','100'])
            add('ALLOC_FAULT',mode,'complete.jpg',['--fail-row-alloc'],expectation='RESOURCE_REJECT')
            add('MEMORY_LIMIT',mode,'large_40mp_progressive_444.jpg',['--memory-mib','1'],expectation='RESOURCE_REJECT')
            add('SCAN_LIMIT',mode,'progressive_rgb_420.jpg',['--scan-limit','1'],expectation='RESOURCE_REJECT')
        for deadline in (0,1,10,50):
            add('NATIVE_CANCEL_DEADLINE','a2','large_40mp_progressive_444.jpg',
                ['--cancel-after-ms',str(deadline)],expectation='CANCELLED')
        for count in (1,1000):
            add('NATIVE_CANCEL_PROGRESS','a2','large_40mp_progressive_444.jpg',
                ['--cancel-after-progress',str(count)],expectation='CANCELLED')
        add('A1_CANCEL_UNSUPPORTED','a1','complete.jpg',['--cancel-after-ms','0'],expectation='INFRA_ERROR')
        add('REPEAT_LARGE_3','a2','large_40mp_progressive_444.jpg',['--repeat','3'],expectation='ACCEPT')
        # Explicit environment errors must not be confused with damaged images.
        add('MISSING_INPUT','a2','intentionally_absent.jpg',expectation='INFRA_ERROR')
    save('expanded-summary.json', {'uploads':uploads,'cases':cases,
        'mutationManifestSha256':sha(BASE/'derived/mutation-manifest.json'),
        'device':SERIAL,'applicationInstallOrDataCommands':False})
    print(json.dumps({'expandedCaseCount':len(cases),'journalSha256':sha(journal)}))
    return 0

if __name__ == '__main__': raise SystemExit(main())
