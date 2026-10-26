package com.cooper.lithic.feature.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class C2ProjectDetailPresentationTest {
    @Test fun active_menu_retains_every_existing_management_action() {
        assertEquals(
            listOf(ProjectDetailAction.EDIT, ProjectDetailAction.END, ProjectDetailAction.REPLACEMENT,
                ProjectDetailAction.TRADE_IN, ProjectDetailAction.DELETE),
            projectDetailMenuEntries(false, false, true, true).map { it.action },
        )
    }

    @Test fun ended_menu_preserves_end_correction_without_authorizing_a_new_trade_in() {
        val actions = projectDetailMenuEntries(true, false, true, true).map { it.action }
        assertTrue(ProjectDetailAction.CORRECT_END in actions)
        assertFalse(ProjectDetailAction.END in actions)
        assertFalse(ProjectDetailAction.TRADE_IN in actions)
        assertTrue(ProjectDetailAction.REPLACEMENT in actions)
    }

    @Test fun trade_in_group_keeps_original_delete_protection() {
        val entries = projectDetailMenuEntries(false, true, true, true)
        assertFalse(entries.single { it.action == ProjectDetailAction.DELETE }.enabled)
        // Edit and end routes retain their existing ViewModel eligibility checks.
        assertTrue(entries.single { it.action == ProjectDetailAction.EDIT }.enabled)
    }

    @Test fun missing_optional_routes_do_not_create_dead_menu_actions() {
        val actions = projectDetailMenuEntries(false, false, false, false).map { it.action }
        assertEquals(listOf(ProjectDetailAction.EDIT, ProjectDetailAction.END, ProjectDetailAction.DELETE), actions)
    }

    @Test fun future_end_does_not_mislabel_an_effective_current_result_as_final() {
        assertEquals("当前日均成本", projectDetailRateLabel(hasEnd = true, contributesToCurrent = true))
        assertEquals("截至观察日的日均成本", projectDetailRateLabel(hasEnd = true, contributesToCurrent = false))
        assertEquals("当前日均成本", projectDetailRateLabel(hasEnd = false, contributesToCurrent = false))
    }
}
