package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Component presentation only; not a SAF, restore-switch or Android runtime PASS. */
class C4SettingsDensitySemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinary_help_is_on_demand_and_never_starts_a_data_operation() {
        var exports = 0
        var restores = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = { exports++ }, onRestoreBackup = { restores++ })
        } }
        compose.onNodeWithTag("backup-not-started").assertDoesNotExist()
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertTextContains("文件未加密", substring = true)
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertTextContains("完整覆盖，不合并", substring = true)
        compose.onNodeWithTag("settings-information").performScrollTo().performClick()
        compose.onNodeWithTag("backup-not-started").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("settings-information-close").performClick()
        compose.onNodeWithTag("backup-not-started").assertDoesNotExist()
        assertEquals(0, exports)
        assertEquals(0, restores)
    }

    @Test fun unknown_and_partial_target_warnings_remain_visible_without_opening_help() {
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"),
                backupState = BackupExportUiState(message = "导出已取消；残留文件请勿用于恢复。",
                    feedback = BackupExportFeedback.CANCELLED, partialTargetRemaining = true),
                restoreState = BackupRestoreUiState(message = "恢复结果暂无法确认，请勿清除应用数据。",
                    feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN))
        } }
        compose.onNodeWithTag("backup-export-feedback").performScrollTo().assertTextContains("请勿用于恢复", substring = true)
        compose.onNodeWithTag("backup-restore-feedback").performScrollTo().assertTextContains("暂无法确认", substring = true)
        compose.onNodeWithTag("settings-information-content").assertDoesNotExist()
        compose.onNodeWithText("恢复成功").assertDoesNotExist()
    }
}
