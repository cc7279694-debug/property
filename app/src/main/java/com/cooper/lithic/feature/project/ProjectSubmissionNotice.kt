package com.cooper.lithic.feature.project

import androidx.compose.runtime.Composable
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.core.design.UiMessageTone

/** Explains the existing phase; submission identity and replay remain owned by ProjectViewModel. */
@Composable
internal fun ProjectSubmissionNotice(phase: SubmissionPhase, tagPrefix: String) {
    val text = when (phase) {
        SubmissionPhase.IDLE -> return
        SubmissionPhase.IN_FLIGHT -> "正在保存到本地"
        SubmissionPhase.UNCONFIRMED -> "保存结果尚未确认，输入和原提交已保留。在结果确认前，不要另建相同记录。"
        SubmissionPhase.COMMITTED -> "记录已保存。重新读取只更新显示，不会再次保存。"
    }
    UiFeedbackText(text, if (phase == SubmissionPhase.UNCONFIRMED) UiMessageTone.WARNING
        else UiMessageTone.INFORMATION, "$tagPrefix-submission-status")
}
