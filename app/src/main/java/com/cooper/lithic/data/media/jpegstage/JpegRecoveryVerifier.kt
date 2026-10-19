package com.cooper.lithic.data.media.jpegstage

import java.io.IOException
import java.util.Locale

/** Trusted OS adapter facts are independently checked; Stage holds its real guard lease. */
internal class JpegRecoveryVerifier(private val probe: Probe) {
    data class ProcessRef(val pid: Int, val uid: Int, val startTicks: Long)
    interface Probe {
        fun currentSupervisor(): JpegStageV2.SupervisorIdentity
        fun bootId(): String
        fun isMainProcess(): Boolean
        fun instanceEnded(identity: ProcessRef): Boolean
    }
    data class LiveQuiescence(
        val owner: JpegStageV2.Owner, val supervisor: JpegStageV2.SupervisorIdentity,
        val worker: JpegStageV2.WorkerIdentity, val hostFdsClosed: Boolean,
        val rpcQuiescent: Boolean, val originalSessionQuiescent: Boolean,
        val connectionCleaned: Boolean, val binderDead: Boolean,
        val matchingProcessDead: Boolean, val guardReleased: Boolean,
    )
    fun verify(request: JpegStageV2.RecoveryRequest, live: LiveQuiescence? = null): JpegStageV2.ClosureProof {
        try {
            val observedBoot = probe.bootId()
            val current = probe.currentSupervisor()
            val original = request.supervisor
            val worker = request.worker ?: reject("No accurate old consumer identity")
            if (!probe.isMainProcess() || current.bootId != observedBoot ||
                current.pid <= 0 || current.uid < 0 || current.startTicks <= 0 ||
                original.pid <= 0 || original.startTicks <= 0 || original.uid != current.uid ||
                worker.pid <= 0 || worker.startTicks <= 0 || worker.uid != current.uid || worker.pid == original.pid) {
                reject("Supervisor or consumer identity is not independently verified")
            }
            val nowCount = bootCount(observedBoot)
            val oldCount = bootCount(original.bootId)
            val closure = if (nowCount != oldCount) {
                if (nowCount <= oldCount) reject("Boot epoch decreased or is not comparable")
                // BOOT_COUNT belongs to this installation's OS, not an mtime or wall clock.
                // Both old process groups and their Binder/FD actors ended at the newer boot.
                JpegStageV2.HostClosure.ORIGINAL_INSTANCE_ENDED
            } else if (current == original) {
                if (live == null || live.owner != request.owner || live.supervisor != original ||
                    live.worker != worker || !live.hostFdsClosed || !live.rpcQuiescent ||
                    !live.originalSessionQuiescent || !live.connectionCleaned || !live.binderDead ||
                    !live.matchingProcessDead || !live.guardReleased ||
                    !probe.instanceEnded(ProcessRef(worker.pid, worker.uid, worker.startTicks))) {
                    reject("Original live Session still has unproved actors")
                }
                JpegStageV2.HostClosure.SAME_SESSION_QUIESCENT
            } else {
                if (!probe.instanceEnded(ProcessRef(original.pid, original.uid, original.startTicks)) ||
                    !probe.instanceEnded(ProcessRef(worker.pid, worker.uid, worker.startTicks))) {
                    reject("Both exact old process instances must have ended")
                }
                JpegStageV2.HostClosure.ORIGINAL_INSTANCE_ENDED
            }
            if (probe.bootId() != observedBoot || probe.currentSupervisor() != current || !probe.isMainProcess()) {
                reject("Recovery observation changed while verifying")
            }
            // The caller is still holding the real namespace and consumer.guard lease.
            // No structured decode result or source/output-close callback is invented.
            return JpegStageV2.ClosureProof(request, observedBoot, closure, workerEnded = true,
                hostFdsClosed = true, noFutureRpc = true, connectionCleaned = true,
                noConsumerActorsEnded = false)
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("Recovery OS observation is unknown", error)
        }
    }
    companion object {
        private const val PREFIX = "6c697468-6963-4000-8000-"
        fun bootId(count: Int): String {
            if (count < 0) reject("Boot counter unavailable")
            return PREFIX + String.format(Locale.ROOT, "%012x", count)
        }
        fun bootCount(value: String): Int {
            if (!value.matches(Regex(PREFIX + "[0-9a-f]{12}"))) reject("Unknown boot epoch format")
            val count = value.substring(PREFIX.length).toLongOrNull(16) ?: reject("Invalid boot counter")
            if (count !in 0..Int.MAX_VALUE.toLong()) reject("Boot counter overflow")
            if (bootId(count.toInt()) != value) reject("Noncanonical boot epoch")
            return count.toInt()
        }
        private fun reject(message: String): Nothing = throw IOException(message)
    }
}
