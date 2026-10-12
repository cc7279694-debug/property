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
import java.math.BigDecimal

@Composable
internal fun ProjectDetailScreen(
    state: ProjectDetailUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddMoney: (MoneyDirection) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().testTag("project-detail-screen")) {
    LazyColumn(Modifier.fillMaxSize().testTag("project-detail-content"),
        verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        item {
            TextButton(onBack, Modifier.padding(horizontal = LithicSpacing.medium)
                .heightIn(min = 48.dp).testTag("project-detail-back")) {
                Text("返回项目列表", style = MaterialTheme.typography.bodyLarge)
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
        state.detail?.let { detail ->
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = LithicSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Text(detail.project.name, style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.testTag("project-detail-name").semantics { heading() })
                    Text(projectStatusLabel(detail), style = MaterialTheme.typography.bodyLarge)
                    Text("开始拥有：${detail.project.startedOn}", style = MaterialTheme.typography.bodyMedium)
                    detail.project.endedOn?.let { Text("已记录结束日期：$it", style = MaterialTheme.typography.bodyMedium) }
                    if (detail.cost.unavailableReason == null) Text("已持有${detail.cost.heldDays}天", style = MaterialTheme.typography.bodyLarge)
                    Text(if (detail.project.endedOn != null && !detail.cost.contributesToCurrentTotal)
                        "截至观察日的日均成本" else "当前日均成本", style = MaterialTheme.typography.bodyMedium)
                    AmountText(detail.cost.dailyRate, Modifier.testTag("project-detail-daily"))
                    if (detail.cost.unavailableReason != null) Text("尚未开始，没有有效日均成本。", style = MaterialTheme.typography.bodyMedium)
                    Text("结果截至 ${detail.asOf}", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    OutlinedButton(onEdit, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-edit")) {
                        Text("编辑项目资料", style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton({ onAddMoney(MoneyDirection.OUTLAY) },
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-outlay")) {
                        Text("记录投入", style = MaterialTheme.typography.bodyLarge)
                    }
                    OutlinedButton({ onAddMoney(MoneyDirection.RECOVERY) },
                        Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("project-add-recovery")) {
                        Text("记录收回", style = MaterialTheme.typography.bodyLarge)
                    }
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
                    Text(if (event.role == ProjectMoneyEventRole.INITIAL) "初始$direction" else direction,
                        style = MaterialTheme.typography.bodyLarge)
                    Text(event.occurredOn.toString(), style = MaterialTheme.typography.bodyMedium)
                    AmountText(BigDecimal.valueOf(event.amountCents, 2), unit = AmountUnit.YUAN,
                        style = MaterialTheme.typography.titleLarge)
                    event.description?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
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
