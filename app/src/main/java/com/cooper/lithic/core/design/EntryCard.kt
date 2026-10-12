package com.cooper.lithic.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
) {
    if (onClick == null) {
        Card(modifier = modifier.fillMaxWidth()) { EntryContent(name, statusLabel, timeLabel, dailyCost, cover) }
    } else {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth()) { EntryContent(name, statusLabel, timeLabel, dailyCost, cover) }
    }
}

@Composable
private fun EntryContent(name: String, statusLabel: String, timeLabel: String, dailyCost: BigDecimal?, cover: (@Composable () -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(LithicSpacing.medium)) {
        Row {
            if (cover == null) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .semantics { contentDescription = "暂无封面" })
            } else {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))) { cover() }
            }
            Spacer(Modifier.width(LithicSpacing.small))
            Text(name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, softWrap = true)
        }
        Spacer(Modifier.height(LithicSpacing.xSmall))
        Text(statusLabel, style = MaterialTheme.typography.bodyMedium)
        Text(timeLabel, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, softWrap = true)
        Spacer(Modifier.height(LithicSpacing.xSmall))
        AmountText(dailyCost, style = MaterialTheme.typography.titleLarge)
    }
}
