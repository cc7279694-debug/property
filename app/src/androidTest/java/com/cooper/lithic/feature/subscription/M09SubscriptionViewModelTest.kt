package com.cooper.lithic.feature.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room current reads and saved immutable lifecycle envelopes. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionViewModelTest {
    private val fixture = M09SubscriptionRoomFixture()
    @Before fun before() = runBlocking { fixture.open() }
    @After fun after() = runBlocking { fixture.close() }

    @Test fun current_management_excludes_effective_stopped_but_keeps_paused_and_expired() = runBlocking {
        val active = fixture.seed(M09SubscriptionFixtures.facts("正在订阅"))
        val paused = fixture.seed(M09SubscriptionFixtures.facts("当前暂停", SubscriptionOperationState.PAUSED))
        val expiredFacts = M09SubscriptionFixtures.facts("当前到期").let { facts -> facts.copy(renewals = facts.renewals.map {
            it.copy(effectiveFrom = LocalDate.of(2025, 12, 1), paidUntilExclusive = LocalDate.of(2026, 1, 1))
        }) }
        val expired = fixture.seed(expiredFacts)
        val stopped = fixture.seed(M09SubscriptionFixtures.facts("历史停止", SubscriptionOperationState.STOPPED))
        val model = fixture.model()
        fixture.onMain { model.loadList() }
        withTimeout(10_000) { model.state.first { !it.list.loading } }
        assertEquals("Current management must never contain effective STOPPED identities",
            setOf(active, paused, expired), model.state.value.list.items.map { it.subscription.id }.toSet())
        assertEquals(3L, model.state.value.list.totalCount)
        assertEquals(SubscriptionDisplayStatus.STOPPED, fixture.repository.loadSubscriptionDetail(stopped)!!.cost.displayStatus)
    }

    @Test fun future_stopped_boundary_remains_current_until_it_becomes_effective() = runBlocking {
        val facts = M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED,
            stateOn = LocalDate.of(2026, 3, 1), futurePayment = true)
        fixture.seed(facts)
        val model = fixture.model()
        fixture.onMain { model.loadList() }
        withTimeout(10_000) { model.state.first { !it.list.loading } }
        assertEquals(listOf(facts.subscription.id), model.state.value.list.items.map { it.subscription.id })
        assertEquals(SubscriptionDisplayStatus.ACTIVE, model.state.value.list.items.single().cost.displayStatus)
        assertEquals(2L, fixture.generation.database.subscriptionDao().countRenewals(facts.subscription.id))
    }

    @Test fun restored_inflight_state_keeps_exact_envelope_and_blocks_departure() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        fixture.seed(facts)
        val form = M09SubscriptionFixtures.stateForm(facts, SubmissionPhase.IN_FLIGHT)
        val submitted = requireNotNull(form.getBundle("submitted"))
        val handle = SavedStateHandle(mapOf(M09SubscriptionFixtures.FORM_KEY to form))
        val model = fixture.model(handle)
        assertEquals("A restored state write must remain guarded until its original result is checked",
            LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        val restored = handle.get<android.os.Bundle>(M09SubscriptionFixtures.FORM_KEY)
        assertNotNull("The only active form must retain the submitted lifecycle facts", restored)
        assertEquals(SubmissionPhase.UNCONFIRMED.name, restored!!.getString("phase"))
        val after = requireNotNull(restored.getBundle("submitted"))
        listOf("commandId", "stateId", "subscriptionId", "action", "effectiveOn", "reason", "writtenAt").forEach {
            assertEquals("The immutable submitted $it must survive recreation", submitted.getString(it), after.getString(it))
        }
        assertEquals(1L, fixture.generation.database.subscriptionDao().countRenewals(facts.subscription.id))
        assertEquals(emptyList<Any>(), fixture.generation.database.subscriptionDao().statesForSubscriptions(listOf(facts.subscription.id)))
        assertNull(model.state.value.committedSubscriptionId)
    }

    @Test fun restored_dirty_state_requires_explicit_discard_without_writing_facts() = runBlocking {
        val facts = M09SubscriptionFixtures.facts()
        fixture.seed(facts)
        val handle = SavedStateHandle(mapOf(M09SubscriptionFixtures.FORM_KEY to
            M09SubscriptionFixtures.stateForm(facts, SubmissionPhase.IDLE)))
        val model = fixture.model(handle)
        assertEquals(LeaveDecision.CONFIRM_DISCARD, model.leaveDecision())
        assertNotNull(handle.get<android.os.Bundle>(M09SubscriptionFixtures.FORM_KEY))
        fixture.onMain { model.discardForm() }
        assertNull(handle.get<android.os.Bundle>(M09SubscriptionFixtures.FORM_KEY))
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        assertEquals(emptyList<Any>(), fixture.generation.database.subscriptionDao().statesForSubscriptions(listOf(facts.subscription.id)))
    }
}
