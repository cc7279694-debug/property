package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.backup.BackupInspectionResult
import com.cooper.lithic.data.backup.BackupInspectionStatus
import com.cooper.lithic.data.backup.BackupInspectionSummary
import com.cooper.lithic.data.media.PhotoStorageReadFailure
import com.cooper.lithic.data.media.PhotoStorageSummary
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production presentation fixtures only. NOT_RUN; future execution needs an isolated plain host. */
@RunWith(AndroidJUnit4::class)
class DataConfidencePresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun a_verified_file_is_not_presented_as_a_completed_restore() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            inspectionState = BackupInspectionUiState(result = BackupInspectionResult(BackupInspectionStatus.VERIFIED,
                BackupInspectionSummary(4, "2026-11-01T00:00:00Z", mapOf("long_term_projects" to 1L, "subscriptions" to 1L), 2, 256)),
                message = "VERIFIED · 文件符合当前完整性规则。")) } }
        compose.onNodeWithTag("backup-inspection-summary").performScrollTo().assertExists()
        compose.onNodeWithTag("backup-inspection-limits").performScrollTo().assertTextContains("实际恢复尚未验证", substring = true)
        compose.onNodeWithTag("backup-restore-feedback").assertDoesNotExist()
    }

    @Test fun inspection_blocks_export_restore_and_duplicate_inspection_controls() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            inspectionState = BackupInspectionUiState(BackupInspectionPhase.CHECKING, "synthetic-request"),
            onExportBackup = {}, onRestoreBackup = {}, onInspectBackup = {}) } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("inspect-backup-file").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("cancel-backup-inspection").performScrollTo().assertIsEnabled()
    }

    @Test fun photo_counts_are_metadata_estimates_with_retention_disclosure() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            photoStorageState = PhotoStorageUiState.Ready(PhotoStorageSummary(5, 2, 3, 1234, 345))) } }
        compose.onNodeWithTag("photo-storage-registered").performScrollTo().assertTextEquals("5 张")
        compose.onNodeWithTag("photo-storage-referenced").performScrollTo().assertTextEquals("2 张")
        compose.onNodeWithTag("photo-storage-historical").performScrollTo().assertTextEquals("3 张")
        compose.onNodeWithTag("photo-storage-bytes").performScrollTo().assertTextEquals("1234 字节")
        compose.onNodeWithTag("photo-storage-historical-bytes").performScrollTo().assertTextEquals("345 字节")
        compose.onNodeWithTag("photo-storage-cleanup").assertDoesNotExist()
        compose.onNodeWithTag("photo-storage-cleanup-unavailable").performScrollTo()
            .assertTextContains("实际磁盘占用、可释放空间有差异", substring = true)
            .assertTextContains("暂不支持逐张查看或清理", substring = true)
        compose.onNodeWithTag("photo-storage-estimate-note").performScrollTo().assertTextContains("按登记长度估算", substring = true)
        compose.onNodeWithTag("photo-storage-retention-note").performScrollTo().assertTextContains("纳入完整备份", substring = true)
    }

    @Test fun unavailable_statistics_never_render_zero_counts_or_private_paths() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            photoStorageState = PhotoStorageUiState.Unavailable(PhotoStorageReadFailure.READ_FAILED),
            inspectionState = BackupInspectionUiState(result = BackupInspectionResult(BackupInspectionStatus.UNKNOWN),
                message = "UNKNOWN · 文件完整性尚未确认。")) } }
        compose.onNodeWithTag("photo-storage-unavailable").performScrollTo().assertExists()
        compose.onNodeWithTag("photo-storage-registered").assertDoesNotExist()
        compose.onNodeWithTag("photo-storage-bytes").assertDoesNotExist()
        compose.onNodeWithTag("photo-storage-historical-bytes").assertDoesNotExist()
        compose.onNodeWithTag("photo-storage-cleanup").assertDoesNotExist()
        compose.onAllNodes(hasText("/data/user/", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("content://", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("backup-inspection-summary").assertDoesNotExist()
    }
}
