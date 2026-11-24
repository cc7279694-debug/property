package com.cooper.lithic.feature.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import com.cooper.lithic.core.design.C2StatusChip
import com.cooper.lithic.core.design.MoneyMetric
import com.cooper.lithic.core.design.MoneyMetricLevel
import com.cooper.lithic.core.design.moneyMetricFitsAtPreferredSize
import java.math.BigDecimal

/** Archive presentation only. Each screen supplies its own identity, metric and safe cover. */
@Composable
internal fun ArchiveEntryRow(
    name: String,
    status: String,
    dates: String,
    dailyCost: BigDecimal?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    category: String,
    metricLabel: String,
    cover: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = "查看详情", onClick = onClick)
            .heightIn(min = 76.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            val amountWidth = 112.dp
            val beside = maxWidth >= 320.dp && LocalDensity.current.fontScale <= 1.3f &&
                moneyMetricFitsAtPreferredSize(dailyCost, amountWidth, level = MoneyMetricLevel.ROW)
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(Modifier.size(64.dp, 48.dp).clip(RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                        cover()
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        C2StatusChip(status)
                        if (category.isNotBlank()) Text(category, style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant)
                        Text(dates, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (beside) ArchiveMetric(dailyCost, metricLabel, Modifier.width(amountWidth))
                    C2Icon(C2Glyph.Chevron, Modifier.size(16.dp).align(Alignment.CenterVertically))
                }
                if (!beside) ArchiveMetric(dailyCost, metricLabel, Modifier.fillMaxWidth())
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = colors.outlineVariant)
    }
}

@Composable
private fun ArchiveMetric(cost: BigDecimal?, label: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MoneyMetric(cost, level = MoneyMetricLevel.ROW, label = label, keepPreferredSize = true)
    }
}
