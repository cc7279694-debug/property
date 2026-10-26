package com.cooper.lithic.core.design

import org.junit.Assert.*
import org.junit.Test

class R1AmountLayoutTest {
    @Test fun sufficient_width_preserves_the_preferred_visual_size() {
        assertEquals(AmountLayout(38f, false), amountLayout(38f, 320) { size -> (size * 5).toInt() })
    }
    @Test fun long_negative_amount_selects_a_fitting_whole_line_before_scroll() {
        val layout = amountLayout(38f, 288) { size -> (size * 9).toInt() }
        assertEquals(32f, layout.fontSp)
        assertFalse(layout.horizontalScroll)
    }
    @Test fun font_scale_two_is_in_the_measurement_and_not_removed_by_the_policy() {
        val layout = amountLayout(38f, 288) { size -> (size * 9 * 2).toInt() }
        assertEquals(18f, layout.fontSp)
        assertTrue(layout.horizontalScroll)
    }
    @Test fun insufficient_width_uses_readable_floor_and_whole_amount_scroll() {
        assertEquals(AmountLayout(18f, true), amountLayout(18f, 100) { 250 })
    }
    @Test fun smaller_requested_text_is_never_enlarged_or_reduced_below_itself() {
        assertEquals(AmountLayout(14f, true), amountLayout(14f, 40) { 100 })
    }
    @Test fun actual_measurements_are_used_instead_of_amount_character_count() {
        assertEquals(AmountLayout(38f, false), amountLayout(38f, 160) { 159 })
        assertEquals(AmountLayout(36f, false), amountLayout(38f, 160) { size -> if (size > 36) 161 else 160 })
    }
}
