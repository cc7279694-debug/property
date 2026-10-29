package com.cooper.lithic.feature.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.data.media.*
import kotlinx.coroutines.*

private val LocalCoverReader = staticCompositionLocalOf<CoverReader?> { null }
private object MissingCoverOwner
private class CoverIdentityKey(private val value: Any?) {
    override fun equals(other: Any?) = other is CoverIdentityKey && value === other.value
    override fun hashCode() = value?.let(System::identityHashCode) ?: 0
}

/** Injection only: this scope never creates or closes a generation, Room database or storage Owner. */
@Composable
internal fun ProvideCoverPreviews(reader: CoverReader?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalCoverReader provides reader, content = content)
}

private sealed interface PreviewImage {
    data object Pending : PreviewImage
    data object Unavailable : PreviewImage
    data class Ready(val bitmap: ImageBitmap) : PreviewImage
}

@Composable
internal fun RecordCover(reference: CoverReference, modifier: Modifier = Modifier, fallback: @Composable () -> Unit) {
    val reader = LocalCoverReader.current
    var measured by remember { mutableStateOf(IntSize.Zero) }
    val target = coverPreviewTarget(measured.width, measured.height)
    val generation = reader?.generationId ?: "unavailable"
    val owner = reader?.ownerKey ?: MissingCoverOwner
    // A fresh remembered state replaces the old picture synchronously, before the next effect starts.
    val ticket = remember(CoverIdentityKey(reader), generation, CoverIdentityKey(owner), reference, target) {
        CoverPreviewTicket(generation, owner, reference, target)
    }
    var image by remember(ticket) { mutableStateOf<PreviewImage>(PreviewImage.Pending) }
    var explainFailure by remember(ticket) { mutableStateOf(false) }
    DisposableEffect(ticket) { onDispose { ticket.retire() } }
    LaunchedEffect(ticket) {
        if (reference.assetId == null || target == null) return@LaunchedEffect
        if (reader == null || !reader.currentSafely()) { image = PreviewImage.Unavailable; return@LaunchedEffect }
        try {
            CoverPreviewPipeline.serialized {
                // Keep this owner outside withContext: prompt cancellation must not lose a native bitmap return value.
                val pending = PendingCoverRaster()
                try {
                    withContext(Dispatchers.IO) {
                        currentCoroutineContext().ensureActive()
                        if (!ticket.matches(generation, owner, reference, target, reader.currentSafely())) return@withContext
                        val result = reader.read(reference)
                        currentCoroutineContext().ensureActive()
                        if (result !is CoverReadResult.Ready) return@withContext
                        val verified = result.image
                        require(verified.assetId == reference.assetId && verified.mime in setOf("image/png", "image/jpeg"))
                        val bytes = verified.bytes
                        require(bytes.size in 1 until 2 * 1024 * 1024)
                        val plan = coverSamplePlan(verified.width, verified.height, target)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true; inScaled = false }
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                        require(bounds.outWidth == verified.width && bounds.outHeight == verified.height && bounds.outMimeType == verified.mime)
                        currentCoroutineContext().ensureActive()
                        if (!ticket.matches(generation, owner, reference, target, reader.currentSafely())) return@withContext
                        pending.reservation = CoverPreviewPipeline.pixels.reserve(plan.reservedBytes) ?: return@withContext
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = plan.sampleSize
                            inScaled = false
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                            inPreferredColorSpace = ColorSpace.get(ColorSpace.Named.SRGB)
                        }
                        // This is a thumbnail of registered, verified bytes, never a strict JPEG-import certificate.
                        pending.bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        val bitmap = pending.bitmap ?: return@withContext
                        require(bitmap.width in plan.minimumWidth..plan.maxWidth && bitmap.height in plan.minimumHeight..plan.maxHeight)
                        require(options.outMimeType == verified.mime)
                        require(bitmap.allocationByteCount.toLong() in 1..requireNotNull(pending.reservation).bytes)
                        currentCoroutineContext().ensureActive()
                    }
                    currentCoroutineContext().ensureActive()
                    val bitmap = pending.bitmap
                    val reservation = pending.reservation
                    if (bitmap != null && reservation != null &&
                        ticket.matches(generation, owner, reference, target, reader.currentSafely()) && ticket.attach(reservation)) {
                        image = PreviewImage.Ready(bitmap.asImageBitmap())
                        pending.published = true
                    } else image = PreviewImage.Unavailable
                } finally {
                    withContext(NonCancellable + Dispatchers.IO) { pending.recycleUnpublished() }
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: OutOfMemoryError) { image = PreviewImage.Unavailable }
        catch (_: Exception) { image = PreviewImage.Unavailable }
    }
    Box(modifier.fillMaxSize().onSizeChanged { measured = it }, contentAlignment = Alignment.Center) {
        when {
            reference.assetId == null -> fallback()
            reader == null || !reader.currentSafely() || image is PreviewImage.Unavailable -> {
                Box(Modifier.fillMaxSize().sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .testTag("record-cover-unavailable").semantics { contentDescription = "封面暂不可用" }
                    .clickable(onClickLabel = "查看封面状态", role = Role.Button) { explainFailure = true }, contentAlignment = Alignment.Center) {
                    C2Icon(C2Glyph.ImageError)
                }
            }
            image is PreviewImage.Ready -> Image((image as PreviewImage.Ready).bitmap,
                if (reference.type == CoverOwnerType.PROJECT) "长期项目封面" else "订阅封面",
                Modifier.fillMaxSize().testTag("record-cover-image"), contentScale = ContentScale.Crop)
            else -> C2Icon(C2Glyph.Image, Modifier.testTag("record-cover-loading"), "正在读取封面")
        }
    }
    if (explainFailure) AlertDialog(onDismissRequest = { explainFailure = false },
        title = { Text("封面暂不可用") }, text = { Text("暂时无法读取这张封面。查看封面不会修改记录。") },
        confirmButton = { TextButton(onClick = { explainFailure = false }) { Text("知道了") } })
}

private fun CoverReader.currentSafely() = try { isCurrent() } catch (_: Exception) { false }

/** Native decoding cannot be forcibly interrupted. Structured IO keeps the global slot until it returns.
 * Only an unpublished bitmap is recycled here; displayed images are left to normal rendering/GC. */
private class PendingCoverRaster {
    var bitmap: Bitmap? = null
    var reservation: CoverPixelReservation? = null
    var published = false
    fun recycleUnpublished() {
        try {
            if (!published) bitmap?.let { if (!it.isRecycled) it.recycle() }
        } finally {
            if (!published) reservation?.close()
            bitmap = null
            reservation = null
        }
    }
}
