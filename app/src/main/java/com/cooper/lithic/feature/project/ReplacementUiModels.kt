package com.cooper.lithic.feature.project

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.ReplacementEntity
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.data.repository.ReplacementChain
import java.time.LocalDate

internal data class ReplacementUiState(
    val anchorId: String = "",
    val chain: ReplacementChain? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val form: ReplacementFormUiState? = null,
    val choices: List<ProjectDetail> = emptyList(),
    val hasMoreChoices: Boolean = false,
    val loadingChoices: Boolean = false,
    val committedProjectId: String? = null,
)

internal enum class ReplacementFormKind { LINK, UNLINK, TRADE_IN }

internal data class ReplacementFormUiState(
    val kind: ReplacementFormKind,
    val projectId: String,
    val projectName: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val selectedLink: ReplacementEntity? = null,
    val asSuccessor: Boolean = true,
    val name: String = "",
    val date: String = "",
    val cash: String = "",
    val credit: String = "",
    val categoryId: String = "",
    val note: String = "",
    val linkReplacement: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val today: LocalDate,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val loading: Boolean = false,
    val confirmed: Boolean = false,
    val needsInitialRead: Boolean = false,
    val message: String? = null,
    val errors: Map<String, String> = emptyMap(),
) {
    val canEdit get() = phase == SubmissionPhase.IDLE && !loading
    val busy get() = phase == SubmissionPhase.IN_FLIGHT
}
