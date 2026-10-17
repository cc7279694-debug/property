package com.cooper.lithic.m10r3.probe;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.After;
import org.junit.Test;

/** Runs the actual serial command owner; no Android, filesystem durability or device claim. */
public final class HostCommandQueueTest {
    private final List<HostCommandQueue> queues = new ArrayList<>();

    private HostCommandQueue queue(int capacity, HostCommandQueue.Initializer init,
            HostCommandQueue.Observer observer) {
        HostCommandQueue q = new HostCommandQueue(capacity, init, observer);
        queues.add(q);
        return q;
    }
    private static void await(CountDownLatch latch) throws Exception {
        assertTrue("bounded fixture completion", latch.await(5, TimeUnit.SECONDS));
    }
    private static void hold(CountDownLatch latch) throws InterruptedException {
        if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("fixture release missing");
    }
    @After public void releaseQueues() {
        for (HostCommandQueue queue : queues) queue.close();
    }

    @Test public void initializationRunsOffSubmittingThread() throws Exception {
        long caller = Thread.currentThread().getId();
        AtomicLong initializer = new AtomicLong(caller);
        CountDownLatch initialized = new CountDownLatch(1);
        queue(4, () -> { initializer.set(Thread.currentThread().getId()); initialized.countDown(); }, null);
        await(initialized);
        assertNotEquals("bootstrap must not perform I/O on caller", caller, initializer.get());
    }
    @Test public void blockedBootstrapDoesNotBlockSubmitOrClaim() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger claims = new AtomicInteger(), tasks = new AtomicInteger();
        HostCommandQueue q = queue(2, () -> { entered.countDown(); hold(release); },
                (c, r, o, e) -> done.countDown());
        try {
            await(entered);
            assertEquals(HostCommandQueue.Readiness.BOOTSTRAPPING, q.readiness());
            assertEquals(HostCommandQueue.Submission.ACCEPTED,
                    q.submit("run", "boot_hold", () -> { claims.incrementAndGet(); return true; }, tasks::incrementAndGet));
            assertEquals(0, claims.get()); assertEquals(0, tasks.get());
        } finally { release.countDown(); }
        await(done); assertEquals(1, claims.get()); assertEquals(1, tasks.get());
    }
    @Test public void bootstrapClaimAndTaskUseSameSerialWorker() throws Exception {
        List<Long> threads = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch done = new CountDownLatch(1);
        HostCommandQueue q = queue(2, () -> threads.add(Thread.currentThread().getId()),
                (c, r, o, e) -> done.countDown());
        assertEquals(HostCommandQueue.Submission.ACCEPTED,
                q.submit("run", "serial", () -> { threads.add(Thread.currentThread().getId()); return true; },
                        () -> threads.add(Thread.currentThread().getId())));
        await(done); assertEquals(3, threads.size());
        assertEquals(1, new HashSet<>(threads).size());
        assertNotEquals(Long.valueOf(Thread.currentThread().getId()), threads.get(0));
    }
    @Test public void taskCannotStartBeforeClaimReturnsDurablyConfirmed() throws Exception {
        CountDownLatch claimEntered = new CountDownLatch(1), claimRelease = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger tasks = new AtomicInteger();
        HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> done.countDown());
        try {
            q.submit("run", "claim_hold", () -> { claimEntered.countDown(); hold(claimRelease); return true; }, tasks::incrementAndGet);
            await(claimEntered); assertEquals(0, tasks.get());
        } finally { claimRelease.countDown(); }
        await(done); assertEquals(1, tasks.get());
    }
    @Test public void initializationFailureRejectsAlreadyQueuedCommandWithoutClaim() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger claims = new AtomicInteger(), tasks = new AtomicInteger();
        Exception failure = new Exception("owner identity not established");
        List<HostCommandQueue.Outcome> outcomes = new ArrayList<>();
        HostCommandQueue q = queue(2, () -> { entered.countDown(); hold(release); throw failure; },
                (c, r, o, e) -> { outcomes.add(o); assertSame(failure, e); done.countDown(); });
        try {
            await(entered);
            assertEquals(HostCommandQueue.Submission.ACCEPTED,
                    q.submit("run", "queued_unknown", () -> { claims.incrementAndGet(); return true; }, tasks::incrementAndGet));
        } finally { release.countDown(); }
        await(done); assertEquals(HostCommandQueue.Readiness.UNKNOWN, q.readiness());
        assertSame(failure, q.initializationError());
        assertEquals(Collections.singletonList(HostCommandQueue.Outcome.INITIALIZATION_UNKNOWN), outcomes);
        assertEquals(0, claims.get()); assertEquals(0, tasks.get());
    }
    @Test public void initializationUnknownIsStickyForLaterSubmissions() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicInteger claims = new AtomicInteger();
        HostCommandQueue q = queue(2, () -> { throw new Exception("unknown owner"); },
                (c, r, o, e) -> done.countDown());
        HostCommandQueue.Submission submitted = q.submit("run", "first", () -> true, () -> {});
        if (submitted == HostCommandQueue.Submission.ACCEPTED) await(done);
        assertEquals(HostCommandQueue.Readiness.UNKNOWN, q.readiness());
        assertEquals(HostCommandQueue.Submission.INITIALIZATION_UNKNOWN,
                q.submit("run", "second", () -> { claims.incrementAndGet(); return true; }, () -> fail("no task")));
        assertEquals(0, claims.get());
    }
    @Test public void claimFalseNeverInvokesTaskAndIsReportedAsReplay() throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicInteger tasks = new AtomicInteger();
        List<HostCommandQueue.Outcome> outcomes = new ArrayList<>();
        HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> { outcomes.add(o); assertNull(e); done.countDown(); });
        q.submit("run", "replay", () -> false, tasks::incrementAndGet);
        await(done); assertEquals(0, tasks.get());
        assertEquals(Collections.singletonList(HostCommandQueue.Outcome.CLAIM_REPLAY), outcomes);
    }
    @Test public void claimExceptionNeverInvokesTaskAndRetainsOriginalError() throws Exception {
        CountDownLatch done = new CountDownLatch(1); AtomicInteger tasks = new AtomicInteger();
        Exception failed = new Exception("claim fsync unknown");
        HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> {
            assertEquals(HostCommandQueue.Outcome.CLAIM_UNKNOWN, o); assertSame(failed, e); done.countDown();
        });
        q.submit("run", "claim_error", () -> { throw failed; }, tasks::incrementAndGet);
        await(done); assertEquals(0, tasks.get());
    }
    @Test public void completedKeyIsReleasedButDurableClaimStillRejectsReplay() throws Exception {
        Set<String> claims = new HashSet<>(); AtomicInteger tasks = new AtomicInteger();
        CountDownLatch first = new CountDownLatch(1), second = new CountDownLatch(1);
        List<HostCommandQueue.Outcome> outcomes = new ArrayList<>();
        HostCommandQueue q = queue(3, () -> {}, (c, r, o, e) -> {
            outcomes.add(o); if(outcomes.size()==1)first.countDown(); else second.countDown();
        });
        HostCommandQueue.Claim durable = () -> claims.add("run:same_run");
        assertEquals(HostCommandQueue.Submission.ACCEPTED, q.submit("run", "same_run", durable, tasks::incrementAndGet));
        await(first);
        assertEquals(HostCommandQueue.Submission.ACCEPTED, q.submit("run", "same_run", durable, tasks::incrementAndGet));
        await(second); assertEquals(1, tasks.get());
        assertEquals(java.util.Arrays.asList(HostCommandQueue.Outcome.EXECUTED,HostCommandQueue.Outcome.CLAIM_REPLAY),outcomes);
    }
    @Test public void inFlightReplayIsRejectedWithoutWaitingBehindActiveInvocation() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);
        AtomicInteger claims=new AtomicInteger(),tasks=new AtomicInteger();
        HostCommandQueue q=queue(2,()->{},(c,r,o,e)->done.countDown());
        try {
            assertEquals(HostCommandQueue.Submission.ACCEPTED,q.submit("run","active_replay",()->{claims.incrementAndGet();return true;},()->{tasks.incrementAndGet();entered.countDown();hold(release);}));
            await(entered);
            assertEquals(HostCommandQueue.Submission.DUPLICATE,q.submit("run","active_replay",()->{claims.incrementAndGet();return true;},tasks::incrementAndGet));
            assertEquals(1,claims.get());assertEquals(1,tasks.get());
        } finally {release.countDown();}
        await(done);
    }
    @Test public void concurrentReplayHasExactlyOneAcceptedInMemoryOwner() throws Exception {
        CountDownLatch initialized=new CountDownLatch(1),release=new CountDownLatch(1),start=new CountDownLatch(1),done=new CountDownLatch(1);
        AtomicInteger accepted=new AtomicInteger(),duplicates=new AtomicInteger(),claims=new AtomicInteger(),tasks=new AtomicInteger();
        HostCommandQueue q=queue(4,()->{initialized.countDown();hold(release);},(c,r,o,e)->done.countDown());
        List<Thread> callers=new ArrayList<>();
        try {
            await(initialized);
            for(int i=0;i<12;i++){
                Thread caller=new Thread(()->{
                    try{hold(start);}catch(InterruptedException e){throw new AssertionError(e);}
                    HostCommandQueue.Submission value=q.submit("run","same_key",()->{claims.incrementAndGet();return true;},tasks::incrementAndGet);
                    if(value==HostCommandQueue.Submission.ACCEPTED)accepted.incrementAndGet();
                    else if(value==HostCommandQueue.Submission.DUPLICATE)duplicates.incrementAndGet();
                });callers.add(caller);caller.start();
            }
            start.countDown();for(Thread caller:callers){caller.join(5000);assertFalse(caller.isAlive());}
            assertEquals(1,accepted.get());assertEquals(11,duplicates.get());assertEquals(0,claims.get());
        } finally {start.countDown();release.countDown();}
        await(done);assertEquals(1,claims.get());assertEquals(1,tasks.get());
    }
    @Test public void fullQueueRejectsWithoutCallerExecutionOrClaim() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger rejectedClaims = new AtomicInteger(), rejectedTasks = new AtomicInteger();
        HostCommandQueue q = queue(1, () -> { entered.countDown(); hold(release); },
                (c, r, o, e) -> done.countDown());
        try {
            await(entered);
            assertEquals(HostCommandQueue.Submission.ACCEPTED, q.submit("run", "queued", () -> true, () -> {}));
            assertEquals(HostCommandQueue.Submission.QUEUE_FULL,
                    q.submit("run", "over_capacity", () -> { rejectedClaims.incrementAndGet(); return true; }, rejectedTasks::incrementAndGet));
            assertEquals(0, rejectedClaims.get()); assertEquals(0, rejectedTasks.get());
        } finally { release.countDown(); }
        await(done);
    }
    @Test public void queuedInvocationsNeverOverlap() throws Exception {
        CountDownLatch taskEntered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(2);
        AtomicInteger active = new AtomicInteger(), maximum = new AtomicInteger();
        HostCommandQueue q = queue(3, () -> {}, (c, r, o, e) -> done.countDown());
        try {
            q.submit("run", "first", () -> true, () -> {
                maximum.accumulateAndGet(active.incrementAndGet(), Math::max); taskEntered.countDown();
                try { hold(release); } finally { active.decrementAndGet(); }
            });
            await(taskEntered);
            q.submit("run", "second", () -> true, () -> {
                maximum.accumulateAndGet(active.incrementAndGet(), Math::max); active.decrementAndGet();
            });
            assertEquals(1, active.get());
        } finally { release.countDown(); }
        await(done); assertEquals(1, maximum.get());
    }
    @Test public void malformedCommandIsRejectedBeforeAnyClaim() {
        AtomicInteger claims = new AtomicInteger(); HostCommandQueue q = queue(2, () -> {}, null);
        for (String command : new String[]{null, "", "RUN", "run;rm", "123", "abcdefghijklmnopqrstu"}) {
            assertEquals(HostCommandQueue.Submission.INVALID,
                    q.submit(command, "valid", () -> { claims.incrementAndGet(); return true; }, () -> fail("no task")));
        }
        assertEquals(0, claims.get());
    }
    @Test public void malformedRunIdIsRejectedBeforeAnyClaim() {
        AtomicInteger claims = new AtomicInteger(); HostCommandQueue q = queue(2, () -> {}, null);
        for (String run : new String[]{null, "", "../old", "a b", "中", String.join("", Collections.nCopies(81, "a"))}) {
            assertEquals(HostCommandQueue.Submission.INVALID,
                    q.submit("run", run, () -> { claims.incrementAndGet(); return true; }, () -> fail("no task")));
        }
        assertEquals(0, claims.get());
    }
    @Test public void missingClaimOrTaskIsRejectedWithoutSideEffects() {
        HostCommandQueue q = queue(2, () -> {}, null); AtomicInteger claims = new AtomicInteger();
        assertEquals(HostCommandQueue.Submission.INVALID, q.submit("run", "missing_claim", null, () -> fail("no task")));
        assertEquals(HostCommandQueue.Submission.INVALID,
                q.submit("run", "missing_task", () -> { claims.incrementAndGet(); return true; }, null));
        assertEquals(0, claims.get());
    }
    @Test public void taskFailureIsReportedWithoutRepeatingClaimOrTask() throws Exception {
        AtomicInteger claims = new AtomicInteger(), tasks = new AtomicInteger(); CountDownLatch done = new CountDownLatch(1);
        Exception failed = new Exception("invocation failed");
        HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> {
            assertEquals(HostCommandQueue.Outcome.TASK_FAILED, o); assertSame(failed, e); done.countDown();
        });
        q.submit("run", "failed_task", () -> { claims.incrementAndGet(); return true; },
                () -> { tasks.incrementAndGet(); throw failed; });
        await(done); assertEquals(1, claims.get()); assertEquals(1, tasks.get());
    }
    @Test public void observerFailureIsExplicitlyRetained() throws Exception {
        CountDownLatch task = new CountDownLatch(1), observed = new CountDownLatch(1);
        Exception failed = new Exception("diagnostic unavailable");
        HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> { observed.countDown(); throw failed; });
        q.submit("run", "observer_unknown", () -> true, task::countDown); await(task); await(observed);
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (q.observerError() == null && System.nanoTime() < until) Thread.yield();
        assertSame(failed, q.observerError()); assertEquals(1, q.observerFailures());
    }
    @Test public void closeRejectsNewCommandsWithoutInterruptingActiveTask() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger interrupted = new AtomicInteger(); HostCommandQueue q = queue(2, () -> {}, (c, r, o, e) -> done.countDown());
        try {
            q.submit("run", "active", () -> true, () -> {
                entered.countDown(); try { hold(release); } catch (InterruptedException e) { interrupted.incrementAndGet(); throw e; }
            });
            await(entered); q.close();
            assertEquals(HostCommandQueue.Submission.CLOSED, q.submit("run", "after_close", () -> failClaim(), () -> fail("no task")));
            assertEquals(0, interrupted.get());
        } finally { release.countDown(); }
        await(done); assertEquals(0, interrupted.get());
    }
    @Test public void invalidCapacityOrMissingInitializerCannotCreateExecutionPath() {
        for (int capacity : new int[]{0, -1}) {
            try { new HostCommandQueue(capacity, () -> {}); fail("invalid capacity accepted"); }
            catch (IllegalArgumentException expected) { }
        }
        try { new HostCommandQueue(2, null); fail("missing initializer accepted"); }
        catch (NullPointerException expected) { }
    }
    private static boolean failClaim() { fail("claim must not execute"); return false; }
}
