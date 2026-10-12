package com.cooper.lithic.feature.project

import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.repository.ProjectDetail
import java.time.LocalDate

/** These are rebuildable presentations of Repository facts, never another ledger. */
data class ProjectListUiState(
    val items: List<ProjectDetail> = emptyList(),
    val totalCount: Long? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

data class ProjectDetailUiState(
    val detail: ProjectDetail? = null,
    val loading: Boolean = true,
    val missing: Boolean = false,
    val error: String? = null,
)

enum class ProjectEditorMode { CREATE, EDIT }
enum class SubmissionPhase { IDLE, IN_FLIGHT, UNCONFIRMED, COMMITTED }

data class ProjectEditorUiState(
    val mode: ProjectEditorMode = ProjectEditorMode.CREATE,
    val projectId: String = "",
    val name: String = "",
    val startedOn: String = "",
    val initialAmount: String = "",
    val initialPaidOn: String = "",
    val categoryId: String = "",
    val note: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val today: LocalDate? = null,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val dirty: Boolean = false,
    val loading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val message: String? = null,
    val conflict: Boolean = false,
    val needsInitialRead: Boolean = false,
) {
    val busy: Boolean get() = phase == SubmissionPhase.IN_FLIGHT
    val canEdit: Boolean get() = !loading && phase == SubmissionPhase.IDLE
}

data class MoneyEventEditorUiState(
    val projectId: String = "",
    val projectName: String = "",
    val direction: MoneyDirection = MoneyDirection.OUTLAY,
    val amount: String = "",
    val occurredOn: String = "",
    val description: String = "",
    val today: LocalDate? = null,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val dirty: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val message: String? = null,
) {
    val busy: Boolean get() = phase == SubmissionPhase.IN_FLIGHT
    val canEdit: Boolean get() = phase == SubmissionPhase.IDLE
}

data class ProjectUiState(
    val list: ProjectListUiState = ProjectListUiState(),
    val detail: ProjectDetailUiState = ProjectDetailUiState(),
    val editor: ProjectEditorUiState? = null,
    val moneyEditor: MoneyEventEditorUiState? = null,
    val committedProjectId: String? = null,
)

enum class LeaveDecision { ALLOW, CONFIRM_DISCARD, BLOCK_PENDING }
