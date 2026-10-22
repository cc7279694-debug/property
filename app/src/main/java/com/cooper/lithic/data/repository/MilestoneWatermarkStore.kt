package com.cooper.lithic.data.repository

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MilestoneWatermarkEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import com.cooper.lithic.domain.reminder.MilestoneWatermark
import com.cooper.lithic.domain.reminder.ProjectMilestoneSource
import com.cooper.lithic.domain.reminder.ReminderRenewal
import com.cooper.lithic.domain.reminder.RenewalReminderSource
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.Instant
import java.time.LocalDate

internal data class MilestoneSources(
    val projects: List<ProjectMilestoneSource>,
    val subscriptions: List<RenewalReminderSource>,
)

/** Non-owning transaction helper. Caller holds this generation's coordinator and Room
 * transaction; this object never reacquires either boundary or publishes a receipt. */
internal object MilestoneWatermarkStore {
    suspend fun sourcesWithinTransaction(db: LithicDatabase): MilestoneSources {
        requireTransaction(db)
        ReplacementStoredFacts.requireValid(db)
        check(db.projectDao().countOrphanEvents() == 0L) { "A money event has no project" }
        check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L) {
            "A payment or state has no subscription"
        }
        val categories = db.categoryDao().list().associateBy { it.id }
        val projects = mutableListOf<ProjectMilestoneSource>()
        val projectIds = mutableSetOf<String>()
        var cursor: ProjectCursor? = null
        while (true) {
            val rows = db.projectDao().pageProjects(cursor?.startedOn, cursor?.id, BATCH_SIZE)
            if (rows.isEmpty()) break
            val events = db.projectDao().eventsForProjects(rows.map { it.id }).groupBy { it.projectId }
            for (row in rows) {
                check(projectIds.add(row.id)) { "A project was read twice" }
                projects += projectSource(db, row, events[row.id].orEmpty(), categories)
            }
            cursor = rows.last().let { ProjectCursor(it.startedOn, it.id) }
            if (rows.size < BATCH_SIZE) break
        }
        check(projects.size.toLong() == db.projectDao().countProjects()) { "Milestone read omitted project facts" }
        val subscriptions = mutableListOf<RenewalReminderSource>()
        val subscriptionIds = mutableSetOf<String>()
        var afterId: String? = null
        while (true) {
            val rows = db.subscriptionDao().validationSubscriptions(afterId, BATCH_SIZE)
            if (rows.isEmpty()) break
            val ids = rows.map { it.id }
            val payments = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
            val states = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
            for (row in rows) {
                check(subscriptionIds.add(row.id)) { "A subscription was read twice" }
                subscriptions += subscriptionSource(db, row, payments[row.id].orEmpty(), states[row.id].orEmpty(), categories)
            }
            afterId = rows.last().id
            if (rows.size < BATCH_SIZE) break
        }
        check(subscriptions.size.toLong() == db.subscriptionDao().countSubscriptions()) { "Milestone read omitted subscription facts" }
        return MilestoneSources(projects, subscriptions)
    }

    /** First admission only. Existing baselines are validated, never advanced by a routine read. */
    suspend fun initializeWithinTransaction(db: LithicDatabase, today: LocalDate): MilestoneSources {
        val sources = sourcesWithinTransaction(db)
        val identities = sources.projects.map { MilestoneOwnerType.PROJECT to it.ownerId }.toSet() +
            sources.subscriptions.map { MilestoneOwnerType.SUBSCRIPTION to it.ownerId }.toSet()
        val existing = db.milestoneWatermarkDao().list().map { row ->
            StoredValueValidation.requireUuid(row.ownerId)
            row.domain()
        }
        check(existing.map { it.ownerType to it.ownerId }.toSet().size == existing.size) { "Duplicate milestone baselines" }
        check(existing.all { (it.ownerType to it.ownerId) in identities }) { "A milestone baseline has no typed owner" }
        val keys = existing.map { it.ownerType to it.ownerId }.toSet()
        for (source in sources.projects) if ((MilestoneOwnerType.PROJECT to source.ownerId) !in keys) {
            put(db, MilestonePlanner.projectWatermark(source, today))
        }
        for (source in sources.subscriptions) if ((MilestoneOwnerType.SUBSCRIPTION to source.ownerId) !in keys) {
            put(db, MilestonePlanner.subscriptionWatermark(source, today))
        }
        return sources
    }

    suspend fun projectCreatedWithinTransaction(db: LithicDatabase, id: String, today: LocalDate) {
        requireTransaction(db)
        check(db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, id) == null) { "A new project already has a baseline" }
        projectChangedWithinTransaction(db, id, today)
    }

    suspend fun projectChangedWithinTransaction(db: LithicDatabase, id: String, today: LocalDate) {
        requireTransaction(db)
        StoredValueValidation.requireUuid(id)
        val row = checkNotNull(db.projectDao().findProject(id)) { "The milestone project is missing" }
        val source = projectSource(db, row, db.projectDao().eventsForProjects(listOf(id)), db.categoryDao().list().associateBy { it.id })
        val previous = db.milestoneWatermarkDao().find(MilestoneOwnerType.PROJECT, id)?.domain()
        put(db, MilestonePlanner.projectWatermark(source, today, previous))
    }

    suspend fun subscriptionCreatedWithinTransaction(db: LithicDatabase, id: String, today: LocalDate) {
        requireTransaction(db)
        check(db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, id) == null) { "A new subscription already has a baseline" }
        subscriptionChangedWithinTransaction(db, id, today)
    }

    suspend fun subscriptionChangedWithinTransaction(db: LithicDatabase, id: String, today: LocalDate) {
        requireTransaction(db)
        StoredValueValidation.requireUuid(id)
        val row = checkNotNull(db.subscriptionDao().findSubscription(id)) { "The milestone subscription is missing" }
        val source = subscriptionSource(db, row, db.subscriptionDao().renewalsForSubscriptions(listOf(id)),
            db.subscriptionDao().statesForSubscriptions(listOf(id)), db.categoryDao().list().associateBy { it.id })
        val previous = db.milestoneWatermarkDao().find(MilestoneOwnerType.SUBSCRIPTION, id)?.domain()
        put(db, MilestonePlanner.subscriptionWatermark(source, today, previous))
    }

    suspend fun deleteProjectWithinTransaction(db: LithicDatabase, id: String) {
        requireTransaction(db)
        StoredValueValidation.requireUuid(id)
        check(db.projectDao().findProject(id) == null) { "The project must first be deleted in this transaction" }
        db.milestoneWatermarkDao().delete(MilestoneOwnerType.PROJECT, id)
        db.notificationReceiptDao().deleteMilestones(MilestoneOwnerType.PROJECT, id)
    }

    private suspend fun put(db: LithicDatabase, watermark: MilestoneWatermark) = db.milestoneWatermarkDao().upsert(
        MilestoneWatermarkEntity(watermark.ownerType, watermark.ownerId, watermark.cutoffOn, watermark.suppressedMask),
    )

    private suspend fun requireIdentity(
        db: LithicDatabase, id: String, name: String, categoryId: String, coverId: String?,
        createdAt: Instant, updatedAt: Instant, type: CategoryType, categories: Map<String, CategoryEntity>,
    ) {
        listOf(id, categoryId).plus(listOfNotNull(coverId)).forEach(StoredValueValidation::requireUuid)
        check(name.isNotBlank() && !updatedAt.isBefore(createdAt)) { "Invalid milestone source identity" }
        check(categories[categoryId]?.type == type) { "Missing or wrong-type milestone source category" }
        if (coverId != null) checkNotNull(db.mediaDao().find(coverId)) { "Missing milestone source cover metadata" }
    }

    private suspend fun projectSource(
        db: LithicDatabase, row: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>, categories: Map<String, CategoryEntity>,
    ): ProjectMilestoneSource {
        requireIdentity(db, row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt, CategoryType.PROJECT, categories)
        check((row.endedOn == null) == (row.endReason == null) && row.endReason?.isBlank() != true) { "Invalid project end facts" }
        check(events.count { it.role == ProjectMoneyEventRole.INITIAL } == 1 && events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT } <= 1) {
            "Invalid project initial or settlement facts"
        }
        check(events.map { it.id }.toSet().size == events.size && events.map { it.operationId }.toSet().size == events.size)
        val money = events.map { event ->
            listOf(event.id, event.projectId, event.operationId).forEach(StoredValueValidation::requireUuid)
            check(event.projectId == row.id) { "A money event belongs to another project" }
            when (event.role) {
                ProjectMoneyEventRole.INITIAL -> check(event.direction == MoneyDirection.OUTLAY)
                ProjectMoneyEventRole.END_SETTLEMENT -> check(row.endedOn != null && event.direction == MoneyDirection.RECOVERY)
                ProjectMoneyEventRole.REGULAR -> Unit
            }
            ProjectMoneyEvent(event.direction, Money(event.amountCents), event.occurredOn)
        }
        ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(row.startedOn, row.endedOn), money), LocalDate.MAX)
        return ProjectMilestoneSource(row.id, row.name, row.startedOn, row.endedOn)
    }

    private suspend fun subscriptionSource(
        db: LithicDatabase, row: SubscriptionEntity, payments: List<SubscriptionRenewalEntity>,
        states: List<SubscriptionStateChangeEntity>, categories: Map<String, CategoryEntity>,
    ): RenewalReminderSource {
        requireIdentity(db, row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt, CategoryType.SUBSCRIPTION, categories)
        check(payments.isNotEmpty()) { "A subscription must retain a real payment" }
        check(payments.map { it.id }.toSet().size == payments.size && payments.map { it.operationId }.toSet().size == payments.size)
        check(states.map { it.id }.toSet().size == states.size && states.map { it.operationId }.toSet().size == states.size)
        val renewals = payments.map { payment ->
            listOf(payment.id, payment.subscriptionId, payment.operationId).forEach(StoredValueValidation::requireUuid)
            check(payment.subscriptionId == row.id) { "A payment belongs to another subscription" }
            ReminderRenewal(payment.id, Renewal(Money(payment.amountCents), payment.cycle, payment.paidOn,
                payment.effectiveFrom, payment.paidUntilExclusive, payment.reminderDueOn, payment.standardDays))
        }
        val changes = states.map { state ->
            listOf(state.id, state.subscriptionId, state.operationId).forEach(StoredValueValidation::requireUuid)
            check(state.subscriptionId == row.id) { "A state belongs to another subscription" }
            SubscriptionStateChange(state.effectiveOn, state.state)
        }
        SubscriptionCostEngine.evaluate(SubscriptionSnapshot(Subscription(row.id), renewals.map { it.renewal }, changes), LocalDate.MAX)
        return RenewalReminderSource(row.id, row.name, renewals, changes)
    }

    private suspend fun requireTransaction(db: LithicDatabase) {
        check(db.inTransaction()) { "Milestone facts and baselines require the caller's Room transaction" }
        // Cursor.getInt can narrow a malformed SQLite INTEGER before Entity.init sees it.
        // Validate raw column storage classes and bounds inside the same transaction.
        check(db.milestoneWatermarkDao().countMalformed() == 0L) { "Malformed stored milestone baseline" }
    }

    private const val BATCH_SIZE = 50
}
