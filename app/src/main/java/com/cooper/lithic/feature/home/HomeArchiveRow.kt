package com.cooper.lithic.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.LocalLithicVisuals
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.c2IdentityGlyph
import com.cooper.lithic.core.design.moneyMetricAnnotatedText
import com.cooper.lithic.core.design.moneyMetricFontCandidates
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.feature.media.RecordCover
import java.math.BigDecimal

@Composable
internal fun HomeArchiveRow(
    name: String,
    category: String,
    icon: C2Glyph,
    status: String,
    time: String,
    metric: String,
    amount: BigDecimal?,
    coverReference: CoverReference,
    tag: String,
    onOpen: (() -> Unit)?,
    spokenMetric: String = metric,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val supporting = MaterialTheme.colorScheme.onSurfaceVariant
    val metricStyle = MaterialTheme.typography.bodySmall
    val preferredSp = moneyMetricFontCandidates(MoneyMetricLevel.ROW).first()
    val amountStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = preferredSp.sp, lineHeight = (preferredSp + 8).sp,
    )
    val amountWidth = with(density) {
        maxOf(
            measurer.measure(moneyMetricAnnotatedText(amount, AmountUnit.YUAN_PER_DAY, supporting),
                amountStyle, softWrap = false, maxLines = 1).size.width,
            measurer.measure(AnnotatedString(metric), metricStyle, softWrap = false, maxLines = 1).size.width,
        ).toDp()
    }
    val gap = 12.dp
    val factsMinWidth = 112.dp
    Column(Modifier.fillMaxWidth()) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            val coverWidth = if (coverReference.assetId == null) 48.dp
                else (maxWidth - gap - factsMinWidth).coerceIn(64.dp, 80.dp)
            val beside = density.fontScale <= 1.3f &&
                maxWidth >= coverWidth + gap + factsMinWidth + gap + amountWidth
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.Top) {
                if (coverReference.assetId == null) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        C2Icon(c2IdentityGlyph(category) ?: icon)
                    }
                } else {
                    // Keep this size independent of the text reflow and the error action separate from opening details.
                    RecordCover(
                        coverReference,
                        Modifier.size(coverWidth, coverWidth * 0.75f)
                            .background(LocalLithicVisuals.current.identitySurface),
                        contentScale = ContentScale.Fit,
                    ) { C2Icon(c2IdentityGlyph(category) ?: icon) }
                }
                Column(
                    Modifier.weight(1f).heightIn(min = 48.dp)
                        .then(if (onOpen != null) Modifier.clickable(role = Role.Button, onClick = onOpen) else Modifier)
                        .testTag(tag),
                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                ) {
                    if (beside) {
                        Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                            ArchiveFacts(name, category, status, time, Modifier.weight(1f))
                            ArchiveCost(amount, metric, spokenMetric, Modifier.width(amountWidth), Alignment.End)
                        }
                    } else {
                        ArchiveFacts(name, category, status, time)
                        ArchiveCost(amount, metric, spokenMetric)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ArchiveFacts(name: String, category: String, status: String, time: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val style = MaterialTheme.typography.bodySmall
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(category, style = style, color = color)
            Text(status, style = style, color = color, fontWeight = FontWeight.Medium)
            Text(time, style = style, color = color)
        }
    }
}

@Composable
private fun ArchiveCost(
    amount: BigDecimal?,
    metric: String,
    spokenMetric: String,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = alignment) {
        MoneyMetric(amount, level = MoneyMetricLevel.ROW, label = spokenMetric)
        // The money component supplies the combined spoken metric and value.
        Text(metric, Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
