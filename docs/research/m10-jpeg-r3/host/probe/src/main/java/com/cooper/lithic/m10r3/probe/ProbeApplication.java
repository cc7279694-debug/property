package com.cooper.lithic.m10r3.probe;

import android.app.Application;
import java.io.File;

public final class ProbeApplication extends Application {
    @Override public void onCreate() {
        super.onCreate(); ProbeSupport.context=this;
        try { ProbeSupport.processName=ProbeSupport.processName26(); } catch(Exception e) { ProbeSupport.event("IDENTITY_ERROR","detail",e.toString()); }
        String role=ProbeSupport.role(ProbeSupport.processName);
        ProbeSupport.mainOwner="HOST".equals(role);
        // Synthetic marker only: no Room, generation or real AppContainer in this APK.
        if(ProbeSupport.mainOwner) {
            File owner=new File(getFilesDir(),"synthetic-main-owner");
            try { if(!owner.exists()&&!owner.createNewFile())throw new java.io.IOException("owner marker"); }
            catch(Exception e) { ProbeSupport.mainOwner=false; }
        }
        ProbeSupport.event("APPLICATION_ROLE","role",role,"syntheticOwner",ProbeSupport.mainOwner,"api26Path","bounded-proc-self-cmdline");
    }
}
