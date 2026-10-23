package com.cooper.lithic.core.design

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ChartGeometryTest {
    private val day = LocalDate.of(2026, 1, 1)
    private fun point(offset: Long, value: String) = CostChartPoint(day.plusDays(offset), BigDecimal(value))

    @Test fun empty_data_has_no_projection_instead_of_fake_zero() {
        assertNull(ChartGeometry.forPoints(emptyList()))
    }

    @Test fun horizontal_spacing_uses_real_dates_instead_of_list_indices() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "10"), point(1, "9"), point(10, "1")))!!
        assertEquals(0f, geometry.x(day), 0f)
        assertEquals(0.1f, geometry.x(day.plusDays(1)), 0.00001f)
        assertEquals(1f, geometry.x(day.plusDays(10)), 0f)
    }

    @Test fun all_zero_has_a_visible_finite_centerline() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "0"), point(4, "0")))!!
        assertEquals(0.5f, geometry.zero, 0f)
        assertEquals(geometry.zero, geometry.y(BigDecimal.ZERO), 0f)
        assertTrue(geometry.y(BigDecimal.ZERO).isFinite())
    }

    @Test fun all_negative_values_have_real_negative_space_below_zero() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "-10"), point(4, "-2")))!!
        assertEquals(0f, geometry.zero, 0f)
        assertTrue(geometry.y(BigDecimal("-10")) > geometry.y(BigDecimal("-2")))
        assertTrue(geometry.y(BigDecimal("-2")) > geometry.zero)
    }

    @Test fun mixed_signs_keep_zero_between_positive_and_negative() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "-2"), point(1, "3")))!!
        assertTrue(geometry.y(BigDecimal("3")) < geometry.zero)
        assertTrue(geometry.y(BigDecimal("-2")) > geometry.zero)
    }

    @Test fun extremely_small_nonzero_values_do_not_collapse_to_fake_zero() {
        val tiny = BigDecimal("1E-1000")
        val geometry = ChartGeometry.forPoints(listOf(CostChartPoint(day, tiny), point(1, "0")))!!
        assertEquals(0f, geometry.y(tiny), 0f)
        assertEquals(1f, geometry.zero, 0f)
    }

    @Test fun single_day_is_centered_and_large_bigdecimal_range_stays_finite() {
        val single = ChartGeometry.forPoints(listOf(point(0, "2")))!!
        assertEquals(0.5f, single.x(day), 0f)
        val large = BigDecimal("1E1000")
        val geometry = ChartGeometry.forPoints(listOf(CostChartPoint(day, large.negate()), CostChartPoint(day.plusDays(1), large)))!!
        assertEquals(0.5f, geometry.zero, 0.00001f)
        assertEquals(0f, geometry.y(large), 0f)
        assertEquals(1f, geometry.y(large.negate()), 0f)
    }

    @Test fun date_selection_uses_natural_days_between_display_points() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "10"), point(10, "1")))!!
        assertEquals(day.plusDays(6), geometry.dateAt(.6f))
        assertEquals(day.plusDays(5), geometry.dateAt(.5f))
        assertEquals(day, geometry.dateAt(-1f))
        assertEquals(day.plusDays(10), geometry.dateAt(2f))
    }

    @Test fun date_mapping_does_not_depend_on_money_or_list_index() {
        val tiny = ChartGeometry.forPoints(listOf(point(0, "1E-1000"), point(10, "0")))!!
        val negative = ChartGeometry.forPoints(listOf(point(0, "-1E1000"), point(1, "0"), point(10, "-1")))!!
        for (fraction in listOf(0f, .13f, .51f, .91f, 1f)) assertEquals(tiny.dateAt(fraction), negative.dateAt(fraction))
    }

    @Test fun calendar_actions_move_one_day_not_one_retained_point() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "10"), point(100, "1")))!!
        assertEquals(day.plusDays(49), geometry.adjacentDay(day.plusDays(50), -1))
        assertEquals(day.plusDays(51), geometry.adjacentDay(day.plusDays(50), 1))
        assertEquals(day, geometry.adjacentDay(day, -1))
        assertEquals(day.plusDays(100), geometry.adjacentDay(day.plusDays(100), 1))
    }

    @Test fun date_selection_handles_single_day_and_extreme_supported_dates() {
        val single = ChartGeometry.forPoints(listOf(point(0, "1")))!!
        assertEquals(day, single.dateAt(.8f))
        val extreme = ChartGeometry.forPoints(listOf(CostChartPoint(LocalDate.MIN, BigDecimal.ZERO),
            CostChartPoint(LocalDate.MAX, BigDecimal.ONE)))!!
        assertEquals(LocalDate.MIN, extreme.dateAt(0f))
        assertEquals(LocalDate.MAX, extreme.dateAt(1f))
        assertEquals(LocalDate.MIN, extreme.adjacentDay(LocalDate.MIN, -1))
        assertEquals(LocalDate.MAX, extreme.adjacentDay(LocalDate.MAX, 1))
    }

    @Test fun nonfinite_pointer_positions_are_rejected() {
        val geometry = ChartGeometry.forPoints(listOf(point(0, "1"), point(3, "0")))!!
        listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { fraction ->
            assertThrows(IllegalArgumentException::class.java) { geometry.dateAt(fraction) }
        }
    }
}
