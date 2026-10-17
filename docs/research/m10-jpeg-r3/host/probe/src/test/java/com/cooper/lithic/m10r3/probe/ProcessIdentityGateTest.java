package com.cooper.lithic.m10r3.probe;

import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

/** Real research identity gate only; no Android startup, Binder or ANR claim. */
public final class ProcessIdentityGateTest {
    private static final String HOST="com.cooper.lithic.m10r3.probe";
    private static final String WORKER=HOST+":jpeg_worker";
    private final ConcurrentLinkedQueue<Throwable> threadFailures=new ConcurrentLinkedQueue<>();
    private ProcessIdentityGate gate(){return new ProcessIdentityGate(2);}
    private ProcessIdentityGate.NanoClock clock(long before,long after){AtomicInteger n=new AtomicInteger();return ()->n.getAndIncrement()==0?before:after;}
    private String main(ProcessIdentityGate g,int api,String read){return g.role("UNKNOWN",api,true,()->read,clock(10,20));}
    private void verified(ProcessIdentityGate g,String name){assertNotEquals("REJECT",g.beginBackground(name));assertTrue(g.completeBackground());}
    private void await(CountDownLatch latch)throws Exception{assertTrue("test synchronization",latch.await(2,TimeUnit.SECONDS));}
    private Thread thread(Runnable task){Thread t=new Thread(()->{try{task.run();}catch(Throwable failure){threadFailures.add(failure);}},"identity-test");t.setDaemon(true);t.start();return t;}
    private void joined(Thread t)throws Exception{t.join(2000);assertFalse("test thread unfinished",t.isAlive());if(!threadFailures.isEmpty()){AssertionError failure=new AssertionError("background test failure");failure.initCause(threadFailures.peek());throw failure;}}

    @Test public void api26MainPendingProbeRecognizesWorkerWithoutReady(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,26,WORKER));assertEquals(ProcessIdentityGate.State.PENDING,g.state());assertEquals("REJECT",g.verifiedRole());}
    @Test public void api27MainPendingProbeRecognizesWorkerWithoutReady(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,27,WORKER));assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void api26HostProbeNeverMasqueradesAsWorker(){assertEquals("HOST",main(gate(),26,HOST));}
    @Test public void unknownProcessNameCannotDefaultToWorker(){ProcessIdentityGate g=gate();assertEquals("REJECT",main(g,26,"unknown"));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void arbitraryCallerUnknownIsRejectedWithoutReading(){AtomicInteger reads=new AtomicInteger();assertEquals("REJECT",gate().role("unknown",26,true,()->{reads.incrementAndGet();return WORKER;},clock(1,2)));assertEquals(0,reads.get());}
    @Test public void nullCallerIsRejectedWithoutReading(){AtomicInteger reads=new AtomicInteger();assertEquals("REJECT",gate().role(null,26,true,()->{reads.incrementAndGet();return WORKER;},clock(1,2)));assertEquals(0,reads.get());}
    @Test public void pendingProbeCannotAuthorizeBinder(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,26,WORKER));assertEquals("REJECT",g.role("UNKNOWN",26,false,()->WORKER,clock(1,2)));assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void frameworkHintDoesNotAuthorizeEarlyBinder(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER);assertEquals("REJECT",g.role(WORKER,28,false,()->WORKER,clock(1,2)));assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void api28MainUsesHintWithoutProcRead(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER);AtomicInteger reads=new AtomicInteger();assertEquals("WORKER",g.role(WORKER,28,true,()->{reads.incrementAndGet();throw new IOException();},clock(1,2)));assertEquals(0,reads.get());assertEquals("REJECT",g.verifiedRole());}
    @Test public void api28WithoutHintCannotGuessWorker(){assertEquals("REJECT",main(gate(),28,WORKER));}
    @Test public void frameworkHintConflictIsStickyFailure(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER);g.frameworkHint(HOST);assertEquals(ProcessIdentityGate.State.FAILED,g.state());g.frameworkHint(WORKER);assertEquals("REJECT",g.beginBackground(WORKER));assertFalse(g.completeBackground());}
    @Test public void malformedFrameworkHintIsRejected(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER+" ");assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void independentBackgroundObservationDoesNotSetReady(){ProcessIdentityGate g=gate();assertEquals("WORKER",g.beginBackground(WORKER));assertEquals(ProcessIdentityGate.State.PENDING,g.state());assertEquals("REJECT",g.verifiedRole());}
    @Test public void completeWithoutBackgroundObservationFailsClosed(){ProcessIdentityGate g=gate();assertFalse(g.completeBackground());assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void successfulBackgroundConfirmsRole(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,26,WORKER));verified(g,WORKER);assertEquals("WORKER",g.verifiedRole());}
    @Test public void failedBackgroundCannotBeBypassedByKnownClaim(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER);g.fail("owner or role initialization failed");assertEquals("REJECT",g.role(WORKER,28,false,()->WORKER,clock(1,2)));}
    @Test public void failedGateRejectsKnownMainClaim(){ProcessIdentityGate g=gate();verified(g,WORKER);g.fail("late conflict");assertEquals("REJECT",g.role(WORKER,26,true,()->WORKER,clock(1,2)));}
    @Test public void repeatedFailureKeepsFirstReason(){ProcessIdentityGate g=gate();g.fail("first");g.fail("second");assertEquals("first",g.failureReason());}
    @Test public void successfulBackgroundCannotWashAwayProbeFailure(){ProcessIdentityGate g=gate();assertEquals("REJECT",main(g,26,"bad"));assertEquals("REJECT",g.beginBackground(WORKER));assertFalse(g.completeBackground());assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void probeVersusBackgroundConflictIsSticky(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,26,WORKER));assertEquals("REJECT",g.beginBackground(HOST));assertFalse(g.completeBackground());}
    @Test public void frameworkVersusBackgroundConflictIsSticky(){ProcessIdentityGate g=gate();g.frameworkHint(WORKER);assertEquals("REJECT",g.beginBackground(HOST));assertEquals("REJECT",g.verifiedRole());}
    @Test public void repeatedBackgroundMismatchCannotReplaceVerifiedRole(){ProcessIdentityGate g=gate();verified(g,WORKER);assertEquals("REJECT",g.beginBackground(HOST));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void unknownBackgroundObservationFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.beginBackground("UNKNOWN"));assertFalse(g.completeBackground());}
    @Test public void readerIOExceptionIsStickyFailure(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->{throw new IOException("read failed");},clock(1,2)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void missingReaderFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,null,clock(1,2)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void missingClockFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,null));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void clockExceptionFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,()->{throw new IOException("clock unavailable");}));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void clockBackwardsFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,clock(20,10)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void clockSubtractionOverflowFailsClosed(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,clock(Long.MIN_VALUE,Long.MAX_VALUE)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void exactProbeTimeBudgetRejects(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,clock(10,100000010)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void subBudgetProbeCanReturnRoleOnly(){ProcessIdentityGate g=gate();assertEquals("WORKER",g.role("UNKNOWN",26,true,()->WORKER,clock(10,100000009)));assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void callerConflictCannotBorrowVerifiedWorkerIdentity(){ProcessIdentityGate g=gate();verified(g,WORKER);assertEquals("REJECT",g.role(HOST,28,false,()->WORKER,clock(1,2)));}
    @Test public void staleUnknownCanObserveCompletedVerifiedIdentity(){ProcessIdentityGate g=gate();verified(g,WORKER);assertEquals("WORKER",g.role("UNKNOWN",26,false,()->{throw new IOException();},clock(1,2)));}
    @Test public void readyMainDoesNotRepeatProcProbe(){ProcessIdentityGate g=gate();verified(g,WORKER);AtomicInteger reads=new AtomicInteger();assertEquals("WORKER",g.role(WORKER,26,true,()->{reads.incrementAndGet();return WORKER;},clock(1,2)));assertEquals(0,reads.get());}
    @Test public void apiBelow26RemainsUnsupported(){assertEquals("REJECT",main(gate(),25,WORKER));}
    @Test public void binderEarlyCanProceedOnlyAfterBackgroundCompletion()throws Exception{ProcessIdentityGate g=new ProcessIdentityGate(1000);CountDownLatch entered=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();Thread t=thread(()->{entered.countDown();result.set(g.role("UNKNOWN",26,false,()->WORKER,clock(1,2)));});await(entered);verified(g,WORKER);joined(t);assertEquals("WORKER",result.get());}
    @Test public void binderTimeoutDoesNotDeclareGlobalReady(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,false,()->WORKER,clock(1,2)));assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void binderInterruptionRestoresInterruptAndRejects(){ProcessIdentityGate g=gate();Thread.currentThread().interrupt();try{assertEquals("REJECT",g.role("UNKNOWN",26,false,()->WORKER,clock(1,2)));assertTrue(Thread.currentThread().isInterrupted());}finally{Thread.interrupted();}}
    @Test public void mainProbeNeverWaitsForBackground(){ProcessIdentityGate g=gate();AtomicInteger calls=new AtomicInteger();assertEquals("WORKER",g.role("UNKNOWN",26,true,()->{calls.incrementAndGet();return WORKER;},clock(1,2)));assertEquals(1,calls.get());assertEquals(ProcessIdentityGate.State.PENDING,g.state());}
    @Test public void backgroundCannotBecomeReadyWhileSyncProbeIsUnresolved()throws Exception{ProcessIdentityGate g=gate();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<String> role=new AtomicReference<>();Thread t=thread(()->role.set(g.role("UNKNOWN",26,true,()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException("test timeout");return WORKER;},clock(1,2))));await(entered);assertEquals("WORKER",g.beginBackground(WORKER));assertFalse(g.completeBackground());assertEquals("REJECT",g.verifiedRole());release.countDown();joined(t);assertEquals("WORKER",role.get());assertEquals("WORKER",g.verifiedRole());}
    @Test public void lateConflictingProbeCannotPublishOverCompletedBackground()throws Exception{ProcessIdentityGate g=gate();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<String> role=new AtomicReference<>();Thread t=thread(()->role.set(g.role("UNKNOWN",26,true,()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException("test timeout");return HOST;},clock(1,2))));await(entered);assertEquals("WORKER",g.beginBackground(WORKER));assertFalse(g.completeBackground());release.countDown();joined(t);assertEquals("REJECT",role.get());assertEquals(ProcessIdentityGate.State.FAILED,g.state());assertEquals("REJECT",g.verifiedRole());}
    @Test public void concurrentConsistentProbesRequireAllToFinishBeforeReady()throws Exception{ProcessIdentityGate g=gate();CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);ProcessIdentityGate.NameReader reader=()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException("test timeout");return WORKER;};Thread a=thread(()->g.role("UNKNOWN",26,true,reader,clock(1,2)));Thread b=thread(()->g.role("UNKNOWN",27,true,reader,clock(1,2)));await(entered);assertEquals("WORKER",g.beginBackground(WORKER));assertFalse(g.completeBackground());assertEquals("REJECT",g.verifiedRole());release.countDown();joined(a);joined(b);assertEquals("WORKER",g.verifiedRole());}
    @Test public void concurrentConflictingProbesCannotAuthorizeBinder()throws Exception{ProcessIdentityGate g=gate();CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);Thread a=thread(()->g.role("UNKNOWN",26,true,()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException();return HOST;},clock(1,2)));Thread b=thread(()->g.role("UNKNOWN",27,true,()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException();return WORKER;},clock(1,2)));await(entered);assertEquals("WORKER",g.beginBackground(WORKER));assertFalse(g.completeBackground());release.countDown();joined(a);joined(b);assertEquals(ProcessIdentityGate.State.FAILED,g.state());assertEquals("REJECT",g.verifiedRole());}
    @Test public void failureDuringUnlockedProbeCannotBeOverwritten()throws Exception{ProcessIdentityGate g=gate();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicReference<String> role=new AtomicReference<>();Thread t=thread(()->role.set(g.role("UNKNOWN",26,true,()->{entered.countDown();if(!release.await(2,TimeUnit.SECONDS))throw new IOException();return WORKER;},clock(1,2))));await(entered);g.fail("parallel failure");release.countDown();joined(t);assertEquals("REJECT",role.get());assertEquals("parallel failure",g.failureReason());}
    @Test public void parserAcceptsHealthyWorkerCmdline()throws Exception{assertEquals(WORKER,ProcessIdentityGate.readCmdline(new ByteArrayInputStream((WORKER+'\0').getBytes(StandardCharsets.US_ASCII))));}
    @Test public void parserAcceptsHealthyHostCmdline()throws Exception{assertEquals(HOST,ProcessIdentityGate.readCmdline(new ByteArrayInputStream((HOST+'\0').getBytes(StandardCharsets.US_ASCII))));}
    @Test public void parserAllowsTrailingArgumentsOnlyAfterNul()throws Exception{assertEquals(WORKER,ProcessIdentityGate.readCmdline(new ByteArrayInputStream((WORKER+'\0'+"ignored arguments").getBytes(StandardCharsets.US_ASCII))));}
    @Test public void parserHandlesShortReadsWithoutExceedingBound()throws Exception{byte[] bytes=(WORKER+'\0').getBytes(StandardCharsets.US_ASCII);InputStream input=new ByteArrayInputStream(bytes){@Override public synchronized int read(byte[] b,int off,int len){return super.read(b,off,Math.min(len,3));}};assertEquals(WORKER,ProcessIdentityGate.readCmdline(input));}
    @Test public void parserRejectsEmptyInput(){rejectBytes(new byte[0]);}
    @Test public void parserRejectsEmptyFirstArg(){rejectBytes(new byte[]{0});}
    @Test public void parserRejectsMissingNul(){rejectBytes(WORKER.getBytes(StandardCharsets.US_ASCII));}
    @Test public void parserRejectsUnknownValidAsciiName(){rejectBytes("unknown\0".getBytes(StandardCharsets.US_ASCII));}
    @Test public void parserRejectsNonAscii(){rejectBytes(new byte[]{(byte)0xC3,(byte)0xA9,0});}
    @Test public void parserRejectsControlCharacters(){rejectBytes((WORKER+'\n'+'\0').getBytes(StandardCharsets.US_ASCII));}
    @Test public void parserNeverConsumesBeyond256Bytes()throws Exception{AtomicInteger read=new AtomicInteger();InputStream input=new InputStream(){public int read(){read.incrementAndGet();return 'x';}public int read(byte[] b,int off,int len){for(int i=0;i<len;i++)b[off+i]='x';read.addAndGet(len);return len;}};try{ProcessIdentityGate.readCmdline(input);fail("unbounded name accepted");}catch(IOException expected){}assertEquals(256,read.get());}
    @Test public void parserRejectsZeroProgressRead(){InputStream in=new InputStream(){public int read(){return 0;}public int read(byte[] b,int o,int n){return 0;}};try{ProcessIdentityGate.readCmdline(in);fail("zero progress accepted");}catch(IOException expected){}}
    @Test public void parserPropagatesReadFailure(){try{ProcessIdentityGate.readCmdline(new InputStream(){public int read()throws IOException{throw new IOException("input failure");}});fail("read failed");}catch(IOException expected){assertEquals("input failure",expected.getMessage());}}
    @Test public void invalidCallerDoesNotPoisonIndependentVerification(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("unknown",26,true,()->WORKER,clock(1,2)));verified(g,WORKER);assertEquals("WORKER",g.verifiedRole());}
    @Test public void snapshotHasExplicitPendingAndUnknownDuration(){ProcessIdentityGate g=gate();assertEquals("PENDING",g.snapshot().get("state"));assertEquals(-1L,g.snapshot().get("lastProbeNanos"));assertEquals(0L,g.snapshot().get("probeCount"));}
    @Test public void snapshotReportsRoleOnlyProbeAndMeasuredNanos(){ProcessIdentityGate g=gate();assertEquals("WORKER",main(g,26,WORKER));assertEquals(WORKER,g.snapshot().get("fastObservation"));assertEquals(1L,g.snapshot().get("probeCount"));assertEquals(10L,g.snapshot().get("lastProbeNanos"));assertEquals(false,g.snapshot().get("backgroundCompleted"));}
    @Test public void snapshotMutationCannotModifyGate(){ProcessIdentityGate g=gate();g.snapshot().put("state","READY");assertEquals(ProcessIdentityGate.State.PENDING,g.state());assertEquals("REJECT",g.verifiedRole());}
    @Test public void repeatedCompleteCannotReopenFailedGate(){ProcessIdentityGate g=gate();verified(g,WORKER);g.fail("late failure");assertFalse(g.completeBackground());assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void negativeStartClockIsUnknownEvenWhenDeltaPositive(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,clock(-10,10)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void negativeFinishClockIsUnknownEvenWhenDeltaPositive(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->WORKER,clock(-20,-10)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void resourceCloseFailureIsStickyFailure(){ProcessIdentityGate g=gate();assertEquals("REJECT",g.role("UNKNOWN",26,true,()->{try(InputStream input=new ByteArrayInputStream((WORKER+'\0').getBytes(StandardCharsets.US_ASCII)){@Override public void close()throws IOException{throw new IOException("close failed");}}){return ProcessIdentityGate.readCmdline(input);}},clock(1,2)));assertEquals(ProcessIdentityGate.State.FAILED,g.state());}
    @Test public void binderReadyAfterOriginalWaitDeadlineStillRejects()throws Exception{
        ProcessIdentityGate g=new ProcessIdentityGate(1000);
        AtomicReference<String> result=new AtomicReference<>();
        Thread t=thread(()->result.set(g.role("UNKNOWN",26,false,()->WORKER,clock(1,2))));
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(t.getState()!=Thread.State.TIMED_WAITING&&System.nanoTime()<deadline)Thread.yield();
        assertEquals("fixture must observe actual gate wait",Thread.State.TIMED_WAITING,t.getState());
        synchronized(g){
            assertNull(result.get());
            while(t.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.yield();
            assertEquals("gate wait elapsed before READY publication",Thread.State.BLOCKED,t.getState());
            verified(g,WORKER);
        }
        joined(t);assertEquals("REJECT",result.get());assertEquals("WORKER",g.verifiedRole());
    }
    private void rejectBytes(byte[] bytes){try{ProcessIdentityGate.readCmdline(new ByteArrayInputStream(bytes));fail("invalid input accepted");}catch(IOException expected){}}
}
