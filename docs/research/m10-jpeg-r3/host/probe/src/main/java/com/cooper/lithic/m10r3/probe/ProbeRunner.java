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
    final ExecutorService executor=Executors.newSingleThreadExecutor();
    volatile int rejectedCommands;
    Session activeSession; // Access only while holding store.authorizationLock.
    ProbeRunner(Context c)throws Exception {context=c.getApplicationContext();store=new StageStore(context);reports=new File(context.getFilesDir(),"r3-reports");if(!reports.isDirectory()&&!reports.mkdir())throw new IOException("reports");}
    static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static boolean claim(Context context,String command,String run)throws Exception {
        if(run==null||!run.matches("[a-zA-Z0-9_-]{1,80}")||!command.matches("[a-z_]{1,20}"))return false;
        File commands=new File(context.getFilesDir(),"r3-command-claims");if(!commands.isDirectory()&&!commands.mkdir())throw new IOException("command claim directory");
        File file=StageStore.safe(commands,command+"-"+run+".claim");if(!file.createNewFile())return false;
        try(FileOutputStream o=new FileOutputStream(file)){o.write("CLAIMED_RESEARCH_COMMAND".getBytes(StandardCharsets.UTF_8));o.getFD().sync();}ProbeSupport.syncDir(commands);return true;
    }
    void dispatch(Activity activity,Intent intent){
        final String command=intent.getStringExtra("command"),run=intent.getStringExtra("runId"),name=intent.getStringExtra("case"),fault=intent.getStringExtra("fault");
        if(run==null||!run.matches("[a-zA-Z0-9_-]{1,80}"))return;
        try{if(!claim(context,command,run)){rejectedCommands++;ProbeSupport.event("COMMAND_REPLAY_REJECTED","runId",run,"command",command);return;}}
        catch(Exception failure){ProbeSupport.event("COMMAND_CLAIM_FAILED","detail",failure.toString());return;}
        executor.execute(()->{
            long start=SystemClock.elapsedRealtime();
            try {
                ProbeSupport.event("CASE_BEGIN","runId",run,"command",command,"case",String.valueOf(name),"fault",String.valueOf(fault));
                JSONObject result;
                if(command.equals("recover"))result=recover();
                else if(command.equals("fault"))result=runCase(activity,run,"fault",fault);
                else if(command.equals("run"))result=runCase(activity,run,name,null);
                else throw new IllegalArgumentException("unknown command");
                result.put("status","PASS").put("runId",run).put("case",String.valueOf(name)).put("hostPid",android.os.Process.myPid()).put("hostUid",android.os.Process.myUid()).put("wallMs",SystemClock.elapsedRealtime()-start).put("formalReferences",0);
                StageStore.atomic(reports,run+".json",result);ProbeSupport.event("CASE_PASS","runId",run);
            }catch(Throwable failure){
                ProbeSupport.event("CASE_FAIL_STOP","runId",run,"detail",failure.toString());
                try{StageStore.atomic(reports,run+".json",new JSONObject().put("status","FAIL").put("runId",run).put("detail",failure.toString()));}catch(Exception e){ProbeSupport.event("REPORT_ERROR","detail",e.toString());}
            }
        });
    }
    void fault(String run,String point,String requested,Session s)throws Exception {
        if(!point.equals(requested))return;
        JSONObject ready=new JSONObject().put("status","FAULT_READY").put("runId",run).put("fault",point).put("attemptId",s.id).put("hostPid",android.os.Process.myPid()).put("workerPid",s.workerPid).put("sourceExists",store.sourceExists(s.id));
        StageStore.atomic(reports,run+".ready.json",ready);ProbeSupport.event("FAULT_READY","runId",run,"point",point,"attempt",s.id,"workerPid",s.workerPid);
        new CountDownLatch(1).await(); // Only a controlled external command kills this Host itself.
    }
    JSONObject recover()throws Exception {
        String id=store.currentAttempt();
        if(id==null){store.closeAllPriorAttempts();return new JSONObject().put("recovery","NO_CURRENT_ALL_STAGES_CHECKED");}
        store.owner(id);
        boolean initialHeld=!store.canAcquireGuard(id),initialSource=store.sourceExists(id);long begin=SystemClock.elapsedRealtime();
        // No new worker is bound here. The same-inode guard, not an old/new PID, is the evidence.
        while(!store.canAcquireGuard(id)&&SystemClock.elapsedRealtime()-begin<10000){
            require(store.sourceExists(id),"live source disappeared during recovery");ProbeSupport.event("RECOVERY_UNKNOWN_PRESERVE","attempt",id);Thread.sleep(40);
        }
        require(store.canAcquireGuard(id),"BLOCKED: old consumer cannot converge internally");
        synchronized(store.authorizationLock){
            store.closeAllPriorAttempts();
            if(activeSession!=null)activeSession.gate.revoke("RECOVERED");
        }
        require(!store.sourceExists(id),"cleanup failed");
        return new JSONObject().put("attemptId",id).put("initialGuardHeld",initialHeld).put("initialSourceExists",initialSource).put("recoveryWaitMs",SystemClock.elapsedRealtime()-begin).put("recoveryProof","OWNED_KERNEL_LEASE_AND_OLD_CLIENT_DEATH_GRANT_FENCE").put("replayed",false).put("newWorkerStarted",false).put("cleanup",true).put("businessCommit",false);
    }
    JSONObject runCase(Activity activity,String run,String name,String cut)throws Exception {
        require(ProbeSupport.mainOwner,"synthetic main owner missing");
        require("REJECT".equals(ProbeSupport.role("unknown")),"unknown role accepted");
        final Session s;
        synchronized(store.authorizationLock){
            // No Binder call/wait in this section. Failure preserves current.json;
            // a newly created but unpublished stage remains discoverable next time.
            store.closeAllPriorAttempts();
            if(activeSession!=null)activeSession.gate.revoke("REPLACED");
            s=new Session();store.publishCurrent(s.id);activeSession=s;
        }
        fault(run,"before_pin",cut,s);store.pin(s.id,"DELIVERY_UNKNOWN");fault(run,"after_pin",cut,s);
        if(name.equals("stage_model"))return stageModel(s);
        s.bind();require(s.connected.await(5,TimeUnit.SECONDS),"bind timeout");require(s.worker!=null,"worker unavailable");
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
        ParcelFileDescriptor source=store.open(s.id,"source.bin",ParcelFileDescriptor.MODE_READ_ONLY);
        File out=StageStore.safe(store.dir(s.id),"output.bin");require(out.createNewFile(),"output exclusive");
        store.recordOutput(s.id);android.system.StructStat outStat=Os.lstat(out.getPath());request.putLong("outputDevice",outStat.st_dev);request.putLong("outputInode",outStat.st_ino);
        ParcelFileDescriptor output=store.open(s.id,"output.bin",ParcelFileDescriptor.MODE_READ_WRITE);
        if(name.equals("revoked_start")){
            store.revoke(s.id);store.pin(s.id,"REVOKED");require(!s.gate.isRevoked(),"persistent-only start revoke setup");
            try{require(!s.worker.start(ProbeSupport.envelope(request),source,output),"SAFETY: late grant after revoke");}finally{source.close();output.close();}
            require(!store.canAcquireGuard(s.id)&&store.sourceExists(s.id),"lease lost after denied source grant");
            s.retire();require(s.death.await(5,TimeUnit.SECONDS),"revoked worker not retired");s.unbind();require(store.cleanup(s.id),"revoke cleanup");
            return new JSONObject().put("lateSourceGrantRejected",true).put("durableFenceTestWithoutMemoryRevoke",true).put("formalReferences",0);
        }
        // Deliberately change shared offset before transfer; worker must use explicit pread.
        Os.lseek(source.getFileDescriptor(),123,OsConstants.SEEK_SET);
        require(s.worker.start(ProbeSupport.envelope(request),source,output),"start rejected");
        source.close();output.close();ProbeSupport.event("HOST_DATA_FDS_CLOSED","attempt",s.id);
        if(name.equals("intent_replay")){
            int before=rejectedCommands;CountDownLatch replay=new CountDownLatch(1);
            activity.runOnUiThread(()->{((ProbeActivity)activity).onNewIntent(new Intent(activity,ProbeActivity.class).putExtra("command","run").putExtra("case","intent_replay").putExtra("runId",run));replay.countDown();});
            require(replay.await(2,TimeUnit.SECONDS)&&rejectedCommands==before+1,"SAFETY: duplicate intent not rejected");
        }
        fault(run,"after_fd_before_ack",cut,s);store.pin(s.id,"FD_SENT");
        require(!store.canAcquireGuard(s.id),"worker guard missing while busy");require(store.sourceExists(s.id),"source missing while busy");
        require(!store.cleanup(s.id),"SAFETY: cleanup accepted live consumer");
        require(!s.gate.canPrepare(),"prepared without result/death");
        if(name.equals("fd_duplicate")){
            ParcelFileDescriptor dup=store.open(s.id,"source.bin",ParcelFileDescriptor.MODE_READ_ONLY),dupOut=store.open(s.id,"output.bin",ParcelFileDescriptor.MODE_READ_WRITE);
            try{require(!s.worker.start(ProbeSupport.envelope(request),dup,dupOut),"SAFETY: duplicate consumer accepted");}finally{dup.close();dupOut.close();}
        }
        if(name.equals("rebind")){
            Session second=s;CountDownLatch rebound=new CountDownLatch(1);
            ServiceConnection connection=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){ProbeSupport.event("DUPLICATE_BIND","sameEndpoint",b.equals(second.worker.asBinder()));rebound.countDown();}public void onServiceDisconnected(ComponentName n){}};
            activity.runOnUiThread(()->context.bindService(new Intent(context,WorkerService.class),connection,Context.BIND_AUTO_CREATE));
            require(rebound.await(2,TimeUnit.SECONDS),"rebind unavailable");
            require(!s.worker.reserve(ProbeSupport.envelope(s.identity),s.callback),"SAFETY: duplicate reservation");
            context.unbindService(connection);
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
        if(expectedSuccess){require(s.gate.acceptedResults()==1,"duplicate result accepted");StageStore.atomic(store.dir(s.id),"receipt.json",store.receipt(s.id,true));}
        require(store.cleanup(s.id),"cleanup failed after death");
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
    final class Session {
        final String id=UUID.randomUUID().toString();final JSONObject owner;final Bundle identity;final SessionGate gate;
        final CountDownLatch connected=new CountDownLatch(1),result=new CountDownLatch(1),death=new CountDownLatch(1);
        volatile IWorker worker;volatile int workerPid=-1,workerUid=-1;volatile boolean bound,disconnected;
        Session()throws Exception{owner=store.create(id);identity=ProbeSupport.identity(owner);gate=new SessionGate(1,identity.getString("sessionNonce"),id,identity.getString("generationIdentity"),identity.getString("sourceSHA256"),8192,android.os.Process.myUid());}
        final IHost.Stub callback=new IHost.Stub(){
            public boolean hello(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->{
                    boolean accepted=ProbeSupport.same(identity,b)&&pid!=android.os.Process.myPid()&&gate.handshake(uid,pid,b.getInt("workerPid"),b.getInt("workerUid"),!b.getBoolean("ownerInitialized",true));
                    if(accepted){workerPid=pid;workerUid=uid;}return accepted;
                });}catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","hello","detail",error.toString());}
                ProbeSupport.event("SYNC_HELLO","callingUid",uid,"callingPid",pid,"claimedPid",b.getInt("workerPid"),"accepted",ok);return ok;}
            public boolean grant(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->uid==workerUid&&pid==workerPid&&ProbeSupport.same(identity,b)&&!gate.isRevoked()&&!gate.isDead());}
                catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","grant","detail",error.toString());}
                ProbeSupport.event("FD_GRANT_DECISION","attempt",id,"accepted",ok);return ok;}
            public boolean result(SessionEnvelope envelope){Bundle b=ProbeSupport.bundle(envelope);int uid=Binder.getCallingUid(),pid=Binder.getCallingPid();if(b.getBoolean("delayHostResult")){ProbeSupport.event("RESULT_RACE_CALLBACK_ENTERED","attempt",id);try{Thread.sleep(350);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
                boolean ok=false;
                try{ok=AuthorizationFence.decide(store.authorizationLock,()->store.isCurrentAuthorized(id),()->uid==workerUid&&pid==workerPid&&ProbeSupport.same(identity,b)&&"SYNTHETIC_TASK_ONLY".equals(b.getString("kind"))&&gate.result(b.getInt("protocolVersion"),b.getString("sessionNonce"),b.getString("attemptId"),b.getString("generationIdentity"),b.getString("sourceSHA256"),b.getLong("sourceSize"),b.getString("workerResult"),b.getBoolean("fdClosed")));}
                catch(Exception error){ProbeSupport.event("CALLBACK_AUTHORITY_REJECTED","attempt",id,"callback","result","detail",error.toString());}
                ProbeSupport.event("HOST_RESULT","attempt",id,"callingUid",uid,"callingPid",pid,"accepted",ok,"status",b.getString("workerResult"),"fdClosed",b.getBoolean("fdClosed"));
                if(ProbeSupport.same(identity,b))result.countDown();return ok;}
        };
        final ServiceConnection connection=new ServiceConnection(){
            public void onServiceConnected(ComponentName n,IBinder binder){if(disconnected){ProbeSupport.event("AUTO_RECONNECT_REJECTED");return;}worker=IWorker.Stub.asInterface(binder);try{binder.linkToDeath(()->{gate.died();disconnected=true;ProbeSupport.event("WORKER_ENDPOINT_DIED","attempt",id);death.countDown();new Handler(Looper.getMainLooper()).post(()->unbind());},0);}catch(RemoteException e){gate.died();death.countDown();}connected.countDown();}
            public void onServiceDisconnected(ComponentName n){ProbeSupport.event("SERVICE_DISCONNECTED","attempt",id);}
            public void onBindingDied(ComponentName n){ProbeSupport.event("BINDING_DIED","attempt",id);}
        };
        void bind(){new Handler(Looper.getMainLooper()).post(()->{bound=context.bindService(new Intent(context,WorkerService.class),connection,Context.BIND_AUTO_CREATE);if(!bound)connected.countDown();});}
        boolean reserve()throws Exception{return worker.reserve(ProbeSupport.envelope(identity),callback);}
        void retire(){try{if(worker!=null&&!gate.isDead())worker.retire(identity.getString("sessionNonce"),id);}catch(RemoteException e){ProbeSupport.event("RETIRE_REMOTE_GONE","attempt",id);}}
        synchronized void unbind(){if(bound){bound=false;context.unbindService(connection);}}
    }
}
