package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Presentation fixtures only; runtime, SAF, switching and device permission are NOT_RUN. */
class C2B4DataManagementTest {
    @get:Rule val compose = createComposeRule()
    @Test fun restore_in_progress_disables_both_new_data_operations() {
        var exports = 0; var restores = 0
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            onExportBackup = { exports++ }, onRestoreBackup = { restores++ },
            restoreState = BackupRestoreUiState(phase = RestoreUiPhase.RESTORING)) } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        assertEquals(0, exports); assertEquals(0, restores)
    }
    @Test fun unknown_result_is_persistent_and_does_not_become_success() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            restoreState = BackupRestoreUiState(message = "结果暂无法确认，请勿清除应用数据。", feedback = RestoreFeedbackKind.OUTCOME_UNKNOWN)) } }
        compose.onNodeWithTag("backup-restore-feedback-kind").performScrollTo().assertTextEquals("需要核对")
        compose.onNodeWithTag("backup-restore-feedback").performScrollTo().assertTextContains("暂无法确认", substring = true)
        compose.onNodeWithText("恢复成功").assertDoesNotExist()
    }
    @Test fun grouped_settings_keep_read_only_zone_and_real_navigation_callbacks() {
        var categories = 0
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"), onManageCategories = { categories++ }) } }
        compose.onNodeWithTag("record-zone", useUnmergedTree = true).assertTextEquals("Asia/Shanghai")
        compose.onNodeWithTag("open-category-management").performClick()
        assertEquals(1, categories)
        compose.onNodeWithTag("open-about-lithic").assertDoesNotExist()
    }
}
