package com.cooper.lithic.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.C2PageHeader
import com.cooper.lithic.core.design.LithicSpacing

/** Safe fallback for a previously restored reminder route; it does not access preferences. */
@Composable
internal fun NotificationDeferredScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = LithicSpacing.page, vertical = 16.dp)
        .testTag("notification-deferred-screen")) {
        C2PageHeader("提醒", onBack, "notification-deferred-back", backLabel = "返回设置")
        Text("自动提醒将在 V1.1 提供。", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
