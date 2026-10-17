package com.cooper.lithic.m10r3.probe;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.net.LocalServerSocket;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import org.json.JSONObject;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Synthetic v2 model, not production MediaFileProtocol/GC. No business references exist. */
final class StageStore {
    final File root;
    static final Set<String> OWNER_KEYS=new HashSet<>(Arrays.asList("protocol","version","attemptId","sessionNonce","generationIdentity","consumerLeaseName","stageDevice","stageInode","sourceSHA256","sourceSize","sourceDevice","sourceInode","guardDevice","guardInode"));
    StageStore(Context c) throws Exception {
        root=new File(c.getFilesDir(),"synthetic-stages"); if(!root.isDirectory()&&!root.mkdir())throw new IOException("stages mkdir");
    }
    File dir(String id) throws Exception { if(!UUID.fromString(id).toString().equals(id))throw new IOException("bad attempt"); return safe(root,id); }
    static File safe(File d,String n) throws Exception {
        if(n.contains("/")||n.contains("\\")||n.equals("..")||Files.isSymbolicLink(d.toPath()))throw new IOException("path");
        File f=new File(d,n); if(Files.isSymbolicLink(f.toPath())||!f.getCanonicalFile().getParentFile().equals(d.getCanonicalFile()))throw new IOException("link/path"); return f;
    }
    JSONObject create(String id) throws Exception {
        File d=dir(id); if(!d.mkdir())throw new IOException("exclusive stage");
        byte[] bytes=new byte[8192]; for(int i=0;i<bytes.length;i++)bytes[i]=(byte)((i*31+7)&255);
        File src=safe(d,"source.bin"),guard=safe(d,"consumer.guard");
        try(FileOutputStream o=new FileOutputStream(src)){o.write(bytes);o.getFD().sync();}
        if(!guard.createNewFile())throw new IOException("guard create");
        StructStat s=Os.lstat(src.getPath()),g=Os.lstat(guard.getPath()),stage=Os.lstat(d.getPath());
        JSONObject j=new JSONObject().put("protocol","lithic-r3-synthetic-stage").put("version",2).put("attemptId",id)
            .put("sessionNonce",UUID.randomUUID().toString()).put("generationIdentity","synthetic-generation-r3").put("consumerLeaseName","lithic-r3-"+id)
            .put("stageDevice",stage.st_dev).put("stageInode",stage.st_ino)
            .put("sourceSHA256",ProbeSupport.sha(bytes)).put("sourceSize",bytes.length).put("sourceDevice",s.st_dev).put("sourceInode",s.st_ino)
            .put("guardDevice",g.st_dev).put("guardInode",g.st_ino);
        atomic(d,"owner.json",j); ProbeSupport.syncDir(root); ProbeSupport.event("STAGE_CREATED","attempt",id,"sourceSHA256",j.getString("sourceSHA256")); return j;
    }
    JSONObject owner(String id) throws Exception {
        File d=dir(id); JSONObject j=read(safe(d,"owner.json"));
        if(!keys(j).equals(OWNER_KEYS)||j.getInt("version")!=2||!j.getString("protocol").equals("lithic-r3-synthetic-stage")||!j.getString("attemptId").equals(id)||!j.getString("generationIdentity").equals("synthetic-generation-r3"))throw new IOException("owner rejected");
        StructStat g=Os.lstat(safe(d,"consumer.guard").getPath());
        if(!OsConstants.S_ISREG(g.st_mode)||g.st_dev!=j.getLong("guardDevice")||g.st_ino!=j.getLong("guardInode"))throw new IOException("guard identity rejected");
        StructStat stage=Os.lstat(d.getPath());if(!OsConstants.S_ISDIR(stage.st_mode)||stage.st_dev!=j.getLong("stageDevice")||stage.st_ino!=j.getLong("stageInode")||!j.getString("consumerLeaseName").equals("lithic-r3-"+id))throw new IOException("stage identity rejected");
        File state=safe(d,"state.json");if(state.exists()){
            JSONObject pin=read(state);for(String k:OWNER_KEYS)if(!j.get(k).equals(pin.get(k)))throw new IOException("owner/pin identity mismatch");
        }
        return j;
    }
    static Set<String> keys(JSONObject j){Set<String>s=new HashSet<>();j.keys().forEachRemaining(s::add);return s;}
    static JSONObject read(File f) throws Exception {return new JSONObject(new String(ProbeSupport.bytes(f),StandardCharsets.UTF_8));}
    static void atomic(File d,String name,JSONObject j) throws Exception {
        byte[] b=j.toString().getBytes(StandardCharsets.UTF_8); if(b.length>8192)throw new IOException("receipt too large");
        File tmp=safe(d,name+".pending"),dst=safe(d,name);
        try(FileOutputStream o=new FileOutputStream(tmp)){o.write(b);o.getFD().sync();}
        Files.move(tmp.toPath(),dst.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); ProbeSupport.syncDir(d);
    }
    void pin(String id,String state) throws Exception {JSONObject j=owner(id); j.put("requestStatus",state).put("pin",true); atomic(dir(id),"state.json",j); ProbeSupport.event("PIN_DURABLE","attempt",id,"state",state);}
    ParcelFileDescriptor open(String id,String name,int mode) throws Exception {owner(id); return ParcelFileDescriptor.open(safe(dir(id),name),mode);}
    boolean sourceExists(String id) throws Exception{return safe(dir(id),"source.bin").isFile();}
    boolean canAcquireGuard(String id) throws Exception {
        JSONObject j=owner(id);
        try(LocalServerSocket lease=new LocalServerSocket(j.getString("consumerLeaseName"))){return true;}catch(IOException occupied){return false;}
    }
    boolean cleanup(String id) throws Exception {
        JSONObject j=owner(id); File d=dir(id);
        File receipt=safe(d,"receipt.json");
        try{if(receipt.exists()&&!validReceipt(read(receipt),j))return false;}catch(Exception bad){return false;}
        final LocalServerSocket lease;
        try{lease=new LocalServerSocket(j.getString("consumerLeaseName"));}catch(IOException occupied){ProbeSupport.event("CLEANUP_DEFERRED_LEASE","attempt",id);return false;}
        try {
                if(!ownedSourceAndOutput(id,j)){ProbeSupport.event("CLEANUP_DEFERRED_IDENTITY","attempt",id);return false;}
                // Durable revoke before deleting anything. Old host Binder is dead after a restart;
                // pending grants must also fail client-liveness checks in WorkerService.
                atomic(d,"revoked.json",new JSONObject().put("attemptId",id).put("sessionNonce",j.getString("sessionNonce")).put("revoked",true));
                for(String n:new String[]{"source.bin","output.bin","receipt.json.pending","state.json.pending"}) {
                    File f=safe(d,n); if(f.exists()&&!f.delete())throw new IOException("cleanup failed");
                }
                atomic(d,"cleanup.json",new JSONObject().put("attemptId",id).put("proof","EXCLUSIVE_KERNEL_LEASE_AND_OWNED_INODES").put("syntheticOnly",true));
                JSONObject state=new JSONObject(j.toString()).put("requestStatus","CLEANED_SYNTHETIC").put("pin",false);atomic(d,"state.json",state);
                ProbeSupport.syncDir(d);ProbeSupport.event("CLEANUP_DONE","attempt",id,"unknownChildrenPreserved",true);return true;
        } finally {lease.close();}
    }
    boolean revoked(String id)throws Exception{return safe(dir(id),"revoked.json").exists();}
    void revoke(String id)throws Exception {JSONObject o=owner(id);atomic(dir(id),"revoked.json",new JSONObject().put("attemptId",id).put("sessionNonce",o.getString("sessionNonce")).put("revoked",true));}
    void recordOutput(String id) throws Exception {
        JSONObject o=owner(id);StructStat s=Os.lstat(safe(dir(id),"output.bin").getPath());
        atomic(dir(id),"output-owner.json",new JSONObject().put("attemptId",id).put("sessionNonce",o.getString("sessionNonce")).put("device",s.st_dev).put("inode",s.st_ino));
    }
    boolean ownedSourceAndOutput(String id,JSONObject j) throws Exception {
        File d=dir(id),src=safe(d,"source.bin"),out=safe(d,"output.bin");
        if(src.exists()){StructStat s=Os.lstat(src.getPath());if(!OsConstants.S_ISREG(s.st_mode)||s.st_dev!=j.getLong("sourceDevice")||s.st_ino!=j.getLong("sourceInode")||s.st_size!=j.getLong("sourceSize")||!ProbeSupport.sha(ProbeSupport.bytes(src)).equals(j.getString("sourceSHA256")))return false;}
        if(out.exists()){
            File metadata=safe(d,"output-owner.json");if(!metadata.isFile())return false;JSONObject o=read(metadata);StructStat s=Os.lstat(out.getPath());
            if(!keys(o).equals(new HashSet<>(Arrays.asList("attemptId","sessionNonce","device","inode")))||!o.getString("attemptId").equals(id)||!o.getString("sessionNonce").equals(j.getString("sessionNonce"))||!OsConstants.S_ISREG(s.st_mode)||s.st_dev!=o.getLong("device")||s.st_ino!=o.getLong("inode"))return false;
        }
        return true;
    }
    static boolean legacyV1Recognizes(JSONObject j) throws Exception {
        return keys(j).equals(new HashSet<>(Arrays.asList("protocol","version","assetId","generation")))&&j.getInt("version")==1&&j.getString("protocol").equals("lithic-m10-import");
    }
    JSONObject receipt(String id,boolean retired) throws Exception {
        JSONObject o=owner(id);return new JSONObject().put("protocol","lithic-r3-synthetic-result").put("version",1)
            .put("attemptId",id).put("sessionNonce",o.getString("sessionNonce")).put("generationIdentity",o.getString("generationIdentity"))
            .put("sourceSHA256",o.getString("sourceSHA256")).put("sourceSize",o.getLong("sourceSize"))
            .put("kind","SYNTHETIC_TASK_ONLY").put("workerResult","SUCCESS").put("resourceCloseStatus","DATA_FDS_CLOSED")
            .put("workerRetired",retired).put("formalReferences",0);
    }
    static boolean validReceipt(JSONObject r,JSONObject o) {
        try {
            if(!keys(r).equals(new HashSet<>(Arrays.asList("protocol","version","attemptId","sessionNonce","generationIdentity","sourceSHA256","sourceSize","kind","workerResult","resourceCloseStatus","workerRetired","formalReferences"))))return false;
            if(!r.getString("protocol").equals("lithic-r3-synthetic-result")||r.getInt("version")!=1||!r.getString("kind").equals("SYNTHETIC_TASK_ONLY")||r.getInt("formalReferences")!=0||!r.getString("workerResult").equals("SUCCESS")||!r.getString("resourceCloseStatus").equals("DATA_FDS_CLOSED"))return false;
            for(String k:new String[]{"attemptId","sessionNonce","generationIdentity","sourceSHA256"})if(!r.getString(k).equals(o.getString(k)))return false;
            r.getBoolean("workerRetired");return r.getLong("sourceSize")==o.getLong("sourceSize");
        }catch(Exception e){return false;}
    }
}
