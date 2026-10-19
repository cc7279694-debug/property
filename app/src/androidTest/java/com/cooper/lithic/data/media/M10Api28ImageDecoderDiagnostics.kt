package com.cooper.lithic.data.media

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import java.nio.ByteBuffer
import org.json.JSONArray
import org.json.JSONObject

/**
 * API28-only diagnostics live outside the JUnit test class so API26's declared-method
 * discovery never has to resolve ImageDecoder types in its synthetic method signatures.
 * The caller must retain the SDK guard; this boundary exposes only API26-available types.
 */
@TargetApi(28)
internal object M10Api28ImageDecoderDiagnostics {
    fun report(bytes: ByteArray, bitmapOutcome: (Bitmap?) -> JSONObject): JSONObject {
        val partialErrors = JSONArray()
        return try {
            val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes))) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.setOnPartialImageListener { failure ->
                    partialErrors.put(JSONObject().put("error", failure.error).put("message", failure.message))
                    false
                }
            }
            val outcome = bitmapOutcome(decoded).put("partialErrors", partialErrors)
            decoded.recycle()
            outcome
        } catch (failure: ImageDecoder.DecodeException) {
            JSONObject().put("result", "FAILURE").put("error", failure.error)
                .put("message", failure.message).put("partialErrors", partialErrors)
        }
    }
}
