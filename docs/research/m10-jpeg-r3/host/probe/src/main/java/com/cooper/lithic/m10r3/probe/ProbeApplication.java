package com.cooper.lithic.m10r3.probe;

import android.app.Application;
import android.app.Activity;
import android.os.Build;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;

public final class ProbeApplication extends Application {
    private HostCommandQueue commands;
    private volatile ProbeRunner runner;
    @Override public void onCreate() {
        super.onCreate();
        ProbeSupport.context=this;
        // API28+ exposes a cached framework process identity without app file reads.
        if(Build.VERSION.SDK_INT>=28){
            ProbeSupport.processName=Application.getProcessName();
            ProbeSupport.identity.frameworkHint(ProbeSupport.processName);
        }
        ProbeSupport.initializeDiagnostics();
        commands=new HostCommandQueue(16,this::initializeIdentity,(command,run,outcome,error)->{
            if(outcome==HostCommandQueue.Outcome.CLAIM_REPLAY&&runner!=null)runner.rejectedCommands.incrementAndGet();
            ProbeSupport.event("COMMAND_OUTCOME","command",command,"runId",run,"outcome",outcome,"error",String.valueOf(error));
        });
    }
    private void initializeIdentity()throws Exception {
        try {
            String actual=ProbeSupport.processName26();
            String role=ProbeSupport.identity.beginBackground(actual);
            if("REJECT".equals(role))throw new IOException("unknown research role");
            ProbeSupport.processName=actual;
            if("HOST".equals(role)) {
                File owner=new File(getFilesDir(),"synthetic-main-owner");
                if(Files.isSymbolicLink(owner.toPath()))throw new IOException("owner link");
                if(!owner.exists()){
                    if(!owner.createNewFile())throw new IOException("owner exclusive create");
                    try(FileOutputStream out=new FileOutputStream(owner)){out.getFD().sync();}
                    ProbeSupport.syncDir(getFilesDir());
                }
                if(!OsConstants.S_ISREG(Os.lstat(owner.getPath()).st_mode)||owner.length()!=0)throw new IOException("owner marker shape");
                ProbeSupport.mainOwner=true;
                runner=new ProbeRunner(this);
            }
            ProbeSupport.event("APPLICATION_ROLE","role",role,"syntheticOwner",ProbeSupport.mainOwner,"api26Path","background-bounded-proc-self-cmdline");
            ProbeSupport.identity.completeBackground();
            if(ProbeSupport.identity.state()==ProcessIdentityGate.State.FAILED)throw new IOException("identity verification failed");
        }catch(Exception error){ProbeSupport.mainOwner=false;ProbeSupport.identity.fail(error.toString());throw error;}
    }
    boolean queueObserverUnknown(){return commands==null||commands.readiness()==HostCommandQueue.Readiness.UNKNOWN||commands.observerFailures()!=0;}
    org.json.JSONObject queueSummary()throws Exception{
        return new org.json.JSONObject().put("readiness",commands==null?"NOT_CREATED":commands.readiness().name())
            .put("observerFailures",commands==null?-1:commands.observerFailures()).put("observerError",commands==null?"NOT_CREATED":String.valueOf(commands.observerError()));
    }
    void submit(Activity activity,String command,String run,String name,String fault){
        WeakReference<Activity> reference=new WeakReference<>(activity);
        HostCommandQueue.Submission submitted=commands.submit(command,run,()->{
            if(!ProbeSupport.mainOwner||!"HOST".equals(ProbeSupport.identity.verifiedRole()))throw new IOException("unconfirmed Host/owner");
            return ProbeRunner.claim(this,command,run);
        },()->{
            Activity current=reference.get();
            if(current==null)throw new IOException("activity unavailable");
            if("kill_host".equals(command)){
                ProbeSupport.event("HOST_SELF_SIGKILL","targetIsSelf",true);
                ProbeSupport.flushDiagnostics(2000);
                android.os.Process.killProcess(android.os.Process.myPid());return;
            }
            if(runner==null)throw new IOException("runner not initialized");
            runner.dispatch(current,command,run,name,fault);
        });
        if(submitted==HostCommandQueue.Submission.DUPLICATE&&runner!=null)runner.rejectedCommands.incrementAndGet();
        if(submitted!=HostCommandQueue.Submission.ACCEPTED)ProbeSupport.event("COMMAND_ADMISSION","runId",run,"outcome",submitted);
    }
}
