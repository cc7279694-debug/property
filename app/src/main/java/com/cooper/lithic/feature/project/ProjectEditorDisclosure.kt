package com.cooper.lithic.feature.project

/** Rebuildable visibility only. Values, validation, command identity and defaults stay in the VM. */
internal data class ProjectEditorDisclosure(
    val startedOn: Boolean,
    val initialPaidOn: Boolean,
    val note: Boolean,
    val firstError: ProjectEditorField?,
)

internal enum class ProjectEditorField(val errorKey: String) {
    NAME("name"), INITIAL_AMOUNT("initialAmount"), STARTED_ON("startedOn"),
    CATEGORY("categoryId"), INITIAL_PAID_ON("initialPaidOn"), NOTE("note"),
}

internal fun projectEditorDisclosure(
    state: ProjectEditorUiState,
    startExpanded: Boolean = false,
    paymentExpanded: Boolean = false,
    notesExpanded: Boolean? = null,
): ProjectEditorDisclosure {
    val creating = state.mode == ProjectEditorMode.CREATE
    return ProjectEditorDisclosure(
        startedOn = !creating || startExpanded || state.startedOn.isBlank() || "startedOn" in state.errors,
        initialPaidOn = creating && (paymentExpanded || state.initialPaidOn.isBlank() ||
            state.initialPaidOn != state.startedOn || "initialPaidOn" in state.errors),
        note = "note" in state.errors || (notesExpanded ?: (!creating || state.note.isNotBlank())),
        firstError = ProjectEditorField.entries.firstOrNull { field ->
            field.errorKey in state.errors && (creating || field !in listOf(
                ProjectEditorField.INITIAL_AMOUNT, ProjectEditorField.INITIAL_PAID_ON))
        },
    )
}
