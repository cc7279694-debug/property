package com.cooper.lithic.feature.project

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.feature.media.CoverDisplay
import com.cooper.lithic.feature.media.CoverEditorUiState
import com.cooper.lithic.feature.media.CoverViewModel

/** PickVisualMedia selects one URI and falls back to the system document picker on API26. */
@Composable
internal fun CoverPicker(state: CoverEditorUiState, model: CoverViewModel, repository: MediaRepository?, onBack: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (model.state.value.editor?.target == state.target) model.chooseImage(uri)
    }
    CoverPickerContent(state, repository, onBack,
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, model::removeCover, model::save, model::retryRead)
}

@Composable
internal fun CoverPickerContent(state: CoverEditorUiState, repository: MediaRepository?, onBack: () -> Unit,
    onPick: () -> Unit, onRemove: () -> Unit, onSave: () -> Unit, onRead: () -> Unit) {
    val locked = state.loading || state.preparing || state.phase != SubmissionPhase.IDLE || state.conflict || state.missing
    Column(Modifier.fillMaxSize().testTag("cover-editor-screen").verticalScroll(rememberScrollState()).imePadding().padding(LithicSpacing.medium),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("cover-editor-back")) { Text("返回详情") }
        Text("本地封面", style = MaterialTheme.typography.headlineLarge)
        if (state.name.isNotBlank()) Text(state.name, Modifier.testTag("cover-editor-name"), style = MaterialTheme.typography.titleLarge)
        if (state.loading) Text("正在读取已保存记录", Modifier.testTag("cover-editor-loading"))
        if (state.name.isNotBlank() && !state.loading) CoverDisplay(state.oldAssetId, repository, "editor")
        else Text("封面信息尚未完成读取。", Modifier.testTag("cover-editor-unavailable"), style = MaterialTheme.typography.bodyMedium)
        Text("只保存你选中的静态JPEG或PNG。原相册图片不会被修改。", style = MaterialTheme.typography.bodyMedium)
        state.message?.let { Text(it, Modifier.testTag("cover-editor-error"), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge) }
        if (state.preparing) Text("正在准备本地封面，请稍候", Modifier.testTag("cover-editor-preparing"))
        state.preparedAsset?.let {
            Text("新封面已准备，确认保存后会替换当前封面。",
                Modifier.testTag("cover-editor-prepared"), style = MaterialTheme.typography.bodyLarge)
        }
        if (state.remove) Text("本次将移除此记录的封面，其他记录不会被删除。", Modifier.testTag("cover-editor-remove-preview"))
        if (state.phase == SubmissionPhase.COMMITTED) {
            Text("封面已保存。下面的操作只读取，不会再次导入或保存。", Modifier.testTag("cover-editor-committed"))
            OutlinedButton(onRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-retry-read"), enabled = !state.loading) { Text("重新读取已保存封面") }
        } else if (state.conflict || state.missing) {
            Text("重新读取会放弃本次未提交的图片选择，请核对当前记录后重新选择。", style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-resolve-conflict"), enabled = !state.loading) { Text("重新读取当前记录") }
        } else {
            OutlinedButton(onPick, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-select"), enabled = !locked) {
                Text(if (state.oldAssetId == null) "选择图片" else "更换封面")
            }
            if (state.oldAssetId != null) OutlinedButton(onRemove, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-remove"), enabled = !locked) { Text("移除封面") }
            Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-save"), enabled = !state.loading && !state.preparing &&
                state.dirty && state.phase != SubmissionPhase.IN_FLIGHT) {
                Text(when (state.phase) {
                    SubmissionPhase.IN_FLIGHT -> "正在保存到本地"
                    SubmissionPhase.UNCONFIRMED -> "核对并重试同一封面保存"
                    else -> if (state.remove) "确认移除封面" else "保存新封面"
                })
            }
            if (state.message != null && state.phase == SubmissionPhase.IDLE && !state.preparing)
                TextButton(onRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("cover-reload")) { Text("重新读取当前记录") }
        }
    }
}
