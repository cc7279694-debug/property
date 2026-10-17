package com.cooper.lithic.m10r3.probe;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Short, in-memory control boundary for this research Host. No method may perform I/O,
 * Binder calls, logging or waits. Callers may hold the slow authorization lock before
 * entering this monitor; they must never acquire that lock from a decision callback.
 * A completed candidate is logical eligibility, not proof that a filesystem write was
 * atomic with failure. A failed or unknown in-flight write remains recovery-only.
 */
public final class InvocationFence {
    public enum CandidateKind { CURRENT, RECEIPT }

    public static final class Ticket {
        private final InvocationFence owner;
        private final long epoch;
        private volatile String firstFailure;
        private SessionGate gate;
        private boolean currentStarted, currentPublished, receiptStarted, receiptCommitted, terminal;

        private Ticket(InvocationFence owner, long epoch) { this.owner = owner; this.epoch = epoch; }
        public long epoch() { return epoch; }
        public String firstFailure() { return firstFailure; }
    }

    public static final class Candidate {
        private final Ticket ticket;
        private final CandidateKind kind;
        private boolean finished;
        private Candidate(Ticket ticket, CandidateKind kind) { this.ticket = ticket; this.kind = kind; }
    }

    private long epoch;
    private Ticket current;

    /** Admission stays occupied even when creation, persistence or failure closing is unknown. */
    public synchronized Ticket begin() {
        if (current != null && !current.terminal) throw new IllegalStateException("old invocation nonterminal");
        long next = Math.incrementExact(epoch);
        current = new Ticket(this, next);
        epoch = next;
        return current;
    }

    public synchronized void attach(Ticket ticket, SessionGate gate) {
        requireOwnedCurrent(ticket);
        if (ticket.gate != null) throw new IllegalStateException("session already attached");
        ticket.gate = Objects.requireNonNull(gate, "gate");
        if (ticket.firstFailure != null) gate.revoke(ticket.firstFailure);
    }

    public synchronized boolean isCurrentOpen(Ticket ticket) {
        return isAdmitted(ticket) && ticket.gate != null && !ticket.gate.isRevoked();
    }

    /** Creation-only check: unlike callback authorization, a Gate does not exist yet. */
    public synchronized boolean isAdmitted(Ticket ticket) {
        return ownedCurrent(ticket) && ticket.firstFailure == null && !ticket.terminal;
    }

    /** Only memory mutations are permitted in decision. Publication must already be confirmed. */
    public synchronized boolean decide(Ticket ticket, BooleanSupplier decision) {
        Objects.requireNonNull(decision, "decision");
        return isCurrentOpen(ticket) && ticket.currentPublished && decision.getAsBoolean();
    }

    /** Revokes only this ticket's gate. A stale catch cannot close a later invocation. */
    public synchronized void fail(Ticket ticket, String reason) {
        if (ticket == null || ticket.owner != this) throw new IllegalArgumentException("foreign invocation");
        if (ticket.firstFailure == null)
            ticket.firstFailure = reason == null || reason.trim().isEmpty() ? "INVOCATION_FAILED" : reason;
        if (ticket.gate != null) ticket.gate.revoke(ticket.firstFailure);
        if (ownedCurrent(ticket)) ticket.terminal = false;
        // No successful failure path opens admission. Only verified healthy terminal completion does.
    }

    /** The single-use candidate must be obtained before starting the corresponding slow write. */
    public synchronized Candidate beginCandidate(Ticket ticket, CandidateKind kind) {
        Objects.requireNonNull(kind, "kind");
        if (!isCurrentOpen(ticket)) throw new IllegalStateException("invocation fenced");
        if (kind == CandidateKind.CURRENT) {
            if (ticket.currentStarted) throw new IllegalStateException("current publication already attempted");
            ticket.currentStarted = true;
        } else {
            if (!ticket.currentPublished || ticket.receiptStarted || !ticket.gate.canPrepare())
                throw new IllegalStateException("receipt prerequisites incomplete");
            ticket.receiptStarted = true;
        }
        return new Candidate(ticket, kind);
    }

    /** False after failure means preserve the written candidate; do not bind, commit or clean it. */
    public synchronized boolean finishCandidate(Candidate candidate) {
        if (candidate == null || candidate.ticket.owner != this || candidate.finished) return false;
        candidate.finished = true;
        Ticket ticket = candidate.ticket;
        if (!isCurrentOpen(ticket)) return false;
        if (candidate.kind == CandidateKind.CURRENT) ticket.currentPublished = true;
        else {
            if (!ticket.currentPublished || !ticket.gate.canPrepare()) return false;
            ticket.receiptCommitted = true;
        }
        return true;
    }

    /** Caller must first verify the existing strict terminal cleanup proof under its own protocol. */
    public synchronized boolean markTerminal(Ticket ticket) {
        if (!ownedCurrent(ticket)) return false;
        if (ticket.terminal) return ticket.firstFailure == null;
        if (!isCurrentOpen(ticket) || !ticket.currentPublished || !ticket.receiptCommitted
                || !ticket.gate.canPrepare()) return false;
        ticket.terminal = true;
        return true;
    }

    public synchronized boolean isTerminal(Ticket ticket) {
        return ticket != null && ticket.owner == this && ticket.terminal;
    }

    private boolean ownedCurrent(Ticket ticket) {
        return ticket != null && ticket.owner == this && ticket == current && ticket.epoch == epoch;
    }

    private void requireOwnedCurrent(Ticket ticket) {
        if (!ownedCurrent(ticket)) throw new IllegalArgumentException("stale or foreign invocation");
    }
}
