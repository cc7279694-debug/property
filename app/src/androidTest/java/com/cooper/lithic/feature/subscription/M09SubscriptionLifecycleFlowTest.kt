package com.cooper.lithic.feature.subscription

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.app.StorageInitializationState
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.ProjectViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Formal Nav3; every M09 mutation originates from the visible confirmation operation. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionLifecycleFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = M09SubscriptionRoomFixture()
    private lateinit var holder: LedgerUiStateHolder
    private lateinit var project: ProjectViewModel
    private lateinit var model: SubscriptionViewModel
    @Before fun before() = runBlocking<Unit> { fixture.open() }
    @After fun after() = runBlocking<Unit> { fixture.close() }

    @Test fun expiry_needs_no_worker() = runBlocking<Unit> {
        val facts = seed()
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-detail-current-rate").assertIsDisplayed()
        observe(LocalDate.of(2026, 2, 1))
        detailNode("subscription-detail-status").assertTextContains("已到期")
        detailNode("subscription-detail-no-current-plan").assertIsDisplayed()
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(SubscriptionDisplayStatus.EXPIRED, after.cost.displayStatus)
        assertNull(after.cost.currentPrice); assertEquals(0, after.cost.currentRate.signum())
        assertEquals(facts.renewals, after.renewals); assertEquals(emptyList<Any>(), after.stateChanges)
    }

    @Test fun cancel_renewal_keeps_paid_access() = runBlocking<Unit> {
        val facts = seed(M09SubscriptionFixtures.facts(futurePayment = true))
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-detail-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-state-effective-input").performScrollTo().assertTextContains("2026-03-01")
        confirmState(facts.subscription.id)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.cost.displayStatus)
        detailNode("subscription-detail-scheduled-stop").assertTextContains("2026-03-01", substring = true)
        detailNode("subscription-detail-correct-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-state-effective-input").performScrollTo().performTextReplacement("2026-02-28")
        confirmState(facts.subscription.id)
        detailNode("subscription-detail-scheduled-stop").assertTextContains("2026-02-28", substring = true)
        detailNode("subscription-detail-cancel-stop").performClick(); waitState(); confirmState(facts.subscription.id)
        assertEquals(emptyList<Any>(), fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges)
        detailNode("subscription-detail-stop").performClick(); waitState(); confirmState(facts.subscription.id)
        observe(LocalDate.of(2026, 3, 1))
        val boundary = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(SubscriptionDisplayStatus.STOPPED, boundary.cost.displayStatus)
        assertEquals(facts.renewals.toSet(), boundary.renewals.toSet())
        assertEquals(30000L, boundary.cost.confirmedPaidCents); assertEquals(60L, boundary.cost.standardPurchasedDays)
    }

    @Test fun pause_is_not_history() = runBlocking<Unit> {
        val facts = seed()
        val id = facts.subscription.id
        render(); openCurrent(id)
        detailNode("subscription-detail-pause").performClick(); waitState(); confirmState(id)
        detailNode("subscription-detail-back").performClick()
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-group-PAUSED"))
        compose.onNodeWithTag("subscription-group-PAUSED").assertTextContains("暂停中")
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("review-subscription-history").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.state.value.stoppedHistory.loading }
        compose.onNodeWithTag("subscription-history-empty").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").performClick(); openCurrent(id)
        detailNode("subscription-detail-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-stop-early").performScrollTo().performClick(); confirmState(id)
        detailNode("subscription-detail-back").assertTextContains("已停止订阅", substring = true).performClick()
        compose.onNodeWithTag("subscription-history-content").performScrollToNode(hasTestTag("subscription-history-row-$id"))
        compose.onNodeWithTag("subscription-history-row-$id").performClick(); waitDetail(id)
        detailNode("subscription-detail-reopen").performClick(); waitState(); confirmState(id)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, fixture.repository.loadSubscriptionDetail(id)!!.cost.displayStatus)
        detailNode("subscription-detail-back").assertTextContains("订阅列表", substring = true).performClick()
        compose.onNodeWithTag("subscription-list-screen").assertIsDisplayed()
        assertEquals(1L, fixture.generation.database.subscriptionDao().countSubscriptions())
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(id)!!.renewals)
    }

    @Test fun resume_without_new_payment() = runBlocking<Unit> {
        val facts = seed(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.PAUSED,
            stateOn = LocalDate.of(2026, 1, 20)))
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-detail-resume").performClick(); waitState(); confirmState(facts.subscription.id)
        val covered = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(SubscriptionDisplayStatus.ACTIVE, covered.cost.displayStatus)
        assertEquals(facts.renewals, covered.renewals)
        assertEquals(20L, covered.cost.elapsedPaidDays) // Jan 1-19 and Jan 25, inclusive.
        detailNode("subscription-detail-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-stop-early").performScrollTo().performClick(); confirmState(facts.subscription.id)
        observe(LocalDate.of(2026, 2, 2))
        detailNode("subscription-detail-reopen").performClick()
        compose.waitUntil(10_000) { model.state.value.editor?.canEdit == true }
        compose.onNodeWithTag("renewal-amount").performScrollTo().assertTextContains("")
        assertEquals("", model.state.value.editor!!.amount); assertEquals("", model.state.value.editor!!.paidOn)
        fillPayment("180", "2026-02-02", "2026-02-02", "2026-03-02", "2026-02-27")
        compose.onNodeWithTag("renewal-confirm-payment").performClick(); waitDetail(facts.subscription.id)
        val reopened = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(facts.subscription, reopened.subscription)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, reopened.cost.displayStatus)
        assertEquals(2, reopened.renewals.size)
        detailNode("subscription-detail-back").assertTextContains("订阅列表", substring = true)
    }

    @Test fun future_prepaid_has_own_label() = runBlocking<Unit> {
        val facts = seed(M09SubscriptionFixtures.facts(futurePayment = true))
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-renewal-future-${facts.renewals.last().id}").assertTextContains("尚未生效")
        assertEquals(listOf(LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 1)), model.state.value.detail.history.items.map { it.paidOn })
        detailNode("subscription-detail-pause").performClick(); waitState(); confirmState(facts.subscription.id)
        detailNode("subscription-detail-resume").performClick(); waitState(); confirmState(facts.subscription.id)
        detailNode("subscription-renewal-future-${facts.renewals.last().id}").assertTextContains("尚未生效")
        assertEquals(facts.renewals.toSet(), fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals.toSet())
        assertEquals(60L, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.cost.standardPurchasedDays)
    }

    @Test fun stop_reason_does_not_cancel_vendor() = runBlocking<Unit> {
        val facts = seed()
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-detail-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-state-vendor-explanation").performScrollTo()
            .assertTextContains("不会替服务商取消自动续费", substring = true)
        compose.onNodeWithTag("subscription-state-back").performClick(); waitDetail(facts.subscription.id)
        assertEquals(emptyList<Any>(), fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges)
        detailNode("subscription-detail-stop").performClick(); waitState()
        compose.onNodeWithTag("subscription-state-reason").performScrollTo().performTextReplacement("用户确认停止记录的原因")
        confirmState(facts.subscription.id)
        assertEquals("用户确认停止记录的原因", fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.single().reason)
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals)
    }

    @Test fun payment_history_correction_and_delete_require_visible_confirmation() = runBlocking<Unit> {
        val facts = seed(M09SubscriptionFixtures.facts(futurePayment = true))
        val original = facts.renewals.first()
        val future = facts.renewals.last()
        render(); openCurrent(facts.subscription.id)
        detailNode("subscription-renewal-correct-${original.id}").performClick()
        compose.waitUntil(10_000) { model.state.value.editor?.canEdit == true }
        compose.onNodeWithTag("renewal-amount").performScrollTo().performTextReplacement("146.83")
        assertEquals(original, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals.single { it.id == original.id })
        Espresso.closeSoftKeyboard(); compose.onNodeWithTag("renewal-confirm-payment").performClick(); waitDetail(facts.subscription.id)
        assertEquals(original.copy(amountCents = 14683), fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals.single { it.id == original.id })
        detailNode("subscription-renewal-delete-${future.id}").performClick(); waitState()
        compose.onNodeWithTag("subscription-delete-payment-explanation").performScrollTo().assertTextContains("这不是退款", substring = true)
        assertEquals(2, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals.size)
        confirmState(facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(listOf(original.copy(amountCents = 14683)), after.renewals)
        assertEquals(14683L, after.cost.confirmedPaidCents); assertEquals(30L, after.cost.standardPurchasedDays)
        detailNode("subscription-renewal-delete-${original.id}").performClick(); waitState()
        compose.onNodeWithTag("subscription-delete-last-payment").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("subscription-state-confirm").assertDoesNotExist()
        assertEquals(1L, fixture.generation.database.subscriptionDao().countSubscriptions())
    }

    private suspend fun seed(facts: SubscriptionFacts = M09SubscriptionFixtures.facts()): SubscriptionFacts { fixture.seed(facts); return facts }
    private suspend fun render() {
        val states = MutableStateFlow<StorageInitializationState>(StorageInitializationState.Ready(fixture.generation))
        fixture.onMain { holder = LedgerUiStateHolder(states) { fixture.clock }; fixture.retain("holder", holder) }
        val ready = withTimeout(10_000) { holder.state.filterIsInstance<LedgerUiState.Ready>().first() }
        fixture.onMain { project = ProjectViewModel(ready.repository, SavedStateHandle(), fixture.clock); fixture.retain("project", project) }
        model = fixture.model()
        compose.setContent { LithicTheme { val ledger by holder.state.collectAsState(); LithicApp(ledger, {}, project, subscriptionViewModel = model) } }
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }
    private fun openCurrent(id: String) {
        compose.onNodeWithTag("open-subscription-list").performScrollTo().performClick()
        compose.waitUntil(10_000) { !model.state.value.list.loading && model.state.value.list.items.any { it.subscription.id == id } }
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(hasTestTag("subscription-row-$id"))
        compose.onNodeWithTag("subscription-row-$id").performClick(); waitDetail(id)
    }
    private fun waitState() {
        compose.waitUntil(10_000) { model.state.value.stateEditor?.let { !it.loading && !it.needsInitialRead } == true }
        compose.onNodeWithTag("subscription-state-screen").assertIsDisplayed()
    }
    private fun confirmState(id: String) {
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
        compose.onNodeWithTag("subscription-state-confirm").assertIsEnabled().performClick(); waitDetail(id)
    }
    private fun waitDetail(id: String) {
        compose.waitUntil(10_000) { model.state.value.let { it.editor == null && it.stateEditor == null && !it.detail.loading &&
            !it.detail.history.loading && it.detail.detail?.subscription?.id == id } }
        compose.onNodeWithTag("subscription-detail-screen").assertIsDisplayed()
    }
    private fun detailNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        val management = tag in setOf("subscription-detail-vendor-explanation", "subscription-detail-pause",
            "subscription-detail-resume", "subscription-detail-stop", "subscription-detail-reopen",
            "subscription-detail-scheduled-stop", "subscription-detail-correct-stop", "subscription-detail-cancel-stop", "subscription-price-history")
        val open = compose.onAllNodesWithTag("subscription-detail-close-management").fetchSemanticsNodes().isNotEmpty()
        if (management) {
            if (!open) compose.onNodeWithTag("subscription-detail-open-management").performClick()
            return compose.onNodeWithTag(tag).performScrollTo()
        }
        if (open) compose.onNodeWithTag("subscription-detail-close-management").performClick()
        if (tag != "subscription-detail-renewal")
            compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
    private fun observe(date: LocalDate) {
        fixture.clock.date = date; fixture.onMain { model.refresh() }
        compose.waitUntil(10_000) { model.state.value.detail.detail?.asOf == date && !model.state.value.detail.loading }
    }
    private fun fillPayment(amount: String, paidOn: String, from: String, until: String, reminder: String) {
        compose.onNodeWithTag("renewal-amount").performScrollTo().performTextReplacement(amount)
        compose.onNodeWithTag("renewal-paid-on-input").performScrollTo().performTextReplacement(paidOn)
        compose.onNodeWithTag("renewal-effective-from-input").performScrollTo().performTextReplacement(from)
        compose.onNodeWithTag("renewal-paid-until-input").performScrollTo().performTextReplacement(until)
        compose.onNodeWithTag("renewal-reminder-due-input").performScrollTo().performTextReplacement(reminder)
        Espresso.closeSoftKeyboard(); compose.waitForIdle()
    }
}
