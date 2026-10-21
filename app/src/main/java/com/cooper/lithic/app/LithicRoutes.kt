package com.cooper.lithic.app

import androidx.navigation3.runtime.NavKey
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.data.local.StoredValueValidation
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
internal data class ProjectDetail(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectEdit(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectMoneyEntry(val id: String, val direction: MoneyDirection = MoneyDirection.OUTLAY, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectUpdateEnd(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectMoneyCorrection(val id: String, val eventId: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

@Serializable
internal data class ProjectDelete(val id: String, val origin: ProjectOrigin = ProjectOrigin.CURRENT) : NavKey

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
    is ProjectDetail -> if (StoredValueValidation.isUuid(destination.id)) projectParent(destination.origin) + destination else listOf(Home)
    is ProjectEdit -> editorStack(destination.id, destination.origin, destination)
    is ProjectMoneyEntry -> editorStack(destination.id, destination.origin, destination)
    is ProjectEnd -> editorStack(destination.id, destination.origin, destination)
    is ProjectUpdateEnd -> editorStack(destination.id, destination.origin, destination)
    is ProjectMoneyCorrection -> if (StoredValueValidation.isUuid(destination.eventId)) editorStack(destination.id, destination.origin, destination) else listOf(Home)
    is ProjectDelete -> editorStack(destination.id, destination.origin, destination)
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

private fun editorStack(id: String, origin: ProjectOrigin, destination: NavKey): List<NavKey> =
    if (StoredValueValidation.isUuid(id)) projectParent(origin) + listOf(ProjectDetail(id, origin), destination) else listOf(Home)

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home, ProjectList, CreateProject)) return restored.toList()
    if (restored == listOf(Home, SubscriptionList, CreateSubscription)) return restored.toList()
    val destination = restored.lastOrNull() ?: return listOf(Home)
    if (restored == canonicalBackStack(destination)) return restored.toList()
    return listOf(Home)
}
