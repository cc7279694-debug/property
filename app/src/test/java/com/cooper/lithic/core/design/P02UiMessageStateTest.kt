package com.cooper.lithic.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class P02UiMessageStateTest {
    @Test fun new_message_state_has_no_text_and_conservative_error_tone() {
        val state = UiMessageState()
        assertNull(state.text)
        assertEquals(UiMessageTone.ERROR, state.tone)
    }

    @Test fun explicitly_verified_read_information_preserves_its_exact_text() {
        val state = UiMessageState()
        state.text = "已读取当前资料，请重新确认修改后保存。"
        state.markInformation()
        assertEquals("已读取当前资料，请重新确认修改后保存。", state.text)
        assertEquals(UiMessageTone.INFORMATION, state.tone)
    }

    @Test fun later_error_assignment_cannot_inherit_a_prior_information_tone() {
        val state = UiMessageState()
        state.text = "已读取"
        state.markInformation()
        state.text = "当前记录暂时无法读取，原始提交与输入仍然保留。"
        assertEquals(UiMessageTone.ERROR, state.tone)
        assertEquals("当前记录暂时无法读取，原始提交与输入仍然保留。", state.text)
    }

    @Test fun clearing_a_message_also_clears_information_classification() {
        val state = UiMessageState()
        state.text = "读取说明"
        state.markInformation()
        state.text = null
        assertNull(state.text)
        assertEquals(UiMessageTone.ERROR, state.tone)
    }

    @Test fun identical_text_reassignment_is_not_inferred_to_be_information() {
        val state = UiMessageState()
        state.text = "已读取当前资料"
        state.markInformation()
        state.text = "已读取当前资料"
        assertEquals(UiMessageTone.ERROR, state.tone)
    }

    @Test fun absent_message_cannot_be_marked_as_a_success_or_information() {
        val state = UiMessageState()
        state.markInformation()
        assertNull(state.text)
        assertEquals(UiMessageTone.ERROR, state.tone)
    }
}
