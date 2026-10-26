package com.cooper.lithic.core.design

import org.junit.Assert.*
import org.junit.Test

class C2R2RecordIdentityTest {
    @Test fun known_categories_have_distinct_non_brand_symbols() {
        assertEquals(C2Glyph.Computer, c2IdentityGlyph("电子设备"))
        assertEquals(C2Glyph.Book, c2IdentityGlyph("书籍"))
        assertEquals(C2Glyph.Audio, c2IdentityGlyph("音乐"))
        assertEquals(C2Glyph.Wellness, c2IdentityGlyph("健身"))
    }
    @Test fun custom_categories_are_not_guessed_from_a_product_name() {
        assertNull(c2IdentityGlyph("我的收藏"))
        assertEquals("M", c2IdentityInitial("MacBook"))
        assertEquals("相", c2IdentityInitial("相机"))
    }
    @Test fun identity_initial_preserves_a_complete_unicode_code_point() {
        assertEquals("𠮷", c2IdentityInitial(" 𠮷野笔记 "))
    }
    @Test fun blank_identity_keeps_an_explicit_icon_fallback() {
        assertNull(c2IdentityInitial(" \n "))
        assertNull(c2IdentityGlyph("其他"))
    }
}
