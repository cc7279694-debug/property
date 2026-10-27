package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.C2Section
import com.cooper.lithic.core.design.C2StatusChip
import com.cooper.lithic.core.design.C2RecordIdentity
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C5InformationButton
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.data.local.ReplacementEntity

@Composable
internal fun ReplacementComparisonScreen(
    state: ReplacementUiState,
    onBack: () -> Unit,
    onOpenProject: (String) -> Unit,
    onLink: () -> Unit,
    onUnlink: (ReplacementEntity) -> Unit,
    onRetry: () -> Unit,
    onSelectTarget: (String) -> Unit,
    onDirection: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onSave: () -> Unit,
    onDismissForm: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier.fillMaxSize().testTag("replacement-comparison-screen"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            Column(Modifier.padding(LithicSpacing.medium), verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                C2PageHeader("替换链与纵向对比", onBack, "replacement-back", backLabel = "返回项目详情")
                C5InformationButton("替换关系说明", Modifier.testTag("replacement-information"), buttonLabel = "替换关系说明") {
                    Text("只记录你明确建立的关系；不会自动结束项目或生成金额。每个项目最多一位前任和一位后任。")
                    Text("解除关系不撤销现金付款、折抵或结束事实，也不自动连接两边项目。")
                }
                if (state.loading) Text("正在读取替换链")
                state.error?.let {
                    Text(it, Modifier.testTag("replacement-read-error"), color = MaterialTheme.colorScheme.error)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp)) { Text("重新读取") }
                }
                OutlinedButton(onLink, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("replacement-link"),
                    enabled = !state.loading && state.chain != null && state.form == null) { Text("明确建立替换关系") }
            }
        }
        val chain = state.chain
        if (chain != null) {
            item { Text("结果截至 ${chain.asOf}；各项使用同一观察日。", Modifier.padding(horizontal = LithicSpacing.medium)) }
            items(chain.items, key = { it.project.id }) { detail ->
                C2Section(
                    if (detail.project.id == state.anchorId) "当前查看" else if (chain.items.indexOf(detail) < chain.items.indexOfFirst { it.project.id == state.anchorId }) "前任" else "后任",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium).testTag("replacement-item-${detail.project.id}")) {
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (detail.project.coverAssetId == null) C2RecordIdentity(detail.project.name, detail.category.name, C2Glyph.Project)
                        else androidx.compose.material3.Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) { C2Icon(C2Glyph.ImageError, description = "封面暂不可用") }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(detail.project.name, style = MaterialTheme.typography.titleLarge)
                            Text(detail.category.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            C2StatusChip(projectStatusLabel(detail))
                        }
                    }
                    if (detail.project.coverAssetId != null) Text("封面暂不可用", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (detail.cost.unavailableReason == null) "持有 ${detail.cost.heldDays} 天" else "尚未开始，持有时长不可计算")
                    val metricLabel = if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal) "已结束；结果截至今天" else "当前日均成本"
                    Text(metricLabel)
                    MoneyMetric(detail.cost.dailyRate, Modifier.fillMaxWidth().testTag("replacement-daily-${detail.project.id}"),
                        level = MoneyMetricLevel.ROW, label = metricLabel)
                    if (detail.events.any { it.exchangeGroupId != null }) Text("成本包含已记录的非现金折抵。")
                    TextButton({ onOpenProject(detail.project.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .testTag("replacement-open-${detail.project.id}")) { Text("查看此项目详情") }
                    chain.links.singleOrNull { it.predecessorId == detail.project.id }?.let { link ->
                        val next = chain.items.single { it.project.id == link.successorId }
                        Text("↓ 替换为 ${next.project.name}", style = MaterialTheme.typography.bodyLarge)
                        TextButton({ onUnlink(link) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("replacement-unlink-${link.id}")) { Text("解除这一关系") }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    state.form?.takeIf { it.kind != ReplacementFormKind.TRADE_IN }?.let { form ->
        val pending = form.phase == SubmissionPhase.UNCONFIRMED
        AlertDialog(onDismissRequest = { if (form.canEdit) onDismissForm() }, modifier = Modifier.testTag("replacement-link-dialog"),
            title = { Text(if (form.kind == ReplacementFormKind.UNLINK) "解除替换关系" else "建立替换关系") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                    Text(form.projectName)
                    if (form.loading) Text("正在核对项目与关系")
                    if (form.kind == ReplacementFormKind.LINK) {
                        if (form.confirmed || pending) {
                            Text(if (form.asSuccessor) "${form.projectName} → ${form.targetName}" else "${form.targetName} → ${form.projectName}",
                                Modifier.testTag("replacement-link-summary"), style = MaterialTheme.typography.titleLarge)
                        } else {
                            OutlinedButton({ onDirection(true) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = form.canEdit) {
                                Text(if (form.asSuccessor) "✓ 当前项目作为前任" else "当前项目作为前任")
                            }
                            OutlinedButton({ onDirection(false) }, Modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = form.canEdit) {
                                Text(if (!form.asSuccessor) "✓ 当前项目作为后任" else "当前项目作为后任")
                            }
                            Text("选择另一项目", style = MaterialTheme.typography.titleMedium)
                            state.choices.forEach { detail ->
                                TextButton({ onSelectTarget(detail.project.id) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                    .testTag("replacement-choice-${detail.project.id}"), enabled = form.canEdit) {
                                    Text((if (form.targetId == detail.project.id) "✓ " else "") + detail.project.name)
                                }
                            }
                            if (state.hasMoreChoices) TextButton(onLoadMore, enabled = !state.loadingChoices, modifier = Modifier.heightIn(min = 48.dp)) { Text("加载更多项目") }
                            if (state.loadingChoices) Text("正在读取可选项目")
                        }
                    } else Text("只解除这条关系，不撤销现金付款、折抵或结束事实，也不自动连接两边项目。")
                    Text("每个项目最多一位前任和一位后任；冲突时不会覆盖旧关系。")
                    form.errors.values.forEach { Text(it, color = MaterialTheme.colorScheme.error) }
                    form.message?.let { Text(it, Modifier.testTag("replacement-form-message"), color = MaterialTheme.colorScheme.error) }
                    if (!form.loading && (form.needsInitialRead || form.message != null)) TextButton(onRetry,
                        Modifier.heightIn(min = 48.dp).testTag("replacement-form-retry")) { Text("重新读取") }
                    if (pending) Text("提交结果待核对，原身份已保留。")
                }
            },
            confirmButton = {
                Button(if (form.confirmed || pending) onSave else onPreview, enabled = !form.busy && !form.loading,
                    modifier = Modifier.heightIn(min = 48.dp).testTag("replacement-confirm")) {
                    Text(if (pending) "核对并重试" else if (form.confirmed) "确认保存" else "预览并核对")
                }
            },
            dismissButton = { TextButton(onDismissForm, enabled = form.canEdit, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消") } })
    }
}
