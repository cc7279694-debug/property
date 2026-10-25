package com.cooper.lithic.core.design

/** Rebuildable UI presentation only; never evidence that a business operation committed. */
enum class UiMessageTone { INFORMATION, WARNING, ERROR }

/** Every new raw assignment starts conservatively, including a repeated string or clear. */
internal class UiMessageState {
    var text: String? = null
        set(value) {
            field = value
            tone = UiMessageTone.ERROR
        }
    var tone: UiMessageTone = UiMessageTone.ERROR
        private set

    /** Called only where the ViewModel has independently completed the displayed read. */
    fun markInformation() {
        if (text != null) tone = UiMessageTone.INFORMATION
    }
}
