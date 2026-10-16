package com.cooper.lithic.feature.subscription

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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.FormHeader
import com.cooper.lithic.core.design.LithicCard
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.ProjectDateInput
import com.cooper.lithic.feature.project.ProjectNoteInput
import com.cooper.lithic.feature.project.SubmissionPhase

/** One scrollable confirmation page; no calendar/workflow or service-provider action. */
@Composable
internal fun SubscriptionStateDialog(state: SubscriptionStateEditorUiState, onBack: () -> Unit,
    onEffectiveOn: (String) -> Unit, onReason: (String) -> Unit, onNote: (String) -> Unit,
    onStateValue: (SubscriptionOperationState) -> Unit, onEarlyStop: (Boolean) -> Unit,
    onConfirm: () -> Unit, onRetryRead: () -> Unit, modifier: Modifier = Modifier) {
    val stop = state.kind == SubscriptionStateEditorKind.STATE && state.action in listOf("STOP_AT_END", "STOP_EARLY")
    val title = when (state.kind) {
        SubscriptionStateEditorKind.CORRECT_STATE -> "修正状态事实"
        SubscriptionStateEditorKind.CANCEL_STOP -> "取消停止计划"
        SubscriptionStateEditorKind.DELETE_RENEWAL -> "删除错误付款"
        SubscriptionStateEditorKind.STATE -> when (state.action) { "PAUSE" -> "暂停"; "RESUME" -> "恢复"; "REOPEN" -> "重新开通"; else -> "停止记录" }
    }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(LithicSpacing.medium)
        .testTag("subscription-state-screen"), verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        FormHeader(title, onBack, "subscription-state-back", backLabel = "返回订阅详情")
        LithicCard {
            Text(state.serviceName, Modifier.testTag("subscription-state-service"), style = MaterialTheme.typography.titleLarge)
            if (state.loading) Text("正在读取原始事实", style = MaterialTheme.typography.bodyLarge)
            Text("只改变砺石中的记录和统计，不会替服务商取消自动续费或暂停扣费。真实付款历史保留。",
                Modifier.testTag("subscription-state-vendor-explanation"), style = MaterialTheme.typography.bodyLarge)
        }
        if (stop) {
            LithicCard {
                Text("已确认付费的最后边界：${state.fixedStopOn.orEmpty()}。停止日期确认后固定，后续付款不会自动移动它。",
                    style = MaterialTheme.typography.bodyLarge)
                OutlinedButton({ onEarlyStop(false) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-stop-at-end")
                    .semantics { selected = state.action == "STOP_AT_END" }, enabled = state.canEdit) {
                    Text("到期后停止", style = MaterialTheme.typography.bodyLarge)
                }
                OutlinedButton({ onEarlyStop(true) }, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-stop-early")
                    .semantics { selected = state.action == "STOP_EARLY" }, enabled = state.canEdit && state.fixedStopOn != state.today?.toString()) {
                    Text("提前停止", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        LithicCard {
            when (state.kind) {
                SubscriptionStateEditorKind.DELETE_RENEWAL -> {
                    Text("真实付款日：${state.renewalPaidOn} · 金额：${state.renewalAmount}元", Modifier.testTag("subscription-delete-payment-fact"),
                        style = MaterialTheme.typography.titleMedium)
                    Text("仅用于错误录入的付款。确认后只删除这一笔，无法在App撤回，并重新计算历史金额、标准时长和成本；这不是退款。",
                        Modifier.testTag("subscription-delete-payment-explanation"), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error)
                    if (state.lastPayment) Text("最后一笔真实付款不能单独删除。服务身份和原始付款会保留。",
                        Modifier.testTag("subscription-delete-last-payment"), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge)
                }
                SubscriptionStateEditorKind.CANCEL_STOP -> Text("取消 ${state.effectiveOn} 尚未生效的停止计划；其他状态和付款不变。",
                    style = MaterialTheme.typography.bodyLarge)
                else -> {
                    state.today?.let { today -> ProjectDateInput(state.effectiveOn, onEffectiveOn, today, "状态生效日期", "subscription-state-effective",
                        error = state.errors["effectiveOn"], enabled = state.canEdit && (state.kind == SubscriptionStateEditorKind.CORRECT_STATE || state.action == "STOP_EARLY")) }
                    if (state.kind == SubscriptionStateEditorKind.CORRECT_STATE) {
                        Text("纠正原状态日期或内容会重算历史有效天数；请核对原事实，保留原记录身份。",
                            style = MaterialTheme.typography.bodyLarge)
                        SubscriptionOperationState.entries.forEach { value ->
                            OutlinedButton({ onStateValue(value) }, Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .testTag("subscription-state-value-${value.name}").semantics { selected = state.stateValue == value.name }, enabled = state.canEdit) {
                                Text(when (value) { SubscriptionOperationState.ENABLED -> "启用"; SubscriptionOperationState.PAUSED -> "暂停"; SubscriptionOperationState.STOPPED -> "停止" },
                                    style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    ProjectNoteInput(state.reason, onReason, "原因（可选）", "subscription-state-reason", state.canEdit, state.errors["reason"])
                    ProjectNoteInput(state.note, onNote, "备注（可选）", "subscription-state-note", state.canEdit, state.errors["note"])
                }
            }
        }
        LithicCard {
            state.message?.let { Text(it, Modifier.testTag("subscription-state-error"), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge) }
            state.errors["form"]?.let { Text(it, Modifier.testTag("subscription-state-form-error"), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge) }
            when {
                state.verifiedCannotWrite || state.lastPayment -> Text("已有记录保持原样，请返回后核对。", style = MaterialTheme.typography.bodyLarge)
                state.phase == SubmissionPhase.COMMITTED || state.conflict || state.needsInitialRead -> Button(onRetryRead,
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-state-retry-read"), enabled = !state.loading && !state.busy) {
                    Text(if (state.phase == SubmissionPhase.COMMITTED && state.conflict) "核对当前事实并安全返回"
                        else if (state.phase == SubmissionPhase.COMMITTED) "重新读取已保存结果" else "读取并核对原事实",
                        style = MaterialTheme.typography.bodyLarge)
                }
                else -> Button(onConfirm, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("subscription-state-confirm"),
                    enabled = !state.loading && !state.busy,
                    colors = if (state.kind == SubscriptionStateEditorKind.DELETE_RENEWAL)
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                        else ButtonDefaults.buttonColors()) {
                    Text(if (state.phase == SubmissionPhase.UNCONFIRMED) "核对并重试同一提交" else "确认$title",
                        style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (state.busy) Text("正在保存到本地", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
