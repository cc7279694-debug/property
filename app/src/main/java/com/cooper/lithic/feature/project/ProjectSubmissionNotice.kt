package com.cooper.lithic.feature.project

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

/** Explains the existing phase; submission identity and replay remain owned by ProjectViewModel. */
@Composable
internal fun ProjectSubmissionNotice(phase: SubmissionPhase, tagPrefix: String) {
    if (phase == SubmissionPhase.IDLE) return
    Column(Modifier.fillMaxWidth().testTag("$tagPrefix-submission-status")
        .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        when (phase) {
            SubmissionPhase.IN_FLIGHT -> Text("正在保存到本地", style = MaterialTheme.typography.bodyLarge)
            SubmissionPhase.UNCONFIRMED -> Text(
                "保存结果尚未确认，输入和原提交已保留。在结果确认前，不要另建相同记录。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SubmissionPhase.COMMITTED -> Text(
                "记录已保存。重新读取只更新显示，不会再次保存。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SubmissionPhase.IDLE -> Unit
        }
    }
}
