"""Single authorized normal emulator exit after all natural gates; no app kill."""
from __future__ import annotations

import json
from pathlib import Path
import socket
import time

from natural_tests import APP_SHA, TEST_SHA, METHODS, immutable, prior_result


def normal_exit(supervisor, evidence_api=None):
    """Never called after a failed/unknown method, and never retried."""
    if supervisor.api != 26 or supervisor.serial != "emulator-5624":
        raise RuntimeError("Only the newly isolated API26 has conditional exit authorization")
    for index, method in enumerate(METHODS):
        result = prior_result(Path(supervisor.evidence) / f"method-{index:02d}" / "result.json", index)
        expected = {"status": "PASS", "method": method, "serial": supervisor.serial,
                    "api": 26, "appSHA256": APP_SHA, "testSHA256": TEST_SHA}
        if any(result.get(key) != value for key, value in expected.items()):
            raise RuntimeError("Natural gates incomplete/unknown: do not exit device")
    if evidence_api is None:
        import device_evidence as evidence_api
    supervisor.identity()
    supervisor.installed("com.cooper.lithic.debug", APP_SHA)
    supervisor.installed("com.cooper.lithic.debug.test", TEST_SHA)
    before = evidence_api.before(supervisor, METHODS[-1])
    if before.get("status") != "PASS":
        raise RuntimeError("Consumer/stage closure not independently confirmed")
    matching = supervisor.validate_process()
    targets = matching.get("targets", [])
    if not targets:
        raise RuntimeError("New emulator process identity missing")
    immutable(Path(supervisor.evidence) / "normal-exit-before.json", before)
    supervisor.marker("normal-exit")
    reply = supervisor.adb("emu", "kill", timeout=15)
    # The documented emulator-console normal exit affects this exact new serial.
    # ACK alone never substitutes for independently observed process termination.
    if "OK" not in reply.splitlines():
        raise RuntimeError("Normal exit acknowledgement unknown; do not issue another exit")
    deadline = time.monotonic() + 30
    last = None
    while time.monotonic() < deadline:
        last = supervisor.processes(timeout=min(10, max(1, deadline - time.monotonic())))
        if not last.get("targets"):
            occupied = []
            for port in (supervisor.port, supervisor.port + 1):
                with socket.socket() as connection:
                    connection.settimeout(0.25)
                    if connection.connect_ex(("127.0.0.1", port)) == 0:
                        occupied.append(port)
            if not occupied:
                proof = {"status": "PASS", "serial": supervisor.serial, "api": 26,
                         "allNaturalMethodsPassed": True, "consumersSafe": True, "osProcessEnded": True,
                         "matchingProcessesBefore": targets, "after": last,
                         "portsClosed": [supervisor.port, supervisor.port + 1],
                         "command": "targeted emulator-console normal exit", "appKill": False}
                immutable(supervisor.home / "normal-exit-proof.json", proof)
                return proof
        time.sleep(1)
    raise RuntimeError("Normal exit OS proof UNKNOWN; preserve state and block API36")
