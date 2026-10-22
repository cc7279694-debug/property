package com.cooper.lithic.data.repository

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.room.withTransaction
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionIntervalRules
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.DateTimeException
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Non-owning facade over the existing generation, one coordinator and the frozen M03 engine. */
open class SubscriptionRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    open suspend fun setState(command: SetSubscriptionStateCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val expected = command.expected
            val target = command.target
            validateStateIdentity(target, expected.subscription.id)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            val sameDay = expected.stateChanges.singleOrNull { it.effectiveOn == target.effectiveOn }
            if (sameDay != null && (sameDay.id != target.id || sameDay.operationId != target.operationId)) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("Same-day correction must preserve the original state identity and operation")
            }
            if (sameDay == null && (expected.stateChanges.any { it.id == target.id || it.operationId == target.operationId } || target.operationId != command.commandId)) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("A new boundary requires independent state identity and matching operation")
            }
            val candidate = expected.copy(stateChanges = expected.stateChanges.filterNot { it.id == target.id } + target)
            // The original envelope may be recovered on a later business day. Persisted facts, not
            // today's newly computed defaults, prove whether that fixed target already committed.
            if (sameFacts(current, candidate)) return@lifecycleMutation savedState(target, true)
            if (!sameFacts(current, expected)) return@lifecycleMutation staleFacts()
            if (target.state == SubscriptionOperationState.STOPPED && sameDay == null &&
                current.stateChanges.any { it.state == SubscriptionOperationState.STOPPED && it.effectiveOn.isAfter(clock.today()) }) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("A future stop is already scheduled; explicitly correct or cancel its original fact")
            }
            stateIdentityConflict(target)?.let { return@lifecycleMutation SubscriptionLifecycleResult.Conflict(it) }
            validateStateAction(current, target, command.action)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            if (sameDay == null) db.subscriptionDao().insertStateChange(target)
            else check(db.subscriptionDao().updateStateChange(target) == 1) { "The checked state disappeared during correction" }
            requireCommitted(candidate)
            val today = clock.today()
            if (MilestoneHistoryChanges.state(sameDay?.let(::milestoneState), milestoneState(target), today)) {
                MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, target.subscriptionId, today)
            }
            savedState(target, false)
        }

    open suspend fun updateState(command: UpdateSubscriptionStateCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val old = command.expectedState
            val target = command.target
            validateStateIdentity(target, command.expected.subscription.id)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            if (old !in command.expected.stateChanges || target.id != old.id || target.subscriptionId != old.subscriptionId || target.operationId != old.operationId) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("State correction must preserve the complete loaded fact identity")
            }
            val candidate = command.expected.copy(stateChanges = command.expected.stateChanges.map { if (it.id == old.id) target else it })
            if (sameFacts(current, candidate)) return@lifecycleMutation savedState(target, true)
            if (!sameFacts(current, command.expected)) return@lifecycleMutation staleFacts()
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            check(db.subscriptionDao().updateStateChange(target) == 1) { "The checked state disappeared during correction" }
            requireCommitted(candidate)
            val today = clock.today()
            if (MilestoneHistoryChanges.state(milestoneState(old), milestoneState(target), today)) {
                MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, target.subscriptionId, today)
            }
            savedState(target, false)
        }

    open suspend fun cancelScheduledStop(command: CancelScheduledStopCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val old = command.expectedState
            if (old !in command.expected.stateChanges || old.state != SubscriptionOperationState.STOPPED) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Only the loaded scheduled STOPPED fact can be cancelled")
            }
            if (current.stateChanges.none { it.id == old.id }) {
                return@lifecycleMutation SubscriptionLifecycleResult.CurrentAbsence(current.subscription.id, old.id)
            }
            if (!sameFacts(current, command.expected)) return@lifecycleMutation staleFacts()
            if (!old.effectiveOn.isAfter(clock.today())) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("An already effective stop is history; use explicit reopening or correction")
            }
            val candidate = current.copy(stateChanges = current.stateChanges.filterNot { it.id == old.id })
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            check(db.subscriptionDao().deleteStateChange(old.id) == 1) { "The checked stop disappeared during cancellation" }
            requireCommitted(candidate)
            SubscriptionLifecycleResult.CurrentAbsence(current.subscription.id, old.id)
        }

    open suspend fun updateRenewal(command: UpdateSubscriptionRenewalCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val old = command.expectedRenewal
            val target = command.target
            if (old !in command.expected.renewals || !sameRenewalIdentity(old, target)) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Payment correction must preserve the original renewal, service and operation identities")
            }
            val candidate = command.expected.copy(renewals = command.expected.renewals.map { if (it.id == old.id) target else it })
            if (sameFacts(current, candidate)) return@lifecycleMutation savedRenewal(target, true)
            if (!sameFacts(current, command.expected)) return@lifecycleMutation staleFacts()
            validatePayment(target)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            paymentStopConflict(current, target, old)?.let { return@lifecycleMutation SubscriptionLifecycleResult.Conflict(it) }
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            check(db.subscriptionDao().updateRenewal(target) == 1) { "The checked payment disappeared during correction" }
            requireCommitted(candidate)
            val today = clock.today()
            if (MilestoneHistoryChanges.renewal(toRenewal(old), toRenewal(target), today)) {
                MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, target.subscriptionId, today)
            }
            savedRenewal(target, false)
        }

    open suspend fun deleteRenewal(command: DeleteSubscriptionRenewalCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val old = command.expectedRenewal
            if (old !in command.expected.renewals) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Deletion requires the complete loaded real payment fact")
            }
            if (current.renewals.none { it.id == old.id }) {
                return@lifecycleMutation SubscriptionLifecycleResult.CurrentAbsence(current.subscription.id, old.id)
            }
            if (!sameFacts(current, command.expected)) return@lifecycleMutation staleFacts()
            if (current.renewals.size == 1) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("The last confirmed payment cannot be deleted while preserving its subscription")
            }
            val candidate = current.copy(renewals = current.renewals.filterNot { it.id == old.id })
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            check(db.subscriptionDao().deleteRenewal(old.id) == 1) { "The checked payment disappeared during deletion" }
            requireCommitted(candidate)
            val today = clock.today()
            if (MilestoneHistoryChanges.renewal(toRenewal(old), null, today)) {
                MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, old.subscriptionId, today)
            }
            SubscriptionLifecycleResult.CurrentAbsence(current.subscription.id, old.id)
        }

    open suspend fun reenableWithRenewal(command: ReenableSubscriptionWithRenewalCommand): SubscriptionLifecycleResult =
        lifecycleMutation(command.commandId, command.expected) { current ->
            val expected = command.expected
            val payment = command.renewal
            val target = command.targetState
            validateCommandIdentity(command.commandId, payment)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            validateStateIdentity(target, expected.subscription.id)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            if (payment.subscriptionId != expected.subscription.id || target.state != SubscriptionOperationState.ENABLED || expected.renewals.any { it.id == payment.id || it.operationId == payment.operationId }) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Explicit reopening requires a new real payment and ENABLED on the original service")
            }
            val sameDay = expected.stateChanges.singleOrNull { it.effectiveOn == target.effectiveOn }
            if (sameDay != null && (target.id != sameDay.id || target.operationId != sameDay.operationId)) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("Same-day reopening must correct the original state fact")
            }
            if (sameDay == null && (target.operationId != command.commandId || expected.stateChanges.any { it.id == target.id || it.operationId == target.operationId })) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("New reopening state requires independent identity and matching operation")
            }
            val candidate = expected.copy(renewals = expected.renewals + payment, stateChanges = expected.stateChanges.filterNot { it.id == target.id } + target)
            if (sameFacts(current, candidate)) return@lifecycleMutation SubscriptionLifecycleResult.Saved(expected.subscription.id, target.id, payment.id, true)
            if (!sameFacts(current, expected)) return@lifecycleMutation staleFacts()
            if (db.subscriptionDao().findRenewal(payment.id) != null || db.subscriptionDao().findRenewalByOperation(payment.operationId) != null) {
                return@lifecycleMutation SubscriptionLifecycleResult.Conflict("The payment identity or operation already belongs to stored facts")
            }
            stateIdentityConflict(target)?.let { return@lifecycleMutation SubscriptionLifecycleResult.Conflict(it) }
            val today = clock.today()
            val status = SubscriptionCostEngine.evaluate(snapshot(current.subscription, current.renewals, current.stateChanges), today).displayStatus
            if (status != SubscriptionDisplayStatus.STOPPED && status != SubscriptionDisplayStatus.PAUSED) {
                return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Use ordinary real renewal for a service already enabled")
            }
            if (target.effectiveOn != today) return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure("Reopening must use the business day of confirmation")
            validatePayment(payment)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            paymentStopConflict(current, payment)?.let { return@lifecycleMutation SubscriptionLifecycleResult.Conflict(it) }
            validateLifecycleCandidate(candidate)?.let { return@lifecycleMutation SubscriptionLifecycleResult.ValidationFailure(it) }
            db.subscriptionDao().insertRenewal(payment)
            if (sameDay == null) db.subscriptionDao().insertStateChange(target)
            else check(db.subscriptionDao().updateStateChange(target) == 1) { "The checked state disappeared during reopening" }
            requireCommitted(candidate)
            if (MilestoneHistoryChanges.renewal(null, toRenewal(payment), today) ||
                MilestoneHistoryChanges.state(sameDay?.let(::milestoneState), milestoneState(target), today)) {
                MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, target.subscriptionId, today)
            }
            SubscriptionLifecycleResult.Saved(expected.subscription.id, target.id, payment.id, false)
        }

    open suspend fun loadManagementPage(
        stopped: Boolean,
        cursor: SubscriptionCursor? = null,
        pageSize: Int = 50,
        asOf: LocalDate = clock.today(),
    ): SubscriptionPage {
        require(pageSize in 1..50) { "A subscription page must contain between 1 and 50 records" }
        cursor?.let { StoredValueValidation.requireUuid(it.id) }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                pageInTransaction(cursor, pageSize, asOf, db.subscriptionDao().countManagement(stopped, asOf), stopped)
            }
        }
    }

    open suspend fun loadManagementWindow(
        stopped: Boolean,
        pageCount: Int,
        asOf: LocalDate = clock.today(),
    ): SubscriptionPage {
        require(pageCount > 0) { "A subscription window must contain at least one page" }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                val count = db.subscriptionDao().countManagement(stopped, asOf)
                val items = mutableListOf<SubscriptionDetail>()
                var cursor: SubscriptionCursor? = null
                for (pageIndex in 0 until pageCount) {
                    val page = pageInTransaction(cursor, 50, asOf, count, stopped)
                    items += page.items
                    cursor = page.nextCursor
                    if (cursor == null) break
                }
                SubscriptionPage(items, cursor, count, asOf)
            }
        }
    }

    open suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val identity = command.subscription
                val renewal = command.initialRenewal
                validateCommandIdentity(command.commandId, renewal, identity)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                requireAllStoredFacts(clock.today())
                val previous = db.subscriptionDao().findRenewalByOperation(command.commandId)
                if (previous != null) {
                    val existingIdentity = db.subscriptionDao().findSubscription(previous.subscriptionId)
                    storedFacts(previous.subscriptionId)
                    return@withTransaction if (previous == renewal && existingIdentity == identity) {
                        SubscriptionMutationResult.Success(identity.id, renewal.id, true)
                    } else SubscriptionMutationResult.Conflict("The operation was already used for different contents")
                }
                if (db.subscriptionDao().findSubscription(identity.id) != null || db.subscriptionDao().findRenewal(renewal.id) != null) {
                    return@withTransaction SubscriptionMutationResult.Conflict("The subscription or renewal identity already belongs to stored facts")
                }
                validateNewIdentity(identity)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                if (renewal.subscriptionId != identity.id) return@withTransaction SubscriptionMutationResult.ValidationFailure("The first payment must reference its new subscription")
                val category = db.categoryDao().find(identity.categoryId)
                    ?: return@withTransaction SubscriptionMutationResult.ValidationFailure("The referenced category does not exist")
                if (category.type != CategoryType.SUBSCRIPTION) return@withTransaction SubscriptionMutationResult.ValidationFailure("A subscription cannot use a project category")
                if (identity.coverAssetId != null && db.mediaDao().find(identity.coverAssetId) == null) {
                    return@withTransaction SubscriptionMutationResult.ValidationFailure("The referenced cover metadata does not exist")
                }
                validatePayment(renewal)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                val snapshot = SubscriptionSnapshot(Subscription(identity.id), listOf(toRenewal(renewal)))
                validateCandidate(snapshot)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                db.subscriptionDao().insertSubscription(identity)
                db.subscriptionDao().insertRenewal(renewal)
                MilestoneWatermarkStore.subscriptionCreatedWithinTransaction(db, identity.id, clock.today())
                SubscriptionMutationResult.Success(identity.id, renewal.id, false)
            }
        }
    }

    open suspend fun confirmRenewal(command: ConfirmRenewalCommand): SubscriptionMutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val renewal = command.renewal
                validateCommandIdentity(command.commandId, renewal)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                requireAllStoredFacts(clock.today())
                val previous = db.subscriptionDao().findRenewalByOperation(command.commandId)
                if (previous != null) {
                    storedFacts(previous.subscriptionId)
                    return@withTransaction if (previous == renewal) {
                        SubscriptionMutationResult.Success(previous.subscriptionId, previous.id, true)
                    } else SubscriptionMutationResult.Conflict("The operation was already used for different contents")
                }
                val identity = db.subscriptionDao().findSubscription(renewal.subscriptionId)
                    ?: return@withTransaction SubscriptionMutationResult.ValidationFailure("The referenced subscription does not exist")
                if (db.subscriptionDao().findRenewal(renewal.id) != null) return@withTransaction SubscriptionMutationResult.Conflict("The renewal identity already belongs to another fact")
                val category = db.categoryDao().find(identity.categoryId) ?: throw InvalidStoredFacts("The subscription category is missing")
                requireStoredIdentity(identity, category)
                val renewals = db.subscriptionDao().renewalsForSubscriptions(listOf(identity.id))
                val states = db.subscriptionDao().statesForSubscriptions(listOf(identity.id))
                val existing = snapshot(identity, renewals, states)
                // Stored corruption/overflow is distinct from a rejected new candidate.
                val status = SubscriptionCostEngine.evaluate(existing, clock.today()).displayStatus
                if (status == SubscriptionDisplayStatus.STOPPED) {
                    return@withTransaction SubscriptionMutationResult.Conflict("A stopped service requires explicit reopening with its real payment")
                }
                validatePayment(renewal)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                paymentStopConflict(SubscriptionFacts(identity, renewals, states), renewal)?.let {
                    return@withTransaction SubscriptionMutationResult.Conflict(it)
                }
                val candidate = existing.copy(renewals = existing.renewals + toRenewal(renewal))
                validateCandidate(candidate)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                db.subscriptionDao().insertRenewal(renewal)
                val today = clock.today()
                if (MilestoneHistoryChanges.renewal(null, toRenewal(renewal), today)) {
                    MilestoneWatermarkStore.subscriptionChangedWithinTransaction(db, identity.id, today)
                }
                SubscriptionMutationResult.Success(identity.id, renewal.id, false)
            }
        }
    }

    /** Null is only a successful query proving the identity is absent; read failures propagate. */
    open suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate = clock.today()): SubscriptionDetail? {
        StoredValueValidation.requireUuid(id)
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                val identity = db.subscriptionDao().findSubscription(id) ?: return@withTransaction null
                val category = db.categoryDao().find(identity.categoryId) ?: throw InvalidStoredFacts("The subscription category is missing")
                detail(identity, category, db.subscriptionDao().renewalsForSubscriptions(listOf(id)), db.subscriptionDao().statesForSubscriptions(listOf(id)), asOf)
            }
        }
    }

    open suspend fun loadSubscriptionPage(cursor: SubscriptionCursor? = null, pageSize: Int = 50, asOf: LocalDate = clock.today()): SubscriptionPage {
        require(pageSize in 1..50) { "A subscription page must contain between 1 and 50 records" }
        cursor?.let { StoredValueValidation.requireUuid(it.id) }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                pageInTransaction(cursor, pageSize, asOf, db.subscriptionDao().countSubscriptions())
            }
        }
    }

    /** Rebuild all loaded pages inside one snapshot when a payment changes the recent-sort key. */
    open suspend fun loadSubscriptionWindow(pageCount: Int, asOf: LocalDate = clock.today()): SubscriptionPage {
        require(pageCount > 0) { "A subscription window must contain at least one page" }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                val count = db.subscriptionDao().countSubscriptions()
                val items = mutableListOf<SubscriptionDetail>()
                var cursor: SubscriptionCursor? = null
                for (pageIndex in 0 until pageCount) {
                    val page = pageInTransaction(cursor, 50, asOf, count)
                    items += page.items
                    cursor = page.nextCursor
                    if (cursor == null) break
                }
                SubscriptionPage(items, cursor, count, asOf)
            }
        }
    }

    open suspend fun loadRenewalHistory(subscriptionId: String, cursor: RenewalCursor? = null, pageSize: Int = 50, asOf: LocalDate = clock.today()): RenewalHistoryPage {
        StoredValueValidation.requireUuid(subscriptionId)
        cursor?.let { StoredValueValidation.requireUuid(it.id) }
        require(pageSize in 1..50) { "A renewal page must contain between 1 and 50 records" }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                val count = requireHistoryIdentity(subscriptionId)
                historyInTransaction(subscriptionId, cursor, pageSize, count, asOf)
            }
        }
    }

    open suspend fun loadRenewalHistoryWindow(subscriptionId: String, pageCount: Int, asOf: LocalDate = clock.today()): RenewalHistoryPage {
        StoredValueValidation.requireUuid(subscriptionId)
        require(pageCount > 0) { "A renewal window must contain at least one page" }
        return coordinator.withSnapshot {
            db.withTransaction {
                requireAllStoredFacts(asOf)
                val count = requireHistoryIdentity(subscriptionId)
                val items = mutableListOf<SubscriptionRenewalEntity>()
                var cursor: RenewalCursor? = null
                for (pageIndex in 0 until pageCount) {
                    val page = historyInTransaction(subscriptionId, cursor, 50, count, asOf)
                    items += page.items
                    cursor = page.nextCursor
                    if (cursor == null) break
                }
                RenewalHistoryPage(items, cursor, count, asOf)
            }
        }
    }

    open suspend fun loadSubscriptionCategories(): List<CategoryEntity> = coordinator.withSnapshot {
        db.withTransaction { requireAllStoredFacts(clock.today()); db.subscriptionDao().subscriptionCategories() }
    }
    open suspend fun loadSubscriptionCount(): Long = coordinator.withSnapshot {
        db.withTransaction { requireAllStoredFacts(clock.today()); db.subscriptionDao().countSubscriptions() }
    }
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "subscriptions", "subscription_renewals", "subscription_state_changes", "categories",
    ).map { Unit }

    private suspend fun pageInTransaction(cursor: SubscriptionCursor?, size: Int, asOf: LocalDate, count: Long, stopped: Boolean? = null): SubscriptionPage {
        val dao = db.subscriptionDao()
        val queried = if (stopped == null) dao.pageSubscriptionKeys(cursor?.lastPaidOn, cursor?.id, size + 1)
            else dao.pageManagementKeys(stopped, asOf, cursor?.lastPaidOn, cursor?.id, size + 1)
        val visible = queried.take(size)
        if (visible.isEmpty()) return SubscriptionPage(emptyList(), null, count, asOf)
        val ids = visible.map { it.id }
        val identities = dao.subscriptionsForIds(ids).associateBy { it.id }
        val renewals = dao.renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
        val states = dao.statesForSubscriptions(ids).groupBy { it.subscriptionId }
        val categories = dao.categoriesForSubscriptions(identities.values.map { it.categoryId }.distinct()).associateBy { it.id }
        val items = visible.map { key ->
            val identity = identities[key.id] ?: throw InvalidStoredFacts("The queried subscription is missing")
            val category = categories[identity.categoryId] ?: throw InvalidStoredFacts("The subscription category is missing")
            if (key.lastPaidOn == null) throw InvalidStoredFacts("A stored subscription has no confirmed payment")
            detail(identity, category, renewals[key.id].orEmpty(), states[key.id].orEmpty(), asOf)
        }
        val next = if (queried.size > size) visible.last().let { SubscriptionCursor(checkNotNull(it.lastPaidOn), it.id) } else null
        return SubscriptionPage(items, next, count, asOf)
    }

    private suspend fun requireHistoryIdentity(id: String): Long {
        val identity = db.subscriptionDao().findSubscription(id) ?: error("The subscription does not exist")
        val category = db.categoryDao().find(identity.categoryId) ?: throw InvalidStoredFacts("The subscription category is missing")
        requireStoredIdentity(identity, category)
        val renewals = db.subscriptionDao().renewalsForSubscriptions(listOf(id))
        val states = db.subscriptionDao().statesForSubscriptions(listOf(id))
        SubscriptionCostEngine.evaluate(snapshot(identity, renewals, states), clock.today())
        val count = db.subscriptionDao().countRenewals(id)
        if (count == 0L) throw InvalidStoredFacts("A stored subscription has no confirmed payment")
        return count
    }

    private suspend fun historyInTransaction(id: String, cursor: RenewalCursor?, size: Int, count: Long, asOf: LocalDate): RenewalHistoryPage {
        val queried = db.subscriptionDao().pageRenewals(id, cursor?.paidOn, cursor?.id, size + 1)
        val visible = queried.take(size)
        visible.forEach { row -> requireStoredRenewal(row, id); toRenewal(row) }
        val next = if (queried.size > size) visible.last().let { RenewalCursor(it.paidOn, it.id) } else null
        return RenewalHistoryPage(visible, next, count, asOf)
    }

    private fun detail(identity: SubscriptionEntity, category: CategoryEntity, renewals: List<SubscriptionRenewalEntity>, states: List<SubscriptionStateChangeEntity>, asOf: LocalDate): SubscriptionDetail {
        requireStoredIdentity(identity, category)
        val input = snapshot(identity, renewals, states)
        val cost = SubscriptionCostEngine.evaluate(input, asOf)
        val current = if (cost.displayStatus == SubscriptionDisplayStatus.ACTIVE) renewals.singleOrNull {
            !it.paidOn.isAfter(asOf) && !asOf.isBefore(it.effectiveFrom) && asOf.isBefore(it.paidUntilExclusive)
        } else null
        return SubscriptionDetail(identity, category, renewals, states, cost, current, asOf)
    }

    private fun snapshot(identity: SubscriptionEntity, renewals: List<SubscriptionRenewalEntity>, states: List<SubscriptionStateChangeEntity>): SubscriptionSnapshot {
        if (renewals.isEmpty()) throw InvalidStoredFacts("A stored subscription has no confirmed payment")
        if (renewals.map { it.id }.distinct().size != renewals.size || renewals.map { it.operationId }.distinct().size != renewals.size ||
            states.map { it.id }.distinct().size != states.size || states.map { it.operationId }.distinct().size != states.size) {
            throw InvalidStoredFacts("Stored payment or state identities are duplicated")
        }
        renewals.forEach { requireStoredRenewal(it, identity.id) }
        states.forEach {
            listOf(it.id, it.subscriptionId, it.operationId).forEach(StoredValueValidation::requireUuid)
            if (it.subscriptionId != identity.id) throw InvalidStoredFacts("A state fact refers to another subscription")
        }
        val result = SubscriptionSnapshot(Subscription(identity.id), renewals.map(::toRenewal), states.map { SubscriptionStateChange(it.effectiveOn, it.state) })
        SubscriptionIntervalRules.validateStructure(result)
        return result
    }

    private fun requireStoredIdentity(identity: SubscriptionEntity, category: CategoryEntity) {
        listOf(identity.id, identity.categoryId).plus(listOfNotNull(identity.coverAssetId)).forEach(StoredValueValidation::requireUuid)
        if (identity.name.isBlank() || identity.updatedAt.isBefore(identity.createdAt) || category.id != identity.categoryId || category.type != CategoryType.SUBSCRIPTION) {
            throw InvalidStoredFacts("Stored subscription identity or category is invalid")
        }
    }

    private fun requireStoredRenewal(row: SubscriptionRenewalEntity, subscriptionId: String) {
        listOf(row.id, row.subscriptionId, row.operationId).forEach(StoredValueValidation::requireUuid)
        if (row.subscriptionId != subscriptionId) throw InvalidStoredFacts("A payment refers to another subscription")
    }

    private fun toRenewal(row: SubscriptionRenewalEntity) = Renewal(Money(row.amountCents), row.cycle, row.paidOn, row.effectiveFrom, row.paidUntilExclusive, row.reminderDueOn, row.standardDays)

    private fun milestoneState(row: SubscriptionStateChangeEntity) = SubscriptionStateChange(row.effectiveOn, row.state)

    private fun validateCommandIdentity(commandId: String, renewal: SubscriptionRenewalEntity, identity: SubscriptionEntity? = null): String? {
        val ids = listOf(commandId, renewal.id, renewal.subscriptionId, renewal.operationId) +
            if (identity == null) emptyList() else listOf(identity.id, identity.categoryId) + listOfNotNull(identity.coverAssetId)
        if (ids.any { !StoredValueValidation.isUuid(it) }) return "Business identities must be canonical UUID strings"
        if (renewal.operationId != commandId) return "The payment operation must match its command"
        return null
    }

    private fun validateNewIdentity(identity: SubscriptionEntity): String? = when {
        identity.name.isBlank() -> "The subscription name cannot be blank"
        identity.updatedAt.isBefore(identity.createdAt) -> "The update instant cannot precede creation"
        else -> null
    }

    private fun validatePayment(renewal: SubscriptionRenewalEntity): String? = try {
        toRenewal(renewal)
        null
    } catch (invalid: IllegalArgumentException) {
        invalid.message ?: "The real payment, cycle or coverage dates are invalid"
    }

    private fun validateCandidate(snapshot: SubscriptionSnapshot): String? = try {
        val today = clock.today()
        SubscriptionIntervalRules.validateForWrite(snapshot, BusinessClock { today })
        SubscriptionCostEngine.evaluate(snapshot, today)
        null
    } catch (invalid: IllegalArgumentException) {
        invalid.message ?: "The payment cannot be recorded"
    } catch (_: ArithmeticException) {
        "The confirmed payment totals exceed the supported range"
    }

    private suspend fun lifecycleMutation(
        commandId: String,
        expected: SubscriptionFacts,
        operation: suspend (SubscriptionFacts) -> SubscriptionLifecycleResult,
    ): SubscriptionLifecycleResult = coordinator.withWrite {
        lifecycleStorageOperation {
            db.withTransaction {
                validateLifecycleEnvelope(commandId, expected)?.let {
                    return@withTransaction SubscriptionLifecycleResult.ValidationFailure(it)
                }
                requireAllStoredFacts(clock.today())
                val current = storedFacts(expected.subscription.id)
                    ?: return@withTransaction SubscriptionLifecycleResult.Conflict("The loaded subscription no longer exists")
                operation(current)
            }
        }
    }

    private fun validateLifecycleEnvelope(commandId: String, expected: SubscriptionFacts): String? {
        val identity = expected.subscription
        val ids = listOf(commandId, identity.id, identity.categoryId) + listOfNotNull(identity.coverAssetId) +
            expected.renewals.flatMap { listOf(it.id, it.subscriptionId, it.operationId) } +
            expected.stateChanges.flatMap { listOf(it.id, it.subscriptionId, it.operationId) }
        if (ids.any { !StoredValueValidation.isUuid(it) }) return "Command and loaded fact identities must be canonical UUID strings"
        if (identity.name.isBlank() || identity.updatedAt.isBefore(identity.createdAt)) return "The loaded subscription identity is invalid"
        if (expected.renewals.isEmpty() || expected.renewals.any { it.subscriptionId != identity.id } || expected.stateChanges.any { it.subscriptionId != identity.id }) {
            return "The complete baseline must contain real payments and states belonging to its subscription"
        }
        if (expected.renewals.map { it.id }.distinct().size != expected.renewals.size ||
            expected.renewals.map { it.operationId }.distinct().size != expected.renewals.size ||
            expected.stateChanges.map { it.id }.distinct().size != expected.stateChanges.size ||
            expected.stateChanges.map { it.operationId }.distinct().size != expected.stateChanges.size ||
            expected.stateChanges.map { it.effectiveOn }.distinct().size != expected.stateChanges.size) {
            return "The loaded baseline contains duplicate identity, operation or state-day facts"
        }
        return try {
            snapshot(identity, expected.renewals, expected.stateChanges)
            null
        } catch (invalid: IllegalArgumentException) {
            invalid.message ?: "The loaded payment baseline is invalid"
        }
    }

    /** Validate complete facts before paging/filtering so a corrupt excluded row cannot disappear. */
    private suspend fun requireAllStoredFacts(asOf: LocalDate) {
        val dao = db.subscriptionDao()
        if (dao.countOrphanRenewals() != 0L || dao.countOrphanStates() != 0L) {
            throw InvalidStoredFacts("A payment or state references a missing subscription")
        }
        var afterId: String? = null
        while (true) {
            // Complete corruption detection remains O(total facts), but each identity batch and
            // its associated timelines are released before advancing the UUID keyset cursor.
            val identities = dao.validationSubscriptions(afterId, 50)
            if (identities.isEmpty()) break
            val ids = identities.map { it.id }
            val categories = dao.categoriesForSubscriptions(identities.map { it.categoryId }.distinct()).associateBy { it.id }
            val paymentsBySubscription = dao.renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
            val statesBySubscription = dao.statesForSubscriptions(ids).groupBy { it.subscriptionId }
            for (identity in identities) {
                val category = categories[identity.categoryId] ?: throw InvalidStoredFacts("The subscription category is missing")
                requireStoredIdentity(identity, category)
                if (identity.coverAssetId != null && db.mediaDao().find(identity.coverAssetId) == null) {
                    throw InvalidStoredFacts("The subscription cover metadata is missing")
                }
                val input = snapshot(identity, paymentsBySubscription[identity.id].orEmpty(), statesBySubscription[identity.id].orEmpty())
                SubscriptionCostEngine.evaluate(input, asOf)
                // Check totals including later real facts even when observing an earlier business day.
                if (input.renewals.any { it.paidOn.isAfter(asOf) }) SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
            }
            afterId = identities.last().id
            if (identities.size < 50) break
        }
    }

    private suspend fun storedFacts(id: String): SubscriptionFacts? {
        val identity = db.subscriptionDao().findSubscription(id) ?: return null
        val category = db.categoryDao().find(identity.categoryId) ?: throw InvalidStoredFacts("The subscription category is missing")
        requireStoredIdentity(identity, category)
        val renewals = db.subscriptionDao().renewalsForSubscriptions(listOf(id))
        val states = db.subscriptionDao().statesForSubscriptions(listOf(id))
        SubscriptionCostEngine.evaluate(snapshot(identity, renewals, states), clock.today())
        return SubscriptionFacts(identity, renewals, states)
    }

    private fun sameFacts(first: SubscriptionFacts, second: SubscriptionFacts): Boolean =
        first.subscription == second.subscription && first.renewals.size == second.renewals.size &&
            first.stateChanges.size == second.stateChanges.size && first.renewals.toSet() == second.renewals.toSet() &&
            first.stateChanges.toSet() == second.stateChanges.toSet()

    private fun sameRenewalIdentity(old: SubscriptionRenewalEntity, target: SubscriptionRenewalEntity): Boolean =
        old.id == target.id && old.subscriptionId == target.subscriptionId && old.operationId == target.operationId

    private fun validateStateIdentity(target: SubscriptionStateChangeEntity, subscriptionId: String): String? = when {
        listOf(target.id, target.subscriptionId, target.operationId).any { !StoredValueValidation.isUuid(it) } -> "State identities must be canonical UUID strings"
        target.subscriptionId != subscriptionId -> "The state must belong to the loaded subscription"
        else -> null
    }

    private suspend fun stateIdentityConflict(target: SubscriptionStateChangeEntity): String? {
        val dao = db.subscriptionDao()
        val byId = dao.findStateChange(target.id)
        val byOperation = dao.findStateByOperation(target.operationId)
        return if ((byId != null && (byId.subscriptionId != target.subscriptionId || byId.operationId != target.operationId)) ||
            (byOperation != null && (byOperation.id != target.id || byOperation.subscriptionId != target.subscriptionId))) {
            "The state identity or operation already belongs to another stored fact"
        } else null
    }

    private fun validateStateAction(current: SubscriptionFacts, target: SubscriptionStateChangeEntity, action: SubscriptionStateAction): String? {
        val today = clock.today()
        val input = snapshot(current.subscription, current.renewals, current.stateChanges)
        val status = SubscriptionCostEngine.evaluate(input, today).displayStatus
        val expectedState = when (action) {
            SubscriptionStateAction.PAUSE -> SubscriptionOperationState.PAUSED
            SubscriptionStateAction.RESUME, SubscriptionStateAction.REOPEN -> SubscriptionOperationState.ENABLED
            SubscriptionStateAction.STOP_AT_END, SubscriptionStateAction.STOP_EARLY -> SubscriptionOperationState.STOPPED
        }
        if (target.state != expectedState) return "The explicit action and its target state must agree"
        when (action) {
            SubscriptionStateAction.PAUSE -> {
                if (target.effectiveOn != today) return "Pause must use the business day of confirmation"
                if (status == SubscriptionDisplayStatus.STOPPED) return "A stopped subscription requires explicit reopening"
            }
            SubscriptionStateAction.RESUME, SubscriptionStateAction.REOPEN -> {
                if (target.effectiveOn != today) return "Resume or reopen must use the business day of confirmation"
                if (action == SubscriptionStateAction.RESUME && status != SubscriptionDisplayStatus.PAUSED) return "Resume requires a paused subscription"
                if (action == SubscriptionStateAction.REOPEN && status != SubscriptionDisplayStatus.STOPPED) return "Reopen requires a stopped subscription"
                if (current.renewals.none { !it.paidOn.isAfter(today) && it.paidUntilExclusive.isAfter(today) }) {
                    return "Without current or future paid coverage, confirm a real renewal together with enabling"
                }
            }
            SubscriptionStateAction.STOP_AT_END, SubscriptionStateAction.STOP_EARLY -> {
                val lastPaidBoundary = current.renewals.maxOf { it.paidUntilExclusive }
                val defaultBoundary = maxOf(today, lastPaidBoundary)
                if (action == SubscriptionStateAction.STOP_AT_END && target.effectiveOn != defaultBoundary) return "Default stop must use today's fixed last confirmed paid boundary"
                if (action == SubscriptionStateAction.STOP_EARLY && (target.effectiveOn.isBefore(today) || !target.effectiveOn.isBefore(defaultBoundary))) {
                    return "Early stop must be today or a future day before the default paid boundary"
                }
            }
        }
        return null
    }

    private fun validateLifecycleCandidate(facts: SubscriptionFacts): String? = try {
        val input = SubscriptionSnapshot(Subscription(facts.subscription.id), facts.renewals.map(::toRenewal),
            facts.stateChanges.map { SubscriptionStateChange(it.effectiveOn, it.state) })
        validateCandidate(input)
    } catch (invalid: IllegalArgumentException) {
        invalid.message ?: "The corrected facts are invalid"
    }

    /** Only expanded coverage after a pending stop conflicts; existing truncated coverage is kept. */
    private fun paymentStopConflict(facts: SubscriptionFacts, payment: SubscriptionRenewalEntity, old: SubscriptionRenewalEntity? = null): String? {
        for (stop in facts.stateChanges.filter { it.state == SubscriptionOperationState.STOPPED && it.effectiveOn.isAfter(clock.today()) }) {
            val proposedFrom = maxOf(payment.effectiveFrom, stop.effectiveOn)
            if (!proposedFrom.isBefore(payment.paidUntilExclusive)) continue
            if (old == null) return "New coverage conflicts with a fixed scheduled stop; explicitly correct or cancel the stop first"
            val oldFrom = maxOf(old.effectiveFrom, stop.effectiveOn)
            if (!oldFrom.isBefore(old.paidUntilExclusive) || proposedFrom.isBefore(oldFrom) || payment.paidUntilExclusive.isAfter(old.paidUntilExclusive)) {
                return "Coverage after a fixed stop cannot expand beyond the loaded payment; explicitly correct or cancel the stop first"
            }
        }
        return null
    }

    private fun savedState(target: SubscriptionStateChangeEntity, replayed: Boolean) =
        SubscriptionLifecycleResult.Saved(target.subscriptionId, stateId = target.id, replayed = replayed)
    private fun savedRenewal(target: SubscriptionRenewalEntity, replayed: Boolean) =
        SubscriptionLifecycleResult.Saved(target.subscriptionId, renewalId = target.id, replayed = replayed)
    private fun staleFacts() = SubscriptionLifecycleResult.Conflict("Subscription, payments or states changed after this editor was loaded")

    private suspend fun requireCommitted(candidate: SubscriptionFacts) {
        val stored = storedFacts(candidate.subscription.id)
        check(stored != null && sameFacts(stored, candidate)) { "The transaction did not store its exact checked target facts" }
    }

    private suspend fun lifecycleStorageOperation(block: suspend () -> SubscriptionLifecycleResult): SubscriptionLifecycleResult = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (constraint: SQLiteConstraintException) {
        SubscriptionLifecycleResult.Conflict(constraint.message ?: "A stored identity or relationship conflicts")
    } catch (full: SQLiteFullException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.DISK_FULL, full.message ?: "Storage is full")
    } catch (corrupt: SQLiteDatabaseCorruptException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.CORRUPT, corrupt.message ?: "The database is corrupt")
    } catch (database: SQLiteException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.DATABASE, database.message ?: "The database operation failed")
    } catch (invalid: InvalidStoredFacts) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored subscription facts are invalid")
    } catch (invalid: IllegalArgumentException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored metadata is invalid")
    } catch (invalid: DateTimeException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored dates are invalid")
    } catch (overflow: ArithmeticException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.CORRUPT, overflow.message ?: "Stored payment totals overflow")
    } catch (state: IllegalStateException) {
        SubscriptionLifecycleResult.StorageFailure(StorageFailureKind.DATABASE, state.message ?: "The database is unavailable")
    }

    private class InvalidStoredFacts(message: String) : RuntimeException(message)

    private suspend fun storageOperation(block: suspend () -> SubscriptionMutationResult): SubscriptionMutationResult = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (constraint: SQLiteConstraintException) {
        SubscriptionMutationResult.Conflict(constraint.message ?: "A stored identity or relationship conflicts")
    } catch (full: SQLiteFullException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.DISK_FULL, full.message ?: "Storage is full")
    } catch (corrupt: SQLiteDatabaseCorruptException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.CORRUPT, corrupt.message ?: "The database is corrupt")
    } catch (database: SQLiteException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.DATABASE, database.message ?: "The database operation failed")
    } catch (invalid: InvalidStoredFacts) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored subscription facts are invalid")
    } catch (invalid: IllegalArgumentException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored metadata is invalid")
    } catch (invalid: DateTimeException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.CORRUPT, invalid.message ?: "Stored dates are invalid")
    } catch (overflow: ArithmeticException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.CORRUPT, overflow.message ?: "Stored payment totals overflow")
    } catch (state: IllegalStateException) {
        SubscriptionMutationResult.StorageFailure(StorageFailureKind.DATABASE, state.message ?: "The database is unavailable")
    }
}
