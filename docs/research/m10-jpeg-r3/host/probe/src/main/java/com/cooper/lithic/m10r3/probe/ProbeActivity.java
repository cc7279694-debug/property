package com.cooper.lithic.m10r3.probe;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

public final class ProbeActivity extends Activity {
    private static ProbeRunner runner;
    @Override public void onCreate(Bundle b){super.onCreate(b);TextView view=new TextView(this);view.setText("LITHIC M10-R3 — ISOLATED RESEARCH ONLY\nSynthetic Binder / FD lifecycle probe\nNo personal records or JPEG decoder");view.setPadding(24,60,24,24);setContentView(view);accept(getIntent());}
    @Override public void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);accept(i);}
    private void accept(Intent i){
        if(!ProbeSupport.mainOwner){ProbeSupport.event("HOST_ROLE_REJECTED");return;}
        String cmd=i.getStringExtra("command");if(cmd==null)return;
        if(cmd.equals("kill_host")){
            try{if(!ProbeRunner.claim(this,cmd,i.getStringExtra("runId"))){ProbeSupport.event("COMMAND_REPLAY_REJECTED","command",cmd);return;}}
            catch(Exception e){ProbeSupport.event("COMMAND_CLAIM_FAILED");return;}
            ProbeSupport.event("HOST_SELF_SIGKILL","targetIsSelf",true);android.os.Process.killProcess(android.os.Process.myPid());return;}
        try{if(runner==null)runner=new ProbeRunner(this);runner.dispatch(this,i);}catch(Exception e){ProbeSupport.event("DISPATCH_ERROR","detail",e.toString());}
    }
}
