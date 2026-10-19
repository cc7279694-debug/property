package com.cooper.lithic.data.media.jpegworker

import android.content.Context
import android.provider.Settings
import com.cooper.lithic.data.media.jpegstage.JpegRecoveryVerifier
import java.io.IOException

/** Public API24 boot counter. No default zero and no global setting is written. */
internal object JpegBootIdentity {
    fun current(context: Context): String = try {
        JpegRecoveryVerifier.bootId(Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT))
    } catch (error: Exception) {
        throw IOException("A verified boot epoch is unavailable", error)
    }
}
