package com.cooper.lithic.data.media

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import java.io.OutputStream
import java.util.UUID

/** Instrumentation utilities run with the target APK's Kotlin runtime. */
data class SyntheticImageSource(val uri: Uri) {
    fun delete(): Boolean = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.delete(uri, null, null) == 1
    fun write(block: (OutputStream) -> Unit) {
        requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.contentResolver.openOutputStream(uri, "w")).use(block)
    }
}

object M10ImageSourceFixtures {
    fun source(bytes: ByteArray, mime: String = "image/png"): SyntheticImageSource {
        val uri = Uri.Builder().scheme("content").authority("com.cooper.lithic.m10.testsource")
            .appendPath("${UUID.randomUUID()}.bin").appendQueryParameter("mime", mime).build()
        return SyntheticImageSource(uri).apply { write { it.write(bytes) } }
    }
}
