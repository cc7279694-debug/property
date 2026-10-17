package com.cooper.lithic.m10r3.probe;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

final class ProbeSupport {
    static final String PKG = "com.cooper.lithic.m10r3.probe";
    static Context context;
    static String processName = "UNKNOWN";
    static boolean mainOwner;
    static synchronized void event(String name, Object... pairs) {
        try {
            JSONObject j = new JSONObject().put("event",name).put("pid",Process.myPid()).put("uid",Process.myUid())
                .put("process",processName).put("wallMs",System.currentTimeMillis()).put("elapsedNs",SystemClock.elapsedRealtimeNanos());
            for(int i=0;i<pairs.length;i+=2) j.put((String)pairs[i],pairs[i+1]);
            String line=j.toString(); Log.i("LITHIC_R3",line);
            if(context!=null) {
                File dir=new File(context.getFilesDir(),"r3-traces"); if(!dir.isDirectory()&&!dir.mkdir())throw new IOException("trace mkdir");
                try(FileOutputStream o=new FileOutputStream(new File(dir,"trace-"+Process.myPid()+".jsonl"),true)) {
                    o.write((line+"\n").getBytes(StandardCharsets.UTF_8)); o.getFD().sync();
                }
            }
        } catch(Exception e) { Log.e("LITHIC_R3","TRACE_ERROR "+e); }
    }
    static String role(String name) { return PKG.equals(name)?"HOST":(PKG+":jpeg_worker").equals(name)?"WORKER":"REJECT"; }
    static String processName26() throws IOException {
        try(FileInputStream in=new FileInputStream("/proc/self/cmdline")) {
            byte[] b=new byte[256]; int n=in.read(b); if(n<=0)throw new IOException("empty cmdline");
            int end=0; while(end<n&&b[end]!=0)end++; if(end==n)throw new IOException("unbounded cmdline");
            return new String(b,0,end,StandardCharsets.UTF_8);
        }
    }
    static String sha(byte[] bytes) throws Exception {
        byte[] hash=MessageDigest.getInstance("SHA-256").digest(bytes); StringBuilder s=new StringBuilder();
        for(byte b:hash)s.append(String.format(Locale.ROOT,"%02x",b&255)); return s.toString();
    }
    static boolean readOnlyDescriptor(android.os.ParcelFileDescriptor fd)throws Exception {
        // API26 path: inspect this process's FD metadata, not hidden fcntlInt (public only API30).
        String info=new String(bytes(new File("/proc/self/fdinfo/"+fd.getFd())),StandardCharsets.UTF_8);
        for(String line:info.split("\n"))if(line.startsWith("flags:")){
            long flags=Long.parseLong(line.substring(6).trim(),8);return (flags&OsConstants.O_ACCMODE)==OsConstants.O_RDONLY;
        }
        throw new IOException("descriptor flags unavailable");
    }
    static byte[] bytes(File f) throws IOException {
        if(f.length()>16384)throw new IOException("research metadata bound");
        try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream o=new ByteArrayOutputStream()){
            byte[] b=new byte[1024];int n;while((n=in.read(b))!=-1){if(o.size()+n>16384)throw new IOException("bounded read");o.write(b,0,n);}return o.toByteArray();}
    }
    static void syncDir(File dir) throws Exception {
        java.io.FileDescriptor fd=Os.open(dir.getPath(),OsConstants.O_RDONLY|OsConstants.O_NOFOLLOW,0);
        try {if(!OsConstants.S_ISDIR(Os.fstat(fd).st_mode))throw new IOException("not directory");Os.fsync(fd);} finally {Os.close(fd);}
    }
    static Bundle identity(JSONObject j) throws Exception {
        Bundle b=new Bundle(); b.putInt("protocolVersion",1);
        for(String k:new String[]{"sessionNonce","attemptId","generationIdentity","sourceSHA256","consumerLeaseName"})b.putString(k,j.getString(k));
        for(String k:new String[]{"sourceSize","sourceDevice","sourceInode","guardDevice","guardInode"})b.putLong(k,j.getLong(k));
        return b;
    }
    static boolean same(Bundle a,Bundle b) {
        if(a==null||b==null||a.getInt("protocolVersion")!=1||b.getInt("protocolVersion")!=1)return false;
        for(String k:new String[]{"sessionNonce","attemptId","generationIdentity","sourceSHA256","consumerLeaseName"})
            if(!Objects.equals(a.getString(k),b.getString(k))||a.getString(k)==null)return false;
        for(String k:new String[]{"sourceSize","sourceDevice","sourceInode","guardDevice","guardInode"})if(a.getLong(k,-1)!=b.getLong(k,-2))return false;
        return true;
    }
    // Strongly typed AIDL envelope; Bundles remain local implementation details only.
    static SessionEnvelope envelope(Bundle b) {
        SessionEnvelope e=new SessionEnvelope();e.protocolVersion=b.getInt("protocolVersion");
        e.sessionNonce=b.getString("sessionNonce");e.attemptId=b.getString("attemptId");e.generationIdentity=b.getString("generationIdentity");
        e.sourceSize=b.getLong("sourceSize");e.sourceSHA256=b.getString("sourceSHA256");e.sourceDevice=b.getLong("sourceDevice");e.sourceInode=b.getLong("sourceInode");
        e.guardDevice=b.getLong("guardDevice");e.guardInode=b.getLong("guardInode");e.consumerLeaseName=b.getString("consumerLeaseName");
        e.outputDevice=b.getLong("outputDevice");e.outputInode=b.getLong("outputInode");e.requestStatus=b.getString("requestStatus","REQUESTED");
        e.workerResult=b.getString("workerResult","");e.resourceCloseStatus=b.getString("resourceCloseStatus","");e.fdClosed=b.getBoolean("fdClosed");e.kind=b.getString("kind","");
        e.workerPid=b.getInt("workerPid");e.workerUid=b.getInt("workerUid");e.ownerInitialized=b.getBoolean("ownerInitialized");
        e.durationMs=b.getInt("durationMs",600);e.scenario=b.getString("scenario","normal");e.delayHostResult=b.getBoolean("delayHostResult");return e;
    }
    static Bundle bundle(SessionEnvelope e) {
        Bundle b=new Bundle();if(e==null)return b;b.putInt("protocolVersion",e.protocolVersion);
        b.putString("sessionNonce",e.sessionNonce);b.putString("attemptId",e.attemptId);b.putString("generationIdentity",e.generationIdentity);
        b.putLong("sourceSize",e.sourceSize);b.putString("sourceSHA256",e.sourceSHA256);b.putLong("sourceDevice",e.sourceDevice);b.putLong("sourceInode",e.sourceInode);
        b.putLong("guardDevice",e.guardDevice);b.putLong("guardInode",e.guardInode);b.putString("consumerLeaseName",e.consumerLeaseName);
        b.putLong("outputDevice",e.outputDevice);b.putLong("outputInode",e.outputInode);b.putString("requestStatus",e.requestStatus);
        b.putString("workerResult",e.workerResult);b.putString("resourceCloseStatus",e.resourceCloseStatus);b.putBoolean("fdClosed",e.fdClosed);b.putString("kind",e.kind);
        b.putInt("workerPid",e.workerPid);b.putInt("workerUid",e.workerUid);b.putBoolean("ownerInitialized",e.ownerInitialized);
        b.putInt("durationMs",e.durationMs);b.putString("scenario",e.scenario);b.putBoolean("delayHostResult",e.delayHostResult);return b;
    }
}
