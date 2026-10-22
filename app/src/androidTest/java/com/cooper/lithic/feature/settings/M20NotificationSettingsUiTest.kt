package com.cooper.lithic.feature.settings

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.settings.ReminderPreferences
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Plain Application + ComponentActivity only; no personal ledger or system notification is opened. */
@RunWith(AndroidJUnit4::class)
class M20NotificationSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Before fun requireIsolatedApplication() {
        assertEquals(Application::class.java,
            ApplicationProvider.getApplicationContext<Context>().applicationContext.javaClass)
    }

    @Test fun default_switches_are_independent_of_denied_permission_and_request_only_on_click() {
        var requests = 0; var renewals = 0; var milestones = 0
        compose.setContent { LithicTheme {
            NotificationSettingsContent(ready(), denied(), { renewals++ }, { milestones++ },
                { requests++ }, {}, {})
        } }
        compose.onNodeWithTag("renewal-reminder-switch").performScrollTo().assertIsOn()
        compose.onNodeWithTag("milestone-reminder-switch").performScrollTo().assertIsOn()
        compose.onNodeWithText("当前仅保存开关偏好，里程碑通知尚未提供。").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("应用内已开启，系统通知未授权").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, requests); assertEquals(0, renewals); assertEquals(0, milestones) }
        compose.onNodeWithTag("renewal-reminder-switch").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, renewals); assertEquals(0, requests) }
        compose.onNodeWithTag("request-notification-permission").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }

    @Test fun api26_to32_uses_system_availability_without_runtime_permission_request() {
        var settings = 0; var requests = 0
        compose.setContent { LithicTheme {
            NotificationSettingsContent(ready(), NotificationPermissionState(false, false, false), {}, {},
                { requests++ }, { settings++ }, {})
        } }
        compose.onNodeWithTag("request-notification-permission").assertDoesNotExist()
        compose.onNodeWithText("系统通知已关闭").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("应用内已开启，系统通知已关闭").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("open-system-notification-settings").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, settings); assertEquals(0, requests) }
    }

    @Test fun channel_blocked_and_runtime_granted_remains_explicitly_unavailable() {
        compose.setContent { LithicTheme {
            NotificationSettingsContent(ready(), NotificationPermissionState(true, true, true, false), {}, {}, {}, {}, {})
        } }
        compose.onNodeWithTag("request-notification-permission").assertDoesNotExist()
        compose.onNodeWithText("续费提醒通知渠道已关闭").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("系统允许发送通知").assertDoesNotExist()
    }

    @Test fun save_pending_disables_both_switches_without_claiming_saved_success() {
        var writes = 0
        compose.setContent { LithicTheme {
            NotificationSettingsContent(ready().copy(saving = true), denied(), { writes++ }, { writes++ }, {}, {}, {})
        } }
        compose.onNodeWithTag("renewal-reminder-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("milestone-reminder-switch").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("notification-preferences-saving").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("notification-preferences-notice").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, writes) }
    }

    @Test fun unreadable_preferences_expose_retry_and_no_editable_default_switches() {
        var retries = 0; var writes = 0
        compose.setContent { LithicTheme {
            NotificationSettingsContent(NotificationSettingsUiState(loading = false, error = "合成读取失败"), denied(),
                { writes++ }, { writes++ }, {}, {}, {}, { retries++ })
        } }
        compose.onNodeWithTag("renewal-reminder-switch").assertDoesNotExist()
        compose.onNodeWithTag("milestone-reminder-switch").assertDoesNotExist()
        compose.onNodeWithTag("notification-permission-explanation").assertDoesNotExist()
        compose.onNodeWithTag("retry-notification-preferences").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, retries); assertEquals(0, writes) }
    }

    @Test fun settings_entry_preserves_manual_backup_actions_and_uses_explicit_navigation() {
        var opened = 0; var exports = 0; var restores = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onExportBackup = { exports++ },
                onRestoreBackup = { restores++ }, onNotificationClick = { opened++ })
        } }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().performClick()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().performClick()
        compose.onNodeWithTag("open-notification-settings").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, opened); assertEquals(1, exports); assertEquals(1, restores) }
    }

    @Test fun narrow360dp_local200percent_text_keeps_switch_permission_and_back_targets_accessible() {
        var back = 0; var request = 0
        var canvasDensity = 1f
        compose.setContent { LithicTheme {
            canvasDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(canvasDensity, 2f)) {
                Box(Modifier.width(360.dp).testTag("m20-notification-test-canvas")) {
                    NotificationSettingsContent(ready(), denied(), {}, {}, { request++ }, {}, { back++ })
                }
            }
        } }
        listOf("renewal-reminder-switch", "milestone-reminder-switch", "request-notification-permission",
            "open-system-notification-settings", "notification-settings-back").forEach { tag ->
            val node = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            val canvas = compose.onNodeWithTag("m20-notification-test-canvas").fetchSemanticsNode().boundsInRoot
            assertTrue("$tag must provide 48dp height", bounds.height >= 48f * canvasDensity - 1f)
            assertTrue("$tag left edge", bounds.left >= canvas.left - 1f)
            assertTrue("$tag right edge", bounds.right <= canvas.right + 1f)
        }
        compose.onNodeWithTag("request-notification-permission").performScrollTo().performClick()
        compose.onNodeWithTag("notification-settings-back").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, request); assertEquals(1, back) }
    }

    private fun ready() = NotificationSettingsUiState(loading = false, preferences = ReminderPreferences())
    private fun denied() = NotificationPermissionState(true, false, false)
}
