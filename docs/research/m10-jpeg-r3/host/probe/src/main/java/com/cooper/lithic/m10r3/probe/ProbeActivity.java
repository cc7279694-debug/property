package com.cooper.lithic.m10r3.probe;
import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;

public final class ProbeActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);TextView view=new TextView(this);view.setText("LITHIC M10-R3 — ISOLATED RESEARCH ONLY\nSynthetic Binder / FD lifecycle probe\nNo personal records or JPEG decoder");view.setPadding(24,60,24,24);setContentView(view);accept(getIntent());}
    @Override public void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);accept(i);}
    private void accept(Intent i){
        if(i==null)return;
        // Immutable bounded strings only. All role/owner/claim/Runner work is serialized off UI.
        String cmd=i.getStringExtra("command"),run=i.getStringExtra("runId"),name=i.getStringExtra("case"),fault=i.getStringExtra("fault");
        if(cmd==null)return;
        if(cmd.length()>20||run==null||run.length()>80||(name!=null&&name.length()>80)||(fault!=null&&fault.length()>80)){
            ProbeSupport.event("COMMAND_INPUT_REJECTED");return;
        }
        ((ProbeApplication)getApplication()).submit(this,cmd,run,name,fault);
    }
}
