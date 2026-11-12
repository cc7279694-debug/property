"""Offline behavior and ADB protocol checks. No actual socket or emulator is used."""

import json
from pathlib import Path
import struct
import subprocess
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import call, patch

import home_android_admission as admission


AVD = "Lithic_Home_Acceptance_01_API36"
CONSOLE_PORT = 17640
TRANSPORT_PORT = 17641
ENDPOINT = f"127.0.0.1:{TRANSPORT_PORT}"


class FakeClock:
    def __init__(self):
        self.now = 0.0

    def read(self):
        return self.now

    def advance(self, seconds):
        self.now += seconds


class AdmissionTests(unittest.TestCase):
    def setUp(self):
        self.clock = FakeClock()
        self.records = []
        self.actions = []

    def observe(self, host, shell=None, listener=None, alive=lambda: True):
        def unexpected_shell(*args):
            self.fail("Unexpected guest command")

        return admission._observe(
            launched_at=0, avd=AVD, console_port=CONSOLE_PORT, transport_port=TRANSPORT_PORT,
            alive=alive, host=host, shell=shell or unexpected_shell,
            listener=listener or (lambda _: admission.Reply("OWNED_LOOPBACK", exit_code=0)),
            record=self.records.append, clock=self.clock.read, sleep=self.clock.advance,
        )

    def consume_timeout(self, deadline, error):
        self.clock.now = deadline
        raise error

    def test_connect_and_later_read_only_timeouts_continue_to_valid_identity(self):
        poll_count = 0

        def host(service, deadline):
            nonlocal poll_count
            self.actions.append(service)
            if service == f"host:connect:{ENDPOINT}":
                self.consume_timeout(deadline, TimeoutError("connect reply did not arrive"))
            poll_count += 1
            if poll_count == 1:
                return admission.Reply("")
            if poll_count == 2:
                self.consume_timeout(deadline, TimeoutError("inventory read timed out"))
            return admission.Reply(f"{ENDPOINT}\tdevice product:sdk")

        def shell(serial, command, deadline):
            self.assertEqual(serial, ENDPOINT)
            self.actions.append(command)
            if command == admission.BOOT_PROPERTY and self.actions.count(command) == 1:
                self.consume_timeout(deadline, TimeoutError("read-only boot probe timed out"))
            outputs = {admission.BOOT_PROPERTY: "1\n", admission.SHELL_PROBE: "LITHIC_SHELL_OK\n",
                       admission.IDENTITY_PROPERTIES: f"{AVD}\n1\n36\nx86_64\n"}
            return admission.Reply(outputs[command], exit_code=0)

        result = self.observe(host, shell)
        self.assertEqual((result.status, result.serial), ("ADMITTED", ENDPOINT))
        self.assertEqual(self.actions.count(f"host:connect:{ENDPOINT}"), 1)
        self.assertEqual(self.actions.count(admission.BOOT_PROPERTY), 2)
        self.assertEqual(sum(row["status"] == "TIMEOUT" for row in self.records), 3)
        self.assertEqual(result.identity, {"avd": AVD, "qemu": "1", "api": "36",
                                           "abi": "x86_64", "bootCompleted": "1"})
        self.assertLess(self.clock.now, 180)
        self.assertTrue(all({"startedUtc", "endedUtc", "elapsedSeconds", "exitCode", "timeoutSeconds"}
                            <= row.keys() for row in self.records))

    def test_timeout_without_device_waits_full_window_and_never_connects_twice(self):
        def host(service, deadline):
            self.actions.append(service)
            if service.startswith("host:connect:"):
                self.consume_timeout(deadline, TimeoutError("slow connection"))
            return admission.Reply("")

        result = self.observe(host)
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(self.clock.now, 180)
        self.assertEqual(self.actions.count(f"host:connect:{ENDPOINT}"), 1)
        self.assertGreater(self.actions.count("host:devices-l"), 1)

    def test_unresponsive_server_has_bounded_observations_and_no_connect(self):
        def host(service, deadline):
            self.actions.append(service)
            self.consume_timeout(deadline, TimeoutError("ADB socket timed out"))

        self.assertEqual(self.observe(host).status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(self.clock.now, 180)
        self.assertEqual(set(self.actions), {"host:devices-l"})
        self.assertTrue(all(row["status"] == "TIMEOUT" for row in self.records))
        self.assertLessEqual(len(self.records), 60)

    def test_command_timeout_is_clipped_to_existing_launch_deadline(self):
        self.clock.now = 179.5

        def host(service, deadline):
            self.assertEqual(service, "host:devices-l")
            self.assertEqual(deadline, 180)
            self.consume_timeout(deadline, TimeoutError("no response before launch deadline"))

        self.assertEqual(self.observe(host).status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(self.clock.now, 180)
        self.assertEqual(len(self.records), 1)
        self.assertEqual(self.records[0]["timeoutSeconds"], 0.5)

    def test_host_ownership_subprocess_timeout_is_caught_and_recorded(self):
        def listener(deadline):
            self.actions.append("listener")
            if self.actions.count("listener") == 1:
                error = subprocess.TimeoutExpired(["powershell.exe"], 8, stderr=b"listener lookup exceeded budget")
                self.consume_timeout(deadline, error)
            return admission.Reply("NOT_LISTENING", exit_code=0)

        result = self.observe(lambda *_: admission.Reply(""), listener=listener)
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(self.clock.now, 180)
        timed_out = [row for row in self.records if row["status"] == "TIMEOUT"]
        self.assertEqual(len(timed_out), 1)
        self.assertEqual(timed_out[0]["stderr"], "listener lookup exceeded budget")
        self.assertIsNone(timed_out[0]["exitCode"])
        self.assertGreater(self.actions.count("listener"), 1)

    def test_foreign_transport_is_redacted_and_stops_before_device_queries(self):
        result = self.observe(lambda *_: admission.Reply("PRIVATE123\tdevice model:phone"))
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(len(self.records), 1)
        self.assertEqual(self.records[0]["devices"], [["<unexpected transport>"]])
        self.assertNotIn("PRIVATE123", str(self.records))

    def test_ambiguous_matching_transports_are_rejected(self):
        result = self.observe(lambda *_: admission.Reply(f"{ENDPOINT}\tdevice\nemulator-{CONSOLE_PORT}\tdevice"))
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertEqual(len(self.records), 1)

    def test_foreign_listener_blocks_without_connect(self):
        result = self.observe(lambda *_: admission.Reply(""),
                              listener=lambda _: admission.Reply("FOREIGN_OR_NON_LOOPBACK", exit_code=0))
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertEqual([row["command"] for row in self.records], ["host:devices-l", "Windows listener ownership"])

    def test_guest_identity_mismatch_never_admits(self):
        for identity in (f"wrong-avd\n1\n36\nx86_64\n", f"{AVD}\n0\n36\nx86_64\n",
                         f"{AVD}\n1\n35\nx86_64\n", f"{AVD}\n1\n36\narm64-v8a\n"):
            with self.subTest(identity=identity):
                outputs = {admission.BOOT_PROPERTY: "1", admission.SHELL_PROBE: "LITHIC_SHELL_OK",
                           admission.IDENTITY_PROPERTIES: identity}
                result = self.observe(lambda *_: admission.Reply(f"{ENDPOINT}\tdevice"),
                                      lambda _, command, deadline: admission.Reply(outputs[command], exit_code=0))
                self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
                self.assertIn("identity mismatch", result.reason)

    def test_process_exit_stops_before_any_command(self):
        result = self.observe(lambda *_: self.fail("Inventory after exit"), alive=lambda: False)
        self.assertEqual(result.status, "ENVIRONMENT_BLOCKED")
        self.assertFalse(self.records)


class FakeSocket:
    def __init__(self, response):
        self.response = response
        self.sent = []
        self.closed = False

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.closed = True

    def settimeout(self, timeout):
        if timeout <= 0:
            raise AssertionError("Invalid timeout")

    def sendall(self, command):
        self.sent.append(command)

    def recv(self, count):
        chunk, self.response = self.response[:min(count, 2)], self.response[min(count, 2):]
        return chunk


class SocketProtocolTests(unittest.TestCase):
    def test_direct_existing_server_connect_request_never_spawns_adb(self):
        stream = FakeSocket(b"OKAY0007pending")
        service = f"host:connect:{ENDPOINT}"
        with patch.object(admission.socket, "create_connection", return_value=stream) as connect, \
                patch.object(admission.time, "monotonic", return_value=0), \
                patch.object(admission.subprocess, "run", side_effect=AssertionError("Unexpected process launch")):
            result = admission._host(service, 6)
        self.assertEqual(result.stdout, "pending")
        self.assertIsNone(result.exit_code)
        connect.assert_called_once_with(("127.0.0.1", 5038), timeout=6)
        self.assertEqual(stream.sent, [f"{len(service):04x}".encode("ascii") + service.encode("ascii")])
        self.assertTrue(stream.closed)

    def test_shell_v2_keeps_exit_code_and_stderr(self):
        def packet(channel, data):
            return struct.pack("<BI", channel, len(data)) + data

        stream = FakeSocket(b"OKAYOKAY" + packet(1, b"1\n") + packet(2, b"error") + packet(3, b"\x07"))
        with patch.object(admission.socket, "create_connection", return_value=stream) as connect, \
                patch.object(admission.time, "monotonic", return_value=0):
            result = admission._shell(ENDPOINT, admission.BOOT_PROPERTY, 4)
        self.assertEqual(result, admission.Reply("1\n", "error", 7))
        connect.assert_called_once_with(("127.0.0.1", 5038), timeout=4)
        self.assertIn(f"host:transport:{ENDPOINT}".encode(), stream.sent[0])
        self.assertIn(b"shell,v2,raw:getprop sys.boot_completed", stream.sent[1])
        self.assertTrue(stream.closed)

    def test_adb_fail_response_preserves_error_and_closes_socket(self):
        stream = FakeSocket(b"FAIL0005error")
        with patch.object(admission.socket, "create_connection", return_value=stream), \
                patch.object(admission.time, "monotonic", return_value=0):
            with self.assertRaisesRegex(admission.ProtocolError, "^error$"):
                admission._host("host:devices-l", 3)
        self.assertTrue(stream.closed)

    def test_shared_port_is_rejected_before_any_admission_request(self):
        with patch.object(admission.socket, "create_connection") as connect, \
                patch.object(admission.subprocess, "run") as run:
            with self.assertRaisesRegex(ValueError, "5037"):
                admission._host("host:devices-l", 3, adb_port=5037)
            with self.assertRaisesRegex(ValueError, "5037"):
                admission._host("host:connect:127.0.0.1:5037", 3)
            with self.assertRaisesRegex(ValueError, "5037"):
                admission._host("host:connect:127.0.0.1:05037", 3)
            with self.assertRaises(ValueError):
                admission._host("host:kill", 3)
            with self.assertRaisesRegex(ValueError, "5037"):
                admission._shell(ENDPOINT, admission.BOOT_PROPERTY, 4, adb_port=5037)
            with self.assertRaisesRegex(ValueError, "5037"):
                admission.admit_owned_emulator(object(), 0, AVD, CONSOLE_PORT, TRANSPORT_PORT,
                                               None, adb_port=5037)
        connect.assert_not_called()
        run.assert_not_called()

    def test_invalid_adb_ports_are_rejected_before_networking(self):
        with patch.object(admission.socket, "create_connection") as connect:
            for port in (0, -1, 65536, True, "5038", None):
                with self.subTest(port=port), self.assertRaisesRegex(ValueError, "ADB port"):
                    admission._host("host:devices-l", 3, adb_port=port)
        connect.assert_not_called()

    def test_non_loopback_hosts_are_rejected_before_networking(self):
        with patch.object(admission.socket, "create_connection") as connect:
            for host in ("", "0.0.0.0", "192.0.2.1", "not-loopback.example", None):
                with self.subTest(host=host), self.assertRaisesRegex(ValueError, "ADB host"):
                    admission._shell(ENDPOINT, admission.BOOT_PROPERTY, 4, adb_host=host)
        connect.assert_not_called()

    def test_admission_uses_configured_service_for_inventory_and_guest_probes(self):
        class OwnedEmulator:
            pid = 123
            args = ["emulator.exe", "-avd", AVD, "-ports", f"{CONSOLE_PORT},{TRANSPORT_PORT}"]

            def poll(self):
                return None

        def shell_response(output):
            return FakeSocket(b"OKAYOKAY" + struct.pack("<BI", 1, len(output)) + output +
                              struct.pack("<BI", 3, 1) + b"\0")

        inventory = f"{ENDPOINT}\tdevice product:sdk\n".encode("ascii")
        streams = [FakeSocket(b"OKAY" + f"{len(inventory):04x}".encode("ascii") + inventory),
                   shell_response(b"1\n"), shell_response(b"LITHIC_SHELL_OK\n"),
                   shell_response(f"{AVD}\n1\n36\nx86_64\n".encode("ascii"))]
        observe = admission._observe

        def observe_now(**kwargs):
            return observe(**kwargs, clock=lambda: 0, sleep=lambda _: self.fail("Unexpected additional poll"))

        with tempfile.TemporaryDirectory() as temporary, \
                patch.object(admission, "os", SimpleNamespace(name="nt")), \
                patch.object(admission.subprocess, "Popen", OwnedEmulator), \
                patch.object(admission.time, "monotonic", return_value=0), \
                patch.object(admission, "_windows_listener", return_value=admission.Reply("OWNED_LOOPBACK")), \
                patch.object(admission, "_observe", side_effect=observe_now), \
                patch.object(admission.socket, "create_connection", side_effect=streams) as connect:
            result = admission.admit_owned_emulator(OwnedEmulator(), 0, AVD, CONSOLE_PORT,
                                                   TRANSPORT_PORT, temporary, adb_host="::1", adb_port=5040)
            evidence = Path(temporary)
            events = [json.loads(line) for line in (evidence / "admission-commands.jsonl").read_text(
                encoding="utf-8").splitlines()]
            claim = json.loads((evidence / "admission-claim.json").read_text(encoding="utf-8"))
            outcome = json.loads((evidence / "admission-result.json").read_text(encoding="utf-8"))
        self.assertEqual((result.status, result.serial), ("ADMITTED", ENDPOINT))
        self.assertEqual(connect.call_args_list, [call(("::1", 5040), timeout=seconds) for seconds in (3, 4, 4, 5)])
        self.assertTrue(all(stream.closed for stream in streams))
        self.assertEqual(len(events), 5)
        for entry in [claim, *events, outcome]:
            self.assertEqual((entry["adbHost"], entry["adbPort"]), ("::1", 5040))


if __name__ == "__main__":
    unittest.main()
