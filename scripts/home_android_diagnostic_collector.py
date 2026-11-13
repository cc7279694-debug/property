"""Bounded logging and resource capture for the owned Android acceptance emulator."""

from datetime import datetime, timezone
import json
from pathlib import Path
import re
import subprocess as sp
import threading
import time


class AnrDetected(RuntimeError):
    pass


FILES = ('host-resource-samples.jsonl', 'guest-resource-samples.txt', 'logcat-all.txt',
         'logcat-events.txt', 'logcat-main-system.txt', 'logcat-crash.txt', 'anr-events.txt',
         'documentsui-process.txt', 'system-services.txt', 'anr-thread-dump.txt',
         'bugreport-status.txt', 'timeline.jsonl')
ANR = re.compile(r'^\s*(\d{9,}(?:\.\d+)?)\s+.*?\bam_anr\s*:\s*\[\s*\d+,\s*(\d+),\s*([^,]+),')
GUEST = '''date -u +%s.%N; echo uptime; cat /proc/uptime; echo meminfo; cat /proc/meminfo;
echo vmstat; cat /proc/vmstat; for p in memory cpu io; do echo pressure/$p;
if test -r /proc/pressure/$p; then cat /proc/pressure/$p; else echo unavailable; fi; done;
for p in com.android.documentsui com.android.systemui system_server; do echo "$p $(pidof "$p")"; done'''



PROCESS_NAMES = ('com.android.documentsui', 'com.android.systemui', 'system_server')
RESOURCE_SECTIONS = ('uptime', 'meminfo', 'vmstat', 'pressure/memory', 'pressure/cpu', 'pressure/io')
REPLAY_HEADER = re.compile(
    rb'^=== host_send_utc=\S+ host_recv_utc=\S+ mono_start=\S+ mono_end=\S+ exit=(-?\d+) ===$',
    re.M,
)


def validate_guest_sample(stdout, exit_code):
    """Validate the same complete shell output in live capture and offline replay."""
    if exit_code != 0:
        raise ValueError(f'Guest resource command exit={exit_code}')
    lines = stdout.decode('utf-8').splitlines()
    if not lines or re.fullmatch(r'\d{9,}\.\d+', lines[0]) is None:
        raise ValueError('Missing or invalid guest epoch')
    if any(lines.count(section) != 1 for section in RESOURCE_SECTIONS):
        raise ValueError('Missing or duplicate guest resource section')
    uptime, meminfo, vmstat, pressure_memory, pressure_cpu, pressure_io = (
        lines.index(section) for section in RESOURCE_SECTIONS
    )
    if not (uptime == 1 and meminfo == 3 < vmstat < pressure_memory < pressure_cpu < pressure_io):
        raise ValueError('Unexpected guest resource section order or incomplete uptime')
    if re.fullmatch(r'\d+\.\d+ \d+\.\d+', lines[2]) is None:
        raise ValueError('Invalid guest uptime')
    for label in ('MemTotal', 'MemAvailable', 'SwapTotal', 'SwapFree'):
        if sum(re.fullmatch(rf'{label}:\s+\d+ kB', row) is not None
               for row in lines[meminfo+1:vmstat]) != 1:
            raise ValueError(f'Missing or invalid meminfo field {label}')
    for label in ('pswpin', 'pswpout', 'oom_kill'):
        if sum(re.fullmatch(rf'{label} \d+', row) is not None
               for row in lines[vmstat+1:pressure_memory]) != 1:
            raise ValueError(f'Missing or invalid vmstat field {label}')
    if not stdout.endswith(b'\n'):
        raise ValueError('Guest resource output has no final line terminator')
    processes = lines[-len(PROCESS_NAMES):]
    for name, row in zip(PROCESS_NAMES, processes):
        if re.fullmatch(rf'{re.escape(name)}(?: \d+)* ?', row) is None:
            raise ValueError(f'Missing or invalid terminal PID row for {name}')
    for start, end in ((pressure_memory, pressure_cpu), (pressure_cpu, pressure_io),
                       (pressure_io, len(lines)-len(PROCESS_NAMES))):
        if start+1 >= end:
            raise ValueError(f'Missing value for {lines[start]}')
    return processes


def replay_guest_samples(raw):
    """Replay the raw recorder format without changing or rewriting the evidence."""
    headers = list(REPLAY_HEADER.finditer(raw))
    if not headers or headers[0].start() != 0 or raw.count(b'=== host_send_utc=') != len(headers):
        raise ValueError('Guest resource receipt framing is incomplete')
    result = {'raw_samples': len(headers), 'command_success': 0, 'crlf_samples': 0,
              'valid_samples': 0, 'invalid_samples': 0, 'failures': []}
    for index, header in enumerate(headers):
        end = headers[index+1].start() if index+1 < len(headers) else len(raw)
        # The recorder adds one LF after the header and one after the untouched stdout.
        body = raw[header.end()+1:end]
        exit_code = int(header[1])
        result['command_success'] += exit_code == 0
        try:
            if not body.endswith(b'\n'):
                raise ValueError('Truncated guest receipt framing')
            stdout = body[:-1]
            result['crlf_samples'] += b'\r\n' in stdout
            validate_guest_sample(stdout, exit_code)
        except ValueError as exc:
            result['failures'].append({'sample': index+1, 'reason': str(exc)})
        else:
            result['valid_samples'] += 1
    result['invalid_samples'] = len(result['failures'])
    return result


def utc():
    return datetime.now(timezone.utc).isoformat()


class Collector:
    def __init__(self, controller, root, host_sampler):
        self.controller = controller
        self.root = Path(root)
        self.host_sampler = host_sampler
        for name in FILES:
            (self.root/name).touch(exist_ok=False)
        self.done = threading.Event()
        self.stop_sampling = threading.Event()
        self.early_serial = None
        self.early_verified = False
        self.workers = []
        self.children = []
        self.lock = threading.RLock()
        self.anr = None
        self.broken = None
        self.stream_count = 0
        self.guest_samples = 0
        self.raw_guest_samples = 0
        self.host_samples = 0
        self.snapshot_complete = False
        self.seen_events = set()
        self.deadline = time.monotonic() + 1800
        self.note('capture-prepared', maximum_seconds=1800, interval_seconds=5)

    def note(self, kind, **fields):
        row = dict(kind=kind, utc=utc(), monotonic=time.monotonic(), **fields)
        self.write('timeline.jsonl', json.dumps(row, ensure_ascii=False)+'\n')

    def write(self, name, text):
        with self.lock, (self.root/name).open('a', encoding='utf-8', newline='\n') as stream:
            stream.write(text)

    def spawn(self, target, *args):
        thread = threading.Thread(target=target, args=args, daemon=True)
        self.workers.append(thread)
        thread.start()

    def start_host(self):
        self.spawn(self.samples, 'host', self.host_sampler)

    def owned_ready(self, serial):
        self.early_serial = serial
        self.note('first-owned-device', serial=serial)
        self.spawn(self.start_guest)

    def fail(self, reason):
        with self.lock:
            if self.broken is None:
                self.broken = reason
        self.note('capture-failure', reason=reason)

    def start_guest(self):
        try:
            self.controller.confirm_early_device(self.early_serial)
            self.early_verified = True
            self.note('early-guest-identity', serial=self.early_serial)
            # Start reading from the head of the current boot ring, before the snapshots return.
            for name, buffers in [('logcat-all.txt', ('all',)), ('logcat-events.txt', ('events',)),
                                  ('logcat-main-system.txt', ('main','system')), ('logcat-crash.txt', ('crash',))]:
                args = [item for b in buffers for item in ('-b', b)]
                self.spawn(self.stream, name, args)
            self.spawn(self.snapshot)
            self.spawn(self.samples, 'guest', self.sample_guest)
        except Exception as exc:
            self.fail(str(exc))

    def snapshot(self):
        try:
            output = self.controller.adb_readonly(self.early_serial, 'logcat', '-b', 'all', '-d', '-v', 'epoch', timeout=15)
            if output.returncode or len(output.stdout) > 12*1024**2:
                raise RuntimeError(f'Initial ring snapshot unavailable, exit={output.returncode}, bytes={len(output.stdout)}')
            self.write('logcat-first-device.txt', output.stdout.decode('utf-8', errors='replace'))
            for line in output.stdout.decode('utf-8', errors='replace').splitlines():
                self.inspect_event(line)
            self.snapshot_complete = True
            self.note('initial-ring-saved', bytes=len(output.stdout))
        except Exception as exc:
            self.fail(str(exc))

    def stream(self, name, args):
        proc = None
        try:
            proc = self.controller.adb_stream(self.early_serial, 'logcat', *args, '-v', 'epoch')
            with self.lock:
                self.children.append(proc)
                self.stream_count += 1
            self.note('stream-open', file=name, pid=proc.pid)
            size = 0
            with (self.root/name).open('ab', buffering=0) as output:
                for line in iter(proc.stdout.readline, b''):
                    if self.done.is_set():
                        break
                    size += len(line)
                    if size > 24*1024**2 or time.monotonic() >= self.deadline:
                        raise RuntimeError(f'Capture limit reached for {name}')
                    output.write(line)
                    if name in ('logcat-events.txt', 'logcat-all.txt'):
                        self.inspect_event(line.decode('utf-8', errors='replace'))
            if not self.done.is_set():
                raise RuntimeError(f'{name} stream ended before cleanup, exit={proc.poll()}')
        except Exception as exc:
            if not self.done.is_set():
                self.fail(str(exc))
        finally:
            if proc is not None:
                self.stop_child(proc)

    def inspect_event(self, line):
        if re.search(r'\bam_anr\s*:', line) is None:
            return
        match = ANR.search(line)
        if match is None:
            self.fail('An unparseable am_anr event was captured')
            return
        epoch, pid, package = match.groups()
        item = {'epoch': float(epoch), 'pid': int(pid), 'package': package.strip(), 'line': line.strip()}
        with self.lock:
            key = (item['epoch'], item['pid'], item['package'])
            if key in self.seen_events:
                return
            self.seen_events.add(key)
            self.write('anr-events.txt', json.dumps(item, ensure_ascii=False)+'\n')
            if self.anr is None:
                self.anr = item
                self.note('first-anr', **item)
                self.controller.dispatch_anr(item)

    def require_healthy_capture(self):
        if self.anr is not None:
            raise AnrDetected(f"Observed ANR for {self.anr['package']} PID {self.anr['pid']}")
        if self.broken is not None:
            raise RuntimeError(self.broken)
        if time.monotonic() >= self.deadline:
            raise RuntimeError('Diagnostic observation exceeded the collection deadline')

    def samples(self, kind, callback):
        while not self.done.is_set() and not self.stop_sampling.is_set() and self.anr is None:
            start = time.monotonic()
            try:
                callback()
                if kind == 'host':
                    self.host_samples += 1
            except Exception as exc:
                self.note(f'{kind}-sample-failed', error=str(exc))
            if self.done.wait(max(0, 5-(time.monotonic()-start))):
                return

    def sample_guest(self):
        sent, started = utc(), time.monotonic()
        result = self.controller.adb_readonly(self.early_serial, 'shell', GUEST, timeout=5)
        finished, received = time.monotonic(), utc()
        text = result.stdout.decode('utf-8', errors='replace')
        self.write('guest-resource-samples.txt',
                   f'=== host_send_utc={sent} host_recv_utc={received} mono_start={started} mono_end={finished} exit={result.returncode} ===\n{text}\n')
        self.raw_guest_samples += 1
        processes = validate_guest_sample(result.stdout, result.returncode)
        self.guest_samples += 1
        self.write('documentsui-process.txt', f'{received} {started} '+
                   ' / '.join(processes)+'\n')

    @staticmethod
    def stop_child(proc):
        try:
            if proc.poll() is None:
                proc.terminate()
                proc.wait(timeout=3)
        except (OSError, sp.TimeoutExpired):
            if proc.poll() is None:
                proc.kill()
                proc.wait(timeout=3)

    def close(self):
        self.done.set()
        for proc in tuple(self.children):
            self.stop_child(proc)
        for thread in tuple(self.workers):
            thread.join(timeout=8)
        for proc in tuple(self.children):
            self.stop_child(proc)
        remaining = [thread.name for thread in self.workers if thread.is_alive()]
        self.note('capture-closed', streams=self.stream_count, raw_guest_samples=self.raw_guest_samples,
                  guest_samples=self.guest_samples,
                  running_workers=remaining, running_streams=[p.pid for p in self.children if p.poll() is None])
        if remaining or any(p.poll() is None for p in self.children):
            raise RuntimeError('Could not confirm closure of owned capture workers')
