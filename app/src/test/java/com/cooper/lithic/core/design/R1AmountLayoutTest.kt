package com.cooper.lithic.core.design

import org.junit.Assert.*
import org.junit.Test

class R1AmountLayoutTest {
    @Test fun sufficient_width_preserves_the_preferred_visual_size() {
        assertEquals(AmountLayout(38f, false), amountLayout(MoneyMetricLevel.HERO, 320) { size -> (size * 5).toInt() })
    }
    @Test fun long_negative_amount_selects_a_fitting_whole_line_before_scroll() {
        val layout = amountLayout(MoneyMetricLevel.HERO, 288) { size -> (size * 9).toInt() }
        assertEquals(32f, layout.fontSp)
        assertFalse(layout.horizontalScroll)
    }
    @Test fun font_scale_two_is_in_the_measurement_and_not_removed_by_the_policy() {
        val layout = amountLayout(MoneyMetricLevel.HERO, 288) { size -> (size * 9 * 2).toInt() }
        assertEquals(28f, layout.fontSp)
        assertTrue(layout.horizontalScroll)
    }
    @Test fun insufficient_width_uses_readable_floor_and_whole_amount_scroll() {
        assertEquals(AmountLayout(18f, true), amountLayout(MoneyMetricLevel.ROW, 100) { 250 })
    }
    @Test fun each_role_keeps_its_readable_floor_instead_of_all_shrinking_to_row_size() {
        assertEquals(AmountLayout(28f, true), amountLayout(MoneyMetricLevel.HERO, 40) { 100 })
        assertEquals(AmountLayout(24f, true), amountLayout(MoneyMetricLevel.DETAIL, 40) { 100 })
        assertEquals(AmountLayout(20f, true), amountLayout(MoneyMetricLevel.CARD, 40) { 100 })
        assertEquals(AmountLayout(18f, true), amountLayout(MoneyMetricLevel.ROW, 40) { 100 })
    }
    @Test fun actual_measurements_are_used_instead_of_amount_character_count() {
        assertEquals(AmountLayout(38f, false), amountLayout(MoneyMetricLevel.HERO, 160) { 159 })
        assertEquals(AmountLayout(32f, false), amountLayout(MoneyMetricLevel.HERO, 160) { size -> if (size > 32) 161 else 160 })
    }
    @Test fun suffix_width_can_require_the_next_candidate_even_when_the_digits_alone_fit() {
        assertEquals(AmountLayout(32f, false), amountLayout(MoneyMetricLevel.HERO, 280) { size -> (size * 8).toInt() })
        assertEquals(AmountLayout(28f, false), amountLayout(MoneyMetricLevel.HERO, 280) { size -> (size * 8).toInt() + 40 })
    }
    @Test fun short_values_use_the_four_specified_preferred_sizes() {
        assertEquals(AmountLayout(38f, false), amountLayout(MoneyMetricLevel.HERO, 320) { 80 })
        assertEquals(AmountLayout(28f, false), amountLayout(MoneyMetricLevel.DETAIL, 320) { 80 })
        assertEquals(AmountLayout(24f, false), amountLayout(MoneyMetricLevel.CARD, 320) { 80 })
        assertEquals(AmountLayout(18f, false), amountLayout(MoneyMetricLevel.ROW, 320) { 80 })
    }
}
