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
import com.cooper.lithic.data.backup.RestorePreview
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupRestoreSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun restore_preview_shows_current_and_backup_counts_before_explicit_overwrite() {
        var confirmed = 0
        var cancelled = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {}, onRestoreBackup = {},
                restoreState = BackupRestoreUiState(RestoreUiPhase.PREVIEW, preview()),
                onConfirmRestore = { confirmed += 1 }, onDiscardRestore = { cancelled += 1 })
        } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("完整覆盖当前记录，不合并。当前记录、图片和设置将被备份内容替换。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("restore-count-long_term_projects").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("当前 1 · 备份 3").performScrollTo().assertIsDisplayed()
        assertEquals(0, confirmed)
        compose.onNodeWithTag("confirm-backup-restore").performScrollTo().performClick()
        assertEquals(1, confirmed)
        compose.onNodeWithTag("discard-backup-restore").performScrollTo().performClick()
        assertEquals(1, cancelled)
    }

    @Test fun an_export_in_progress_disables_restore_selection() {
        var restores = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"),
                backupState = BackupExportUiState(BackupUiPhase.EXPORTING), onExportBackup = {},
                onRestoreBackup = { restores += 1 })
        } }
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        assertEquals(0, restores)
    }

    @Test fun validation_cannot_confirm_and_an_active_switch_cannot_be_cancelled_or_repeated() {
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {}, onRestoreBackup = {},
                restoreState = BackupRestoreUiState(RestoreUiPhase.RESTORING), onConfirmRestore = {})
        } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("confirm-backup-restore").assertDoesNotExist()
        compose.onNodeWithTag("discard-backup-restore").assertDoesNotExist()
        compose.onNodeWithTag("cancel-backup-restore").assertDoesNotExist()
    }

    @Test fun restore_failure_feedback_retains_manual_actions_without_claiming_success() {
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {}, onRestoreBackup = {},
                restoreState = BackupRestoreUiState(message = "恢复未完成。当前记录已保留。"))
        } }
        compose.onNodeWithTag("backup-restore-feedback").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("恢复成功，已重新读取恢复后的本地记录。").assertDoesNotExist()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsDisplayed()
    }

    private fun preview() = RestorePreview("private-validated-token", mapOf(
        "categories" to 2, "long_term_projects" to 3, "project_money_events" to 4,
        "local_media_assets" to 1, "subscriptions" to 2, "subscription_renewals" to 3,
        "subscription_state_changes" to 1).mapValues { it.value.toLong() }, mapOf(
        "categories" to 2, "long_term_projects" to 1, "project_money_events" to 1,
        "local_media_assets" to 0, "subscriptions" to 0, "subscription_renewals" to 0,
        "subscription_state_changes" to 0).mapValues { it.value.toLong() },
        "2026-10-20T00:00:00Z", "Asia/Shanghai")
}
