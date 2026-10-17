"""Static source boundaries only; not Android ANR/StrictMode evidence."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[3] / 'docs/research/m10-jpeg-r3/host/probe/src/main/java/com/cooper/lithic/m10r3/probe'
def body(file, signature):
    source = (ROOT / file).read_text(encoding='utf-8')
    start = source.index('{', source.index(signature))
    depth = 1
    for i in range(start + 1, len(source)):
        depth += (source[i] == '{') - (source[i] == '}')
        if depth == 0: return source[start:i+1]
    raise AssertionError('unclosed method')

class HostSourceContract(unittest.TestCase):
    def test_activity_does_not_construct_runner_on_ui(self):
        self.assertNotIn('new ProbeRunner', body('ProbeActivity.java', 'void accept('))

    def test_activity_does_not_claim_on_ui(self):
        self.assertNotIn('ProbeRunner.claim(', body('ProbeActivity.java', 'void accept('))

    def test_application_oncreate_does_not_open_owner_or_proc(self):
        text = body('ProbeApplication.java', 'void onCreate(')
        for banned in ('new File(', 'createNewFile(', 'processName26(', '.await(', '.getFD().sync('):
            self.assertNotIn(banned, text)

    def test_trace_producer_does_not_do_logcat_or_file_io(self):
        text = body('ProbeSupport.java', 'void event(')
        for banned in ('Log.i(', 'Log.e(', 'new File(', 'new FileOutputStream(', '.sync(', '.await('):
            self.assertNotIn(banned, text)

    def test_trace_producer_has_no_global_synchronized_monitor(self):
        source = (ROOT / 'ProbeSupport.java').read_text(encoding='utf-8')
        self.assertNotIn('static synchronized void event', source)

    def test_authority_writes_remain_synchronous_store_operations(self):
        source = (ROOT / 'StageStore.java').read_text(encoding='utf-8')
        self.assertIn('o.getFD().sync()', source)
        self.assertIn('StageAuthorization.revokeVerified', source)
        self.assertNotIn('BoundedTraceWriter', source)

    def test_reserve_does_not_return_before_final_deadline_observation(self):
        text = body('ProbeRunner.java', 'boolean reserve(')
        self.assertNotIn('return accepted&&open()', text)
        self.assertIn('handshakeReady()', text)

    def test_grant_and_result_recheck_timing_in_short_authorization(self):
        for signature in ('boolean grant(', 'boolean result('):
            text = body('ProbeRunner.java', signature)
            self.assertIn('accepting()', text)

if __name__ == '__main__': unittest.main(verbosity=2)
