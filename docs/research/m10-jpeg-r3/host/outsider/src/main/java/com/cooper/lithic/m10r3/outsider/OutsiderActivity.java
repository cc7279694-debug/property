package com.cooper.lithic.m10r3.outsider;
import android.app.Activity;
import android.content.*;
import android.os.*;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class OutsiderActivity extends Activity {
    private static final String TAG = "LITHIC_R3";
    private BindingAttempt bindingAttempt;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.e(TAG, "SAFETY_OUTSIDER_BOUND");
        }
        @Override public void onServiceDisconnected(ComponentName name) { }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        bindingAttempt = new BindingAttempt(new BindingAttempt.Binding() {
            @Override public boolean bind() {
                return bindService(new Intent().setComponent(new ComponentName(
                        "com.cooper.lithic.m10r3.probe", "com.cooper.lithic.m10r3.probe.WorkerService")),
                        connection, BIND_AUTO_CREATE);
            }
            @Override public void unbind() { unbindService(connection); }
        });
        try {
            bindingAttempt.run();
        } finally {
            publishResult("onCreate");
            finish();
        }
    }

    @Override public void onDestroy() {
        try {
            if (bindingAttempt != null) {
                // Retry only a prior cleanup failure; released/unregistered attempts are idempotent.
                bindingAttempt.close();
                publishResult("onDestroy");
            }
        } finally {
            super.onDestroy();
        }
    }

    private void publishResult(String phase) {
        try {
            JSONObject result = new JSONObject()
                    .put("uid", android.os.Process.myUid()).put("pid", android.os.Process.myPid())
                    .put("phase", phase).put("status", bindingAttempt.probePassed() ? "PASS" : "FAIL")
                    .put("bindingOutcome", bindingAttempt.outcome().name())
                    .put("cleanupStatus", bindingAttempt.cleanup().name());
            if (bindingAttempt.outcome() == BindingAttempt.Outcome.DENIED) result.put("bindingDenied", true);
            if (bindingAttempt.outcome() == BindingAttempt.Outcome.RETURNED_TRUE
                    || bindingAttempt.outcome() == BindingAttempt.Outcome.RETURNED_FALSE)
                result.put("bindingReturned", bindingAttempt.outcome() == BindingAttempt.Outcome.RETURNED_TRUE);
            if (bindingAttempt.bindingError() != null) {
                result.put("error", bindingAttempt.bindingError().toString());
                Log.w(TAG, "Binding outcome " + bindingAttempt.outcome(), bindingAttempt.bindingError());
            }
            if (bindingAttempt.cleanupError() != null)
                result.put("cleanupError", bindingAttempt.cleanupError().toString());
            if (bindingAttempt.firstUnexpectedCleanupError() != null) {
                result.put("firstUnexpectedCleanupError", bindingAttempt.firstUnexpectedCleanupError().toString());
                Log.e(TAG, "Connection cleanup failed", bindingAttempt.firstUnexpectedCleanupError());
            }
            try (FileOutputStream output = new FileOutputStream(new File(getFilesDir(), "result.json"))) {
                output.write(result.toString().getBytes(StandardCharsets.UTF_8)); output.getFD().sync();
            }
            Log.i(TAG, result.toString());
        } catch (Exception error) {
            Log.e(TAG, "Cannot persist Outsider result for " + phase, error);
        }
    }
}
