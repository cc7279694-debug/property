package com.cooper.lithic.feature.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class C2ProjectEditorDisclosureTest {
    @Test fun same_day_payment_and_empty_optional_note_start_as_summaries() {
        val disclosure = projectEditorDisclosure(create())
        assertFalse(disclosure.startedOn)
        assertFalse(disclosure.initialPaidOn)
        assertFalse(disclosure.note)
    }

    @Test fun distinct_or_empty_payment_date_cannot_be_hidden_behind_same_day_summary() {
        assertTrue(projectEditorDisclosure(create().copy(initialPaidOn = "2026-01-01")).initialPaidOn)
        assertTrue(projectEditorDisclosure(create().copy(initialPaidOn = "")).initialPaidOn)
    }

    @Test fun hidden_field_errors_force_disclosure_even_after_user_collapsed_it() {
        val state = create().copy(errors = mapOf("initialPaidOn" to "日期错误", "note" to "备注错误"))
        val disclosure = projectEditorDisclosure(state, notesExpanded = false)
        assertTrue(disclosure.initialPaidOn)
        assertTrue(disclosure.note)
        assertEquals(ProjectEditorField.INITIAL_PAID_ON, disclosure.firstError)
    }

    @Test fun start_date_error_is_directly_editable_and_first_error_follows_visual_order() {
        val state = create().copy(errors = linkedMapOf("note" to "错误", "startedOn" to "错误", "initialAmount" to "错误"))
        val disclosure = projectEditorDisclosure(state)
        assertTrue(disclosure.startedOn)
        assertEquals(ProjectEditorField.INITIAL_AMOUNT, disclosure.firstError)
    }

    @Test fun existing_note_and_edit_fields_are_visible_before_any_disclosure_click() {
        assertTrue(projectEditorDisclosure(create().copy(note = "先前输入仍在")).note)
        assertTrue(projectEditorDisclosure(create().copy(mode = ProjectEditorMode.EDIT)).note)
    }

    @Test fun explicit_disclosure_only_changes_visibility_and_does_not_rewrite_input() {
        val state = create().copy(initialAmount = "", note = "保留备注")
        assertFalse(projectEditorDisclosure(state, notesExpanded = false).note)
        assertTrue(projectEditorDisclosure(state, paymentExpanded = true, notesExpanded = true).note)
        assertTrue(projectEditorDisclosure(state, paymentExpanded = true).initialPaidOn)
        assertEquals("", state.initialAmount)
        assertEquals("保留备注", state.note)
        assertEquals("2026-01-02", state.initialPaidOn)
    }

    @Test fun edit_does_not_acquire_a_creation_payment_field_or_amount_error_target() {
        val state = create().copy(mode = ProjectEditorMode.EDIT,
            errors = mapOf("initialPaidOn" to "旧值", "initialAmount" to "旧值", "name" to "请输入名称"))
        val disclosure = projectEditorDisclosure(state, paymentExpanded = true)
        assertFalse(disclosure.initialPaidOn)
        assertEquals(ProjectEditorField.NAME, disclosure.firstError)
    }

    private fun create() = ProjectEditorUiState(projectId = "editor-presentation-only",
        startedOn = "2026-01-02", initialPaidOn = "2026-01-02")
}
