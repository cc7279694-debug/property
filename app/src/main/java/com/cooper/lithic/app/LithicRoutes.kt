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
internal data object Settings : NavKey

@Serializable
internal data object ProjectList : NavKey

@Serializable
internal data object ProjectHistory : NavKey

@Serializable
internal enum class ProjectOrigin { CURRENT, HISTORY }

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

internal fun canonicalBackStack(destination: NavKey): List<NavKey> = when (destination) {
    Home -> listOf(Home)
    Review, Settings, ProjectList -> listOf(Home, destination)
    ProjectHistory -> listOf(Home, Review, ProjectHistory)
    CreateProject -> listOf(Home, CreateProject)
    is ProjectDetail -> if (StoredValueValidation.isUuid(destination.id)) projectParent(destination.origin) + destination else listOf(Home)
    is ProjectEdit -> editorStack(destination.id, destination.origin, destination)
    is ProjectMoneyEntry -> editorStack(destination.id, destination.origin, destination)
    is ProjectEnd -> editorStack(destination.id, destination.origin, destination)
    is ProjectUpdateEnd -> editorStack(destination.id, destination.origin, destination)
    is ProjectMoneyCorrection -> if (StoredValueValidation.isUuid(destination.eventId)) editorStack(destination.id, destination.origin, destination) else listOf(Home)
    is ProjectDelete -> editorStack(destination.id, destination.origin, destination)
    else -> listOf(Home)
}

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
}

private fun editorStack(id: String, origin: ProjectOrigin, destination: NavKey): List<NavKey> =
    if (StoredValueValidation.isUuid(id)) projectParent(origin) + listOf(ProjectDetail(id, origin), destination) else listOf(Home)

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home, ProjectList, CreateProject)) return restored.toList()
    val destination = restored.lastOrNull() ?: return listOf(Home)
    if (restored == canonicalBackStack(destination)) return restored.toList()
    return listOf(Home)
}
