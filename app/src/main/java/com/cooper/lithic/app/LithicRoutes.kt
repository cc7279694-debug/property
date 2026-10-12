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
internal data object CreateProject : NavKey

@Serializable
internal data class ProjectDetail(val id: String) : NavKey

@Serializable
internal data class ProjectEdit(val id: String) : NavKey

@Serializable
internal data class ProjectMoneyEntry(val id: String, val direction: MoneyDirection = MoneyDirection.OUTLAY) : NavKey

internal fun canonicalBackStack(destination: NavKey): List<NavKey> = when (destination) {
    Home -> listOf(Home)
    Review, Settings, ProjectList -> listOf(Home, destination)
    CreateProject -> listOf(Home, CreateProject)
    is ProjectDetail -> if (StoredValueValidation.isUuid(destination.id)) listOf(Home, ProjectList, destination) else listOf(Home)
    is ProjectEdit -> if (StoredValueValidation.isUuid(destination.id)) listOf(Home, ProjectList, ProjectDetail(destination.id), destination) else listOf(Home)
    is ProjectMoneyEntry -> if (StoredValueValidation.isUuid(destination.id)) listOf(Home, ProjectList, ProjectDetail(destination.id), destination) else listOf(Home)
    else -> listOf(Home)
}

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home)) return listOf(Home)
    if (restored.size == 2 && restored.first() == Home &&
        restored.last() in listOf(Review, Settings, ProjectList, CreateProject)) {
        return restored.toList()
    }
    if (restored == listOf(Home, ProjectList, CreateProject)) return restored.toList()
    if (restored.size in 3..4 && restored.take(2) == listOf(Home, ProjectList)) {
        val detail = restored[2] as? ProjectDetail ?: return listOf(Home)
        if (!StoredValueValidation.isUuid(detail.id)) return listOf(Home)
        if (restored.size == 3) return restored.toList()
        val editor = restored.last()
        if ((editor is ProjectEdit && editor.id == detail.id) || (editor is ProjectMoneyEntry && editor.id == detail.id)) return restored.toList()
    }
    return listOf(Home)
}
