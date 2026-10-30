package com.cooper.lithic.app

import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Future V1-A1 preflight only: actual Activity configuration, not Compose/font/IME acceptance.
 * The merged test Manifest must select M13IsolatedRunner before Application construction;
 * the assertions below cannot undo business startup under an incorrectly installed Runner.
 * No production Activity, Owner, Room, screenshot, system setting or persistent file is used.
 */
@RunWith(AndroidJUnit4::class)
class V1AcceptanceConfigurationTest {
    @Test fun plain_host_activity_configuration() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val target = instrumentation.targetContext
        assertEquals(TARGET_PACKAGE, target.packageName)
        assertEquals(TEST_PACKAGE, instrumentation.context.packageName)
        assertEquals(M13IsolatedRunner::class.java, instrumentation.javaClass)
        assertEquals(Application::class.java, target.applicationContext.javaClass)
        assertEquals(Process.myUid(), target.applicationInfo.uid)
        assertNotEquals(target.applicationInfo.uid, instrumentation.context.applicationInfo.uid)

        // Launch after the identity checks, rather than an Activity Rule preceding @Before.
        // This uses the existing Debug test-host component, never the production MainActivity.
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertEquals(ComponentActivity::class.java, activity.javaClass)
                assertEquals(TARGET_PACKAGE, activity.packageName)
                assertSame(target.applicationContext, activity.application)
                val configuration = activity.resources.configuration
                val metrics = activity.resources.displayMetrics
                assertTrue(configuration.screenWidthDp > 0)
                assertTrue(configuration.screenHeightDp > 0)
                assertTrue(configuration.fontScale.isFinite() && configuration.fontScale > 0f)
                assertTrue(configuration.densityDpi > 0)
                instrumentation.sendStatus(2, Bundle().apply {
                    putString("v1a1.phase", "ACTIVITY_CONFIGURATION_OBSERVED_NOT_TEST_COMPLETION")
                    putInt("v1a1.schemaVersion", 1)
                    putString("v1a1.targetPackage", target.packageName)
                    putString("v1a1.testPackage", instrumentation.context.packageName)
                    putString("v1a1.runner", instrumentation.javaClass.name)
                    putString("v1a1.application", activity.application.javaClass.name)
                    putString("v1a1.activity", activity.javaClass.name)
                    putInt("v1a1.api", Build.VERSION.SDK_INT)
                    putInt("v1a1.hostUid", Process.myUid())
                    putInt("v1a1.hostPid", Process.myPid())
                    putInt("v1a1.testUid", instrumentation.context.applicationInfo.uid)
                    putInt("v1a1.screenWidthDp", configuration.screenWidthDp)
                    putInt("v1a1.screenHeightDp", configuration.screenHeightDp)
                    putInt("v1a1.smallestScreenWidthDp", configuration.smallestScreenWidthDp)
                    putFloat("v1a1.fontScale", configuration.fontScale)
                    putInt("v1a1.densityDpi", configuration.densityDpi)
                    putFloat("v1a1.density", metrics.density)
                    putInt("v1a1.orientation", configuration.orientation)
                    putLong("v1a1.elapsedRealtimeMs", SystemClock.elapsedRealtime())
                })
            }
        }
        // This marker is narrower than process/Owner closure proof. Only JUnit ends the test.
        instrumentation.sendStatus(2, Bundle().apply {
            putString("v1a1.phase", "ACTIVITY_SCENARIO_CLOSED_NOT_PROCESS_EXIT_PROOF")
        })
    }

    private companion object {
        const val TARGET_PACKAGE = "com.cooper.lithic.v1acceptance.debug"
        const val TEST_PACKAGE = "$TARGET_PACKAGE.test"
    }
}
