package com.cooper.lithic.feature.subscription

import android.content.Context
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.ui.openHomeCreate
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Formal navigation over actual Room, including restored submissions hidden behind Home. */
@RunWith(AndroidJUnit4::class)
class SubscriptionNavigationGuardTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 1, 20))
    private val store = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var ready: LedgerUiState.Ready
    private lateinit var project: ProjectViewModel
    private lateinit var subscription: SubscriptionViewModel

    @Before fun before() = runBlocking {
        root = File(context.noBackupFilesDir, "m08-nav-guard-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        val states = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        onMain { holder = LedgerUiStateHolder(states) { clock }; retain("holder", holder) }
        ready = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        onMain {
            project = ProjectViewModel(ready.repository, SavedStateHandle(), clock); retain("project", project)
            subscription = SubscriptionViewModel(ready.subscriptionRepository, SavedStateHandle(), clock); retain("subscription", subscription)
        }
    }
    @After fun after() = runBlocking {
        onMain { store.clear() }
        jobs.forEach { it.join() }
        generation.close()
        check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(root.deleteRecursively())
    }

    @Test fun dirty_subscription_form_guards_all_tabs_toolbar_and_real_system_back() = runBlocking {
        render()
        openCreate()
        fillVisibleCreate()
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        val actions: List<() -> Unit> = listOf(
            { compose.onNodeWithTag("tab-now").performClick() },
            { compose.onNodeWithTag("tab-review").performClick() },
            { compose.onNodeWithTag("tab-settings").performClick() },
            { compose.onNodeWithTag("subscription-editor-back").performScrollTo().performClick() },
            { Espresso.pressBack() },
        )
        actions.forEach { leave ->
            leave()
            compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
            compose.onNodeWithTag("discard-keep").performClick()
            assertEquals("150.00", subscription.state.value.editor!!.amount)
            assertEquals("2026-01-02", subscription.state.value.editor!!.paidOn)
        }
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun hidden_restored_pending_payment_blocks_project_entry_and_has_reachable_original_form() = runBlocking {
        val handle = SavedStateHandle()
        val lossy = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult {
                super.createWithRenewal(command)
                throw IOException("Test delivery interrupted after real payment commit")
            }
        }
        lateinit var first: SubscriptionViewModel
        onMain {
            first = SubscriptionViewModel(lossy, handle, clock); retain("first-payment", first)
            first.startCreate(); first.changeName("恢复后待核对付款"); first.changeAmount("150.00")
            first.changePaidOn("2026-01-02"); first.changeEffectiveFrom("2026-01-03")
            first.changePaidUntil("2026-02-03"); first.changeReminderDue("2026-02-05"); first.save()
        }
        withTimeout(15_000) { first.state.first { it.editor?.phase == SubmissionPhase.UNCONFIRMED } }
        onMain { subscription = SubscriptionViewModel(ready.subscriptionRepository, restoredHandle(handle), clock); retain("restored-payment", subscription) }
        render()
        compose.openHomeCreate("home-create-project")
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-dialog").assertDoesNotExist()
        compose.onNodeWithTag("pending-submit-return-to-form").performClick()
        compose.onNodeWithTag("subscription-editor-screen").assertIsDisplayed()
        compose.onNodeWithTag("subscription-confirm-payment").performScrollTo().assertTextContains("核对并重试同一付款").performClick()
        compose.waitUntil(15_000) { subscription.state.value.editor == null && subscription.state.value.detail.detail != null }
        compose.onNodeWithTag("subscription-detail-screen").assertIsDisplayed()
        val detail = ready.subscriptionRepository.loadSubscriptionPage().items.single()
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(detail.subscription.id))
        assertEquals(0L, generation.database.projectDao().countProjects())
    }

    @Test fun root_home_system_back_keeps_restored_pending_envelope_and_reachable_payment_form() = runBlocking {
        val firstHandle = SavedStateHandle()
        val lossy = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult {
                super.createWithRenewal(command)
                throw IOException("Test delivery interrupted after root-home payment commit")
            }
        }
        lateinit var first: SubscriptionViewModel
        onMain {
            first = SubscriptionViewModel(lossy, firstHandle, clock); retain("root-first-payment", first)
            first.startCreate(); first.changeName("首页恢复待核对付款"); first.changeAmount("150.00")
            first.changePaidOn("2026-01-02"); first.changeEffectiveFrom("2026-01-03")
            first.changePaidUntil("2026-02-03"); first.changeReminderDue("2026-02-05"); first.save()
        }
        withTimeout(15_000) { first.state.first { it.editor?.phase == SubmissionPhase.UNCONFIRMED } }
        val restored = restoredHandle(firstHandle)
        val saved = Bundle(requireNotNull(restored.get<Bundle>("lithic.subscription.activeForm")))
        val submitted = Bundle(requireNotNull(saved.getBundle("submitted")))
        onMain { subscription = SubscriptionViewModel(ready.subscriptionRepository, restored, clock); retain("root-restored-payment", subscription) }
        render()
        val host = compose.activity
        onMain { assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState) }
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        // Assert against the captured Activity before asking Compose for nodes: exiting Home
        // must fail as a real guard violation, not as a no-resumed-Activity runner error.
        onMain {
            assertFalse("System Back must not finish Home with a restored pending payment", host.isFinishing)
            assertFalse("The host must remain alive to recover the original payment", host.isDestroyed)
            assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState)
        }
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        val active = requireNotNull(restored.get<Bundle>("lithic.subscription.activeForm"))
        val afterSubmitted = requireNotNull(active.getBundle("submitted"))
        for (key in listOf("commandId", "subscriptionId", "renewalId", "writtenAt", "serviceName", "amount", "cycle",
            "paidOn", "effectiveFrom", "paidUntilExclusive", "reminderDueOn")) {
            assertEquals("The frozen submitted $key must survive system Back", submitted.getString(key), afterSubmitted.getString(key))
        }
        assertEquals("150.00", subscription.state.value.editor!!.amount)
        assertEquals("2026-01-02", subscription.state.value.editor!!.paidOn)
        assertEquals("2026-01-03", subscription.state.value.editor!!.effectiveFrom)
        assertEquals("2026-02-03", subscription.state.value.editor!!.paidUntilExclusive)
        assertEquals("2026-02-05", subscription.state.value.editor!!.reminderDueOn)
        compose.onNodeWithTag("pending-submit-return-to-form").performClick()
        compose.onNodeWithTag("subscription-editor-screen").assertIsDisplayed()
        assertEquals(SubmissionPhase.UNCONFIRMED, subscription.state.value.editor!!.phase)
        val detail = ready.subscriptionRepository.loadSubscriptionPage().items.single()
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(detail.subscription.id))
    }

    @Test fun root_home_system_back_keeps_restored_dirty_form_until_explicit_discard() = runBlocking {
        val firstHandle = SavedStateHandle()
        onMain {
            subscription = SubscriptionViewModel(ready.subscriptionRepository, firstHandle, clock); retain("root-dirty-first", subscription)
            subscription.startCreate(); subscription.changeName("首页隐藏的未保存输入"); subscription.changeAmount("123.456")
            subscription.changePaidOn("2026-01-02")
        }
        val restored = restoredHandle(firstHandle)
        onMain { subscription = SubscriptionViewModel(ready.subscriptionRepository, restored, clock); retain("root-dirty-restored", subscription) }
        render()
        val host = compose.activity
        onMain { assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState) }
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onMain {
            assertFalse("System Back must not finish Home with restored unsaved input", host.isFinishing)
            assertFalse(host.isDestroyed)
            assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState)
        }
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        assertEquals("123.456", subscription.state.value.editor!!.amount)
        assertEquals("2026-01-02", subscription.state.value.editor!!.paidOn)
        assertNotNull(restored.get<Bundle>("lithic.subscription.activeForm"))
        Espresso.pressBackUnconditionally()
        compose.onNodeWithTag("discard-confirm").performClick()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        assertEquals(null, subscription.state.value.editor)
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun root_home_without_active_forms_still_allows_real_system_back_to_finish() {
        render()
        val host = compose.activity
        onMain { assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState) }
        Espresso.pressBackUnconditionally()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        onMain { assertTrue("An unguarded root Home Back must initiate Activity exit", host.isFinishing) }
        var destroyed = false
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (!destroyed && SystemClock.uptimeMillis() < deadline) {
            onMain { destroyed = host.lifecycle.currentState == Lifecycle.State.DESTROYED }
            if (!destroyed) SystemClock.sleep(50)
        }
        onMain { assertEquals(Lifecycle.State.DESTROYED, host.lifecycle.currentState) }
    }

    @Test fun hidden_restored_project_submission_blocks_subscription_entry_without_changing_project_vm() = runBlocking {
        val handle = SavedStateHandle()
        val lossy = object : ProjectRepository(generation.database, generation.coordinator, clock) {
            override suspend fun createProject(command: CreateProjectCommand): MutationResult {
                super.createProject(command)
                throw IOException("Test delivery interrupted after real project commit")
            }
        }
        lateinit var first: ProjectViewModel
        onMain {
            first = ProjectViewModel(lossy, handle, clock); retain("first-project", first)
            first.startCreate(); first.changeName("恢复后待核对项目"); first.changeInitialAmount("100")
            first.changeStartedOn("2026-01-01"); first.save()
        }
        withTimeout(15_000) { first.state.first { it.editor?.phase == SubmissionPhase.UNCONFIRMED } }
        onMain { project = ProjectViewModel(ready.repository, restoredHandle(handle), clock); retain("restored-project", project) }
        render()
        compose.openHomeCreate("home-create-subscription")
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("pending-submit-return-to-form").performClick()
        compose.onNodeWithTag("project-editor-screen").assertIsDisplayed()
        compose.onNodeWithTag("project-save").performScrollTo().performClick()
        compose.waitUntil(15_000) { project.state.value.editor == null && project.state.value.detail.detail != null }
        assertEquals(1L, generation.database.projectDao().countProjects())
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun formal_committed_read_failure_exposes_only_reread_not_another_payment_confirmation() = runBlocking {
        var failRead = true
        val unreadable = object : SubscriptionRepository(generation.database, generation.coordinator, clock) {
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (failRead) throw IOException("Test read failed after real saved payment")
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        onMain { subscription = SubscriptionViewModel(unreadable, SavedStateHandle(), clock); retain("unreadable-payment", subscription) }
        render(); openCreate(); fillVisibleCreate()
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        compose.onNodeWithTag("subscription-confirm-payment").performScrollTo().performClick()
        compose.waitUntil(15_000) { subscription.state.value.editor?.phase == SubmissionPhase.COMMITTED && subscription.state.value.editor?.message?.contains("已保存") == true }
        compose.onNodeWithTag("subscription-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("subscription-retry-read").performScrollTo().assertIsDisplayed()
        assertEquals(1L, generation.database.subscriptionDao().countSubscriptions())
        failRead = false
        compose.onNodeWithTag("subscription-retry-read").performClick()
        compose.waitUntil(15_000) { subscription.state.value.editor == null && subscription.state.value.detail.detail != null }
        val detail = ready.subscriptionRepository.loadSubscriptionPage().items.single()
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(detail.subscription.id))
    }

    private fun render() {
        compose.setContent { LithicTheme {
            val ledger by holder.state.collectAsState()
            LithicApp(ledger, {}, project, subscriptionViewModel = subscription)
        } }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }
    private fun openCreate() {
        compose.openHomeCreate("home-create-subscription")
        compose.waitUntil(10_000) { subscription.state.value.editor?.today != null }
    }
    private fun fillVisibleCreate() {
        compose.onNodeWithTag("subscription-name").performScrollTo().performTextReplacement("正式付款表单")
        compose.onNodeWithTag("subscription-amount").performScrollTo().performTextReplacement("150.00")
        for ((tag, date) in listOf("subscription-paid-on-input" to "2026-01-02", "subscription-effective-from-input" to "2026-01-03",
            "subscription-paid-until-input" to "2026-02-03", "subscription-reminder-due-input" to "2026-02-05")) {
            compose.onNodeWithTag(tag).performScrollTo().performTextReplacement(date)
        }
    }
    private fun retain(key: String, model: androidx.lifecycle.ViewModel) { store.put(key, model); model.viewModelScope.coroutineContext[Job]?.let(jobs::add) }
    private fun restoredHandle(source: SavedStateHandle): SavedStateHandle = SavedStateHandle(source.keys().associateWith { key ->
        val value = source.get<Any?>(key); if (value is Bundle) Bundle(value) else value
    })
    private fun onMain(action: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(action)
}
