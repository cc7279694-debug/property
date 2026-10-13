package com.cooper.lithic.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.subscription.SubscriptionViewModel

/** Normal navigation only exists after both real storage reads have succeeded. */
@Composable
internal fun LithicApp(state: LedgerUiState, onExit: () -> Unit, projectViewModel: ProjectViewModel? = null, onRetryLedger: () -> Unit = {},
    subscriptionViewModel: SubscriptionViewModel? = null) {
    when (state) {
        is LedgerUiState.Ready -> LithicNavigation(state, projectViewModel = projectViewModel, onRetryLedger = onRetryLedger, subscriptionViewModel = subscriptionViewModel)
        LedgerUiState.Initializing -> StorageGate("storage_loading") {
            Text("正在读取本地记录", style = MaterialTheme.typography.headlineLarge)
            Text("请稍候，读取完成后会显示你的记录。", style = MaterialTheme.typography.bodyLarge)
        }
        LedgerUiState.Failed -> StorageGate("storage_error") {
            Text("本地数据暂时无法读取。", style = MaterialTheme.typography.headlineLarge)
            Text("为保护已有记录，应用不会自动创建空账本。请不要卸载应用或清除应用数据。",
                style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("storage_exit")) {
                Text("退出应用")
            }
        }
    }
}

@Composable
private fun StorageGate(tag: String, content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().testTag(tag)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) { content() }
    }
}
