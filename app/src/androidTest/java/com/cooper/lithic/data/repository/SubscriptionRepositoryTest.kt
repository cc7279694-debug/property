package com.cooper.lithic.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriptionRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var directory: File
    private lateinit var file: File
    private lateinit var db: LithicDatabase
    private val coordinator = LocalWriteCoordinator()
    private val today = LocalDate.of(2026, 10, 12)
    private val instant = Instant.parse("2026-01-01T03:04:05.123456789Z")
    private val repository get() = SubscriptionRepository(db, coordinator, FixedBusinessClock(today))

    @Before fun setUp() = runBlocking {
        directory = File(context.noBackupFilesDir, "m08-repository-test-${UUID.randomUUID()}").apply { check(mkdir()) }
        file = File(directory, "data.db")
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, file) }
        db.initializeDefaults()
    }

    @After fun tearDown() {
        if (::db.isInitialized) db.close()
        check(directory.canonicalFile.parentFile == context.noBackupFilesDir.canonicalFile)
        check(directory.deleteRecursively())
    }

    // M08-T02: one logical confirmation must create exactly one identity and real payment.
    @Test fun create_and_first_payment_atomic() = runBlocking {
        val command = create(1)
        val results = (1..8).map { async(Dispatchers.IO) { repository.createWithRenewal(command) } }.awaitAll()
        assertTrue(results.all { it is SubscriptionMutationResult.Success })
        assertEquals(1, results.count { !(it as SubscriptionMutationResult.Success).replayed })
        assertEquals(command.subscription, db.subscriptionDao().findSubscription(command.subscription.id))
        assertEquals(listOf(command.initialRenewal), db.subscriptionDao().renewalsForSubscriptions(listOf(command.subscription.id)))
        assertEquals(1L, db.subscriptionDao().countSubscriptions())
        assertTrue(db.subscriptionDao().statesForSubscriptions(listOf(command.subscription.id)).isEmpty())
    }

    @Test fun first_payment_insert_failure_rolls_back_new_identity() = runBlocking {
        val command = create(1)
        withContext(Dispatchers.IO) {
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER synthetic_reject_first_payment BEFORE INSERT ON subscription_renewals BEGIN SELECT RAISE(ABORT,'synthetic payment failure'); END")
        }
        val result = repository.createWithRenewal(command)
        assertTrue(result is SubscriptionMutationResult.Conflict)
        assertNull(db.subscriptionDao().findSubscription(command.subscription.id))
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
        assertNull(db.subscriptionDao().findRenewal(command.initialRenewal.id))
        assertEquals(2, db.categoryDao().list().size)
    }

    @Test fun unknown_result_reopen_replays_same_ids_without_duplicate_payment() = runBlocking {
        val command = create(1)
        saved(repository.createWithRenewal(command))
        reopen()
        val result = saved(repository.createWithRenewal(command))
        assertTrue(result.replayed)
        assertEquals(command.subscription.id, result.subscriptionId)
        assertEquals(command.initialRenewal.id, result.renewalId)
        assertEquals(1L, db.subscriptionDao().countRenewals(command.subscription.id))
    }

    @Test fun reused_operation_with_different_content_conflicts() = runBlocking {
        val command = create(1)
        saved(repository.createWithRenewal(command))
        assertTrue(repository.createWithRenewal(command.copy(initialRenewal = command.initialRenewal.copy(amountCents = 15001))) is SubscriptionMutationResult.Conflict)
        assertEquals(command.initialRenewal, db.subscriptionDao().findRenewal(command.initialRenewal.id))
        assertEquals(1L, db.subscriptionDao().countRenewals(command.subscription.id))
    }

    @Test fun reused_renewal_primary_key_does_not_leave_second_identity() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val second = create(2).let { it.copy(initialRenewal = it.initialRenewal.copy(id = first.initialRenewal.id)) }
        assertTrue(repository.createWithRenewal(second) is SubscriptionMutationResult.Conflict)
        assertNull(db.subscriptionDao().findSubscription(second.subscription.id))
        assertEquals(first.subscription, db.subscriptionDao().findSubscription(first.subscription.id))
        assertEquals(first.initialRenewal, db.subscriptionDao().findRenewal(first.initialRenewal.id))
    }

    @Test fun project_category_cannot_be_used_for_subscription() = runBlocking {
        val command = create(1).let { it.copy(subscription = it.subscription.copy(categoryId = LithicDatabase.PROJECT_FALLBACK_ID)) }
        assertTrue(repository.createWithRenewal(command) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun positive_bounded_amount_and_fixed_cycle_days_are_required() = runBlocking {
        listOf(0L, -1L, 1_000_000_000_000L).forEachIndexed { index, cents ->
            val command = create(index + 1).let { it.copy(initialRenewal = it.initialRenewal.copy(amountCents = cents)) }
            assertTrue(repository.createWithRenewal(command) is SubscriptionMutationResult.ValidationFailure)
        }
        val wrongDays = create(8).let { it.copy(initialRenewal = it.initialRenewal.copy(standardDays = 31)) }
        assertTrue(repository.createWithRenewal(wrongDays) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun payment_after_fixed_business_today_is_rejected() = runBlocking {
        val command = create(1).let { it.copy(initialRenewal = it.initialRenewal.copy(paidOn = today.plusDays(1))) }
        assertTrue(repository.createWithRenewal(command) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun noncanonical_identity_and_operation_mismatch_are_rejected() = runBlocking {
        val command = create(1)
        assertTrue(repository.createWithRenewal(command.copy(commandId = command.commandId.uppercase())) is SubscriptionMutationResult.ValidationFailure)
        assertTrue(repository.createWithRenewal(command.copy(commandId = id(999))) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun confirm_requires_existing_subscription() = runBlocking {
        val first = create(1)
        assertTrue(repository.confirmRenewal(ConfirmRenewalCommand(first.commandId, first.initialRenewal)) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun adjacent_and_gapped_paid_coverage_are_allowed_but_overlap_is_rejected() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val adjacent = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01")
        saved(repository.confirmRenewal(ConfirmRenewalCommand(adjacent.operationId, adjacent)))
        val gap = renewal(3, first.subscription.id, "2026-03-20", "2026-04-01", "2026-05-01")
        saved(repository.confirmRenewal(ConfirmRenewalCommand(gap.operationId, gap)))
        val overlap = renewal(4, first.subscription.id, "2026-01-20", "2026-02-15", "2026-03-15")
        assertTrue(repository.confirmRenewal(ConfirmRenewalCommand(overlap.operationId, overlap)) is SubscriptionMutationResult.ValidationFailure)
        assertEquals(3L, db.subscriptionDao().countRenewals(first.subscription.id))
        assertEquals(first.subscription, db.subscriptionDao().findSubscription(first.subscription.id))
    }

    @Test fun concurrent_overlapping_commands_cannot_both_commit() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val candidates = listOf(2, 3).map { renewal(it, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01") }
        val results = candidates.map { candidate -> async(Dispatchers.IO) { repository.confirmRenewal(ConfirmRenewalCommand(candidate.operationId, candidate)) } }.awaitAll()
        assertEquals(1, results.count { it is SubscriptionMutationResult.Success })
        assertEquals(1, results.count { it is SubscriptionMutationResult.ValidationFailure || it is SubscriptionMutationResult.Conflict })
        assertEquals(2L, db.subscriptionDao().countRenewals(first.subscription.id))
    }

    @Test fun confirm_exact_replay_precedes_overlap_check_and_changed_replay_conflicts() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val next = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01")
        val command = ConfirmRenewalCommand(next.operationId, next)
        assertFalse(saved(repository.confirmRenewal(command)).replayed)
        assertTrue(saved(repository.confirmRenewal(command)).replayed)
        assertTrue(repository.confirmRenewal(command.copy(renewal = next.copy(reminderDueOn = next.reminderDueOn.plusDays(1)))) is SubscriptionMutationResult.Conflict)
        assertEquals(2L, db.subscriptionDao().countRenewals(first.subscription.id))
    }

    // M08-T04: changed price and changed cycle must never overwrite an earlier fact.
    @Test fun renewal_history_is_preserved() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val second = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01", 18000)
        val third = renewal(3, first.subscription.id, "2026-02-15", "2026-03-01", "2027-03-01", 120000, BillingCycle.YEARLY)
        saved(repository.confirmRenewal(ConfirmRenewalCommand(second.operationId, second)))
        saved(repository.confirmRenewal(ConfirmRenewalCommand(third.operationId, third)))
        reopen()
        val detail = read { repository.loadSubscriptionDetail(first.subscription.id, LocalDate.of(2026, 3, 1)) }!!
        assertEquals(listOf(third, second, first.initialRenewal), detail.renewals)
        assertEquals(listOf(365, 30, 30), detail.renewals.map { it.standardDays })
        assertEquals(153000L, detail.cost.confirmedPaidCents)
        assertEquals(425L, detail.cost.standardPurchasedDays)
        assertEquals(third, detail.currentRenewal)
        assertEquals(120000L, detail.cost.currentPrice!!.cents)
    }

    // M08-T05: four distinct dates survive real close/reopen unchanged.
    @Test fun accurate_dates_retained() = runBlocking {
        val command = create(1).let { it.copy(initialRenewal = it.initialRenewal.copy(
            paidOn = LocalDate.of(2026, 1, 1), effectiveFrom = LocalDate.of(2026, 1, 4),
            paidUntilExclusive = LocalDate.of(2026, 2, 5), reminderDueOn = LocalDate.of(2026, 2, 9),
        )) }
        saved(repository.createWithRenewal(command))
        reopen()
        val stored = db.subscriptionDao().findRenewal(command.initialRenewal.id)
        assertEquals(command.initialRenewal, stored)
        assertEquals(LocalDate.of(2026, 2, 5), stored!!.paidUntilExclusive)
        assertEquals(LocalDate.of(2026, 2, 9), stored.reminderDueOn)
    }

    // M08-T06: fixed cycle permits independently different actual CNY amounts.
    @Test fun currency_change_is_allowed() = runBlocking {
        val first = create(1).let { it.copy(initialRenewal = it.initialRenewal.copy(amountCents = 14683)) }
        saved(repository.createWithRenewal(first))
        val second = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01", 14810)
        saved(repository.confirmRenewal(ConfirmRenewalCommand(second.operationId, second)))
        val stored = db.subscriptionDao().renewalsForSubscriptions(listOf(first.subscription.id))
        assertEquals(listOf(second, first.initialRenewal), stored)
        assertEquals(listOf(BillingCycle.MONTHLY, BillingCycle.MONTHLY), stored.map { it.cycle })
        assertEquals(listOf(30, 30), stored.map { it.standardDays })
    }

    @Test fun prepaid_price_waits_for_effective_date_and_keeps_purchased_days_separate() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val next = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01", 18000)
        saved(repository.confirmRenewal(ConfirmRenewalCommand(next.operationId, next)))
        val jan19 = read { repository.loadSubscriptionDetail(first.subscription.id, LocalDate.of(2026, 1, 19)) }!!
        val jan20 = read { repository.loadSubscriptionDetail(first.subscription.id, LocalDate.of(2026, 1, 20)) }!!
        val feb1 = read { repository.loadSubscriptionDetail(first.subscription.id, LocalDate.of(2026, 2, 1)) }!!
        rate("5", jan19.cost.currentRate)
        rate("5", jan20.cost.currentRate)
        rate("6", feb1.cost.currentRate)
        assertEquals(15000L, jan19.cost.confirmedPaidCents)
        assertEquals(33000L, jan20.cost.confirmedPaidCents)
        assertEquals(60L, jan20.cost.standardPurchasedDays)
        assertEquals(20L, jan20.cost.elapsedPaidDays)
        assertEquals(first.initialRenewal, jan20.currentRenewal)
        assertEquals(next, feb1.currentRenewal)
    }

    @Test fun expired_service_has_no_fake_price_or_automatic_payment() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        val expired = read { repository.loadSubscriptionDetail(first.subscription.id, LocalDate.of(2026, 2, 1)) }!!
        assertEquals(SubscriptionDisplayStatus.EXPIRED, expired.cost.displayStatus)
        assertNull(expired.currentRenewal)
        assertNull(expired.cost.currentPrice)
        rate("0", expired.cost.currentRate)
        assertEquals(1L, db.subscriptionDao().countRenewals(first.subscription.id))
        assertTrue(expired.stateChanges.isEmpty())
    }

    @Test fun real_recent_payment_sort_is_not_effective_date_sort() = runBlocking {
        val earlyEffectiveLaterPaid = create(1).let { it.copy(initialRenewal = it.initialRenewal.copy(paidOn = LocalDate.of(2026, 1, 20))) }
        val laterEffectiveEarlierPaid = create(2).let { it.copy(initialRenewal = it.initialRenewal.copy(effectiveFrom = LocalDate.of(2026, 3, 1), paidUntilExclusive = LocalDate.of(2026, 4, 1))) }
        saved(repository.createWithRenewal(earlyEffectiveLaterPaid))
        saved(repository.createWithRenewal(laterEffectiveEarlierPaid))
        val page = read { repository.loadSubscriptionPage(asOf = LocalDate.of(2026, 1, 21)) }
        assertEquals(listOf(earlyEffectiveLaterPaid.subscription.id, laterEffectiveEarlierPaid.subscription.id), page.items.map { it.subscription.id })
        assertEquals(2L, page.totalCount)
    }

    @Test fun subscription_pages_and_refresh_window_keep_ties_and_moved_rows() = runBlocking {
        val commands = (1..53).map { create(it) }
        commands.forEach { command -> db.subscriptionDao().insertSubscription(command.subscription); db.subscriptionDao().insertRenewal(command.initialRenewal) }
        val first = read { repository.loadSubscriptionPage() }
        assertEquals(commands.take(50).map { it.subscription.id }, first.items.map { it.subscription.id })
        assertEquals(53L, first.totalCount)
        assertNotNull(first.nextCursor)
        val second = read { repository.loadSubscriptionPage(first.nextCursor) }
        assertEquals(commands.drop(50).map { it.subscription.id }, second.items.map { it.subscription.id })
        assertNull(second.nextCursor)
        val moved = renewal(900, commands.last().subscription.id, "2026-02-01", "2026-02-01", "2026-03-01")
        saved(repository.confirmRenewal(ConfirmRenewalCommand(moved.operationId, moved)))
        val refreshed = read { repository.loadSubscriptionWindow(2) }
        assertEquals(53, refreshed.items.size)
        assertEquals(53, refreshed.items.map { it.subscription.id }.distinct().size)
        assertEquals(commands.last().subscription.id, refreshed.items.first().subscription.id)
        assertEquals(commands.map { it.subscription.id }.toSet(), refreshed.items.map { it.subscription.id }.toSet())
    }

    @Test fun renewal_history_pages_are_sorted_by_payment_and_cost_uses_complete_history() = runBlocking {
        val identity = create(1).subscription
        db.subscriptionDao().insertSubscription(identity)
        val base = LocalDate.of(2020, 1, 1)
        val facts = (0..52).map { index ->
            val start = base.plusMonths(index.toLong())
            renewal(100 + index, identity.id, start.toString(), start.toString(), start.plusMonths(1).toString(), 100)
        }
        facts.forEach { db.subscriptionDao().insertRenewal(it) }
        val first = read { repository.loadRenewalHistory(identity.id) }
        assertEquals(facts.asReversed().take(50), first.items)
        assertEquals(53L, first.totalCount)
        val second = read { repository.loadRenewalHistory(identity.id, first.nextCursor) }
        assertEquals(facts.asReversed().drop(50), second.items)
        assertNull(second.nextCursor)
        val window = read { repository.loadRenewalHistoryWindow(identity.id, 2) }
        assertEquals(facts.asReversed(), window.items)
        val detail = read { repository.loadSubscriptionDetail(identity.id) }!!
        assertEquals(5300L, detail.cost.confirmedPaidCents)
        assertEquals(1590L, detail.cost.standardPurchasedDays)
    }

    @Test fun subscription_categories_and_count_read_real_typed_facts() = runBlocking {
        val categories = read { repository.loadSubscriptionCategories() }
        assertEquals(1, categories.size)
        assertEquals(CategoryType.SUBSCRIPTION, categories.single().type)
        assertEquals(LithicDatabase.SUBSCRIPTION_FALLBACK_ID, categories.single().id)
        assertEquals(0L, read { repository.loadSubscriptionCount() })
        saved(repository.createWithRenewal(create(1)))
        assertEquals(1L, read { repository.loadSubscriptionCount() })
    }

    @Test fun dao_rejects_orphan_renewal_and_duplicate_operation() = runBlocking {
        val first = create(1)
        constraint { db.subscriptionDao().insertRenewal(first.initialRenewal) }
        db.subscriptionDao().insertSubscription(first.subscription)
        db.subscriptionDao().insertRenewal(first.initialRenewal)
        constraint { db.subscriptionDao().insertRenewal(first.initialRenewal.copy(id = id(901))) }
        assertEquals(1L, db.subscriptionDao().countRenewals(first.subscription.id))
    }

    @Test fun state_schema_roundtrip_requires_unique_day_and_operation_and_existing_parent() = runBlocking {
        val identity = create(1).subscription
        val state = SubscriptionStateChangeEntity(id(901), identity.id, LocalDate.of(2026, 1, 10), SubscriptionOperationState.PAUSED, "明确原因", "历史说明", id(902))
        constraint { db.subscriptionDao().insertStateChange(state) }
        db.subscriptionDao().insertSubscription(identity)
        db.subscriptionDao().insertStateChange(state)
        constraint { db.subscriptionDao().insertStateChange(state.copy(id = id(903), operationId = id(904))) }
        constraint { db.subscriptionDao().insertStateChange(state.copy(id = id(905), effectiveOn = LocalDate.of(2026, 1, 11))) }
        assertEquals(listOf(state), db.subscriptionDao().statesForSubscriptions(listOf(identity.id)))
    }

    @Test fun corrupt_missing_payment_identity_and_closed_reads_are_never_fake_empty() = runBlocking {
        val existing = repository
        db.subscriptionDao().insertSubscription(create(1).subscription)
        assertReadFails { existing.loadSubscriptionPage() }
        assertReadFails { existing.loadSubscriptionCount() }
        db.close()
        assertReadFails { existing.loadSubscriptionCategories() }
    }

    @Test fun closed_database_write_returns_explicit_storage_failure_without_new_facts() = runBlocking {
        val existing = repository
        db.close()
        val result = existing.createWithRenewal(create(1))
        assertTrue(result is SubscriptionMutationResult.StorageFailure)
        assertEquals(StorageFailureKind.DATABASE, (result as SubscriptionMutationResult.StorageFailure).kind)
        reopen()
        assertEquals(0L, db.subscriptionDao().countSubscriptions())
    }

    @Test fun malformed_stored_cycle_is_corruption_and_never_overwritten_by_confirmation() = runBlocking {
        val first = create(1)
        saved(repository.createWithRenewal(first))
        withContext(Dispatchers.IO) { db.openHelper.writableDatabase.execSQL("UPDATE subscription_renewals SET cycle='VARIABLE' WHERE id=?", arrayOf(first.initialRenewal.id)) }
        val next = renewal(2, first.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01")
        val result = repository.confirmRenewal(ConfirmRenewalCommand(next.operationId, next))
        assertTrue(result is SubscriptionMutationResult.StorageFailure)
        assertEquals(StorageFailureKind.CORRUPT, (result as SubscriptionMutationResult.StorageFailure).kind)
        assertEquals(1L, db.subscriptionDao().countRenewals(first.subscription.id))
        assertReadFails { repository.loadSubscriptionDetail(first.subscription.id) }
    }

    @Test fun prepaid_first_period_has_purchased_days_but_no_elapsed_time_or_current_price() = runBlocking {
        val command = create(1).let { it.copy(initialRenewal = it.initialRenewal.copy(effectiveFrom = LocalDate.of(2026, 11, 1), paidUntilExclusive = LocalDate.of(2026, 12, 1))) }
        saved(repository.createWithRenewal(command))
        val detail = read { repository.loadSubscriptionDetail(command.subscription.id) }!!
        assertEquals(15000L, detail.cost.confirmedPaidCents)
        assertEquals(30L, detail.cost.standardPurchasedDays)
        assertEquals(0L, detail.cost.elapsedPaidDays)
        assertEquals(SubscriptionDisplayStatus.EXPIRED, detail.cost.displayStatus)
        assertNull(detail.cost.currentPrice)
        assertNull(detail.currentRenewal)
        rate("0", detail.cost.currentRate)
    }

    @Test fun invalid_page_sizes_and_noncanonical_cursors_are_rejected() = runBlocking {
        listOf(0, 51).forEach { size ->
            assertTrue(runCatching { repository.loadSubscriptionPage(pageSize = size) }.exceptionOrNull() is IllegalArgumentException)
            assertTrue(runCatching { repository.loadRenewalHistory(id(1), pageSize = size) }.exceptionOrNull() is IllegalArgumentException)
        }
        assertTrue(runCatching { repository.loadSubscriptionPage(SubscriptionCursor(today, "BAD-ID")) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { repository.loadRenewalHistory(id(1), RenewalCursor(today, "BAD-ID")) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun real_changes_emit_initial_state_create_and_confirmation_without_count_guessing() = runBlocking {
        val events = Channel<SubscriptionPage>(Channel.UNLIMITED)
        val observing = launch(Dispatchers.IO) { repository.changes.collect { events.send(repository.loadSubscriptionWindow(1)) } }
        suspend fun awaitDays(expected: Long): SubscriptionPage = withTimeout(5000) {
            var page = events.receive()
            while (page.items.sumOf { it.cost.standardPurchasedDays } != expected) page = events.receive()
            page
        }
        try {
            assertEquals(0L, awaitDays(0).totalCount)
            val command = create(1)
            saved(repository.createWithRenewal(command))
            assertEquals(1L, awaitDays(30).totalCount)
            val next = renewal(2, command.subscription.id, "2026-01-20", "2026-02-01", "2026-03-01")
            saved(repository.confirmRenewal(ConfirmRenewalCommand(next.operationId, next)))
            val updated = awaitDays(60)
            assertEquals(1L, updated.totalCount)
            assertEquals(listOf(next, command.initialRenewal), updated.items.single().renewals)
        } finally {
            observing.cancel()
            observing.join()
            events.close()
        }
    }

    private fun create(index: Int): CreateSubscriptionCommand {
        val subscription = SubscriptionEntity(id(1000 + index), "服务$index", LithicDatabase.SUBSCRIPTION_FALLBACK_ID, createdAt = instant, updatedAt = instant)
        val first = renewal(index, subscription.id, "2026-01-01", "2026-01-01", "2026-02-01")
        return CreateSubscriptionCommand(first.operationId, subscription, first)
    }

    private fun renewal(index: Int, subscriptionId: String, paid: String, from: String, until: String,
        cents: Long = 15000, cycle: BillingCycle = BillingCycle.MONTHLY): SubscriptionRenewalEntity =
        SubscriptionRenewalEntity(id(2000 + index), subscriptionId, LocalDate.parse(paid), LocalDate.parse(from), LocalDate.parse(until),
            LocalDate.parse(until).plusDays(2), cents, cycle, cycle.standardDays, id(3000 + index))

    private fun id(value: Int): String = "9a000000-0000-4000-8000-${value.toString().padStart(12, '0')}"
    private fun saved(value: SubscriptionMutationResult): SubscriptionMutationResult.Success {
        assertTrue("A valid real confirmation must be saved, actual=$value", value is SubscriptionMutationResult.Success)
        return value as SubscriptionMutationResult.Success
    }
    private fun rate(expected: String, actual: BigDecimal) { assertEquals(0, BigDecimal(expected).compareTo(actual)) }
    private suspend fun <T> read(block: suspend () -> T): T = try { block() } catch (failure: Exception) {
        throw AssertionError("A valid stored subscription must be readable: ${failure.message}", failure)
    }
    private suspend fun constraint(block: suspend () -> Unit) {
        try { block(); fail("Expected a real SQLite constraint rejection") } catch (_: SQLiteConstraintException) { }
    }
    private suspend fun assertReadFails(block: suspend () -> Unit) {
        var rejected = false
        try { block() } catch (_: Exception) { rejected = true }
        assertTrue("Invalid/unavailable facts must fail rather than appear as successful empty records", rejected)
    }
    private suspend fun reopen() {
        db.close()
        db = withContext(Dispatchers.IO) { LithicDatabase.open(context, file, requireExisting = true) }
        db.requireDefaults()
    }
}
