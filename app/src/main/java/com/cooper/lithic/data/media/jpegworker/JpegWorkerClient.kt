package com.cooper.lithic.data.media.jpegworker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.system.Os
import android.system.OsConstants
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import java.nio.channels.OverlappingFileLockException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

/** App-lifetime supervisor. Caller cancellation fences eligibility; it never means the consumer has ended. */
internal class JpegWorkerClient(context:Context) {
    private val app=context.applicationContext

    suspend fun execute(
        source:File, output:File, guard:File, request:JpegWorkerRequest,
        beforeDelivery:(JpegWorkerIdentity)->Unit,
        revoke:(String)->Unit,
        abandonedResult:(JpegWorkerResult)->Unit,
    ):JpegWorkerResult = suspendCancellableCoroutine {continuation->
        request.validate()
        val control=Control(revoke)
        val abandoned=JpegAbandonedDelivery<JpegWorkerResult>(safety,{value -> abandonedResult(value)},{error ->
            control.recordError("ABANDONED_RESULT_HOOK_UNKNOWN",error)
            // Technical category only: do not print source paths, private data or image bytes.
            Log.e("LITHIC_JPEG","ABANDONED_RESULT_HOOK_UNKNOWN:${error.javaClass.simpleName}")
        })
        continuation.invokeOnCancellation {control.fail(JpegWorkerOutcome.CANCELLED)}
        supervisor.execute {
            val result=if(!admission.compareAndSet(false,true)) JpegWorkerResult(JpegWorkerOutcome.RESULT_UNKNOWN,detail="NONTERMINAL_CONSUMER_BLOCKS_ADMISSION")
            else try {run(source,output,guard,request,beforeDelivery,control)}
            catch(error:Exception) {control.fail(JpegWorkerOutcome.RESULT_UNKNOWN);JpegWorkerResult(control.failure.get() ?: JpegWorkerOutcome.RESULT_UNKNOWN,detail="SUPERVISION_UNKNOWN:${error.javaClass.simpleName}")}
            // If no conclusive process/lease proof exists admission remains closed. Restart also sees the durable pin.
            if(result.mayReleaseSupervisor)admission.set(false)
            if(continuation.isActive)continuation.resume(result) {_,value,_ -> abandoned.abandon(value)}
            else abandoned.abandon(result)
        }
    }

    private class Control(private val durableRevoke:(String)->Unit) {
        val failure=AtomicReference<JpegWorkerOutcome?>()
        val gate=AtomicReference<JpegSessionFence?>()
        val endpoint=AtomicReference<IBinder?>()
        val request=AtomicReference<JpegWorkerRequest?>()
        val revokeSucceeded=AtomicBoolean(false)
        val firstError=AtomicReference<String?>()
        fun recordCode(code:String) {firstError.compareAndSet(null,code)}
        fun recordError(label:String,error:Throwable) {firstError.compareAndSet(null,"$label:${error.javaClass.simpleName}")}
        fun fail(reason:JpegWorkerOutcome) {
            if(!failure.compareAndSet(null,reason))return
            gate.get()?.fail(reason.name) // Short memory fence before any disk, Binder, wait or UI work.
            safety.execute {
                try {
                    durableRevoke(reason.name);revokeSucceeded.set(true)
                    val peer=endpoint.get();val identity=request.get()
                    if(peer!=null&&identity!=null)try {JpegWorkerWire.call(peer,JpegWorkerWire.REVOKE){it.writeBundle(identity.bundle())}}
                    catch(error:Exception) {recordError("REVOKE_RPC_UNKNOWN",error)}
                } catch(error:Exception) {recordError("PERSISTENT_REVOKE_UNKNOWN",error)}
            }
        }
    }

    private fun run(source:File,output:File,guard:File,request:JpegWorkerRequest,beforeDelivery:(JpegWorkerIdentity)->Unit,control:Control):JpegWorkerResult {
        if(JpegProcessRole.role(app.packageName)!=JpegProcessRole.Role.MAIN)throw IOException("Only verified main process may supervise")
        val gate=JpegSessionFence(request.attemptId,request.sessionId,request.nonce,Process.myPid(),Process.myUid())
        control.gate.set(gate);control.request.set(request)
        control.failure.get()?.let {gate.fail(it.name)}
        val born=SystemClock.elapsedRealtime()
        val connected=CountDownLatch(1);val gotResult=CountDownLatch(1);val died=CountDownLatch(1)
        val endpoint=AtomicReference<IBinder?>();val worker=AtomicReference<JpegWorkerIdentity?>();val result=AtomicReference<Bundle?>()
        val binding=JpegBindingDuty();val bound=AtomicBoolean(false)
        val unbound=AtomicBoolean(false);val bindingError=AtomicReference<String?>()
        val dataClosed=AtomicBoolean(false)
        val main=Handler(Looper.getMainLooper())
        val death=IBinder.DeathRecipient {gate.binderDied();died.countDown()}
        val callback=object:Binder() {
            override fun onTransact(code:Int,data:Parcel,reply:Parcel?,flags:Int):Boolean {
                if(code==INTERFACE_TRANSACTION){reply?.writeString(JpegWorkerWire.CALLBACK);return true}
                if(code!=JpegWorkerWire.HELLO&&code!=JpegWorkerWire.RESULT)return super.onTransact(code,data,reply,flags)
                var accepted=false
                try {
                    data.enforceInterface(JpegWorkerWire.CALLBACK)
                    val actualRequest=JpegWorkerRequest.read(data.readBundle(javaClass.classLoader) ?: throw IOException("Missing callback identity"))
                    val identityBundle=data.readBundle(javaClass.classLoader) ?: throw IOException("Missing Worker identity")
                    if(identityBundle.keySet()!=setOf("pid","uid","start"))throw IOException("Worker identity shape")
                    val claimed=JpegWorkerWire.identity(identityBundle)
                    val extra=data.readBundle(javaClass.classLoader) ?: throw IOException("Missing callback result")
                    if(actualRequest!=request||Binder.getCallingUid()!=Process.myUid()||Binder.getCallingPid()!=claimed.pid||claimed.uid!=Process.myUid())throw IOException("Binder identity conflict")
                    if(code==JpegWorkerWire.HELLO) {
                        val os=JpegProcessRole.identity(claimed.pid)
                        if(os!=claimed||JpegProcessRole.readCmdline(File("/proc/${claimed.pid}/cmdline"))!="${app.packageName}:jpeg_worker"||
                            extra.keySet()!=setOf("decoderPolicy","normalizationPolicy")||
                            !JpegDecoderPolicy.matches(extra.getString("decoderPolicy"),extra.getString("normalizationPolicy")))throw IOException("OS Worker identity or policy conflict")
                        accepted=gate.hello(request.attemptId,request.sessionId,request.nonce,claimed.uid,claimed.pid,claimed.startTicks)
                        if(accepted)worker.set(claimed)
                    } else {
                        if(extra.keySet()!=setOf("outcome","detail","sourceClosed","outputClosed","strictFinished","decoderDestroyed","width","height","outputSize","outputSha","decoderPolicy","normalizationPolicy")||
                            !JpegDecoderPolicy.matches(extra.getString("decoderPolicy"),extra.getString("normalizationPolicy")))throw IOException("Result shape or policy unknown")
                        val status=JpegWorkerOutcome.valueOf(extra.getString("outcome") ?: "")
                        if(status==JpegWorkerOutcome.SUCCESS && (extra.getInt("width") !in 1..1600 || extra.getInt("height") !in 1..1600 ||
                            extra.getLong("outputSize") !in 1..2_097_152L || extra.getString("outputSha")?.matches(Regex("[a-f0-9]{64}"))!=true))throw IOException("Output metadata rejected")
                        accepted=gate.result(request.attemptId,request.sessionId,request.nonce,claimed.pid,claimed.startTicks,
                            status==JpegWorkerOutcome.SUCCESS,extra.getBoolean("sourceClosed"),extra.getBoolean("outputClosed"),extra.getBoolean("strictFinished"),extra.getBoolean("decoderDestroyed"))
                        if(accepted){result.set(extra);gotResult.countDown()}
                    }
                } catch(error:Exception) {control.recordError("CALLBACK_REJECTED",error);control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)}
                reply?.writeNoException();reply?.writeInt(if(accepted)1 else 0)
                return true
            }
        }
        val connection=object:ServiceConnection {
            override fun onServiceConnected(name:ComponentName,binder:IBinder) {
                if(name!=ComponentName(app,JpegWorkerService::class.java)||control.failure.get()!=null) {
                    control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)
                } else {
                    if(!endpoint.compareAndSet(null,binder))control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)
                    else try {binder.linkToDeath(death,0);control.endpoint.set(binder)}catch(error:Exception){control.recordError("DEATH_LINK_UNKNOWN",error);control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)}
                }
                connected.countDown()
            }
            override fun onServiceDisconnected(name:ComponentName) {connected.countDown()} // Not Binder death or OS-death evidence.
            override fun onNullBinding(name:ComponentName){bindingError.set("NULL_BINDING");connected.countDown()}
            override fun onBindingDied(name:ComponentName){bindingError.set("BINDING_DIED");connected.countDown()}
        }
        fun cleanupConnection():Boolean {
            if(binding.cleaned()){unbound.set(true);return true}
            val cleaned=CountDownLatch(1)
            if(main.post {
                if(binding.takeCleanup())try {app.unbindService(connection);binding.cleanupSucceeded()}
                catch(error:Exception){binding.cleanupFailed(error.javaClass.simpleName);bindingError.set("UNBIND_UNKNOWN:${error.javaClass.simpleName}")}
                unbound.set(binding.cleaned());cleaned.countDown()
            })cleaned.await(5,TimeUnit.SECONDS)
            return unbound.get()
        }
        val timeout=watchdog.schedule({
            if(died.count==0L&&gotResult.count!=0L) {
                control.recordCode("PROCESS_DIED_WITHOUT_RESULT");control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)
            } else control.fail(JpegWorkerOutcome.TIMEOUT)
        },JpegWorkerWire.HARD_DEADLINE_MS,TimeUnit.MILLISECONDS)
        var hostSource:ParcelFileDescriptor?=null;var hostOutput:ParcelFileDescriptor?=null;var hostGuard:ParcelFileDescriptor?=null
        var normalRetire=false
        try {
            if(!main.post {
                if(control.failure.get()!=null){connected.countDown();return@post}
                if(!binding.begin()){connected.countDown();return@post}
                try {bound.set(app.bindService(Intent(app,JpegWorkerService::class.java),connection,Context.BIND_AUTO_CREATE));binding.returned(bound.get());if(!bound.get())bindingError.set("BIND_FALSE")}
                catch(error:Exception){binding.threw();bindingError.set("BIND_EXCEPTION:${error.javaClass.simpleName}")}
                finally {if(!bound.get())connected.countDown()}
            })throw IOException("UI bind dispatch failed")
            if(!connected.await(10,TimeUnit.SECONDS))throw java.util.concurrent.TimeoutException("Bind queue/call/connect deadline")
            if(control.failure.get()!=null||bindingError.get()!=null)throw IOException(bindingError.get() ?: "Invocation fenced")
            val peer=endpoint.get() ?: throw IOException("Connected endpoint absent")
            boundedRpc {JpegWorkerWire.call(peer,JpegWorkerWire.RESERVE){it.writeBundle(request.bundle());it.writeStrongBinder(callback)}}
            val identity=worker.get() ?: throw IOException("Synchronous hello absent")
            if(!gate.isAuthorized())throw IOException("Hello not authorized")
            hostSource=JpegWorkerWire.open(source,OsConstants.O_RDONLY)
            hostOutput=JpegWorkerWire.open(output,OsConstants.O_RDWR)
            hostGuard=JpegWorkerWire.open(guard,OsConstants.O_RDWR)
            val src=Os.fstat(hostSource.fileDescriptor);val out=Os.fstat(hostOutput.fileDescriptor);val held=Os.fstat(hostGuard.fileDescriptor)
            if(src.st_dev!=request.sourceDevice||src.st_ino!=request.sourceInode||src.st_size!=request.sourceSize||held.st_dev!=request.guardDevice||held.st_ino!=request.guardInode||held.st_size!=0L||out.st_size!=0L)throw IOException("Host FD identity mismatch")
            beforeDelivery(identity) // Synchronous durable pin success is mandatory before beginDelivery/FD IPC.
            if(control.failure.get()!=null||!gate.beginDelivery())throw IOException("Delivery fenced")
            val stableSource=hostSource;val stableOutput=hostOutput;val stableGuard=hostGuard
            boundedRpc {JpegWorkerWire.call(peer,JpegWorkerWire.DELIVER){parcel->
                parcel.writeBundle(request.bundle());parcel.writeLong(out.st_dev);parcel.writeLong(out.st_ino)
                stableSource.writeToParcel(parcel,0);stableOutput.writeToParcel(parcel,0);stableGuard.writeToParcel(parcel,0)
            }}
            val closedSource=JpegWorkerWire.close(hostSource);hostSource=null
            val closedOutput=JpegWorkerWire.close(hostOutput);hostOutput=null
            val closedGuard=JpegWorkerWire.close(hostGuard);hostGuard=null
            dataClosed.set(closedSource&&closedOutput&&closedGuard)
            if(!dataClosed.get())throw IOException("Host descriptor close unknown")
            val wait=JpegResultWait(Math.addExact(born,JpegWorkerWire.HARD_DEADLINE_MS),500)
            while(true) {
                if(control.failure.get()!=null)throw IOException("Invocation fenced while waiting")
                when(wait.observe(SystemClock.elapsedRealtime(),result.get()!=null,died.count==0L)) {
                    JpegResultWait.Decision.RESULT_READY -> break
                    JpegResultWait.Decision.DEAD_WITHOUT_RESULT -> {
                        control.recordCode("PROCESS_DIED_WITHOUT_RESULT");control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)
                        throw IOException("Matching Binder died without structured result")
                    }
                    JpegResultWait.Decision.TIMEOUT -> throw java.util.concurrent.TimeoutException("Compute result deadline")
                    JpegResultWait.Decision.CLOCK_UNKNOWN -> throw IOException("Monotonic wait state unknown")
                    JpegResultWait.Decision.WAIT -> gotResult.await(25,TimeUnit.MILLISECONDS)
                    else -> throw IOException("Wait decision unknown")
                }
            }
            val reply=result.get() ?: throw IOException("Result absent")
            // Remove BIND_AUTO_CREATE responsibility before a known worker self-exits. The retained
            // endpoint remains authenticated, while unbinding supplies no process/FD death proof.
            if(!cleanupConnection())throw IOException("Connection cleanup unknown before retirement")
            if(!JpegRetirementGate.mayRequest(gate.acceptedResults()==1,dataClosed.get(),
                    reply.getBoolean("sourceClosed")&&reply.getBoolean("outputClosed"),unbound.get(),control.failure.get()!=null))
                throw IOException("Normal retirement not authorized")
            boundedRpc {JpegWorkerWire.call(peer,JpegWorkerWire.RETIRE){it.writeBundle(request.bundle())}}
            normalRetire=true
        } catch(error:java.util.concurrent.TimeoutException){control.recordError("PHASE_TIMEOUT",error);control.fail(JpegWorkerOutcome.TIMEOUT)}
        catch(error:Exception) {control.recordError("INVOCATION_FAILED",error);control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)}
        finally {
            val closedSource=JpegWorkerWire.close(hostSource);val closedOutput=JpegWorkerWire.close(hostOutput);val closedGuard=JpegWorkerWire.close(hostGuard)
            if(!closedSource||!closedOutput||!closedGuard)control.fail(JpegWorkerOutcome.RESULT_UNKNOWN)
            cleanupConnection() // Idempotent ownership cleanup also covers every failing bind/delivery path.
        }
        // Unbind cannot prove a consumer stopped. Retain death linkage while obtaining both OS and guard proof.
        died.await(10,TimeUnit.SECONDS)
        val identity=worker.get()
        var processGone=false;var guardReleased=false
        if(identity!=null&&died.count==0L) {
            repeat(20) {
                if(!processGone)try {processGone=JpegProcessRole.instanceAbsent(identity)}catch(error:Exception) {control.recordError("OS_DEATH_UNKNOWN",error)}
                if(!processGone)Thread.sleep(25)
            }
            if(processGone)guardReleased=try {
                JpegWorkerWire.open(guard,OsConstants.O_RDWR).use {fd->
                    val st=Os.fstat(fd.fileDescriptor)
                    if(st.st_dev!=request.guardDevice||st.st_ino!=request.guardInode||st.st_size!=0L)false
                    else ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.dup(fd.fileDescriptor)).use {stream->
                        val lock=stream.channel.tryLock();if(lock==null)false else {lock.release();true}
                    }
                }
            } catch(error:Exception) {control.recordError("GUARD_UNKNOWN",error);false}
        }
        gate.ended(died.count==0L,processGone,guardReleased)
        cleanupConnection()
        timeout.cancel(false)
        val envelope=result.get()
        val safeSuccess=gate.canPrepare()&&JpegRetirementGate.mayPrepare(normalRetire,died.count==0L,processGone,guardReleased)&&
            unbound.get()&&dataClosed.get()&&bindingError.get()==null
        val outcome=control.failure.get() ?: if(safeSuccess)JpegWorkerOutcome.SUCCESS else if(envelope!=null&&gate.consumerEnded()&&unbound.get())
            JpegWorkerOutcome.valueOf(envelope.getString("outcome")!!) else JpegWorkerOutcome.RESULT_UNKNOWN
        // A failed safety check must never retain the wire's SUCCESS.
        val finalOutcome=if(outcome==JpegWorkerOutcome.SUCCESS&&!safeSuccess)JpegWorkerOutcome.RESULT_UNKNOWN else outcome
        if(finalOutcome!=JpegWorkerOutcome.SUCCESS&&control.failure.get()==null)control.fail(finalOutcome)
        return JpegWorkerResult(finalOutcome,identity,gate.acceptedResults(),
            envelope?.getBoolean("sourceClosed")==true,envelope?.getBoolean("outputClosed")==true,
            died.count==0L,processGone,guardReleased,envelope?.getBoolean("strictFinished")==true,
            envelope?.getBoolean("decoderDestroyed")==true,envelope?.getInt("width") ?: 0,envelope?.getInt("height") ?: 0,
            if((envelope?.getInt("width") ?: 0)>0)"image/jpeg" else "",envelope?.getString("outputSha") ?: "",
            envelope?.getLong("outputSize") ?: 0, control.firstError.get() ?: bindingError.get() ?: envelope?.getString("detail") ?: "NO_STRUCTURED_RESULT",
            connectionCleaned=unbound.get())
    }
    private fun <T> boundedRpc(action:()->T):T {
        // Waiting thread can time out while the IPC thread remains blocked; no Future.cancel death inference.
        val future=ipc.submit<T>{action()}
        return future.get(10,TimeUnit.SECONDS)
    }
    companion object {
        private val admission=AtomicBoolean(false)
        private val supervisor=Executors.newSingleThreadExecutor {r->Thread(r,"jpeg-main-supervisor")}
        private val ipc=Executors.newSingleThreadExecutor {r->Thread(r,"jpeg-main-ipc")}
        private val safety=Executors.newSingleThreadExecutor {r->Thread(r,"jpeg-main-revoke")}
        private val watchdog=Executors.newSingleThreadScheduledExecutor {r->Thread(r,"jpeg-main-watchdog")}
    }
}
