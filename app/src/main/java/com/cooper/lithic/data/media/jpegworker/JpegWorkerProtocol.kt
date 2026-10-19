package com.cooper.lithic.data.media.jpegworker

import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.util.Log
import com.cooper.lithic.data.media.MediaNativeFlags
import java.io.File
import java.io.IOException
import java.security.MessageDigest

internal enum class JpegWorkerOutcome { SUCCESS, INVALID_IMAGE, UNSUPPORTED, RESOURCE_REJECTED, CANCELLED, TIMEOUT, NATIVE_CRASH, LOAD_ERROR, RESULT_UNKNOWN }

internal data class JpegWorkerRequest(
    val generation: String, val assetId: String, val attemptId: String, val sessionId: String,
    val nonce: String, val sourceSize: Long, val sourceSha256: String,
    val sourceDevice: Long, val sourceInode: Long, val guardDevice: Long, val guardInode: Long,
) {
    fun validate() {
        require(listOf(generation,assetId,attemptId,sessionId,nonce).all { it.length in 1..128 && it.all { c -> c.code in 33..126 } })
        require(sourceSize in 1..(64L*1024*1024) && sourceSha256.matches(Regex("[a-f0-9]{64}")))
        require(sourceDevice>=0 && sourceInode>0 && guardDevice>=0 && guardInode>0)
    }
    fun bundle() = Bundle().apply {
        putInt("version",2)
        putString("generation",generation);putString("asset",assetId);putString("attempt",attemptId)
        putString("session",sessionId);putString("nonce",nonce);putLong("sourceSize",sourceSize)
        putString("sourceSha",sourceSha256);putLong("sourceDev",sourceDevice);putLong("sourceIno",sourceInode)
        putLong("guardDev",guardDevice);putLong("guardIno",guardInode)
    }
    companion object {
        private val keys=setOf("version","generation","asset","attempt","session","nonce","sourceSize","sourceSha","sourceDev","sourceIno","guardDev","guardIno")
        fun read(bundle: Bundle): JpegWorkerRequest {
            require(bundle.keySet()==keys && bundle.getInt("version")==2)
            return JpegWorkerRequest(bundle.getString("generation")!!,bundle.getString("asset")!!,
                bundle.getString("attempt")!!,bundle.getString("session")!!,bundle.getString("nonce")!!,
                bundle.getLong("sourceSize"),bundle.getString("sourceSha")!!,bundle.getLong("sourceDev"),
                bundle.getLong("sourceIno"),bundle.getLong("guardDev"),bundle.getLong("guardIno")).also { it.validate() }
        }
    }
}

internal data class JpegWorkerResult(
    val outcome: JpegWorkerOutcome,
    val workerIdentity: JpegWorkerIdentity? = null,
    val resultCount: Int = 0,
    val sourceFdClosed: Boolean = false,
    val outputFdClosed: Boolean = false,
    val binderDead: Boolean = false,
    val matchingProcessDead: Boolean = false,
    val guardReleased: Boolean = false,
    val strictFinished: Boolean = false,
    val decoderDestroyed: Boolean = false,
    val width: Int = 0, val height: Int = 0, val mime: String = "",
    val outputSha256: String = "", val outputSize: Long = 0,
    val detail: String = "",
    val connectionCleaned: Boolean = false,
    // Host observations, never fabricated Worker RESULT/FD flags. Used only for failed-session recovery.
    val hostFdsClosed: Boolean = false,
    val rpcQuiescent: Boolean = false,
    val originalSessionQuiescent: Boolean = false,
) {
    val consumerEnded: Boolean get() = binderDead && matchingProcessDead && guardReleased
    val mayReleaseSupervisor: Boolean get() = JpegResultWait.mayReleaseAdmission(consumerEnded, connectionCleaned) &&
        (canPrepare || originalSessionQuiescent)
    val canPrepare: Boolean get() = outcome==JpegWorkerOutcome.SUCCESS && resultCount==1 &&
        consumerEnded && connectionCleaned && sourceFdClosed && outputFdClosed && strictFinished && decoderDestroyed
}

internal object JpegWorkerWire {
    const val DESCRIPTOR="com.cooper.lithic.jpeg.stage.v2"
    const val CALLBACK="com.cooper.lithic.jpeg.stage.v2.host"
    const val RESERVE=IBinder.FIRST_CALL_TRANSACTION
    const val DELIVER=IBinder.FIRST_CALL_TRANSACTION+1
    const val RETIRE=IBinder.FIRST_CALL_TRANSACTION+2
    const val REVOKE=IBinder.FIRST_CALL_TRANSACTION+3
    const val HELLO=IBinder.FIRST_CALL_TRANSACTION
    const val RESULT=IBinder.FIRST_CALL_TRANSACTION+1
    const val REQUEST_BUDGET=512L*1024*1024 // Research request accounting; not a device/RSS hard cap.
    const val HARD_DEADLINE_MS=120_000L
    fun policyBundle()=Bundle().apply {
        putString("decoderPolicy",JpegDecoderPolicy.DECODER);putString("normalizationPolicy",JpegDecoderPolicy.NORMALIZATION)
    }
    fun identity(bundle: Bundle)=JpegWorkerIdentity(bundle.getInt("pid"),bundle.getInt("uid"),bundle.getLong("start"))
    fun identityBundle(identity: JpegWorkerIdentity)=Bundle().apply {
        putInt("pid",identity.pid);putInt("uid",identity.uid);putLong("start",identity.startTicks)
    }
    fun retirementReply(phase:String,request:JpegWorkerRequest,worker:JpegWorkerIdentity)=Bundle().apply {
        putBoolean("accepted",true);putString("phase",phase)
        putBundle("request",request.bundle());putBundle("worker",identityBundle(worker))
    }
    fun requireRetirementReply(reply:Bundle,phase:String,request:JpegWorkerRequest,worker:JpegWorkerIdentity) {
        if(reply.keySet()!=setOf("accepted","phase","request","worker"))throw IOException("Retirement acknowledgement shape unknown")
        val claimed=reply.getBundle("worker") ?: throw IOException("Retirement Worker absent")
        if(claimed.keySet()!=setOf("pid","uid","start"))throw IOException("Retirement Worker shape unknown")
        val acknowledged=reply.getBundle("request") ?: throw IOException("Retirement request absent")
        if(!JpegRetirementGate.acknowledgement(phase,reply.getString("phase"),JpegWorkerRequest.read(acknowledged)==request,
                identity(claimed)==worker,reply.getBoolean("accepted")))throw IOException("Retirement acknowledgement conflict")
    }
    /** Bounded lifecycle diagnostics, with no file path, source bytes or business contents. */
    fun event(phase:String,request:JpegWorkerRequest?,worker:JpegWorkerIdentity?) {
        Log.i("LITHIC_JPEG","$phase attempt=${request?.attemptId ?: "NONE"} session=${request?.sessionId ?: "NONE"} "+
            "worker=${worker?.pid ?: 0}/${worker?.uid ?: 0}/${worker?.startTicks ?: 0} at=${android.os.SystemClock.elapsedRealtime()}")
    }
    fun call(endpoint: IBinder, code: Int, write: (Parcel)->Unit = {}): Bundle {
        val data=Parcel.obtain();val reply=Parcel.obtain()
        try {
            data.writeInterfaceToken(DESCRIPTOR);write(data)
            if(!endpoint.transact(code,data,reply,0))throw IOException("Worker transaction rejected")
            reply.readException()
            return reply.readBundle(JpegWorkerWire::class.java.classLoader) ?: throw IOException("Worker reply absent")
        } finally {reply.recycle();data.recycle()}
    }
    fun callback(endpoint: IBinder,code:Int,request:JpegWorkerRequest,identity:JpegWorkerIdentity,extra:Bundle=Bundle()):Boolean {
        val data=Parcel.obtain();val reply=Parcel.obtain()
        try {
            data.writeInterfaceToken(CALLBACK);data.writeBundle(request.bundle());data.writeBundle(identityBundle(identity));data.writeBundle(extra)
            if(!endpoint.transact(code,data,reply,0))return false
            reply.readException();return reply.readInt()==1
        } finally {reply.recycle();data.recycle()}
    }
    fun readFd(parcel: Parcel): ParcelFileDescriptor = ParcelFileDescriptor.CREATOR.createFromParcel(parcel)
    fun open(file:File,flags:Int):ParcelFileDescriptor {
        val raw=Os.open(file.path,flags or OsConstants.O_NOFOLLOW or MediaNativeFlags.CLOSE_ON_EXEC,0)
        try {
            if(!OsConstants.S_ISREG(Os.fstat(raw).st_mode))throw IOException("Worker descriptor is not regular")
            return ParcelFileDescriptor.dup(raw)
        } finally {Os.close(raw)}
    }
    fun fdMode(fd: ParcelFileDescriptor):Int {
        val text=JpegProcessRole.bounded(File("/proc/self/fdinfo/${fd.fd}"),4096)
        val flags=text.lineSequence().filter {it.startsWith("flags:")}.toList().singleOrNull()
            ?.substringAfter(':')?.trim()?.toLongOrNull(8) ?: throw IOException("Descriptor flags unknown")
        return (flags and OsConstants.O_ACCMODE.toLong()).toInt()
    }
    fun close(fd:ParcelFileDescriptor?):Boolean {
        if(fd==null)return true
        val raw=fd.fileDescriptor
        return try {
            fd.close()
            try {Os.fstat(raw);false} catch(e:android.system.ErrnoException) {e.errno==OsConstants.EBADF}
        } catch (_:Exception) {false}
    }
    fun sha(file:File):String {
        val digest=MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input->val bytes=ByteArray(64*1024);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)} }
        return digest.digest().joinToString("") {"%02x".format(it.toInt() and 255)}
    }
}
