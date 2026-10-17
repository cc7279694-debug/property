"""Offline analysis of locked repository files only. No subprocess/network/device API."""
from pathlib import Path
import hashlib
import json
import re

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[2]
EVIDENCE = HERE / 'evidence'
JAVA = 'docs/research/m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe/'
H2 = 'docs/research/m10-jpeg-r3-f1b/evidence/h2-20261017-01/'
SOURCE = '7612c0461df59fabf85d01d32e9b051eaab6bc1f'


def load(relative):
    return json.loads((REPO / relative).read_text(encoding='utf-8-sig'))


def save(name, value):
    (EVIDENCE / name).write_text(json.dumps(value, indent=2, ensure_ascii=True) + '\n', encoding='utf-8')


def main():
    java = ['ProbeActivity.java', 'ProbeRunner.java', 'ProbeSupport.java', 'ProbeApplication.java',
            'StageStore.java', 'WorkerService.java', 'SessionGate.java', 'StageAdmission.java',
            'AuthorizationFence.java', 'StageIdentity.java']
    data = ['normal-host-trace-6423.jsonl', 'h2-ending-worker-trace-6610.jsonl',
            'h2-ending-window-main.log', 'h2-ending-window-system.log', 'h2-ending-window-events.log',
            'normal-failure-result.json', 'h2-final-files.json', 'h2-final-stage-check.json',
            'g2-after-processes.json', 'h2-final-protection-processes.json', 'h2-final-services.json',
            'h2-final-protection-result.json', 'h2-source-correspondence.json', 'h2-command-audit.json',
            'h2-stopped-collection-error.json', 'h2-final-readonly-error.json', 'h2-delivery-sha256.json']
    aidl = 'docs/research/m10-jpeg-r3/host/probe/src/main/aidl/com/cooper/lithic/m10r3/probe/'
    paths = [JAVA + name for name in java] + [H2 + name for name in data]
    paths += [aidl + name for name in ('IHost.aidl', 'IWorker.aidl', 'SessionEnvelope.aidl')]
    manifest = {p: {'bytes': (REPO / p).stat().st_size,
                    'sha256': hashlib.sha256((REPO / p).read_bytes()).hexdigest()} for p in paths}
    # Verify existing H2 raw evidence against its archived manifest, not a regenerated fixture.
    archived = load(H2 + 'h2-delivery-sha256.json')['files']
    for p in paths:
        if p in archived:
            assert manifest[p] == archived[p], p
    build_inputs = load(H2 + 'h2-source-correspondence.json')['unchangedF1ABuildInputs']
    for p in paths:
        if p in build_inputs:
            assert manifest[p] == build_inputs[p], p
    save('input_manifest.json', {'sourceCommit': SOURCE, 'files': manifest, 'count': len(paths),
          'evidenceFreshness': 'ARCHIVED_H2_NOT_CURRENT_DEVICE', 'deviceCalls': 0})
    pattern = re.compile(r'^\s*(\d+\.\d+)\s+(\d+)\s+(\d+)\s+([A-Z])\s+([^:]+):\s?(.*)$')
    observations = []
    for filename in ('h2-ending-window-main.log', 'h2-ending-window-system.log', 'h2-ending-window-events.log'):
        for line_number, line in enumerate((REPO / H2 / filename).read_text().splitlines(), 1):
            match = pattern.fullmatch(line)
            assert match, (filename, line_number)
            stamp, pid, tid, level, tag, message = match.groups()
            value = {'source': H2 + filename, 'line': line_number, 'epoch': float(stamp),
                     'pid': int(pid), 'tid': int(tid), 'tag': tag.strip(), 'message': message}
            if tag.strip() == 'LITHIC_R3':
                value['researchEvent'] = json.loads(message)
            observations.append(value)
    save('timeline.json', {'observations': sorted(observations, key=lambda x: x['epoch']),
          'kind': 'DERIVED_FROM_LOCKED_SCOPED_LOGS_NOT_NEW_ANDROID_EVIDENCE'})
    research = [x['researchEvent'] for x in observations if 'researchEvent' in x]
    by_name = {x['event']: x for x in research if x['pid'] == 6423}
    failure = by_name['CASE_FAIL_STOP']
    pin = by_name['PIN_DURABLE']
    worker = [x for x in research if x['pid'] == 6610]
    created = next(x for x in worker if x['event'] == 'SERVICE_CREATED')
    retirement = next(x for x in worker if x['event'] == 'RETIRE_REQUESTED')
    assert retirement['reason'] == 'UNRESERVED_DEADLINE'
    assert load(H2 + 'normal-failure-result.json')['status'] == 'FAIL'
    assert not any(x['event'] in ('SYNC_HELLO', 'FD_GRANT_DECISION', 'SOURCE_VERIFIED', 'HOST_RESULT') for x in research)
    inventory = load(H2 + 'h2-final-files.json')
    current = inventory['json']['files/synthetic-stages/current.json']['attemptId']
    assert current == '586d0904-eaac-403a-895a-48c34cd98810'
    prefix = 'files/synthetic-stages/' + current + '/'
    state = inventory['json'][prefix + 'state.json']
    assert state['requestStatus'] == 'DELIVERY_UNKNOWN' and state['pin'] is True
    assert load(H2 + 'h2-final-stage-check.json')['status'] == 'BLOCKED'
    measurements = {
        'pin_to_fail_elapsed_ms': (failure['elapsedNs'] - pin['elapsedNs']) / 1e6,
        'worker_create_to_deadline_elapsed_ms': (retirement['elapsedNs'] - created['elapsedNs']) / 1e6,
        'worker_process_start_minus_fail_wall_ms': round((next(x['epoch'] for x in observations
             if x['tag'] == 'am_proc_start' and ',6610,10218,' in x['message']) - failure['wallMs'] / 1000) * 1000, 3),
        'scope': 'TIMESTAMP_DIFFERENCES_NOT_INDIVIDUAL_FSYNC_OR_BIND_DURATION'}
    def tid(pid, event):
        return next(x['tid'] for x in observations if x['pid'] == pid
                    and x.get('researchEvent', {}).get('event') == event)
    threads = {'host': tid(6423, 'APPLICATION_ROLE'), 'host_executor': tid(6423, 'CASE_BEGIN'),
               'host_death_callback_observed': tid(6423, 'WORKER_ENDPOINT_DIED'),
               'worker': tid(6610, 'SERVICE_CREATED'), 'worker_deadline_thread': tid(6610, 'RETIRE_REQUESTED')}
    save('analysis_results.json', {'status': 'PASS_OFFLINE_INPUT_AND_DERIVATION_ONLY', 'measurements': measurements,
          'threadIDs': threads,
          'h2Attempt': current, 'h2State': state['requestStatus'], 'h2Pin': state['pin'],
          'h2SourceSize': 8192, 'h2NormalResult': 'FAIL', 'anrBlockingStack': 'UNKNOWN_NOT_IN_ARCHIVE',
          'firstEndingUnknownProcessIdentity': 'UNKNOWN_ORIGINAL_ROW_NOT_ARCHIVED',
          'h2Lifecycle': 'BLOCKED', 'sourceAndRawEvidenceUnchanged': True, 'androidTests': 'NOT_RUN',
          'deviceCalls': 0, 'currentDeviceState': 'NOT_OBSERVED'})
    print(json.dumps({'inputs': len(paths), 'observations': len(observations), 'measurements': measurements,
                      'deviceCalls': 0, 'h2ResultUnchanged': 'FAIL/BLOCKED'}))


if __name__ == '__main__':
    main()
