package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.FormHeader
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField

@Composable
internal fun ProjectEditorScreen(
    state: ProjectEditorUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onStartedOnChange: (String) -> Unit,
    onInitialAmountChange: (String) -> Unit,
    onInitialPaidOnChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetryRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resolveEditConflict = state.mode == ProjectEditorMode.EDIT && state.conflict
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())
        .padding(LithicSpacing.medium).testTag("project-editor-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        FormHeader(if (state.mode == ProjectEditorMode.CREATE) "新增长期项目" else "编辑项目资料",
            onBack, "project-editor-back")
        if (state.loading) Text("正在读取项目资料", style = MaterialTheme.typography.bodyLarge)
        LithicCard {
            NameField(state.name, onNameChange, Modifier.testTag("project-name"),
                error = state.errors["name"], enabled = state.canEdit)
            state.today?.let { today ->
                ProjectDateInput(state.startedOn, onStartedOnChange, today, "开始拥有日期", "project-started-on",
                    error = state.errors["startedOn"], enabled = state.canEdit)
            }
        }
        if (state.mode == ProjectEditorMode.CREATE) {
            LithicCard {
                AmountField(state.initialAmount, onInitialAmountChange, Modifier.testTag("project-initial-amount"),
                    label = "初始实际投入（元）", error = state.errors["initialAmount"], enabled = state.canEdit)
                Text("只记录实际已投入金额，0元也可以保存。", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.today?.let { today ->
                    ProjectDateInput(state.initialPaidOn, onInitialPaidOnChange, today, "初始实际付款日期", "project-initial-paid-on",
                        error = state.errors["initialPaidOn"], enabled = state.canEdit)
                }
                Text("付款日期默认同开始拥有日期；先付款后收到时，请修改为真实付款日。",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LithicCard {
            ProjectCategoryPicker(state.categories, state.categoryId, onCategoryChange,
                error = state.errors["categoryId"], enabled = state.canEdit)
            ProjectNoteInput(state.note, onNoteChange, "备注（可选）", "project-note", state.canEdit, state.errors["note"])
        }
        state.message?.let {
            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.errorContainer) {
                Text(it, Modifier.fillMaxWidth().padding(LithicSpacing.medium).testTag("project-form-error"),
                    color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (resolveEditConflict) {
            Text("重新读取会用当前已提交资料替换此处输入，请重新核对后编辑。",
                style = MaterialTheme.typography.bodyLarge)
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-resolve-conflict")) {
                Text("读取当前资料重新编辑", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (state.needsInitialRead && !state.loading) {
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-initial-read-retry")) {
                Text("重新读取项目资料", style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (state.busy) Text("正在保存到本地", style = MaterialTheme.typography.bodyLarge)
        if (state.phase == SubmissionPhase.COMMITTED) {
            Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-retry-read")) {
                Text("重新读取已保存项目", style = MaterialTheme.typography.bodyLarge)
            }
        } else if (!resolveEditConflict) {
            Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-save"),
                enabled = !state.loading && !state.busy && !state.needsInitialRead) {
                Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对并重试" else "保存项目",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
