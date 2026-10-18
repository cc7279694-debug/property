"""Pure host orchestration tests: the fakes cannot issue a device command."""
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

import natural_tests as natural
from test_natural_tests import FakeSupervisor, instrument


class Collector:
    def __init__(self, events):
        self.events=events; self.start_error=None; self.live_error=None
        self.finish_error=None; self.status="PASS"; self.finish_calls=0; self.start_status="PASS"; self.live_status="PASS"
    def start(self):
        self.events.append("capture-start")
        if self.start_error: raise self.start_error
        return {"status":self.start_status}
    def assert_live(self):
        self.events.append("capture-live")
        if self.live_error: raise self.live_error
        return {"status":self.live_status}
    def finish(self):
        self.events.append("capture-finish"); self.finish_calls+=1
        if self.finish_error: raise self.finish_error
        return {"status":self.status,"evidence":"HOST_FAKE_ONLY","rawBytes":20,"rawSHA256":"a"*64}


class ReadOnlyEvidence:
    def __init__(self,events): self.events=events; self.after_calls=0; self.after_error=None; self.capture=None
    def before(self,s,method):
        self.events.append("before"); return {"status":"PASS","serial":s.serial}
    def after(self,s,method,prior,**kwargs):
        self.events.append("after"); self.after_calls+=1; self.capture=kwargs.get("capture")
        if self.after_error: raise self.after_error
        return {"status":"PASS","serial":s.serial}
    def assess(self,*args): self.events.append("assess"); return {"status":"PASS"}


class FinalCaptureIntegrationTest(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory(); self.addCleanup(self.temp.cleanup)
        self.sup=FakeSupervisor(Path(self.temp.name)); self.events=[]
        self.collector=Collector(self.events); self.evidence=ReadOnlyEvidence(self.events)
        self.original_adb=self.sup.adb
        def adb(*args,**kwargs):
            if args[:3]==("shell","am","instrument"): self.events.append("instrument")
            return self.original_adb(*args,**kwargs)
        self.sup.adb=adb
        self.factory_calls=[]
        def factory(s,directory):
            self.factory_calls.append((s,Path(directory))); return self.collector
        self.factory=factory
        self.fixture_patch=patch.object(natural,"audit_fixtures",return_value={"status":"PASS"})
        self.fixture_patch.start(); self.addCleanup(self.fixture_patch.stop)
    def run_first(self):
        return natural.run_one(self.sup,natural.METHODS[0],self.evidence,capture_factory=self.factory)
    def result(self): return json.loads((self.sup.evidence/"method-00/result.json").read_bytes())
    def blocked(self):
        with self.assertRaises(natural.MethodBlocked): self.run_first()
    def test_capture_is_started_before_the_single_instrumentation_command(self):
        row=self.run_first(); self.assertEqual("PASS",row["status"])
        self.assertLess(self.events.index("capture-start"),self.events.index("instrument"))
        self.assertEqual(1,self.events.count("instrument"))
    def test_live_capture_is_checked_before_instrumentation(self):
        self.run_first()
        self.assertLess(self.events.index("capture-live"),self.events.index("instrument"))
    def test_capture_start_failure_never_issues_instrumentation(self):
        self.collector.start_error=RuntimeError("ready UNKNOWN"); self.blocked()
        self.assertNotIn("instrument",self.events)
        self.assertNotEqual("PASS",self.result()["status"])
    def test_not_live_capture_never_issues_instrumentation(self):
        self.collector.live_error=RuntimeError("observer exited"); self.blocked()
        self.assertNotIn("instrument",self.events)
    def test_start_unknown_status_never_issues_instrumentation(self):
        self.collector.start_status="UNKNOWN"; self.blocked()
        self.assertNotIn("instrument",self.events)
    def test_live_unknown_status_never_issues_instrumentation(self):
        self.collector.live_status="UNKNOWN"; self.blocked()
        self.assertNotIn("instrument",self.events)
    def test_factory_failure_never_issues_instrumentation(self):
        def broken_factory(*args): raise RuntimeError("observer setup UNKNOWN")
        self.factory=broken_factory; self.blocked()
        self.assertNotIn("instrument",self.events)
    def test_capture_is_finished_after_instrumentation(self):
        self.run_first()
        self.assertLess(self.events.index("instrument"),self.events.index("capture-finish"))
        self.assertEqual(1,self.collector.finish_calls)
    def test_unknown_capture_overrides_successful_junit(self):
        self.collector.status="UNKNOWN"; self.blocked(); row=self.result()
        self.assertEqual("PASS",row["instrumentation"]["status"])
        self.assertEqual("UNKNOWN",row["status"])
        self.assertEqual("UNKNOWN",row["logCapture"]["status"])
    def test_capture_finish_exception_is_unknown_and_keeps_readonly_ending(self):
        self.collector.finish_error=RuntimeError("reader did not drain"); self.blocked()
        self.assertEqual("UNKNOWN",self.result()["status"])
        self.assertEqual(1,self.evidence.after_calls)
        self.assertEqual(1,self.collector.finish_calls)
    def test_instrumentation_exception_still_finishes_the_capture(self):
        self.sup.run_error=TimeoutError("instrumentation UNKNOWN"); self.blocked()
        self.assertEqual(1,self.collector.finish_calls)
        self.assertEqual(1,self.evidence.after_calls)
        self.assertEqual("UNKNOWN",self.result()["status"])
    def test_instrumentation_failure_still_finishes_the_capture(self):
        self.sup.response="INSTRUMENTATION_FAILED: synthetic host fixture\n"; self.blocked()
        self.assertEqual(1,self.collector.finish_calls)
        self.assertNotEqual("PASS",self.result()["status"])
    def test_readonly_after_failure_does_not_abandon_the_collector(self):
        self.evidence.after_error=RuntimeError("stage UNKNOWN"); self.blocked()
        self.assertEqual(1,self.collector.finish_calls)
        self.assertEqual("UNKNOWN",self.result()["status"])
    def test_factory_receives_the_exclusive_current_method_directory(self):
        self.run_first(); self.assertEqual(1,len(self.factory_calls))
        self.assertIs(self.sup,self.factory_calls[0][0])
        self.assertEqual(self.sup.evidence/"method-00",self.factory_calls[0][1])
    def test_actual_required_mode_cannot_fall_back_to_legacy_snapshot_only(self):
        self.sup.continuous_capture_required=True
        with self.assertRaises(natural.MethodBlocked): natural.run_one(self.sup,natural.METHODS[0],self.evidence)
        self.assertNotIn("instrument",self.events)
    def test_original_unknown_evidence_is_never_rewritten(self):
        old=self.sup.evidence/"method-01"; old.mkdir(); path=old/"result.json"
        raw=b'{"status":"UNKNOWN","reason":"old log rolling"}\n'; path.write_bytes(raw)
        self.run_first(); self.assertEqual(raw,path.read_bytes())
    def test_capture_proof_is_preserved_even_when_junit_fails(self):
        self.sup.response="INSTRUMENTATION_FAILED: synthetic host fixture\n"; self.blocked()
        self.assertEqual("PASS",self.result()["logCapture"]["status"])
    def test_unknown_capture_never_automatically_runs_a_following_method(self):
        self.collector.status="UNKNOWN"; self.blocked()
        self.assertEqual(1,self.events.count("instrument"))
        self.assertFalse((self.sup.evidence/"method-01").exists())

if __name__=="__main__": unittest.main(verbosity=2)
