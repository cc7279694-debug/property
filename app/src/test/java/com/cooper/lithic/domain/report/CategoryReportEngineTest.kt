package com.cooper.lithic.domain.report

import com.cooper.lithic.domain.report.FinancialFactKind.*
import com.cooper.lithic.domain.report.ReportEngineFixtures.pc
import com.cooper.lithic.domain.report.ReportEngineFixtures.sc
import com.cooper.lithic.domain.report.ReportEngineFixtures.today
import com.cooper.lithic.domain.report.ReportEngineFixtures.fact
import com.cooper.lithic.domain.report.ReportEngineFixtures.ledger
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class CategoryReportEngineTest {
    @Test fun current_category_rate_uses_frozen_overview_without_rounding() {
        val input = ledger(listOf(fact("one", 100), fact("two", 100, record = "other")))
        val expected = OverviewEngine.evaluate(input.overview, today).projectCategories.single().dailyRate
        assertEquals(0, expected.compareTo(CategoryReportEngine.evaluate(input, today).first { it.category.id == pc.id }.currentRate))
    }
    @Test fun category_changes_restate_history_using_current_stable_mapping() {
        val target = OverviewCategory("pc2", "新分类", OverviewKind.PROJECT)
        val facts = listOf(fact("one", 800_000), fact("late", 80_000, 2028))
        val old = ledger(facts, listOf(pc, target, sc))
        val changed = ledger(facts, listOf(pc, target.copy(name = "改名后"), sc), mapOf("p" to target.id))
        val oldTotal = CategoryReportEngine.evaluate(old, today).first { it.category.id == pc.id }
        val actual = CategoryReportEngine.evaluate(changed, today)
        assertEquals(880_000, oldTotal.historical.projectOutlayCents)
        assertFalse(actual.first { it.category.id == pc.id }.historical.hasRecords)
        val moved = actual.first { it.category.id == target.id }
        assertEquals("改名后", moved.category.name); assertEquals(880_000, moved.historical.projectOutlayCents)
        assertEquals(listOf(2028, 2026), moved.annual.map { it.year }); assertEquals("p", moved.records.single().id)
        assertEquals(facts, changed.financialFacts)
    }
    @Test fun historical_only_category_keeps_finished_records_and_zero_current_contribution() {
        val input = ledger(listOf(fact("one", 100)), endByProject = mapOf("p" to LocalDate.of(2027, 1, 1)))
        val actual = CategoryReportEngine.evaluate(input, today).first { it.category.id == pc.id }
        assertEquals(0, BigDecimal.ZERO.compareTo(actual.currentRate))
        assertEquals(100, actual.historical.projectOutlayCents); assertEquals(ReportRecordStatus.ENDED, actual.records.single().status)
    }
    @Test fun project_and_subscription_categories_keep_distinct_amount_semantics() {
        val input = ledger(listOf(fact("one", 100), fact("paid", 365_000, kind = SUBSCRIPTION_PAYMENT)))
        val actual = CategoryReportEngine.evaluate(input, today)
        val project = actual.first { it.category.kind == OverviewKind.PROJECT }
        val subscription = actual.first { it.category.kind == OverviewKind.SUBSCRIPTION }
        assertEquals(100, project.historical.projectOutlayCents); assertEquals(0, project.historical.subscriptionPaidCents)
        assertEquals(365_000, subscription.historical.subscriptionPaidCents); assertEquals(0, subscription.historical.projectFactCount)
    }
    @Test fun zero_record_and_empty_category_are_distinct_and_record_order_is_stable() {
        val input = ledger(listOf(fact("z", 0, record = "z"), fact("a", 0, record = "a")))
        val actual = CategoryReportEngine.evaluate(input, today)
        assertTrue(actual.first { it.category.id == pc.id }.historical.hasRecords)
        assertFalse(actual.first { it.category.id == sc.id }.historical.hasRecords)
        assertEquals(listOf("a", "z"), actual.first { it.category.id == pc.id }.records.map { it.id })
    }
    @Test fun not_started_project_is_listed_without_inventing_current_contribution() {
        val input = ledger(listOf(fact("prepaid", 100)))
        val result = CategoryReportEngine.evaluate(input, LocalDate.of(2025, 1, 1)).first { it.category.id == pc.id }
        assertEquals(ReportRecordStatus.NOT_STARTED, result.records.single().status)
        assertFalse(result.historical.hasRecords); assertEquals(0, result.currentRate.signum())
    }
    @Test fun corrupt_record_category_or_name_rejects_whole_report() {
        val good = ledger(listOf(fact("one", 100)))
        val invalid = good.copy(recordIdentities = good.recordIdentities.map { it.copy(name = " ") })
        assertThrows(IllegalArgumentException::class.java) { CategoryReportEngine.evaluate(invalid, today) }
    }
}
