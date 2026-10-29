package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicIconButton as IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C5InformationButton

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
    var startExpanded by rememberSaveable(state.projectId, state.mode) { mutableStateOf(false) }
    var paymentExpanded by rememberSaveable(state.projectId, state.mode) { mutableStateOf(false) }
    var notesExpanded by rememberSaveable(state.projectId, state.mode) { mutableStateOf<Boolean?>(null) }
    var saveAttempt by remember { mutableIntStateOf(0) }
    val disclosure = projectEditorDisclosure(state, startExpanded, paymentExpanded, notesExpanded)
    val errorTargets = remember(state.projectId, state.mode) {
        ProjectEditorField.entries.associateWith { BringIntoViewRequester() }
    }
    LaunchedEffect(disclosure.startedOn, disclosure.initialPaidOn, disclosure.note) {
        // A field revealed for an error/value stays open while the user corrects that value.
        if (disclosure.startedOn) startExpanded = true
        if (disclosure.initialPaidOn) paymentExpanded = true
        if (disclosure.note) notesExpanded = true
    }
    LaunchedEffect(state.errors, saveAttempt) {
        disclosure.firstError?.let { target ->
            if (state.today == null && target in listOf(ProjectEditorField.STARTED_ON, ProjectEditorField.INITIAL_PAID_ON)) {
                return@let
            }
            // Newly revealed fields must be laid out before their scroll destination is requested.
            withFrameNanos { }
            errorTargets.getValue(target).bringIntoView()
        }
    }

    Column(modifier.fillMaxSize().imePadding().testTag("project-editor-screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            IconButton(onBack, Modifier.size(48.dp).testTag("project-editor-back")) {
                C2Icon(C2Glyph.Back, description = "返回")
            }
        }
        // Back keeps its own measured row. The complete title width belongs to the scroll body,
        // so system font scaling cannot squeeze it against the return target or trap fields below it.
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(LithicSpacing.medium).testTag("project-editor-content"),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
            Text(if (state.mode == ProjectEditorMode.CREATE) "新增长期项目" else "编辑项目资料",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().testTag("project-editor-title").semantics { heading() })
            if (state.loading) Text("正在读取项目资料", style = MaterialTheme.typography.bodyLarge)
            NameField(state.name, onNameChange,
                Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.NAME)).testTag("project-name"),
                error = state.errors["name"], enabled = state.canEdit, labelOutside = true)

            if (state.mode == ProjectEditorMode.CREATE) {
                Column(Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.INITIAL_AMOUNT)),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    AmountField(state.initialAmount, onInitialAmountChange, Modifier.testTag("project-initial-amount"),
                        label = "初始实际投入（元）", error = state.errors["initialAmount"], enabled = state.canEdit,
                        labelOutside = true)
                    Text("0元也可保存，空白不代表0。", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            state.today?.let { today ->
                Column(Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.STARTED_ON)),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    if (disclosure.startedOn) {
                        ProjectDateInput(state.startedOn, onStartedOnChange, today, "开始拥有日期", "project-started-on",
                            error = state.errors["startedOn"], enabled = state.canEdit && !state.tradeInProtected,
                            labelOutside = true)
                    } else {
                        EditorSummary("开始拥有日期", state.startedOn, "project-started-on-summary",
                            enabled = state.canEdit && !state.tradeInProtected, onClick = { startExpanded = true })
                    }
                }
            }
            if (state.tradeInProtected) Text("此项目参与以旧换新，取得日期与关联金额日期保持一致，不能单独修改。")

            ProjectCategoryPicker(state.categories, state.categoryId, onCategoryChange,
                Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.CATEGORY)),
                error = state.errors["categoryId"], enabled = state.canEdit, compactRow = true)

            if (state.mode == ProjectEditorMode.CREATE) {
                state.today?.let { today ->
                    Column(Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.INITIAL_PAID_ON)),
                        verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        if (disclosure.initialPaidOn) {
                            ProjectDateInput(state.initialPaidOn, onInitialPaidOnChange, today, "初始实际付款日期", "project-initial-paid-on",
                                error = state.errors["initialPaidOn"], enabled = state.canEdit, labelOutside = true)
                        } else {
                            EditorSummary("初始实际付款日期", "与开始日期相同 · ${state.initialPaidOn}",
                                "project-payment-summary", state.canEdit, onClick = { paymentExpanded = true })
                        }
                    }
                }
            }

            C5InformationButton("金额与日期说明", Modifier.testTag("project-editor-information"), buttonLabel = "金额与日期说明") {
                Text("初始投入只记录已经实际投入的金额，0元也可保存。")
                Text("开始日计入持有天数。先付款后收到时，请将初始实际付款日期改为真实付款日；付款日期不会改变开始拥有日期。")
            }

            Column(Modifier.bringIntoViewRequester(errorTargets.getValue(ProjectEditorField.NOTE)),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                EditorSummary("补充信息", when {
                    "note" in state.errors -> "请核对下方备注"
                    disclosure.note -> "收起备注"
                    state.note.isNotBlank() -> "已填写备注 · 展开查看"
                    else -> "备注（可选）"
                }, "project-extra-toggle", enabled = "note" !in state.errors,
                    onClick = { notesExpanded = !disclosure.note })
                if (disclosure.note) {
                    ProjectNoteInput(state.note, onNoteChange, "备注（可选）", "project-note", state.canEdit, state.errors["note"],
                        labelOutside = true)
                }
            }

            state.message?.let { UiFeedbackText(it, state.messageTone, "project-form-error") }
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
            ProjectSubmissionNotice(state.phase, "project")
            Spacer(Modifier.fillMaxWidth().height(16.dp).testTag("project-editor-content-end"))
        }
        // A measured sibling, never an overlay: the weighted body ends above this actual height,
        // including when its button grows with system fonts. Outer imePadding handles keyboard space.
        Surface(Modifier.fillMaxWidth().testTag("project-editor-save-bar"), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium, vertical = LithicSpacing.small)) {
                if (state.phase == SubmissionPhase.COMMITTED) {
                    Button(onRetryRead, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-retry-read")) {
                        Text("重新读取已保存项目", style = MaterialTheme.typography.bodyLarge)
                    }
                } else if (!resolveEditConflict) {
                    Button(onClick = { saveAttempt++; onSave() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-save"),
                        enabled = !state.loading && !state.busy && !state.needsInitialRead) {
                        Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对并重试" else "保存项目",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorSummary(label: String, value: String, tag: String, enabled: Boolean, onClick: () -> Unit) {
    ProjectFormSummaryRow(label, value, tag, enabled, onClick)
}
