package com.cooper.lithic.feature.media

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.cooper.lithic.data.media.photolite.PhotoLiteCoverChange
import com.cooper.lithic.core.design.LithicTextButton as TextButton

/** System Picker only: never accesses a gallery path, requests broad media permission or persists a URI. */
@Composable
internal fun PhotoLiteCoverPanel(controller: PhotoLiteCoverController, state: PhotoLiteCoverUiState) {
    // A replacement owner receives a different ActivityResult registration, never the old chooser's result.
    key(controller.sessionKey) { PhotoLiteCoverPanelSession(controller, state) }
}

@Composable
private fun PhotoLiteCoverPanelSession(controller: PhotoLiteCoverController, state: PhotoLiteCoverUiState) {
    // Only a nonce survives Activity recreation. A new generation/controller rejects that old nonce.
    var launchedTicket by rememberSaveable(controller.sessionKey) { mutableStateOf<String?>(null) }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val ticket = launchedTicket
        launchedTicket = null
        controller.pickerResult(ticket, uri)
    }
    LaunchedEffect(state.pickerTicket, controller) {
        val ticket = state.pickerTicket ?: return@LaunchedEffect
        if (!controller.pickerWillLaunch(ticket)) return@LaunchedEffect
        if (launchedTicket != null) {
            // Android's callback has no request nonce: do not overwrite the outstanding launch's ticket.
            controller.pickerLaunchFailed(ticket, previousChoicePending = true)
            return@LaunchedEffect
        }
        launchedTicket = ticket
        try {
            chooser.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            launchedTicket = null
            controller.pickerLaunchFailed(ticket)
        }
    }
    PhotoLiteCoverDialog(state, controller::confirm, controller::discard)
}

/** Production dialog also used by compile-only semantic assertions with synthetic state. */
@Composable
internal fun PhotoLiteCoverDialog(state: PhotoLiteCoverUiState, onConfirm: () -> Unit, onCancel: () -> Unit) {
    if (state.phase in setOf(PhotoLiteCoverPhase.IDLE, PhotoLiteCoverPhase.CHOOSING)) return
    val busy = state.phase in setOf(PhotoLiteCoverPhase.PREPARING, PhotoLiteCoverPhase.LOADING, PhotoLiteCoverPhase.SAVING)
    val preview = state.preview
    val removing = state.change == PhotoLiteCoverChange.REMOVE
    val replacing = state.change == PhotoLiteCoverChange.REPLACE
    AlertDialog(
        onDismissRequest = { if (!state.blocksLeaving) onCancel() },
        modifier = Modifier.testTag("photo-lite-dialog"),
        properties = DialogProperties(dismissOnBackPress = !state.blocksLeaving, dismissOnClickOutside = false),
        title = { Text(when (state.phase) {
            PhotoLiteCoverPhase.PREVIEW -> if (replacing) "确认更换照片" else "确认物品照片"
            PhotoLiteCoverPhase.REMOVE_CONFIRM -> "取消封面显示？"
            PhotoLiteCoverPhase.SAVING -> if (removing) "正在取消封面显示" else if (replacing) "正在更换封面" else "正在保存封面"
            PhotoLiteCoverPhase.SUCCESS -> if (removing) "已取消封面显示" else if (replacing) "封面已更换" else "封面已保存"
            PhotoLiteCoverPhase.UNKNOWN -> "请保留当前现场"
            PhotoLiteCoverPhase.REJECTED -> "封面未更改"
            else -> "正在准备照片"
        }) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.phase == PhotoLiteCoverPhase.PREVIEW && preview != null) {
                    val bitmap = remember(preview) {
                        // A published raster must not be recycled while a rendering frame may still use it.
                        preview.markPublished()
                        preview.bitmap.asImageBitmap()
                    }
                    Image(bitmap, "待确认的物品照片", Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp)
                        .testTag("photo-lite-preview"), contentScale = ContentScale.Fit)
                    Text(if (replacing) "确认后更换当前物品的封面；取消会保留原封面。旧照片不会被删除，仍会随完整备份保留。"
                        else "确认后仅保存这张本地封面；取消不会更改物品。")
                }
                if (state.phase == PhotoLiteCoverPhase.REMOVE_CONFIRM) Text(
                    "只取消当前物品的封面显示，名称、金额和日期不变。这里不会彻底删除旧照片；已有文件可能继续占用空间，完整备份仍会包含并校验旧照片。",
                    Modifier.testTag("photo-lite-removal-warning"))
                if (busy) CircularProgressIndicator(Modifier.testTag("photo-lite-progress"))
                state.message?.let { Text(it, Modifier.testTag("photo-lite-feedback")) }
                if (state.phase == PhotoLiteCoverPhase.SAVING) Text("请等待明确结果，不要重复提交。")
                if (state.phase == PhotoLiteCoverPhase.PREPARING) Text("正在核对当前物品与账本。")
                if (state.phase == PhotoLiteCoverPhase.LOADING) Text("正在读取系统照片预览。尚未更改封面。")
            }
        },
        confirmButton = {
            if (state.phase == PhotoLiteCoverPhase.REMOVE_CONFIRM) TextButton(onConfirm,
                Modifier.heightIn(min = 48.dp).testTag("photo-lite-remove-confirm")) { Text("取消封面显示") }
            else if (state.phase == PhotoLiteCoverPhase.PREVIEW) TextButton(onConfirm,
                Modifier.heightIn(min = 48.dp).testTag("photo-lite-confirm")) { Text("保存为封面") }
            else if (state.phase in setOf(PhotoLiteCoverPhase.SUCCESS, PhotoLiteCoverPhase.REJECTED)) TextButton(onCancel,
                Modifier.heightIn(min = 48.dp).testTag("photo-lite-dismiss")) { Text("知道了") }
        },
        dismissButton = {
            if (!state.blocksLeaving && state.phase !in setOf(PhotoLiteCoverPhase.SUCCESS, PhotoLiteCoverPhase.REJECTED))
                TextButton(onCancel, Modifier.heightIn(min = 48.dp).testTag("photo-lite-cancel")) {
                    Text(if (state.phase == PhotoLiteCoverPhase.REMOVE_CONFIRM) "保留封面" else "取消")
                }
        },
    )
}
