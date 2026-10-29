package com.cooper.lithic.core.design

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.cooper.lithic.core.design.LithicTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

/** Complete display/copy text. The formatter remains the only rounding authority. */
fun moneyMetricDisplay(amount: BigDecimal?, unit: AmountUnit = AmountUnit.YUAN_PER_DAY): String {
    val value = when (unit) {
        AmountUnit.YUAN -> AmountFormatter.formatCny(amount)
        AmountUnit.YUAN_PER_DAY -> AmountFormatter.formatDailyRate(amount)
    }
    return value + if (amount != null && unit == AmountUnit.YUAN_PER_DAY) "/天" else ""
}

/** One spoken readout, including the caller's original metric identity when supplied. */
fun moneyMetricDescription(
    amount: BigDecimal?,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    label: String? = null,
): String = listOfNotNull(label?.trim()?.takeIf { it.isNotEmpty() }, AmountFormatter.description(amount, unit))
    .joinToString("，")

/** Shared with responsive parent layouts so their width probe matches the rendered group. */
internal fun moneyMetricAnnotatedText(amount: BigDecimal?, unit: AmountUnit, supportingColor: Color): AnnotatedString {
    val fullDisplay = moneyMetricDisplay(amount, unit)
    val hasDailySuffix = amount != null && unit == AmountUnit.YUAN_PER_DAY
    return buildAnnotatedString {
        append(if (hasDailySuffix) fullDisplay.removeSuffix("/天") else fullDisplay)
        if (hasDailySuffix) withStyle(SpanStyle(fontSize = 14.sp, color = supportingColor)) {
            // A thin typographic gap keeps one baseline and one unbreakable Text run.
            // Plain display/copy text stays compact and contains no separator glyph.
            append("\u2009/天")
        }
    }
}

/** Parent groups stack before the individual metric reduces its preferred role size. */
@Composable
internal fun moneyMetricFitsAtPreferredSize(
    amount: BigDecimal?,
    availableWidth: Dp,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    level: MoneyMetricLevel = MoneyMetricLevel.ROW,
): Boolean {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val run = moneyMetricAnnotatedText(amount, unit, MaterialTheme.colorScheme.onSurfaceVariant)
    val preferredSp = moneyMetricFontCandidates(level).first()
    val measured = measurer.measure(run, style = MaterialTheme.typography.displayLarge.copy(
        fontSize = preferredSp.sp, lineHeight = (preferredSp + 8).sp,
    ), softWrap = false, maxLines = 1)
    return measured.size.width <= with(density) { availableWidth.toPx() }
}

/**
 * Read-only money presentation; it never alters the supplied value or user font scale.
 * [label] is the metric identity for the combined spoken readout only. The caller keeps
 * responsibility for its visible label and spacing; this component does not draw it again.
 */
@Composable
fun MoneyMetric(
    amount: BigDecimal?,
    modifier: Modifier = Modifier,
    unit: AmountUnit = AmountUnit.YUAN_PER_DAY,
    level: MoneyMetricLevel = MoneyMetricLevel.ROW,
    color: Color = MaterialTheme.colorScheme.onSurface,
    supportingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    label: String? = null,
) {
    val display = moneyMetricDisplay(amount, unit)
    var fullOpen by remember(display, unit) { mutableStateOf(false) }
    var copyFailed by remember(display, unit) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current

    MoneyMetricReadout(amount, modifier, unit, level, color, supportingColor, label) {
        copyFailed = false
        fullOpen = true
    }
    if (fullOpen) AlertDialog(
        onDismissRequest = { fullOpen = false },
        title = { Text("完整金额") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SelectionContainer {
                    // The historical full-value tag now represents the entire amount/unit group.
                    MoneyMetricReadout(amount, Modifier.testTag("amount-full-value"), unit,
                        MoneyMetricLevel.CARD, MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurfaceVariant, label)
                }
                Text("可左右滑动，或长按选择完整金额。", style = MaterialTheme.typography.bodyMedium)
                if (copyFailed) Text("暂时无法复制，可长按金额选择文字。", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton({ fullOpen = false }, Modifier.heightIn(min = 48.dp).testTag("amount-close-full")) { Text("关闭") }
        },
        dismissButton = {
            TextButton({
                copyFailed = runCatching { clipboard.setText(AnnotatedString(display)) }.isFailure
            }, Modifier.heightIn(min = 48.dp).testTag("amount-copy-full")) { Text("复制金额") }
        },
    )
}

@Composable
private fun MoneyMetricReadout(
    amount: BigDecimal?,
    modifier: Modifier,
    unit: AmountUnit,
    level: MoneyMetricLevel,
    color: Color,
    supportingColor: Color,
    label: String?,
    onOpenFull: (() -> Unit)? = null,
) {
    val fullDisplay = moneyMetricDisplay(amount, unit)
    val readout = remember(fullDisplay, unit, supportingColor) {
        moneyMetricAnnotatedText(amount, unit, supportingColor)
    }
    val spokenReadout = moneyMetricDescription(amount, unit, label)
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.displayLarge
    var textLayout by remember(readout, level) { mutableStateOf<TextLayoutResult?>(null) }

    BoxWithConstraints(modifier.semantics(mergeDescendants = true) {
        contentDescription = spokenReadout
        // One semantic value node retains the exact composite for text-based assertions.
        // The drawn child has no separate reading, so neither value nor unit is duplicated.
        text = AnnotatedString(fullDisplay)
    }) {
        val available = constraints.maxWidth
        val layout = remember(readout, level, available, density.fontScale, density.density, measurer, baseStyle) {
            amountLayout(level, available) { size ->
                measurer.measure(readout, baseStyle.copy(fontSize = size.sp, lineHeight = (size + 8).sp),
                    softWrap = false, maxLines = 1).size.width
            }
        }
        Column {
            val line: @Composable () -> Unit = {
                Text(readout,
                    // The parent supplies the sole spoken value. Retain layout inspection without
                    // exposing a second text/unit reading to accessibility services.
                    modifier = Modifier.clearAndSetSemantics {
                        getTextLayoutResult { results ->
                            val result = textLayout
                            if (result != null) results.add(result) else false
                        }
                    },
                    style = baseStyle.copy(fontSize = layout.fontSp.sp, lineHeight = (layout.fontSp + 8).sp),
                    color = color, softWrap = false, maxLines = 1,
                    onTextLayout = { textLayout = it })
            }
            if (layout.horizontalScroll) {
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .horizontalScroll(rememberScrollState()).testTag("amount-horizontal-view")) { line() }
                Text("左右滑动查看完整金额", Modifier.testTag("amount-horizontal-hint").clearAndSetSemantics { },
                    style = MaterialTheme.typography.bodySmall, color = supportingColor)
                if (onOpenFull != null) {
                    TextButton(onOpenFull, Modifier.heightIn(min = 48.dp).testTag("amount-open-full")) {
                        Text("查看完整金额", color = supportingColor)
                    }
                }
            } else line()
        }
    }
}
