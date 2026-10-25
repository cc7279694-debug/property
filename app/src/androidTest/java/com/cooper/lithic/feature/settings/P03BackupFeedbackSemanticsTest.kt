package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Presentation fixtures, not SAF or real generation-switch execution. */
@RunWith(AndroidJUnit4::class)
class P03BackupFeedbackSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun verified_success_is_information_without_treating_idle_as_success() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            backupState = BackupExportUiState(message = "完整备份已导出并通过校验。",
                feedback = BackupExportFeedback.VERIFIED_SUCCESS)) } }
        compose.onNodeWithTag("backup-export-feedback-kind").performScrollTo().assertTextEquals("提示")
        compose.onNodeWithTag("backup-not-started").assertDoesNotExist()
    }
    @Test fun cancellation_with_target_residue_warns_that_file_is_not_restore_input() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            backupState = BackupExportUiState(message = "导出已取消；残留文件请勿用于恢复。",
                feedback = BackupExportFeedback.CANCELLED, partialTargetRemaining = true)) } }
        compose.onNodeWithTag("backup-export-feedback-kind").performScrollTo().assertTextEquals("需要核对")
        compose.onNodeWithTag("backup-export-feedback").performScrollTo().assertTextContains("请勿用于恢复", substring = true)
    }
    @Test fun preserved_restore_failure_is_error_and_not_success() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            restoreState = BackupRestoreUiState(message = "恢复未完成，当前记录已保留。",
                feedback = RestoreFeedbackKind.FAILED_CURRENT_PRESERVED)) } }
        compose.onNodeWithTag("backup-restore-feedback-kind").performScrollTo().assertTextEquals("遇到问题")
    }
    @Test fun unknown_restore_warns_without_pretending_old_data_was_kept() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            restoreState = BackupRestoreUiState(message = "恢复结果暂无法确认，请勿清除应用数据。",
                feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN)) } }
        compose.onNodeWithTag("backup-restore-feedback-kind").performScrollTo().assertTextEquals("需要核对")
        compose.onNodeWithTag("backup-restore-feedback").performScrollTo().assertTextContains("暂无法确认", substring = true)
    }
}
