package com.cooper.lithic.m10r3.outsider;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class OutsiderActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);JSONObject result=new JSONObject();
        try{result.put("uid",android.os.Process.myUid()).put("pid",android.os.Process.myPid());
            ServiceConnection c=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder x){Log.e("LITHIC_R3","SAFETY_OUTSIDER_BOUND");}public void onServiceDisconnected(ComponentName n){}};
            boolean bound=bindService(new Intent().setComponent(new ComponentName("com.cooper.lithic.m10r3.probe","com.cooper.lithic.m10r3.probe.WorkerService")),c,BIND_AUTO_CREATE);
            result.put("status",bound?"FAIL":"PASS").put("bindingReturned",bound);if(bound)unbindService(c);
        }catch(SecurityException denied){try{result.put("status","PASS").put("bindingDenied",true).put("error",denied.toString());}catch(Exception ignored){}}
        catch(Exception error){try{result.put("status","FAIL").put("error",error.toString());}catch(Exception ignored){}}
        try(FileOutputStream o=new FileOutputStream(new File(getFilesDir(),"result.json"))){o.write(result.toString().getBytes(StandardCharsets.UTF_8));o.getFD().sync();}catch(Exception e){Log.e("LITHIC_R3",e.toString());}
        Log.i("LITHIC_R3",result.toString());finish();
    }
}
