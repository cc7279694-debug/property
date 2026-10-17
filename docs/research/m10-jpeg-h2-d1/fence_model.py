"""MODEL_ONLY proposal. Atomic transitions are assumed, not Android locks.

No filesystem, Binder, socket, process, Android API or real stage is accessed.
Recovery booleans stand for independently verified evidence, never real evidence.
"""
from dataclasses import dataclass, fields


class Blocked(RuntimeError):
    pass


@dataclass
class Fence:
    # Default is a previously authenticated session: the static N04 witness.
    handshaken: bool = True
    epoch: int = 1
    endpoint: str = 'endpoint-1'
    binding: str = 'CONNECTED'
    bind_return: str = 'TRUE'
    cleanup_needed: bool = True
    failure: str | None = None
    durable: str = 'NOT_REVOKED'
    state: str = 'FD_SENT'
    pin: bool = True
    current: str = 'synthetic-attempt'
    dead: bool = False
    accepted_results: int = 0
    fd_closed: bool = False
    global_admission_closed: bool = False
    bind_calls: int = 0

    def fail(self, reason):
        if self.failure is None:
            self.failure = reason
        self.global_admission_closed = True

    def persist_revocation(self, verified):
        if self.failure is None:
            raise Blocked('failure must be linearized first')
        if self.durable == 'VERIFIED_REVOKED':
            return
        self.durable = 'VERIFIED_REVOKED' if verified else 'FENCE_UNKNOWN'
        if verified:
            self.state = 'REVOKED'

    def may_authorize(self, operation, *, epoch=1, identity=True):
        return (operation in ('hello', 'grant', 'result') and identity and epoch == self.epoch
                and self.failure is None and self.durable == 'NOT_REVOKED' and self.pin
                and self.state in ('DELIVERY_UNKNOWN', 'FD_SENT')
                and (operation == 'hello' or self.handshaken))

    def execute_queued_bind(self):
        if self.failure is not None:
            self.binding = 'CLOSED_NOT_STARTED'
            return False
        if self.binding != 'POSTED':
            return False
        self.binding = 'CALLING'
        self.cleanup_needed = True
        self.bind_calls += 1
        return True

    def binding_returned(self, outcome):
        if outcome not in ('TRUE', 'FALSE', 'THREW'):
            raise Blocked('unknown binding result')
        self.bind_return = outcome
        self.cleanup_needed = True
        self.binding = 'CLEANUP_REQUIRED' if self.failure else 'RETURNED'

    def connected(self, epoch, endpoint):
        if epoch != self.epoch or self.failure is not None:
            self.cleanup_needed = True
            self.binding = 'CLEANUP_REQUIRED'
            return False  # optional death observation remains quarantine-only
        self.endpoint = endpoint
        self.binding = 'CONNECTED'
        return True

    def unbind_completed(self, outcome):
        if outcome == 'RELEASED' or (outcome == 'NOT_REGISTERED' and self.bind_return != 'TRUE'):
            self.binding = 'UNBOUND'
            self.cleanup_needed = False
        else:
            self.binding = 'UNBIND_UNKNOWN'
            self.cleanup_needed = True

    def endpoint_died(self, endpoint):
        if endpoint != self.endpoint:
            return False
        self.dead = True
        return True

    def result(self, *, epoch=1, identity=True):
        if not self.may_authorize('result', epoch=epoch, identity=identity) or self.accepted_results:
            return False
        self.accepted_results = 1
        self.fd_closed = True
        return True

    def can_prepare(self):
        # Preserve healthy matching result/death in either order.
        return (self.failure is None and self.durable == 'NOT_REVOKED'
                and self.dead and self.accepted_results == 1 and self.fd_closed)


@dataclass(frozen=True)
class RecoveryFacts:
    explicitly_authorized: bool = False
    recovery_owner_exclusive: bool = False
    corrected_host_fences_verified: bool = False
    global_admission_closed: bool = False
    old_authorities_quiesced: bool = False
    bindings_closed: bool = False
    service_restart_fenced: bool = False
    consumers_terminal_evidence: bool = False
    host_fds_closed_or_host_dead: bool = False
    worker_fds_closed_or_worker_dead: bool = False
    namespace_complete: bool = False
    other_attempts_terminal_or_absent: bool = False
    current_exact: bool = False
    owner_full_match: bool = False
    stage_guard_inodes_verified: bool = False
    source_inode_size_hash_verified: bool = False
    unknown_children_absent: bool = False
    pending_absent: bool = False
    durable_revocation_verified: bool = False
    lease_continuously_held: bool = False

    def missing(self):
        return tuple(f.name for f in fields(self) if getattr(self, f.name) is not True)


@dataclass
class Recovery:
    """In-memory drawing; simulated deletion only changes a bool."""
    source_present: bool = True
    pin: bool = True
    terminal_proof: bool = False
    blocked_partial: bool = False
    receipt: str = 'ABSENT'
    current: str = 'synthetic-attempt'
    simulated_deletions: int = 0

    def simulate_cleanup(self, facts, fault=None):
        if facts.missing():
            raise Blocked(','.join(facts.missing()))
        if self.blocked_partial:
            raise Blocked('partial/unknown stage requires separate review')
        if self.terminal_proof:
            if self.source_present or self.pin or self.receipt != 'ABSENT':
                raise Blocked('terminal evidence inconsistent')
            return 'VERIFIED_EXISTING_TERMINAL'
        if fault == 'before_delete':
            raise Blocked('predelete failed, preserve source')
        self.source_present = False
        self.simulated_deletions += 1
        if fault == 'after_delete':
            self.blocked_partial = True
            raise Blocked('partial deletion, no automatic proof repair')
        self.pin = False
        if fault == 'terminal_sync':
            self.blocked_partial = True
            raise Blocked('pin may already be false; proof still absent')
        self.terminal_proof = True
        return 'FAILED_ATTEMPT_TERMINAL_ONLY'

    def may_publish_next(self):
        return (self.terminal_proof and not self.blocked_partial
                and not self.source_present and not self.pin and self.receipt == 'ABSENT')
