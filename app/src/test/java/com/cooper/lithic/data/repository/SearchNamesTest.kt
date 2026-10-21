package com.cooper.lithic.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SearchNamesTest {
    @Test fun percent_underscore_and_escape_are_literal() {
        assertEquals("%100!%!_!!%", SearchNames.literalPattern("100%_!"))
    }

    @Test fun apostrophes_stay_in_the_bound_value() {
        assertEquals("%O'Reilly%", SearchNames.literalPattern("O'Reilly"))
        assertEquals("%' OR 1=1 --%", SearchNames.literalPattern("' OR 1=1 --"))
    }

    @Test fun trims_only_outer_query_spaces_and_preserves_name_text() {
        assertEquals("相机  Pro", SearchNames.query("  相机  Pro  "))
        assertEquals("", SearchNames.query(" \t "))
    }

    @Test fun rejects_unbounded_or_control_input() {
        assertThrows(IllegalArgumentException::class.java) { SearchNames.query("x".repeat(201)) }
        assertThrows(IllegalArgumentException::class.java) { SearchNames.query("a\u0000b") }
        assertThrows(IllegalArgumentException::class.java) { SearchNames.query("a\nb") }
    }
}
