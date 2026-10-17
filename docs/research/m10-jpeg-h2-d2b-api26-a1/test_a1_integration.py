"""Source integration contracts only; no Android execution or ANR claims."""
import importlib.util
from pathlib import Path
import unittest

repo = Path(__file__).resolve().parents[3]
spec = importlib.util.spec_from_file_location('original_contract', repo/'docs/research/m10-jpeg-h2-d2b/test_source_contract.py')
original = importlib.util.module_from_spec(spec)
spec.loader.exec_module(original)

class IdentityIntegrationContract(unittest.TestCase):
    def test_role_uses_gate_and_never_promotes_global_identity(self):
        text = original.body('ProbeSupport.java', 'String role(')
        self.assertIn('identity.role(', text)
        for banned in ('processName=', 'mainOwner=', 'countDown(', '.await(', 'return knownRole('):
            self.assertNotIn(banned, text)

    def test_background_verification_has_distinct_completion(self):
        text = original.body('ProbeApplication.java', 'void initializeIdentity(')
        self.assertIn('identity.beginBackground(', text)
        self.assertIn('identity.completeBackground(', text)
        self.assertLess(text.index('identity.beginBackground('), text.index('mainOwner=true'))
        self.assertLess(text.index('new ProbeRunner('), text.index('identity.completeBackground('))
        self.assertIn('identity.fail(', text)

    def test_cmdline_reader_uses_actual_bounded_parser_and_resource_scope(self):
        text = original.body('ProbeSupport.java', 'String processName26(')
        self.assertIn('try(FileInputStream', text)
        self.assertIn('ProcessIdentityGate.readCmdline(', text)

    def test_host_claim_checks_full_identity_not_name_hint(self):
        text = original.body('ProbeApplication.java', 'void submit(')
        self.assertIn('identity.verifiedRole()', text)
        self.assertNotIn('knownRole(ProbeSupport.processName)', text)

if __name__ == '__main__': unittest.main(verbosity=2)
