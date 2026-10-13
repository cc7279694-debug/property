package com.cooper.lithic.feature.subscription

import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Formal Nav3 and real Room. Tested payment writes originate only from visible confirmation buttons. */
@RunWith(AndroidJUnit4::class)
class SubscriptionRenewalFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = MutableBusinessClock(LocalDate.of(2026, 1, 20))
    private val stores = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private val activeHolder = mutableStateOf<LedgerUiStateHolder?>(null)
    private val activeModel = mutableStateOf<SubscriptionViewModel?>(null)
    private val activeProject = mutableStateOf<ProjectViewModel?>(null)
    private val showing = mutableStateOf(true)
    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var model: SubscriptionViewModel
    private lateinit var savedState: SavedStateHandle
    private lateinit var repository: RecordingRepository

    @Before fun before() = runBlocking<Unit> {
        root = File(context.noBackupFilesDir, "m08-subscription-flow-${UUID.randomUUID()}")
        check(root.mkdir())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        attachOwners()
    }

    @After fun after() = runBlocking<Unit> {
        if (::holder.isInitialized) clearOwners()
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) {
            check(root.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
            check(root.name.startsWith("m08-subscription-flow-"))
            check(root.deleteRecursively())
        }
    }

    @Test fun renewal_needs_confirmation() = runBlocking<Unit> {
        render()
        val id = create("只由真实确认续费", "150", "2026-01-01", "2026-01-01", "2026-02-01", "2026-01-29")
        val original = repository.loadSubscriptionDetail(id)!!
        openRenewal()
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        fill("renewal", "180", BillingCycle.MONTHLY, "2026-01-20", "2026-02-01", "2026-03-01", "2026-02-25")
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        compose.onNodeWithTag("renewal-editor-back").performScrollTo().performClick()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        assertEditorRaw("180", "2026-01-20", "2026-02-01", "2026-03-01", "2026-02-25")
        compose.onNodeWithTag("renewal-editor-back").performScrollTo().performClick()
        compose.onNodeWithTag("discard-confirm").performClick()
        waitDetail(id)
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)

        observe(LocalDate.of(2026, 2, 2))
        assertEquals(SubscriptionDisplayStatus.EXPIRED, repository.loadSubscriptionDetail(id)!!.cost.displayStatus)
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        detailNode("subscription-detail-no-current-plan").assertTextContains("当前无有效套餐")
        openRenewal()
        fill("renewal", "180", BillingCycle.MONTHLY, "2026-02-02", "2026-02-02", "2026-03-02", "2026-02-27")
        generation.database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER m08_flow_fail_payment BEFORE INSERT ON subscription_renewals " +
                "WHEN NEW.subscriptionId = '$id' AND NEW.amountCents = 18000 " +
                "BEGIN SELECT RAISE(ABORT, 'synthetic local payment write failure'); END",
        )
        clickConfirm("renewal")
        compose.waitUntil(15_000) { model.state.value.editor?.phase == SubmissionPhase.UNCONFIRMED }
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        assertEditorRaw("180", "2026-02-02", "2026-02-02", "2026-03-02", "2026-02-27")
        val failed = model.state.value.editor!!
        assertTrue(failed.errors.isEmpty()) // The valid command reached the real SQLite write, not form validation.
        assertTrue("SQLite ABORT is an actual constraint conflict, not form validation", failed.conflict)
        assertTrue(repository.renewalResults.last() is SubscriptionMutationResult.Conflict)
        val originalCommand = repository.renewalCommands.single()
        val originalEnvelope = onMainValue { Bundle(savedState.get<Bundle>(FORM_KEY)!!.getBundle("submitted")!!) }
        assertEquals(originalCommand.commandId, originalEnvelope.getString("commandId"))
        assertEquals(originalCommand.renewal.id, originalEnvelope.getString("renewalId"))
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("pending-submit-dialog").assertIsDisplayed()
        compose.onNodeWithTag("pending-submit-keep").performClick()
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(id))
        generation.database.openHelper.writableDatabase.execSQL("DROP TRIGGER m08_flow_fail_payment")
        compose.onNodeWithTag("renewal-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("renewal-resolve-conflict").performScrollTo().performClick()
        compose.waitUntil(15_000) { !model.state.value.editor!!.loading && model.leaveDecision() == LeaveDecision.ALLOW }
        assertEditorRaw("180", "2026-02-02", "2026-02-02", "2026-03-02", "2026-02-27")
        val checkedEnvelope = onMainValue { savedState.get<Bundle>(FORM_KEY)!!.getBundle("submitted")!! }
        for (key in originalEnvelope.keySet()) assertEquals(originalEnvelope.get(key), checkedEnvelope.get(key))
        assertNull(generation.database.subscriptionDao().findRenewal(originalCommand.renewal.id))
        compose.onNodeWithTag("renewal-editor-back").performScrollTo().performClick()
        waitDetail(id)
        assertEquals(original.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        openRenewal()
        fill("renewal", "180", BillingCycle.MONTHLY, "2026-02-02", "2026-02-02", "2026-03-02", "2026-02-27")
        clickConfirm("renewal")
        waitDetail(id)
        assertEquals(2, repository.renewalCommands.size)
        assertFalse(originalCommand.commandId == repository.renewalCommands.last().commandId)
        val detail = repository.loadSubscriptionDetail(id)!!
        assertEquals(2, detail.renewals.size)
        assertFalse(detail.renewals.any { it.id == originalCommand.renewal.id })
        assertEquals(repository.renewalCommands.last().renewal, detail.renewals.single { it.amountCents == 18000L })
        assertEquals(original.renewals.single(), detail.renewals.single { it.id == original.renewals.single().id })
    }

    @Test fun real_renewal_commit_with_lost_result_replays_original_payment_after_saved_task_restore() = runBlocking<Unit> {
        val restoration = render(restorable = true)!!
        val id = create("真实写成功但回传中断", "150", "2026-01-01", "2026-01-01", "2026-02-01", "2026-01-29")
        openRenewal()
        fill("renewal", "180", BillingCycle.MONTHLY, "2026-01-20", "2026-02-01", "2026-03-01", "2026-02-25")
        repository.loseNextRenewalResult = true
        clickConfirm("renewal")
        compose.waitUntil(15_000) { model.state.value.editor?.phase == SubmissionPhase.UNCONFIRMED }
        assertFalse(model.state.value.editor!!.conflict)
        val original = repository.renewalCommands.single()
        val committed = repository.loadSubscriptionDetail(id)!!.renewals
        assertEquals(2, committed.size)
        assertEquals(original.renewal, committed.single { it.id == original.renewal.id })
        val job = model.viewModelScope.coroutineContext[Job]
        onMain {
            savedState = restoredHandle(savedState)
            model = SubscriptionViewModel(repository, savedState, clock)
            retain("subscription", model); activeModel.value = model
        }
        job?.join()
        restoration.emulateSavedInstanceStateRestore()
        waitEditor(SubscriptionEditorKind.RENEWAL)
        assertEquals(SubmissionPhase.UNCONFIRMED, model.state.value.editor!!.phase)
        assertEditorRaw("180", "2026-01-20", "2026-02-01", "2026-03-01", "2026-02-25")
        compose.onNodeWithTag("renewal-confirm-payment").performScrollTo().assertTextContains("核对并重试同一付款")
        clickConfirm("renewal"); waitDetail(id)
        assertEquals(listOf(original, original), repository.renewalCommands.toList())
        val replay = repository.renewalResults.last() as SubscriptionMutationResult.Success
        assertTrue(replay.replayed)
        assertEquals(original.renewal.id, replay.renewalId)
        assertEquals(committed, repository.loadSubscriptionDetail(id)!!.renewals)
        assertEquals(2L, generation.database.subscriptionDao().countRenewals(id))
    }

    @Test fun renewal_history_is_preserved() = runBlocking<Unit> {
        render()
        val id = create("预付不能提前调价", "150", "2026-01-01", "2026-01-01", "2026-02-01", "2026-01-29")
        observe(LocalDate.of(2026, 1, 19))
        val beforePrepaid = repository.loadSubscriptionDetail(id)!!
        assertEquals(15000L, beforePrepaid.cost.confirmedPaidCents)
        assertEquals(30L, beforePrepaid.cost.standardPurchasedDays)
        assertEquals(0, beforePrepaid.cost.currentRate.compareTo(java.math.BigDecimal("5")))
        detailNode("subscription-detail-current-rate").assert(hasText("¥5.00"))
        detailNode("subscription-detail-total-paid").assert(hasText("¥150.00"))
        observe(LocalDate.of(2026, 1, 20))
        openRenewal()
        fill("renewal", "180", BillingCycle.MONTHLY, "2026-01-20", "2026-02-01", "2026-03-01", "2026-02-26")
        clickConfirm("renewal"); waitDetail(id)
        val prepaid = repository.loadSubscriptionDetail(id)!!
        assertEquals(33000L, prepaid.cost.confirmedPaidCents)
        assertEquals(60L, prepaid.cost.standardPurchasedDays)
        assertEquals(0, prepaid.cost.currentRate.compareTo(java.math.BigDecimal("5")))
        assertEquals(0, prepaid.cost.historicalAverage!!.compareTo(java.math.BigDecimal("5.5")))
        detailNode("subscription-detail-historical-average").assert(hasText("¥5.50"))
        detailNode("subscription-detail-purchased-days").assertTextContains("折算付费时长：60天")
        detailNode("subscription-detail-current-rate").assert(hasText("¥5.00"))
        detailNode("subscription-detail-total-paid").assert(hasText("¥330.00"))
        val next = prepaid.renewals.single { it.amountCents == 18000L }
        detailNode("subscription-renewal-future-${next.id}").assertTextContains("尚未生效")
        compose.onNodeWithText("已使用60天").assertDoesNotExist()
        observe(LocalDate.of(2026, 2, 1))
        detailNode("subscription-detail-current-rate").assert(hasText("¥6.00"))
        assertEquals(33000L, repository.loadSubscriptionDetail(id)!!.cost.confirmedPaidCents)
        observe(LocalDate.of(2026, 2, 20))
        openRenewal()
        fill("renewal", "1200", BillingCycle.YEARLY, "2026-02-20", "2026-03-01", "2027-03-01", "2027-02-25")
        clickConfirm("renewal"); waitDetail(id)
        val before = repository.loadSubscriptionDetail(id)!!
        assertEquals(listOf(120000L, 18000L, 15000L), before.renewals.map { it.amountCents })
        assertEquals(listOf(BillingCycle.YEARLY, BillingCycle.MONTHLY, BillingCycle.MONTHLY), before.renewals.map { it.cycle })
        assertEquals(listOf(365, 30, 30), before.renewals.map { it.standardDays })
        assertEquals(425L, before.cost.standardPurchasedDays)
        detailNode("subscription-renewal-future-${before.renewals.first().id}").assertTextContains("尚未生效")
        reopenFixture()
        assertEquals(before.subscription, repository.loadSubscriptionDetail(id)!!.subscription)
        assertEquals(before.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        assertEquals(ZoneId.of("Asia/Shanghai"), generation.settings.recordZone())
        openFromList(id)
        detailNode("subscription-detail-current-rate").assert(hasText("¥6.00"))
        detailNode("subscription-detail-purchased-days").assertTextContains("折算付费时长：425天")
    }

    @Test fun accurate_dates_retained() = runBlocking<Unit> {
        render()
        val id = create("四日期真实保存", "120", "2026-01-05", "2026-02-01", "2026-03-01", "2026-02-26")
        val before = repository.loadSubscriptionDetail(id)!!
        assertNull(before.currentRenewal)
        assertNull(before.cost.currentPrice)
        assertEquals(0, before.cost.currentRate.signum())
        detailNode("subscription-detail-no-current-plan").assertTextContains("当前无有效套餐")
        compose.onNodeWithTag("subscription-detail-current-rate").assertDoesNotExist()
        detailNode("subscription-detail-historical-average").assert(hasText("¥4.00"))
        val payment = before.renewals.single()
        detailNode("subscription-renewal-future-${payment.id}").assertTextContains("尚未生效")
        reopenFixture()
        val persisted = repository.loadSubscriptionDetail(id)!!.renewals.single()
        assertEquals(payment, persisted)
        assertEquals(LocalDate.of(2026, 1, 5), persisted.paidOn)
        assertEquals(LocalDate.of(2026, 2, 1), persisted.effectiveFrom)
        assertEquals(LocalDate.of(2026, 3, 1), persisted.paidUntilExclusive)
        assertEquals(LocalDate.of(2026, 2, 26), persisted.reminderDueOn)
        openFromList(id)
        detailText("真实付款日期：2026-01-05")
        detailText("实际生效日期：2026-02-01")
        detailText("付费到期边界：2026-03-01（当天不包含）")
        detailText("提醒到期日：2026-02-26")
    }

    @Test fun currency_change_is_allowed() = runBlocking<Unit> {
        render()
        val id = create("固定月付人民币实付变化", "146.83", "2025-12-01", "2025-12-01", "2026-01-01", "2025-12-29")
        openRenewal()
        fill("renewal", "148.10", BillingCycle.MONTHLY, "2026-01-15", "2026-01-01", "2026-02-01", "2026-01-28")
        clickConfirm("renewal"); waitDetail(id)
        val before = repository.loadSubscriptionDetail(id)!!
        assertEquals(listOf(14810L, 14683L), before.renewals.map { it.amountCents })
        assertTrue(before.renewals.all { it.cycle == BillingCycle.MONTHLY && it.standardDays == 30 })
        assertEquals(29493L, before.cost.confirmedPaidCents)
        detailNode("subscription-detail-current-rate").assert(hasText("¥4.94"))
        detailNode("subscription-detail-historical-average").assert(hasText("¥4.92"))
        reopenFixture()
        assertEquals(before.renewals, repository.loadSubscriptionDetail(id)!!.renewals)
        openFromList(id)
        detailText("真实付款日期：2026-01-15")
        detailText("真实付款日期：2025-12-01")
    }

    @Test fun paid_on_history_and_list_order_with_real_paging() = runBlocking<Unit> {
        // This named read fixture is not claimed as a user-confirmed creation path.
        val id = UUID.randomUUID().toString()
        val newerCoverageId = UUID.randomUUID().toString()
        val timestamp = Instant.parse("2026-01-20T00:00:00Z")
        val rows = (0 until 52).map { index ->
            val from = LocalDate.of(2020, 1, 1).plusMonths(index.toLong())
            SubscriptionRenewalEntity(UUID.randomUUID().toString(), id, clock.today().minusDays(index.toLong()),
                from, from.plusMonths(1), from.plusMonths(1).minusDays(2), 15000L + index,
                BillingCycle.MONTHLY, 30, UUID.randomUUID().toString())
        }
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                val dao = generation.database.subscriptionDao()
                dao.insertSubscription(SubscriptionEntity(id, "按真实付款排序的52笔历史", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                    createdAt = timestamp, updatedAt = timestamp))
                rows.forEach { dao.insertRenewal(it) }
                dao.insertSubscription(SubscriptionEntity(newerCoverageId, "较新生效但较早付款的服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
                    createdAt = timestamp, updatedAt = timestamp))
                dao.insertRenewal(SubscriptionRenewalEntity(UUID.randomUUID().toString(), newerCoverageId,
                    clock.today().minusDays(1), LocalDate.of(2025, 1, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2026, 12, 29),
                    130000L, BillingCycle.YEARLY, 365, UUID.randomUUID().toString()))
            }
        }
        render()
        compose.onNodeWithTag("open-subscription-list").performScrollTo().performClick()
        compose.waitUntil(15_000) { model.state.value.list.let { !it.loading && it.items.size == 2 } }
        assertEquals(listOf(id, newerCoverageId), model.state.value.list.items.map { it.subscription.id })
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$id"))
        compose.onNodeWithTag("subscription-row-$id").performClick()
        waitDetail(id)
        assertEquals(50, model.state.value.detail.history.items.size)
        assertEquals(rows.take(50).map { it.id }, model.state.value.detail.history.items.map { it.id })
        assertTrue(model.state.value.detail.history.hasMore)
        detailText("真实付款日期：2026-01-20")
        detailText("实际生效日期：2020-01-01")
        detailNode("subscription-detail-load-more-history").assertIsEnabled().performClick()
        compose.waitUntil(15_000) { model.state.value.detail.history.let { !it.loading && !it.loadingMore && it.items.size == 52 } }
        assertEquals(rows.map { it.id }, model.state.value.detail.history.items.map { it.id })
        assertFalse(model.state.value.detail.history.hasMore)
        assertEquals(52, model.state.value.detail.history.items.map { it.id }.distinct().size)
        detailNode("subscription-renewal-${rows.last().id}").assertIsDisplayed()
        observe(LocalDate.of(2026, 1, 21))
        assertEquals(rows.map { it.id }, model.state.value.detail.history.items.map { it.id })
        assertEquals(52L, generation.database.subscriptionDao().countRenewals(id))
    }

    @Test fun narrow_large_font_real_ime_saved_task_and_confirmation() = runBlocking<Unit> {
        val restoration = render(largeFont = true, restorable = true)!!
        compose.onNodeWithTag("home-create-subscription").performScrollTo().performClick()
        waitEditor(SubscriptionEditorKind.CREATE)
        compose.onNodeWithTag("subscription-name").performScrollTo().performTextReplacement("大字体保留原任务输入")
        fill("subscription", "180", BillingCycle.MONTHLY, "2026-01-05", "2026-02-01", "2026-03-01", "2026-02-26")
        val previousJob = model.viewModelScope.coroutineContext[Job]
        onMain {
            val restored = restoredHandle(savedState)
            savedState = restored
            model = SubscriptionViewModel(repository, restored, clock)
            retain("subscription", model)
            activeModel.value = model
        }
        previousJob?.join()
        restoration.emulateSavedInstanceStateRestore()
        waitEditor(SubscriptionEditorKind.CREATE)
        compose.onNodeWithTag("subscription-name").performScrollTo().assertTextContains("大字体保留原任务输入")
        assertCreateRaw("180", "2026-01-05", "2026-02-01", "2026-03-01", "2026-02-26")
        assertEquals(0L, generation.database.subscriptionDao().countSubscriptions())
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        Espresso.pressBack()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithTag("discard-dialog").assertIsDisplayed()
        compose.onNodeWithTag("discard-keep").performClick()
        assertCreateRaw("180", "2026-01-05", "2026-02-01", "2026-03-01", "2026-02-26")
        clickConfirm("subscription")
        compose.waitUntil(15_000) { model.state.value.editor == null && model.state.value.detail.detail != null }
        val id = model.state.value.detail.detail!!.subscription.id
        waitDetail(id)
        assertEquals(1L, generation.database.subscriptionDao().countRenewals(id))
        assertEquals(18000L, repository.loadSubscriptionDetail(id)!!.renewals.single().amountCents)
        detailNode("subscription-detail-no-current-plan").assertIsDisplayed()
    }

    private suspend fun attachOwners() {
        val storage = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(generation))
        onMain { holder = LedgerUiStateHolder(storage) { clock }; retain("holder", holder); activeHolder.value = holder }
        val ready = withTimeout(15_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        repository = RecordingRepository(generation, clock)
        onMain {
            val project = ProjectViewModel(ready.repository, SavedStateHandle(), clock)
            savedState = SavedStateHandle()
            model = SubscriptionViewModel(repository, savedState, clock)
            retain("project", project); retain("subscription", model)
            activeProject.value = project; activeModel.value = model
        }
    }

    private fun render(largeFont: Boolean = false, restorable: Boolean = false): StateRestorationTester? {
        val content: @Composable () -> Unit = {
            LithicTheme {
                val currentHolder = activeHolder.value
                val currentModel = activeModel.value
                val currentProject = activeProject.value
                if (showing.value && currentHolder != null && currentModel != null && currentProject != null) {
                    val ledger by currentHolder.state.collectAsState()
                    if (largeFont) {
                        val density = LocalDensity.current.density
                        CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
                            Box(Modifier.width(360.dp).fillMaxHeight()) {
                                LithicApp(ledger, {}, currentProject, subscriptionViewModel = currentModel)
                            }
                        }
                    } else LithicApp(ledger, {}, currentProject, subscriptionViewModel = currentModel)
                }
            }
        }
        val restoration = if (restorable) StateRestorationTester(compose).also { it.setContent(content) }
            else { compose.setContent(content); null }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        return restoration
    }

    private fun create(name: String, amount: String, paid: String, from: String, until: String, reminder: String): String {
        compose.onNodeWithTag("home-create-subscription").performScrollTo().performClick()
        waitEditor(SubscriptionEditorKind.CREATE)
        compose.onNodeWithTag("subscription-name").performScrollTo().performTextReplacement(name)
        fill("subscription", amount, BillingCycle.MONTHLY, paid, from, until, reminder)
        clickConfirm("subscription")
        compose.waitUntil(15_000) { model.state.value.editor == null && model.state.value.detail.detail != null }
        val id = model.state.value.detail.detail!!.subscription.id
        waitDetail(id)
        return id
    }

    private fun fill(prefix: String, amount: String, cycle: BillingCycle, paid: String, from: String, until: String, reminder: String) {
        compose.onNodeWithTag("$prefix-amount").performScrollTo().performTextReplacement(amount)
        compose.onNodeWithTag("$prefix-cycle-${cycle.name}").performScrollTo().performClick()
        compose.onNodeWithTag("$prefix-paid-on-input").performScrollTo().performTextReplacement(paid)
        compose.onNodeWithTag("$prefix-effective-from-input").performScrollTo().performTextReplacement(from)
        compose.onNodeWithTag("$prefix-paid-until-input").performScrollTo().performTextReplacement(until)
        compose.onNodeWithTag("$prefix-reminder-due-input").performScrollTo().performTextReplacement(reminder)
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
    }

    private fun clickConfirm(prefix: String) {
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        compose.onNodeWithTag("$prefix-confirm-payment").performScrollTo().assertIsDisplayed().assertIsEnabled().performClick()
    }

    private fun openRenewal() {
        detailNode("subscription-detail-renewal").assertIsEnabled().performClick()
        waitEditor(SubscriptionEditorKind.RENEWAL)
    }

    private fun waitEditor(kind: SubscriptionEditorKind) {
        compose.waitUntil(15_000) { model.state.value.editor?.let { it.kind == kind && !it.loading && !it.needsInitialRead && it.today != null } == true }
        compose.onNodeWithTag(if (kind == SubscriptionEditorKind.CREATE) "subscription-editor-screen" else "renewal-editor-screen").assertIsDisplayed()
    }

    private fun waitDetail(id: String) {
        compose.waitUntil(15_000) { model.state.value.let {
            it.editor == null && !it.detail.loading && !it.detail.history.loading && it.detail.detail?.subscription?.id == id
        } }
        compose.onNodeWithTag("subscription-detail-screen").assertIsDisplayed()
    }

    private fun detailNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun detailText(text: String) {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasText(text))
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    private fun assertEditorRaw(amount: String, paid: String, from: String, until: String, reminder: String) {
        assertRaw("renewal", amount, paid, from, until, reminder)
    }
    private fun assertCreateRaw(amount: String, paid: String, from: String, until: String, reminder: String) {
        assertRaw("subscription", amount, paid, from, until, reminder)
    }
    private fun assertRaw(prefix: String, amount: String, paid: String, from: String, until: String, reminder: String) {
        compose.onNodeWithTag("$prefix-amount").performScrollTo().assertTextContains(amount)
        compose.onNodeWithTag("$prefix-paid-on-input").performScrollTo().assertTextContains(paid)
        compose.onNodeWithTag("$prefix-effective-from-input").performScrollTo().assertTextContains(from)
        compose.onNodeWithTag("$prefix-paid-until-input").performScrollTo().assertTextContains(until)
        compose.onNodeWithTag("$prefix-reminder-due-input").performScrollTo().assertTextContains(reminder)
        val editor = model.state.value.editor!!
        assertEquals(amount, editor.amount); assertEquals(BillingCycle.MONTHLY, editor.cycle)
        assertEquals(paid, editor.paidOn); assertEquals(from, editor.effectiveFrom)
        assertEquals(until, editor.paidUntilExclusive); assertEquals(reminder, editor.reminderDueOn)
    }

    private fun observe(day: LocalDate) {
        clock.date = day
        onMain { model.refresh() } // Observation/read only; no direct business save.
        compose.waitUntil(15_000) { model.state.value.detail.let { !it.loading && !it.history.loading && it.detail?.asOf == day } }
    }

    private fun openFromList(id: String) {
        compose.onNodeWithTag("open-subscription-list").performScrollTo().performClick()
        compose.waitUntil(15_000) { !model.state.value.list.loading && model.state.value.list.items.any { it.subscription.id == id } }
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$id"))
        compose.onNodeWithTag("subscription-row-$id").performClick()
        waitDetail(id)
    }

    private suspend fun reopenFixture() {
        compose.runOnIdle { showing.value = false }
        compose.waitForIdle()
        clearOwners()
        generation.close()
        generation = DataGenerationStore(context, root).open(ZoneId.of("Pacific/Honolulu"))
        attachOwners()
        compose.runOnIdle { showing.value = true }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    private fun retain(key: String, value: ViewModel) { stores.put(key, value); value.viewModelScope.coroutineContext[Job]?.let(jobs::add) }
    private suspend fun clearOwners() {
        onMain { stores.clear(); activeHolder.value = null; activeModel.value = null; activeProject.value = null }
        jobs.forEach { it.join() }
        jobs.clear()
    }
    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun restoredHandle(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { key ->
        val value = handle.get<Any?>(key)
        if (value is Bundle) Bundle(value) else value
    })
    private fun <T> onMainValue(block: () -> T): T {
        var result: T? = null
        onMain { result = block() }
        return requireNotNull(result)
    }

    private class MutableBusinessClock(@Volatile var date: LocalDate) : BusinessClock {
        override fun today() = date
    }
    private class RecordingRepository(generation: ManagedGeneration, clock: BusinessClock) :
        SubscriptionRepository(generation.database, generation.coordinator, clock) {
        val renewalCommands = CopyOnWriteArrayList<ConfirmRenewalCommand>()
        val renewalResults = CopyOnWriteArrayList<SubscriptionMutationResult>()
        @Volatile var loseNextRenewalResult = false
        override suspend fun confirmRenewal(command: ConfirmRenewalCommand): SubscriptionMutationResult {
            renewalCommands += command
            val result = super.confirmRenewal(command)
            renewalResults += result
            if (loseNextRenewalResult && result is SubscriptionMutationResult.Success) {
                loseNextRenewalResult = false
                throw IOException("Synthetic result delivery interruption after actual Room commit")
            }
            return result
        }
    }
    private companion object { const val FORM_KEY = "lithic.subscription.activeForm" }
}
