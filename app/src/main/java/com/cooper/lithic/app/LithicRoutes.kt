package com.cooper.lithic.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
internal data object Home : NavKey

@Serializable
internal data object Review : NavKey

@Serializable
internal data object Settings : NavKey

@Serializable
internal data object ProjectList : NavKey

internal fun canonicalBackStack(destination: NavKey): List<NavKey> = when (destination) {
    Home -> listOf(Home)
    Review, Settings, ProjectList -> listOf(Home, destination)
    else -> listOf(Home)
}

/** Only decoded, known routes can be presented; old Skeleton and invalid stacks return home. */
internal fun normalizeBackStack(restored: List<NavKey>): List<NavKey> {
    if (restored == listOf(Home)) return listOf(Home)
    if (restored.size == 2 && restored.first() == Home &&
        restored.last() in listOf(Review, Settings, ProjectList)) {
        return restored.toList()
    }
    return listOf(Home)
}
