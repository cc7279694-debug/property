package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.math.RoundingMode

enum class AmountUnit(val label: String) { YUAN("元"), YUAN_PER_DAY("元/天") }

/** Display only: callers retain their original numeric value for all calculations. */
object AmountFormatter {
    private val oneCent = BigDecimal("0.01")

    fun formatCny(amount: BigDecimal?): String {
        if (amount == null) return "—"
        val rounded = amount.setScale(2, RoundingMode.HALF_UP)
        val sign = if (rounded.signum() < 0) "−" else ""
        return "$sign¥${rounded.abs().toPlainString()}"
    }

    fun formatDailyRate(amount: BigDecimal?): String = when {
        amount == null -> "—"
        isSubcent(amount) -> if (amount.signum() < 0) "−<¥0.01" else "<¥0.01"
        else -> formatCny(amount)
    }

    internal fun description(amount: BigDecimal?, unit: AmountUnit): String {
        if (amount == null) return "暂无数据"
        if (unit == AmountUnit.YUAN_PER_DAY && isSubcent(amount)) {
            return if (amount.signum() < 0) "负值，绝对值小于0.01元/天" else "小于0.01元/天"
        }
        val display = formatCny(amount)
        return if (display.startsWith("−")) "负${display.removePrefix("−¥")}${unit.label}"
            else "${display.removePrefix("¥")}${unit.label}"
    }

    private fun isSubcent(amount: BigDecimal): Boolean = amount.signum() != 0 && amount.abs() < oneCent
}

@Composable
fun AmountText(
    amount: BigDecimal?,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    showUnit: Boolean = true,
    color: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val display = when (unit) {
        AmountUnit.YUAN -> AmountFormatter.formatCny(amount)
        AmountUnit.YUAN_PER_DAY -> AmountFormatter.formatDailyRate(amount)
    }
    val density = LocalDensity.current
    var fullOpen by remember(display, unit) { mutableStateOf(false) }
    var copyFailed by remember(display, unit) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier.semantics(mergeDescendants = true) {
        contentDescription = AmountFormatter.description(amount, unit)
    }) {
        val preferred = style.fontSize.takeIf { it.isSp && it.value.isFinite() && it.value > 0 }?.value ?: 38f
        val available = constraints.maxWidth
        val layout = remember(display, style, available, density.fontScale, density.density, measurer) {
            amountLayout(preferred, available) { size ->
                measurer.measure(AnnotatedString(display), style.copy(fontSize = size.sp),
                    softWrap = false, maxLines = 1).size.width
            }
        }
        val lineHeight = if (style.lineHeight.isSp) (style.lineHeight.value * layout.fontSp / preferred).sp else style.lineHeight
        Column {
            Text(display, modifier = if (layout.horizontalScroll) Modifier.fillMaxWidth().heightIn(min = 48.dp).horizontalScroll(rememberScrollState()).testTag("amount-horizontal-view") else Modifier,
                style = style.copy(fontSize = layout.fontSp.sp, lineHeight = lineHeight),
                color = color, softWrap = false, maxLines = 1)
            if (showUnit) Text(unit.label, style = MaterialTheme.typography.bodyMedium,
                color = supportingColor, softWrap = true)
            if (layout.horizontalScroll) Text("左右滑动查看完整金额", Modifier.testTag("amount-horizontal-hint"),
                style = MaterialTheme.typography.bodySmall, color = supportingColor)
            if (layout.horizontalScroll) TextButton({ fullOpen = true }, Modifier.heightIn(min = 48.dp).testTag("amount-open-full")) {
                Text("查看完整金额", color = supportingColor)
            }
        }
    }
    if (fullOpen) AlertDialog(onDismissRequest = { fullOpen = false }, title = { Text("完整金额") },
        text = { Column(Modifier.verticalScroll(rememberScrollState())) {
            SelectionContainer {
                Text(display, Modifier.fillMaxWidth().heightIn(min = 48.dp).horizontalScroll(rememberScrollState())
                    .testTag("amount-full-value"), style = MaterialTheme.typography.titleLarge, softWrap = false, maxLines = 1)
            }
            Text(unit.label, Modifier.testTag("amount-full-unit"))
            Text("可左右滑动，或长按选择完整金额。", style = MaterialTheme.typography.bodyMedium)
            if (copyFailed) Text("暂时无法复制，可长按金额选择文字。", color = MaterialTheme.colorScheme.error)
        } }, confirmButton = { TextButton({ fullOpen = false }, Modifier.testTag("amount-close-full")) { Text("关闭") } },
        dismissButton = { TextButton({
            copyFailed = runCatching { clipboard.setText(AnnotatedString("$display ${unit.label}")) }.isFailure
        }, Modifier.testTag("amount-copy-full")) { Text("复制金额") } })
}
