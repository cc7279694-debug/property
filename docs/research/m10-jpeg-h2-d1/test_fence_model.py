"""Design-level synthetic assertions. No Android, device, files or real cleanup."""
import unittest
from dataclasses import fields, replace
from itertools import permutations
from fence_model import Fence, RecoveryFacts, Recovery, Blocked


class FailureFenceTests(unittest.TestCase):
    def test_failure_revokes_existing_attempt_before_any_late_grant(self):
        session = Fence()
        self.assertTrue(session.may_authorize('grant'))
        session.fail('BIND_TIMEOUT')
        self.assertFalse(session.may_authorize('grant'))

    def test_persistence_unknown_keeps_memory_fence_and_admission_closed(self):
        s = Fence()
        s.fail('RESULT_TIMEOUT')
        s.persist_revocation(False)
        self.assertEqual(s.durable, 'FENCE_UNKNOWN')
        self.assertTrue(s.pin and s.global_admission_closed)
        self.assertFalse(s.result())

    def test_duplicate_failure_keeps_first_reason_and_verified_revoke(self):
        s = Fence()
        s.fail('FIRST')
        s.persist_revocation(True)
        s.fail('SECOND')
        s.persist_revocation(False)
        self.assertEqual((s.failure, s.durable), ('FIRST', 'VERIFIED_REVOKED'))

    def test_failed_queued_bind_never_enters_binding(self):
        s = Fence(handshaken=False, binding='POSTED')
        s.fail('QUEUE_TIMEOUT')
        self.assertFalse(s.execute_queued_bind())
        self.assertEqual(s.bind_calls, 0)

    def test_inflight_bind_return_preserves_cleanup_for_every_outcome(self):
        for outcome in ('TRUE', 'FALSE', 'THREW'):
            s = Fence(handshaken=False, binding='POSTED')
            self.assertTrue(s.execute_queued_bind())
            s.fail('BIND_TIMEOUT')
            s.binding_returned(outcome)
            self.assertTrue(s.cleanup_needed)
            self.assertFalse(s.connected(1, 'late-endpoint'))
            self.assertFalse(s.may_authorize('hello'))

    def test_unbind_exception_never_erases_responsibility(self):
        s = Fence()
        s.fail('FAILED')
        s.unbind_completed('THREW')
        self.assertEqual(s.binding, 'UNBIND_UNKNOWN')
        self.assertTrue(s.cleanup_needed)
        s.unbind_completed('RELEASED')
        self.assertFalse(s.cleanup_needed)
        self.assertIsNotNone(s.failure)

    def test_not_registered_after_successful_bind_stays_unknown(self):
        s = Fence(bind_return='TRUE')
        s.unbind_completed('NOT_REGISTERED')
        self.assertEqual(s.binding, 'UNBIND_UNKNOWN')

    def test_old_epoch_and_wrong_identity_cannot_authorize(self):
        s = Fence()
        for operation in ('hello', 'grant', 'result'):
            self.assertFalse(s.may_authorize(operation, epoch=0))
            self.assertFalse(s.may_authorize(operation, identity=False))

    def test_old_endpoint_death_does_not_retire_current_endpoint(self):
        s = Fence(endpoint='new-endpoint')
        self.assertFalse(s.endpoint_died('old-endpoint'))
        self.assertFalse(s.dead)

    def test_healthy_result_death_both_orders_preserve_original_contract(self):
        for death_first in (True, False):
            s = Fence()
            if death_first:
                s.endpoint_died(s.endpoint)
            self.assertTrue(s.result())
            if not death_first:
                s.endpoint_died(s.endpoint)
            self.assertTrue(s.can_prepare())
            self.assertFalse(s.result())

    def test_failure_after_death_rejects_late_result(self):
        s = Fence()
        s.endpoint_died(s.endpoint)
        s.fail('FAILED')
        self.assertFalse(s.result())
        self.assertFalse(s.can_prepare())

    def test_revoked_pin_without_revoked_file_is_denied_in_proposal(self):
        # N01 dependency design only; original code and its status remain unchanged.
        self.assertFalse(Fence(state='REVOKED').may_authorize('grant'))

    def test_all_120_post_failure_schedules_stay_closed(self):
        for order in permutations(('persist_ok', 'persist_fail', 'connected', 'result', 'death')):
            s = Fence()
            s.fail('FAILED')
            for action in order:
                if action.startswith('persist'):
                    s.persist_revocation(action == 'persist_ok')
                elif action == 'connected':
                    s.connected(1, 'late-endpoint')
                elif action == 'result':
                    self.assertFalse(s.result())
                else:
                    s.endpoint_died(s.endpoint)
                self.assertFalse(s.can_prepare())
                self.assertTrue(s.global_admission_closed)
                for operation in ('hello', 'grant', 'result'):
                    self.assertFalse(s.may_authorize(operation))


class RecoveryDesignTests(unittest.TestCase):
    @staticmethod
    def ready():
        return RecoveryFacts(**{f.name: True for f in fields(RecoveryFacts)})

    def test_historical_h2_snapshot_is_not_recovery_eligibility(self):
        r = Recovery()
        with self.assertRaises(Blocked):
            r.simulate_cleanup(RecoveryFacts(current_exact=True, namespace_complete=True))
        self.assertTrue(r.source_present and r.pin)

    def test_every_missing_or_unknown_precondition_blocks_before_deletion(self):
        for f in fields(RecoveryFacts):
            for missing in (False, None):
                r = Recovery()
                with self.assertRaises(Blocked):
                    r.simulate_cleanup(replace(self.ready(), **{f.name: missing}))
                self.assertEqual(r.simulated_deletions, 0)
                self.assertTrue(r.source_present and r.pin)

    def test_free_lease_does_not_replace_old_host_quiescence(self):
        r = Recovery()
        with self.assertRaises(Blocked):
            r.simulate_cleanup(replace(self.ready(), old_authorities_quiesced=False))
        self.assertTrue(r.source_present)

    def test_lease_loss_or_pending_restart_still_blocks(self):
        for fact in ('lease_continuously_held', 'service_restart_fenced', 'bindings_closed'):
            r = Recovery()
            with self.assertRaises(Blocked):
                r.simulate_cleanup(replace(self.ready(), **{fact: False}))
            self.assertTrue(r.pin and r.source_present)

    def test_failed_attempt_terminal_keeps_current_without_success_receipt(self):
        r = Recovery()
        self.assertEqual(r.simulate_cleanup(self.ready()), 'FAILED_ATTEMPT_TERMINAL_ONLY')
        self.assertFalse(r.source_present or r.pin)
        self.assertEqual((r.current, r.receipt), ('synthetic-attempt', 'ABSENT'))
        self.assertTrue(r.may_publish_next())

    def test_partial_deletion_requires_separate_review_not_proof_fabrication(self):
        r = Recovery()
        with self.assertRaises(Blocked):
            r.simulate_cleanup(self.ready(), fault='after_delete')
        self.assertFalse(r.source_present or r.terminal_proof)
        self.assertTrue(r.pin and r.blocked_partial)
        with self.assertRaises(Blocked):
            r.simulate_cleanup(self.ready())
        self.assertFalse(r.may_publish_next())

    def test_terminal_sync_failure_may_clear_pin_but_never_allows_admission(self):
        r = Recovery()
        with self.assertRaises(Blocked):
            r.simulate_cleanup(self.ready(), fault='terminal_sync')
        self.assertFalse(r.pin or r.terminal_proof)
        self.assertFalse(r.may_publish_next())

    def test_terminal_replay_verifies_without_deleting_twice(self):
        r = Recovery()
        r.simulate_cleanup(self.ready())
        self.assertEqual(r.simulate_cleanup(self.ready()), 'VERIFIED_EXISTING_TERMINAL')
        self.assertEqual(r.simulated_deletions, 1)
        r.source_present = True
        with self.assertRaises(Blocked):
            r.simulate_cleanup(self.ready())

    def test_separate_execution_permission_is_always_required(self):
        with self.assertRaises(Blocked):
            Recovery().simulate_cleanup(replace(self.ready(), explicitly_authorized=False))


if __name__ == '__main__':
    unittest.main(verbosity=2)
