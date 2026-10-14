package com.cooper.lithic.data.media

import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/** Exact synthetic bytes supplied with the independent M10 review, never a regenerated fixture. */
internal object M10JpegFixtures {
    const val COMPLETE = "complete.jpg"
    const val BROKEN = "entropy_cut_eoi_retained.jpg"

    fun bytes(name: String): ByteArray {
        val expected = when (name) {
            COMPLETE -> 59_334 to "2ac334a075ff0928c4c08821b82ee804fae44d2282d594e9ebcbfdc5076f6907"
            BROKEN -> 20_194 to "f611d6b0537a66b5f78bd5a51886b70edf1ea9910a6905257ce8faead285452a"
            else -> error("Unknown locked synthetic JPEG")
        }
        val bytes = InstrumentationRegistry.getInstrumentation().context.assets
            .open("m10_jpeg_integrity/$name").use { it.readBytes() }
        check(bytes.size == expected.first && sha(bytes) == expected.second)
        check(bytes.takeLast(2) == listOf(0xff.toByte(), 0xd9.toByte()))
        return bytes
    }

    fun source(name: String): SyntheticImageSource = M10ImageSourceFixtures.source(bytes(name), "image/jpeg").also {
        val resolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
        check(resolver.getType(it.uri) == "image/jpeg")
        check(resolver.openInputStream(it.uri)!!.use { input -> sha(input.readBytes()) } == sha(bytes(name)))
    }

    fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
}

/** Test-only immutable observations survive only to let the host collect actual decoder outcomes. */
internal object M10JpegObservation {
    fun record(name: String, value: JSONObject) {
        require(name.matches(Regex("[a-z0-9_-]+")))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.noBackupFilesDir, "m10-jpeg-observations")
        check(root.isDirectory || root.mkdirs())
        val file = File(root, "$name-${UUID.randomUUID()}.json")
        check(file.createNewFile())
        FileOutputStream(file).use { output ->
            output.write(value.toString(2).toByteArray(Charsets.UTF_8))
            output.flush()
            output.fd.sync()
        }
    }
}
