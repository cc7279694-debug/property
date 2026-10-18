"""Host-only tests; FakeSupervisor never invokes ADB or any Android process."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("natural_tests", HERE / "natural_tests.py")
natural = importlib.util.module_from_spec(spec)
spec.loader.exec_module(natural)


def instrument(method=None):
    method = method or natural.METHODS[0]
    cls, name = method.split("#")
    return (f"INSTRUMENTATION_STATUS: class={cls}\nINSTRUMENTATION_STATUS: numtests=1\n"
            f"INSTRUMENTATION_STATUS: test={name}\nINSTRUMENTATION_STATUS_CODE: 1\n"
            f"INSTRUMENTATION_STATUS: class={cls}\nINSTRUMENTATION_STATUS: numtests=1\n"
            f"INSTRUMENTATION_STATUS: test={name}\nINSTRUMENTATION_STATUS_CODE: 0\n"
            "INSTRUMENTATION_RESULT: stream=\nTime: 1.234\n\nOK (1 test)\n\n"
            "INSTRUMENTATION_CODE: -1\n")


class FakeSupervisor:
    def __init__(self, root):
        self.evidence = root
        self.serial = "emulator-5624"
        self.api = 26
        self.calls = []
        self.response = instrument()
        self.run_error = None
    def identity(self): self.calls.append(("identity",))
    def apk_audit(self): self.calls.append(("apk_audit",)); return {"status": "PASS", "apks": []}
    def installed(self, package, sha): self.calls.append(("installed", package)); return 10001 if package == natural.APP else 10002
    def note(self, value): self.calls.append(("note", value))
    def marker(self, operation): self.calls.append(("marker", operation))
    def adb(self, *args, **kwargs):
        self.calls.append(("adb", args))
        if args[:3] == ("shell", "pm", "list"):
            return f"instrumentation:{natural.COMPONENT} (target={natural.APP})"
        if self.run_error: raise self.run_error
        return self.response


class FakeEvidence:
    def __init__(self):
        self.calls = []
        self.before_status = "PASS"
        self.assess_status = "PASS"
    def before(self, supervisor, method):
        self.calls.append("before"); return {"status": self.before_status, "serial": supervisor.serial}
    def after(self, supervisor, method, before):
        self.calls.append("after"); return {"status": "PASS", "serial": supervisor.serial}
    def assess(self, method, before, after, result):
        self.calls.append("assess"); return {"status": self.assess_status}


class NaturalTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.sup = FakeSupervisor(Path(self.tmp.name))
        self.evidence = FakeEvidence()
        self.fixture_patch = patch.object(natural, "audit_fixtures", return_value={"status": "PASS"})
        self.fixture_patch.start(); self.addCleanup(self.fixture_patch.stop)
    def run_first(self): return natural.run_one(self.sup, natural.METHODS[0], self.evidence)
    def run_calls(self): return [x for x in self.sup.calls if x[0] == "adb" and x[1][:3] == ("shell", "am", "instrument")]
    def blocked(self, fn):
        with self.assertRaises(natural.MethodBlocked): fn()

    def test_real_instrumentation_success_requires_matching_single_method(self):
        self.assertEqual("PASS", natural.parse_instrumentation(instrument(), natural.METHODS[0])["status"])
    def test_exit_zero_is_not_a_test_result(self):
        self.assertNotEqual("PASS", natural.parse_instrumentation("", natural.METHODS[0])["status"])
    def test_ok_text_without_protocol_is_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation("OK (1 test)", natural.METHODS[0])["status"])
    def test_failure_status_is_fail_even_if_ok_text_present(self):
        self.assertEqual("FAIL", natural.parse_instrumentation(instrument().replace("STATUS_CODE: 0", "STATUS_CODE: -2"), natural.METHODS[0])["status"])
    def test_truncated_protocol_is_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument().split("INSTRUMENTATION_CODE:")[0], natural.METHODS[0])["status"])
    def test_different_method_is_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument(natural.METHODS[1]), natural.METHODS[0])["status"])
    def test_duplicate_terminal_is_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument()+"INSTRUMENTATION_CODE: -1\n", natural.METHODS[0])["status"])
    def test_multiple_tests_are_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument().replace("numtests=1", "numtests=2"), natural.METHODS[0])["status"])
    def test_missing_start_is_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument().replace("STATUS_CODE: 1", "STATUS_CODE: 2"), natural.METHODS[0])["status"])
    def test_crash_protocol_is_fail(self):
        self.assertEqual("FAIL", natural.parse_instrumentation("INSTRUMENTATION_RESULT: shortMsg=Process crashed.\nINSTRUMENTATION_CODE: 0\n", natural.METHODS[0])["status"])
    def test_skipped_protocol_is_not_pass(self):
        self.assertNotEqual("PASS", natural.parse_instrumentation(instrument().replace("STATUS_CODE: 0", "STATUS_CODE: -3"), natural.METHODS[0])["status"])
    def test_control_characters_are_unknown(self):
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(instrument()+"\x00", natural.METHODS[0])["status"])
    def test_missing_numtests_is_unknown(self):
        text=instrument().replace("INSTRUMENTATION_STATUS: numtests=1\n", "", 1)
        self.assertEqual("UNKNOWN", natural.parse_instrumentation(text,natural.METHODS[0])["status"])
    def test_replacement_decoding_is_unknown(self):
        self.assertEqual("UNKNOWN",natural.parse_instrumentation(instrument()+"\ufffd",natural.METHODS[0])["status"])
    def test_extra_malformed_protocol_code_is_unknown(self):
        self.assertEqual("UNKNOWN",natural.parse_instrumentation(instrument()+"INSTRUMENTATION_CODE: unfinished\n",natural.METHODS[0])["status"])
    def test_allowlist_is_fixed_nine_natural_methods(self):
        self.assertEqual(9, len(natural.METHODS)); self.assertEqual(9, len(set(natural.METHODS)))
        self.assertFalse(any(x in str(natural.METHODS) for x in ("unresolved_v2_pin", "kill", "cancel", "fault")))
    def test_unknown_method_is_rejected_before_any_operation(self):
        self.blocked(lambda: natural.run_one(self.sup, "arbitrary#method", self.evidence)); self.assertEqual([], self.sup.calls)
    def test_later_method_cannot_run_without_prior_pass(self):
        self.blocked(lambda: natural.run_one(self.sup, natural.METHODS[1], self.evidence)); self.assertEqual([], self.run_calls())
    def test_failed_prior_result_blocks_next(self):
        directory = self.sup.evidence / "method-00"; directory.mkdir()
        (directory / "result.json").write_text(json.dumps({"status":"FAIL", "method":natural.METHODS[0], "serial":self.sup.serial}))
        self.blocked(lambda: natural.run_one(self.sup,natural.METHODS[1],self.evidence)); self.assertEqual([],self.run_calls())
    def test_corrupt_prior_result_blocks_next(self):
        directory=self.sup.evidence/"method-00"; directory.mkdir(); (directory/"result.json").write_text("{")
        self.blocked(lambda:natural.run_one(self.sup,natural.METHODS[1],self.evidence)); self.assertEqual([],self.run_calls())
    def test_matching_prior_pass_permits_next_single_method(self):
        self.run_first(); self.sup.response=instrument(natural.METHODS[1])
        value=natural.run_one(self.sup,natural.METHODS[1],self.evidence)
        self.assertEqual("PASS",value["status"]); self.assertEqual(2,len(self.run_calls()))
    def test_prior_pass_from_other_serial_blocks(self):
        self.run_first(); path=self.sup.evidence/"method-00/result.json"; value=json.loads(path.read_text())
        value["serial"]="emulator-5596"; path.write_text(json.dumps(value))
        self.blocked(lambda:natural.run_one(self.sup,natural.METHODS[1],self.evidence)); self.assertEqual(1,len(self.run_calls()))
    def test_single_instrumentation_command_and_no_fault_extras(self):
        self.assertEqual("PASS", self.run_first()["status"])
        calls = self.run_calls(); self.assertEqual(1,len(calls))
        self.assertEqual(("shell","am","instrument","-w","-r","-e","class",natural.METHODS[0],natural.COMPONENT),calls[0][1])
    def test_identity_and_package_checks_precede_instrumentation(self):
        self.run_first(); names = [x[0] for x in self.sup.calls]
        self.assertLess(names.index("identity"), names.index("marker"))
        self.assertEqual(2,names.count("installed")); self.assertIn("apk_audit",names)
    def test_marker_precedes_only_instrumentation(self):
        self.run_first(); marker = next(i for i,x in enumerate(self.sup.calls) if x[0]=="marker")
        command = next(i for i,x in enumerate(self.sup.calls) if x[0]=="adb" and x[1][:3]==("shell","am","instrument"))
        self.assertLess(marker,command)
    def test_preflight_unknown_blocks_test(self):
        self.evidence.before_status = "UNKNOWN"; self.blocked(self.run_first); self.assertEqual([],self.run_calls())
    def test_lifecycle_unknown_blocks_successful_test(self):
        self.evidence.assess_status="UNKNOWN"; self.blocked(self.run_first)
        self.assertEqual(1,len(self.run_calls()))
        self.assertEqual("UNKNOWN",json.loads((self.sup.evidence/"method-00/result.json").read_text())["status"])
    def test_timeout_is_unknown_and_not_retried(self):
        self.sup.run_error=RuntimeError("Command supervision timeout: preserve device state")
        self.blocked(self.run_first); self.assertEqual(1,len(self.run_calls()))
        result=json.loads((self.sup.evidence/"method-00/result.json").read_text()); self.assertEqual("UNKNOWN",result["status"])
        self.assertIn("after",self.evidence.calls)
    def test_parser_unknown_preserves_output_and_stops(self):
        self.sup.response="INSTRUMENTATION_CODE: -1\n"; self.blocked(self.run_first)
        self.assertEqual(self.sup.response,(self.sup.evidence/"method-00/instrumentation.txt").read_text())
    def test_unknown_instrumentation_still_collects_readonly_after(self):
        self.sup.response="unrecognised"; self.blocked(self.run_first); self.assertIn("after",self.evidence.calls)
    def test_same_method_cannot_be_reissued(self):
        self.run_first(); self.blocked(self.run_first); self.assertEqual(1,len(self.run_calls()))
    def test_result_records_source_and_command_without_claiming_android_cleanup(self):
        value=self.run_first(); self.assertEqual(natural.METHODS[0],value["method"])
        self.assertEqual("PASS",value["instrumentation"]["status"])
        self.assertNotIn("cleanupIssued",value)
    def test_instrumentation_component_mismatch_blocks(self):
        original=self.sup.adb
        def wrong(*args,**kwargs):
            return "instrumentation:wrong/Runner (target=com.cooper.lithic.debug)" if args[:3]==("shell","pm","list") else original(*args,**kwargs)
        self.sup.adb=wrong; self.blocked(self.run_first); self.assertEqual([],self.run_calls())
    def test_duplicate_instrumentation_component_blocks(self):
        original=self.sup.adb
        def duplicate(*args,**kwargs):
            return (f"instrumentation:{natural.COMPONENT} (target={natural.APP})\n"*2) if args[:3]==("shell","pm","list") else original(*args,**kwargs)
        self.sup.adb=duplicate; self.blocked(self.run_first); self.assertEqual([],self.run_calls())
    def test_fixture_missing_blocks(self):
        self.fixture_patch.stop()
        self.blocked(self.run_first); self.assertEqual([],self.run_calls())
    def test_fixture_changed_bytes_are_rejected(self):
        self.fixture_patch.stop()
        apk=Path(self.tmp.name)/"fixture.apk"
        with zipfile.ZipFile(apk,"w") as stream:
            stream.writestr("assets/m10_jpeg_integrity/complete.jpg",b"x"*59334)
            stream.writestr("assets/m10_jpeg_integrity/entropy_cut_eoi_retained.jpg",b"y"*20194)
        sha=natural.hashlib.sha256(apk.read_bytes()).hexdigest()
        with patch.object(natural,"TEST_SHA",sha):
            self.blocked(lambda:natural.audit_fixtures({"apks":[{"kind":"test","path":str(apk)}]}))
    def test_fixture_original_apk_hash_is_required(self):
        self.fixture_patch.stop(); apk=Path(self.tmp.name)/"fixture.apk"; apk.write_bytes(b"changed")
        self.blocked(lambda:natural.audit_fixtures({"apks":[{"kind":"test","path":str(apk)}]}))
    def test_fixture_duplicate_entry_rejected(self):
        self.fixture_patch.stop(); apk=Path(self.tmp.name)/"fixture.apk"
        import warnings
        with warnings.catch_warnings():
            warnings.simplefilter("ignore",UserWarning)
            with zipfile.ZipFile(apk,"w") as stream:
                stream.writestr("assets/m10_jpeg_integrity/complete.jpg",b"x"*59334)
                stream.writestr("assets/m10_jpeg_integrity/complete.jpg",b"x"*59334)
        with patch.object(natural,"TEST_SHA",natural.hashlib.sha256(apk.read_bytes()).hexdigest()):
            self.blocked(lambda:natural.audit_fixtures({"apks":[{"kind":"test","path":str(apk)}]}))
    def test_status_file_is_never_overwritten(self):
        self.run_first(); path=self.sup.evidence/"method-00/result.json"; prior=path.read_bytes()
        self.blocked(self.run_first); self.assertEqual(prior,path.read_bytes())
    def test_after_failure_is_unknown_not_pass(self):
        def broken(*_): raise RuntimeError("snapshot unavailable")
        self.evidence.after=broken; self.blocked(self.run_first)
        self.assertEqual("UNKNOWN",json.loads((self.sup.evidence/"method-00/result.json").read_text())["status"])


if __name__ == "__main__": unittest.main(verbosity=2)

