package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun settings_exposes_manual_complete_export_and_keeps_restore_closed() {
        var clicks = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), backupState = BackupExportUiState(), onExportBackup = { clicks += 1 })
        } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, clicks)
        // Keep the M11 test identity: a host without a restore callback cannot start an overwrite.
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun pending_export_disables_new_request_and_has_cancel_action() {
        var cancels = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), backupState = BackupExportUiState(phase = BackupUiPhase.EXPORTING),
                onExportBackup = {}, onCancelBackup = { cancels += 1 })
        } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("cancel-backup-export").performScrollTo().assertIsDisplayed().performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun failure_feedback_never_replaces_the_visible_export_action_with_success() {
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), backupState = BackupExportUiState(message = "备份未完成。保存位置可能有残留文件。"),
                onExportBackup = {})
        } }
        compose.onNodeWithTag("backup-export-feedback").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("完整备份已导出并通过校验。").assertDoesNotExist()
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsDisplayed()
    }
}
