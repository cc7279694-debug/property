package com.cooper.lithic.data.repository

import androidx.room.withTransaction
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewEngine
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.TotalCostEvent
import com.cooper.lithic.domain.report.FinancialFact
import com.cooper.lithic.domain.report.FinancialFactKind
import com.cooper.lithic.domain.report.ReportRecordIdentity
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** A non-owning overview reader over the application's existing generation and coordinator. */
class ReportRepository(
    private val db: LithicDatabase,
    private val coordinator: LocalWriteCoordinator,
    private val clock: BusinessClock,
) {
    /** One observation day and one database snapshot for totals, counts and both category lists. */
    suspend fun loadOverview(asOf: LocalDate? = null): OverviewResult = coordinator.withSnapshot {
        val observedOn = asOf ?: clock.today()
        db.withTransaction {
                ReplacementStoredFacts.requireValid(db)
            check(db.projectDao().countOrphanEvents() == 0L) { "A money event refers to a missing project" }
            check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L) {
                "A payment or state refers to a missing subscription"
            }
            val categoryRows = db.categoryDao().list().associateBy { it.id }
            val categories = categoryRows.values.map {
                OverviewCategory(it.id, it.name, when (it.type) {
                    CategoryType.PROJECT -> OverviewKind.PROJECT
                    CategoryType.SUBSCRIPTION -> OverviewKind.SUBSCRIPTION
                })
            }
            var result = OverviewEngine.evaluate(OverviewSnapshot(categories, emptyList(), emptyList()), observedOn)
            val verifiedMedia = mutableSetOf<String>()
            val projectIds = mutableSetOf<String>()
            var projectCursor: ProjectCursor? = null
            while (true) {
                val rows = db.projectDao().pageProjects(projectCursor?.startedOn, projectCursor?.id, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { projectIds.add(it.id) }) { "A project identity was read twice" }
                val events = db.projectDao().eventsForProjects(rows.map { it.id }).groupBy { it.projectId }
                val inputs = rows.map { row ->
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.PROJECT, categoryRows, verifiedMedia)
                    val input = projectSnapshot(row, events[row.id].orEmpty())
                    if (input.moneyEvents.any { it.occurredOn.isAfter(observedOn) }) {
                        ProjectCostEngine.evaluate(input, LocalDate.MAX)
                    }
                    OverviewProject(row.id, row.categoryId, input)
                }
                result = OverviewEngine.combine(result,
                    OverviewEngine.evaluate(OverviewSnapshot(categories, inputs, emptyList()), observedOn))
                projectCursor = rows.last().let { ProjectCursor(it.startedOn, it.id) }
                if (rows.size < BATCH_SIZE) break
            }
            check(projectIds.size.toLong() == db.projectDao().countProjects()) { "The overview omitted project identities" }

            val subscriptionIds = mutableSetOf<String>()
            var afterSubscriptionId: String? = null
            while (true) {
                val rows = db.subscriptionDao().validationSubscriptions(afterSubscriptionId, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { subscriptionIds.add(it.id) }) { "A subscription identity was read twice" }
                val ids = rows.map { it.id }
                val renewals = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
                val states = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
                val inputs = rows.map { row ->
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.SUBSCRIPTION, categoryRows, verifiedMedia)
                    val input = subscriptionSnapshot(row, renewals[row.id].orEmpty(), states[row.id].orEmpty())
                    if (input.renewals.any { it.paidOn.isAfter(observedOn) }) {
                        SubscriptionCostEngine.evaluate(input, LocalDate.MAX)
                    }
                    OverviewSubscription(row.id, row.categoryId, input)
                }
                result = OverviewEngine.combine(result,
                    OverviewEngine.evaluate(OverviewSnapshot(categories, emptyList(), inputs), observedOn))
                afterSubscriptionId = rows.last().id
                if (rows.size < BATCH_SIZE) break
            }
            check(subscriptionIds.size.toLong() == db.subscriptionDao().countSubscriptions()) { "The overview omitted subscription identities" }
            result
        }
    }

    /** Complete M18 facts from one coordinator lease and one Room transaction. This method
     * deliberately performs no timeline sampling; the transaction ends before CPU work begins.
     * Existing identity, relation, money and payment validators are shared with the home reader. */
    suspend fun loadLedgerSnapshot(asOf: LocalDate = clock.today()): LedgerSnapshot = coordinator.withSnapshot {
        db.withTransaction {
            ReplacementStoredFacts.requireValid(db)
            check(db.projectDao().countOrphanEvents() == 0L) { "A money event refers to a missing project" }
            check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L) {
                "A payment or state refers to a missing subscription"
            }
            val categoryRows = db.categoryDao().list().associateBy { it.id }
            val categories = categoryRows.values.map {
                OverviewCategory(it.id, it.name, when (it.type) {
                    CategoryType.PROJECT -> OverviewKind.PROJECT
                    CategoryType.SUBSCRIPTION -> OverviewKind.SUBSCRIPTION
                })
            }
            val verifiedMedia = mutableSetOf<String>()
            val projects = mutableListOf<OverviewProject>()
            val subscriptions = mutableListOf<OverviewSubscription>()
            val events = mutableListOf<TotalCostEvent>()
            val financialFacts = mutableListOf<FinancialFact>()
            val identities = mutableListOf<ReportRecordIdentity>()
            val projectIds = mutableSetOf<String>()
            var cursor: ProjectCursor? = null
            while (true) {
                val rows = db.projectDao().pageProjects(cursor?.startedOn, cursor?.id, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { projectIds.add(it.id) }) { "A project identity was read twice" }
                val money = db.projectDao().eventsForProjects(rows.map { it.id }).groupBy { it.projectId }
                for (row in rows) {
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.PROJECT, categoryRows, verifiedMedia)
                    val facts = money[row.id].orEmpty()
                    val input = projectSnapshot(row, facts)
                    // Validation must include later real facts when observing an earlier day.
                    ProjectCostEngine.evaluate(input, if (input.moneyEvents.any { it.occurredOn.isAfter(asOf) }) LocalDate.MAX else asOf)
                    projects += OverviewProject(row.id, row.categoryId, input)
                    identities += ReportRecordIdentity(row.id, OverviewKind.PROJECT, row.name, row.categoryId)
                    events += TotalCostEvent("project:${row.id}:start", row.startedOn, OverviewKind.PROJECT,
                        row.id, row.name, "开始拥有", "取得日；金额以各条已保存的资金事件为准。")
                    row.endedOn?.let { ended -> events += TotalCostEvent("project:${row.id}:end", ended,
                        OverviewKind.PROJECT, row.id, row.name, "结束持有", "该日日末退出总额；后续真实结算不重新加入当前贡献。") }
                    for (fact in facts) {
                        financialFacts += FinancialFact("project-money:${fact.id}", row.id,
                            if (fact.direction == MoneyDirection.OUTLAY) FinancialFactKind.PROJECT_OUTLAY else FinancialFactKind.PROJECT_RECOVERY,
                            fact.occurredOn, fact.amountCents, fact.basis == MoneyBasis.TRADE_IN_CREDIT,
                            fact.operationId, fact.exchangeGroupId)
                        val role = when (fact.role) {
                            ProjectMoneyEventRole.INITIAL -> "初始投入"
                            ProjectMoneyEventRole.REGULAR -> "后续资金事件"
                            ProjectMoneyEventRole.END_SETTLEMENT -> "结束结算"
                        }
                        events += TotalCostEvent("money:${fact.id}", fact.occurredOn, OverviewKind.PROJECT,
                            row.id, row.name, if (fact.direction == MoneyDirection.OUTLAY) "投入" else "收回",
                            listOfNotNull(role, fact.description?.takeIf(String::isNotBlank)).joinToString("；"),
                            fact.amountCents, fact.basis == MoneyBasis.TRADE_IN_CREDIT)
                    }
                }
                cursor = rows.last().let { ProjectCursor(it.startedOn, it.id) }
                if (rows.size < BATCH_SIZE) break
            }
            check(projectIds.size.toLong() == db.projectDao().countProjects()) { "The ledger omitted project identities" }
            val subscriptionIds = mutableSetOf<String>()
            var afterId: String? = null
            while (true) {
                val rows = db.subscriptionDao().validationSubscriptions(afterId, BATCH_SIZE)
                if (rows.isEmpty()) break
                check(rows.all { subscriptionIds.add(it.id) }) { "A subscription identity was read twice" }
                val ids = rows.map { it.id }
                val paid = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
                val changes = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
                for (row in rows) {
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.SUBSCRIPTION, categoryRows, verifiedMedia)
                    val payments = paid[row.id].orEmpty()
                    val states = changes[row.id].orEmpty()
                    val input = subscriptionSnapshot(row, payments, states)
                    SubscriptionCostEngine.evaluate(input, if (input.renewals.any { it.paidOn.isAfter(asOf) }) LocalDate.MAX else asOf)
                    subscriptions += OverviewSubscription(row.id, row.categoryId, input)
                    identities += ReportRecordIdentity(row.id, OverviewKind.SUBSCRIPTION, row.name, row.categoryId)
                    for (payment in payments) {
                        financialFacts += FinancialFact("subscription-payment:${payment.id}", row.id,
                            FinancialFactKind.SUBSCRIPTION_PAYMENT, payment.paidOn, payment.amountCents,
                            operationId = payment.operationId)
                        val cycle = when (payment.cycle) {
                            com.cooper.lithic.domain.subscription.BillingCycle.MONTHLY -> "月付"
                            com.cooper.lithic.domain.subscription.BillingCycle.QUARTERLY -> "季付"
                            com.cooper.lithic.domain.subscription.BillingCycle.YEARLY -> "年付"
                        }
                        events += TotalCostEvent("payment:${payment.id}:paid", payment.paidOn, OverviewKind.SUBSCRIPTION,
                            row.id, row.name, "真实付款", "$cycle；折算${payment.standardDays}天；覆盖[${payment.effectiveFrom}, ${payment.paidUntilExclusive})。", payment.amountCents)
                        events += TotalCostEvent("payment:${payment.id}:from", payment.effectiveFrom, OverviewKind.SUBSCRIPTION,
                            row.id, row.name, "付费覆盖生效", "按该期已存周期和标准天数计算；不是另一笔付款。")
                        events += TotalCostEvent("payment:${payment.id}:until", payment.paidUntilExclusive, OverviewKind.SUBSCRIPTION,
                            row.id, row.name, "付费覆盖结束", "排他边界；该日不属于上一期，不自动产生付款。")
                    }
                    for (change in states) {
                        val label = when (change.state) {
                            com.cooper.lithic.domain.subscription.SubscriptionOperationState.ENABLED -> "启用"
                            com.cooper.lithic.domain.subscription.SubscriptionOperationState.PAUSED -> "暂停"
                            com.cooper.lithic.domain.subscription.SubscriptionOperationState.STOPPED -> "停止"
                        }
                        events += TotalCostEvent("state:${change.id}", change.effectiveOn, OverviewKind.SUBSCRIPTION,
                            row.id, row.name, "状态变更：$label",
                            listOfNotNull("以实际生效日为准；不产生付款。", change.reason?.takeIf(String::isNotBlank),
                                change.note?.takeIf(String::isNotBlank)).joinToString("；"))
                    }
                }
                afterId = rows.last().id
                if (rows.size < BATCH_SIZE) break
            }
            check(subscriptionIds.size.toLong() == db.subscriptionDao().countSubscriptions()) { "The ledger omitted subscription identities" }
            LedgerSnapshot(OverviewSnapshot(categories.toList(), projects.toList(), subscriptions.toList()),
                events.sortedWith(compareBy<TotalCostEvent> { it.date }.thenBy { it.id }),
                financialFacts.sortedWith(compareBy<FinancialFact> { it.date }.thenBy { it.id }),
                identities.sortedWith(compareBy<ReportRecordIdentity> { it.kind.ordinal }.thenBy { it.id }))
        }
    }

    /** Includes the first read and fact/category edits that leave identity counts unchanged. */
    val changes: Flow<Unit> = db.invalidationTracker.createFlow(
        "long_term_projects", "project_money_events", "subscriptions", "subscription_renewals",
        "subscription_state_changes", "categories", "local_media_assets", "replacements",
    ).map { Unit }

    private suspend fun requireIdentity(
        id: String,
        name: String,
        categoryId: String,
        coverId: String?,
        createdAt: Instant,
        updatedAt: Instant,
        type: CategoryType,
        categories: Map<String, CategoryEntity>,
        verifiedMedia: MutableSet<String>,
    ) {
        listOf(id, categoryId).plus(listOfNotNull(coverId)).forEach(StoredValueValidation::requireUuid)
        check(name.isNotBlank() && !updatedAt.isBefore(createdAt)) { "A stored business identity is invalid" }
        check(categories[categoryId]?.type == type) { "A stored business category is missing or has the wrong type" }
        if (coverId != null && coverId !in verifiedMedia) {
            checkNotNull(db.mediaDao().find(coverId)) { "The referenced cover metadata is missing" }
            verifiedMedia.add(coverId)
        }
    }

    private fun projectSnapshot(row: LongTermProjectEntity, events: List<ProjectMoneyEventEntity>): ProjectSnapshot {
        check((row.endedOn == null) == (row.endReason == null) && (row.endReason == null || row.endReason.isNotBlank())) {
            "The stored project end facts are invalid"
        }
        check(events.count { it.role == ProjectMoneyEventRole.INITIAL } == 1) { "A project must retain its single initial outlay" }
        check(events.count { it.role == ProjectMoneyEventRole.END_SETTLEMENT } <= 1) { "Legacy end settlements are ambiguous" }
        val input = LongTermProject(row.startedOn, row.endedOn)
        val facts = events.map { event ->
            listOf(event.id, event.projectId, event.operationId).forEach(StoredValueValidation::requireUuid)
            check(event.projectId == row.id) { "A money event belongs to another project" }
            when (event.role) {
                ProjectMoneyEventRole.INITIAL -> check(event.direction == MoneyDirection.OUTLAY) { "An initial event is not an outlay" }
                ProjectMoneyEventRole.END_SETTLEMENT -> check(row.endedOn != null && event.direction == MoneyDirection.RECOVERY) {
                    "An end settlement lacks its ended project or recovery direction"
                }
                ProjectMoneyEventRole.REGULAR -> Unit
            }
            ProjectMoneyEvent(event.direction, Money(event.amountCents), event.occurredOn)
        }
        return ProjectSnapshot(input, facts)
    }

    /** Notification dispatch already holds this generation's non-reentrant coordinator.
     * Reuses the same raw identity/payment validators; no displayed text or first-page limit. */
    internal suspend fun loadRenewalReminderSourcesWithinBoundary(expected: LocalWriteCoordinator):
        List<com.cooper.lithic.domain.reminder.RenewalReminderSource> {
        check(expected === coordinator)
        return db.withTransaction {
            check(db.subscriptionDao().countOrphanRenewals() == 0L && db.subscriptionDao().countOrphanStates() == 0L)
            val categories = db.categoryDao().list().associateBy { it.id }
            val verifiedMedia = mutableSetOf<String>()
            val result = mutableListOf<com.cooper.lithic.domain.reminder.RenewalReminderSource>()
            var after: String? = null
            do {
                val rows = db.subscriptionDao().validationSubscriptions(after, BATCH_SIZE)
                val ids = rows.map { it.id }
                val paid = db.subscriptionDao().renewalsForSubscriptions(ids).groupBy { it.subscriptionId }
                val states = db.subscriptionDao().statesForSubscriptions(ids).groupBy { it.subscriptionId }
                for (row in rows) {
                    requireIdentity(row.id, row.name, row.categoryId, row.coverAssetId, row.createdAt, row.updatedAt,
                        CategoryType.SUBSCRIPTION, categories, verifiedMedia)
                    val raw = paid[row.id].orEmpty()
                    val facts = subscriptionSnapshot(row, raw, states[row.id].orEmpty())
                    SubscriptionCostEngine.evaluate(facts, LocalDate.MAX)
                    result += com.cooper.lithic.domain.reminder.RenewalReminderSource(row.id, row.name,
                        raw.zip(facts.renewals).map { (stored, value) ->
                            com.cooper.lithic.domain.reminder.ReminderRenewal(stored.id, value)
                        }, facts.stateChanges)
                }
                after = rows.lastOrNull()?.id
            } while (rows.size == BATCH_SIZE)
            check(result.map { it.ownerId }.toSet().size == result.size)
            check(result.size.toLong() == db.subscriptionDao().countSubscriptions())
            result
        }
    }

    private fun subscriptionSnapshot(
        row: SubscriptionEntity,
        renewals: List<SubscriptionRenewalEntity>,
        states: List<SubscriptionStateChangeEntity>,
    ): SubscriptionSnapshot {
        check(renewals.isNotEmpty()) { "A stored subscription has no confirmed payment" }
        val paid = renewals.map { payment ->
            listOf(payment.id, payment.subscriptionId, payment.operationId).forEach(StoredValueValidation::requireUuid)
            check(payment.subscriptionId == row.id) { "A payment belongs to another subscription" }
            Renewal(Money(payment.amountCents), payment.cycle, payment.paidOn, payment.effectiveFrom,
                payment.paidUntilExclusive, payment.reminderDueOn, payment.standardDays)
        }
        val changes = states.map { state ->
            listOf(state.id, state.subscriptionId, state.operationId).forEach(StoredValueValidation::requireUuid)
            check(state.subscriptionId == row.id) { "A state belongs to another subscription" }
            SubscriptionStateChange(state.effectiveOn, state.state)
        }
        return SubscriptionSnapshot(Subscription(row.id), paid, changes)
    }

    private companion object { const val BATCH_SIZE = 50 }
}
