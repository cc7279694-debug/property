package com.cooper.lithic.feature.review

import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.CostTimelineEngine
import com.cooper.lithic.domain.report.LedgerSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.TimelineSampler
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Stateless UI derivation only; these tests do not replace Android layout or generation-owner tests. */
class C5R1TrendPresentationTest {
    private val day = LocalDate.of(2026, 10, 28)

    @Test fun default_first_frame_is_waiting_not_empty_or_a_zero_cost_result() {
        val shown = totalTrendPresentation(TotalCostTrendUiState())
        assertEquals(TrendReadPhase.WAITING, shown.phase)
        assertFalse(shown.hasCompleteSnapshot)
        assertFalse(shown.hasRecords)
        assertFalse(shown.showEmpty)
        assertNull(shown.cutoff)
        assertNull(shown.selected)
    }

    @Test fun first_read_remains_a_placeholder_without_inventing_dates_or_amounts() {
        val shown = totalTrendPresentation(TotalCostTrendUiState(loading = true))
        assertEquals(TrendReadPhase.READING, shown.phase)
        assertFalse(shown.hasCompleteSnapshot)
        assertFalse(shown.hasRecords)
        assertFalse(shown.showEmpty)
        assertNull(shown.cutoff)
        assertNull(shown.selected)
    }

    @Test fun refreshing_keeps_the_same_captured_result_and_old_cutoff() {
        val prior = ready(asOf = day.minusDays(1))
        val shown = totalTrendPresentation(prior.copy(loading = true))
        assertEquals(TrendReadPhase.REFRESHING, shown.phase)
        assertTrue(shown.hasCompleteSnapshot)
        assertTrue(shown.hasRecords)
        assertFalse(shown.showEmpty)
        assertEquals(day.minusDays(1), shown.cutoff)
        assertSame(prior.selectedPoint, shown.selected)
    }

    @Test fun first_read_failure_is_not_presented_as_an_empty_ledger() {
        val shown = totalTrendPresentation(TotalCostTrendUiState(error = "合成读取失败"))
        assertEquals(TrendReadPhase.FAILED, shown.phase)
        assertFalse(shown.hasCompleteSnapshot)
        assertFalse(shown.showEmpty)
        assertNull(shown.cutoff)
        assertNull(shown.selected)
    }

    @Test fun failure_with_old_facts_keeps_their_amount_and_cutoff_not_a_new_date() {
        val prior = ready(asOf = day.minusDays(2))
        val shown = totalTrendPresentation(prior.copy(error = "合成刷新失败"))
        assertEquals(TrendReadPhase.FAILED, shown.phase)
        assertTrue(shown.hasCompleteSnapshot)
        assertTrue(shown.hasRecords)
        assertEquals(day.minusDays(2), shown.cutoff)
        assertSame(prior.selectedPoint, shown.selected)
        assertFalse(shown.showEmpty)
    }

    @Test fun selecting_a_date_without_a_point_is_pending_even_when_not_reading() {
        val prior = ready()
        val shown = totalTrendPresentation(prior.copy(selectedDate = day.minusDays(1), selectedPoint = null))
        assertEquals(TrendReadPhase.READY, shown.phase)
        assertTrue(shown.hasCompleteSnapshot)
        assertTrue(shown.hasRecords)
        assertEquals(day, shown.cutoff)
        assertNull(shown.selected)
        assertFalse(shown.showEmpty)
    }

    @Test fun a_previous_dates_point_cannot_supply_the_new_dates_readout() {
        val prior = ready()
        val shown = totalTrendPresentation(prior.copy(selectedDate = day.minusDays(1)))
        assertTrue(shown.hasRecords)
        assertEquals(day, shown.cutoff)
        assertNull(shown.selected)
        assertFalse(shown.showEmpty)
    }

    @Test fun either_half_of_a_snapshot_pair_cannot_authorize_a_chart_or_exact_amount() {
        val prior = ready()
        listOf(prior.copy(snapshot = null), prior.copy(timeline = null)).forEach { partial ->
            val shown = totalTrendPresentation(partial)
            assertEquals(TrendReadPhase.WAITING, shown.phase)
            assertFalse(shown.hasCompleteSnapshot)
            assertFalse(shown.hasRecords)
            assertFalse(shown.showEmpty)
            assertNull(shown.cutoff)
            assertNull(shown.selected)
        }
    }

    @Test fun a_fresh_ui_state_does_not_reuse_a_previously_presented_generation() {
        val old = totalTrendPresentation(ready())
        assertTrue(old.hasRecords)
        val fresh = totalTrendPresentation(TotalCostTrendUiState(loading = true))
        assertEquals(TrendReadPhase.READING, fresh.phase)
        assertFalse(fresh.hasCompleteSnapshot)
        assertFalse(fresh.hasRecords)
        assertFalse(fresh.showEmpty)
        assertNull(fresh.cutoff)
        assertNull(fresh.selected)
        assertTrue(old.hasRecords)
    }

    @Test fun only_a_complete_quiet_empty_snapshot_is_a_confirmed_empty_result() {
        val shown = totalTrendPresentation(empty())
        assertEquals(TrendReadPhase.READY, shown.phase)
        assertTrue(shown.hasCompleteSnapshot)
        assertFalse(shown.hasRecords)
        assertTrue(shown.showEmpty)
        assertEquals(day, shown.cutoff)
        assertNull(shown.selected)
    }

    @Test fun empty_facts_during_refresh_error_or_notice_do_not_hide_the_pending_state() {
        val known = empty()
        listOf(known.copy(loading = true), known.copy(error = "合成读取失败"),
            known.copy(notice = "所选日期已调整，待核对")).forEach { state ->
            val shown = totalTrendPresentation(state)
            assertTrue(shown.hasCompleteSnapshot)
            assertFalse(shown.hasRecords)
            assertFalse(shown.showEmpty)
            assertNull(shown.selected)
        }
    }

    @Test fun genuine_zero_negative_and_unrounded_results_are_retained_as_supplied() {
        listOf(ready(outlayCents = 0) to BigDecimal.ZERO,
            ready(outlayCents = 10_001) to BigDecimal("10.001"),
            ready(outlayCents = 10_000, recoveryCents = 20_000) to BigDecimal("-10")).forEach { (state, expected) ->
            val shown = totalTrendPresentation(state)
            assertEquals(TrendReadPhase.READY, shown.phase)
            assertTrue(shown.hasRecords)
            assertFalse(shown.showEmpty)
            assertSame(state.selectedPoint, shown.selected)
            assertEquals(0, expected.compareTo(requireNotNull(shown.selected).totalRate))
        }
    }

    private fun ready(asOf: LocalDate = day, outlayCents: Long = 10_000, recoveryCents: Long = 0): TotalCostTrendUiState {
        val start = asOf.minusDays(9)
        val events = mutableListOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(outlayCents), start))
        if (recoveryCents > 0) events += ProjectMoneyEvent(MoneyDirection.RECOVERY, Money(recoveryCents), asOf)
        val project = ProjectSnapshot(LongTermProject(start), events)
        val snapshot = LedgerSnapshot(OverviewSnapshot(listOf(OverviewCategory("r1-category", "合成类别", OverviewKind.PROJECT)),
            listOf(OverviewProject("r1-project", "r1-category", project)), emptyList()))
        return TotalCostTrendUiState(snapshot = snapshot, timeline = TimelineSampler.sample(snapshot, asOf),
            selectedDate = asOf, selectedPoint = CostTimelineEngine.point(snapshot, asOf))
    }

    private fun empty(): TotalCostTrendUiState {
        val snapshot = LedgerSnapshot(OverviewSnapshot(emptyList(), emptyList(), emptyList()))
        return TotalCostTrendUiState(snapshot = snapshot, timeline = TimelineSampler.sample(snapshot, day),
            selectedDate = day, selectedPoint = CostTimelineEngine.point(snapshot, day))
    }
}
