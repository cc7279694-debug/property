package com.cooper.lithic.m10r3.probe;

import android.app.Service;
import android.net.LocalServerSocket;
import android.content.Intent;
import android.os.*;
import android.system.*;
import java.io.*;
import java.nio.channels.*;
import java.util.concurrent.*;

/** Java synthetic work only. No native library, JPEG, production owner or path API. */
public final class WorkerService extends Service {
    private final ScheduledExecutorService control=Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService compute=Executors.newSingleThreadExecutor();
    private Bundle request;
    private IHost client;
    private LocalServerSocket consumerLease; // Strong reference; never closed before process retirement.
    private volatile boolean cancelled, started, exiting;
    private final IWorker.Stub endpoint=new IWorker.Stub(){
        @Override public synchronized boolean reserve(SessionEnvelope envelope,IHost host){
            Bundle r=ProbeSupport.bundle(envelope);
            try {
                if(Binder.getCallingUid()!=android.os.Process.myUid()||!"WORKER".equals(ProbeSupport.role(ProbeSupport.processName))||ProbeSupport.mainOwner||request!=null) {
                    ProbeSupport.event("RESERVE_REJECTED");return false;}
                if(r.getInt("protocolVersion")!=1||r.getString("sessionNonce","").length()!=36||r.getString("attemptId","").length()!=36||r.getString("sourceSHA256","").length()!=64||r.getLong("sourceSize")!=8192||!r.getString("consumerLeaseName","").equals("lithic-r3-"+r.getString("attemptId")))return false;
                client=host;
                host.asBinder().linkToDeath(()->{ProbeSupport.event("CLIENT_BINDER_DIED");selfExit("CLIENT_DEATH",80);},0);
                if(!host.asBinder().isBinderAlive())return false;
                consumerLease=new LocalServerSocket(r.getString("consumerLeaseName"));
                request=new Bundle(r);
                if(!host.asBinder().isBinderAlive()){selfExit("DEAD_BEFORE_HELLO",0);return false;}
                Bundle hello=new Bundle(r);hello.putInt("workerPid",android.os.Process.myPid());hello.putInt("workerUid",android.os.Process.myUid());hello.putBoolean("ownerInitialized",ProbeSupport.mainOwner);
                boolean accepted;long token=Binder.clearCallingIdentity();
                try {accepted=host.hello(ProbeSupport.envelope(hello));}finally{Binder.restoreCallingIdentity(token);}
                ProbeSupport.event("LEASE_HELD_HELLO","attempt",r.getString("attemptId"),"accepted",accepted,"ownerInitialized",ProbeSupport.mainOwner);
                if(!accepted){selfExit("HELLO_REJECTED",10);return false;}
                control.schedule(()->selfExit("WORKER_DEADLINE",0),20,TimeUnit.SECONDS);
                return true;
            }catch(Exception e){ProbeSupport.event("RESERVE_ERROR","detail",e.toString());selfExit("RESERVE_ERROR",10);return false;}
        }
        @Override public synchronized boolean start(SessionEnvelope envelope,ParcelFileDescriptor source,ParcelFileDescriptor output){
            Bundle r=ProbeSupport.bundle(envelope);
            try {
                if(Binder.getCallingUid()!=android.os.Process.myUid()||started||!ProbeSupport.same(request,r)||client==null||!client.asBinder().isBinderAlive()||consumerLease==null){
                    source.close();output.close();ProbeSupport.event("START_REJECTED");return false;}
                boolean grant;long identity=Binder.clearCallingIdentity();try{grant=client.grant(ProbeSupport.envelope(r));}finally{Binder.restoreCallingIdentity(identity);}
                if(!grant){source.close();output.close();ProbeSupport.event("START_REVOKED_REJECTED");return false;}
                StructStat s=Os.fstat(source.getFileDescriptor());
                if(s.st_dev!=r.getLong("sourceDevice")||s.st_ino!=r.getLong("sourceInode")||s.st_size!=8192||!OsConstants.S_ISREG(s.st_mode)||!ProbeSupport.readOnlyDescriptor(source)){source.close();output.close();return false;}
                StructStat o=Os.fstat(output.getFileDescriptor());
                if(!OsConstants.S_ISREG(o.st_mode)||o.st_dev!=r.getLong("outputDevice")||o.st_ino!=r.getLong("outputInode")){source.close();output.close();return false;}
                // Lease was acquired and confirmed before this FD was granted. Hold it until OS death.
                started=true;ProbeSupport.event("SOURCE_FD_OPEN","attempt",r.getString("attemptId"),"sourceFd",source.getFd(),"outputFd",output.getFd(),"inode",s.st_ino,"readOnly",true);
                compute.execute(()->work(new Bundle(r),source,output));return true;
            }catch(Exception e){try{source.close();output.close();}catch(Exception ignored){}ProbeSupport.event("START_ERROR","detail",e.toString());return false;}
        }
        @Override public boolean cancel(String nonce,String attempt){if(!authorized(nonce,attempt))return false;cancelled=true;ProbeSupport.event("SOFT_CANCEL_RECEIVED");return true;}
        @Override public boolean retire(String nonce,String attempt){if(!authorized(nonce,attempt))return false;selfExit("AUTHENTICATED_RETIRE",100);return true;}
        @Override public Bundle inspect(){Bundle b=new Bundle();b.putInt("pid",android.os.Process.myPid());b.putInt("uid",android.os.Process.myUid());b.putBoolean("started",started);b.putBoolean("ownerInitialized",ProbeSupport.mainOwner);b.putString("attempt",request==null?"":request.getString("attemptId"));return b;}
    };
    private boolean authorized(String nonce,String attempt){return Binder.getCallingUid()==android.os.Process.myUid()&&request!=null&&nonce.equals(request.getString("sessionNonce"))&&attempt.equals(request.getString("attemptId"));}
    private synchronized void selfExit(String reason,long delay){
        if(exiting)return;exiting=true;ProbeSupport.event("RETIRE_REQUESTED","reason",reason);
        control.schedule(()->{ProbeSupport.event("SELF_SIGKILL","reason",reason,"targetIsSelf",true);android.os.Process.killProcess(android.os.Process.myPid());},delay,TimeUnit.MILLISECONDS);
    }
    private void work(Bundle r,ParcelFileDescriptor source,ParcelFileDescriptor output){
        String status="SUCCESS";boolean closed=false;
        try {
            long until=SystemClock.elapsedRealtime()+r.getInt("durationMs",500);
            String scenario=r.getString("scenario", "normal");
            while(SystemClock.elapsedRealtime()<until){
                if(cancelled&&!scenario.equals("ignore_cancel")&&!scenario.equals("late_success")){status="CANCELLED";break;}
                Thread.sleep(10);
            }
            if(scenario.equals("worker_kill")){selfExit("SIMULATED_ABNORMAL_SIGKILL",0);Thread.sleep(2000);return;}
            byte[] bytes=new byte[8192];int done=0;
            // pread deliberately avoids the shared-offset trap when Host moved its dup offset.
            while(done<bytes.length){int n=Os.pread(source.getFileDescriptor(),bytes,done,bytes.length-done,done);if(n<=0)throw new IOException("short source");done+=n;}
            boolean writeRejected=false;try{Os.write(source.getFileDescriptor(),new byte[]{1},0,1);}catch(ErrnoException expected){writeRejected=expected.errno==OsConstants.EBADF;}
            if(!writeRejected||!ProbeSupport.sha(bytes).equals(r.getString("sourceSHA256")))throw new IOException("source identity/readonly failed");
            ProbeSupport.event("SOURCE_VERIFIED","attempt",r.getString("attemptId"),"sourceFd",source.getFd(),"sha",ProbeSupport.sha(bytes),"readOnlyWriteRejected",writeRejected,"preadBytes",done);
            Os.write(output.getFileDescriptor(),new byte[]{82,51},0,2);Os.fsync(output.getFileDescriptor());
            java.io.FileDescriptor sfd=source.getFileDescriptor(),ofd=output.getFileDescriptor();source.close();output.close();
            boolean sourceClosed=false,outputClosed=false;
            try{Os.fstat(sfd);}catch(ErrnoException expected){sourceClosed=expected.errno==OsConstants.EBADF;}
            try{Os.fstat(ofd);}catch(ErrnoException expected){outputClosed=expected.errno==OsConstants.EBADF;}
            if(!sourceClosed||!outputClosed)throw new IOException("FD close not proven");
            closed=true;ProbeSupport.event("DATA_FDS_CLOSED","attempt",r.getString("attemptId"),"sourceEBADF",sourceClosed,"outputEBADF",outputClosed,"leaseStillHeld",consumerLease!=null);
            Bundle result=new Bundle(r);result.putString("workerResult",status);result.putString("resourceCloseStatus","DATA_FDS_CLOSED_LEASE_HELD");result.putBoolean("fdClosed",true);result.putString("kind","SYNTHETIC_TASK_ONLY");
            if(scenario.equals("identities")){
                for(String key:new String[]{"sessionNonce","attemptId","generationIdentity","sourceSHA256"}){Bundle bad=new Bundle(result);bad.putString(key,"wrong");boolean a=client.result(ProbeSupport.envelope(bad));ProbeSupport.event("WRONG_RESULT_RESPONSE","field",key,"accepted",a);}
                Bundle bad=new Bundle(result);bad.putInt("protocolVersion",99);ProbeSupport.event("WRONG_RESULT_RESPONSE","field","protocolVersion","accepted",client.result(ProbeSupport.envelope(bad)));
            }
            if(scenario.equals("death_race")){result.putBoolean("delayHostResult",true);selfExit("SIMULATED_RESULT_DEATH_RACE",100);}
            boolean accepted=client.result(ProbeSupport.envelope(result));ProbeSupport.event("RESULT_RETURNED","attempt",r.getString("attemptId"),"status",status,"accepted",accepted);
            if(scenario.equals("identities"))ProbeSupport.event("DUPLICATE_RESULT_RESPONSE","accepted",client.result(ProbeSupport.envelope(result)));
            // Host requests retirement after observing result; hard deadline remains independent.
        }catch(Exception e){ProbeSupport.event("WORK_ERROR","detail",e.toString());selfExit("WORK_ERROR",50);}
        finally{if(!closed){try{source.close();}catch(Exception ignored){}try{output.close();}catch(Exception ignored){}}}
    }
    @Override public void onCreate(){super.onCreate();ProbeSupport.event("SERVICE_CREATED");control.schedule(()->selfExit("UNRESERVED_DEADLINE",0),30,TimeUnit.SECONDS);}
    @Override public IBinder onBind(Intent intent){if(!"WORKER".equals(ProbeSupport.role(ProbeSupport.processName))||ProbeSupport.mainOwner)throw new SecurityException("worker role rejected");ProbeSupport.event("SERVICE_BOUND");return endpoint;}
    @Override public boolean onUnbind(Intent intent){ProbeSupport.event("SERVICE_UNBOUND");selfExit("UNBOUND",50);return false;}
    @Override public int onStartCommand(Intent intent,int flags,int id){stopSelf();return START_NOT_STICKY;}
}
