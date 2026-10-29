package com.cooper.lithic.feature.category

import com.cooper.lithic.core.design.*

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import com.cooper.lithic.core.design.LithicButton as Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import com.cooper.lithic.core.design.LithicOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType

@Composable
internal fun CategoryManagementScreen(
    state: CategoryManagementUiState,
    onBack: () -> Unit,
    onTypeChange: (CategoryType) -> Unit,
    onCreate: () -> Unit,
    onRename: (CategoryEntity) -> Unit,
    onMerge: (CategoryEntity) -> Unit,
    onNameChange: (String) -> Unit,
    onTargetSelected: (String) -> Unit,
    onPreviewMerge: () -> Unit,
    onSave: () -> Unit,
    onConfirmMerge: () -> Unit,
    onDismissEditor: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().imePadding().testTag("category-management-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                C2PageHeader("类别管理", onBack, "category-back", backLabel = "返回设置", actions = {
                    C5InformationButton("类别说明", Modifier.testTag("category-information")) {
                        Text("长期项目与订阅分别管理。类别调整后，历史记录也按当前分类展示。")
                        Text("默认“其他”类别保留供未分类记录使用，不提供重命名或作为合并源。")
                    }
                })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (type in CategoryType.entries) {
                        OutlinedButton({ onTypeChange(type) }, Modifier.weight(1f).heightIn(min = 48.dp)
                            .testTag("category-type-${type.name}"), enabled = state.editor == null) {
                            Text((if (state.type == type) "✓ " else "") + if (type == CategoryType.PROJECT) "长期项目" else "订阅服务")
                        }
                    }
                }
                Button(onCreate, Modifier.heightIn(min = 48.dp).testTag("category-create"),
                    enabled = !state.loading && state.error == null && state.editor == null) { Text("新建类别") }
                state.message?.let { UiFeedbackText(it, UiMessageTone.INFORMATION, "category-feedback") }
                state.error?.let {
                    UiFeedbackText(it, UiMessageTone.ERROR, "category-error")
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("category-retry")) { Text("重新读取") }
                }
                if (state.loading) Text("正在读取类别")
            }
        }
        items(state.items, key = { it.category.id }) { entry ->
            val category = entry.category
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                .testTag("category-row-${category.id}")) {
                Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Text(category.name, style = MaterialTheme.typography.bodyLarge)
                    Text("${entry.recordCount} 条记录（含历史）", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (category.isFallback) {
                        Text("默认类别，保留供未分类记录使用。", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton({ onRename(category) }, Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("category-rename-${category.id}"), enabled = state.editor == null) { Text("重命名") }
                            TextButton({ onMerge(category) }, Modifier.weight(1f).heightIn(min = 48.dp)
                                .testTag("category-merge-${category.id}"), enabled = state.editor == null) { Text("合并到…") }
                        }
                    }
                }
            }
        }
    }
    state.editor?.let { editor ->
        val mergeMode = editor.mode == CategoryEditMode.MERGE
        AlertDialog(onDismissRequest = { if (!editor.busy && !editor.uncertain) onDismissEditor() },
            modifier = Modifier.testTag("category-editor-dialog"),
            title = { Text(when (editor.mode) {
                CategoryEditMode.CREATE -> "新建类别"
                CategoryEditMode.RENAME -> "重命名类别"
                CategoryEditMode.MERGE -> if (editor.preview == null) "选择合并目标" else "确认类别合并"
            }) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                    if (!mergeMode) {
                        NameField(editor.name, onNameChange, Modifier.testTag("category-name"), label = "类别名称", enabled = editor.canChange)
                    } else if (editor.preview == null) {
                        Text("将“${editor.source?.name.orEmpty()}”合并到同类型类别：", style = MaterialTheme.typography.bodyLarge)
                        state.items.filter { it.category.id != editor.categoryId }.forEach { target ->
                            OutlinedButton({ onTargetSelected(target.category.id) }, Modifier.fillMaxWidth()
                                .heightIn(min = 48.dp).testTag("category-target-${target.category.id}"), enabled = editor.canChange) {
                                Text((if (editor.targetId == target.category.id) "✓ " else "") + target.category.name)
                            }
                        }
                    } else {
                        val preview = editor.preview
                        Text("“${preview.source.name}”的 ${preview.affectedCount} 条记录（含历史）将移至“${preview.target.name}”。",
                            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("category-merge-scope"))
                        Text("全部关联更新后删除源类别。金额、付款和状态保持原样；历史分类随之调整。",
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("category-merge-impact"))
                    }
                    if (editor.busy) Text("正在处理，请稍候。", style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("category-editor-busy").semantics { liveRegion = LiveRegionMode.Polite })
                    editor.message?.let { UiFeedbackText(it,
                        if (editor.uncertain || editor.conflict) UiMessageTone.WARNING else UiMessageTone.ERROR,
                        "category-editor-feedback") }
                    if (editor.uncertain) Text("提交结果尚未确认。请保留本次内容，重新确认同一次提交。",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag("category-editor-uncertain").semantics { liveRegion = LiveRegionMode.Polite })
                }
            },
            confirmButton = {
                val action = when {
                    mergeMode && editor.preview != null -> onConfirmMerge
                    mergeMode -> onPreviewMerge
                    else -> onSave
                }
                TextButton(action, Modifier.heightIn(min = 48.dp).testTag(if (mergeMode && editor.preview != null) "category-merge-confirm" else "category-save"),
                    enabled = !editor.busy && !editor.conflict && (!mergeMode || editor.targetId != null)) {
                    Text(when {
                        editor.uncertain -> "重新确认本次提交"
                        mergeMode && editor.preview != null -> "确认合并"
                        mergeMode -> "查看影响范围"
                        else -> "保存"
                    })
                }
            },
            dismissButton = {
                TextButton(onDismissEditor, Modifier.heightIn(min = 48.dp).testTag("category-cancel"),
                    enabled = !editor.busy && !editor.uncertain) { Text("取消") }
            })
    }
}
