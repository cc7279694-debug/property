package com.cooper.lithic.feature.category

import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.report.ReportEngineFixtures.fact
import com.cooper.lithic.domain.report.ReportEngineFixtures.ledger
import com.cooper.lithic.domain.report.ReportEngineFixtures.pc
import com.cooper.lithic.domain.report.ReportEngineFixtures.sc
import com.cooper.lithic.domain.report.ReportEngineFixtures.today
import com.cooper.lithic.feature.review.ReportAnalysisUiState
import org.junit.Assert.*
import org.junit.Test

/** Presentation only: the existing engine supplies every amount, date and record. */
class SingleCategoryPresentationTest {
    @Test fun the_only_category_of_the_current_type_is_shown_without_a_selection() {
        val state = ready()
        val project = state.categories.single { it.category.kind == OverviewKind.PROJECT }
        val subscription = state.categories.single { it.category.kind == OverviewKind.SUBSCRIPTION }
        assertSame(project, categoryAnalysisSelection(state))
        assertSame(subscription, categoryAnalysisSelection(state.copy(selectedKind = OverviewKind.SUBSCRIPTION)))
        assertNull(state.selectedCategoryId)
    }

    @Test fun multiple_categories_and_no_categories_do_not_invent_a_default() {
        val state = ready()
        val project = state.categories.single { it.category.kind == OverviewKind.PROJECT }
        val extra = project.copy(category = pc.copy(id = "pc-extra", name = "另一个类别"))
        assertNull(categoryAnalysisSelection(state.copy(categories = state.categories + extra)))
        assertNull(categoryAnalysisSelection(state.copy(categories = state.categories.filter { it.category.kind == OverviewKind.SUBSCRIPTION })))
        assertNull(categoryAnalysisSelection(state.copy(categories = emptyList())))
    }

    @Test fun an_explicit_selection_is_retained_and_an_invalid_explicit_id_is_not_replaced() {
        val state = ready()
        val project = state.categories.single { it.category.kind == OverviewKind.PROJECT }
        val extra = project.copy(category = pc.copy(id = "pc-extra", name = "主动选择"))
        val chosen = state.copy(categories = state.categories + extra, selectedCategoryId = extra.category.id)
        assertSame(extra, categoryAnalysisSelection(chosen))
        assertSame(extra, categoryAnalysisSelection(chosen.copy(loading = true, error = "合成刷新失败")))
        assertNull(categoryAnalysisSelection(state.copy(selectedCategoryId = "removed-category")))
        assertNull(categoryAnalysisSelection(state.copy(selectedCategoryId = sc.id)))
    }

    @Test fun incomplete_or_unresolved_states_cannot_auto_select() {
        val state = ready()
        listOf(state.copy(snapshot = null), state.copy(asOf = null),
            state.copy(snapshot = null, error = "合成首次读取失败"),
            state.copy(notice = "所选类别已合并或删除，待核对")).forEach {
            assertNull(categoryAnalysisSelection(it))
        }
        assertNull(categoryAnalysisSelection(ReportAnalysisUiState()))
    }

    @Test fun refreshing_or_failure_retains_the_same_complete_captured_report_and_cutoff() {
        val prior = ready()
        val expected = requireNotNull(categoryAnalysisSelection(prior))
        listOf(prior.copy(loading = true), prior.copy(error = "合成刷新失败"),
            prior.copy(loading = true, error = "合成刷新失败")).forEach { retained ->
            assertSame(expected, categoryAnalysisSelection(retained))
            assertSame(prior.snapshot, retained.snapshot)
            assertEquals(prior.asOf, retained.asOf)
            assertNull(retained.selectedCategoryId)
        }
    }

    @Test fun real_zero_negative_and_no_records_keep_the_original_report_identity() {
        val zero = ready(listOf(fact("zero", 0)))
        val negative = ready(listOf(fact("paid", 100), fact("refund", 200,
            kind = FinancialFactKind.PROJECT_RECOVERY)))
        val empty = ready(emptyList())
        listOf(zero, negative, empty).forEach { state ->
            assertSame(state.categories.single { it.category.kind == OverviewKind.PROJECT }, categoryAnalysisSelection(state))
        }
        assertTrue(requireNotNull(categoryAnalysisSelection(zero)).historical.hasRecords)
        assertEquals(0, requireNotNull(categoryAnalysisSelection(zero)).currentRate.signum())
        assertEquals(-1, requireNotNull(categoryAnalysisSelection(negative)).currentRate.signum())
        assertFalse(requireNotNull(categoryAnalysisSelection(empty)).historical.hasRecords)
        assertTrue(requireNotNull(categoryAnalysisSelection(empty)).records.isEmpty())
    }

    @Test fun a_new_state_or_type_cannot_borrow_a_previous_category() {
        val old = ready()
        val previous = categoryAnalysisSelection(old)
        assertNotNull(previous)
        val newLedger = ledger(emptyList(), listOf(sc))
        val next = ReportAnalysisUiState(snapshot = newLedger, asOf = today,
            categories = CategoryReportEngine.evaluate(newLedger, today), selectedKind = OverviewKind.PROJECT)
        assertNull(categoryAnalysisSelection(next))
        val subscription = categoryAnalysisSelection(next.copy(selectedKind = OverviewKind.SUBSCRIPTION))
        assertEquals(sc, requireNotNull(subscription).category)
        assertNotSame(previous, subscription)
    }

    private fun ready(facts: List<FinancialFact> = listOf(fact("one", 10000),
        fact("subscription", 36500, kind = FinancialFactKind.SUBSCRIPTION_PAYMENT))): ReportAnalysisUiState {
        val snapshot = ledger(facts)
        return ReportAnalysisUiState(snapshot = snapshot, asOf = today,
            categories = CategoryReportEngine.evaluate(snapshot, today))
    }
}
