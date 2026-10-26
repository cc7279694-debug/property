package com.cooper.lithic.ui

import com.cooper.lithic.core.model.*
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.subscription.*
import com.cooper.lithic.feature.project.ProjectListUiState
import com.cooper.lithic.feature.review.TotalCostTrendUiState
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
import java.time.Instant
import java.time.LocalDate

/** Immutable androidTest-only facts. No Room, storage owner, default ledger or user media is opened. */
internal class C2VisualFixtures(populated: Boolean) {
    val day: LocalDate = LocalDate.of(2026, 10, 26)
    private val start = day.minusDays(120)
    private val created = Instant.parse("2026-06-28T00:00:00Z")
    private val projectId = "94000000-0000-0000-0000-000000000001"
    private val subscriptionId = "94000000-0000-0000-0000-000000000002"
    private val projectCategory = CategoryEntity("94000000-0000-0000-0000-000000000003", CategoryType.PROJECT, "数码", "数码", false)
    private val subscriptionCategory = CategoryEntity("94000000-0000-0000-0000-000000000004", CategoryType.SUBSCRIPTION, "阅读", "阅读", false)
    private val projectFacts = ProjectSnapshot(LongTermProject(start), listOf(
        ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(128_800), start),
        ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(8_800), day.minusDays(20))))
    private val renewalFrom = day.withDayOfMonth(1)
    private val renewalUntil = renewalFrom.plusMonths(1)
    private val subscriptionFacts = SubscriptionSnapshot(Subscription(subscriptionId), listOf(
        Renewal(Money(1_800), BillingCycle.MONTHLY, renewalFrom, renewalFrom, renewalUntil)))
    val ledger: LedgerSnapshot = LedgerSnapshot(OverviewSnapshot(
        listOf(OverviewCategory(projectCategory.id, projectCategory.name, OverviewKind.PROJECT),
            OverviewCategory(subscriptionCategory.id, subscriptionCategory.name, OverviewKind.SUBSCRIPTION)),
        if (populated) listOf(OverviewProject(projectId, projectCategory.id, projectFacts)) else emptyList(),
        if (populated) listOf(OverviewSubscription(subscriptionId, subscriptionCategory.id, subscriptionFacts)) else emptyList()))
    val overview: OverviewResult = OverviewEngine.evaluate(ledger.overview, day)
    private val projectEvents = listOf(
        ProjectMoneyEventEntity("94000000-0000-0000-0000-000000000005", projectId, MoneyDirection.OUTLAY, 128_800, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = "94000000-0000-0000-0000-000000000006"),
        ProjectMoneyEventEntity("94000000-0000-0000-0000-000000000007", projectId, MoneyDirection.RECOVERY, 8_800, day.minusDays(20),
            role = ProjectMoneyEventRole.REGULAR, operationId = "94000000-0000-0000-0000-000000000008"))
    val projects = ProjectListUiState(items = if (populated) listOf(ProjectDetail(
        LongTermProjectEntity(projectId, "合成阅读器", projectCategory.id, start, createdAt = created, updatedAt = created),
        projectCategory, projectEvents, ProjectCostEngine.evaluate(projectFacts, day), day)) else emptyList(),
        totalCount = overview.projectCount, loading = false)
    private val renewal = SubscriptionRenewalEntity("94000000-0000-0000-0000-000000000009", subscriptionId,
        renewalFrom, renewalFrom, renewalUntil, renewalUntil, 1_800, BillingCycle.MONTHLY, 30,
        "94000000-0000-0000-0000-000000000010")
    val subscriptions = SubscriptionListUiState(items = if (populated) listOf(SubscriptionDetail(
        SubscriptionEntity(subscriptionId, "合成阅读服务", subscriptionCategory.id, createdAt = created, updatedAt = created),
        subscriptionCategory, listOf(renewal), emptyList(), SubscriptionCostEngine.evaluate(subscriptionFacts, day), renewal, day)) else emptyList(),
        totalCount = overview.subscriptionCount, loading = false)
    val trend = TotalCostTrendUiState(snapshot = ledger, timeline = TimelineSampler.sample(ledger, day),
        selectedDate = day, selectedPoint = CostTimelineEngine.point(ledger, day))
}
