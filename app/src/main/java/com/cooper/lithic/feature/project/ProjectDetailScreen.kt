package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.StatusBadge
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.feature.media.CoverDisplay
import java.math.BigDecimal

@Composable
internal fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMoney: (MoneyDirection) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onEnd: () -> Unit = {},
    onUpdateEnd: () -> Unit = {},
    onCorrectMoney: (String) -> Unit = {},
    onDelete: () -> Unit = {},
    backLabel: String = "返回项目列表",
    mediaRepository: MediaRepository? = null,
    onEditCover: () -> Unit = {},
) {
    var moreExpanded by remember(state.detail?.project?.id) { mutableStateOf(false) }
    val menuItemHeight = maxOf(48f,
        MaterialTheme.typography.bodyLarge.lineHeight.value * LocalDensity.current.fontScale + 16f).dp
    Column(modifier.fillMaxSize().testTag("project-detail-screen")) {
        LazyColumn(Modifier.fillMaxSize().testTag("project-detail-content"),
            contentPadding = PaddingValues(horizontal = LithicSpacing.medium, vertical = LithicSpacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onBack, Modifier.size(48.dp).testTag("project-detail-back")
                        .semantics { contentDescription = backLabel }) {
                        LithicIcon(LithicIcon.Back, null)
                    }
                    if (!state.missing && state.detail != null) {
                        Box {
                            IconButton({ moreExpanded = true }, Modifier.size(48.dp).testTag("project-more")
                                .semantics { contentDescription = "更多操作" }) {
                                LithicIcon(LithicIcon.More, null)
                            }
                            DropdownMenu(moreExpanded, { moreExpanded = false }, Modifier.widthIn(min = 240.dp, max = 360.dp)) {
                                DropdownMenuItem(text = { Text("编辑项目资料", style = MaterialTheme.typography.bodyLarge) },
                                    modifier = Modifier.heightIn(min = menuItemHeight).testTag("project-edit"),
                                    onClick = { moreExpanded = false; onEdit() })
                                DropdownMenuItem(text = { Text(if (state.detail.project.coverAssetId == null) "添加封面" else "管理封面",
                                    style = MaterialTheme.typography.bodyLarge) },
                                    modifier = Modifier.heightIn(min = menuItemHeight).testTag("project-cover-edit"), enabled = !state.loading && state.error == null,
                                    onClick = { moreExpanded = false; onEditCover() })
                                if (state.detail.project.endedOn == null) {
                                    DropdownMenuItem(text = { Text("结束项目", style = MaterialTheme.typography.bodyLarge) },
                                        modifier = Modifier.heightIn(min = menuItemHeight).testTag("project-end"),
                                        onClick = { moreExpanded = false; onEnd() })
                                } else {
                                    DropdownMenuItem(text = { Text("修正结束记录", style = MaterialTheme.typography.bodyLarge) },
                                        modifier = Modifier.heightIn(min = menuItemHeight).testTag("project-update-end"),
                                        onClick = { moreExpanded = false; onUpdateEnd() })
                                }
                                HorizontalDivider()
                                DropdownMenuItem(text = { Text("删除项目", color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge) },
                                    modifier = Modifier.heightIn(min = menuItemHeight).testTag("project-delete"),
                                    onClick = { moreExpanded = false; onDelete() })
                            }
                        }
                    }
                }
            }
            if (state.loading) item { Text("正在读取项目", style = MaterialTheme.typography.bodyLarge) }
            state.error?.let { message ->
                item {
                    Column {
                        Text(message, Modifier.testTag("project-detail-error"), color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge)
                        TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-detail-retry")) {
                            Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            if (state.missing) item { Text("项目不存在，返回列表查看当前记录。", style = MaterialTheme.typography.bodyLarge) }
            state.detail?.takeUnless { state.missing }?.let { detail ->
                if (detail.project.coverAssetId != null) {
                    item { CoverDisplay(detail.project.coverAssetId, mediaRepository, "project", previewAspectRatio = 2.2f) }
                } else {
                    item {
                        TextButton(onEditCover, Modifier.heightIn(min = 48.dp).testTag("project-cover-empty"), enabled = !state.loading && state.error == null) {
                            LithicIcon(LithicIcon.Image, null)
                            Text("尚未设置封面 · 添加封面", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                item {
                    LithicCard(emphasis = true) {
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 280.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                                    ProjectName(detail)
                                    StatusBadge(projectStatusLabel(detail))
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small),
                                    verticalAlignment = Alignment.Top) {
                                    Column(Modifier.weight(1f)) { ProjectName(detail) }
                                    StatusBadge(projectStatusLabel(detail))
                                }
                            }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (LocalDensity.current.fontScale >= 1.5f || maxWidth < 300.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
                                    ProjectDailyRate(detail)
                                    ProjectHeldDays(detail)
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.medium),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) { ProjectDailyRate(detail) }
                                    Column(Modifier.weight(1f)) { ProjectHeldDays(detail) }
                                }
                            }
                        }
                        Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal && detail.cost.unavailableReason == null) {
                            val hasObservedLateMoney = detail.events.any { event ->
                                event.occurredOn.isAfter(detail.project.endedOn) && !event.occurredOn.isAfter(detail.asOf)
                            }
                            Text(if (hasObservedLateMoney) "包含结束后的实际结算；持有天数不再增加。"
                                else "结束后仍可补录真实尾款或退款；持有天数不再增加。",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                        Button({ onAddMoney(MoneyDirection.OUTLAY) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-outlay")) {
                            Text("记录投入", style = MaterialTheme.typography.bodyLarge)
                        }
                        OutlinedButton({ onAddMoney(MoneyDirection.RECOVERY) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-recovery")) {
                            Text("记录收回", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item {
                    LithicCard {
                        Text("投入与收回", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        ProjectTotal("累计实际投入", detail.cost.grossOutlayCents, "project-detail-outlay")
                        HorizontalDivider()
                        ProjectTotal("累计实际收回", detail.cost.recoveryCents, "project-detail-recovery")
                        HorizontalDivider()
                        ProjectTotal("净投入", detail.cost.netCents, "project-detail-net")
                    }
                }
                item {
                    LithicCard {
                        Text("基本信息", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                        Text("类别：${detail.category.name}", style = MaterialTheme.typography.bodyLarge)
                        Text("开始拥有：${detail.project.startedOn}", style = MaterialTheme.typography.bodyLarge)
                        detail.project.endedOn?.let { Text("已记录结束日期：$it", style = MaterialTheme.typography.bodyLarge) }
                        detail.project.endReason?.takeIf { it.isNotBlank() }?.let { Text("结束原因：$it", style = MaterialTheme.typography.bodyLarge) }
                        detail.project.note?.takeIf { it.isNotBlank() }?.let {
                            HorizontalDivider()
                            Text("备注", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(it, Modifier.testTag("project-detail-note"), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                item { Text("真实金额记录", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
                items(detail.events, key = { it.id }) { event ->
                    LithicCard(Modifier.testTag("project-event-${event.id}").semantics(mergeDescendants = true) {}) {
                        val direction = if (event.direction == MoneyDirection.OUTLAY) "投入" else "收回"
                        Text(when (event.role) {
                            ProjectMoneyEventRole.INITIAL -> "初始$direction"
                            ProjectMoneyEventRole.END_SETTLEMENT -> "结束关联$direction"
                            ProjectMoneyEventRole.REGULAR -> direction
                        }, style = MaterialTheme.typography.titleMedium)
                        Text(event.occurredOn.toString(), style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AmountText(BigDecimal.valueOf(event.amountCents, 2), unit = AmountUnit.YUAN, style = MaterialTheme.typography.titleLarge)
                        event.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                        TextButton(onClick = {
                            if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) onUpdateEnd() else onCorrectMoney(event.id)
                        }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-correct-event-${event.id}")) {
                            Text(if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) "修正结束及关联收回" else "纠正这条记录",
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(LithicSpacing.medium)) }
        }
    }
}

@Composable
private fun ProjectName(detail: ProjectDetail) {
    Text(detail.project.name, style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.testTag("project-detail-name").semantics { heading() })
}

@Composable
private fun ProjectDailyRate(detail: ProjectDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text(if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal)
            "截至观察日的日均成本" else "当前日均成本", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(detail.cost.dailyRate, Modifier.testTag("project-detail-daily"))
    }
}

@Composable
private fun ProjectHeldDays(detail: ProjectDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        if (detail.cost.unavailableReason == null) {
            Text("持有时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("已持有${detail.cost.heldDays}天", style = MaterialTheme.typography.titleLarge)
        } else Text("尚未开始，没有有效日均成本。", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ProjectTotal(label: String, cents: Long, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AmountText(BigDecimal.valueOf(cents, 2), Modifier.testTag(tag), AmountUnit.YUAN, style = MaterialTheme.typography.titleLarge)
    }
}
