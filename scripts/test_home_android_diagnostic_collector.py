"""Offline coverage for live validation and receipt replay."""

import unittest

from home_android_diagnostic_collector import replay_guest_samples, validate_guest_sample


SAMPLE = (
    b"1794557157.135423332\nuptime\n25.63 11.11\n"
    b"meminfo\nMemTotal: 1503188 kB\nMemAvailable: 1126520 kB\n"
    b"SwapTotal: 0 kB\nSwapFree: 0 kB\nvmstat\npswpin 0\npswpout 0\noom_kill 0\n"
    b"pressure/memory\nunavailable\npressure/cpu\nunavailable\npressure/io\nunavailable\n"
    b"com.android.documentsui \ncom.android.systemui 984\nsystem_server 717\n"
)


class GuestSampleTests(unittest.TestCase):
    def test_windows_crlf(self):
        self.assertEqual(validate_guest_sample(SAMPLE.replace(b"\n", b"\r\n"), 0)[-1],
                         "system_server 717")

    def test_unix_lf(self):
        self.assertEqual(validate_guest_sample(SAMPLE, 0)[0], "com.android.documentsui ")

    def test_nonzero_exit_rejected(self):
        with self.assertRaisesRegex(ValueError, "exit=1"):
            validate_guest_sample(SAMPLE, 1)

    def test_missing_uptime_rejected(self):
        with self.assertRaisesRegex(ValueError, "section"):
            validate_guest_sample(SAMPLE.replace(b"uptime\n", b""), 0)

    def test_missing_required_resource_fields_rejected(self):
        for field, content in (("MemAvailable", b"MemAvailable: 1126520 kB\n"),
                               ("oom_kill", b"oom_kill 0\n")):
            with self.subTest(field=field), self.assertRaisesRegex(ValueError, field):
                validate_guest_sample(SAMPLE.replace(content, b""), 0)

    def test_truncation_rejected(self):
        for value in (SAMPLE[:-1], SAMPLE.removesuffix(b"system_server 717\n")):
            with self.subTest(value=value[-30:]), self.assertRaisesRegex(ValueError, "terminal|terminator"):
                validate_guest_sample(value, 0)

    def test_replay_records_counts_and_failure_reason(self):
        header = b"=== host_send_utc=a host_recv_utc=b mono_start=1 mono_end=2 exit=%d ===\n"
        raw = header % 0 + SAMPLE.replace(b"\n", b"\r\n") + b"\n" + header % 1 + SAMPLE + b"\n"
        self.assertEqual(replay_guest_samples(raw), {
            "raw_samples": 2, "command_success": 1, "crlf_samples": 1,
            "valid_samples": 1, "invalid_samples": 1,
            "failures": [{"sample": 2, "reason": "Guest resource command exit=1"}],
        })


if __name__ == "__main__":
    unittest.main()
