package com.cooper.lithic.data.local

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Existing schema-v2 comparison semantics must survive editable category names. */
class CategoryNamesTest {
    @Test fun full_width_and_case_share_the_same_comparison_name() {
        assertEquals(CategoryNames.normalized("Camera"), CategoryNames.normalized("ＣＡＭＥＲＡ"))
    }

    @Test fun unicode_spacing_and_repeated_spacing_are_normalized() {
        assertEquals("home tools", CategoryNames.normalized("　Home\u00a0  Tools　"))
    }

    @Test fun device_locale_does_not_change_category_identity() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("internet", CategoryNames.normalized("INTERNET"))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun blank_comparison_names_cannot_become_valid_categories() {
        assertEquals("", CategoryNames.normalized(" \t\u3000 "))
    }

    @Test fun punctuation_and_chinese_names_remain_literal() {
        assertEquals("家用%_设备'", CategoryNames.normalized(" 家用%_设备' "))
        assertNotEquals(CategoryNames.normalized("家用设备"), CategoryNames.normalized("家用%_设备'"))
    }
}
