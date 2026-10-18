package com.cooper.lithic.data.media.jpegworker

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import com.cooper.lithic.data.media.AndroidImageCodec
import com.cooper.lithic.data.media.ImageStoreException
import com.cooper.lithic.data.media.ImageStoreFaults
import com.cooper.lithic.data.media.MediaFailureKind
import com.cooper.lithic.research.StrictJpegCandidate
import java.io.File
import java.io.IOException
import java.nio.channels.FileLock
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Non-exported same-UID compute process. It never constructs a repository, owner or AppContainer. */
class JpegWorkerService : Service() {
    private val monitor=Any()
    private val compute=Executors.newSingleThreadExecutor {r->Thread(r,"jpeg-worker-compute")}
    private val control=Executors.newSingleThreadScheduledExecutor {r->Thread(r,"jpeg-worker-watchdog")}
    private var request:JpegWorkerRequest?=null
    private var client:IBinder?=null
    private var host:JpegWorkerIdentity?=null
    private lateinit var identity:JpegWorkerIdentity
    private val revoked=AtomicBoolean(false)
    private val finished=AtomicBoolean(false)
    private var delivered=false
    private var guardStream:ParcelFileDescriptor.AutoCloseOutputStream?=null
    private var guardLease:FileLock?=null
    private val hostDeath=IBinder.DeathRecipient { revoked.set(true);selfExit(0) }

    override fun onCreate() {
        super.onCreate()
        check(JpegProcessRole.role(packageName)==JpegProcessRole.Role.WORKER) {"Unverified Worker role"}
        identity=JpegProcessRole.identity()
        // A stray bind cannot leave a permanent idle service process.
        control.schedule({if(synchronized(monitor){request==null})selfExit(0)},30,TimeUnit.SECONDS)
    }
    override fun onBind(intent:Intent?):IBinder? = if(JpegProcessRole.role(packageName)==JpegProcessRole.Role.WORKER) endpoint else null

    private val endpoint=object:Binder() {
        override fun onTransact(code:Int,data:Parcel,reply:Parcel?,flags:Int):Boolean {
            if(code==INTERFACE_TRANSACTION){reply?.writeString(JpegWorkerWire.DESCRIPTOR);return true}
            if(code !in JpegWorkerWire.RESERVE..JpegWorkerWire.REVOKE)return super.onTransact(code,data,reply,flags)
            try {
                data.enforceInterface(JpegWorkerWire.DESCRIPTOR)
                if(Binder.getCallingUid()!=Process.myUid()||Binder.getCallingPid()==Process.myPid())throw SecurityException("Worker caller rejected")
                when(code) {
                    JpegWorkerWire.RESERVE -> reserve(data)
                    JpegWorkerWire.DELIVER -> deliver(data)
                    JpegWorkerWire.RETIRE -> {
                        authenticate(JpegWorkerRequest.read(data.readBundle(javaClass.classLoader) ?: throw IOException("Missing retire identity")))
                        if(!finished.get())throw IOException("Worker has not closed data resources")
                        selfExit(100)
                    }
                    JpegWorkerWire.REVOKE -> {
                        authenticate(JpegWorkerRequest.read(data.readBundle(javaClass.classLoader) ?: throw IOException("Missing revoke identity")))
                        revoked.set(true);selfExit(50)
                    }
                }
                reply?.writeNoException();reply?.writeBundle(Bundle().apply{putBoolean("accepted",true)})
            } catch(error:Exception) {reply?.writeException(error)}
            return true
        }
    }
    private fun reserve(data:Parcel) {
        val next=JpegWorkerRequest.read(data.readBundle(javaClass.classLoader) ?: throw IOException("Missing request"))
        val callback=data.readStrongBinder() ?: throw IOException("Missing Host endpoint")
        val actualHost=JpegProcessRole.identity(Binder.getCallingPid())
        if(actualHost.uid!=Process.myUid()||actualHost.pid==identity.pid||
            JpegProcessRole.readCmdline(File("/proc/${actualHost.pid}/cmdline"))!=packageName)throw SecurityException("Host identity mismatch")
        synchronized(monitor) {
            if(request!=null||revoked.get())throw IOException("Worker is one-shot")
            request=next;client=callback;host=actualHost
        }
        callback.linkToDeath(hostDeath,0)
        if(!JpegWorkerWire.callback(callback,JpegWorkerWire.HELLO,next,identity,JpegWorkerWire.policyBundle())) {
            revoked.set(true);selfExit(0);throw IOException("Synchronous identity hello denied")
        }
        control.schedule({revoked.set(true);selfExit(0)},JpegWorkerWire.HARD_DEADLINE_MS,TimeUnit.MILLISECONDS)
    }
    private fun authenticate(incoming:JpegWorkerRequest) {
        val expected=synchronized(monitor){request}
        val actual=JpegProcessRole.identity(Binder.getCallingPid())
        if(expected!=incoming||actual!=host||Binder.getCallingUid()!=identity.uid||revoked.get())throw SecurityException("Stale Worker command")
    }
    private fun deliver(data:Parcel) {
        val incoming=JpegWorkerRequest.read(data.readBundle(javaClass.classLoader) ?: throw IOException("Missing delivery identity"))
        authenticate(incoming)
        val outputDev=data.readLong();val outputIno=data.readLong()
        var source:ParcelFileDescriptor?=null;var output:ParcelFileDescriptor?=null;var guard:ParcelFileDescriptor?=null
        try {
            source=JpegWorkerWire.readFd(data);output=JpegWorkerWire.readFd(data);guard=JpegWorkerWire.readFd(data)
            synchronized(monitor) {if(delivered||revoked.get())throw IOException("Repeated delivery");delivered=true}
            val srcStat=Os.fstat(source.fileDescriptor);val outStat=Os.fstat(output.fileDescriptor);val guardStat=Os.fstat(guard.fileDescriptor)
            if((srcStat.st_dev==outStat.st_dev&&srcStat.st_ino==outStat.st_ino)||
                (srcStat.st_dev==guardStat.st_dev&&srcStat.st_ino==guardStat.st_ino)||
                (outStat.st_dev==guardStat.st_dev&&outStat.st_ino==guardStat.st_ino))throw IOException("Aliased Worker descriptors")
            if(!OsConstants.S_ISREG(srcStat.st_mode)||srcStat.st_dev!=incoming.sourceDevice||srcStat.st_ino!=incoming.sourceInode||srcStat.st_size!=incoming.sourceSize||JpegWorkerWire.fdMode(source)!=OsConstants.O_RDONLY)throw IOException("Source FD identity mismatch")
            if(!OsConstants.S_ISREG(outStat.st_mode)||outStat.st_dev!=outputDev||outStat.st_ino!=outputIno||outStat.st_size!=0L||JpegWorkerWire.fdMode(output)!=OsConstants.O_RDWR)throw IOException("Output FD identity mismatch")
            if(!OsConstants.S_ISREG(guardStat.st_mode)||guardStat.st_dev!=incoming.guardDevice||guardStat.st_ino!=incoming.guardInode||guardStat.st_size!=0L||JpegWorkerWire.fdMode(guard)!=OsConstants.O_RDWR)throw IOException("Guard FD identity mismatch")
            val stream=ParcelFileDescriptor.AutoCloseOutputStream(guard)
            val lease=stream.channel.tryLock() ?: throw IOException("Consumer guard is already held")
            guardStream=stream;guardLease=lease;guard=null // Stream/lease retained until retirement.
            val stableSource=source;val stableOutput=output
            compute.execute {compute(incoming,stableSource,stableOutput)}
            source=null;output=null
        } finally {JpegWorkerWire.close(source);JpegWorkerWire.close(output);JpegWorkerWire.close(guard)}
    }
    private fun compute(request:JpegWorkerRequest,source:ParcelFileDescriptor,output:ParcelFileDescriptor) {
        var outcome=JpegWorkerOutcome.RESULT_UNKNOWN
        var detail="UNFINISHED"
        var strictFinished=false;var destroyed=false
        var sourceClosed=false;var outputClosed=false
        var width=0;var height=0;var bytes=0L;var sha=""
        var scratch:File?=null
        try {
            if(revoked.get())throw IOException("Worker revoked before decode")
            try {System.loadLibrary("strictjpeg_candidate")}catch(error:LinkageError){outcome=JpegWorkerOutcome.LOAD_ERROR;throw IOException("Native load rejected",error)}
            val native=StrictJpegCandidate.validateReadonlyFd(source.fd,JpegWorkerWire.REQUEST_BUDGET)
            if(native==null||native.size!=16)throw IOException("Native result absent")
            destroyed=native[7]==1L && native[15]==1L && native[3]==0L && native[4]==0L && native[5]==0L
            strictFinished=native[0]==0L && native[10]==1L && native[8]==native[9] && native[6]==1L && destroyed
            when(native[0]) {
                10L->{outcome=JpegWorkerOutcome.INVALID_IMAGE;throw IOException("Strict JPEG rejection code=${native[1]}")}
                20L->{outcome=JpegWorkerOutcome.RESOURCE_REJECTED;throw IOException("Native resource rejection code=${native[1]}")}
                0L->if(!strictFinished)throw IOException("Incomplete native cleanup proof")
                else->throw IOException("Native capability or infrastructure failure code=${native[1]}")
            }
            if(revoked.get())throw IOException("Worker revoked after strict decode")
            val dir=File(cacheDir,"jpeg-compute-${UUID.randomUUID()}")
            if(!dir.mkdir())throw IOException("Exclusive Worker scratch failed")
            scratch=dir
            val copy=File(dir,"source.bin")
            if(!copy.createNewFile())throw IOException("Exclusive scratch source failed")
            val digest=MessageDigest.getInstance("SHA-256")
            copy.outputStream().use {target->
                val buffer=ByteArray(64*1024);var offset=0L
                while(offset<request.sourceSize) {
                    if(revoked.get())throw IOException("Worker revoked while copying")
                    val count=Os.pread(source.fileDescriptor,buffer,0,minOf(buffer.size.toLong(),request.sourceSize-offset).toInt(),offset)
                    if(count<=0)throw IOException("Source FD truncated")
                    target.write(buffer,0,count);digest.update(buffer,0,count);offset+=count
                }
                target.fd.sync()
            }
            if(digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}!=request.sourceSha256)throw IOException("Source byte identity mismatch")
            val again=Os.fstat(source.fileDescriptor)
            if(again.st_dev!=request.sourceDevice||again.st_ino!=request.sourceInode||again.st_size!=request.sourceSize)throw IOException("Source identity changed")
            val normalized=File(dir,"normalized.jpg")
            val info=AndroidImageCodec.normalize(copy,normalized,"image/jpeg",ImageStoreFaults.NONE)
            if(info.mime!="image/jpeg"||normalized.length() !in 1..AndroidImageCodec.STORED_LIMIT)throw IOException("Normalized output identity")
            width=info.width;height=info.height;bytes=normalized.length();sha=JpegWorkerWire.sha(normalized)
            if(revoked.get())throw IOException("Worker revoked before output")
            Os.ftruncate(output.fileDescriptor,0)
            normalized.inputStream().use {input->
                val buffer=ByteArray(64*1024);var offset=0L
                while(true){val n=input.read(buffer);if(n<0)break;var written=0
                    while(written<n){val count=Os.pwrite(output.fileDescriptor,buffer,written,n-written,offset);if(count<=0)throw IOException("Output write failed");written+=count;offset+=count}
                }
                if(offset!=bytes)throw IOException("Output copy length mismatch")
            }
            Os.fsync(output.fileDescriptor)
            outcome=JpegWorkerOutcome.SUCCESS;detail="STRICT_FINISHED_NORMALIZED"
        } catch(error:LinkageError) {outcome=JpegWorkerOutcome.LOAD_ERROR;detail="JNI_LINK_OR_SYMBOL_FAILED"
        } catch(error:OutOfMemoryError) {outcome=JpegWorkerOutcome.RESOURCE_REJECTED;detail="JAVA_ALLOCATION_FAILED"}
        catch(error:ImageStoreException) {
            outcome=when(error.kind){MediaFailureKind.INVALID_IMAGE->JpegWorkerOutcome.INVALID_IMAGE;MediaFailureKind.TOO_LARGE->JpegWorkerOutcome.RESOURCE_REJECTED;MediaFailureKind.UNSUPPORTED->JpegWorkerOutcome.UNSUPPORTED;else->JpegWorkerOutcome.RESULT_UNKNOWN}
            detail=error.kind.name
        } catch(error:Exception) {detail=error.message?.take(160) ?: error.javaClass.simpleName}
        finally {
            sourceClosed=JpegWorkerWire.close(source);outputClosed=JpegWorkerWire.close(output)
            val clean=try {scratch==null||clearKnownScratch(scratch)}catch(_:Exception){false}
            if(!sourceClosed||!outputClosed||!clean){outcome=JpegWorkerOutcome.RESULT_UNKNOWN;detail="RESOURCE_CLOSE_UNKNOWN"}
            finished.set(true)
        }
        val reply=Bundle().apply {
            putString("decoderPolicy",JpegDecoderPolicy.DECODER);putString("normalizationPolicy",JpegDecoderPolicy.NORMALIZATION)
            putString("outcome",outcome.name);putString("detail",detail);putBoolean("sourceClosed",sourceClosed)
            putBoolean("outputClosed",outputClosed);putBoolean("strictFinished",strictFinished);putBoolean("decoderDestroyed",destroyed)
            putInt("width",width);putInt("height",height);putLong("outputSize",bytes);putString("outputSha",sha)
        }
        if(!revoked.get())try {
            val accepted=client?.let{JpegWorkerWire.callback(it,JpegWorkerWire.RESULT,request,identity,reply)}==true
            if(!accepted){revoked.set(true);selfExit(0)}
        }catch(_:Exception){revoked.set(true);selfExit(0)}
        // No next task: an authenticated Host retirement or the independent hard deadline ends this process.
    }
    private fun clearKnownScratch(dir:File):Boolean {
        if(dir.parentFile!=cacheDir||!dir.name.startsWith("jpeg-compute-"))return false
        val children=dir.listFiles() ?: return false
        if(children.any{it.name !in setOf("source.bin","normalized.jpg")||java.nio.file.Files.isSymbolicLink(it.toPath())||!it.isFile})return false
        return children.all{it.delete()}&&dir.delete()
    }
    private fun selfExit(delayMs:Long) {
        control.schedule({
            // If native is still running, retain guard until OS closes it; never expose an early released lease.
            if(finished.get())try {guardLease?.release();guardStream?.close()}catch(_:Exception) { /* Process exit still closes resources. */ }
            Process.killProcess(Process.myPid())
        },delayMs,TimeUnit.MILLISECONDS)
    }
}
