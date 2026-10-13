package com.cooper.lithic.data.repository

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.DatabaseMigrations
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.Collections
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Real Room/SQLite boundaries: no fake DAO, cost engine, operation log, or fixture-only store. */
@RunWith(AndroidJUnit4::class)
class SubscriptionLifecycleRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var file: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private var today = date("2026-01-25")
    private val instant = Instant.parse("2026-01-01T03:04:05.123456789Z")
    private val repository get() = SubscriptionRepository(db, coordinator, BusinessClock { today })

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m09-repository-test-${UUID.randomUUID()}").apply { check(mkdir()) }
        file = File(directory, "data.db")
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, file) }
        db.initializeDefaults()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(directory.deleteRecursively())
    }

    // Missing PAUSED persistence or accidental payment mutation must fail this test.
    @Test fun pause_saves_exact_state_identity_without_changing_real_payments() = runBlocking {
        val before = seed()
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        val result = saved(repository.setState(set(before, target, SubscriptionStateAction.PAUSE)))
        assertEquals(before.subscription.id, result.subscriptionId)
        assertEquals(target.id, result.stateId)
        assertNull(result.renewalId)
        assertFalse(result.replayed)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
        assertEquals(listOf(target), facts(before.subscription.id).stateChanges)
        val detail = detail(before.subscription.id)
        assertEquals(SubscriptionDisplayStatus.PAUSED, detail.cost.displayStatus)
        assertEquals(15000L, detail.cost.confirmedPaidCents)
        assertEquals(30L, detail.cost.standardPurchasedDays)
        assertEquals(24L, detail.cost.elapsedPaidDays)
        assertEquals(0, BigDecimal.ZERO.compareTo(detail.cost.currentRate))
    }

    @Test fun same_day_resume_corrects_original_id_and_operation_instead_of_appending() = runBlocking {
        val original = seedWithState(SubscriptionOperationState.PAUSED, today)
        val old = original.stateChanges.single()
        val target = old.copy(state = SubscriptionOperationState.ENABLED, reason = "误暂停")
        val result = saved(repository.setState(SetSubscriptionStateCommand(id(8001), original, target, SubscriptionStateAction.RESUME)))
        assertEquals(old.id, result.stateId)
        assertEquals(listOf(target), facts(original.subscription.id).stateChanges)
        assertEquals(old.operationId, facts(original.subscription.id).stateChanges.single().operationId)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(original.subscription.id).cost.displayStatus)
        assertEquals(25L, detail(original.subscription.id).cost.elapsedPaidDays)
        assertEquals(original.renewals, facts(original.subscription.id).renewals)
    }

    @Test fun same_day_replacement_with_new_row_or_operation_identity_conflicts() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.PAUSED, today)
        val wrong = state(9, before.subscription.id, today, SubscriptionOperationState.ENABLED)
        assertConflict(repository.setState(set(before, wrong, SubscriptionStateAction.RESUME)))
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun exact_saved_state_can_replay_after_database_reopen_without_new_fact() = runBlocking {
        val before = seed()
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        val command = set(before, target, SubscriptionStateAction.PAUSE)
        saved(repository.setState(command))
        reopen()
        val replay = saved(repository.setState(command))
        assertTrue(replay.replayed)
        assertEquals(target.id, replay.stateId)
        assertEquals(listOf(target), facts(before.subscription.id).stateChanges)
    }

    @Test fun reused_state_operation_with_different_contents_conflicts() = runBlocking {
        val before = seed()
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        val command = set(before, target, SubscriptionStateAction.PAUSE)
        saved(repository.setState(command))
        assertConflict(repository.setState(command.copy(target = target.copy(reason = "另一内容"))))
        assertEquals(listOf(target), facts(before.subscription.id).stateChanges)
    }

    @Test fun concurrent_identical_pause_commands_save_only_one_fact() = runBlocking {
        val before = seed()
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        val command = set(before, target, SubscriptionStateAction.PAUSE)
        val results = (1..8).map { async(Dispatchers.IO) { repository.setState(command) } }.awaitAll()
        assertTrue("All identical calls must acknowledge the exact saved fact: $results", results.all { it is SubscriptionLifecycleResult.Saved })
        assertEquals(1, results.count { !(it as SubscriptionLifecycleResult.Saved).replayed })
        assertEquals(listOf(target), facts(before.subscription.id).stateChanges)
    }

    @Test fun changed_payment_invalidates_whole_state_baseline() = runBlocking {
        val before = seed()
        val next = renewal(2, before.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01")
        db.subscriptionDao().insertRenewal(next)
        val current = facts(before.subscription.id)
        assertConflict(repository.setState(set(before, state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED), SubscriptionStateAction.PAUSE)))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun changed_subscription_identity_invalidates_whole_state_baseline() = runBlocking {
        val before = seed()
        sql("UPDATE subscriptions SET name='后来名称' WHERE id=?", before.subscription.id)
        val current = facts(before.subscription.id)
        assertConflict(repository.setState(set(before, state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED), SubscriptionStateAction.PAUSE)))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun later_state_fact_invalidates_whole_state_baseline() = runBlocking {
        val before = seed()
        db.subscriptionDao().insertStateChange(state(2, before.subscription.id, date("2026-01-24"), SubscriptionOperationState.PAUSED))
        val current = facts(before.subscription.id)
        assertConflict(repository.setState(set(before, state(1, before.subscription.id, today, SubscriptionOperationState.ENABLED), SubscriptionStateAction.RESUME)))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun complete_baseline_comparison_treats_payment_and_state_order_as_sets() = runBlocking {
        var before = seed(prepaid = true)
        db.subscriptionDao().insertStateChange(state(2, before.subscription.id, date("2026-01-10"), SubscriptionOperationState.PAUSED))
        db.subscriptionDao().insertStateChange(state(3, before.subscription.id, date("2026-01-15"), SubscriptionOperationState.ENABLED))
        before = facts(before.subscription.id)
        val shuffled = before.copy(renewals = before.renewals.reversed(), stateChanges = before.stateChanges.reversed())
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        saved(repository.setState(set(shuffled, target, SubscriptionStateAction.PAUSE)))
        assertEquals(3, facts(before.subscription.id).stateChanges.size)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
    }

    @Test fun default_stop_is_fixed_at_latest_prepaid_boundary_and_keeps_history() = runBlocking {
        val before = seed(prepaid = true)
        val target = state(1, before.subscription.id, date("2026-03-01"), SubscriptionOperationState.STOPPED)
        saved(repository.setState(set(before, target, SubscriptionStateAction.STOP_AT_END)))
        assertEquals(listOf(target), facts(before.subscription.id).stateChanges)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(before.subscription.id, date("2026-02-28")).cost.displayStatus)
        val stopped = detail(before.subscription.id, date("2026-03-01"))
        assertEquals(SubscriptionDisplayStatus.STOPPED, stopped.cost.displayStatus)
        assertEquals(33000L, stopped.cost.confirmedPaidCents)
        assertEquals(60L, stopped.cost.standardPurchasedDays)
        assertEquals(59L, stopped.cost.elapsedPaidDays)
        assertNull(stopped.currentRenewal)
        assertNull(stopped.cost.currentPrice)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(before.subscription.id, date("2026-01-25")).cost.displayStatus)
    }

    @Test fun fixed_default_stop_command_replays_across_days_without_moving_saved_boundary() = runBlocking {
        val before = seed(prepaid = true)
        val target = state(1, before.subscription.id, date("2026-03-01"), SubscriptionOperationState.STOPPED)
        val command = set(before, target, SubscriptionStateAction.STOP_AT_END)
        val first = saved(repository.setState(command))
        assertFalse(first.replayed)
        val committed = facts(before.subscription.id)
        today = date("2026-03-05")
        val replay = saved(repository.setState(command))
        assertTrue(replay.replayed)
        assertEquals(before.subscription.id, replay.subscriptionId)
        assertEquals(target.id, replay.stateId)
        assertNull(replay.renewalId)
        assertEquals(committed, facts(before.subscription.id))
        assertEquals(date("2026-03-01"), facts(before.subscription.id).stateChanges.single().effectiveOn)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id).cost.displayStatus)
    }

    @Test fun default_stop_rejects_an_earlier_period_boundary_instead_of_silently_truncating() = runBlocking {
        val before = seed(prepaid = true)
        val wrong = state(1, before.subscription.id, date("2026-02-01"), SubscriptionOperationState.STOPPED)
        assertValidation(repository.setState(set(before, wrong, SubscriptionStateAction.STOP_AT_END)))
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun new_stop_identity_conflicts_with_existing_future_plan_without_moving_its_boundary() = runBlocking {
        var before = seed(prepaid = true)
        val existing = state(1, before.subscription.id, date("2026-01-28"), SubscriptionOperationState.STOPPED)
        db.subscriptionDao().insertStateChange(existing)
        before = facts(before.subscription.id)
        val later = state(2, before.subscription.id, date("2026-03-01"), SubscriptionOperationState.STOPPED)
        val result = repository.setState(set(before, later, SubscriptionStateAction.STOP_AT_END))
        assertConflict(result)
        assertEquals(before, facts(before.subscription.id))
        assertEquals(listOf(existing), facts(before.subscription.id).stateChanges)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id, date("2026-01-28")).cost.displayStatus)
    }

    @Test fun paid_coverage_gap_still_uses_latest_confirmed_end_for_default_stop() = runBlocking {
        val before = seed(prepaid = true, gap = true)
        val target = state(1, before.subscription.id, date("2026-04-01"), SubscriptionOperationState.STOPPED)
        saved(repository.setState(set(before, target, SubscriptionStateAction.STOP_AT_END)))
        assertEquals(SubscriptionDisplayStatus.EXPIRED, detail(before.subscription.id, date("2026-02-15")).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(before.subscription.id, date("2026-03-10")).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id, date("2026-04-01")).cost.displayStatus)
        assertEquals(date("2026-04-01"), facts(before.subscription.id).stateChanges.single().effectiveOn)
    }

    @Test fun all_expired_payments_default_to_business_today_without_backdating() = runBlocking {
        today = date("2026-03-10")
        val before = seed()
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.STOPPED)
        saved(repository.setState(set(before, target, SubscriptionStateAction.STOP_AT_END)))
        assertEquals(SubscriptionDisplayStatus.EXPIRED, detail(before.subscription.id, date("2026-03-09")).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id).cost.displayStatus)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
    }

    @Test fun early_stop_rejects_past_dates_and_preserves_purchased_totals_when_valid() = runBlocking {
        val before = seed(prepaid = true)
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.STOPPED)
        assertValidation(repository.setState(set(before, target.copy(effectiveOn = today.minusDays(1)), SubscriptionStateAction.STOP_EARLY)))
        assertEquals(before, facts(before.subscription.id))
        saved(repository.setState(set(before, target, SubscriptionStateAction.STOP_EARLY)))
        val stopped = detail(before.subscription.id)
        assertEquals(33000L, stopped.cost.confirmedPaidCents)
        assertEquals(60L, stopped.cost.standardPurchasedDays)
        assertEquals(24L, stopped.cost.elapsedPaidDays)
        assertEquals(SubscriptionDisplayStatus.STOPPED, stopped.cost.displayStatus)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
    }

    @Test fun resume_with_current_coverage_keeps_old_payment_and_counts_only_enabled_dates() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.PAUSED, date("2026-01-10"))
        val target = state(2, before.subscription.id, today, SubscriptionOperationState.ENABLED)
        saved(repository.setState(set(before, target, SubscriptionStateAction.RESUME)))
        val active = detail(before.subscription.id)
        assertEquals(SubscriptionDisplayStatus.ACTIVE, active.cost.displayStatus)
        assertEquals(10L, active.cost.elapsedPaidDays)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
        assertEquals(SubscriptionDisplayStatus.PAUSED, detail(before.subscription.id, date("2026-01-24")).cost.displayStatus)
    }

    @Test fun reopen_with_only_future_paid_coverage_is_expired_until_real_start() = runBlocking {
        today = date("2026-02-15")
        val before = seed(prepaid = true, gap = true)
        db.subscriptionDao().insertStateChange(state(1, before.subscription.id, date("2026-02-01"), SubscriptionOperationState.STOPPED))
        val stopped = facts(before.subscription.id)
        val target = state(2, before.subscription.id, today, SubscriptionOperationState.ENABLED)
        saved(repository.setState(set(stopped, target, SubscriptionStateAction.REOPEN)))
        val waiting = detail(before.subscription.id)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, waiting.cost.displayStatus)
        assertNull(waiting.cost.currentPrice)
        assertEquals(0, BigDecimal.ZERO.compareTo(waiting.cost.currentRate))
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(before.subscription.id, date("2026-03-01")).cost.displayStatus)
        assertEquals(stopped.renewals, facts(before.subscription.id).renewals)
    }

    @Test fun reopen_without_current_or_future_payment_requires_real_confirmation() = runBlocking {
        today = date("2026-03-10")
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"))
        val target = state(2, before.subscription.id, today, SubscriptionOperationState.ENABLED)
        assertValidation(repository.setState(set(before, target, SubscriptionStateAction.REOPEN)))
        assertEquals(before, facts(before.subscription.id))
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id).cost.displayStatus)
    }

    @Test fun atomic_reopen_with_real_renewal_keeps_subscription_id_and_supports_exact_replay() = runBlocking {
        today = date("2026-03-10")
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"))
        val payment = renewal(2, before.subscription.id, "2026-03-10", "2026-03-10", "2026-04-10")
        val target = state(2, before.subscription.id, today, SubscriptionOperationState.ENABLED).copy(operationId = payment.operationId)
        val command = ReenableSubscriptionWithRenewalCommand(payment.operationId, before, payment, target)
        val result = saved(repository.reenableWithRenewal(command))
        assertEquals(before.subscription.id, result.subscriptionId)
        assertEquals(payment.id, result.renewalId)
        assertEquals(target.id, result.stateId)
        assertEquals(1L, db.subscriptionDao().countSubscriptions())
        assertEquals(2L, db.subscriptionDao().countRenewals(before.subscription.id))
        assertEquals(SubscriptionDisplayStatus.ACTIVE, detail(before.subscription.id).cost.displayStatus)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id, date("2026-03-09")).cost.displayStatus)
        val replay = saved(repository.reenableWithRenewal(command))
        assertTrue(replay.replayed)
        assertEquals(2L, db.subscriptionDao().countRenewals(before.subscription.id))
    }

    @Test fun real_sqlite_state_failure_rolls_back_atomic_reopen_payment_and_state() = runBlocking {
        today = date("2026-03-10")
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"))
        val payment = renewal(2, before.subscription.id, "2026-03-10", "2026-03-10", "2026-04-10")
        val target = state(2, before.subscription.id, today, SubscriptionOperationState.ENABLED).copy(operationId = payment.operationId)
        sql("CREATE TRIGGER m09_reject_reopen_state BEFORE INSERT ON subscription_state_changes BEGIN SELECT RAISE(ABORT,'synthetic state failure'); END")
        assertStorageOrConflict(repository.reenableWithRenewal(ReenableSubscriptionWithRenewalCommand(payment.operationId, before, payment, target)))
        assertEquals(before, facts(before.subscription.id))
        assertNull(db.subscriptionDao().findRenewal(payment.id))
    }

    @Test fun ordinary_new_renewal_cannot_cross_fixed_future_stop_boundary() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"))
        val payment = renewal(2, before.subscription.id, "2026-01-25", "2026-02-01", "2026-03-01")
        val result = repository.confirmRenewal(ConfirmRenewalCommand(payment.operationId, payment))
        assertTrue("A new coverage after a fixed plan requires explicit plan correction: $result", result is SubscriptionMutationResult.Conflict || result is SubscriptionMutationResult.ValidationFailure)
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun ordinary_new_renewal_on_stopped_identity_does_not_implicitly_enable_it() = runBlocking {
        today = date("2026-03-10")
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"))
        val payment = renewal(2, before.subscription.id, "2026-03-10", "2026-03-10", "2026-04-10")
        val result = repository.confirmRenewal(ConfirmRenewalCommand(payment.operationId, payment))
        assertTrue("Stopped service must use explicit atomic reopening: $result", result is SubscriptionMutationResult.Conflict || result is SubscriptionMutationResult.ValidationFailure)
        assertEquals(before, facts(before.subscription.id))
    }

    // Normal boundary regression; the frozen M03 engine is expected to be correct initially.
    @Test fun stopped_reopened_and_stopped_again_retains_same_identity_and_historical_days() = runBlocking {
        today = date("2026-01-10")
        val original = seed()
        val firstStop = state(1, original.subscription.id, today, SubscriptionOperationState.STOPPED)
        saved(repository.setState(set(original, firstStop, SubscriptionStateAction.STOP_EARLY)))
        today = date("2026-01-20")
        val reopened = state(2, original.subscription.id, today, SubscriptionOperationState.ENABLED)
        saved(repository.setState(set(facts(original.subscription.id), reopened, SubscriptionStateAction.REOPEN)))
        today = date("2026-01-25")
        val secondStop = state(3, original.subscription.id, today, SubscriptionOperationState.STOPPED)
        saved(repository.setState(set(facts(original.subscription.id), secondStop, SubscriptionStateAction.STOP_EARLY)))
        val retained = facts(original.subscription.id)
        assertEquals(original.subscription, retained.subscription)
        assertEquals(original.renewals, retained.renewals)
        assertEquals(listOf(firstStop, reopened, secondStop), retained.stateChanges)
        assertEquals(1L, db.subscriptionDao().countSubscriptions())
        for ((asOf, status, elapsed) in listOf(
            Triple(date("2026-01-09"), SubscriptionDisplayStatus.ACTIVE, 9L),
            Triple(date("2026-01-15"), SubscriptionDisplayStatus.STOPPED, 9L),
            Triple(date("2026-01-20"), SubscriptionDisplayStatus.ACTIVE, 10L),
            Triple(date("2026-01-24"), SubscriptionDisplayStatus.ACTIVE, 14L),
            Triple(date("2026-01-25"), SubscriptionDisplayStatus.STOPPED, 14L),
        )) {
            val observed = detail(original.subscription.id, asOf)
            assertEquals(status, observed.cost.displayStatus)
            assertEquals(elapsed, observed.cost.elapsedPaidDays)
            assertEquals(15000L, observed.cost.confirmedPaidCents)
            assertEquals(30L, observed.cost.standardPurchasedDays)
            if (status == SubscriptionDisplayStatus.STOPPED) {
                assertNull(observed.cost.currentPrice)
                assertEquals(0, BigDecimal.ZERO.compareTo(observed.cost.currentRate))
            } else assertEquals(15000L, observed.cost.currentPrice!!.cents)
        }
    }

    @Test fun renewal_amount_correction_updates_original_fact_and_exact_target_replays() = runBlocking {
        val before = seed(prepaid = true)
        val old = before.renewals.single { it.effectiveFrom == date("2026-01-01") }
        val target = old.copy(amountCents = 16000)
        val command = UpdateSubscriptionRenewalCommand(id(8001), before, old, target)
        val result = saved(repository.updateRenewal(command))
        assertEquals(old.id, result.renewalId)
        assertNull(result.stateId)
        assertEquals(target, db.subscriptionDao().findRenewal(old.id))
        assertEquals(old.operationId, db.subscriptionDao().findRenewal(old.id)!!.operationId)
        assertEquals(2L, db.subscriptionDao().countRenewals(before.subscription.id))
        assertEquals(34000L, detail(before.subscription.id).cost.confirmedPaidCents)
        val replay = saved(repository.updateRenewal(command))
        assertTrue(replay.replayed)
        assertEquals(old.id, replay.renewalId)
    }

    @Test fun renewal_overlap_correction_is_rejected_before_any_partial_update() = runBlocking {
        val before = seed(prepaid = true)
        val old = before.renewals.single { it.effectiveFrom == date("2026-01-01") }
        val target = old.copy(amountCents = 16000, paidUntilExclusive = date("2026-02-02"))
        assertValidation(repository.updateRenewal(UpdateSubscriptionRenewalCommand(id(8001), before, old, target)))
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun renewal_correction_validates_positive_amount_cycle_dates_and_preserved_identities() = runBlocking {
        val before = seed()
        val old = before.renewals.single()
        val invalid = listOf(
            old.copy(amountCents = 0),
            old.copy(amountCents = -1),
            old.copy(standardDays = 90),
            old.copy(paidOn = today.plusDays(1)),
            old.copy(paidUntilExclusive = old.effectiveFrom),
            old.copy(id = id(9991)),
            old.copy(operationId = id(9992)),
            old.copy(subscriptionId = id(9993)),
        )
        for ((index, target) in invalid.withIndex()) {
            val result = repository.updateRenewal(UpdateSubscriptionRenewalCommand(id(8100 + index), before, old, target))
            assertTrue("Invalid payment correction must not be saved: $result", result is SubscriptionLifecycleResult.ValidationFailure || result is SubscriptionLifecycleResult.Conflict)
            assertEquals(before, facts(before.subscription.id))
        }
    }

    @Test fun stale_payment_correction_cannot_overwrite_later_state_or_payment_fact() = runBlocking {
        val before = seed()
        val old = before.renewals.single()
        sql("UPDATE subscription_renewals SET amountCents=17000 WHERE id=?", old.id)
        db.subscriptionDao().insertStateChange(state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED))
        val current = facts(before.subscription.id)
        assertConflict(repository.updateRenewal(UpdateSubscriptionRenewalCommand(id(8001), before, old, old.copy(amountCents = 16000))))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun amount_only_correction_keeps_existing_coverage_truncated_by_early_plan() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-28"))
        val old = before.renewals.single()
        val target = old.copy(amountCents = 17000, reminderDueOn = date("2026-02-05"))
        saved(repository.updateRenewal(UpdateSubscriptionRenewalCommand(id(8001), before, old, target)))
        assertEquals(target, db.subscriptionDao().findRenewal(old.id))
        assertEquals(before.stateChanges, facts(before.subscription.id).stateChanges)
        assertEquals(SubscriptionDisplayStatus.STOPPED, detail(before.subscription.id, date("2026-01-28")).cost.displayStatus)
    }

    @Test fun correction_expanding_coverage_after_fixed_stop_requires_explicit_plan_change() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-28"))
        val old = before.renewals.single()
        val target = old.copy(paidUntilExclusive = date("2026-02-02"))
        val result = repository.updateRenewal(UpdateSubscriptionRenewalCommand(id(8001), before, old, target))
        assertTrue("Expanded coverage must not silently override the plan: $result", result is SubscriptionLifecycleResult.Conflict || result is SubscriptionLifecycleResult.ValidationFailure)
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun scheduled_stop_correction_preserves_original_id_operation_and_other_history() = runBlocking {
        var before = seedWithState(SubscriptionOperationState.PAUSED, date("2026-01-10"))
        val stop = state(2, before.subscription.id, date("2026-02-01"), SubscriptionOperationState.STOPPED)
        db.subscriptionDao().insertStateChange(stop)
        before = facts(before.subscription.id)
        val target = stop.copy(effectiveOn = date("2026-01-30"), reason = "修正日期")
        val command = UpdateSubscriptionStateCommand(id(8001), before, stop, target)
        val result = saved(repository.updateState(command))
        assertEquals(stop.id, result.stateId)
        assertEquals(listOf(before.stateChanges.first(), target), facts(before.subscription.id).stateChanges)
        assertTrue(saved(repository.updateState(command)).replayed)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
    }

    @Test fun state_date_correction_into_existing_day_conflicts_without_changing_either_row() = runBlocking {
        var before = seedWithState(SubscriptionOperationState.PAUSED, date("2026-01-10"))
        val stop = state(2, before.subscription.id, date("2026-02-01"), SubscriptionOperationState.STOPPED)
        db.subscriptionDao().insertStateChange(stop)
        before = facts(before.subscription.id)
        val result = repository.updateState(UpdateSubscriptionStateCommand(id(8001), before, stop, stop.copy(effectiveOn = date("2026-01-10"))))
        assertTrue(result is SubscriptionLifecycleResult.Conflict || result is SubscriptionLifecycleResult.ValidationFailure)
        assertEquals(before, facts(before.subscription.id))
    }

    @Test fun cancel_future_stop_deletes_only_target_and_retry_reports_current_absence() = runBlocking {
        var before = seedWithState(SubscriptionOperationState.PAUSED, date("2026-01-10"))
        val stop = state(2, before.subscription.id, date("2026-02-01"), SubscriptionOperationState.STOPPED)
        db.subscriptionDao().insertStateChange(stop)
        before = facts(before.subscription.id)
        val command = CancelScheduledStopCommand(id(8001), before, stop)
        absence(repository.cancelScheduledStop(command), before.subscription.id, stop.id)
        assertEquals(listOf(before.stateChanges.first()), facts(before.subscription.id).stateChanges)
        assertEquals(before.renewals, facts(before.subscription.id).renewals)
        absence(repository.cancelScheduledStop(command), before.subscription.id, stop.id)
    }

    @Test fun cancel_stop_cannot_delete_already_effective_stop_or_stale_target() = runBlocking {
        val before = seedWithState(SubscriptionOperationState.STOPPED, today)
        val old = before.stateChanges.single()
        assertValidation(repository.cancelScheduledStop(CancelScheduledStopCommand(id(8001), before, old)))
        assertEquals(before, facts(before.subscription.id))
        sql("UPDATE subscription_state_changes SET effectiveOn=? WHERE id=?", today.plusDays(2).toEpochDay(), old.id)
        val current = facts(before.subscription.id)
        assertConflict(repository.cancelScheduledStop(CancelScheduledStopCommand(id(8002), before, old)))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun deleting_one_erroneous_payment_keeps_service_and_fixed_state_and_reports_absence() = runBlocking {
        var before = seed(prepaid = true)
        val stop = state(1, before.subscription.id, date("2026-03-01"), SubscriptionOperationState.STOPPED)
        db.subscriptionDao().insertStateChange(stop)
        before = facts(before.subscription.id)
        val wrong = before.renewals.single { it.effectiveFrom == date("2026-02-01") }
        val command = DeleteSubscriptionRenewalCommand(id(8001), before, wrong)
        absence(repository.deleteRenewal(command), before.subscription.id, wrong.id)
        assertNull(db.subscriptionDao().findRenewal(wrong.id))
        assertEquals(before.subscription, db.subscriptionDao().findSubscription(before.subscription.id))
        assertEquals(listOf(stop), facts(before.subscription.id).stateChanges)
        assertEquals(15000L, detail(before.subscription.id).cost.confirmedPaidCents)
        absence(repository.deleteRenewal(command), before.subscription.id, wrong.id)
    }

    @Test fun deleting_last_payment_is_rejected_without_deleting_subscription_identity() = runBlocking {
        val before = seed()
        assertValidation(repository.deleteRenewal(DeleteSubscriptionRenewalCommand(id(8001), before, before.renewals.single())))
        assertEquals(before, facts(before.subscription.id))
        assertEquals(1L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun stale_delete_cannot_remove_corrected_payment() = runBlocking {
        val before = seed(prepaid = true)
        val old = before.renewals.first()
        sql("UPDATE subscription_renewals SET amountCents=17000 WHERE id=?", old.id)
        val current = facts(before.subscription.id)
        assertConflict(repository.deleteRenewal(DeleteSubscriptionRenewalCommand(id(8001), before, old)))
        assertEquals(current, facts(before.subscription.id))
    }

    @Test fun real_sqlite_after_delete_failure_rolls_back_the_entire_delete() = runBlocking {
        val before = seed(prepaid = true)
        val wrong = before.renewals.first()
        sql("CREATE TRIGGER m09_reject_delete AFTER DELETE ON subscription_renewals BEGIN SELECT RAISE(ABORT,'synthetic delete failure'); END")
        assertStorageOrConflict(repository.deleteRenewal(DeleteSubscriptionRenewalCommand(id(8001), before, wrong)))
        assertEquals(before, facts(before.subscription.id))
        assertEquals(wrong, db.subscriptionDao().findRenewal(wrong.id))
    }

    @Test fun management_and_stopped_queries_use_observation_day_last_boundary() = runBlocking {
        val active = seed(index = 1)
        val paused = seedWithState(SubscriptionOperationState.PAUSED, date("2026-01-10"), index = 2)
        val future = seedWithState(SubscriptionOperationState.STOPPED, date("2026-02-01"), index = 3)
        val reopened = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-10"), index = 4)
        db.subscriptionDao().insertStateChange(state(99, reopened.subscription.id, date("2026-01-20"), SubscriptionOperationState.ENABLED))
        val stopped = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-10"), index = 5)
        val current = repository.loadManagementPage(stopped = false)
        assertEquals(setOf(active.subscription.id, paused.subscription.id, future.subscription.id, reopened.subscription.id), current.items.map { it.subscription.id }.toSet())
        assertEquals(4L, current.totalCount)
        val history = repository.loadManagementPage(stopped = true)
        assertEquals(listOf(stopped.subscription.id), history.items.map { it.subscription.id })
        assertEquals(1L, history.totalCount)
        val past = repository.loadManagementPage(stopped = true, asOf = date("2026-01-15"))
        assertEquals(setOf(reopened.subscription.id, stopped.subscription.id), past.items.map { it.subscription.id }.toSet())
        assertEquals(2L, past.totalCount)
        val after = repository.loadManagementPage(stopped = true, asOf = date("2026-02-01"))
        assertEquals(setOf(future.subscription.id, stopped.subscription.id), after.items.map { it.subscription.id }.toSet())
    }

    @Test fun management_sql_pagination_fills_filtered_pages_and_rebuilds_complete_window() = runBlocking {
        val stoppedIds = mutableListOf<String>()
        db.withTransaction {
            for (index in 1..124) {
                val create = create(index)
                db.subscriptionDao().insertSubscription(create.subscription)
                db.subscriptionDao().insertRenewal(create.initialRenewal)
                if (index % 2 == 0) {
                    stoppedIds += create.subscription.id
                    db.subscriptionDao().insertStateChange(state(index, create.subscription.id, date("2026-01-10"), SubscriptionOperationState.STOPPED))
                }
            }
        }
        val first = repository.loadManagementPage(stopped = true)
        assertEquals(50, first.items.size)
        assertEquals(62L, first.totalCount)
        assertEquals(stoppedIds.take(50), first.items.map { it.subscription.id })
        assertNotNull(first.nextCursor)
        val second = repository.loadManagementPage(stopped = true, cursor = first.nextCursor)
        assertEquals(stoppedIds.drop(50), second.items.map { it.subscription.id })
        assertNull(second.nextCursor)
        val window = repository.loadManagementWindow(stopped = true, pageCount = 2)
        assertEquals(stoppedIds, window.items.map { it.subscription.id })
        assertNull(window.nextCursor)
        assertEquals(62L, window.totalCount)
        assertEquals(50, repository.loadManagementPage(stopped = false).items.size)
    }

    @Test fun corrupt_facts_outside_visible_management_page_are_not_hidden_by_filter_or_limit() = runBlocking {
        val good = seed(index = 1)
        val bad = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-10"), index = 2)
        sql("UPDATE subscription_renewals SET amountCents=0 WHERE id=?", bad.renewals.single().id)
        assertReadFails { repository.loadManagementPage(stopped = false, pageSize = 1) }
        assertReadFails { repository.loadManagementWindow(stopped = false, pageCount = 1) }
        assertEquals(15000L, db.subscriptionDao().findRenewal(good.renewals.single().id)!!.amountCents)
        assertEquals(0L, db.subscriptionDao().findRenewal(bad.renewals.single().id)!!.amountCents)
    }

    @Test fun full_corruption_preflight_uses_bounded_identity_batches_and_finds_bad_tail() = runBlocking {
        db.withTransaction {
            for (index in 1..124) {
                val create = create(index)
                db.subscriptionDao().insertSubscription(create.subscription)
                db.subscriptionDao().insertRenewal(create.initialRenewal)
            }
        }
        val queries = Collections.synchronizedList(mutableListOf<Pair<String, List<Any?>>>())
        db.close()
        db = Room.databaseBuilder(context, LithicDatabase::class.java, file.path)
            .addMigrations(*DatabaseMigrations.ALL)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setQueryCallback(RoomDatabase.QueryCallback { query, arguments ->
                queries += query.replace(Regex("\\s+"), " ").trim().lowercase() to arguments.toList()
            }, Executor { task -> task.run() })
            .build()
        db.requireDefaults()
        queries.clear()
        val page = repository.loadManagementPage(stopped = false, pageSize = 1)
        assertEquals(124L, page.totalCount)
        assertEquals(1, page.items.size)
        val observed = synchronized(queries) { queries.toList() }
        val identityScans = observed.filter { (query, _) ->
            query.startsWith("select * from subscriptions") && !query.contains("where id in")
        }
        assertTrue("124 identities require at least three bounded preflight batches: $identityScans", identityScans.size >= 3)
        assertTrue("Every identity preflight must bound the SQL result to at most 50 identities: $identityScans", identityScans.all { (query, arguments) ->
            query.contains("limit ?") && arguments.lastOrNull() is Number && (arguments.last() as Number).toInt() in 1..50
        })
        val associationScans = observed.filter { (query, _) ->
            query.startsWith("select * from subscription_renewals") || query.startsWith("select * from subscription_state_changes")
        }
        assertTrue("Related facts must be queried for bounded identity batches, not all stored subscriptions: $associationScans", associationScans.all { (query, arguments) ->
            query.contains("subscriptionid in") && arguments.size in 1..50
        })
        val tailPayment = create(124).initialRenewal
        sql("UPDATE subscription_renewals SET amountCents=0 WHERE id=?", tailPayment.id)
        assertReadFails { repository.loadManagementPage(stopped = false, pageSize = 1) }
        assertReadFails { repository.loadManagementWindow(stopped = false, pageCount = 1) }
        assertEquals(0L, db.subscriptionDao().findRenewal(tailPayment.id)!!.amountCents)
        assertEquals(124L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun hidden_wrong_category_fails_both_management_reads_and_lifecycle_write_without_repair() = runBlocking {
        val good = seed(index = 1)
        val bad = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-10"), index = 2)
        sql("UPDATE subscriptions SET categoryId=? WHERE id=?", LithicDatabase.PROJECT_FALLBACK_ID, bad.subscription.id)
        assertReadFails { repository.loadManagementPage(stopped = false, pageSize = 1) }
        assertReadFails { repository.loadManagementPage(stopped = true, pageSize = 1) }
        val target = state(99, good.subscription.id, today, SubscriptionOperationState.PAUSED)
        val result = repository.setState(set(good, target, SubscriptionStateAction.PAUSE))
        assertTrue("Hidden wrong category must produce corruption, not a saved or validation result: $result", result is SubscriptionLifecycleResult.StorageFailure)
        assertEquals(StorageFailureKind.CORRUPT, (result as SubscriptionLifecycleResult.StorageFailure).kind)
        assertEquals(good, facts(good.subscription.id))
        val retainedBad = facts(bad.subscription.id)
        assertEquals(LithicDatabase.PROJECT_FALLBACK_ID, retainedBad.subscription.categoryId)
        assertEquals(bad.renewals, retainedBad.renewals)
        assertEquals(bad.stateChanges, retainedBad.stateChanges)
        assertEquals(2L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun hidden_illegal_state_is_explicit_read_and_write_corruption_without_overwriting_facts() = runBlocking {
        val good = seed(index = 1)
        val bad = seedWithState(SubscriptionOperationState.STOPPED, date("2026-01-10"), index = 2)
        val badState = bad.stateChanges.single()
        sql("UPDATE subscription_state_changes SET state='BROKEN_STATE' WHERE id=?", badState.id)
        assertReadFails { repository.loadManagementPage(stopped = false, pageSize = 1) }
        assertReadFails { repository.loadManagementPage(stopped = true, pageSize = 1) }
        val target = state(99, good.subscription.id, today, SubscriptionOperationState.PAUSED)
        val result = repository.setState(set(good, target, SubscriptionStateAction.PAUSE))
        assertTrue("Invalid persisted state cannot turn into EXPIRED, a fake empty page, or a successful write: $result", result is SubscriptionLifecycleResult.StorageFailure)
        assertEquals(StorageFailureKind.CORRUPT, (result as SubscriptionLifecycleResult.StorageFailure).kind)
        assertEquals(good, facts(good.subscription.id))
        assertEquals(bad.renewals, db.subscriptionDao().renewalsForSubscriptions(listOf(bad.subscription.id)))
        val raw = withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.query("SELECT state FROM subscription_state_changes WHERE id=?", arrayOf(badState.id)).use { cursor ->
                check(cursor.moveToFirst())
                cursor.getString(0)
            }
        }
        assertEquals("BROKEN_STATE", raw)
        assertEquals(2L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun renewal_history_checks_full_timeline_not_only_visible_payment_row() = runBlocking {
        val before = seed(prepaid = true)
        val old = before.renewals.single { it.effectiveFrom == date("2026-01-01") }
        sql("UPDATE subscription_renewals SET amountCents=0 WHERE id=?", old.id)
        assertReadFails { repository.loadRenewalHistory(before.subscription.id, pageSize = 1) }
        assertReadFails { repository.loadRenewalHistoryWindow(before.subscription.id, pageCount = 1) }
        assertEquals(2L, db.subscriptionDao().countRenewals(before.subscription.id))
    }

    @Test fun closed_database_management_read_and_lifecycle_write_have_explicit_failures() = runBlocking {
        val before = seed()
        val owned = repository
        db.close()
        assertReadFails { owned.loadManagementPage(stopped = false) }
        assertReadFails { owned.loadManagementWindow(stopped = true, pageCount = 1) }
        val target = state(1, before.subscription.id, today, SubscriptionOperationState.PAUSED)
        val result = owned.setState(set(before, target, SubscriptionStateAction.PAUSE))
        assertTrue("Unavailable storage cannot be confused with validation or saved facts: $result", result is SubscriptionLifecycleResult.StorageFailure)
        assertEquals(StorageFailureKind.DATABASE, (result as SubscriptionLifecycleResult.StorageFailure).kind)
        reopen()
        assertEquals(before, facts(before.subscription.id))
    }

    private suspend fun seed(index: Int = 1, prepaid: Boolean = false, gap: Boolean = false): SubscriptionFacts {
        val command = create(index)
        val result = repository.createWithRenewal(command)
        assertTrue("Existing M08 valid creation must still work: $result", result is SubscriptionMutationResult.Success)
        if (prepaid) {
            val next = renewal(1000 + index, command.subscription.id, "2026-01-20", if (gap) "2026-03-01" else "2026-02-01", if (gap) "2026-04-01" else "2026-03-01", cents = 18000)
            val confirmed = repository.confirmRenewal(ConfirmRenewalCommand(next.operationId, next))
            assertTrue("Existing M08 valid prepayment must still work: $confirmed", confirmed is SubscriptionMutationResult.Success)
        }
        return facts(command.subscription.id)
    }

    private suspend fun seedWithState(value: SubscriptionOperationState, on: LocalDate, index: Int = 1): SubscriptionFacts {
        val seeded = seed(index)
        db.subscriptionDao().insertStateChange(state(index, seeded.subscription.id, on, value))
        return facts(seeded.subscription.id)
    }

    private fun create(index: Int): CreateSubscriptionCommand {
        val identity = SubscriptionEntity(id(1000 + index), "合成服务$index", LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = instant, updatedAt = instant)
        val initial = renewal(index, identity.id, "2026-01-01", "2026-01-01", "2026-02-01")
        return CreateSubscriptionCommand(initial.operationId, identity, initial)
    }

    private fun renewal(index: Int, subscriptionId: String, paid: String, from: String, until: String, cents: Long = 15000): SubscriptionRenewalEntity =
        SubscriptionRenewalEntity(id(2000 + index), subscriptionId, date(paid), date(from), date(until), date(until).plusDays(2), cents, BillingCycle.MONTHLY, 30, id(4000 + index))

    private fun state(index: Int, subscriptionId: String, on: LocalDate, value: SubscriptionOperationState) =
        SubscriptionStateChangeEntity(id(6000 + index), subscriptionId, on, value, reason = "用户确认", note = "合成事实", operationId = id(7000 + index))

    private fun set(expected: SubscriptionFacts, target: SubscriptionStateChangeEntity, action: SubscriptionStateAction) =
        SetSubscriptionStateCommand(target.operationId, expected, target, action)

    private suspend fun facts(id: String) = SubscriptionFacts(
        checkNotNull(db.subscriptionDao().findSubscription(id)),
        db.subscriptionDao().renewalsForSubscriptions(listOf(id)),
        db.subscriptionDao().statesForSubscriptions(listOf(id)),
    )

    private suspend fun detail(id: String, asOf: LocalDate = today) = checkNotNull(repository.loadSubscriptionDetail(id, asOf))
    private fun date(value: String): LocalDate = LocalDate.parse(value)
    private fun id(value: Int): String = "9b000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
    private fun saved(result: SubscriptionLifecycleResult): SubscriptionLifecycleResult.Saved {
        assertTrue("Valid lifecycle command must save exact identities: $result", result is SubscriptionLifecycleResult.Saved)
        return result as SubscriptionLifecycleResult.Saved
    }
    private fun absence(result: SubscriptionLifecycleResult, subscriptionId: String, factId: String) {
        assertTrue("Deletion acknowledges only present absence, not a permanent operation proof: $result", result is SubscriptionLifecycleResult.CurrentAbsence)
        result as SubscriptionLifecycleResult.CurrentAbsence
        assertEquals(subscriptionId, result.subscriptionId)
        assertEquals(factId, result.factId)
    }
    private fun assertConflict(result: SubscriptionLifecycleResult) { assertTrue("Expected explicit conflict: $result", result is SubscriptionLifecycleResult.Conflict) }
    private fun assertValidation(result: SubscriptionLifecycleResult) { assertTrue("Expected invalid candidate rejection: $result", result is SubscriptionLifecycleResult.ValidationFailure) }
    private fun assertStorageOrConflict(result: SubscriptionLifecycleResult) {
        assertTrue("Real SQLite failure must be reported before any success: $result", result is SubscriptionLifecycleResult.StorageFailure || result is SubscriptionLifecycleResult.Conflict)
    }
    private suspend fun assertReadFails(block: suspend () -> Unit) {
        val failure = runCatching { block() }.exceptionOrNull()
        assertNotNull("Invalid or unavailable facts must not be represented by an empty successful page", failure)
    }
    private suspend fun sql(statement: String, vararg arguments: Any) = withContext(Dispatchers.IO) {
        if (arguments.isEmpty()) db.openHelper.writableDatabase.execSQL(statement)
        else db.openHelper.writableDatabase.execSQL(statement, arguments)
    }
    private suspend fun reopen() {
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, file, requireExisting = true) }
        db.requireDefaults()
    }
}
