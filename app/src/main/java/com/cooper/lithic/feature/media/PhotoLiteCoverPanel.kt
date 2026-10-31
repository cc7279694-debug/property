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
    AlertDialog(
        onDismissRequest = { if (!state.blocksLeaving) onCancel() },
        modifier = Modifier.testTag("photo-lite-dialog"),
        properties = DialogProperties(dismissOnBackPress = !state.blocksLeaving, dismissOnClickOutside = false),
        title = { Text(when (state.phase) {
            PhotoLiteCoverPhase.PREVIEW -> "确认物品照片"
            PhotoLiteCoverPhase.SAVING -> "正在保存封面"
            PhotoLiteCoverPhase.SUCCESS -> "封面已保存"
            PhotoLiteCoverPhase.UNKNOWN -> "请保留当前现场"
            PhotoLiteCoverPhase.REJECTED -> "照片未保存"
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
                    Text("确认后仅保存这张本地封面；取消不会更改物品。")
                }
                if (busy) CircularProgressIndicator(Modifier.testTag("photo-lite-progress"))
                state.message?.let { Text(it, Modifier.testTag("photo-lite-feedback")) }
                if (state.phase == PhotoLiteCoverPhase.SAVING) Text("请等待明确结果，不要重复提交。")
                if (state.phase == PhotoLiteCoverPhase.PREPARING) Text("正在核对当前物品与账本。")
                if (state.phase == PhotoLiteCoverPhase.LOADING) Text("正在读取系统照片预览。尚未更改封面。")
            }
        },
        confirmButton = {
            if (state.phase == PhotoLiteCoverPhase.PREVIEW) TextButton(onConfirm,
                Modifier.heightIn(min = 48.dp).testTag("photo-lite-confirm")) { Text("保存为封面") }
            else if (state.phase in setOf(PhotoLiteCoverPhase.SUCCESS, PhotoLiteCoverPhase.REJECTED)) TextButton(onCancel,
                Modifier.heightIn(min = 48.dp).testTag("photo-lite-dismiss")) { Text("知道了") }
        },
        dismissButton = {
            if (!state.blocksLeaving && state.phase !in setOf(PhotoLiteCoverPhase.SUCCESS, PhotoLiteCoverPhase.REJECTED))
                TextButton(onCancel, Modifier.heightIn(min = 48.dp).testTag("photo-lite-cancel")) { Text("取消") }
        },
    )
}
