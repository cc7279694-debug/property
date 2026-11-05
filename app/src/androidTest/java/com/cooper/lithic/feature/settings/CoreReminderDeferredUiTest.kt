package com.cooper.lithic.feature.settings

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compile-only until an independently authorized Android execution. */
@RunWith(AndroidJUnit4::class)
class CoreReminderDeferredUiTest {
    @get:Rule val compose = createComposeRule()

    @Before fun requireIsolatedApplication() {
        assertEquals(Application::class.java,
            ApplicationProvider.getApplicationContext<Context>().applicationContext.javaClass)
    }

    @Test fun settings_defer_reminders_without_disabling_backup_actions() {
        var opened = 0
        var exports = 0
        var restores = 0
        compose.setContent { LithicTheme {
            SettingsShell(ZoneId.of("Asia/Shanghai"), onNotificationClick = { opened++ },
                onExportBackup = { exports++ }, onRestoreBackup = { restores++ })
        } }
        compose.onNodeWithTag("reminders-deferred").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("open-notification-settings").assertDoesNotExist()
        compose.onNodeWithTag("renewal-reminder-switch").assertDoesNotExist()
        compose.onNodeWithTag("request-notification-permission").assertDoesNotExist()
        compose.onNodeWithTag("export-complete-backup").performScrollTo().performClick()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, opened); assertEquals(1, exports); assertEquals(1, restores) }
    }

    @Test fun restored_legacy_route_only_shows_explanation_and_back() {
        var back = 0
        compose.setContent { LithicTheme { NotificationDeferredScreen { back++ } } }
        compose.onNodeWithTag("notification-deferred-screen").assertIsDisplayed()
        compose.onNodeWithTag("renewal-reminder-switch").assertDoesNotExist()
        compose.onNodeWithTag("milestone-reminder-switch").assertDoesNotExist()
        compose.onNodeWithTag("request-notification-permission").assertDoesNotExist()
        compose.onNodeWithTag("notification-deferred-back").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }
}
