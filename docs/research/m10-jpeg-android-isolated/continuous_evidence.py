"""Interpret a verified method-scoped raw stream, never a logcat prefix delta.

The complete stdout/stderr files are retained. Only exact lines from the prior
read-only snapshot may be classified as initial backlog. This interpretation is
not proof of Android logger completeness beyond the capture's observed window.
"""
from __future__ import annotations

import hashlib
from pathlib import Path
import re

from lifecycle_evidence import PHASES, strict_json


HEADER = "--------- beginning of main"
PHASE = re.compile(
    r"\d\d-\d\d\s+\d\d:\d\d:\d\d\.\d{3,6}\s+(\d+)\s+(\d+)\s+I\s+LITHIC_JPEG\s*:\s*"
    r"([A-Z_]+) attempt=([A-Za-z0-9._:-]+) session=([A-Za-z0-9._:-]+) "
    r"worker=(\d+)/(\d+)/(\d+) at=(\d+)\s*"
)


def need(condition, detail):
    if not condition:
        raise ValueError(detail)


def parsed(line):
    match = PHASE.fullmatch(line)
    need(match is not None and match[3] in PHASES,
         "Unknown, truncated, non-LITHIC or unsafe lifecycle line")
    need(all(int(match[index]) > 0 for index in (1, 2, 6, 7, 8)),
         "Lifecycle numeric identity is not positive")
    # Do not compare ordering here: the existing lifecycle validator owns the
    # permitted partial order and exact Host/Worker emitter identity checks.
    return match[4], match[5], match[6], match[7], match[8], match[3]


def consume_capture(proof, directory, prior_phase_log, *, expected_serial, prior_diagnostics=()):
    """Return newPhaseLog/metadata only after the real collector verifies bytes.

    Replaying historical bytes with a synthetic proof tests this interpreter;
    it does not manufacture a real continuous-capture proof for old UNKNOWNs.
    """
    from continuous_log import read_verified

    need(isinstance(proof, dict) and proof.get("status") == "PASS",
         "Continuous capture is missing, UNKNOWN or non-PASS")
    need(expected_serial in {"emulator-5624", "emulator-5626"} and proof.get("serial") == expected_serial,
         "Capture serial conflicts with the independently authorized device")
    directory = Path(directory)
    proof_path = directory / "capture.json"
    need(proof_path.is_file() and not proof_path.is_symlink() and
         proof_path.stat().st_size <= 65536, "Complete capture proof unavailable")
    raw_proof = proof_path.read_bytes()
    persisted = strict_json(raw_proof.decode("utf-8", errors="strict"))
    need(persisted == proof, "Caller capture proof differs from immutable evidence")
    try:
        verified, text = read_verified(directory, expected_serial=expected_serial)
    except (RuntimeError, OSError, KeyError, TypeError, UnicodeError) as error:
        raise ValueError("Continuous raw capture cannot be verified: " + str(error)) from error
    need(verified == proof, "Verified capture proof differs from caller")
    need(isinstance(text, str) and text.endswith("\n"), "Incomplete lifecycle stream tail")
    normalized = text.replace("\r\n", "\n")
    need("\r" not in normalized and "\ufffd" not in normalized,
         "Malformed lifecycle stream encoding or line endings")
    lines = normalized[:-1].split("\n")
    raw_lines = text.splitlines(keepends=True)
    need(len(raw_lines) == len(lines), "Unexpected raw line separator")
    diagnostic_mode = proof.get("readinessProtocol") == "DIAGNOSTIC_NONCE_V1"
    if diagnostic_mode:
        need(lines.count(HEADER) <= 1 and (HEADER not in lines or lines[0] == HEADER),
             "Main buffer context header repeated or misplaced")
        handshake = proof.get("handshake", {})
        current_diagnostic = handshake.get("authenticatedRawLine")
        need(isinstance(current_diagnostic, str) and current_diagnostic.endswith("\n")
             and len(current_diagnostic.encode("utf-8")) <= 1024,
             "Authenticated handshake raw line unavailable")
        need(isinstance(prior_diagnostics, (list, tuple)) and len(prior_diagnostics) <= 32
             and all(isinstance(line, str) and line.endswith("\n")
                     and len(line.encode("utf-8")) <= 1024 for line in prior_diagnostics),
             "Independent prior diagnostic evidence malformed")
        need(len(set(prior_diagnostics)) == len(prior_diagnostics)
             and current_diagnostic not in prior_diagnostics,
             "Prior/current diagnostic identities duplicate")
        need(proof.get("priorDiagnostics") == list(prior_diagnostics),
             "Capture prior diagnostics differ from independent validated evidence")
    else:
        need(not prior_diagnostics, "Historical V1 capture cannot claim diagnostic authentication")
        need(lines and lines[0] == HEADER and lines.count(HEADER) == 1,
             "Unique initial main-buffer header required")
        current_diagnostic = None
    need(isinstance(prior_phase_log, str) and len(prior_phase_log.encode("utf-8")) <= 2 * 1024 * 1024,
         "Prior phase snapshot unavailable or oversized")
    previous = []
    previous_keys = set()
    for line in prior_phase_log.splitlines():
        if line.startswith("--------- beginning of "):
            need(line in {"--------- beginning of system", "--------- beginning of crash", HEADER},
                 "Unknown prior log buffer header")
            continue
        key = parsed(line)
        need(line not in previous and key not in previous_keys, "Duplicate prior lifecycle line/event")
        previous.append(line)
        previous_keys.add(key)
    old_index = {line: index for index, line in enumerate(previous)}
    seen_lines = set()
    seen_keys = set()
    new_lines = []
    backlog_count = 0
    last_old = -1
    authenticated = set()
    ready_seen = False
    for raw_line, line in zip(raw_lines, lines):
        if line == HEADER:
            continue
        if diagnostic_mode and raw_line == current_diagnostic:
            need(not ready_seen, "Duplicate authenticated current diagnostic")
            ready_seen = True
            authenticated.add(raw_line)
            continue
        if diagnostic_mode and raw_line in prior_diagnostics:
            need(not ready_seen and not new_lines and raw_line not in authenticated,
                 "Prior authenticated diagnostic repeated or outside initial backlog")
            authenticated.add(raw_line)
            continue
        key = parsed(line)
        need(line not in seen_lines and key not in seen_keys, "Duplicate captured lifecycle line/event")
        seen_lines.add(line)
        seen_keys.add(key)
        if line in old_index:
            need(not diagnostic_mode or not ready_seen,
                 "Old business event appears after authenticated capture readiness")
            need(not new_lines, "Old lifecycle line appears after new invocation events")
            need(old_index[line] > last_old, "Old backlog order conflicts with prior snapshot")
            last_old = old_index[line]
            backlog_count += 1
        else:
            need(not diagnostic_mode or ready_seen,
                 "New business lifecycle event precedes authenticated capture readiness")
            need(key not in previous_keys, "Changed old lifecycle event cannot become new evidence")
            new_lines.append(line)
    need(not diagnostic_mode or ready_seen, "Authenticated current diagnostic missing from raw stream")
    return {"newPhaseLog": "\n".join(new_lines), "metadata": {
        "scope": "VERIFIED_CONTINUOUS_METHOD_CAPTURE_WITH_EXACT_INITIAL_BACKLOG",
        "serial": proof["serial"], "rawStdout": proof["stdout"], "rawStderr": proof["stderr"],
        "captureProofSHA256": hashlib.sha256(raw_proof).hexdigest(),
        "oldBacklogLines": backlog_count, "newPhaseLines": len(new_lines),
        "authenticatedDiagnosticsExcluded": len(authenticated),
        "rawFilesRewritten": False, "oldUnknownReclassified": False,
        "globalLoggerLosslessness": "NOT_PROVEN_BY_HOST_CAPTURE_ALONE",
    }}
