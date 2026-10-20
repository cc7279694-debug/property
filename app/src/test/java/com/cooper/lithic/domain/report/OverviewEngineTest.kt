package com.cooper.lithic.domain.report

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewEngineTest {
    private val start = LocalDate.of(2026, 1, 1)
    private val asOf = start.plusDays(2)
    private val projectCategory = OverviewCategory("project-category", "设备", OverviewKind.PROJECT)
    private val subscriptionCategory = OverviewCategory("subscription-category", "服务", OverviewKind.SUBSCRIPTION)
    private val categories = listOf(projectCategory, subscriptionCategory)

    @Test fun no_records_preserves_observation_date_and_is_empty() {
        val result = OverviewEngine.evaluate(OverviewSnapshot(emptyList(), emptyList(), emptyList()), asOf)
        assertEquals(asOf, result.asOf)
        decimal("0", result.totalRate)
        assertEquals(0L, result.projectCount)
        assertEquals(0L, result.subscriptionCount)
        assertFalse(result.hasRecords)
        assertTrue(result.projectCategories.isEmpty())
        assertTrue(result.subscriptionCategories.isEmpty())
    }

    @Test fun no_zero_confused_with_empty() {
        val result = evaluate(projects = listOf(project(cents = 0)))
        assertTrue(result.hasRecords)
        decimal("0", result.totalRate)
        assertEquals(1L, result.projectCount)
        assertEquals(1L, result.activeProjectCount)
        assertEquals(1L, result.projectCategories.single().contributingCount)
    }

    @Test fun home_uses_same_engines_for_project_and_current_subscription() {
        val project = project(cents = 10_001)
        val subscription = subscription(cents = 17_001)
        val result = evaluate(listOf(project), listOf(subscription))
        assertEquals(ProjectCostEngine.evaluate(project.snapshot, asOf).dailyRate, result.projectRate)
        assertEquals(SubscriptionCostEngine.evaluate(subscription.snapshot, asOf).currentRate, result.subscriptionRate)
        assertEquals(result.projectRate.add(result.subscriptionRate), result.totalRate)
        assertEquals(1L, result.activeProjectCount)
        assertEquals(1L, result.activeSubscriptionCount)
    }

    @Test fun active_only_aggregate_keeps_all_record_counts_and_zero_categories() {
        val result = evaluate(
            projects = listOf(project("active", 30_000), project("ended", 90_000, endedOn = asOf),
                project("future", 90_000, startedOn = asOf.plusDays(1))),
            subscriptions = listOf(subscription("active", 15_000),
                subscription("paused", 90_000, state = SubscriptionOperationState.PAUSED),
                subscription("stopped", 90_000, state = SubscriptionOperationState.STOPPED),
                subscription("expired", 90_000, until = asOf),
                subscription("future", 90_000, from = asOf.plusDays(1))),
        )
        decimal("100", result.projectRate)
        decimal("5", result.subscriptionRate)
        decimal("105", result.totalRate)
        assertEquals(3L, result.projectCount)
        assertEquals(5L, result.subscriptionCount)
        assertEquals(1L, result.activeProjectCount)
        assertEquals(1L, result.activeSubscriptionCount)
        assertEquals(1L, result.projectCategories.single().contributingCount)
        assertEquals(1L, result.subscriptionCategories.single().contributingCount)
    }

    @Test fun inactive_only_records_are_not_empty_and_category_rows_remain() {
        val result = evaluate(listOf(project(endedOn = asOf)),
            listOf(subscription(state = SubscriptionOperationState.PAUSED)))
        assertTrue(result.hasRecords)
        decimal("0", result.totalRate)
        assertEquals(0L, result.activeProjectCount)
        assertEquals(0L, result.activeSubscriptionCount)
        assertEquals(0L, result.projectCategories.single().contributingCount)
        assertEquals(0L, result.subscriptionCategories.single().contributingCount)
    }

    @Test fun categories_without_records_are_kept_without_faking_records() {
        val result = evaluate()
        assertFalse(result.hasRecords)
        assertEquals(listOf(projectCategory.id), result.projectCategories.map { it.categoryId })
        assertEquals(listOf(subscriptionCategory.id), result.subscriptionCategories.map { it.categoryId })
        decimal("0", result.projectCategories.single().dailyRate)
        decimal("0", result.subscriptionCategories.single().dailyRate)
    }

    @Test fun negative_and_tiny_nonzero_project_rates_are_not_clamped_or_display_rounded() {
        val negative = project(cents = 0).let { it.copy(snapshot = it.snapshot.copy(moneyEvents = listOf(
            ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(1), start)))) }
        val result = evaluate(listOf(negative))
        decimal("-0.003333333333333333333333333333333333", result.totalRate)
        assertTrue(result.totalRate.signum() < 0)
        assertEquals(1L, result.activeProjectCount)
        decimal("0.003333333333333333333333333333333333", evaluate(listOf(project(cents = 1))).totalRate)
    }

    @Test fun positive_and_negative_rates_can_cancel_to_real_zero_without_hiding_records() {
        val negative = project("negative", 0).let { it.copy(snapshot = it.snapshot.copy(moneyEvents = listOf(
            ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(1), start)))) }
        val result = evaluate(listOf(project("positive", 1), negative))
        decimal("0", result.projectRate)
        decimal("0", result.totalRate)
        assertTrue(result.hasRecords)
        assertEquals(2L, result.activeProjectCount)
        assertEquals(2L, result.projectCategories.single().contributingCount)
    }

    @Test fun category_sum_matches_type_without_intermediate_decimal128_rounding() {
        val tinyCategory = OverviewCategory("tiny", "微小", OverviewKind.PROJECT)
        val large = project("large", Money.MAX_CENTS, startedOn = asOf)
        val tiny = project("tiny", 1).copy(categoryId = tinyCategory.id)
        val result = evaluate(listOf(large, tiny), listOf(subscription(cents = 1)), categories + tinyCategory)
        decimal("9999999999.993333333333333333333333333333333333", result.projectRate)
        assertEquals(result.projectRate, result.projectCategories.fold(BigDecimal.ZERO) { sum, item -> sum.add(item.dailyRate) })
        assertEquals(result.subscriptionRate, result.subscriptionCategories.fold(BigDecimal.ZERO) { sum, item -> sum.add(item.dailyRate) })
        assertEquals(result.projectRate.add(result.subscriptionRate), result.totalRate)
        assertEquals(result, evaluate(listOf(tiny, large), listOf(subscription(cents = 1)), (categories + tinyCategory).reversed()))
    }

    @Test fun categories_sort_by_name_then_id_independent_of_input_order() {
        val sortedCategories = listOf(OverviewCategory("b", "Same", OverviewKind.PROJECT),
            OverviewCategory("a", "Same", OverviewKind.PROJECT), OverviewCategory("c", "Alpha", OverviewKind.PROJECT),
            OverviewCategory("s-b", "Same", OverviewKind.SUBSCRIPTION), OverviewCategory("s-a", "Same", OverviewKind.SUBSCRIPTION))
        val result = evaluate(inputCategories = sortedCategories)
        assertEquals(listOf("c", "a", "b"), result.projectCategories.map { it.categoryId })
        assertEquals(listOf("s-a", "s-b"), result.subscriptionCategories.map { it.categoryId })
        assertEquals(result, evaluate(inputCategories = sortedCategories.reversed()))
    }

    @Test fun project_end_day_and_post_end_settlement_never_reenter_current_total() {
        val ended = project(cents = 30_000, endedOn = asOf).let { it.copy(snapshot = it.snapshot.copy(
            moneyEvents = it.snapshot.moneyEvents + ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(10_000), asOf.plusDays(1)))) }
        decimal("150", evaluate(listOf(ended), observation = asOf.minusDays(1)).projectRate)
        decimal("0", evaluate(listOf(ended)).projectRate)
        val settled = evaluate(listOf(ended), observation = asOf.plusDays(1))
        decimal("0", settled.projectRate)
        assertEquals(1L, settled.projectCount)
        assertEquals(0L, settled.activeProjectCount)
    }

    @Test fun prepaid_future_subscription_does_not_replace_current_price_before_boundary() {
        val first = subscription(cents = 15_000, until = asOf.plusDays(1))
        val prepaid = Renewal(Money(18_000), BillingCycle.MONTHLY, start, asOf.plusDays(1), start.plusMonths(2))
        val input = first.copy(snapshot = first.snapshot.copy(renewals = first.snapshot.renewals + prepaid))
        decimal("5", evaluate(subscriptions = listOf(input)).subscriptionRate)
        decimal("6", evaluate(subscriptions = listOf(input), observation = asOf.plusDays(1)).subscriptionRate)
        assertEquals(1L, evaluate(subscriptions = listOf(input)).subscriptionCount)
    }

    @Test fun payment_unknown_on_observation_day_has_no_current_contribution() {
        val service = subscription().let { it.copy(snapshot = it.snapshot.copy(renewals = it.snapshot.renewals.map { renewal ->
            renewal.copy(paidOn = asOf.plusDays(1)) })) }
        val result = evaluate(subscriptions = listOf(service))
        decimal("0", result.subscriptionRate)
        assertEquals(1L, result.subscriptionCount)
        assertEquals(0L, result.activeSubscriptionCount)
    }

    @Test fun records_in_different_types_may_share_the_same_identity() {
        val result = evaluate(listOf(project("shared", 30_000)), listOf(subscription("shared", 15_000)))
        decimal("105", result.totalRate)
        assertEquals(1L, result.projectCount)
        assertEquals(1L, result.subscriptionCount)
    }

    @Test fun missing_category_relationship_is_rejected_for_each_record_type() {
        invalid { evaluate(listOf(project().copy(categoryId = "missing"))) }
        invalid { evaluate(subscriptions = listOf(subscription().copy(categoryId = "missing"))) }
    }

    @Test fun category_of_wrong_type_is_rejected_for_each_record_type() {
        invalid { evaluate(listOf(project().copy(categoryId = subscriptionCategory.id))) }
        invalid { evaluate(subscriptions = listOf(subscription().copy(categoryId = projectCategory.id))) }
    }

    @Test fun duplicate_category_id_is_rejected_even_when_name_or_kind_differs() {
        invalid { evaluate(inputCategories = categories + projectCategory.copy(name = "Another")) }
        invalid { evaluate(inputCategories = categories + projectCategory.copy(kind = OverviewKind.SUBSCRIPTION)) }
    }

    @Test fun duplicate_identity_is_rejected_within_each_record_type() {
        invalid { evaluate(listOf(project(), project(cents = 500))) }
        invalid { evaluate(subscriptions = listOf(subscription(), subscription(cents = 500))) }
    }

    @Test fun subscription_wrapper_identity_must_match_its_frozen_snapshot() {
        invalid { evaluate(subscriptions = listOf(subscription().copy(id = "different"))) }
    }

    @Test fun malformed_frozen_subscription_snapshot_is_propagated_without_partial_total() {
        val service = subscription().let { it.copy(snapshot = it.snapshot.copy(renewals = it.snapshot.renewals + it.snapshot.renewals)) }
        invalid { evaluate(listOf(project()), listOf(service)) }
    }

    @Test fun blank_identity_category_relationship_and_category_name_are_rejected() {
        invalid { evaluate(listOf(project().copy(id = " "))) }
        invalid { evaluate(subscriptions = listOf(subscription().copy(id = " ", snapshot = SubscriptionSnapshot(Subscription(" "))))) }
        invalid { evaluate(inputCategories = listOf(projectCategory.copy(id = " "))) }
        invalid { evaluate(inputCategories = listOf(projectCategory.copy(name = " "))) }
    }

    @Test fun combine_preserves_exact_rates_merges_categories_and_adds_all_counts() {
        val secondCategory = OverviewCategory("second", "Another", OverviewKind.PROJECT)
        val first = evaluate(listOf(project("large", Money.MAX_CENTS, startedOn = asOf)),
            listOf(subscription("subscription-first", cents = 1)), categories + secondCategory)
        val second = evaluate(listOf(project("tiny", 1), project("second", 0).copy(categoryId = secondCategory.id)),
            listOf(subscription("subscription-second", state = SubscriptionOperationState.PAUSED)), categories + secondCategory)
        val result = OverviewEngine.combine(first, second)
        decimal("9999999999.993333333333333333333333333333333333", result.projectRate)
        assertEquals(first.subscriptionRate.add(second.subscriptionRate), result.subscriptionRate)
        assertEquals(result.projectRate.add(result.subscriptionRate), result.totalRate)
        assertEquals(3L, result.projectCount)
        assertEquals(2L, result.subscriptionCount)
        assertEquals(3L, result.activeProjectCount)
        assertEquals(1L, result.activeSubscriptionCount)
        assertEquals(listOf("second", projectCategory.id), result.projectCategories.map { it.categoryId })
        assertEquals(2L, result.projectCategories.last().contributingCount)
        assertEquals(result.projectRate, result.projectCategories.fold(BigDecimal.ZERO) { sum, category -> sum.add(category.dailyRate) })
        assertEquals(result, OverviewEngine.combine(second, first))
        assertEquals(result, OverviewEngine.combine(result, OverviewEngine.evaluate(OverviewSnapshot(emptyList(), emptyList(), emptyList()), asOf)))
    }

    @Test fun combine_rejects_different_dates_conflicting_names_and_count_overflow() {
        val first = evaluate(listOf(project()), listOf(subscription()))
        invalid { OverviewEngine.combine(first, first.copy(asOf = asOf.plusDays(1))) }
        invalid { OverviewEngine.combine(first, first.copy(projectCategories = first.projectCategories.map { it.copy(name = "Other") })) }
        invalid { OverviewEngine.combine(first, first.copy(subscriptionCategories = first.subscriptionCategories.map { it.copy(name = "Other") })) }
        assertThrows(ArithmeticException::class.java) { OverviewEngine.combine(first.copy(projectCount = Long.MAX_VALUE), first) }
        assertThrows(ArithmeticException::class.java) { OverviewEngine.combine(first.copy(subscriptionCount = Long.MAX_VALUE), first) }
        assertThrows(ArithmeticException::class.java) { OverviewEngine.combine(first.copy(activeProjectCount = Long.MAX_VALUE), first) }
        assertThrows(ArithmeticException::class.java) { OverviewEngine.combine(first.copy(activeSubscriptionCount = Long.MAX_VALUE), first) }
        val overflowCategory = first.projectCategories.map { it.copy(contributingCount = Long.MAX_VALUE) }
        assertThrows(ArithmeticException::class.java) { OverviewEngine.combine(first.copy(projectCategories = overflowCategory), first) }
    }

    private fun evaluate(
        projects: List<OverviewProject> = emptyList(),
        subscriptions: List<OverviewSubscription> = emptyList(),
        inputCategories: List<OverviewCategory> = categories,
        observation: LocalDate = asOf,
    ) = OverviewEngine.evaluate(OverviewSnapshot(inputCategories, projects, subscriptions), observation)

    private fun project(
        id: String = "project", cents: Long = 30_000,
        startedOn: LocalDate = start, endedOn: LocalDate? = null,
    ) = OverviewProject(id, projectCategory.id, ProjectSnapshot(LongTermProject(startedOn, endedOn),
        listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(cents), start))))

    private fun subscription(
        id: String = "subscription", cents: Long = 15_000,
        from: LocalDate = start, until: LocalDate = start.plusMonths(1),
        state: SubscriptionOperationState? = null,
    ) = OverviewSubscription(id, subscriptionCategory.id, SubscriptionSnapshot(Subscription(id),
        listOf(Renewal(Money(cents), BillingCycle.MONTHLY, start, from, until)),
        state?.let { listOf(SubscriptionStateChange(start, it)) } ?: emptyList()))

    private fun decimal(expected: String, actual: BigDecimal) = assertEquals(0, BigDecimal(expected).compareTo(actual))
    private fun invalid(block: () -> Unit) { assertThrows(IllegalArgumentException::class.java, block) }
}
