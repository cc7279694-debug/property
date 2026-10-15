package com.cooper.lithic.feature.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.media.MediaRepository
import kotlinx.coroutines.CancellationException

private sealed interface CoverReadState {
    data object Reading : CoverReadState
    data object Failed : CoverReadState
    data class Ready(val image: ImageBitmap) : CoverReadState
}

private val LocalCoverPreviewLoader = staticCompositionLocalOf<CoverPreviewLoader?> { null }

/** One bounded preview owner for the current navigation UI and live media facade. */
@Composable
internal fun CoverPreviewProvider(repository: MediaRepository?, content: @Composable () -> Unit) {
    val loader = remember(CoverRepositoryIdentity(repository)) { repository?.let { CoverPreviewLoader(it) } }
    DisposableEffect(loader) { onDispose { loader?.close() } }
    CompositionLocalProvider(LocalCoverPreviewLoader provides loader, content = content)
}

@Composable
private fun rememberCoverLoader(repository: MediaRepository?): CoverPreviewLoader? {
    if (repository == null) return null
    LocalCoverPreviewLoader.current?.let { if (it.repository === repository) return it }
    // Standalone screen hosts keep the original public screen signatures usable.
    val loader = remember(CoverRepositoryIdentity(repository)) { CoverPreviewLoader(repository) }
    DisposableEffect(loader) { onDispose { loader.close() } }
    return loader
}

@Composable
private fun coverRead(assetId: String, repository: MediaRepository?, size: CoverPreviewSize): CoverReadState {
    val loader = rememberCoverLoader(repository) ?: return CoverReadState.Failed
    // A changed facade/identity/size starts empty in this very composition, before effects run.
    val state = remember(loader, assetId, size) { mutableStateOf<CoverReadState>(CoverReadState.Reading) }
    LaunchedEffect(loader, assetId, size) {
        state.value = try {
            when (val loaded = loader.load(assetId, size)) {
                CoverPreviewResult.Failed -> CoverReadState.Failed
                is CoverPreviewResult.Ready -> CoverReadState.Ready(loaded.bitmap.asImageBitmap())
            }
        } catch (cancelled: CancellationException) { throw cancelled }
    }
    return state.value
}

/** A real list cover; a missing reference and an unreadable referenced file stay distinct. */
@Composable
internal fun CoverThumbnail(assetId: String?, repository: MediaRepository?, prefix: String, modifier: Modifier = Modifier) {
    val pixels = with(LocalDensity.current) { 64.dp.roundToPx() }.coerceIn(1, 1600)
    val read = assetId?.let { coverRead(it, repository, CoverPreviewSize(pixels, pixels)) }
    Surface(modifier.size(64.dp).clip(MaterialTheme.shapes.small), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (read) {
                null -> LithicIcon(LithicIcon.Image, "未设置封面", Modifier.size(26.dp).testTag("$prefix-cover-empty"))
                CoverReadState.Reading -> CircularProgressIndicator(Modifier.size(24.dp).testTag("$prefix-cover-loading")
                    .semantics { contentDescription = "正在读取本地封面" }, strokeWidth = 2.dp)
                // A compact error shape stays legible at large font sizes; detail keeps the full text.
                CoverReadState.Failed -> LithicIcon(LithicIcon.ImageError, "封面无法读取，进入详情可重新选择或移除",
                    Modifier.size(32.dp).testTag("$prefix-cover-error"), tint = MaterialTheme.colorScheme.error)
                is CoverReadState.Ready -> Image(read.image, "已保存的本地封面", Modifier.fillMaxSize().testTag("$prefix-cover-image"), contentScale = ContentScale.Crop)
            }
        }
    }
}

/** Only verified current-generation bytes reach the decoder, never a source URI or temp path. */
@Composable
internal fun CoverDisplay(assetId: String?, repository: MediaRepository?, prefix: String, modifier: Modifier = Modifier,
    previewAspectRatio: Float = 16f / 9f) {
    require(previewAspectRatio.isFinite() && previewAspectRatio > 0f)
    if (assetId == null) {
        Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            Text("尚未设置封面", Modifier.padding(20.dp).testTag("$prefix-cover-empty"), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    var enlarged by remember(CoverRepositoryIdentity(repository), assetId) { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val width = if (maxWidth.value.isFinite()) with(density) { maxWidth.roundToPx() }.coerceIn(1, 1600) else 1600
        val read = coverRead(assetId, repository, CoverPreviewSize(width, (width / previewAspectRatio).toInt().coerceIn(1, 1600)))
        when (read) {
            CoverReadState.Reading -> Text("正在读取本地封面", Modifier.testTag("$prefix-cover-loading"), style = MaterialTheme.typography.bodyMedium)
            CoverReadState.Failed -> Text("此封面无法读取或校验。记录仍保留，可重新选择或移除封面。",
                Modifier.testTag("$prefix-cover-error"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            is CoverReadState.Ready -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(read.image, contentDescription = "已保存的本地封面", modifier = Modifier.fillMaxWidth().aspectRatio(previewAspectRatio)
                    .clip(MaterialTheme.shapes.small).clickable { enlarged = true }.testTag("$prefix-cover-image"), contentScale = ContentScale.Crop)
                TextButton({ enlarged = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-cover-view")) { Text("查看封面") }
            }
        }
    }
    if (enlarged) CoverViewer(assetId, repository, prefix) { enlarged = false }
}

@Composable
private fun CoverViewer(assetId: String, repository: MediaRepository?, prefix: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val density = LocalDensity.current
                    val width = if (maxWidth.value.isFinite()) with(density) { maxWidth.roundToPx() }.coerceIn(1, 1600) else 1600
                    val height = with(density) { 480.dp.roundToPx() }.coerceIn(1, 1600)
                    when (val read = coverRead(assetId, repository, CoverPreviewSize(width, height))) {
                        CoverReadState.Reading -> Text("正在读取本地封面", Modifier.testTag("$prefix-cover-view-loading"))
                        CoverReadState.Failed -> Text("封面无法读取，可关闭后重新选择或移除。", Modifier.testTag("$prefix-cover-view-error"), color = MaterialTheme.colorScheme.error)
                        is CoverReadState.Ready -> Image(read.image, "已保存的本地封面", Modifier.fillMaxWidth().heightIn(max = 480.dp)
                            .testTag("$prefix-cover-view-image"), contentScale = ContentScale.Fit)
                    }
                }
                TextButton(onClose, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-cover-close")) { Text("关闭封面") }
            }
        }
    }
}

@Composable
internal fun CoverSection(assetId: String?, repository: MediaRepository?, prefix: String, onEdit: () -> Unit, enabled: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Text("本地封面", style = MaterialTheme.typography.titleLarge)
        CoverDisplay(assetId, repository, prefix)
        OutlinedButton(onEdit, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-cover-edit"), enabled = enabled) {
            Text(if (assetId == null) "添加封面" else "管理封面", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
