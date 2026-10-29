package com.cooper.lithic.feature.review

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.ProjectTimelineData
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.data.repository.SubscriptionTimelineData
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.ProjectTimelineEngine
import com.cooper.lithic.domain.report.SubscriptionTimelineEngine
import com.cooper.lithic.domain.report.TimelineSampler
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.feature.report.ItemChartKind
import com.cooper.lithic.feature.report.ItemTimelineUiState
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

/** Actual VM-called UI transitions only; this does not run coroutine, Room or Android work. */
class M22TimelineReadStateTest {
    private val day = LocalDate.of(2026, 10, 23)
    private val first = day.minusDays(9)
    private val instant = Instant.parse("2026-10-23T04:00:00Z")

    @Test fun total_failed_refresh_keeps_read_warning_when_selecting_another_history_day() {
        assertTotalReadWarningRetained(day.minusDays(1))
    }

    @Test fun total_failed_refresh_keeps_read_warning_when_reselecting_the_same_day() {
        assertTotalReadWarningRetained(day)
    }

    @Test fun project_failed_refresh_keeps_read_warning_when_selecting_another_history_day() {
        assertItemReadWarningRetained(project(), day.minusDays(1))
    }

    @Test fun project_failed_refresh_keeps_read_warning_when_reselecting_the_same_day() {
        assertItemReadWarningRetained(project(), day)
    }

    @Test fun subscription_failed_refresh_keeps_read_warning_when_selecting_another_history_day() {
        assertItemReadWarningRetained(subscription(), day.minusDays(1))
    }

    @Test fun subscription_failed_refresh_keeps_read_warning_when_reselecting_the_same_day() {
        assertItemReadWarningRetained(subscription(), day)
    }

    @Test fun total_point_failure_can_be_retried_without_a_sticky_point_error() {
        val failedPoint = total().onPointFailed()
        assertNotNull(failedPoint.error)
        val retry = failedPoint.onSelectingDate(day)
        assertNull(retry.error)
        assertNull(retry.selectedPoint)
        assertEquals(day, retry.selectedDate)
    }

    @Test fun project_point_failure_can_be_retried_without_a_sticky_point_error() {
        assertItemPointRetry(project())
    }

    @Test fun subscription_point_failure_can_be_retried_without_a_sticky_point_error() {
        assertItemPointRetry(subscription())
    }

    @Test fun total_retrying_a_failed_point_keeps_the_unresolved_read_failure() {
        val failedRead = total().onReadFailed()
        val failedPoint = failedRead.onSelectingDate(day.minusDays(1)).onPointFailed()
        assertTrue(checkNotNull(failedPoint.error).contains(checkNotNull(failedRead.readError)))
        val retry = failedPoint.onSelectingDate(day)
        assertEquals(failedRead.error, retry.error)
        assertEquals(failedRead.readError, retry.readError)
        assertSame(failedRead.snapshot, retry.snapshot)
        assertEquals(TrendReadPhase.FAILED, totalTrendPresentation(retry).phase)
    }

    @Test fun project_retrying_a_failed_point_keeps_the_unresolved_read_failure() {
        assertItemReadFailureSurvivesPointRetry(project())
    }

    @Test fun subscription_retrying_a_failed_point_keeps_the_unresolved_read_failure() {
        assertItemReadFailureSurvivesPointRetry(subscription())
    }

    @Test fun total_fresh_read_replaces_the_retained_snapshot_and_clears_its_warning() {
        val failed = total().onReadFailed()
        val fresh = total(day.plusDays(1))
        val result = failed.onReadSucceeded(checkNotNull(fresh.snapshot), checkNotNull(fresh.timeline),
            checkNotNull(fresh.selectedDate), checkNotNull(fresh.selectedPoint), notice = null)
        assertNull(result.error)
        assertNull(result.readError)
        assertSame(fresh.snapshot, result.snapshot)
        assertSame(fresh.timeline, result.timeline)
        assertEquals(day.plusDays(1), result.timeline?.asOf)
        assertEquals(result.selectedDate, result.selectedPoint?.date)
        assertEquals(TrendReadPhase.READY, totalTrendPresentation(result).phase)
    }

    @Test fun project_fresh_read_replaces_the_retained_snapshot_and_clears_its_warning() {
        assertItemFreshRead(project(), project(day.plusDays(1)))
    }

    @Test fun subscription_fresh_read_replaces_the_retained_snapshot_and_clears_its_warning() {
        assertItemFreshRead(subscription(), subscription(day.plusDays(1)))
    }

    private fun assertTotalReadWarningRetained(date: LocalDate) {
        val failed = total().onReadFailed()
        assertNotNull(failed.error)
        val selected = failed.onSelectingDate(date)
        assertSame(failed.snapshot, selected.snapshot)
        assertSame(failed.timeline, selected.timeline)
        assertEquals(day, selected.timeline?.asOf)
        assertEquals(date, selected.selectedDate)
        assertNull(selected.selectedPoint)
        assertEquals("Selecting from the retained snapshot cannot make a failed read fresh", failed.error, selected.error)
        assertEquals(failed.readError, selected.readError)
        assertEquals(TrendReadPhase.FAILED, totalTrendPresentation(selected).phase)
    }

    private fun assertItemReadWarningRetained(ready: ItemTimelineUiState, date: LocalDate) {
        val failed = ready.onReadFailed()
        assertNotNull(failed.error)
        val selected = failed.onSelectingDate(date)
        assertSame(failed.project, selected.project)
        assertSame(failed.subscription, selected.subscription)
        assertEquals(ready.kind, selected.kind)
        assertEquals(date, selected.selectedDate)
        assertNull(selected.selectedProject)
        assertNull(selected.selectedSubscription)
        assertEquals("Selecting from the retained snapshot cannot make a failed read fresh", failed.error, selected.error)
        assertEquals(failed.readError, selected.readError)
    }

    private fun assertItemPointRetry(ready: ItemTimelineUiState) {
        val failedPoint = ready.onPointFailed()
        assertNotNull(failedPoint.error)
        val retry = failedPoint.onSelectingDate(day)
        assertNull(retry.error)
        assertNull(retry.selectedProject)
        assertNull(retry.selectedSubscription)
        assertEquals(day, retry.selectedDate)
    }

    private fun assertItemFreshRead(ready: ItemTimelineUiState, fresh: ItemTimelineUiState) {
        val failed = ready.onReadFailed()
        val result = failed.onReadSucceeded(fresh)
        assertNull(result.error)
        assertNull(result.readError)
        assertSame(fresh.project, result.project)
        assertSame(fresh.subscription, result.subscription)
        assertEquals(day.plusDays(1), result.project?.timeline?.asOf ?: result.subscription?.timeline?.asOf)
        assertEquals(result.selectedDate, result.selectedProject?.date ?: result.selectedSubscription?.date)
    }

    private fun assertItemReadFailureSurvivesPointRetry(ready: ItemTimelineUiState) {
        val failedRead = ready.onReadFailed()
        val failedPoint = failedRead.onSelectingDate(day.minusDays(1)).onPointFailed()
        assertTrue(checkNotNull(failedPoint.error).contains(checkNotNull(failedRead.readError)))
        val retry = failedPoint.onSelectingDate(day)
        assertEquals(failedRead.error, retry.error)
        assertEquals(failedRead.readError, retry.readError)
        assertSame(failedRead.project, retry.project)
        assertSame(failedRead.subscription, retry.subscription)
    }

    private fun total(asOf: LocalDate = day): TotalCostTrendUiState {
        val snapshot = LedgerSnapshot(OverviewSnapshot(
            listOf(OverviewCategory("project-category", "合成类别", OverviewKind.PROJECT)),
            listOf(OverviewProject("project", "project-category", projectSnapshot())), emptyList()))
        return TotalCostTrendUiState().onReadSucceeded(snapshot, TimelineSampler.sample(snapshot, asOf),
            asOf, CostTimelineEngine.point(snapshot, asOf), notice = null)
    }

    private fun project(asOf: LocalDate = day): ItemTimelineUiState {
        val category = CategoryEntity("10000000-0000-0000-0000-000000000001", CategoryType.PROJECT,
            "合成项目类别", "合成项目类别", false)
        val row = LongTermProjectEntity("20000000-0000-0000-0000-000000000001", "合成项目", category.id,
            first, createdAt = instant, updatedAt = instant)
        val fact = ProjectMoneyEventEntity("30000000-0000-0000-0000-000000000001", row.id,
            MoneyDirection.OUTLAY, 10_000, first, role = ProjectMoneyEventRole.INITIAL,
            operationId = "40000000-0000-0000-0000-000000000001")
        val snapshot = projectSnapshot()
        val detail = ProjectDetail(row, category, listOf(fact), ProjectCostEngine.evaluate(snapshot, asOf), asOf)
        val data = ProjectTimelineData(detail, snapshot, ProjectTimelineEngine.build(snapshot, asOf))
        return ItemTimelineUiState(kind = ItemChartKind.PROJECT, id = row.id, title = row.name,
            project = data, selectedDate = asOf, selectedProject = ProjectTimelineEngine.point(snapshot, asOf, asOf))
    }

    private fun subscription(asOf: LocalDate = day): ItemTimelineUiState {
        val category = CategoryEntity("10000000-0000-0000-0000-000000000002", CategoryType.SUBSCRIPTION,
            "合成订阅类别", "合成订阅类别", false)
        val row = SubscriptionEntity("20000000-0000-0000-0000-000000000002", "合成订阅", category.id,
            createdAt = instant, updatedAt = instant)
        val until = first.plusDays(30)
        val renewal = SubscriptionRenewalEntity("30000000-0000-0000-0000-000000000002", row.id,
            first, first, until, until, 3_000, BillingCycle.MONTHLY, 30,
            "40000000-0000-0000-0000-000000000002")
        val snapshot = SubscriptionSnapshot(Subscription(row.id), listOf(
            Renewal(Money(3_000), BillingCycle.MONTHLY, first, first, until)))
        val detail = SubscriptionDetail(row, category, listOf(renewal), emptyList(),
            SubscriptionCostEngine.evaluate(snapshot, asOf), renewal, asOf)
        val data = SubscriptionTimelineData(detail, snapshot, SubscriptionTimelineEngine.build(snapshot, asOf))
        return ItemTimelineUiState(kind = ItemChartKind.SUBSCRIPTION, id = row.id, title = row.name,
            subscription = data, selectedDate = asOf,
            selectedSubscription = SubscriptionTimelineEngine.point(snapshot, asOf, asOf))
    }

    private fun projectSnapshot() = ProjectSnapshot(LongTermProject(first),
        listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(10_000), first)))
}
