package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics

/** A label makes consequence visible without relying on color or interpreting message wording. */
@Composable
internal fun UiFeedbackText(text: String, tone: UiMessageTone, tag: String, modifier: Modifier = Modifier) {
    val label = when (tone) {
        UiMessageTone.INFORMATION -> "提示"
        UiMessageTone.WARNING -> "需要核对"
        UiMessageTone.ERROR -> "遇到问题"
    }
    val color = when (tone) {
        UiMessageTone.INFORMATION -> MaterialTheme.colorScheme.onSurfaceVariant
        UiMessageTone.WARNING -> MaterialTheme.colorScheme.onSurface
        UiMessageTone.ERROR -> MaterialTheme.colorScheme.error
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LithicSpacing.xxSmall)) {
        Text(label, Modifier.testTag("$tag-kind"), style = MaterialTheme.typography.labelMedium, color = color)
        Text(text, Modifier.testTag(tag).semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.bodyMedium, color = color)
    }
}
