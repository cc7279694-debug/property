package com.cooper.lithic.m10r3.probe;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.system.*;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

final class ProbeRunner {
    final Context context;final StageStore store;final File reports;
    final java.util.concurrent.atomic.AtomicInteger rejectedCommands=new java.util.concurrent.atomic.AtomicInteger();
    final InvocationFence invocations=new InvocationFence();
    static final class Invocation {
        InvocationFence.Ticket ticket;
        Session session;
        Runnable traceFence;
    }
    ProbeRunner(Context c)throws Exception {context=c.getApplicationContext();store=new StageStore(context);reports=new File(context.getFilesDir(),"r3-reports");if(!reports.isDirectory()&&!reports.mkdir())throw new IOException("reports");}
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean claim(Context context,String command,String run)throws Exception {
        if(run==null||!run.matches("[a-zA-Z0-9_-]{1,80}")||!command.matches("[a-z_]{1,20}"))return false;
        File commands=new File(context.getFilesDir(),"r3-command-claims");if(!commands.isDirectory()&&!commands.mkdir())throw new IOException("command claim directory");
        File file=StageStore.safe(commands,command+"-"+run+".claim");if(!file.createNewFile())return false;
        try(FileOutputStream o=new FileOutputStream(file)){o.write("CLAIMED_RESEARCH_COMMAND".getBytes(StandardCharsets.UTF_8));o.getFD().sync();}ProbeSupport.syncDir(commands);return true;
    }
    // Called only by ProbeApplication's single claimed-command background queue.
    void dispatch(Activity activity,String command,String run,String name,String fault){
            long start=SystemClock.elapsedRealtime();
            final Invocation invocation=new Invocation();
            try {
                if ("run".equals(command)||"fault".equals(command)) {
                    invocation.ticket=invocations.begin();
                    invocation.traceFence=()->invocations.fail(invocation.ticket,"DIAGNOSTIC_UNKNOWN");
                    ProbeSupport.setDiagnosticFence(invocation.traceFence);
                    require(!ProbeSupport.diagnosticUnknown(),"diagnostic evidence unknown before invocation");
                }
                ProbeSupport.event("CASE_BEGIN","runId",run,"command",command,"case",String.valueOf(name),"fault",String.valueOf(fault));
                JSONObject result;
                if(command.equals("recover"))result=recover();
                else if(command.equals("fault"))result=runCase(activity,run,"fault",fault,invocation);
                else if(command.equals("run"))result=runCase(activity,run,name,null,invocation);
                else throw new IllegalArgumentException("unknown command");
                result.put("status","PASS").put("runId",run).put("case",String.valueOf(name)).put("hostPid",android.os.Process.myPid()).put("hostUid",android.os.Process.myUid()).put("wallMs",SystemClock.elapsedRealtime()-start).put("formalReferences",0);
                ProbeSupport.event("CASE_PASS_CANDIDATE","runId",run);
                require(ProbeSupport.flushDiagnostics(2000),"diagnostic drain unknown before report");
                require(invocation.ticket==null||invocation.ticket.firstFailure()==null,"report eligibility fenced");
                if(invocation.session!=null)result.put("timing",new JSONObject(invocation.session.timeline.snapshot()));
                result.put("diagnostics",ProbeSupport.diagnosticSummary());
                StageStore.atomic(reports,run+".json",result);
                require(invocation.ticket==null||invocation.ticket.firstFailure()==null,"report write completed after failure: result unknown");
                if(invocation.session!=null&&invocation.session.timeline.handshakeReady())require(invocation.session.timeline.completed(),"total invocation deadline after report");
            }catch(Throwable failure){
                // Short memory fence FIRST. No trace, disk, Binder, or main-thread wait here.
                if(invocation.ticket!=null)invocations.fail(invocation.ticket,failure.toString());
                Session failed=invocation.session;
                if(failed!=null) {
                    try { store.revoke(failed.id); failed.durableFence="VERIFIED"; }
                    catch(Throwable revokeError) { failed.durableFence="UNKNOWN"; failure.addSuppressed(revokeError); }
                    failed.unbind(); // Keeps failed/unknown responsibility; never removes stage/pin.
                }
                ProbeSupport.event("CASE_FAIL_STOP","runId",run,"detail",failure.toString(),"firstFailure",invocation.ticket==null?"NO_INVOCATION":invocation.ticket.firstFailure(),"durableFence",failed==null?"NO_COMPLETE_SESSION":failed.durableFence);
                try{StageStore.atomic(reports,run+".json",new JSONObject().put("status","FAIL").put("runId",run).put("detail",failure.toString()).put("suppressed",Arrays.toString(failure.getSuppressed())).put("durableFence",failed==null?"NO_COMPLETE_SESSION":failed.durableFence)
                    .put("diagnostics",ProbeSupport.diagnosticSummary()).put("timing",failed==null?JSONObject.NULL:new JSONObject(failed.timeline.snapshot()))
                    .put("timingPhase",failed==null?"NOT_CREATED":failed.timeline.phase()).put("firstFailure",invocation.ticket==null?"NO_INVOCATION":String.valueOf(invocation.ticket.firstFailure())));}catch(Exception e){ProbeSupport.event("REPORT_ERROR","detail",e.toString());}
            } finally {
                if(invocation.session!=null)invocation.session.watchdog.close(); // Only timer observation ends; no resource/death claim.
                if(invocation.traceFence!=null)ProbeSupport.clearDiagnosticFence(invocation.traceFence);
            }
    }
    void fault(String run,String point,String requested,Session s)throws Exception {
        if(!point.equals(requested))return;
        JSONObject ready=new JSONObject().put("status","FAULT_READY").put("runId",run).put("fault",point).put("attemptId",s.id).put("hostPid",android.os.Process.myPid()).put("workerPid",s.workerPid).put("sourceExists",store.sourceExists(s.id));
        StageStore.atomic(reports,run+".ready.json",ready);ProbeSupport.event("FAULT_READY","runId",run,"point",point,"attempt",s.id,"workerPid",s.workerPid);
        new CountDownLatch(1).await(); // Only a controlled external command kills this Host itself.
    }
    JSONObject recover()throws Exception {
        throw new IOException("D2A_RECOVERY_NOT_AUTHORIZED: preserve all prior stages");
    }
    JSONObject runCase(Activity activity,String run,String name,String cut,Invocation invocation)throws Exception {
        require(ProbeSupport.mainOwner,"synthetic main owner missing");
        require("REJECT".equals(ProbeSupport.role("unknown")),"unknown role accepted");
        final Session s;
        synchronized(store.authorizationLock){
            // No Binder call/wait in this section. Failure preserves current.json;
            // a newly created but unpublished stage remains discoverable next time.
            store.assertAllTerminal(); // Ordinary run never repairs or clears a prior attempt.
            require(invocations.isAdmitted(invocation.ticket),"invocation fenced before create");
            s=new Session(invocation); invocation.session=s; invocations.attach(invocation.ticket,s.gate);
            InvocationFence.Candidate publication=invocations.beginCandidate(invocation.ticket,InvocationFence.CandidateKind.CURRENT);
            require(publication!=null,"current publication fenced");
            store.publishCurrent(s.id);
            require(invocations.finishCandidate(publication),"current publication result unknown after failure");
        }
        fault(run,"before_pin",cut,s);store.pin(s.id,"DELIVERY_UNKNOWN");fault(run,"after_pin",cut,s);
        if(name.equals("stage_model"))return stageModel(s);
        s.bind();
        while(!s.connected.await(20,TimeUnit.MILLISECONDS))require(s.open(),"binding deadline/failure: "+s.timeline.firstFailure());
        require(s.open()&&s.worker!=null&&s.binding.bindState()==BindingResponsibility.BindState.RETURNED_TRUE&&s.timeline.bindingReady(),"worker unavailable or invocation fenced");
        if(name.equals("revoked_reserve")){
            require(store.cleanup(s.id)&&store.revoked(s.id)&&!s.gate.isRevoked(),"persistent-only revoke setup");require(!s.reserve(),"SAFETY: durably revoked hello accepted");
            require(s.death.await(5,TimeUnit.SECONDS),"rejected worker not retired");s.unbind();require(store.cleanup(s.id),"revoked cleanup failed");
            return new JSONObject().put("lateReserveRejected",true).put("durableFenceTestWithoutMemoryRevoke",true).put("formalReferences",0);
        }
        require(s.reserve(),"reserve rejected");require(s.workerPid!=android.os.Process.myPid(),"same PID");
        require(!s.worker.inspect().getBoolean("ownerInitialized"),"worker created owner");
        fault(run,"before_fd",cut,s);
        Bundle request=new Bundle(s.identity);
        String scenario=name.equals("fault")?"normal":name;
        request.putString("scenario",scenario);
        request.putInt("durationMs",name.equals("fault")&&cut.equals("before_result")?30000:name.equals("ignore_cancel")?3500:name.equals("late_success")?350:600);
        HostFdScope hostFds=new HostFdScope();
        java.io.FileDescriptor sourceDescriptor=null,outputDescriptor=null;
        boolean deniedStart=false;
        try(HostFdScope owned=hostFds) {
        try {
        ParcelFileDescriptor source=owned.own("source",store.open(s.id,"source.bin",ParcelFileDescriptor.MODE_READ_ONLY));
        sourceDescriptor=source.getFileDescriptor();
        File out=StageStore.safe(store.dir(s.id),"output.bin");require(out.createNewFile(),"output exclusive");
        store.recordOutput(s.id);android.system.StructStat outStat=Os.lstat(out.getPath());request.putLong("outputDevice",outStat.st_dev);request.putLong("outputInode",outStat.st_ino);
        ParcelFileDescriptor output=owned.own("output",store.open(s.id,"output.bin",ParcelFileDescriptor.MODE_READ_WRITE));
        outputDescriptor=output.getFileDescriptor();
        if(name.equals("revoked_start")){
            store.revoke(s.id);store.pin(s.id,"REVOKED");require(!s.gate.isRevoked(),"persistent-only start revoke setup");
            require(!s.worker.start(ProbeSupport.envelope(request),source,output),"SAFETY: late grant after revoke");
            deniedStart=true;
        } else {
            require(s.open(),"invocation fenced before start");
            // Deliberately change shared offset; the Worker must still use pread.
            Os.lseek(source.getFileDescriptor(),123,OsConstants.SEEK_SET);
            require(s.worker.start(ProbeSupport.envelope(request),source,output),"start rejected");
        }
        } catch(Throwable failure){s.failMemory(failure.toString());throw failure;}
        } catch(Throwable failure){s.failMemory(failure.toString());throw failure;} finally {
            ProbeSupport.event("HOST_FD_SCOPE","attempt",s.id,"allClosed",hostFds.allClosed(),"sourceEBADF",closedDescriptor(sourceDescriptor),"outputEBADF",closedDescriptor(outputDescriptor));
        }
        require(hostFds.allClosed()&&closedDescriptor(sourceDescriptor)&&closedDescriptor(outputDescriptor),"Host FD close unconfirmed");
        ProbeSupport.event("HOST_DATA_FDS_CLOSED","attempt",s.id,"hostOnly",true);
        if(deniedStart){
            require(!store.canAcquireGuard(s.id)&&store.sourceExists(s.id),"lease lost after denied source grant");
            s.retire();require(s.death.await(5,TimeUnit.SECONDS),"revoked worker not retired");s.unbind();require(s.connectionClosed(),"unbind unresolved");require(store.cleanup(s.id),"revoke cleanup");
            return new JSONObject().put("lateSourceGrantRejected",true).put("durableFenceTestWithoutMemoryRevoke",true).put("formalReferences",0);
        }
        if(name.equals("intent_replay")){
            int before=rejectedCommands.get();CountDownLatch replay=new CountDownLatch(1);
            activity.runOnUiThread(()->{((ProbeActivity)activity).onNewIntent(new Intent(activity,ProbeActivity.class).putExtra("command","run").putExtra("case","intent_replay").putExtra("runId",run));replay.countDown();});
            require(replay.await(2,TimeUnit.SECONDS)&&rejectedCommands.get()==before+1,"SAFETY: duplicate intent not rejected");
        }
        fault(run,"after_fd_before_ack",cut,s);store.pin(s.id,"FD_SENT");
        require(!store.canAcquireGuard(s.id),"worker guard missing while busy");require(store.sourceExists(s.id),"source missing while busy");
        require(!store.cleanup(s.id),"SAFETY: cleanup accepted live consumer");
        require(!s.gate.canPrepare(),"prepared without result/death");
        if(name.equals("fd_duplicate")){
            try(HostFdScope duplicates=new HostFdScope()){
                try {
                    ParcelFileDescriptor dup=duplicates.own("source",store.open(s.id,"source.bin",ParcelFileDescriptor.MODE_READ_ONLY));
                    ParcelFileDescriptor dupOut=duplicates.own("output",store.open(s.id,"output.bin",ParcelFileDescriptor.MODE_READ_WRITE));
                    require(!s.worker.start(ProbeSupport.envelope(request),dup,dupOut),"SAFETY: duplicate consumer accepted");
                }catch(Throwable failure){s.failMemory(failure.toString());throw failure;}
            }catch(Throwable failure){s.failMemory(failure.toString());throw failure;}
        }
        if(name.equals("rebind")){
            CountDownLatch rebound=new CountDownLatch(1);
            ServiceConnection duplicateConnection=new ServiceConnection(){
                public void onServiceConnected(ComponentName n,IBinder b){
                    if(!s.open()){s.failMemory("LATE_DUPLICATE_BIND");rebound.countDown();return;}
                    ProbeSupport.event("DUPLICATE_BIND","sameEndpoint",b.equals(s.worker.asBinder()));rebound.countDown();
                }
                public void onServiceDisconnected(ComponentName n){ProbeSupport.event("DUPLICATE_SERVICE_DISCONNECTED","attempt",s.id);}
            };
            BindingResponsibility duplicate=new BindingResponsibility(BindingResponsibility.fenced(new BindingResponsibility.Operations(){
                public boolean bind(){return context.bindService(new Intent(context,WorkerService.class),duplicateConnection,Context.BIND_AUTO_CREATE);}
                public void unbind(){context.unbindService(duplicateConnection);}
            },(reason,error)->s.failMemory(reason+": "+error)));
            try {
                activity.runOnUiThread(()->{duplicate.executeBind(s::open);if(duplicate.bindState()!=BindingResponsibility.BindState.RETURNED_TRUE)rebound.countDown();});
                require(rebound.await(2,TimeUnit.SECONDS)&&s.open()&&duplicate.bindState()==BindingResponsibility.BindState.RETURNED_TRUE,"rebind unavailable");
                require(!s.worker.reserve(ProbeSupport.envelope(s.identity),s.callback),"SAFETY: duplicate reservation");
            }catch(Throwable failure){s.failMemory(failure.toString());throw failure;}
            finally {duplicate.requestCleanup();if(duplicate.hasCleanupDuty())s.failMemory("DUPLICATE_UNBIND_UNKNOWN");}
            require(s.open(),"duplicate connection cleanup unresolved");
        }
        require(!store.canAcquireGuard(s.id),"SAFETY: repeat messages lost consumer lease");
        if(name.equals("cancel")||name.equals("ignore_cancel")||name.equals("late_success")||name.equals("unbind")||name.equals("normal_finish")){
            long t=SystemClock.elapsedRealtime();s.gate.revoke(name.equals("ignore_cancel")?"TIMEOUT":"CANCELLED");store.pin(s.id,"REVOKED");
            require(!s.gate.canPrepare(),"revocation failed");ProbeSupport.event("HOST_REVOKED","attempt",s.id,"responseMs",SystemClock.elapsedRealtime()-t);
            require(s.worker.cancel(s.identity.getString("sessionNonce"),s.id),"cancel auth failed");
            require(!s.worker.retire("old-nonce",s.id),"SAFETY: wrong retire accepted");
            CountDownLatch heartbeat=new CountDownLatch(1);long t0=SystemClock.elapsedRealtime();activity.runOnUiThread(heartbeat::countDown);require(heartbeat.await(2,TimeUnit.SECONDS),"host UI blocked");ProbeSupport.event("HOST_HEARTBEAT","latencyMs",SystemClock.elapsedRealtime()-t0);
            if(name.equals("ignore_cancel")){Thread.sleep(150);require(s.result.getCount()==1,"ignore-cancel task unexpectedly completed");require(!store.cleanup(s.id),"SAFETY: timeout released pin");}
            if(name.equals("normal_finish"))activity.runOnUiThread(activity::finish);
            if(name.equals("unbind")){s.unbind();}else if(!name.equals("late_success")&&!name.equals("cancel")){s.retire();}
        }
        fault(run,"before_result",cut,s);
        if(!name.equals("ignore_cancel")&&!name.equals("unbind")&&!name.equals("normal_finish")&&!name.equals("worker_kill"))require(s.result.await(5,TimeUnit.SECONDS),"result timeout");
        if(s.result.getCount()==0&&!s.gate.isDead()){
            require(!s.gate.canPrepare()&&!store.canAcquireGuard(s.id)&&store.sourceExists(s.id),"SAFETY: result prematurely released lease/prepared");
            ProbeSupport.event("RESULT_WITH_LEASE_HELD","attempt",s.id,"prepared",false);
        }
        fault(run,"after_result",cut,s);
        if(name.equals("fault")&&(cut.equals("before_receipt")||cut.startsWith("receipt_"))){
            fault(run,"before_receipt",cut,s);writeFaultReceipt(s,run,cut);
        }
        fault(run,"before_retire",cut,s);
        s.retire();require(s.death.await(5,TimeUnit.SECONDS),"worker retirement unconfirmed");s.unbind();
        fault(run,"after_retire",cut,s);
        require(store.canAcquireGuard(s.id),"guard retained after endpoint death");
        boolean expectedSuccess=name.equals("normal")||name.equals("identities")||name.equals("fd_duplicate")||name.equals("rebind")||name.equals("death_race")||name.equals("intent_replay");
        require(s.gate.canPrepare()==expectedSuccess,"prepare differs from expected outcome "+s.gate.status());
        require(s.connectionClosed(),"connection cleanup unconfirmed");
        if(expectedSuccess){
            require(s.gate.acceptedResults()==1,"duplicate result accepted");
            synchronized(store.authorizationLock){
                require(ProbeSupport.flushDiagnostics(2000),"diagnostic evidence incomplete before receipt");
                require(store.isCurrentAuthorized(s.id),"receipt durable authority rejected");
                InvocationFence.Candidate receipt=invocations.beginCandidate(invocation.ticket,InvocationFence.CandidateKind.RECEIPT);
                require(receipt!=null,"receipt candidate fenced");
                StageStore.atomic(store.dir(s.id),"receipt.json",store.receipt(s.id,true));
                require(invocations.finishCandidate(receipt),"receipt landed after failure: quarantine, no cleanup");
            }
        }
        require(s.open(),"invocation fenced before cleanup");
        require(store.cleanup(s.id),"cleanup failed after death");
        if(expectedSuccess){
            require(ProbeSupport.flushDiagnostics(2000),"diagnostic evidence incomplete at terminal");
            require(invocations.markTerminal(invocation.ticket),"terminal eligibility unknown");
        }
        return new JSONObject().put("attemptId",s.id).put("workerPid",s.workerPid).put("workerUid",s.workerUid).put("resultStatus",s.gate.status()).put("acceptedResults",s.gate.acceptedResults()).put("syntheticPrepared",s.gate.canPrepare()).put("endpointDeath",s.gate.isDead()).put("sourceRemoved",!store.sourceExists(s.id)).put("kind","SYNTHETIC_TASK_ONLY");
    }
    void writeFaultReceipt(Session s,String run,String cut)throws Exception {
        File d=store.dir(s.id),temp=StageStore.safe(d,"receipt.json.pending");
        try(FileOutputStream o=new FileOutputStream(temp)){
            o.write(store.receipt(s.id,false).toString().getBytes(StandardCharsets.UTF_8));fault(run,"receipt_pending",cut,s);o.getFD().sync();
        }
        Files.move(temp.toPath(),StageStore.safe(d,"receipt.json").toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        fault(run,"receipt_renamed",cut,s);ProbeSupport.syncDir(d);fault(run,"receipt_synced",cut,s);
    }
    JSONObject stageModel(Session s)throws Exception {
        JSONObject owner=store.owner(s.id);require(!StageStore.legacyV1Recognizes(owner),"v1 claims v2");
        JSONObject good=store.receipt(s.id,false);require(StageStore.validReceipt(good,owner),"valid receipt rejected");
        for(String key:new String[]{"sessionNonce","generationIdentity","attemptId","sourceSHA256"}){JSONObject bad=new JSONObject(good.toString());bad.put(key,"wrong");require(!StageStore.validReceipt(bad,owner),"bad receipt claimed "+key);}
        File bad=StageStore.safe(store.dir(s.id),"receipt.json");try(FileOutputStream o=new FileOutputStream(bad)){o.write("{broken".getBytes(StandardCharsets.UTF_8));o.getFD().sync();}
        boolean recognized;try{recognized=StageStore.validReceipt(StageStore.read(bad),owner);}catch(Exception expected){recognized=false;}require(!recognized,"damaged receipt claimed");
        require(!store.cleanup(s.id)&&store.sourceExists(s.id),"damaged receipt cleanup not refused");Files.move(bad.toPath(),StageStore.safe(store.dir(s.id),"unknown-corrupt-receipt.json").toPath());
        File src=StageStore.safe(store.dir(s.id),"source.bin"),saved=StageStore.safe(store.dir(s.id),"source-owned.saved");Files.move(src.toPath(),saved.toPath());require(src.createNewFile(),"replacement source");require(!store.cleanup(s.id)&&src.exists()&&saved.exists(),"wrong source inode deleted");require(src.delete(),"remove synthetic substitute");Files.move(saved.toPath(),src.toPath());
        File output=StageStore.safe(store.dir(s.id),"output.bin");require(output.createNewFile(),"output fixture");store.recordOutput(s.id);File savedOut=StageStore.safe(store.dir(s.id),"output-owned.saved");Files.move(output.toPath(),savedOut.toPath());require(output.createNewFile(),"replacement output");require(!store.cleanup(s.id)&&output.exists()&&store.sourceExists(s.id),"wrong output inode partial cleanup");require(output.delete(),"remove synthetic output substitute");Files.move(savedOut.toPath(),output.toPath());
        File unknown=StageStore.safe(store.dir(s.id),"unknown-preserve.bin");require(unknown.createNewFile(),"unknown fixture");
        require(store.cleanup(s.id),"static cleanup failed");require(unknown.exists(),"unknown removed");
        require(!StageStore.safe(store.dir(s.id),"asset.json").exists(),"formal record created");
        return new JSONObject().put("model","RESEARCH_NOT_PRODUCTION_GC").put("v1RejectsV2",true).put("badReceiptsRejected",true).put("unknownPreserved",true).put("formalReferences",0);
    }
    static boolean closedDescriptor(java.io.FileDescriptor fd){
        if(fd==null)return false;
        try{Os.fstat(fd);return false;}catch(ErrnoException e){return e.errno==OsConstants.EBADF;}
    }
    final class Session {
        final String id=UUID.randomUUID().toString();final JSONObject owner;final Bundle identity;final SessionGate gate;
        final CountDownLatch connected=new CountDownLatch(1),result=new CountDownLatch(1),death=new CountDownLatch(1);
        final Invocation invocation;
        final BindingResponsibility binding;
        final BindingTimeline timeline;
        final DeadlineWatchdog watchdog;
        volatile IWorker worker;volatile IBinder endpoint;volatile int workerPid=-1,workerUid=-1;
        volatile boolean disconnected;volatile String durableFence="NOT_ATTEMPTED";
        Session(Invocation invocation)throws Exception{
            this.invocation=invocation;owner=store.create(id);identity=ProbeSupport.identity(owner);
            gate=new SessionGate(1,identity.getString("sessionNonce"),id,identity.getString("generationIdentity"),identity.getString("sourceSHA256"),8192,android.os.Process.myUid());
            timeline=new BindingTimeline(SystemClock::elapsedRealtimeNanos,BindingTimeline.Limits.researchDefaults(),this::failMemory);
            watchdog=new DeadlineWatchdog(timeline);
            binding=new BindingResponsibility(BindingResponsibility.fenced(new BindingResponsibility.Operations(){
                public boolean bind(){
                    boolean begun=timeline.callBegin();timingEvent("BIND_CALL_BEGIN",begun);
                    require(begun&&open(),"bind call deadline/fence");
                    boolean bound=false;String error=null;
                    try{bound=context.bindService(new Intent(context,WorkerService.class),connection,Context.BIND_AUTO_CREATE);return bound;}
                    catch(RuntimeException|Error failure){error=failure.getClass().getName();throw failure;}
                    finally{boolean accepted=timeline.bindReturned(bound,error);timingEvent("BIND_RETURNED",accepted);}
                }
                public void unbind(){context.unbindService(connection);}
            },(reason,error)->failMemory(reason+": "+error)));
        }
        boolean open(){return invocations.isCurrentOpen(invocation.ticket)&&timeline.firstFailure()==null;}
        void timingEvent(String boundary,boolean accepted){
            ProbeSupport.event("BIND_PHASE","attempt",id,"epoch",invocation.ticket.epoch(),"boundary",boundary,"accepted",accepted,
                "phase",timeline.phase(),"times",new JSONObject(timeline.snapshot()),"threadId",Thread.currentThread().getId(),"failure",String.valueOf(timeline.firstFailure()));
        }
        boolean accepting(){timeline.checkDeadline();return open()&&!ProbeSupport.diagnosticUnknown();}
        void failMemory(String reason){invocations.fail(invocation.ticket,reason);}
        boolean connectionClosed(){return binding.cleanupState()==BindingResponsibility.CleanupState.UNBOUND||binding.cleanupState()==BindingResponsibility.CleanupState.NOT_REGISTERED;}
        void signalConnectedIfReady(){if(open()&&worker!=null&&binding.bindState()==BindingResponsibility.BindState.RETURNED_TRUE)connected.countDown();}
        final IHost.Stub callback=new IHost.Stub(){
            public boolean hello(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->{
                    return invocations.decide(invocation.ticket,()->{
                        boolean accepted=ProbeSupport.same(identity,b)&&pid!=android.os.Process.myPid()&&gate.handshake(uid,pid,b.getInt("workerPid"),b.getInt("workerUid"),!b.getBoolean("ownerInitialized",true));
                        if(accepted)accepted=timeline.helloAccepted();
                        if(accepted){workerPid=pid;workerUid=uid;}return accepted;
                    });
                });}catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","hello","detail",error.toString());}
                timingEvent("HELLO_ACCEPTED",ok);
                ProbeSupport.event("SYNC_HELLO","callingUid",uid,"callingPid",pid,"claimedPid",b.getInt("workerPid"),"accepted",ok);return ok;}
            public boolean grant(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->invocations.decide(invocation.ticket,()->accepting()&&uid==workerUid&&pid==workerPid&&ProbeSupport.same(identity,b)&&gate.grant()));}
                catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","grant","detail",error.toString());}
                ProbeSupport.event("FD_GRANT_DECISION","attempt",id,"accepted",ok);return ok;}
            public boolean result(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();if(b.getBoolean("delayHostResult")){ProbeSupport.event("RESULT_RACE_CALLBACK_ENTERED","attempt",id);try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
                boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->invocations.decide(invocation.ticket,()->accepting()&&uid==workerUid&&pid==workerPid&&ProbeSupport.same(identity,b)&&"SYNTHETIC_TASK_ONLY".equals(b.getString("kind"))&&gate.result(b.getInt("protocolVersion"),b.getString("sessionNonce"),b.getString("attemptId"),b.getString("generationIdentity"),b.getString("sourceSHA256"),b.getLong("sourceSize"),b.getString("workerResult"),b.getBoolean("fdClosed"))));}
                catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","result","detail",error.toString());}
                ProbeSupport.event("HOST_RESULT","attempt",id,"callingUid",uid,"callingPid",pid,"accepted",ok,"status",b.getString("workerResult"),"fdClosed",b.getBoolean("fdClosed"));
                if(ok)result.countDown();return ok;}
        };
        final ServiceConnection connection=new ServiceConnection(){
            public void onServiceConnected(ComponentName n,IBinder binder){
                IWorker proposed=IWorker.Stub.asInterface(binder); // Binder helper outside short memory decision.
                boolean accepted=invocations.decide(invocation.ticket,()->{
                    if(disconnected||endpoint!=null)return false;
                    if(!timeline.connectedAccepted())return false;
                    endpoint=binder;worker=proposed;return true;
                });
                timingEvent("CONNECTED_ACCEPTED",accepted);
                if(!accepted){if(open())failMemory("DUPLICATE_OR_DISCONNECTED_ENDPOINT");ProbeSupport.event("LATE_CONNECTION_REJECTED","attempt",id,"epoch",invocation.ticket.epoch());unbind();return;}
                try{binder.linkToDeath(()->{
                    if(endpoint!=binder)return;
                    gate.died();disconnected=true;
                    ProbeSupport.event("WORKER_ENDPOINT_DIED","attempt",id,"epoch",invocation.ticket.epoch());death.countDown();
                },0);}catch(RemoteException e){
                    gate.died();disconnected=true;ProbeSupport.event("LINK_DEATH_REMOTE_GONE","attempt",id,"detail",e.toString());death.countDown();
                }catch(Throwable e){failMemory("LINK_DEATH_UNKNOWN: "+e);unbind();connected.countDown();return;}
                if(!open()){unbind();return;}
                signalConnectedIfReady();
            }
            public void onServiceDisconnected(ComponentName n){ProbeSupport.event("SERVICE_DISCONNECTED","attempt",id);}
            public void onBindingDied(ComponentName n){failMemory("BINDING_DIED");ProbeSupport.event("BINDING_DIED","attempt",id);unbind();connected.countDown();}
            public void onNullBinding(ComponentName n){failMemory("NULL_BINDING");ProbeSupport.event("NULL_BINDING","attempt",id);unbind();connected.countDown();}
        };
        void bind(){
            require(timeline.posted(),"post deadline/fence");
            watchdog.start(); // Independent timer never shares UI, Host command queue, or trace sink.
            timingEvent("UI_BIND_POSTED",true);
            boolean posted=new Handler(Looper.getMainLooper()).post(()->{
                binding.executeBind(this::open);
                if(binding.bindState()!=BindingResponsibility.BindState.RETURNED_TRUE){
                    failMemory("BIND_"+binding.bindState()+": "+binding.bindingError());connected.countDown();
                } else {signalConnectedIfReady();if(!open())unbind();}
                ProbeSupport.event("BIND_ATTEMPT_STATE","attempt",id,"bind",binding.bindState(),"cleanup",binding.cleanupState(),"error",String.valueOf(binding.bindingError()));
            });
            if(!posted){failMemory("UI_BIND_POST_FAILED");connected.countDown();}
        }
        boolean reserve()throws Exception{
            require(timeline.reserveIssued()&&open(),"reserve deadline/fence");timingEvent("RESERVE_ISSUED",true);
            boolean accepted=false,observed;
            try{accepted=worker.reserve(ProbeSupport.envelope(identity),callback);}
            catch(Exception|Error failure){timeline.watchdogUnknown("RESERVE_THROW_UNKNOWN");throw failure;}
            finally{observed=timeline.reserveReturned(accepted);timingEvent("RESERVE_RETURNED",observed);}
            return accepted&&observed&&timeline.handshakeReady()&&open();
        }
        void retire(){try{if(worker!=null&&!gate.isDead())worker.retire(identity.getString("sessionNonce"),id);}catch(RemoteException e){ProbeSupport.event("RETIRE_REMOTE_GONE","attempt",id);}}
        void unbind(){
            binding.requestCleanup();
            if(binding.cleanupState()==BindingResponsibility.CleanupState.UNBIND_UNKNOWN)failMemory("UNBIND_UNKNOWN: "+binding.firstCleanupError());
            ProbeSupport.event("HOST_CONNECTION_CLEANUP","attempt",id,"bind",binding.bindState(),"cleanup",binding.cleanupState(),"duty",binding.hasCleanupDuty(),"error",String.valueOf(binding.firstCleanupError()));
        }
    }
}
