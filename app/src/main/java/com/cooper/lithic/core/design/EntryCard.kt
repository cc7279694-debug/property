package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

/** A read-only identity card. A supplied cover is UI only; this component never opens media. */
@Composable
fun EntryCard(
    name: String,
    statusLabel: String,
    timeLabel: String,
    dailyCost: BigDecimal?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cover: (@Composable () -> Unit)? = null,
    category: String = "",
    kind: C2Glyph = C2Glyph.Project,
    hasCoverReference: Boolean = false,
    metricLabel: String = "日均成本",
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) {
            EntryContent(name, statusLabel, timeLabel, dailyCost, cover, category, kind, hasCoverReference, metricLabel)
        }
    } else {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) {
            EntryContent(name, statusLabel, timeLabel, dailyCost, cover, category, kind, hasCoverReference, metricLabel)
        }
    }
}

@Composable
private fun EntryContent(name: String, status: String, time: String, cost: BigDecimal?, cover: (@Composable () -> Unit)?,
    category: String, kind: C2Glyph, hasCoverReference: Boolean, metricLabel: String) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val amountStyle = MaterialTheme.typography.titleLarge
    val captionStyle = MaterialTheme.typography.bodySmall
    val amountWidth = with(density) {
        maxOf(measurer.measure(moneyMetricAnnotatedText(cost, AmountUnit.YUAN_PER_DAY,
                MaterialTheme.colorScheme.onSurfaceVariant), amountStyle, softWrap = false).size.width,
            measurer.measure(AnnotatedString(metricLabel), captionStyle, softWrap = false).size.width).toDp()
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
        val beside = density.fontScale <= 1.3f && maxWidth >= 64.dp + 12.dp + 112.dp + 12.dp + amountWidth
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    cover != null -> Box(Modifier.size(64.dp).clip(MaterialTheme.shapes.small)) { cover() }
                    hasCoverReference -> Surface(Modifier.size(64.dp), shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Box(contentAlignment = Alignment.Center) { C2Icon(C2Glyph.ImageError, description = "封面暂不可用") }
                    }
                    else -> C2RecordIdentity(name, category, kind)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    C2StatusChip(status)
                    if (category.isNotBlank()) Text(category, style = captionStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(time, style = captionStyle, color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                    if (hasCoverReference && cover == null) Text("封面暂不可用", style = captionStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (beside) Column(Modifier.width(amountWidth)) { EntryAmount(cost, metricLabel) }
            }
            if (!beside) EntryAmount(cost, metricLabel)
        }
    }
}

@Composable
private fun EntryAmount(cost: BigDecimal?, metricLabel: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(metricLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyMetric(cost, level = MoneyMetricLevel.ROW, label = metricLabel)
    }
}
