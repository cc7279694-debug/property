package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountField
import com.cooper.lithic.core.design.AmountText
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.NameField
import com.cooper.lithic.core.model.MoneyParseResult
import com.cooper.lithic.core.model.MoneyParser
import java.math.BigDecimal

@Composable
internal fun TradeInEditorScreen(
    state: ReplacementFormUiState,
    onBack: () -> Unit,
    onName: (String) -> Unit,
    onDate: (String) -> Unit,
    onCash: (String) -> Unit,
    onCredit: (String) -> Unit,
    onCategory: (String) -> Unit,
    onNote: (String) -> Unit,
    onLink: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onEditConfirmation: () -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val confirming = state.confirmed || state.phase != SubmissionPhase.IDLE
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(LithicSpacing.medium)
        .testTag("trade-in-screen"), verticalArrangement = Arrangement.spacedBy(LithicSpacing.medium)) {
        TextButton(onBack, Modifier.heightIn(min = 48.dp).testTag("trade-in-back")) { Text("返回项目详情") }
        Text(if (confirming) "确认以旧换新" else "记录以旧换新", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.semantics { heading() })
        Text("旧项目：${state.projectName}", Modifier.testTag("trade-in-old-name"), style = MaterialTheme.typography.titleLarge)
        if (state.loading) Text("正在核对旧项目及金额记录")
        if (!confirming) {
            NameField(state.name, onName, Modifier.testTag("trade-in-name"), label = "新项目名称", enabled = state.canEdit, error = state.errors["name"])
            ProjectDateInput(state.date, onDate, state.today, "真实交易日期", "trade-in-date", enabled = state.canEdit, error = state.errors["date"])
            Text("旧项目在这一天结束，新项目在这一天开始拥有。")
            AmountField(state.cash, onCash, Modifier.testTag("trade-in-cash"), label = "实际现金补款（元）", enabled = state.canEdit, error = state.errors["cash"])
            Text("只记录实际已付款；0元现金补款可以保存，不推算未付分期。")
            AmountField(state.credit, onCredit, Modifier.testTag("trade-in-credit"), label = "非现金折抵金额（元）", enabled = state.canEdit, error = state.errors["credit"])
            ProjectCategoryPicker(state.categories, state.categoryId, onCategory, error = state.errors["categoryId"], enabled = state.canEdit)
            ProjectNoteInput(state.note, onNote, "新项目备注（可选）", "trade-in-note", state.canEdit, state.errors["note"])
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trade-in-link")
                .toggleable(state.linkReplacement, enabled = state.canEdit, role = Role.Checkbox, onValueChange = onLink),
                verticalAlignment = Alignment.CenterVertically) {
                Checkbox(state.linkReplacement, onCheckedChange = null, enabled = state.canEdit)
                Text("同时建立旧 → 新替换关系", Modifier.weight(1f))
            }
        } else {
            Text("新项目：${state.name}", Modifier.testTag("trade-in-confirm-new-name"), style = MaterialTheme.typography.titleLarge)
            Text("真实交易日期：${state.date}")
            HorizontalDivider()
            TradeAmount("旧项目收回 · 非现金折抵", cents(state.credit), "trade-in-confirm-old-credit")
            TradeAmount("新项目投入 · 实际现金", cents(state.cash), "trade-in-confirm-cash")
            TradeAmount("新项目投入 · 非现金折抵", cents(state.credit), "trade-in-confirm-new-credit")
            val total = runCatching { Math.addExact(checkNotNull(cents(state.cash)), checkNotNull(cents(state.credit))) }.getOrNull()
            TradeAmount("新项目取得成本（含折抵）", total, "trade-in-confirm-acquisition")
            TradeAmount("两项目合计净投入增加", cents(state.cash), "trade-in-confirm-net-change")
            Text("折抵不是现金支出；以上不是首页日均成本的增量。三笔记录作为一组保存，不再额外重复记录初始总价。")
            Text(if (state.linkReplacement) "同时建立旧 → 新替换关系。" else "不建立替换关系，金额事实仍作为同一组保存。")
            Text("保存后暂不支持整组撤销。交易项目不能单独删除或改取得日期，交易行金额与日期不能单独纠错。名称、类别、备注和后续真实金额仍可管理。")
        }
        state.errors["identity"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.message?.let { Text(it, Modifier.testTag("trade-in-message"), color = MaterialTheme.colorScheme.error) }
        if (!state.loading && state.needsInitialRead) TextButton(onRetry, Modifier.heightIn(min = 48.dp).testTag("trade-in-read-retry")) { Text("重新读取") }
        when {
            state.busy -> Text("正在保存同一组交易，请稍候。", Modifier.testTag("trade-in-saving"))
            state.phase == SubmissionPhase.UNCONFIRMED -> {
                Text("结果待核对，输入和唯一提交身份已保留。不能另建一组交易。")
                Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trade-in-save")) { Text("核对并重试同一提交") }
            }
            state.phase == SubmissionPhase.COMMITTED -> Text("交易已经保存，正在读取新项目。")
            confirming -> {
                Button(onSave, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trade-in-save"), enabled = !state.loading) { Text("确认并保存整组交易") }
                OutlinedButton(onEditConfirmation, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trade-in-edit-confirmation")) { Text("返回修改输入") }
            }
            else -> Button(onPreview, Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("trade-in-preview"), enabled = !state.loading) { Text("预览交易并核对") }
        }
    }
}

private fun cents(value: String) = (MoneyParser.parseCny(value) as? MoneyParseResult.Success)?.money?.cents

@Composable private fun TradeAmount(label: String, cents: Long?, tag: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        AmountText(cents?.let { BigDecimal.valueOf(it, 2) }, Modifier.testTag(tag), AmountUnit.YUAN)
    }
}
