package com.cooper.lithic.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class C4RecordIdentityTest {
    @Test fun home_can_disable_initial_fallback_without_guessing_a_brand_from_name() {
        assertNull(c2IdentityGlyph("其他"))
        assertNull(c2IdentityInitial("手机", allowInitial = false))
        assertNull(c2IdentityInitial("My Device", allowInitial = false))
    }

    @Test fun opting_out_does_not_change_explicit_category_icons_or_existing_initial_callers() {
        assertEquals(C2Glyph.Computer, c2IdentityGlyph("数码"))
        assertEquals("手", c2IdentityInitial("手机"))
        assertEquals("𠮷", c2IdentityInitial("𠮷野笔记"))
    }
}
