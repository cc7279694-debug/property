package com.cooper.lithic.feature.settings

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Component fixture only. Runtime PackageManager and full navigation require separate device evidence. */
@RunWith(AndroidJUnit4::class)
class P03SettingsInformationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settings_exposes_both_real_information_callbacks() {
        var calculation = 0; var about = 0
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"),
            onCalculationExplanation = { calculation++ }, onAboutLithic = { about++ }) } }
        compose.onNodeWithTag("open-calculation-explanation").performScrollTo().performClick()
        compose.onNodeWithTag("open-about-lithic").performScrollTo().performClick()
        assertEquals(1, calculation); assertEquals(1, about)
    }
    @Test fun calculation_page_explains_current_contribution_and_retains_back_action() {
        var back = 0
        compose.setContent { LithicTheme { CalculationExplanationScreen { back++ } } }
        compose.onNodeWithText("它不等于今天实际扣款，也不代表全部生活消费。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("information-back").performScrollTo().performClick()
        assertEquals(1, back)
    }
    @Test fun about_displays_supplied_actual_label_and_version_without_private_paths() {
        compose.setContent { LithicTheme { AboutLithicScreen(InstalledAppInformation("砺石·体验版", "x-preview", 42,
            "com.cooper.lithic.preview.debug", true), {}) } }
        compose.onNodeWithTag("installed-app-name").assertTextEquals("砺石·体验版")
        compose.onNodeWithTag("installed-app-version").assertTextEquals("x-preview（版本Code 42）")
        compose.onNodeWithTag("installed-app-build").assertTextContains("非正式Release", substring = true)
    }
    @Test fun unknown_package_information_never_falls_back_to_historical_version() {
        compose.setContent { LithicTheme { AboutLithicScreen(InstalledAppInformation(), {}) } }
        compose.onNodeWithTag("installed-app-version").assertTextEquals("版本信息暂无法读取")
        compose.onNodeWithTag("installed-app-build").assertTextEquals("构建身份暂无法确认")
    }
    @Test fun calculation_remains_a_complete_reading_page_without_disclosure_controls() {
        compose.setContent { LithicTheme { CalculationExplanationScreen {} } }
        calculationExplanation.flatMap { it.paragraphs }.forEach { paragraph ->
            compose.onNodeWithText(paragraph).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun about_keeps_all_local_data_backup_and_reminder_boundaries_in_the_reading_page() {
        compose.setContent { LithicTheme { AboutLithicScreen(InstalledAppInformation("砺石·体验版", "c5-fixture", 42,
            "com.cooper.lithic.preview.debug", true), {}) } }
        productSafetyExplanation.flatMap { it.paragraphs }.forEach { paragraph ->
            compose.onNodeWithText(paragraph).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithTag("installed-app-version").performScrollTo().assertTextEquals("c5-fixture（版本Code 42）")
    }
    @Test fun idle_backup_is_not_presented_as_completed() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {}) } }
        compose.onNodeWithTag("backup-not-started").assertDoesNotExist()
        compose.onNodeWithTag("settings-information").performClick()
        compose.onNodeWithTag("backup-not-started").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("backup-export-feedback").assertDoesNotExist()
    }
    @Test fun active_restore_keeps_existing_mutual_exclusion_and_has_no_confirmation_button() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = {},
            onRestoreBackup = {}, restoreState = BackupRestoreUiState(RestoreUiPhase.RESTORING)) } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("confirm-backup-restore").assertDoesNotExist()
    }
}
