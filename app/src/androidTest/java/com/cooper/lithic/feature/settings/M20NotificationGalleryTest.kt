package com.cooper.lithic.feature.settings

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.LocalWriteCoordinator
import com.cooper.lithic.data.settings.SettingsStore
import com.cooper.lithic.domain.reminder.RenewalReminderCandidate
import com.cooper.lithic.domain.reminder.RenewalReminderPlanner
import com.cooper.lithic.platform.notification.NotificationPublisher
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real ComponentActivity canvas + persisted synthetic preferences + actual denied device
 * permission. 200% is LocalDensity, not a system-font or MainActivity claim. */
@RunWith(AndroidJUnit4::class)
class M20NotificationGalleryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun capture_actual_denial_and_360dp_local200_controls() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        assertFalse(File(context.noBackupFilesDir, "active-generation").exists())
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val root = File(context.noBackupFilesDir, "m20-gallery-${UUID.randomUUID()}").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        File(root, "owner.txt").writeText(root.name)
        instrumentation.sendStatus(0, Bundle().apply { putString("m20_gallery_evidence_root", root.absolutePath) })
        val settings = SettingsStore.open(File(root, "settings.preferences_pb"), LocalWriteCoordinator(), ZoneId.of("Asia/Shanghai"))
        try {
            val preferences = settings.reminderPreferences()
            assertTrue(preferences.renewalEnabled && preferences.milestoneEnabled)
            val publisher = NotificationPublisher(context).also { it.ensureChannel() }
            val access = publisher.access()
            assertTrue("Capture actual API33+ denial before granting permission for the separate platform case", access.requiresRuntimePermission)
            assertFalse(access.runtimeGranted)
            assertFalse(access.allowed)
            val owner = UUID.randomUUID().toString()
            val due = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(2)
            val key = RenewalReminderPlanner.logicalKey(owner, due)
            assertFalse(publisher.publish(RenewalReminderCandidate(owner, UUID.randomUUID().toString(), due, due.minusDays(3), 2,
                key, RenewalReminderPlanner.notificationId(key), "合成拒绝验证", "此通知在权限拒绝时不得发布。")))
            assertEquals(preferences, settings.reminderPreferences())
            val denied = NotificationPermissionState(access.requiresRuntimePermission, access.runtimeGranted, access.appEnabled, access.channelEnabled)
            val stress = mutableStateOf(false)
            var density = 1f
            var backs = 0
            compose.setContent { LithicTheme {
                density = LocalDensity.current.density
                CompositionLocalProvider(LocalDensity provides Density(density, if (stress.value) 2f else 1f)) {
                    Box(Modifier.width(360.dp).fillMaxHeight().testTag("m20-gallery-canvas")) {
                        NotificationSettingsContent(NotificationSettingsUiState(loading = false, preferences = preferences), denied,
                            {}, {}, {}, {}, { backs++ })
                    }
                }
            } }
            compose.onNodeWithTag("notification-settings-back").performScrollTo().assertIsDisplayed()
            capture(root, "01-preferences-and-actual-denial.png")
            compose.onNodeWithTag("notification-permission-explanation").performScrollTo().assertIsDisplayed()
            capture(root, "02-permission-denial-explanation.png")
            compose.runOnIdle { stress.value = true }
            compose.onNodeWithTag("renewal-reminder-switch").performScrollTo().assertIsDisplayed()
            capture(root, "03-local360-200-switches.png")
            for (tag in listOf("renewal-reminder-switch", "milestone-reminder-switch", "request-notification-permission",
                "open-system-notification-settings", "notification-settings-back")) {
                val node = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
                val bounds = node.fetchSemanticsNode().boundsInRoot
                val canvas = compose.onNodeWithTag("m20-gallery-canvas").fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.height >= 48f * density - 1f)
                assertTrue(bounds.left >= canvas.left - 1f && bounds.right <= canvas.right + 1f)
            }
            capture(root, "04-local360-200-return.png")
            compose.onNodeWithTag("notification-settings-back").performClick()
            compose.runOnIdle { assertEquals(1, backs) }
            val source = JSONObject().put("source", "REAL_ISOLATED_COMPONENT_ACTIVITY")
                .put("activity", compose.activity.javaClass.name).put("mainActivity", false)
                .put("package", context.packageName).put("pid", Process.myPid()).put("uid", Process.myUid())
                .put("preferences", "REAL_SETTINGS_STORE_DEFAULTS").put("runtimeGranted", access.runtimeGranted)
                .put("applicationNotificationsEnabled", access.appEnabled).put("channelEnabled", access.channelEnabled)
                .put("deniedRealPublisherResult", false).put("systemNotificationsSent", 0)
                .put("canvasDp", 360).put("fontScaleSource", "LOCAL_DENSITY").put("fontScale", 2)
                .put("permissionButtonCallback", "NO_SYSTEM_DIALOG_IN_SCREENSHOT_CASE")
            File(root, "gallery-source.json").apply { check(createNewFile()) }.writeText(source.toString(2))
            println("M20_NOTIFICATION_GALLERY=${root.absolutePath}")
        } finally { settings.close() }
        assertFalse(File(context.noBackupFilesDir, "active-generation").exists())
    }

    private fun capture(root: File, name: String) {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        compose.waitForIdle()
        val bitmap = compose.onNodeWithTag("m20-gallery-canvas").captureToImage().asAndroidBitmap()
        val file = File(root, name).apply { check(createNewFile()) }
        FileOutputStream(file).use { output -> check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)); output.fd.sync() }
        check(file.length() > 0)
    }
}
