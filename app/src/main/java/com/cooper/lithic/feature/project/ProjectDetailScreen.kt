package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.feature.media.CoverSection
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
    Column(modifier.fillMaxSize().testTag("project-detail-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("project-detail-content"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            TextButton(onBack, Modifier.padding(horizontal = LithicSpacing.medium)
                .heightIn(min = 48.dp).testTag("project-detail-back")) {
                Text(backLabel, style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (state.loading) item { Text("正在读取项目", Modifier.padding(LithicSpacing.medium)) }
        state.error?.let { message ->
            item {
                Column(Modifier.padding(horizontal = LithicSpacing.medium)) {
                    Text(message, Modifier.testTag("project-detail-error"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                    TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("project-detail-retry")) {
                        Text("重新读取", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        if (state.missing) item { Text("项目不存在，返回列表查看当前记录。", Modifier.padding(LithicSpacing.medium)) }
        state.detail?.takeUnless { state.missing }?.let { detail ->
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Text(detail.project.name, style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.testTag("project-detail-name").semantics { heading() })
                    Text(projectStatusLabel(detail), style = MaterialTheme.typography.bodyLarge)
                    Text("开始拥有：${detail.project.startedOn}", style = MaterialTheme.typography.bodyMedium)
                    detail.project.endedOn?.let { Text("已记录结束日期：$it", style = MaterialTheme.typography.bodyMedium) }
                    detail.project.endReason?.takeIf { it.isNotBlank() }?.let {
                        Text("结束原因：$it", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (detail.cost.unavailableReason == null) Text("已持有${detail.cost.heldDays}天", style = MaterialTheme.typography.bodyLarge)
                    Text(if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal)
                        "截至观察日的日均成本" else "当前日均成本", style = MaterialTheme.typography.bodyMedium)
                    AmountText(detail.cost.dailyRate, Modifier.testTag("project-detail-daily"))
                    if (detail.cost.unavailableReason != null) Text("尚未开始，没有有效日均成本。", style = MaterialTheme.typography.bodyMedium)
                    Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal &&
                        detail.cost.unavailableReason == null) {
                        val hasObservedLateMoney = detail.events.any { event ->
                            event.occurredOn.isAfter(detail.project.endedOn) && !event.occurredOn.isAfter(detail.asOf)
                        }
                        Text(if (hasObservedLateMoney) "包含结束后的实际结算；持有天数不再增加。"
                            else "结束后仍可补录真实尾款或退款；持有天数不再增加。", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
                    ProjectTotal("累计实际投入", detail.cost.grossOutlayCents, "project-detail-outlay")
                    ProjectTotal("累计实际收回", detail.cost.recoveryCents, "project-detail-recovery")
                    ProjectTotal("净投入", detail.cost.netCents, "project-detail-net")
                    Text("类别：${detail.category.name}", style = MaterialTheme.typography.bodyLarge)
                    detail.project.note?.takeIf { it.isNotBlank() }?.let {
                        Text("备注", style = MaterialTheme.typography.bodyMedium)
                        Text(it, Modifier.testTag("project-detail-note"), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                CoverSection(detail.project.coverAssetId, mediaRepository, "project", onEditCover,
                    enabled = !state.loading && state.error == null)
            }
            item {
                OutlinedButton(onEdit, Modifier.padding(horizontal = LithicSpacing.medium)
                    .fillMaxWidth().heightIn(min = 48.dp).testTag("project-edit")) {
                    Text("编辑项目资料", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                OutlinedButton({ onAddMoney(MoneyDirection.OUTLAY) }, Modifier.padding(horizontal = LithicSpacing.medium)
                    .fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-outlay")) {
                    Text("记录投入", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                OutlinedButton({ onAddMoney(MoneyDirection.RECOVERY) }, Modifier.padding(horizontal = LithicSpacing.medium)
                    .fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-recovery")) {
                    Text("记录收回", style = MaterialTheme.typography.bodyLarge)
                }
            }
            item {
                if (detail.project.endedOn == null) {
                    OutlinedButton(onEnd, Modifier.padding(horizontal = LithicSpacing.medium)
                        .fillMaxWidth().heightIn(min = 48.dp).testTag("project-end")) {
                        Text("结束项目", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    OutlinedButton(onUpdateEnd, Modifier.padding(horizontal = LithicSpacing.medium)
                        .fillMaxWidth().heightIn(min = 48.dp).testTag("project-update-end")) {
                        Text("修正结束记录", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            item {
                TextButton(onDelete, Modifier.padding(horizontal = LithicSpacing.medium)
                    .fillMaxWidth().heightIn(min = 48.dp).testTag("project-delete")) {
                    Text("删除项目", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
                    HorizontalDivider()
                    Text("真实金额记录", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() })
                }
            }
            items(detail.events, key = { it.id }) { event ->
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium)
                    .testTag("project-event-${event.id}").semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    val direction = if (event.direction == MoneyDirection.OUTLAY) "投入" else "收回"
                    Text(when (event.role) {
                        ProjectMoneyEventRole.INITIAL -> "初始$direction"
                        ProjectMoneyEventRole.END_SETTLEMENT -> "结束关联$direction"
                        ProjectMoneyEventRole.REGULAR -> direction
                    },
                        style = MaterialTheme.typography.bodyLarge)
                    Text(event.occurredOn.toString(), style = MaterialTheme.typography.bodyMedium)
                    AmountText(BigDecimal.valueOf(event.amountCents, 2), unit = AmountUnit.YUAN,
                        style = MaterialTheme.typography.titleLarge)
                    event.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                    TextButton(onClick = {
                        if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) onUpdateEnd()
                        else onCorrectMoney(event.id)
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-correct-event-${event.id}")) {
                        Text(if (event.role == ProjectMoneyEventRole.END_SETTLEMENT) "修正结束及关联收回" else "纠正这条记录",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
    }
}

@Composable
private fun ProjectTotal(label: String, cents: Long, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        AmountText(BigDecimal.valueOf(cents, 2), Modifier.testTag(tag), AmountUnit.YUAN,
            style = MaterialTheme.typography.titleLarge)
    }
}
