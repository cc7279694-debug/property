package com.cooper.lithic.app

import androidx.navigation3.runtime.NavKey
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.StoredValueValidation
import com.cooper.lithic.feature.project.ProjectEditorMode
import com.cooper.lithic.feature.project.ProjectUiState
import kotlinx.serialization.Serializable

@Serializable
internal data object Home : NavKey

@Serializable
internal data object Review : NavKey

@Serializable
internal data object LongTermReview : NavKey

@Serializable
internal data object Settings : NavKey

@Serializable
internal data object NameSearch : NavKey

@Serializable
internal data object CategoryManagement : NavKey

@Serializable
internal data object ProjectList : NavKey

@Serializable
internal data object ProjectHistory : NavKey

@Serializable
internal enum class ProjectOrigin { CURRENT, HISTORY, REVIEW }

@Serializable
internal data object CreateProject : NavKey

@Serializable
internal data class ProjectDetail(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectEdit(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectMoneyEntry(val id: String, val direction: MoneyDirection = MoneyDirection.OUTLAY, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectUpdateEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectMoneyCorrection(val id: String, val eventId: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ProjectDelete(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data class ReplacementComparison(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class TradeInProject(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT, val comparisonId: String? = null) : NavKey

@Serializable
internal data object SubscriptionList : NavKey

@Serializable
internal data object CreateSubscription : NavKey

@Serializable
internal enum class SubscriptionOrigin { CURRENT, HISTORY, REVIEW }

@Serializable
internal enum class SubscriptionAction { PAUSE, RESUME, REOPEN, STOP }

@Serializable
internal data object SubscriptionHistory : NavKey

@Serializable
internal data class SubscriptionDetail(val id: String, val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class ConfirmSubscriptionRenewal(val id: String, val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class ChangeSubscriptionState(val id: String, val action: SubscriptionAction,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CorrectSubscriptionState(val id: String, val stateId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CancelSubscriptionStop(val id: String, val stateId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class CorrectSubscriptionRenewal(val id: String, val renewalId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

@Serializable
internal data class DeleteSubscriptionRenewal(val id: String, val renewalId: String,
    val origin: SubscriptionOrigin = SubscriptionOrigin.CURRENT) : NavKey

internal fun canonicalBackStack(destination: NavKey): List<NavKey> = when (destination) {
    Home -> listOf(Home)
    Review, Settings, ProjectList, SubscriptionList -> listOf(Home, destination)
    LongTermReview -> listOf(Home, Review, LongTermReview)
    NameSearch -> listOf(Home, NameSearch)
    CategoryManagement -> listOf(Home, Settings, CategoryManagement)
    ProjectHistory -> listOf(Home, Review, ProjectHistory)
    CreateProject -> listOf(Home, CreateProject)
    CreateSubscription -> listOf(Home, CreateSubscription)
    SubscriptionHistory -> listOf(Home, Review, SubscriptionHistory)
    is SubscriptionDetail -> if (StoredValueValidation.isUuid(destination.id)) subscriptionParent(destination.origin) + destination else listOf(Home)
    is ConfirmSubscriptionRenewal -> subscriptionEditorStack(destination.id, destination.origin, destination)
    is ChangeSubscriptionState -> subscriptionEditorStack(destination.id, destination.origin, destination)
    is CorrectSubscriptionState -> if (StoredValueValidation.isUuid(destination.stateId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is CancelSubscriptionStop -> if (StoredValueValidation.isUuid(destination.stateId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is CorrectSubscriptionRenewal -> if (StoredValueValidation.isUuid(destination.renewalId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is DeleteSubscriptionRenewal -> if (StoredValueValidation.isUuid(destination.renewalId)) subscriptionEditorStack(destination.id, destination.origin, destination) else listOf(Home)
    is ProjectDetail -> projectDetailStack(destination.id, destination.origin, destination.comparisonId)
    is ReplacementComparison -> if (StoredValueValidation.isUuid(destination.id)) projectDetailStack(destination.id, destination.origin, null) + destination else listOf(Home)
    is TradeInProject -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectEdit -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectMoneyEntry -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectEnd -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectUpdateEnd -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    is ProjectMoneyCorrection -> if (StoredValueValidation.isUuid(destination.eventId)) editorStack(destination.id, destination.origin, destination, destination.comparisonId) else listOf(Home)
    is ProjectDelete -> editorStack(destination.id, destination.origin, destination, destination.comparisonId)
    else -> listOf(Home)
}

internal fun subscriptionOrigin(destination: NavKey): SubscriptionOrigin = when (destination) {
    SubscriptionHistory -> SubscriptionOrigin.HISTORY
    is SubscriptionDetail -> destination.origin
    is ConfirmSubscriptionRenewal -> destination.origin
    is ChangeSubscriptionState -> destination.origin
    is CorrectSubscriptionState -> destination.origin
    is CancelSubscriptionStop -> destination.origin
    is CorrectSubscriptionRenewal -> destination.origin
    is DeleteSubscriptionRenewal -> destination.origin
    else -> SubscriptionOrigin.CURRENT
}

private fun subscriptionParent(origin: SubscriptionOrigin): List<NavKey> = when (origin) {
    SubscriptionOrigin.CURRENT -> listOf(Home, SubscriptionList)
    SubscriptionOrigin.HISTORY -> listOf(Home, Review, SubscriptionHistory)
    SubscriptionOrigin.REVIEW -> listOf(Home, Review, LongTermReview)
}

private fun subscriptionEditorStack(id: String, origin: SubscriptionOrigin, destination: NavKey): List<NavKey> =
    if (StoredValueValidation.isUuid(id)) subscriptionParent(origin) + listOf(SubscriptionDetail(id, origin), destination) else listOf(Home)

internal fun projectOrigin(destination: NavKey): ProjectOrigin = when (destination) {
    is ProjectDetail -> destination.origin
    is ReplacementComparison -> destination.origin
    is TradeInProject -> destination.origin
    is ProjectEdit -> destination.origin
    is ProjectMoneyEntry -> destination.origin
    is ProjectEnd -> destination.origin
    is ProjectUpdateEnd -> destination.origin
    is ProjectMoneyCorrection -> destination.origin
    is ProjectDelete -> destination.origin
    ProjectHistory -> ProjectOrigin.HISTORY
    else -> ProjectOrigin.CURRENT
}

private fun projectParent(origin: ProjectOrigin): List<NavKey> = when (origin) {
    ProjectOrigin.CURRENT -> listOf(Home, ProjectList)
    ProjectOrigin.HISTORY -> listOf(Home, Review, ProjectHistory)
    ProjectOrigin.REVIEW -> listOf(Home, Review, LongTermReview)
}

private fun projectDetailStack(id: String, origin: ProjectOrigin, comparisonId: String?): List<NavKey> {
    if (!StoredValueValidation.isUuid(id)) return listOf(Home)
    if (comparisonId == null) return projectParent(origin) + ProjectDetail(id, origin)
    if (!StoredValueValidation.isUuid(comparisonId) || comparisonId == id) return listOf(Home)
    return projectParent(origin) + listOf(ProjectDetail(comparisonId, origin), ReplacementComparison(comparisonId, origin), ProjectDetail(id, origin, comparisonId))
}

private fun editorStack(id: String, origin: ProjectOrigin, destination: NavKey, comparisonId: String? = null): List<NavKey> {
    val detail = projectDetailStack(id, origin, comparisonId)
    return if (detail == listOf(Home)) detail else detail + destination
}

internal fun projectComparisonId(destination: NavKey): String? = when (destination) {
    is ProjectDetail -> destination.comparisonId
    is ProjectEdit -> destination.comparisonId
    is ProjectMoneyEntry -> destination.comparisonId
    is ProjectEnd -> destination.comparisonId
    is ProjectUpdateEnd -> destination.comparisonId
    is ProjectMoneyCorrection -> destination.comparisonId
    is ProjectDelete -> destination.comparisonId
    is TradeInProject -> destination.comparisonId
    else -> null
}

/** The pending dialog uses this route without discarding or reconstructing the fixed command. */
internal fun projectRecoveryRoute(state: ProjectUiState, origin: ProjectOrigin, comparisonId: String? = null): NavKey? = when {
    state.editor != null -> if (state.editor.mode == ProjectEditorMode.CREATE) CreateProject else ProjectEdit(state.editor.projectId, origin, comparisonId)
    state.moneyEditor != null -> ProjectMoneyEntry(state.moneyEditor.projectId, state.moneyEditor.direction, origin, comparisonId)
    state.endEditor != null -> if (state.endEditor.correcting) ProjectUpdateEnd(state.endEditor.projectId, origin, comparisonId) else ProjectEnd(state.endEditor.projectId, origin, comparisonId)
    state.moneyCorrection != null -> ProjectMoneyCorrection(state.moneyCorrection.projectId, state.moneyCorrection.eventId, origin, comparisonId)
    state.deleteConfirmation != null -> ProjectDelete(state.deleteConfirmation.projectId, origin, comparisonId)
    else -> null
}

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home, ProjectList, CreateProject)) return restored.toList()
    if (restored == listOf(Home, SubscriptionList, CreateSubscription)) return restored.toList()
    val destination = restored.lastOrNull() ?: return listOf(Home)
    if (restored == canonicalBackStack(destination)) return restored.toList()
    return listOf(Home)
}
