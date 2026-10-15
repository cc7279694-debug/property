package com.cooper.lithic.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import java.math.BigDecimal

@Composable
fun EntryCard(
    name: String,
    statusLabel: String,
    timeLabel: String,
    dailyCost: BigDecimal?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cover: (@Composable () -> Unit)? = null,
    placeholderIcon: LithicIcon = LithicIcon.Project,
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) {
            EntryContent(name, statusLabel, timeLabel, dailyCost, cover, placeholderIcon)
        }
    } else {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = colors) {
            EntryContent(name, statusLabel, timeLabel, dailyCost, cover, placeholderIcon)
        }
    }
}

@Composable
private fun EntryContent(name: String, statusLabel: String, timeLabel: String, dailyCost: BigDecimal?,
    cover: (@Composable () -> Unit)?, placeholderIcon: LithicIcon) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val amountStyle = MaterialTheme.typography.titleLarge
    val amountWidth = with(density) {
        maxOf(
            textMeasurer.measure(AnnotatedString(AmountFormatter.formatDailyRate(dailyCost)), style = amountStyle,
                softWrap = false).size.width,
            textMeasurer.measure(AnnotatedString(AmountUnit.YUAN_PER_DAY.label),
                style = MaterialTheme.typography.bodyMedium, softWrap = false).size.width,
        ).toDp()
    }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
        // Move the entire amount below when text needs room; never shrink or truncate a value.
        val sideAmount = density.fontScale <= 1.3f && maxWidth >= 64.dp + 12.dp + 128.dp + 12.dp + amountWidth
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.small)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LithicSpacing.small),
                verticalAlignment = Alignment.Top) {
                if (cover == null) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .semantics { contentDescription = "暂无封面" }, contentAlignment = Alignment.Center) {
                        LithicIcon(placeholderIcon, null)
                    }
                } else {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))) { cover() }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xSmall)) {
                    Text(name, style = MaterialTheme.typography.titleMedium, softWrap = true)
                    StatusBadge(statusLabel, subdued = statusLabel == "已结束" || statusLabel == "已停止")
                    Text(timeLabel, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
                }
                if (sideAmount) AmountText(dailyCost, Modifier.width(amountWidth), style = amountStyle)
            }
            if (!sideAmount) AmountText(dailyCost, style = amountStyle)
        }
    }
}
