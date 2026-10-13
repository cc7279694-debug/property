package com.cooper.lithic.feature.subscription

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.data.repository.SetSubscriptionStateCommand
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionFacts
import com.cooper.lithic.data.repository.SubscriptionLifecycleResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.SubmissionPhase
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Actual command consumers and actual Room outcomes, not button-display coverage. */
@RunWith(AndroidJUnit4::class)
class M09SubscriptionLifecycleViewModelTest {
    private val fixture = M09SubscriptionRoomFixture()
    @Before fun before() = runBlocking { fixture.open() }
    @After fun after() = runBlocking { fixture.close() }

    @Test fun explicit_pause_saves_one_state_and_preserves_all_payment_facts() = runBlocking {
        val facts = seeded()
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.changeStateReason("主动暂停"); model.save() }
        committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(facts.renewals, after.renewals)
        assertEquals(SubscriptionDisplayStatus.PAUSED, after.cost.displayStatus)
        assertEquals("主动暂停", after.stateChanges.single().reason)
        assertEquals(LocalDate.of(2026, 1, 25), after.stateChanges.single().effectiveOn)
    }

    @Test fun covered_resume_corrects_same_day_boundary_without_new_payment() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.PAUSED))
        val oldState = facts.stateChanges.single()
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.RESUME) }
        fixture.onMain { model.save() }; committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(facts.renewals, after.renewals)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, after.cost.displayStatus)
        assertEquals(oldState.id, after.stateChanges.single().id)
        assertEquals(oldState.operationId, after.stateChanges.single().operationId)
        assertEquals(SubscriptionOperationState.ENABLED, after.stateChanges.single().state)
    }

    @Test fun uncovered_reopen_requires_explicit_amount_and_paid_date_without_prefill() = runBlocking {
        val facts = seeded(expiredStopped())
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.REOPEN, "HISTORY") }
        val editor = requireNotNull(model.state.value.editor)
        assertEquals(SubscriptionEditorKind.REENABLE, editor.kind)
        assertEquals("", editor.amount)
        assertEquals("", editor.paidOn)
        fixture.onMain { model.save() }
        assertTrue(model.state.value.editor!!.errors.containsKey("amount"))
        assertTrue(model.state.value.editor!!.errors.containsKey("paidOn"))
        assertNull(model.state.value.committedSubscriptionId)
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals)
        assertEquals(facts.stateChanges, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges)
    }

    @Test fun uncovered_reopen_explicit_payment_and_enabled_commit_under_same_identity() = runBlocking {
        val facts = seeded(expiredStopped())
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.REOPEN, "HISTORY") }
        fixture.onMain {
            model.changeAmount("180.10"); model.changeCycle(BillingCycle.MONTHLY)
            model.changePaidOn("2026-01-25"); model.changeEffectiveFrom("2026-01-25")
            model.changePaidUntil("2026-02-25"); model.changeReminderDue("2026-02-22"); model.save()
        }
        committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(facts.subscription, after.subscription)
        assertEquals(1L, fixture.generation.database.subscriptionDao().countSubscriptions())
        assertEquals(2, after.renewals.size)
        assertEquals(18010L, after.renewals.first().amountCents)
        assertNotEquals(facts.renewals.single().id, after.renewals.first().id)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, after.cost.displayStatus)
        assertEquals(SubscriptionOperationState.ENABLED, after.stateChanges.last().state)
        assertEquals(facts.stateChanges.single(), after.stateChanges.first())
    }

    @Test fun only_future_paid_reopen_records_enabled_but_keeps_expired_and_payment_count() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED,
            stateOn = LocalDate.of(2026, 1, 2), futurePayment = true).let { value -> value.copy(renewals = value.renewals.mapIndexed { index, row ->
            if (index == 0) row.copy(effectiveFrom = LocalDate.of(2025, 12, 1), paidUntilExclusive = LocalDate.of(2026, 1, 1)) else row
        }) })
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.REOPEN, "HISTORY") }
        assertNull(model.state.value.editor)
        fixture.onMain { model.save() }; committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(facts.renewals.toSet(), after.renewals.toSet())
        assertEquals(SubscriptionDisplayStatus.EXPIRED, after.cost.displayStatus)
        assertNull(after.cost.currentPrice)
        assertEquals(2L, fixture.generation.database.subscriptionDao().countRenewals(facts.subscription.id))
    }

    @Test fun default_stop_uses_last_confirmed_prepaid_boundary_and_preserves_money() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(futurePayment = true))
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.STOP_AT_END) }
        assertEquals("2026-03-01", model.state.value.stateEditor!!.effectiveOn)
        fixture.onMain { model.save() }; committed(model, facts.subscription.id)
        val beforeBoundary = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(SubscriptionDisplayStatus.ACTIVE, beforeBoundary.cost.displayStatus)
        assertEquals(LocalDate.of(2026, 3, 1), beforeBoundary.stateChanges.single().effectiveOn)
        val boundary = fixture.repository.loadSubscriptionDetail(facts.subscription.id, LocalDate.of(2026, 3, 1))!!
        assertEquals(SubscriptionDisplayStatus.STOPPED, boundary.cost.displayStatus)
        assertEquals(30000L, boundary.cost.confirmedPaidCents)
        assertEquals(60L, boundary.cost.standardPurchasedDays)
    }

    @Test fun explicit_early_stop_keeps_original_payments_and_future_fixed_plan() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(futurePayment = true))
        val model = fixture.model()
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.STOP_AT_END) }
        fixture.onMain { model.changeStopEarly(true); model.changeStateEffectiveOn("2026-01-25"); model.save() }
        committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(SubscriptionDisplayStatus.STOPPED, after.cost.displayStatus)
        assertEquals(facts.renewals.toSet(), after.renewals.toSet())
        assertEquals(30000L, after.cost.confirmedPaidCents)
        assertEquals(60L, after.cost.standardPurchasedDays)
    }

    @Test fun state_history_correction_keeps_original_row_and_operation_identity() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.PAUSED,
            stateOn = LocalDate.of(2026, 1, 20)))
        val original = facts.stateChanges.single()
        val model = fixture.model()
        start(model) { model.startCorrectState(facts.subscription.id, original.id) }
        fixture.onMain { model.changeStateEffectiveOn("2026-01-21"); model.changeStateReason("真实日期纠错"); model.save() }
        committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.single()
        assertEquals(original.id, after.id); assertEquals(original.operationId, after.operationId)
        assertEquals(LocalDate.of(2026, 1, 21), after.effectiveOn); assertEquals("真实日期纠错", after.reason)
    }

    @Test fun cancel_future_stop_deletes_only_target_and_preserves_other_facts() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED,
            stateOn = LocalDate.of(2026, 3, 1), futurePayment = true))
        val model = fixture.model()
        start(model) { model.startCancelStop(facts.subscription.id, facts.stateChanges.single().id) }
        fixture.onMain { model.save() }; committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(emptyList<Any>(), after.stateChanges)
        assertEquals(facts.renewals.toSet(), after.renewals.toSet())
        assertEquals(SubscriptionDisplayStatus.ACTIVE, after.cost.displayStatus)
    }

    @Test fun payment_correction_updates_original_fact_without_appending_or_changing_operation() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(futurePayment = true))
        val original = facts.renewals.first()
        val model = fixture.model()
        start(model) { model.startCorrectRenewal(facts.subscription.id, original.id) }
        fixture.onMain { model.changeAmount("146.83"); model.save() }; committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        val changed = after.renewals.single { it.id == original.id }
        assertEquals(2, after.renewals.size); assertEquals(14683L, changed.amountCents)
        assertEquals(original.operationId, changed.operationId); assertEquals(original.subscriptionId, changed.subscriptionId)
        assertEquals(original.copy(amountCents = 14683L), changed)
    }

    @Test fun explicit_delete_error_payment_removes_one_row_and_recomputes_without_refund() = runBlocking {
        val facts = seeded(M09SubscriptionFixtures.facts(futurePayment = true))
        val deleted = facts.renewals.last()
        val model = fixture.model()
        start(model) { model.startDeleteRenewal(facts.subscription.id, deleted.id) }
        fixture.onMain { model.save() }; committed(model, facts.subscription.id)
        val after = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        assertEquals(listOf(facts.renewals.first()), after.renewals)
        assertEquals(15000L, after.cost.confirmedPaidCents); assertEquals(30L, after.cost.standardPurchasedDays)
        assertEquals(1L, fixture.generation.database.subscriptionDao().countSubscriptions())
    }

    @Test fun last_payment_delete_is_locked_and_explained_without_destroying_identity() = runBlocking {
        val facts = seeded()
        val model = fixture.model()
        start(model) { model.startDeleteRenewal(facts.subscription.id, facts.renewals.single().id) }
        assertTrue(model.state.value.stateEditor!!.lastPayment)
        assertTrue(model.state.value.stateEditor!!.message?.contains("最后") == true)
        fixture.onMain { model.save() }
        assertNull(model.state.value.committedSubscriptionId)
        assertEquals(facts.renewals, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals)
    }

    @Test fun lost_state_result_restores_same_immutable_command_and_replays_without_duplicates() = runBlocking {
        val facts = seeded()
        val handle = SavedStateHandle()
        val lossy = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                super.setState(command); throw IOException("Synthetic delivery loss after actual state commit")
            }
        }
        val first = fixture.model(handle, lossy)
        start(first) { first.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { first.save() }; await(first) { it.stateEditor?.phase == SubmissionPhase.UNCONFIRMED }
        val before = fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!
        val submitted = Bundle(requireNotNull(handle.get<Bundle>(M09SubscriptionFixtures.FORM_KEY)?.getBundle("submitted")))
        val restored = fixture.model(restoredHandle(handle))
        assertEquals(LeaveDecision.BLOCK_PENDING, restored.leaveDecision())
        fixture.onMain { restored.changeStateReason("不可覆盖提交内容"); restored.save() }
        committed(restored, facts.subscription.id)
        assertEquals(before.stateChanges, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges)
        val afterSubmitted = requireNotNull(handle.get<Bundle>(M09SubscriptionFixtures.FORM_KEY)?.getBundle("submitted"))
        assertEquals(submitted.getString("commandId"), afterSubmitted.getString("commandId"))
        assertEquals(1, before.stateChanges.size)
    }

    @Test fun mismatched_saved_state_identity_remains_pending_and_cannot_navigate() = runBlocking {
        val facts = seeded()
        val wrong = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                val result = super.setState(command)
                return if (result is SubscriptionLifecycleResult.Saved) result.copy(stateId = UUID.randomUUID().toString()) else result
            }
        }
        val model = fixture.model(repo = wrong)
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.save() }; await(model) { it.stateEditor?.phase == SubmissionPhase.UNCONFIRMED }
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertNull(model.state.value.committedSubscriptionId)
        assertEquals(1, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.size)
    }

    @Test fun committed_state_read_failure_only_rereads_without_writing_again() = runBlocking {
        val facts = seeded()
        var failRead = false
        var writes = 0
        val unreadable = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                writes += 1
                val result = super.setState(command); failRead = true; return result
            }
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (failRead) throw IOException("Synthetic committed read failure")
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        val model = fixture.model(repo = unreadable)
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.save() }; await(model) { it.stateEditor?.phase == SubmissionPhase.COMMITTED && !it.stateEditor.loading }
        assertNull(model.state.value.committedSubscriptionId)
        fixture.onMain { model.save(); model.retryRead() }
        assertEquals(1, writes)
        failRead = false; fixture.onMain { model.retryRead() }; committed(model, facts.subscription.id)
        assertEquals(1, writes)
        assertEquals(1, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.size)
    }

    @Test fun inflight_state_locks_duplicate_save_and_preserves_reason_and_target() = runBlocking {
        val facts = seeded()
        val entered = CompletableDeferred<Unit>(); val released = CompletableDeferred<Unit>()
        val held = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                entered.complete(Unit); released.await(); return super.setState(command)
            }
        }
        val model = fixture.model(repo = held)
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.changeStateReason("确认的原因"); model.save() }
        withTimeout(10_000) { entered.await() }
        fixture.onMain { model.changeStateReason("禁止修改"); model.save() }
        assertEquals("确认的原因", model.state.value.stateEditor!!.reason)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        released.complete(Unit); committed(model, facts.subscription.id)
        assertEquals("确认的原因", fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.single().reason)
    }

    @Test fun committed_later_facts_require_explicit_current_read_and_safe_conflict_exit() = runBlocking {
        val facts = seeded()
        val nextCommand = UUID.randomUUID().toString()
        val nextPayment = facts.renewals.single().copy(id = UUID.randomUUID().toString(), paidOn = LocalDate.of(2026, 1, 25),
            effectiveFrom = LocalDate.of(2026, 2, 1), paidUntilExclusive = LocalDate.of(2026, 3, 1),
            reminderDueOn = LocalDate.of(2026, 2, 26), amountCents = 18000, operationId = nextCommand)
        val changedAfterCommit = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                val result = super.setState(command)
                if (result is SubscriptionLifecycleResult.Saved) super.confirmRenewal(ConfirmRenewalCommand(nextCommand, nextPayment))
                return result
            }
        }
        val model = fixture.model(repo = changedAfterCommit)
        start(model) { model.startStateAction(facts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.save() }
        await(model) { it.stateEditor?.phase == SubmissionPhase.COMMITTED && !it.stateEditor.loading && it.stateEditor.message != null }
        assertNull("A stale full candidate cannot automatically navigate", model.state.value.committedSubscriptionId)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertTrue("A successful read with later changed facts must be distinct from a read failure: ${model.state.value.stateEditor}",
            model.state.value.stateEditor!!.conflict)
        fixture.onMain { model.retryRead() }
        await(model) { it.stateEditor?.verifiedCannotWrite == true }
        assertEquals(LeaveDecision.ALLOW, model.leaveDecision())
        assertNull(model.state.value.committedSubscriptionId)
        assertTrue(model.state.value.stateEditor!!.message?.contains("变化") == true)
        assertEquals(2, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.renewals.size)
        assertEquals(1, fixture.repository.loadSubscriptionDetail(facts.subscription.id)!!.stateChanges.size)
    }

    @Test fun slow_committed_read_cannot_redirect_a_later_opened_detail() = runBlocking {
        val firstFacts = seeded()
        val secondFacts = seeded(M09SubscriptionFixtures.facts("后来明确打开的服务"))
        val entered = CompletableDeferred<Unit>(); val released = CompletableDeferred<Unit>()
        var pauseNextRead = false
        val delayed = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult {
                val result = super.setState(command); pauseNextRead = true; return result
            }
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (id == firstFacts.subscription.id && pauseNextRead) {
                    pauseNextRead = false; entered.complete(Unit); released.await()
                }
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        val model = fixture.model(repo = delayed)
        start(model) { model.startStateAction(firstFacts.subscription.id, SubscriptionStateAction.PAUSE) }
        fixture.onMain { model.save() }; withTimeout(10_000) { entered.await() }
        fixture.onMain { model.openDetail(secondFacts.subscription.id) }
        released.complete(Unit)
        await(model) { it.detail.detail?.subscription?.id == secondFacts.subscription.id && it.stateEditor?.loading == false }
        assertNull("A slow original commit read cannot redirect a subsequently opened detail", model.state.value.committedSubscriptionId)
        assertEquals(secondFacts.subscription.id, model.state.value.detail.detail!!.subscription.id)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertEquals(1, fixture.repository.loadSubscriptionDetail(firstFacts.subscription.id)!!.stateChanges.size)
    }

    @Test fun slow_resolved_conflict_read_cannot_redirect_a_later_opened_detail() = runBlocking {
        val firstFacts = seeded()
        val secondFacts = seeded(M09SubscriptionFixtures.facts("后来打开的当前订阅"))
        val form = M09SubscriptionFixtures.stateForm(firstFacts, SubmissionPhase.UNCONFIRMED).apply { putBoolean("conflict", true) }
        val envelope = requireNotNull(form.getBundle("submitted"))
        val targetBundle = requireNotNull(envelope.getBundle("targetState"))
        val target = SubscriptionStateChangeEntity(requireNotNull(targetBundle.getString("id")), firstFacts.subscription.id,
            LocalDate.of(2026, 1, 25), SubscriptionOperationState.PAUSED, "保留原始原因", "保留原始备注",
            requireNotNull(targetBundle.getString("operationId")))
        val stored = fixture.repository.setState(SetSubscriptionStateCommand(requireNotNull(form.getString("commandId")),
            firstFacts, target, SubscriptionStateAction.PAUSE))
        assertTrue("The exact original target must be stored by the real repository before recovery", stored is SubscriptionLifecycleResult.Saved)
        val entered = CompletableDeferred<Unit>(); val released = CompletableDeferred<Unit>()
        val delayed = object : SubscriptionRepository(fixture.generation.database, fixture.generation.coordinator, fixture.clock) {
            override suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate): SubscriptionDetail? {
                if (id == firstFacts.subscription.id) { entered.complete(Unit); released.await() }
                return super.loadSubscriptionDetail(id, asOf)
            }
        }
        val handle = SavedStateHandle(mapOf(M09SubscriptionFixtures.FORM_KEY to form))
        val model = fixture.model(handle, delayed)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        fixture.onMain { model.retryRead() }; withTimeout(10_000) { entered.await() }
        fixture.onMain { model.openDetail(secondFacts.subscription.id) }; released.complete(Unit)
        await(model) { it.detail.detail?.subscription?.id == secondFacts.subscription.id && it.stateEditor?.loading == false }
        assertNull("A late original conflict read must not redirect a later detail", model.state.value.committedSubscriptionId)
        assertEquals(LeaveDecision.BLOCK_PENDING, model.leaveDecision())
        assertEquals(SubmissionPhase.UNCONFIRMED, model.state.value.stateEditor!!.phase)
        assertEquals(secondFacts.subscription.id, model.state.value.detail.detail!!.subscription.id)
        val restoredSubmission = requireNotNull(handle.get<Bundle>(M09SubscriptionFixtures.FORM_KEY)?.getBundle("submitted"))
        assertEquals(envelope.getString("commandId"), restoredSubmission.getString("commandId"))
        assertEquals(envelope.getString("stateId"), restoredSubmission.getString("stateId"))
        assertEquals(listOf(target), fixture.repository.loadSubscriptionDetail(firstFacts.subscription.id)!!.stateChanges)
    }

    private suspend fun seeded(facts: SubscriptionFacts = M09SubscriptionFixtures.facts()): SubscriptionFacts {
        fixture.seed(facts); return facts
    }
    private fun expiredStopped() = M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED,
        stateOn = LocalDate.of(2026, 1, 2)).let { facts -> facts.copy(renewals = facts.renewals.map {
            it.copy(effectiveFrom = LocalDate.of(2025, 12, 1), paidUntilExclusive = LocalDate.of(2026, 1, 1))
        }) }
    private suspend fun start(model: SubscriptionViewModel, action: () -> Unit) {
        fixture.onMain(action)
        assertNotNull("The actual requested operation must synchronously create the only active form",
            model.state.value.stateEditor ?: model.state.value.editor)
        await(model) { it.stateEditor?.let { value -> !value.loading && !value.needsInitialRead } == true || it.editor?.canEdit == true }
    }
    private suspend fun committed(model: SubscriptionViewModel, id: String) = await(model) { it.committedSubscriptionId == id }
    private suspend fun await(model: SubscriptionViewModel, predicate: (SubscriptionUiState) -> Boolean) {
        withTimeout(10_000) { model.state.first(predicate) }
    }
    private fun restoredHandle(handle: SavedStateHandle) = SavedStateHandle(handle.keys().associateWith { key ->
        val value = handle.get<Any?>(key); if (value is Bundle) Bundle(value) else value
    })
}
