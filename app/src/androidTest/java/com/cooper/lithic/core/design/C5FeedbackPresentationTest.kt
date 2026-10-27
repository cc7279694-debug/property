package com.cooper.lithic.core.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test

/** Presentation contract only; does not execute a backup, restore or business write. */
class C5FeedbackPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compact_feedback_preserves_typed_labels_full_text_and_polite_announcements() {
        val warning = "恢复结果暂无法确认。请保留当前现场，不要清除应用数据；核对当前记录后再继续。"
        val error = "导出失败，保存位置可能留有不完整文件。残留文件请勿用于恢复。"
        compose.setContent { LithicTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            UiFeedbackText("已读取当前记录。", UiMessageTone.INFORMATION, "information")
            UiFeedbackText(warning, UiMessageTone.WARNING, "warning")
            UiFeedbackText(error, UiMessageTone.ERROR, "error")
        } } }
        compose.onNodeWithTag("information-kind").assertTextEquals("提示")
        compose.onNodeWithTag("warning-kind").assertTextEquals("需要核对")
        compose.onNodeWithTag("error-kind").assertTextEquals("遇到问题")
        mapOf("warning" to warning, "error" to error).forEach { (tag, message) ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().assertTextEquals(message)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        }
    }
}
