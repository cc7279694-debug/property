"""H1: one explicit Outsider Activity, bounded full-buffer log observation.

No force-stop, normal, worker start, cancellation, retry or log clearing.
The external runId is an Intent action/evidence label; the APK ignores extras.
"""
import hashlib
import json
import re
import time
import uuid

import actions as A

V, OUT = A.V, A.OUT
BUFFERS = ('main', 'system', 'crash', 'events')
LINE = re.compile(r'^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+([A-Z])\s+([^:]+):\s?(.*)$')


class OneLaunch(A.H1):
    def command(self, args, **kwargs):
        if args[3:5] != ['shell', 'am']:
            return super().command(args, **kwargs)
        issued = A.load('outsider-launch-issued.json')
        A.need(args == [str(V.S.ADB), '-s', V.S.SERIAL, 'shell', 'am', 'start', '-W',
                       '-a', issued['action'], '-n', V.S.OUTSIDER + '/.OutsiderActivity'],
               'only the unique explicit Outsider launch is authorized')
        result = V.D.Diagnostic.command(self, args, **kwargs)
        with (OUT / 'h1-actions-integrity.jsonl').open('a', encoding='utf-8', newline='\n') as handle:
            handle.write(json.dumps({'atUtc': V.S.utc_now(), 'phase': self.phase, 'label': kwargs['label'],
                'argv': args, 'exitCode': result.returncode, 'stdoutBytes': len(result.stdout),
                'stdoutSHA256': hashlib.sha256(result.stdout).hexdigest(), 'stderrBytes': len(result.stderr),
                'stderrSHA256': hashlib.sha256(result.stderr).hexdigest()}) + '\n')
        return result


def epoch(d, label):
    value = d.text('shell', 'date', '+%s.%N', label=label)
    A.need(re.fullmatch(r'[0-9]+\.[0-9]{9}', value), 'device clock format unknown')
    return value


def read_buffer(d, buffer, phase, start=None, end=None, pid=None):
    result = d.adb('logcat', '-d', '-b', buffer, '-v', 'epoch',
                   label=phase + '-full-' + buffer, private_metadata=True)
    A.need(result.returncode == 0 and not result.stderr and len(result.stdout) <= 32 * 1024 * 1024,
           'log buffer read failed/oversize')
    text = result.stdout.decode('utf-8', errors='strict')
    A.need(not text or text.endswith('\n'), 'log buffer output incomplete')
    times, selected, parsed, bad = [], [], [], 0
    for line in text.splitlines():
        if not line.strip() or line.startswith('--------- '):
            continue
        match = LINE.fullmatch(line)
        if match is None:
            bad += 1
            continue
        stamp, process, thread, level, tag, message = match.groups()
        stamp, process, tag = float(stamp), int(process), tag.strip()
        times.append(stamp)
        if start is None or not float(start) <= stamp <= float(end):
            continue
        related = ((process == pid and tag in ('LITHIC_R3', 'ActivityThread', 'AndroidRuntime')) or
                   (tag in ('ActivityManager', 'ActivityTaskManager', 'am_proc_start', 'am_proc_bound',
                            'am_proc_died', 'am_kill', 'wm_create_activity', 'wm_on_create_called',
                            'wm_on_destroy_called', 'wm_destroy_activity', 'wm_finish_activity',
                            'wm_restart_activity', 'wm_set_resumed_activity') and
                    (V.S.OUTSIDER in message or V.G.SERVICE.PROCESS in message or
                     V.S.PROBE + '/.WorkerService' in message)))
        if related:
            selected.append(line)
            parsed.append({'epoch': stamp, 'pid': process, 'tag': tag, 'message': message})
    value = {'buffer': buffer, 'exitCode': result.returncode, 'bytes': len(result.stdout),
             'sha256': hashlib.sha256(result.stdout).hexdigest(), 'parsedEntries': len(times),
             'oldestEpoch': min(times) if times else None, 'latestEpoch': max(times) if times else None,
             'unparsedLines': bad, 'fullOutputArchived': False, 'selectedEntries': len(selected)}
    A.save(phase + '-buffer-' + buffer + '.json', value)
    A.need(bad == 0, 'unknown log format; no complete-window claim')
    if selected:
        with (OUT / ('outsider-window-' + buffer + '.log')).open('x', encoding='utf-8', newline='\n') as handle:
            handle.write('\n'.join(selected) + '\n')
    return value, parsed


def main():
    A.need(not (OUT / 'h1-blocked.json').exists(), 'prior blocker; no launch')
    for module in ('probe', 'outsider'):
        A.need(A.load('install-' + module + '-result.json')['status'] == 'PASS', 'both verified APKs required')
    run_id = str(uuid.uuid4())
    A.save('outsider-test-attempt.json', {'atUtc': V.S.utc_now(), 'runId': run_id,
           'runIdScope': 'EXTERNAL_EVIDENCE_AND_INTENT_ACTION_ONLY_NOT_APP_RESULT', 'maximumLaunches': 1})
    d = OneLaunch(OUT, 'outsider-test')
    launched = False
    outcome = None
    error = None
    baseline = {}
    start = end = None
    try:
        idle = A.check_idle(d, 'before-outsider-test')
        for module in ('probe', 'outsider'):
            facts = A.load('h1-host-apk-audit.json')['apks'][module]
            A.need(idle['packages'][facts['package']]['apks'][0]['sha256'] == facts['sha256'], 'target APK not installed')
        old = A.outsider_result(d, 'before-outsider-test-result')
        for buffer in BUFFERS:
            baseline[buffer], _ = read_buffer(d, buffer, 'before-outsider')
        start = epoch(d, 'outsider-window-start')
        action = 'com.cooper.lithic.m10r3.H1.' + run_id
        A.save('outsider-launch-issued.json', {'atUtc': V.S.utc_now(), 'runId': run_id,
                 'action': action, 'deviceEpochStart': start, 'noRetry': True})
        launched = True
        result = d.command([str(V.S.ADB), '-s', V.S.SERIAL, 'shell', 'am', 'start', '-W', '-a', action,
                            '-n', V.S.OUTSIDER + '/.OutsiderActivity'], label='only-outsider-launch', timeout=45)
        A.need(result.returncode == 0 and b'Status: ok' in result.stdout and not result.stderr,
               'Outsider launch failed/unknown; no repeat')
        limit = time.monotonic() + 12
        index = 0
        while time.monotonic() < limit:
            index += 1
            outcome = A.outsider_result(d, 'outsider-observed-result-' + str(index))
            if outcome['present'] and outcome['result'].get('phase') == 'onDestroy':
                obj = outcome['result']
                if not old['present'] or obj.get('pid') != old['result'].get('pid') or outcome['sha256'] != old['sha256']:
                    break
            time.sleep(0.5)
        A.need(outcome and outcome['present'] and outcome['result'].get('phase') == 'onDestroy',
               'new durable destruction result not observed')
        obj = outcome['result']
        A.need(obj.get('uid') == A.current_uids()[V.S.OUTSIDER] and type(obj.get('pid')) is int,
               'result process identity unknown')
        A.need(obj.get('status') == 'PASS' and obj.get('bindingOutcome') == 'DENIED' and
               obj.get('bindingDenied') is True and obj.get('cleanupStatus') in ('RELEASED', 'NOT_REGISTERED') and
               'firstUnexpectedCleanupError' not in obj, 'denial/connection cleanup failed')
        # F0 leak appeared after super.onDestroy. Observe a full 10 seconds after
        # the new durable onDestroy result, without changing any app/process.
        time.sleep(10)
    except Exception as stopped:
        error = {'type': type(stopped).__name__, 'detail': str(stopped)}
    if launched:
        try:
            end = epoch(d, 'outsider-window-end')
            pid = outcome['result']['pid'] if outcome and outcome.get('present') else None
            events, coverage = [], {}
            for buffer in BUFFERS:
                after, entries = read_buffer(d, buffer, 'after-outsider', start, end, pid)
                events.extend(entries)
                before = baseline[buffer]
                retained = ((after['oldestEpoch'] is not None and after['oldestEpoch'] <= float(start)) or
                            (before['parsedEntries'] == 0 and after['parsedEntries'] == 0))
                coverage[buffer] = {'status': 'PASS' if retained else 'UNKNOWN',
                    'oldestRetainedAtOrBeforeStart': after['oldestEpoch'] is not None and after['oldestEpoch'] <= float(start),
                    'emptyBeforeAndAfter': before['parsedEntries'] == after['parsedEntries'] == 0,
                    'rawBytes': after['bytes'], 'rawSHA256': after['sha256']}
                A.need(retained, 'log buffer may not retain complete time window: ' + buffer)
            app = [x for x in events if x['tag'] == 'LITHIC_R3' and x['pid'] == pid]
            created = [x for x in app if '"phase":"onCreate"' in x['message']]
            destroyed = [x for x in app if '"phase":"onDestroy"' in x['message']]
            system_destroy = [x for x in events if x['tag'] == 'wm_on_destroy_called' and x['pid'] == pid]
            denied = [x for x in events if x['tag'] == 'ActivityManager' and 'Permission Denial:' in x['message'] and
                      ('pid=' + str(pid)) in x['message'] and 'uid=10219' in x['message'] and 'not exported from uid 10218' in x['message']]
            process_start = [x for x in events if x['tag'] == 'am_proc_start' and
                             re.search(r'\[0,' + str(pid) + r',10219,' + re.escape(V.S.OUTSIDER) + r',', x['message'])]
            leaks = [x for x in events if 'ServiceConnectionLeaked' in x['message'] or 'has leaked ServiceConnection' in x['message']]
            bound = [x for x in events if 'SAFETY_OUTSIDER_BOUND' in x['message'] or
                     (x['tag'] == 'am_proc_start' and V.G.SERVICE.PROCESS in x['message'])]
            A.need(created and destroyed and system_destroy and denied and process_start,
                   'required actual lifecycle/identity/denial logs missing')
            A.need(float(end) - max(x['epoch'] for x in system_destroy) >= 9.5, 'post-destruction log observation too short')
            A.need(not leaks and not bound, 'connection leak or unauthorized Worker activity observed')
            A.save('outsider-lifecycle-result.json', {'status': 'PASS' if error is None else 'BLOCKED',
                'runId': run_id, 'externalIdentityOnly': True, 'pid': pid, 'uid': A.current_uids()[V.S.OUTSIDER],
                'deviceEpochStart': start, 'deviceEpochEnd': end, 'bufferCoverage': coverage,
                'onCreateEvidence': created, 'onDestroyEvidence': destroyed, 'systemDestroyEvidence': system_destroy,
                'systemDenialEvidence': denied, 'processStartEvidence': process_start,
                'leakEvents': leaks, 'unauthorizedBoundEvents': bound, 'durableResult': outcome,
                'priorError': error, 'scope': 'ONE_OUTSIDER_ACTIVITY_ONLY_NO_NORMAL'})
        except Exception as stopped:
            error = error or {'type': type(stopped).__name__, 'detail': str(stopped)}
    if error:
        A.save('h1-blocked.json', {'atUtc': V.S.utc_now(), 'phase': 'outsider-test', 'status': 'BLOCKED',
                                 'detail': error, 'launchedOnce': launched, 'noRepeat': True})
        print('Outsider H1 BLOCKED; preserve evidence and perform only ending protection')
        return 2
    print('Single Outsider denial, cleanup, destruction and bounded no-leak observation PASS')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
