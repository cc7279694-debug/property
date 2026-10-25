package com.cooper.lithic.core.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.feature.project.DeleteProjectDialog
import com.cooper.lithic.feature.project.DeleteProjectUiState
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectEditorUiState
import com.cooper.lithic.feature.project.SubmissionPhase
import com.cooper.lithic.feature.subscription.SubscriptionEditorScreen
import com.cooper.lithic.feature.subscription.SubscriptionEditorUiState
import com.cooper.lithic.feature.subscription.SubscriptionStateDialog
import com.cooper.lithic.feature.subscription.SubscriptionStateEditorKind
import com.cooper.lithic.feature.subscription.SubscriptionStateEditorUiState
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Component-only assertions: compilation is not device execution; no storage Owner is created. */
@RunWith(AndroidJUnit4::class)
class P02FeedbackSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun typed_read_information_and_identical_untyped_text_have_different_severity() {
        val message = "已读取当前资料，请重新确认修改后保存。"
        var state by mutableStateOf(ProjectEditorUiState(name = "合成项目", message = message,
            messageTone = UiMessageTone.INFORMATION))
        val saves = AtomicInteger()
        compose.setContent { LithicTheme { ProjectHost(state, onSave = { saves.incrementAndGet() }) } }
        compose.onNodeWithTag("project-form-error").performScrollTo().assertTextContains(message)
        compose.onNodeWithTag("project-form-error-kind", useUnmergedTree = true).assertTextEquals("提示")
        compose.onNodeWithTag("project-save").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("project-submission-status").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(messageTone = UiMessageTone.ERROR) }
        compose.onNodeWithTag("project-form-error").performScrollTo().assertTextContains(message)
        compose.onNodeWithTag("project-form-error-kind", useUnmergedTree = true).assertTextEquals("遇到问题")
        assertEquals(0, saves.get())
    }

    @Test fun unconfirmed_warning_does_not_downgrade_original_error_or_unlock_fields() {
        compose.setContent { LithicTheme {
            ProjectHost(ProjectEditorUiState(name = "合成项目", phase = SubmissionPhase.UNCONFIRMED,
                message = "本地数据异常，原始事实和输入保留，请不要清除应用数据。"))
        } }
        compose.onNodeWithTag("project-name").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("project-form-error").performScrollTo()
            .assertTextContains("本地数据异常", substring = true)
        compose.onNodeWithTag("project-form-error-kind", useUnmergedTree = true).assertTextEquals("遇到问题")
        compose.onNodeWithTag("project-submission-status").performScrollTo()
            .assertTextContains("原提交已保留", substring = true)
        compose.onNodeWithTag("project-submission-status-kind", useUnmergedTree = true).assertTextEquals("需要核对")
        compose.onNodeWithTag("project-save").performScrollTo().assertTextContains("核对并重试")
    }

    @Test fun committed_payment_loading_is_not_reported_as_read_failure_and_real_error_stays_error() {
        var state by mutableStateOf(SubscriptionEditorUiState(serviceName = "合成订阅",
            phase = SubmissionPhase.COMMITTED, loading = true))
        compose.setContent { LithicTheme { SubscriptionHost(state) } }
        compose.onNodeWithTag("subscription-payment-read-status").performScrollTo()
            .assertTextContains("正在读取当前详情", substring = true)
        compose.onNodeWithText("已保存，暂时无法读取。请重试读取，不必重新付款。").assertDoesNotExist()
        compose.onNodeWithTag("subscription-retry-read").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(loading = false, message = "已保存，当前详情无法读取。") }
        compose.onNodeWithTag("subscription-form-error").performScrollTo()
            .assertTextContains("已保存，当前详情无法读取。")
        compose.onNodeWithTag("subscription-form-error-kind", useUnmergedTree = true).assertTextEquals("遇到问题")
        compose.onNodeWithTag("subscription-confirm-payment").assertDoesNotExist()
        compose.onNodeWithTag("subscription-retry-read").performScrollTo().assertIsEnabled()
    }

    @Test fun committed_state_readback_keeps_absence_context_without_claiming_this_call_saved() {
        val reads = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionStateDialog(SubscriptionStateEditorUiState(serviceName = "合成订阅",
                kind = SubscriptionStateEditorKind.CANCEL_STOP, phase = SubmissionPhase.COMMITTED),
                {}, {}, {}, {}, {}, {}, {}, { reads.incrementAndGet() })
        } }
        compose.onNodeWithTag("subscription-state-readback-status").performScrollTo()
            .assertTextContains("只读核对", substring = true)
            .assertTextContains("不会再次修改记录", substring = true)
        compose.onNodeWithTag("subscription-state-readback-status-kind", useUnmergedTree = true)
            .assertTextEquals("提示")
        compose.onNodeWithText("本次操作已确认保存", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("subscription-state-confirm").assertDoesNotExist()
        compose.onNodeWithTag("subscription-state-retry-read").performScrollTo().performClick()
        assertEquals(1, reads.get())
    }

    @Test fun committed_delete_readback_error_is_not_downgraded_by_the_phase() {
        var state by mutableStateOf(DeleteProjectUiState(projectName = "合成项目", phase = SubmissionPhase.COMMITTED,
            message = "当前记录暂时无法读取，原始提交与输入仍然保留。", messageTone = UiMessageTone.ERROR))
        compose.setContent { LithicTheme { DeleteProjectDialog(state, {}, {}, {}) } }
        compose.onNodeWithTag("project-delete-message-kind", useUnmergedTree = true).assertTextEquals("遇到问题")
        compose.runOnIdle { state = state.copy(message = "已核对当前不存在；无法证明是哪次删除。",
            messageTone = UiMessageTone.INFORMATION) }
        compose.onNodeWithTag("project-delete-message-kind", useUnmergedTree = true).assertTextEquals("提示")
        compose.onNodeWithTag("project-delete-message").assertTextContains("无法证明是哪次删除", substring = true)
    }

    @Composable private fun ProjectHost(state: ProjectEditorUiState, onSave: () -> Unit = {}) {
        ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, onSave, {})
    }

    @Composable private fun SubscriptionHost(state: SubscriptionEditorUiState) {
        SubscriptionEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
