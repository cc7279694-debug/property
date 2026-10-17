package com.cooper.lithic.m10r3.probe;

import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.net.LocalServerSocket;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Synthetic v2 model, not production MediaFileProtocol/GC. No business references exist. */
final class StageStore {
    // Host instances and Binder callbacks share one authorization transition lock.
    static final Object authorizationLock=new Object();
    final File root;
    static final Set<String> OWNER_KEYS=set("protocol","version","attemptId","sessionNonce","generationIdentity","consumerLeaseName","stageDevice","stageInode","sourceSHA256","sourceSize","sourceDevice","sourceInode","guardDevice","guardInode");
    static final Set<String> KNOWN_CHILDREN=set("owner.json","consumer.guard","source.bin","output.bin","output-owner.json","state.json","receipt.json","revoked.json","cleanup.json","receipt.json.pending","state.json.pending");
    static final Set<String> STATES=set("DELIVERY_UNKNOWN","FD_SENT","REVOKED","CLEANED_SYNTHETIC");
    StageStore(Context c) throws Exception {
        root=new File(c.getFilesDir(),"synthetic-stages");
        if(Files.isSymbolicLink(root.toPath())||(!root.isDirectory()&&!root.mkdir()))throw new IOException("stages mkdir/link");
        require(OsConstants.S_ISDIR(Os.lstat(root.getPath()).st_mode),"stages not directory");
    }
    static Set<String> set(String... values){return new HashSet<>(Arrays.asList(values));}
    static Set<String> extended(String... values){Set<String> out=new HashSet<>(OWNER_KEYS);out.addAll(Arrays.asList(values));return out;}
    static void require(boolean ok,String message)throws IOException {if(!ok)throw new IOException(message);}
    static boolean integral(Object value){return value instanceof Integer||value instanceof Long;}
    static void attempt(String id)throws IOException {try{require(UUID.fromString(id).toString().equals(id),"bad attempt");}catch(IllegalArgumentException|NullPointerException e){throw new IOException("bad attempt",e);}}
    File dir(String id) throws Exception {attempt(id);return safe(root,id);}
    static File safe(File d,String n) throws Exception {
        if(n.contains("/")||n.contains("\\")||n.equals("..")||Files.isSymbolicLink(d.toPath()))throw new IOException("path");
        File f=new File(d,n);if(Files.isSymbolicLink(f.toPath())||!f.getCanonicalFile().getParentFile().equals(d.getCanonicalFile()))throw new IOException("link/path");return f;
    }
    static void regular(File f)throws Exception {require(OsConstants.S_ISREG(Os.lstat(f.getPath()).st_mode),"not regular: "+f.getName());}
    JSONObject create(String id) throws Exception {
        synchronized(authorizationLock) {
            File d=dir(id);if(!d.mkdir())throw new IOException("exclusive stage");
            byte[] bytes=new byte[8192];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)((i*31+7)&255);
            File src=safe(d,"source.bin"),guard=safe(d,"consumer.guard");
            try(FileOutputStream o=new FileOutputStream(src)){o.write(bytes);o.getFD().sync();}
            if(!guard.createNewFile())throw new IOException("guard create");
            StructStat s=Os.lstat(src.getPath()),g=Os.lstat(guard.getPath()),stage=Os.lstat(d.getPath());
            JSONObject j=new JSONObject().put("protocol","lithic-r3-synthetic-stage").put("version",2).put("attemptId",id)
                .put("sessionNonce",UUID.randomUUID().toString()).put("generationIdentity","synthetic-generation-r3").put("consumerLeaseName","lithic-r3-"+id)
                .put("stageDevice",stage.st_dev).put("stageInode",stage.st_ino)
                .put("sourceSHA256",ProbeSupport.sha(bytes)).put("sourceSize",bytes.length).put("sourceDevice",s.st_dev).put("sourceInode",s.st_ino)
                .put("guardDevice",g.st_dev).put("guardInode",g.st_ino);
            atomic(d,"owner.json",j);ProbeSupport.syncDir(root);ProbeSupport.event("STAGE_CREATED","attempt",id,"sourceSHA256",j.getString("sourceSHA256"));return j;
        }
    }
    JSONObject owner(String id) throws Exception {
        File d=dir(id);JSONObject j=read(safe(d,"owner.json"));
        require(keys(j).equals(OWNER_KEYS)&&j.getInt("version")==2&&j.getString("protocol").equals("lithic-r3-synthetic-stage")&&j.getString("attemptId").equals(id)&&j.getString("generationIdentity").equals("synthetic-generation-r3"),"owner rejected");
        attempt(j.getString("sessionNonce"));
        require(j.getString("sourceSHA256").matches("[0-9a-f]{64}")&&j.getLong("sourceSize")==8192,"source identity shape");
        for(String n:new String[]{"version","stageDevice","stageInode","sourceSize","sourceDevice","sourceInode","guardDevice","guardInode"})require(integral(j.get(n)),"owner integral number type");
        StructStat g=Os.lstat(safe(d,"consumer.guard").getPath()),stage=Os.lstat(d.getPath());
        require(StageIdentity.sameInode(OsConstants.S_ISREG(g.st_mode),j.getLong("guardDevice"),j.getLong("guardInode"),g.st_dev,g.st_ino),"guard identity rejected");
        require(StageIdentity.sameInode(OsConstants.S_ISDIR(stage.st_mode),j.getLong("stageDevice"),j.getLong("stageInode"),stage.st_dev,stage.st_ino)&&j.getString("consumerLeaseName").equals("lithic-r3-"+id),"stage identity rejected");
        File state=safe(d,"state.json");if(state.exists())validateState(read(state),j);
        return j;
    }
    static Map<String,Object> fields(JSONObject j)throws Exception {Map<String,Object> out=new HashMap<>();for(String key:keys(j)){Object value=j.get(key);out.put(key,integral(value)?((Number)value).longValue():value);}return out;}
    static void sameOwner(JSONObject record,JSONObject owner,String... extraKeys)throws Exception {require(StageIdentity.sameRecord(fields(owner),fields(record),set(extraKeys)),"full owner identity mismatch");}
    static void validateState(JSONObject state,JSONObject owner)throws Exception {
        sameOwner(state,owner,"requestStatus","pin");
        require(STATES.contains(state.getString("requestStatus"))&&state.get("pin") instanceof Boolean,"state shape");
        require(state.getBoolean("pin")!=state.getString("requestStatus").equals("CLEANED_SYNTHETIC"),"state pin/status");
    }
    static Set<String> keys(JSONObject j){Set<String>s=new HashSet<>();j.keys().forEachRemaining(s::add);return s;}
    static JSONObject read(File f) throws Exception {regular(f);return new JSONObject(new String(ProbeSupport.bytes(f),StandardCharsets.UTF_8));}
    static void atomic(File d,String name,JSONObject j) throws Exception {
        byte[] b=j.toString().getBytes(StandardCharsets.UTF_8);if(b.length>8192)throw new IOException("receipt too large");
        File tmp=safe(d,name+".pending"),dst=safe(d,name);
        if(tmp.exists())throw new IOException("unresolved pending metadata: "+name);
        if(dst.exists())regular(dst);
        if(!tmp.createNewFile())throw new IOException("exclusive pending metadata");
        try(FileOutputStream o=new FileOutputStream(tmp)){o.write(b);o.getFD().sync();}
        Files.move(tmp.toPath(),dst.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);ProbeSupport.syncDir(d);
    }
    void pin(String id,String state) throws Exception {
        synchronized(authorizationLock){require(STATES.contains(state)&&!state.equals("CLEANED_SYNTHETIC"),"pin state");JSONObject j=owner(id);require(!safe(dir(id),"cleanup.json").exists(),"terminal cannot repin");j.put("requestStatus",state).put("pin",true);atomic(dir(id),"state.json",j);ProbeSupport.event("PIN_DURABLE","attempt",id,"state",state);}
    }
    ParcelFileDescriptor open(String id,String name,int mode) throws Exception {synchronized(authorizationLock){require(isCurrentAuthorized(id),"source/output authorization revoked");return ParcelFileDescriptor.open(safe(dir(id),name),mode);}}
    boolean sourceExists(String id) throws Exception{return safe(dir(id),"source.bin").isFile();}
    boolean canAcquireGuard(String id) throws Exception {
        JSONObject j=owner(id);
        // Diagnostic only. Admission and cleanup acquire once and retain the lease throughout.
        try(LocalServerSocket lease=new LocalServerSocket(j.getString("consumerLeaseName"))){return true;}catch(IOException occupied){return false;}
    }
    String currentAttempt()throws Exception {
        synchronized(authorizationLock){File pointer=safe(root,"current.json");if(!pointer.exists())return null;JSONObject p=read(pointer);require(keys(p).equals(set("attemptId")),"current pointer shape");String id=p.getString("attemptId");attempt(id);return id;}
    }
    List<String> attempts()throws Exception {
        File[] entries=root.listFiles();require(entries!=null,"stage namespace unreadable");List<String> ids=new ArrayList<>();
        for(File f:entries){File checked=safe(root,f.getName());if(f.getName().equals("current.json")){regular(checked);continue;}attempt(f.getName());require(OsConstants.S_ISDIR(Os.lstat(checked.getPath()).st_mode),"unknown stage entry");ids.add(f.getName());}
        return ids;
    }
    void closeAllPriorAttempts()throws Exception {synchronized(authorizationLock){StageAdmission.closeAll(admissionBackend());}}
    void assertAllTerminal()throws Exception {synchronized(authorizationLock){StageAdmission.assertAllTerminal(admissionBackend(true));}}
    void publishCurrent(String id)throws Exception {synchronized(authorizationLock){StageAdmission.publish(admissionBackend(),id);}}
    StageAdmission.Backend admissionBackend(){return admissionBackend(false);}
    StageAdmission.Backend admissionBackend(boolean rejectPending){return new StageAdmission.Backend(){
        public List<String> attempts()throws Exception{return StageStore.this.attempts();}
        public String currentAttempt()throws Exception{return StageStore.this.currentAttempt();}
        public StageAdmission.Lease acquire(String id)throws Exception {attempt(id);LocalServerSocket lease=new LocalServerSocket("lithic-r3-"+id);return lease::close;}
        public void validate(String id)throws Exception {
            validateStage(id,true);
            if(rejectPending){
                // Even an apparently terminal proof cannot authorize ordinary run while any
                // metadata write is unresolved. This executes with every consumer lease held.
                String[] names=dir(id).list();require(names!=null,"stage unreadable");
                StageAuthorization.assertNoPending(Arrays.asList(names));
            }
        }
        public boolean terminal(String id)throws Exception {return safe(dir(id),"cleanup.json").exists();}
        public void revoke(String id)throws Exception {StageStore.this.revoke(id);}
        public void removeKnownData(String id)throws Exception {StageStore.this.removeKnownData(id);}
        public void writeTerminal(String id)throws Exception {StageStore.this.writeTerminal(id);}
        public void verifyTerminal(String id)throws Exception {StageStore.this.verifyTerminal(id);}
        public void publish(String id)throws Exception {
            require(!revoked(id),"revoked attempt cannot publish");
            // Only the exclusively created, not-yet-pinned candidate can be published. An old
            // REVOKED/pinned attempt without revoked.json must not regain authority via current.
            require(!safe(dir(id),"state.json").exists(),"existing state cannot republish");
            atomic(root,"current.json",new JSONObject().put("attemptId",id));
        }
    };}
    boolean isCurrentAuthorized(String id)throws Exception {
        synchronized(authorizationLock){if(!id.equals(currentAttempt()))return false;JSONObject j=owner(id);validateStage(id,true);return authorityWithOwner(id,j);}
    }
    boolean authorityWithOwner(String id,JSONObject owner)throws Exception {
        File d=dir(id),state=safe(d,"state.json");JSONObject s=state.exists()?read(state):null;
        if(s!=null)validateState(s,owner);
        File[] entries=d.listFiles();require(entries!=null,"stage unreadable");boolean pending=false;
        for(File entry:entries){require(KNOWN_CHILDREN.contains(entry.getName()),"unknown child preserved: "+entry.getName());regular(safe(d,entry.getName()));pending|=entry.getName().endsWith(".pending");}
        return StageAuthorization.isAuthorized(id.equals(currentAttempt()),s!=null,s==null?null:s.getString("requestStatus"),s!=null&&s.getBoolean("pin"),safe(d,"cleanup.json").exists(),revokedWithOwner(id,owner),pending);
    }
    void validateStage(String id,boolean rejectUnknown)throws Exception {
        JSONObject j=owner(id);File d=dir(id);File[] entries=d.listFiles();require(entries!=null,"stage unreadable");
        for(File f:entries)if(KNOWN_CHILDREN.contains(f.getName()))regular(safe(d,f.getName()));else require(!rejectUnknown,"unknown child preserved: "+f.getName());
        for(String n:new String[]{"receipt.json","receipt.json.pending"}){File f=safe(d,n);if(f.exists())require(validReceipt(read(f),j),"receipt rejected");}
        File pending=safe(d,"state.json.pending");if(pending.exists())validateState(read(pending),j);
        boolean revoked=revokedWithOwner(id,j),terminal=safe(d,"cleanup.json").exists();
        if(terminal){verifyTerminalWithOwner(id,j);return;}
        File state=safe(d,"state.json");if(state.exists())require(!read(state).getString("requestStatus").equals("CLEANED_SYNTHETIC"),"cleaned state without terminal proof");
        require(safe(d,"source.bin").exists(),"missing source without terminal proof");
        require(ownedSourceAndOutput(id,j),"owned inode/source/output rejected");
        if(revoked)require(!safe(d,"revoked.json.pending").exists(),"revoke pending");
    }
    boolean cleanup(String id) throws Exception {
        synchronized(authorizationLock) {
            final LocalServerSocket lease;
            try{attempt(id);lease=new LocalServerSocket("lithic-r3-"+id);}catch(IOException occupied){ProbeSupport.event("CLEANUP_DEFERRED_LEASE","attempt",id);return false;}
            try(LocalServerSocket held=lease) {
                try{validateStage(id,false);}catch(Exception bad){ProbeSupport.event("CLEANUP_DEFERRED_IDENTITY","attempt",id,"detail",bad.toString());return false;}
                if(safe(dir(id),"cleanup.json").exists()){verifyTerminal(id);return true;}
                revoke(id);removeKnownData(id);writeTerminal(id);verifyTerminal(id);
                ProbeSupport.event("CLEANUP_DONE","attempt",id,"unknownChildrenPreserved",true);return true;
            }
        }
    }
    void removeKnownData(String id)throws Exception {
        JSONObject j=owner(id);require(revokedWithOwner(id,j),"cleanup needs durable revoke");
        // All ownership checks precede deletion. Interrupted partial cleanup is not auto-repaired.
        require(safe(dir(id),"source.bin").exists()&&ownedSourceAndOutput(id,j),"resource pre-delete identity");
        for(String n:new String[]{"source.bin","output.bin","receipt.json.pending","state.json.pending"}){File f=safe(dir(id),n);if(f.exists()&&!f.delete())throw new IOException("cleanup failed: "+n);}
        ProbeSupport.syncDir(dir(id));
    }
    void writeTerminal(String id)throws Exception {
        JSONObject j=owner(id);File d=dir(id);require(revokedWithOwner(id,j)&&!safe(d,"source.bin").exists()&&!safe(d,"output.bin").exists(),"terminal resources present");
        atomic(d,"state.json",new JSONObject(j.toString()).put("requestStatus","CLEANED_SYNTHETIC").put("pin",false));
        // Last durable write: interruption before this marker leaves an unclaimable stage.
        atomic(d,"cleanup.json",new JSONObject(j.toString()).put("proof","EXCLUSIVE_KERNEL_LEASE_AND_OWNED_INODES").put("syntheticOnly",true).put("resourcesAbsent",true).put("receiptSHA256",retainedHash(d,"receipt.json")).put("outputOwnerSHA256",retainedHash(d,"output-owner.json")));
    }
    void verifyTerminal(String id)throws Exception {verifyTerminalWithOwner(id,owner(id));}
    void verifyTerminalWithOwner(String id,JSONObject j)throws Exception {
        File d=dir(id);JSONObject c=read(safe(d,"cleanup.json"));
        sameOwner(c,j,"proof","syntheticOnly","resourcesAbsent","receiptSHA256","outputOwnerSHA256");
        require(c.getString("proof").equals("EXCLUSIVE_KERNEL_LEASE_AND_OWNED_INODES")&&Boolean.TRUE.equals(c.get("syntheticOnly"))&&Boolean.TRUE.equals(c.get("resourcesAbsent")),"terminal proof");
        require(revokedWithOwner(id,j),"terminal revoke missing");JSONObject state=read(safe(d,"state.json"));validateState(state,j);require(state.getString("requestStatus").equals("CLEANED_SYNTHETIC")&&!state.getBoolean("pin"),"terminal state");
        require(c.getString("receiptSHA256").equals(retainedHash(d,"receipt.json"))&&c.getString("outputOwnerSHA256").equals(retainedHash(d,"output-owner.json")),"retained metadata changed after terminal proof");
        for(String n:new String[]{"source.bin","output.bin","receipt.json.pending","state.json.pending"})require(!safe(d,n).exists(),"terminal resource/pending remains");
        File outOwner=safe(d,"output-owner.json");if(outOwner.exists())validateOutputOwner(read(outOwner),j);
    }
    static String retainedHash(File d,String name)throws Exception {File f=safe(d,name);if(!f.exists())return "ABSENT";regular(f);return ProbeSupport.sha(ProbeSupport.bytes(f));}
    boolean revoked(String id)throws Exception {synchronized(authorizationLock){return revokedWithOwner(id,owner(id));}}
    boolean revokedWithOwner(String id,JSONObject j)throws Exception {File f=safe(dir(id),"revoked.json");if(!f.exists())return false;JSONObject r=read(f);sameOwner(r,j,"revoked");require(Boolean.TRUE.equals(r.get("revoked")),"revoke shape");return true;}
    void revoke(String id)throws Exception {synchronized(authorizationLock){JSONObject o=owner(id);StageAuthorization.revokeVerified(new StageAuthorization.Revocation(){public boolean sameOwnerRevoked()throws Exception{sameOwner(owner(id),o);require(!safe(dir(id),"revoked.json.pending").exists(),"revoke pending");return revokedWithOwner(id,o);}public void writeSameOwnerRevoke()throws Exception{atomic(dir(id),"revoked.json",new JSONObject(o.toString()).put("revoked",true));}});}}
    void recordOutput(String id) throws Exception {
        synchronized(authorizationLock){
            JSONObject o=owner(id);File d=dir(id);
            // Output is exclusively created before its inode metadata exists. Full validateStage
            // would reject that expected intermediate state; use the identical authority predicate
            // with strict namespace/source validation, then establish this one new output identity.
            require(authorityWithOwner(id,o),"output authorization revoked");
            require(safe(d,"source.bin").exists()&&ownedSource(id,o),"output source identity rejected");
            for(String n:new String[]{"receipt.json","receipt.json.pending"}){File f=safe(d,n);if(f.exists())require(validReceipt(read(f),o),"receipt rejected");}
            StructStat s=Os.lstat(safe(d,"output.bin").getPath());require(OsConstants.S_ISREG(s.st_mode),"output regular");
            File metadata=safe(d,"output-owner.json");
            if(metadata.exists()){
                JSONObject previous=read(metadata);validateOutputOwner(previous,o);
                require(StageIdentity.sameInode(true,previous.getLong("device"),previous.getLong("inode"),s.st_dev,s.st_ino),"output identity changed");
                return;
            }
            atomic(d,"output-owner.json",new JSONObject().put("attemptId",id).put("sessionNonce",o.getString("sessionNonce")).put("device",s.st_dev).put("inode",s.st_ino));
            JSONObject recorded=read(metadata);validateOutputOwner(recorded,o);
            require(StageIdentity.sameInode(true,recorded.getLong("device"),recorded.getLong("inode"),s.st_dev,s.st_ino),"output identity readback");
        }
    }
    static void validateOutputOwner(JSONObject o,JSONObject j)throws Exception {
        require(keys(o).equals(set("attemptId","sessionNonce","device","inode"))&&o.getString("attemptId").equals(j.getString("attemptId"))&&o.getString("sessionNonce").equals(j.getString("sessionNonce"))&&integral(o.get("device"))&&integral(o.get("inode")),"output owner");
    }
    boolean ownedSource(String id,JSONObject j)throws Exception {
        File src=safe(dir(id),"source.bin");
        if(src.exists()){StructStat s=Os.lstat(src.getPath());return StageIdentity.sameInode(OsConstants.S_ISREG(s.st_mode),j.getLong("sourceDevice"),j.getLong("sourceInode"),s.st_dev,s.st_ino)&&s.st_size==j.getLong("sourceSize")&&ProbeSupport.sha(ProbeSupport.bytes(src)).equals(j.getString("sourceSHA256"));}
        return true;
    }
    boolean ownedSourceAndOutput(String id,JSONObject j) throws Exception {
        File d=dir(id),out=safe(d,"output.bin"),metadata=safe(d,"output-owner.json");
        if(!ownedSource(id,j))return false;
        if(out.exists()){if(!metadata.isFile())return false;JSONObject o=read(metadata);validateOutputOwner(o,j);StructStat s=Os.lstat(out.getPath());if(!StageIdentity.sameInode(OsConstants.S_ISREG(s.st_mode),o.getLong("device"),o.getLong("inode"),s.st_dev,s.st_ino))return false;}
        else if(metadata.exists())return false;
        return true;
    }
    static boolean legacyV1Recognizes(JSONObject j) throws Exception {return keys(j).equals(set("protocol","version","assetId","generation"))&&j.getInt("version")==1&&j.getString("protocol").equals("lithic-m10-import");}
    JSONObject receipt(String id,boolean retired) throws Exception {
        JSONObject o=owner(id);return new JSONObject().put("protocol","lithic-r3-synthetic-result").put("version",1)
            .put("attemptId",id).put("sessionNonce",o.getString("sessionNonce")).put("generationIdentity",o.getString("generationIdentity"))
            .put("sourceSHA256",o.getString("sourceSHA256")).put("sourceSize",o.getLong("sourceSize"))
            .put("kind","SYNTHETIC_TASK_ONLY").put("workerResult","SUCCESS").put("resourceCloseStatus","DATA_FDS_CLOSED").put("workerRetired",retired).put("formalReferences",0);
    }
    static boolean validReceipt(JSONObject r,JSONObject o) {
        try {
            if(!keys(r).equals(set("protocol","version","attemptId","sessionNonce","generationIdentity","sourceSHA256","sourceSize","kind","workerResult","resourceCloseStatus","workerRetired","formalReferences")))return false;
            if(!r.getString("protocol").equals("lithic-r3-synthetic-result")||r.getInt("version")!=1||!r.getString("kind").equals("SYNTHETIC_TASK_ONLY")||r.getInt("formalReferences")!=0||!r.getString("workerResult").equals("SUCCESS")||!r.getString("resourceCloseStatus").equals("DATA_FDS_CLOSED"))return false;
            Map<String,Object> identity=new HashMap<>();Map<String,Object> ownerFields=fields(o);
            for(String k:new String[]{"attemptId","sessionNonce","generationIdentity","sourceSHA256","sourceSize"})identity.put(k,ownerFields.get(k));
            return StageIdentity.sameRecord(identity,fields(r),set("protocol","version","kind","workerResult","resourceCloseStatus","workerRetired","formalReferences"))&&r.get("workerRetired") instanceof Boolean&&integral(r.get("version"))&&integral(r.get("formalReferences"))&&integral(r.get("sourceSize"));
        }catch(Exception e){return false;}
    }
}
