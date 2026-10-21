import datetime, hashlib, json, pathlib, re, subprocess, sys, time

ROOT = next(p for p in pathlib.Path(__file__).resolve().parents if (p/'gradlew.bat').is_file())
ADB = r'C:/Users/CDD/AppData/Local/MirraAndroid/sdk/platform-tools/adb.exe'
SERIAL = 'emulator-5692'
RUNNER = 'com.cooper.lithic.debug.test/com.cooper.lithic.app.M13IsolatedRunner'
SCOPES = {
 'core': ['com.cooper.lithic.data.repository.M18TimelineRepositoryTest','com.cooper.lithic.feature.review.M18TotalCostTrendViewModelTest'],
 'ui': ['com.cooper.lithic.ui.M18TotalCostTrendSemanticsTest','com.cooper.lithic.app.M18TotalCostTrendJourneyTest'],
 'journey': ['com.cooper.lithic.app.M18TotalCostTrendJourneyTest'],
 'all': ['com.cooper.lithic.data.repository.M18TimelineRepositoryTest','com.cooper.lithic.feature.review.M18TotalCostTrendViewModelTest','com.cooper.lithic.ui.M18TotalCostTrendSemanticsTest','com.cooper.lithic.app.M18TotalCostTrendJourneyTest'],
}
artifact_phase = sys.argv[3] if len(sys.argv)>3 else 'initial'
artifact_identity = json.loads((ROOT/('docs/evidence/m18-android/install-'+artifact_phase)/'artifacts.json').read_text(encoding='utf-8'))
CLASSES=SCOPES[sys.argv[1]]
out = pathlib.Path(sys.argv[2]).resolve()
out.mkdir(parents=True, exist_ok=False)
summary = []
for selector in CLASSES:
    cls,_,method=selector.partition('#')
    source = ROOT / 'app/src/androidTest/java' / (cls.replace('.', '/') + '.kt')
    expected = re.findall(r'@Test\s+fun\s+(\w+)\s*\(', source.read_text(encoding='utf-8'))
    if method: expected=[x for x in expected if x==method]
    assert expected
    cmd = [ADB, '-s', SERIAL, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', selector, RUNNER]
    item = dict(command=cmd, source=str(source.relative_to(ROOT)),
                source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                started_at=datetime.datetime.now(datetime.timezone.utc).isoformat(), expected=expected, apk_identity=artifact_identity)

    prefix = out / cls.rsplit('.', 1)[1]
    prefix.with_suffix('.claim.json').write_text(json.dumps(item, ensure_ascii=False, indent=2), encoding='utf-8')
    start = time.monotonic()
    process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        stdout, stderr = process.communicate(timeout=600)
    except subprocess.TimeoutExpired:
        item.update(status='UNKNOWN', reason='600s supervision timeout; app not terminated', adb_pid=process.pid)
        prefix.with_suffix('.result.json').write_text(json.dumps(item, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps(item, ensure_ascii=False), flush=True)
        raise SystemExit(2)
    prefix.with_suffix('.stdout.txt').write_bytes(stdout)
    prefix.with_suffix('.stderr.txt').write_bytes(stderr)
    text = stdout.decode('utf-8', errors='strict')
    records, current = [], {}
    for line in text.splitlines():
        if line.startswith('INSTRUMENTATION_STATUS: '):
            key, sep, value = line[len('INSTRUMENTATION_STATUS: '):].partition('=')
            if sep: current[key] = value
        elif line.startswith('INSTRUMENTATION_STATUS_CODE: '):
            records.append(dict(current, status_code=int(line.split(': ', 1)[1])))
            current = {}
    completed = [r['test'] for r in records if r.get('class') == cls and r.get('status_code') == 0]
    bad = [r for r in records if r['status_code'] in (-1, -2, -3, -4)]
    passed = (process.returncode == 0 and not stderr and not bad and
              sorted(completed) == sorted(expected) and len(completed) == len(set(completed)) and
              re.search(r'OK \(' + str(len(expected)) + r' tests?\)', text) is not None and
              'INSTRUMENTATION_CODE: -1' in text)
    item.update(status='PASS' if passed else 'FAIL_OR_UNKNOWN', exit_code=process.returncode,
                seconds=round(time.monotonic()-start, 3), completed=completed, bad=bad,
                stdout_bytes=len(stdout), stdout_sha256=hashlib.sha256(stdout).hexdigest(),
                stderr_bytes=len(stderr), stderr_sha256=hashlib.sha256(stderr).hexdigest(), records=records)
    prefix.with_suffix('.result.json').write_text(json.dumps(item, ensure_ascii=False, indent=2), encoding='utf-8')
    summary.append(item)
    (out/'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    print(f'{cls}: {item["status"]} {len(completed)}/{len(expected)} ({item["seconds"]}s)', flush=True)
    if not passed:
        print(text[-14000:], flush=True)
        raise SystemExit(1)
