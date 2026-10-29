package com.cooper.lithic.app

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.os.Process
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/** Future isolated-runner checks only; no normal MainActivity, ledger, or device operation. */
class MediaSandboxStartupTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun requireDedicatedPlainHostBeforeAnyProbe() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        check(context.packageName == "com.cooper.lithic.c6isolation.debug")
        check(instrumentation.context.packageName == "com.cooper.lithic.c6isolation.debug.test")
        check(instrumentation.javaClass == M13IsolatedRunner::class.java)
        check(context.applicationContext.javaClass == Application::class.java)
        check(context.applicationInfo.uid == Process.myUid())
        instrumentation.sendStatus(2, Bundle().apply {
            putString("c6.phase", "PLAIN_HOST_ADMITTED_NOT_TEST_COMPLETION")
            putInt("c6.hostUid", Process.myUid())
            putInt("c6.hostPid", Process.myPid())
        })
    }

    @Test fun manifest_declares_private_isolated_service_in_only_the_media_process() {
        val component = ComponentName(context.packageName, "com.cooper.lithic.data.media.isolation.MediaSandboxService")
        @Suppress("DEPRECATION")
        val service = context.packageManager.getServiceInfo(component, 0)
        assertFalse(service.exported)
        assertTrue(service.flags and ServiceInfo.FLAG_ISOLATED_PROCESS != 0)
        assertEquals(context.applicationInfo.processName + AppProcessRoles.SANDBOX_SUFFIX, service.processName)
    }

    @Test fun startup_providers_are_not_assigned_to_sandbox() {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PROVIDERS)
        val providers = requireNotNull(info.providers)
        assertTrue(providers.any { it.name == "androidx.startup.InitializationProvider" })
        assertTrue(providers.all { it.processName != context.applicationInfo.processName + AppProcessRoles.SANDBOX_SUFFIX })
        assertEquals(context.applicationInfo.processName,
            providers.single { it.name == "androidx.startup.InitializationProvider" }.processName)
    }

    @Test fun unknown_unattached_application_cannot_create_owner_or_business_configuration() {
        val application = LithicApplication()
        assertFalse(application.businessOwnerInitialized)
        assertEquals(AppProcessRole.UNKNOWN, application.processRole)
        application.onCreate()
        for (access in listOf<() -> Any>({ application.appContainer }, { application.workManagerConfiguration })) {
            try { access(); fail("Unknown process admitted business resources") }
            catch (_: IllegalStateException) { }
            assertFalse(application.businessOwnerInitialized)
        }
    }

    @Test fun host_test_application_is_plain_and_has_not_run_normal_business_startup() {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
    }
}
