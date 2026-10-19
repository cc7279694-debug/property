package com.cooper.lithic.data.media.jpegworker

import android.content.Context
import com.cooper.lithic.data.media.jpegstage.JpegRecoveryVerifier
import com.cooper.lithic.data.media.jpegstage.JpegStageV2
import java.io.IOException

/** Only proc identity/boot metadata is read; this adapter never kills, binds or cleans files. */
internal class AndroidJpegRecoveryProbe(private val context: Context) : JpegRecoveryVerifier.Probe {
    override fun currentSupervisor(): JpegStageV2.SupervisorIdentity {
        if (!isMainProcess()) throw IOException("Only the verified main process can recover JPEG stages")
        val before = bootId()
        val identity = JpegProcessRole.identity()
        if (bootId() != before) throw IOException("Boot epoch observation changed")
        return JpegStageV2.SupervisorIdentity(before, identity.pid, identity.uid, identity.startTicks)
    }
    override fun bootId(): String = JpegBootIdentity.current(context)
    override fun isMainProcess(): Boolean = JpegProcessRole.role(context.packageName) == JpegProcessRole.Role.MAIN
    override fun instanceEnded(identity: JpegRecoveryVerifier.ProcessRef): Boolean =
        JpegProcessRole.instanceAbsent(JpegWorkerIdentity(identity.pid, identity.uid, identity.startTicks))
}
