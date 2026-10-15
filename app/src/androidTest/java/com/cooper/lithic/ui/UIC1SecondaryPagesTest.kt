package com.cooper.lithic.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.settings.SettingsShell
import com.cooper.lithic.feature.project.ProjectHistoryScreen
import com.cooper.lithic.feature.project.ProjectListUiState
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UIC1SecondaryPagesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun review_only_offers_real_history_routes_without_guessing_ledger_absence() {
        var projects = 0
        var subscriptions = 0
        compose.setContent { LithicTheme {
            ReviewShell(0, onProjectHistory = { projects++ }, onSubscriptionHistory = { subscriptions++ })
        } }
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        for (label in listOf("尚未开放", "长期复盘", "成本趋势", "类别分析", "年度回顾")) {
            compose.onNodeWithText(label).assertDoesNotExist()
        }
        compose.onNodeWithTag("review-project-history").assertHasClickAction().performClick()
        compose.onNodeWithTag("review-subscription-history").assertHasClickAction().performClick()
        assertEquals(1, projects)
        assertEquals(1, subscriptions)
    }

    @Test fun settings_exposes_installed_version_and_persisted_zone_as_read_only_information() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        @Suppress("DEPRECATION")
        val installed = context.packageManager.getPackageInfo(context.packageName, 0)
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("Asia/Shanghai")) } }
        compose.onNodeWithTag("record-zone").assertTextEquals("Asia/Shanghai")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.onNodeWithTag("installed-version").performScrollTo()
            .assertTextEquals(requireNotNull(installed.versionName))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        compose.onNodeWithText("关于").performScrollTo().assertIsDisplayed()
    }

    @Test fun settings_keeps_backup_warning_prominent_and_has_no_unimplemented_reminder_entry() {
        compose.setContent { LithicTheme { SettingsShell(ZoneId.of("UTC")) } }
        compose.onNodeWithTag("development-warning").assertIsDisplayed()
        compose.onNodeWithText("完整备份与恢复尚未开放").assertIsDisplayed()
        compose.onNodeWithText("提醒功能尚未开放").assertDoesNotExist()
        compose.onNodeWithText("记录与数据").assertIsDisplayed()
    }

    @Test fun project_history_loading_or_read_failure_never_claims_a_successful_empty_archive() {
        var state by mutableStateOf(ProjectListUiState(loading = true))
        var retries = 0
        compose.setContent { LithicTheme {
            ProjectHistoryScreen(state, {}, {}, {}, { retries++ })
        } }
        compose.onNodeWithText("正在读取历史档案").assertIsDisplayed()
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(loading = false, error = "无法读取历史档案") }
        compose.onNodeWithTag("project-history-error").assertTextEquals("无法读取历史档案")
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        compose.onNodeWithTag("project-history-retry").performClick()
        assertEquals(1, retries)
        compose.runOnIdle { state = state.copy(error = null, totalCount = 0) }
        compose.onNodeWithText("暂无已结束项目").assertIsDisplayed()
    }
}
