package com.cooper.lithic.platform.notification

import com.cooper.lithic.app.LithicApplication
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Host bytecode checks; real Android system work remains NOT_RUN. */
class CoreReminderNoWorkManagerTest {
    private fun compiledBytes(type: Class<*>): String {
        val resource = "/${type.name.replace('.', '/')}.class"
        return requireNotNull(type.getResourceAsStream(resource)) { "Missing compiled class: $resource" }
            .use { it.readBytes().toString(Charsets.ISO_8859_1) }
    }

    @Test fun core_scheduler_and_legacy_entry_do_not_reference_workmanager() {
        for (type in listOf(ReminderScheduling::class.java, MilestoneScheduling::class.java,
            ReminderLegacyCancellation::class.java)) {
            val bytes = compiledBytes(type)
            assertFalse("${type.simpleName} must not access WorkManager", bytes.contains("androidx/work/WorkManager"))
            for (method in listOf("cancelUniqueWork", "cancelAllWorkByTag", "cancelAllWork", "cancelWorkById",
                "enqueueUniqueWork", "enqueueUniquePeriodicWork")) {
                assertFalse("${type.simpleName} must not invoke $method", bytes.contains(method))
            }
        }
    }

    @Test fun production_application_does_not_connect_legacy_cancellation() {
        assertFalse(compiledBytes(LithicApplication::class.java).contains("ReminderLegacyCancellation"))
    }

    @Test fun reminder_capability_stays_closed_independent_of_saved_preferences() {
        assertFalse(ReminderCapability.AUTOMATIC_NOTIFICATIONS)
        assertTrue(compiledBytes(ReminderLegacyCancellation::class.java).isNotEmpty())
    }
}
