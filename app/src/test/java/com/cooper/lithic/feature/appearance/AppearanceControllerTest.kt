package com.cooper.lithic.feature.appearance

import com.cooper.lithic.app.AppProcessRole
import java.io.IOException
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AppearanceControllerTest {
    @Test fun only_three_stable_theme_identities_are_supported() {
        assertEquals(listOf("charcoal", "pure_white", "stone_mist"), LithicAppearance.entries.map { it.storageId })
        LithicAppearance.entries.forEach { assertEquals(it, LithicAppearance.fromStorage(it.storageId)) }
        listOf("", "dark", "light", "system", "0", "深色黑灰", "CHARCOAL", "new_theme").forEach {
            assertNull(LithicAppearance.fromStorage(it))
        }
    }

    @Test fun missing_preference_defaults_to_stone_without_inventing_a_migration_or_write() = withController(MemoryStorage()) { controller, storage ->
        assertEquals(LithicAppearance.STONE_MIST, controller.state.value.selected)
        assertTrue(controller.state.value.readable)
        assertFalse(controller.state.value.loading)
        assertNull(controller.state.value.error)
        assertEquals(0, storage.writes.size)
    }

    @Test fun each_saved_theme_is_read_back_without_replacing_it() {
        LithicAppearance.entries.forEach { theme -> withController(MemoryStorage(theme.storageId)) { controller, storage ->
            assertEquals(theme, controller.state.value.selected)
            assertTrue(controller.state.value.readable)
            assertTrue(storage.writes.isEmpty())
        } }
    }

    @Test fun unknown_persisted_value_is_an_error_not_a_fresh_install() = withController(MemoryStorage("future_theme")) { controller, storage ->
        assertFalse(controller.state.value.readable)
        assertFalse(controller.state.value.loading)
        assertNotNull(controller.state.value.error)
        assertEquals("future_theme", storage.value)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun failed_initial_read_is_visible_and_does_not_write_a_default() = withController(MemoryStorage().apply {
        readFailure = IOException("synthetic read failure")
    }) { controller, storage ->
        assertFalse(controller.state.value.readable)
        assertFalse(controller.state.value.loading)
        assertNotNull(controller.state.value.error)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun retry_after_read_failure_uses_the_saved_value() = withController(MemoryStorage("charcoal").apply {
        readFailure = IOException("synthetic read failure")
    }) { controller, storage ->
        storage.readFailure = null
        controller.retryRead()
        assertTrue(controller.state.value.readable)
        assertEquals(LithicAppearance.CHARCOAL, controller.state.value.selected)
        assertNull(controller.state.value.error)
    }

    @Test fun successful_selection_updates_only_after_storage_completion() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val storage = MemoryStorage("pure_white").apply { beforeWrite = { entered.complete(Unit); release.await() } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = AppearanceController(scope, storage)
            assertTrue(controller.state.value.readable)
            controller.select(LithicAppearance.CHARCOAL)
            withTimeout(2_000) { entered.await() }
            assertTrue(controller.state.value.saving)
            assertEquals(LithicAppearance.PURE_WHITE, controller.state.value.selected)
            assertEquals("pure_white", storage.value)
            release.complete(Unit)
            yield()
            assertEquals("charcoal", storage.value)
            assertEquals(LithicAppearance.CHARCOAL, controller.state.value.selected)
            assertFalse(controller.state.value.saving)
        } finally { release.complete(Unit); scope.cancel() }
    }

    @Test fun failed_save_preserves_previous_confirmed_theme_and_exposes_error() = withController(MemoryStorage("charcoal").apply {
        writeFailure = IOException("synthetic disk failure")
    }) { controller, storage ->
        assertTrue(controller.state.value.readable)
        controller.select(LithicAppearance.PURE_WHITE)
        assertEquals("charcoal", storage.value)
        assertEquals(LithicAppearance.CHARCOAL, controller.state.value.selected)
        assertFalse(controller.state.value.saving)
        assertNotNull(controller.state.value.error)
    }

    @Test fun explicit_successful_choice_can_replace_an_unknown_theme_without_an_automatic_reset() = withController(MemoryStorage("future_theme")) { controller, storage ->
        assertFalse(controller.state.value.readable)
        assertTrue(storage.writes.isEmpty())
        controller.select(LithicAppearance.PURE_WHITE)
        assertEquals(listOf("pure_white"), storage.writes)
        assertEquals("pure_white", storage.value)
        assertTrue(controller.state.value.readable)
        assertEquals(LithicAppearance.PURE_WHITE, controller.state.value.selected)
    }

    @Test fun rapid_choices_are_serial_and_the_last_intent_matches_persisted_value() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var active = 0
        var maximum = 0
        val storage = MemoryStorage("stone_mist").apply {
            beforeWrite = {
                active++; maximum = maxOf(maximum, active)
                if (it == "charcoal") { entered.complete(Unit); release.await() }
            }
            afterWrite = { active-- }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = AppearanceController(scope, storage)
            assertTrue(controller.state.value.readable)
            controller.select(LithicAppearance.CHARCOAL)
            withTimeout(2_000) { entered.await() }
            controller.select(LithicAppearance.PURE_WHITE)
            controller.select(LithicAppearance.STONE_MIST)
            release.complete(Unit); yield()
            assertEquals(1, maximum)
            assertEquals("stone_mist", storage.value)
            assertEquals(LithicAppearance.STONE_MIST, controller.state.value.selected)
            assertFalse(controller.state.value.saving)
        } finally { release.complete(Unit); scope.cancel() }
    }

    @Test fun later_failed_choice_keeps_a_completed_earlier_save() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val storage = MemoryStorage("stone_mist").apply {
            beforeWrite = {
                if (it == "charcoal") { entered.complete(Unit); release.await() }
                if (it == "pure_white") throw IOException("synthetic second write failure")
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val controller = AppearanceController(scope, storage)
            assertTrue(controller.state.value.readable)
            controller.select(LithicAppearance.CHARCOAL)
            withTimeout(2_000) { entered.await() }
            controller.select(LithicAppearance.PURE_WHITE)
            release.complete(Unit); yield()
            assertEquals("charcoal", storage.value)
            assertEquals(LithicAppearance.CHARCOAL, controller.state.value.selected)
            assertNotNull(controller.state.value.error)
        } finally { release.complete(Unit); scope.cancel() }
    }

    @Test fun reread_failure_keeps_a_known_theme_without_claiming_fresh_installation() = withController(MemoryStorage("charcoal")) { controller, storage ->
        assertTrue(controller.state.value.readable)
        storage.readFailure = IOException("synthetic reread failure")
        controller.retryRead()
        assertEquals(LithicAppearance.CHARCOAL, controller.state.value.selected)
        assertNotNull(controller.state.value.error)
        assertTrue(storage.writes.isEmpty())
    }

    @Test fun sandbox_and_unknown_roles_do_not_call_the_storage_factory() {
        listOf(AppProcessRole.SANDBOX, AppProcessRole.UNKNOWN).forEach { role ->
            var creations = 0
            try {
                AppearanceInitialization.mainOnly(role) { creations++; Unit }
                fail("Non-main process initialized appearance")
            } catch (_: IllegalStateException) { }
            assertEquals(0, creations)
        }
    }

    @Test fun main_role_can_create_the_one_requested_preference_instance() {
        var creations = 0
        val actual = Any()
        assertSame(actual, AppearanceInitialization.mainOnly(AppProcessRole.MAIN) { creations++; actual })
        assertEquals(1, creations)
    }

    private fun withController(storage: MemoryStorage, block: (AppearanceController, MemoryStorage) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(AppearanceController(scope, storage), storage) } finally { scope.cancel() }
    }

    private class MemoryStorage(var value: String? = null) : AppearanceStorage {
        val writes = mutableListOf<String>()
        var readFailure: Exception? = null
        var writeFailure: Exception? = null
        var beforeWrite: suspend (String) -> Unit = {}
        var afterWrite: () -> Unit = {}
        override suspend fun read(): String? { readFailure?.let { throw it }; return value }
        override suspend fun write(storageId: String) {
            beforeWrite(storageId)
            writeFailure?.let { throw it }
            value = storageId; writes += storageId; afterWrite()
        }
    }
}
