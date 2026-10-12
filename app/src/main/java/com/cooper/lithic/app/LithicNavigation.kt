package com.cooper.lithic.app

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.home.ProjectListShell
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.settings.SettingsShell
import kotlinx.serialization.Serializable

/** Retained under its original class name so M04 saved state can still be decoded. */
@Serializable
internal data object Skeleton : NavKey

@Composable
internal fun LithicNavigation(state: LedgerUiState.Ready, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Home)
    val routes = normalizeBackStack(backStack.toList())
    SideEffect {
        if (backStack.toList() != routes) {
            backStack.clear()
            backStack.addAll(routes)
        }
    }
    fun select(destination: NavKey) {
        val target = canonicalBackStack(destination)
        if (backStack.toList() != target) {
            backStack.clear()
            backStack.addAll(target)
        }
    }
    val onBack: () -> Unit = { if (routes.size > 1) select(Home) }
    val currentPrimary = when (routes.last()) {
        Review -> Review
        Settings -> Settings
        else -> Home
    }
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("primary_navigation"),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val destinations: List<Pair<NavKey, String>> = listOf(Home to "现在", Review to "回看", Settings to "设置")
                destinations.forEach { (destination, label) ->
                    val tag = when (destination) {
                        Home -> "tab-now"
                        Review -> "tab-review"
                        else -> "tab-settings"
                    }
                    NavigationBarItem(
                        selected = currentPrimary == destination,
                        onClick = { select(destination) },
                        icon = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(tag),
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = routes,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
            onBack = onBack,
            entryProvider = entryProvider<NavKey> {
                entry<Home> { HomeShell(state.snapshot.projects.size, onProjectList = { select(ProjectList) }) }
                entry<Review> { ReviewShell(state.snapshot.projects.size) }
                entry<Settings> { SettingsShell(state.recordZone) }
                entry<ProjectList> { ProjectListShell(state.snapshot.projects, onBack) }
            },
        )
    }
}
