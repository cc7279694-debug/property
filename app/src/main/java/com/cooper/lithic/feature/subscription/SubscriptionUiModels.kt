package com.cooper.lithic.feature.subscription

import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.project.SubmissionPhase
import java.time.LocalDate

enum class SubscriptionEditorKind { CREATE, RENEWAL, CORRECT_RENEWAL, REENABLE }

enum class SubscriptionStateEditorKind { STATE, CORRECT_STATE, CANCEL_STOP, DELETE_RENEWAL }

data class SubscriptionStateEditorUiState(
    val kind: SubscriptionStateEditorKind = SubscriptionStateEditorKind.STATE,
    val subscriptionId: String = "",
    val serviceName: String = "",
    val action: String = "PAUSE",
    val origin: String = "CURRENT",
    val effectiveOn: String = "",
    val stateValue: String = "PAUSED",
    val reason: String = "",
    val note: String = "",
    val fixedStopOn: String? = null,
    val targetFactId: String = "",
    val renewalAmount: String = "",
    val renewalPaidOn: String = "",
    val today: LocalDate? = null,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val dirty: Boolean = false,
    val loading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val message: String? = null,
    val conflict: Boolean = false,
    val needsInitialRead: Boolean = false,
    val verifiedCannotWrite: Boolean = false,
    val lastPayment: Boolean = false,
) {
    val busy: Boolean get() = phase == SubmissionPhase.IN_FLIGHT
    val canEdit: Boolean get() = !loading && phase == SubmissionPhase.IDLE && !conflict && !needsInitialRead && !verifiedCannotWrite
}

data class SubscriptionEditorUiState(
    val kind: SubscriptionEditorKind = SubscriptionEditorKind.CREATE,
    val subscriptionId: String = "",
    val serviceName: String = "",
    val categoryId: String = "",
    val note: String = "",
    val amount: String = "",
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val paidOn: String = "",
    val effectiveFrom: String = "",
    val paidUntilExclusive: String = "",
    val reminderDueOn: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val today: LocalDate? = null,
    val phase: SubmissionPhase = SubmissionPhase.IDLE,
    val dirty: Boolean = false,
    val loading: Boolean = false,
    val errors: Map<String, String> = emptyMap(),
    val message: String? = null,
    val conflict: Boolean = false,
    val needsInitialRead: Boolean = false,
    val verifiedCannotWrite: Boolean = false,
    val origin: String = "CURRENT",
    val renewalId: String = "",
    val fixedStopOn: String? = null,
    val reenableAction: String? = null,
) {
    val busy: Boolean get() = phase == SubmissionPhase.IN_FLIGHT
    val canEdit: Boolean get() = !loading && phase == SubmissionPhase.IDLE && !conflict && !needsInitialRead && !verifiedCannotWrite
}

data class SubscriptionListUiState(
    val items: List<SubscriptionDetail> = emptyList(),
    val totalCount: Long? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

data class RenewalHistoryUiState(
    val items: List<SubscriptionRenewalEntity> = emptyList(),
    val totalCount: Long? = null,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

data class SubscriptionDetailUiState(
    val detail: SubscriptionDetail? = null,
    val history: RenewalHistoryUiState = RenewalHistoryUiState(),
    val loading: Boolean = true,
    val missing: Boolean = false,
    val error: String? = null,
)

data class SubscriptionUiState(
    val list: SubscriptionListUiState = SubscriptionListUiState(),
    val detail: SubscriptionDetailUiState = SubscriptionDetailUiState(),
    val editor: SubscriptionEditorUiState? = null,
    val committedSubscriptionId: String? = null,
    val stateEditor: SubscriptionStateEditorUiState? = null,
    val stoppedHistory: SubscriptionListUiState = SubscriptionListUiState(),
    val committedOrigin: String? = null,
)

