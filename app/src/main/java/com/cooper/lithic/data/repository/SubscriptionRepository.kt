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
    open suspend fun createWithRenewal(command: CreateSubscriptionCommand): SubscriptionMutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val identity = command.subscription
                val renewal = command.initialRenewal
                validateCommandIdentity(command.commandId, renewal, identity)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                val previous = db.subscriptionDao().findRenewalByOperation(command.commandId)
                if (previous != null) {
                    val existingIdentity = db.subscriptionDao().findSubscription(previous.subscriptionId)
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
                SubscriptionMutationResult.Success(identity.id, renewal.id, false)
            }
        }
    }

    open suspend fun confirmRenewal(command: ConfirmRenewalCommand): SubscriptionMutationResult = coordinator.withWrite {
        storageOperation {
            db.withTransaction {
                val renewal = command.renewal
                validateCommandIdentity(command.commandId, renewal)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                val previous = db.subscriptionDao().findRenewalByOperation(command.commandId)
                if (previous != null) return@withTransaction if (previous == renewal) {
                    SubscriptionMutationResult.Success(previous.subscriptionId, previous.id, true)
                } else SubscriptionMutationResult.Conflict("The operation was already used for different contents")
                val identity = db.subscriptionDao().findSubscription(renewal.subscriptionId)
                    ?: return@withTransaction SubscriptionMutationResult.ValidationFailure("The referenced subscription does not exist")
                if (db.subscriptionDao().findRenewal(renewal.id) != null) return@withTransaction SubscriptionMutationResult.Conflict("The renewal identity already belongs to another fact")
                val category = db.categoryDao().find(identity.categoryId) ?: throw InvalidStoredFacts("The subscription category is missing")
                requireStoredIdentity(identity, category)
                val renewals = db.subscriptionDao().renewalsForSubscriptions(listOf(identity.id))
                val states = db.subscriptionDao().statesForSubscriptions(listOf(identity.id))
                val existing = snapshot(identity, renewals, states)
                // Stored corruption/overflow is distinct from a rejected new candidate.
                SubscriptionCostEngine.evaluate(existing, clock.today())
                validatePayment(renewal)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                val candidate = existing.copy(renewals = existing.renewals + toRenewal(renewal))
                validateCandidate(candidate)?.let { return@withTransaction SubscriptionMutationResult.ValidationFailure(it) }
                db.subscriptionDao().insertRenewal(renewal)
                SubscriptionMutationResult.Success(identity.id, renewal.id, false)
            }
        }
    }

    /** Null is only a successful query proving the identity is absent; read failures propagate. */
    open suspend fun loadSubscriptionDetail(id: String, asOf: LocalDate = clock.today()): SubscriptionDetail? {
        StoredValueValidation.requireUuid(id)
        return coordinator.withSnapshot {
            db.withTransaction {
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
                requirePaidIdentities()
                pageInTransaction(cursor, pageSize, asOf, db.subscriptionDao().countSubscriptions())
            }
        }
    }

    /** Rebuild all loaded pages inside one snapshot when a payment changes the recent-sort key. */
    open suspend fun loadSubscriptionWindow(pageCount: Int, asOf: LocalDate = clock.today()): SubscriptionPage {
        require(pageCount > 0) { "A subscription window must contain at least one page" }
        return coordinator.withSnapshot {
            db.withTransaction {
                requirePaidIdentities()
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
        db.withTransaction { db.subscriptionDao().subscriptionCategories() }
    }
    open suspend fun loadSubscriptionCount(): Long = coordinator.withSnapshot {
        db.withTransaction { requirePaidIdentities(); db.subscriptionDao().countSubscriptions() }
    }
    open val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "subscriptions", "subscription_renewals", "subscription_state_changes", "categories",
    ).map { Unit }

    private suspend fun requirePaidIdentities() {
        if (db.subscriptionDao().countSubscriptionsWithoutRenewals() != 0L) throw InvalidStoredFacts("A stored subscription has no confirmed payment")
    }

    private suspend fun pageInTransaction(cursor: SubscriptionCursor?, size: Int, asOf: LocalDate, count: Long): SubscriptionPage {
        val dao = db.subscriptionDao()
        val queried = dao.pageSubscriptionKeys(cursor?.lastPaidOn, cursor?.id, size + 1)
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
