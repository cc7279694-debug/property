"""Bounded, single-method, fixed-serial log observer; import has no side effects.

The observer starts before instrumentation, preserves its entire filtered stream
(including backlog), and drains both pipes after a controlled HOST observer stop.
No Android process is stopped and no log buffer is cleared or reconfigured.

A PASS proves this observer's recorded window, not that Android logd can never
drop an unreported message. New captures authenticate an end-to-end nonce sent
after the reader starts; legacy V1 evidence retains its original header contract.
Neither condition substitutes for six-phase/attempt/identity verification.
"""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import math
import os
from pathlib import Path
import re
import secrets
import subprocess
import threading
import time


FORMAT = "LITHIC_SINGLE_METHOD_CONTINUOUS_LOG_V1"
SERIALS = frozenset({"emulator-5624", "emulator-5626"})
HEADERS = (b"--------- beginning of main\n", b"--------- beginning of main\r\n")
TAIL = ["logcat", "-b", "main", "-v", "threadtime", "-s", "LITHIC_JPEG:I", "*:S"]
HANDSHAKE_PROTOCOL = "DIAGNOSTIC_NONCE_V1"
DIAGNOSTIC_TAG = "LITHIC_M10_CAPTURE"
HANDSHAKE_TAIL = TAIL[:-1] + [DIAGNOSTIC_TAG + ":I", "*:S"]
DIAGNOSTIC_LINE = re.compile(
    r"\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3,9} +[1-9][0-9]* +[1-9][0-9]* I "
    + DIAGNOSTIC_TAG + r": M10_CAPTURE_READY nonce=([0-9a-f]{32})\r?\n\Z")
DROP = re.compile(rb"logcat:|unexpected eof|buffer.{0,20}overflow|"
                  rb"\bdropped\b|\blost [0-9]+\b|\bchatty:|\btruncated\b", re.I)


def stamp():
    # Python 3.12's Windows monotonic clock can have a coarse tick resolution;
    # perf_counter is also monotonic and supplies distinct observed boundaries.
    return {"utc": datetime.now(timezone.utc).isoformat(), "monotonicNs": time.perf_counter_ns()}


def clone(value):
    return json.loads(json.dumps(value))


def need(condition, detail):
    if not condition:
        raise ValueError(detail)


def stream_errors(raw, final=False, require_header=True):
    errors = []
    if DROP.search(raw):
        errors.append("VISIBLE_STREAM_LOSS_OR_DIAGNOSTIC")
    if raw.count(b"--------- beginning of main") > 1:
        errors.append("DUPLICATE_MAIN_BUFFER_HEADER")
    if b"\x00" in raw:
        errors.append("STDOUT_NUL_BYTE")
    try:
        raw.decode("utf-8", errors="strict")
    except UnicodeDecodeError as error:
        if final or error.reason != "unexpected end of data":
            errors.append("STDOUT_INVALID_UTF8")
    if final:
        if require_header and not any(raw.startswith(header) for header in HEADERS):
            errors.append("MAIN_BUFFER_HEADER_NOT_PROVEN")
        if not require_header and b"--------- beginning of main" in raw and not any(
                raw.startswith(header) for header in HEADERS):
            errors.append("MAIN_BUFFER_HEADER_POSITION_UNKNOWN")
        if not raw.endswith(b"\n"):
            errors.append("STDOUT_PARTIAL_FINAL_LINE")
    return errors


def diagnostic_state(raw, nonce, prior_diagnostics):
    """Parse complete lines only; unknown, duplicate or reordered tags fail closed."""
    seen = set()
    current = None
    for line in raw.decode("utf-8", errors="strict").splitlines(keepends=True):
        if not line.endswith("\n") or DIAGNOSTIC_TAG not in line:
            continue
        match = DIAGNOSTIC_LINE.fullmatch(line)
        need(match is not None, "HANDSHAKE_MALFORMED_DIAGNOSTIC")
        need(line not in seen, "HANDSHAKE_DUPLICATE_DIAGNOSTIC")
        seen.add(line)
        if match[1] == nonce:
            need(current is None, "HANDSHAKE_DUPLICATE_DIAGNOSTIC")
            current = line
        else:
            need(line in prior_diagnostics, "HANDSHAKE_UNEXPECTED_DIAGNOSTIC")
            need(current is None, "HANDSHAKE_LATE_PRIOR_DIAGNOSTIC")
    return current


class ContinuousLogCapture:
    """One owned HOST adb reader. start/assert_live/finish return evidence dicts.

    ``popen_factory`` is a test seam for synthetic HOST processes only; it does
    not change the command used in actual supervision. ``finish`` terminates
    only the Popen handle created here, never a PID selected from a process list.
    Failure to stop/drain is UNKNOWN; there is no kill escalation or retry.
    """

    def __init__(self, adb_path, serial, directory, *, env=None,
                 max_stdout_bytes=2 * 1024 * 1024, max_stderr_bytes=65536,
                 max_seconds=360, readiness_timeout=10, stop_timeout=3,
                 popen_factory=subprocess.Popen, handshake_writer=None, nonce=None,
                 prior_diagnostics=()):
        need(serial in SERIALS, "Observer serial outside isolated natural-test environment")
        for bound, ceiling in ((max_stdout_bytes, 2 * 1024 * 1024),
                               (max_stderr_bytes, 65536)):
            need(type(bound) is int and 0 < bound <= ceiling, "Invalid raw byte bound")
        for bound, ceiling in ((max_seconds, 600), (readiness_timeout, 30), (stop_timeout, 10)):
            need(type(bound) in (int, float) and math.isfinite(bound) and 0 < bound <= ceiling,
                 "Invalid observer time bound")
        need(readiness_timeout <= max_seconds, "Readiness exceeds total observer bound")
        self.adb_path = Path(adb_path).resolve()
        self.serial = serial
        self.directory = Path(directory)
        need(handshake_writer is None or callable(handshake_writer), "Invalid handshake writer")
        self.handshake_writer = handshake_writer
        self.nonce = (nonce or secrets.token_hex(16)) if handshake_writer is not None else None
        need(self.nonce is None or (isinstance(self.nonce, str) and re.fullmatch(
            r"[0-9a-f]{32}", self.nonce)), "Invalid handshake nonce")
        need(isinstance(prior_diagnostics, (tuple, list)) and all(
            isinstance(line, str) and DIAGNOSTIC_LINE.fullmatch(line)
            for line in prior_diagnostics), "Invalid prior diagnostics")
        need(len(set(prior_diagnostics)) == len(prior_diagnostics), "Duplicate prior diagnostic")
        need(len({DIAGNOSTIC_LINE.fullmatch(line)[1] for line in prior_diagnostics})
             == len(prior_diagnostics), "Conflicting prior diagnostic nonce")
        need(self.nonce is None or all(DIAGNOSTIC_LINE.fullmatch(line)[1] != self.nonce
            for line in prior_diagnostics), "Nonce reuses an authenticated prior diagnostic")
        self.prior_diagnostics = list(prior_diagnostics)
        self._handshake = None
        self.command = [str(self.adb_path), "-s", serial,
                        *(HANDSHAKE_TAIL if handshake_writer is not None else TAIL)]
        self.env = dict(env) if env is not None else None
        self.limits = {"stdoutBytes": max_stdout_bytes, "stderrBytes": max_stderr_bytes,
                       "seconds": max_seconds, "readinessSeconds": readiness_timeout,
                       "stopSeconds": stop_timeout}
        self.factory = popen_factory
        self._lock = threading.Condition(threading.Lock())
        self._finish_lock = threading.Lock()
        self._deadline_stop = threading.Event()
        self._streams = {
            name: {"raw": bytearray(), "observed": 0, "eof": False, "thread": None,
                   "file": None, "readerStopped": False}
            for name in ("stdout", "stderr")}
        self._errors = []
        self._process = None
        self._started = None
        self._ready = None
        self._stop = None
        self._termination_attempted = False
        self._controlled_stop = False
        self._watchdog = None
        self._proof = None

    def _error(self, message):
        with self._lock:
            if message not in self._errors:
                self._errors.append(message)
            self._lock.notify_all()

    def _terminate_owned(self):
        """Stop this HOST observer once. This sends no device-side command."""
        with self._lock:
            process = self._process
            if process is None or self._termination_attempted:
                return
            self._termination_attempted = True
            if process.poll() is not None:
                return
            self._stop = self._stop or stamp()
            self._controlled_stop = True
        try:
            process.terminate()
        except OSError as error:
            self._error("HOST_OBSERVER_STOP_UNKNOWN: " + str(error))

    def _abort(self, reason):
        self._error(reason)
        self._terminate_owned()

    def _read(self, name, pipe):
        state = self._streams[name]
        bound = self.limits[name + "Bytes"]
        try:
            while True:
                chunk = pipe.read(8192)
                if not chunk:
                    with self._lock:
                        state["eof"] = True
                        unexpected = self._stop is None
                        self._lock.notify_all()
                    if unexpected:
                        self._error(name.upper() + "_UNEXPECTED_EOF")
                    break
                with self._lock:
                    state["observed"] += len(chunk)
                    remaining = max(0, bound - len(state["raw"]))
                    retained = chunk[:remaining]
                    # The raw output is saved as bytes, never through text newline
                    # conversion. The prefix is retained even when overflow fails.
                    if retained:
                        state["file"].write(retained)
                        state["raw"].extend(retained)
                    overflow = state["observed"] > bound
                    visible = stream_errors(bytes(state["raw"])) if name == "stdout" else []
                    if name == "stdout" and self.handshake_writer is not None:
                        try:
                            diagnostic_state(bytes(state["raw"]), self.nonce, self.prior_diagnostics)
                        except (ValueError, UnicodeDecodeError) as error:
                            # An incomplete UTF-8 suffix can be a normal pipe chunk.
                            if not isinstance(error, UnicodeDecodeError) or error.reason != "unexpected end of data":
                                visible.append(str(error))
                    self._lock.notify_all()
                if overflow:
                    self._abort(name.upper() + "_LIMIT_EXCEEDED")
                for error in visible:
                    self._abort(error)
                if name == "stderr":
                    self._abort("STDERR_NOT_EMPTY")
        except (OSError, ValueError) as error:
            self._abort(name.upper() + "_READER_ERROR: " + str(error))
        finally:
            with self._lock:
                state["readerStopped"] = True
                self._lock.notify_all()

    def _watch(self):
        remaining = max(0, self.limits["seconds"] -
                        (time.perf_counter_ns() - self._started["monotonicNs"]) / 1e9)
        if not self._deadline_stop.wait(remaining):
            self._abort("CAPTURE_DEADLINE_EXCEEDED")

    def start(self):
        if self._started is not None:
            raise RuntimeError("A single-method observer cannot be started twice")
        # Refuse old evidence, even if it is an empty directory. No overwrite.
        self.directory.mkdir(parents=True, exist_ok=False)
        self._started = stamp()
        # The diagnostic write consumes the same readiness budget; do not reset
        # the clock when the write returns. The writer's command is independently
        # bounded by supervision, while the capture watchdog bounds all recording.
        ready_deadline = time.monotonic() + self.limits["readinessSeconds"]
        for name, state in self._streams.items():
            state["file"] = (self.directory / (name + ".raw")).open("xb", buffering=0)
        try:
            self._process = self.factory(self.command, stdin=subprocess.DEVNULL,
                stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=False,
                env=self.env, bufsize=0,
                creationflags=getattr(subprocess, "CREATE_NO_WINDOW", 0))
            need(type(self._process.pid) is int and self._process.pid > 0,
                 "Owned observer PID not established")
            for name, state in self._streams.items():
                thread = threading.Thread(target=self._read,
                    args=(name, getattr(self._process, name)),
                    name="m10-log-" + name, daemon=True)
                state["thread"] = thread
                thread.start()
            self._watchdog = threading.Thread(target=self._watch, name="m10-log-deadline", daemon=True)
            self._watchdog.start()
        except (OSError, ValueError) as error:
            self._error("HOST_OBSERVER_LAUNCH_ERROR: " + str(error))
            return self.finish()
        if self.handshake_writer is not None:
            message = "M10_CAPTURE_READY nonce=" + self.nonce
            self._handshake = {"tag": DIAGNOSTIC_TAG, "nonce": self.nonce,
                "message": message, "issued": stamp(), "completed": None,
                "writerResult": None, "authenticatedRawLine": None,
                "authenticatedAt": None, "diagnosticEmissionCount": 1}
            try:
                result = self.handshake_writer(message)
                self._handshake["completed"] = stamp()
                self._handshake["writerResult"] = clone(result)
                need(isinstance(result, dict) and result.get("status") == "PASS"
                    and result.get("serial") == self.serial and type(result.get("exitCode")) is int
                    and result["exitCode"] == 0
                    and result.get("tag") == DIAGNOSTIC_TAG and result.get("nonce") == self.nonce,
                    "HANDSHAKE_WRITE_NOT_PROVEN")
            except Exception as error:
                self._error(str(error) if isinstance(error, ValueError) else
                            "HANDSHAKE_WRITE_ERROR: " + str(error))
                return self.finish()
        limit = ready_deadline
        with self._lock:
            while True:
                raw = self._streams["stdout"]["raw"]
                alive = self._process.poll() is None
                # Readers record bytes/EOF under this lock before publishing
                # _abort() outside it. Inspect those observations directly so
                # READY cannot race ahead of an already-observed failure.
                if self._streams["stderr"]["observed"]:
                    self._errors.append("STDERR_NOT_EMPTY")
                if any(state["eof"] or state["readerStopped"] for state in self._streams.values()):
                    self._errors.append("OBSERVER_READER_STATE_UNKNOWN")
                if self._errors or not alive:
                    break
                if time.monotonic() >= limit:
                    break
                diagnostic = None
                if self.handshake_writer is not None:
                    try:
                        diagnostic = diagnostic_state(bytes(raw), self.nonce, self.prior_diagnostics)
                    except (ValueError, UnicodeDecodeError) as error:
                        self._errors.append(str(error))
                        break
                if diagnostic is not None or (self.handshake_writer is None and any(
                        raw.startswith(header) for header in HEADERS)):
                    self._ready = {**stamp(), "stdoutBytes": len(raw)}
                    if diagnostic is not None:
                        self._handshake["authenticatedRawLine"] = diagnostic
                        self._handshake["authenticatedAt"] = clone(self._ready)
                    return {"status": "PASS", "serial": self.serial,
                            "observerPid": self._process.pid, "ready": clone(self._ready),
                            "scope": "END_TO_END_DIAGNOSTIC_AND_OWNED_HOST_READER_ALIVE" if diagnostic
                            else "ACTUAL_MAIN_HEADER_AND_OWNED_HOST_READER_ALIVE"}
                remaining = limit - time.monotonic()
                if remaining <= 0:
                    break
                self._lock.wait(min(.025, remaining))
        self._error("READINESS_NOT_PROVEN")
        return self.finish()

    def assert_live(self):
        if self._ready is None or self._proof is not None:
            return {"status": "UNKNOWN", "errors": ["OBSERVER_NOT_IN_READY_WINDOW"]}
        with self._lock:
            alive = self._process is not None and self._process.poll() is None
            intact = (not self._errors and self._stop is None
                and self._streams["stderr"]["observed"] == 0
                and all(not state["eof"] and not state["readerStopped"]
                        for state in self._streams.values()))
            in_bound = (time.perf_counter_ns() - self._started["monotonicNs"]) / 1e9 < self.limits["seconds"]
            if alive and intact and in_bound:
                return {"status": "PASS", "observerPid": self._process.pid,
                        "observed": stamp(), "serial": self.serial}
        self._error("OBSERVER_LIVENESS_UNKNOWN")
        return {"status": "UNKNOWN", "errors": list(self._errors)}

    def finish(self):
        with self._finish_lock:
            if self._proof is not None:
                return clone(self._proof)
            if self._started is None:
                return {"status": "UNKNOWN", "errors": ["OBSERVER_NOT_STARTED"]}
            # Natural early exit is not equivalent to a requested observation end.
            if self._process is not None and self._process.poll() is not None and self._stop is None:
                self._error("HOST_OBSERVER_EXITED_BEFORE_REQUESTED_END")
            self._terminate_owned()
            self._deadline_stop.set()
            if self._process is not None:
                try:
                    self._process.wait(timeout=self.limits["stopSeconds"])
                except subprocess.TimeoutExpired:
                    self._error("HOST_OBSERVER_EXIT_UNKNOWN")
            if self._watchdog is not None:
                self._watchdog.join(timeout=self.limits["stopSeconds"])
                if self._watchdog.is_alive():
                    self._error("HOST_OBSERVER_DEADLINE_THREAD_UNKNOWN")
            for name, state in self._streams.items():
                thread = state["thread"]
                if thread is not None:
                    thread.join(timeout=self.limits["stopSeconds"])
                if thread is None or thread.is_alive() or not state["eof"]:
                    self._error(name.upper() + "_DRAIN_NOT_PROVEN")
                # Do not close a pipe underneath an unconfirmed blocked reader.
                # Its UNKNOWN is retained rather than blocking finish indefinitely.
                if thread is None or not thread.is_alive():
                    if self._process is not None:
                        getattr(self._process, name).close()
                if state["file"] is not None and (thread is None or not thread.is_alive()):
                    state["file"].flush()
                    os.fsync(state["file"].fileno())
                    state["file"].close()
            raw = bytes(self._streams["stdout"]["raw"])
            for error in stream_errors(raw, final=True, require_header=self.handshake_writer is None):
                self._error(error)
            if self.handshake_writer is not None:
                try:
                    need(diagnostic_state(raw, self.nonce, self.prior_diagnostics) is not None,
                         "HANDSHAKE_DIAGNOSTIC_NOT_PROVEN")
                except (ValueError, UnicodeDecodeError) as error:
                    self._error(str(error))
            if self._ready is None:
                self._error("READINESS_NOT_PROVEN")
            if self._streams["stderr"]["observed"]:
                self._error("STDERR_NOT_EMPTY")
            ended = stamp()
            self._proof = {
                "format": FORMAT, "status": "UNKNOWN" if self._errors else "PASS",
                "serial": self.serial, "command": self.command,
                "started": self._started, "ready": self._ready,
                "stopRequested": self._stop, "ended": ended,
                "process": {"pid": self._process.pid if self._process is not None else None,
                    "createdByThisCapture": self._process is not None,
                    "exitCode": self._process.poll() if self._process is not None else None,
                    "controlledHostObserverStop": self._controlled_stop},
                "limits": self.limits, "errors": list(self._errors),
                "monotonicClock": "time.perf_counter_ns",
                "diagnosticScope": "HOST_OBSERVER_WINDOW_ONLY_NOT_UNOBSERVABLE_LOGD_LOSS_PROOF"}
            if self.handshake_writer is not None:
                self._proof.update(readinessProtocol=HANDSHAKE_PROTOCOL,
                    handshake=clone(self._handshake), priorDiagnostics=self.prior_diagnostics)
            for name, state in self._streams.items():
                data = bytes(state["raw"])
                self._proof[name] = {"file": name + ".raw", "bytes": len(data),
                    "sha256": hashlib.sha256(data).hexdigest(), "observedBytes": state["observed"],
                    "eof": state["eof"], "readerStopped": state["readerStopped"]}
            data = (json.dumps(self._proof, indent=2, sort_keys=True) + "\n").encode("utf-8")
            with (self.directory / "capture.json").open("xb") as output:
                output.write(data)
                output.flush()
                os.fsync(output.fileno())
            return clone(self._proof)


def _strict_object(pairs):
    result = {}
    for key, value in pairs:
        need(key not in result, "Duplicate proof JSON key")
        result[key] = value
    return result


def read_verified(directory, *, expected_serial=None):
    """Re-read proof and raw bytes without newline conversion; raise if incomplete.

    This is observer verification only. Callers must separately match *all* new
    attempt/session/PID/UID events and the exact proven old backlog, then run the
    original lifecycle/Stage/business evidence checks.
    """
    directory = Path(directory)
    need(directory.is_dir() and not directory.is_symlink(), "Invalid capture directory")
    proof_path = directory / "capture.json"
    need(proof_path.is_file() and not proof_path.is_symlink() and proof_path.stat().st_size <= 65536,
         "Missing/oversized/linked capture proof")
    proof = json.loads(proof_path.read_bytes(), object_pairs_hook=_strict_object)
    need(isinstance(proof, dict) and proof.get("format") == FORMAT and proof.get("status") == "PASS",
         "Capture not a complete PASS proof")
    serial = proof.get("serial")
    need(serial in SERIALS and (expected_serial is None or serial == expected_serial), "Capture serial mismatch")
    command = proof.get("command")
    handshake_mode = proof.get("readinessProtocol") == HANDSHAKE_PROTOCOL
    need(proof.get("readinessProtocol") in (None, HANDSHAKE_PROTOCOL), "Unknown readiness protocol")
    tail = HANDSHAKE_TAIL if handshake_mode else TAIL
    need(isinstance(command, list) and len(command) == 3 + len(tail) and isinstance(command[0], str)
         and Path(command[0]).is_absolute()
         and command[1:] == ["-s", serial, *tail], "Observer command is not fixed-serial read-only capture")
    need(proof.get("errors") == [], "Capture contains unknown/error state")
    limits = proof.get("limits", {})
    need(isinstance(limits, dict), "Capture limits are not an object")
    for key, ceiling in (("stdoutBytes", 2097152), ("stderrBytes", 65536)):
        need(type(limits.get(key)) is int and 0 < limits[key] <= ceiling, "Missing capture byte bound")
    seconds = limits.get("seconds")
    need(type(seconds) in (int, float) and math.isfinite(seconds) and 0 < seconds <= 600,
         "Missing capture time bound")
    timestamps = []
    for key in ("started", "ready", "stopRequested", "ended"):
        value = proof.get(key)
        need(isinstance(value, dict) and type(value.get("monotonicNs")) is int
             and value["monotonicNs"] > 0 and isinstance(value.get("utc"), str), "Capture time missing")
        try:
            utc = datetime.fromisoformat(value["utc"])
        except ValueError as error:
            raise ValueError("Malformed capture UTC") from error
        need(utc.tzinfo is not None, "Capture UTC timezone missing")
        timestamps.append(value["monotonicNs"])
    need(timestamps == sorted(timestamps) and timestamps[0] < timestamps[1] <= timestamps[2] < timestamps[3],
         "Capture start/ready/stop/end order conflicts")
    need((timestamps[2] - timestamps[0]) / 1e9 <= seconds,
         "Capture exceeded supervised recording deadline")
    process = proof.get("process", {})
    need(isinstance(process, dict), "Capture process proof is not an object")
    need(type(process.get("pid")) is int and process["pid"] > 0
         and process.get("createdByThisCapture") is True
         and process.get("controlledHostObserverStop") is True
         and type(process.get("exitCode")) is int, "Owned observer termination not established")
    streams = {}
    for name in ("stdout", "stderr"):
        meta = proof.get(name, {})
        need(isinstance(meta, dict), "Capture stream proof is not an object")
        need(meta.get("file") == name + ".raw" and meta.get("eof") is True
             and meta.get("readerStopped") is True, "Capture reader EOF/drain incomplete")
        size = meta.get("bytes")
        need(type(size) is int and 0 <= size <= limits[name + "Bytes"]
             and type(meta.get("observedBytes")) is int and meta["observedBytes"] == size,
             "Capture byte accounting/truncation conflicts")
        path = directory / (name + ".raw")
        need(path.is_file() and not path.is_symlink() and path.stat().st_size == size,
             "Capture raw file size/type conflicts")
        raw = path.read_bytes()
        need(hashlib.sha256(raw).hexdigest() == meta.get("sha256"), "Capture raw SHA mismatch")
        streams[name] = raw
    need(not streams["stderr"], "Capture stderr is not empty")
    errors = stream_errors(streams["stdout"], final=True, require_header=not handshake_mode)
    need(not errors, "Capture raw stream invalid: " + ", ".join(errors))
    ready_size = proof["ready"].get("stdoutBytes")
    need(type(ready_size) is int and 0 < ready_size <= len(streams["stdout"]),
         "Ready header byte evidence absent")
    if handshake_mode:
        handshake = proof.get("handshake")
        prior = proof.get("priorDiagnostics")
        need(isinstance(handshake, dict) and isinstance(prior, list) and all(
            isinstance(line, str) and DIAGNOSTIC_LINE.fullmatch(line) for line in prior)
            and len(set(prior)) == len(prior), "Malformed prior diagnostic proof")
        need(len({DIAGNOSTIC_LINE.fullmatch(line)[1] for line in prior}) == len(prior),
             "Conflicting prior diagnostic nonce")
        nonce = handshake.get("nonce")
        need(isinstance(nonce, str) and re.fullmatch(r"[0-9a-f]{32}", nonce)
            and handshake.get("tag") == DIAGNOSTIC_TAG
            and handshake.get("message") == "M10_CAPTURE_READY nonce=" + nonce
            and handshake.get("diagnosticEmissionCount") == 1
            and all(DIAGNOSTIC_LINE.fullmatch(line)[1] != nonce for line in prior),
            "Malformed diagnostic identity proof")
        result = handshake.get("writerResult")
        need(isinstance(result, dict) and result.get("status") == "PASS"
            and result.get("serial") == serial and type(result.get("exitCode")) is int
            and result["exitCode"] == 0
            and result.get("tag") == DIAGNOSTIC_TAG and result.get("nonce") == nonce,
            "Diagnostic write not proven")
        for key in ("issued", "completed", "authenticatedAt"):
            value = handshake.get(key)
            need(isinstance(value, dict) and type(value.get("monotonicNs")) is int
                 and isinstance(value.get("utc"), str), "Handshake timing absent")
            try:
                utc = datetime.fromisoformat(value["utc"])
            except ValueError as error:
                raise ValueError("Malformed handshake UTC") from error
            need(utc.tzinfo is not None, "Handshake UTC timezone absent")
        readiness_seconds = limits.get("readinessSeconds")
        need(type(readiness_seconds) in (int, float) and math.isfinite(readiness_seconds)
            and 0 < readiness_seconds <= 30
            and (timestamps[1] - timestamps[0]) / 1e9 <= readiness_seconds,
            "Handshake exceeded its readiness budget")
        need(timestamps[0] <= handshake["issued"]["monotonicNs"]
            <= handshake["completed"]["monotonicNs"] <= timestamps[1]
            and handshake["authenticatedAt"] == proof["ready"], "Handshake timing conflicts")
        authenticated = diagnostic_state(streams["stdout"], nonce, prior)
        need(authenticated == handshake.get("authenticatedRawLine")
            and diagnostic_state(streams["stdout"][:ready_size], nonce, prior) == authenticated,
            "Actual matching diagnostic not captured at readiness")
    else:
        need(any(streams["stdout"][:ready_size].startswith(header) for header in HEADERS),
             "Actual complete main header was not captured at readiness")
    return proof, streams["stdout"].decode("utf-8", errors="strict")
