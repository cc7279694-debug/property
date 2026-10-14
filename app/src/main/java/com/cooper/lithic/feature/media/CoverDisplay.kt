package com.cooper.lithic.feature.media

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface CoverReadState {
    data object Reading : CoverReadState
    data object Failed : CoverReadState
    data class Ready(val image: ImageBitmap) : CoverReadState
}

/** Only verified current-generation bytes reach the decoder, never a source URI or temp path. */
@Composable
internal fun CoverDisplay(assetId: String?, repository: MediaRepository?, prefix: String, modifier: Modifier = Modifier) {
    if (assetId == null) {
        Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
            Text("尚未设置封面", Modifier.padding(24.dp).testTag("$prefix-cover-empty"), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val read by produceState<CoverReadState>(CoverReadState.Reading, repository, assetId) {
        value = CoverReadState.Reading
        value = try {
            withContext(Dispatchers.IO) {
                val result = repository?.openAsset(assetId)
                if (result !is MediaResult.Success) CoverReadState.Failed
                else {
                    val bytes = result.value.bytes
                    val asset = result.value.asset
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth != asset.width || bounds.outHeight != asset.height ||
                        bounds.outWidth !in 1..1600 || bounds.outHeight !in 1..1600) CoverReadState.Failed
                    else BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { CoverReadState.Ready(it.asImageBitmap()) } ?: CoverReadState.Failed
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { CoverReadState.Failed }
    }
    var enlarged by remember(assetId) { mutableStateOf(false) }
    when (val current = read) {
        CoverReadState.Reading -> Text("正在读取本地封面", modifier.testTag("$prefix-cover-loading"), style = MaterialTheme.typography.bodyMedium)
        CoverReadState.Failed -> Text("此封面无法读取或校验。记录仍保留，可重新选择或移除封面。",
            modifier.testTag("$prefix-cover-error"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
        is CoverReadState.Ready -> {
            Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(current.image, contentDescription = "已保存的本地封面", modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp, max = 256.dp)
                    .clickable { enlarged = true }.testTag("$prefix-cover-image"), contentScale = ContentScale.Fit)
                TextButton({ enlarged = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-cover-view")) { Text("查看封面") }
            }
            if (enlarged) Dialog(onDismissRequest = { enlarged = false }) {
                Surface(shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Image(current.image, "已保存的本地封面", Modifier.fillMaxWidth().heightIn(max = 480.dp), contentScale = ContentScale.Fit)
                        TextButton({ enlarged = false }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("$prefix-cover-close")) { Text("关闭封面") }
                    }
                }
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
