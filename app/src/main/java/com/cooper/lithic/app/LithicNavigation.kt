package com.cooper.lithic.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.MoneyEventEditor
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectListScreen
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.settings.SettingsShell
import kotlinx.serialization.Serializable

/** Retained under its original name so previously saved M04 state can be decoded. */
@Serializable
internal data object Skeleton : NavKey

@Composable
internal fun LithicNavigation(
    state: LedgerUiState.Ready,
    modifier: Modifier = Modifier,
    projectViewModel: ProjectViewModel? = null,
    onRetryLedger: () -> Unit = {},
) {
    val owner = checkNotNull(LocalView.current.findViewTreeViewModelStoreOwner())
    val model = projectViewModel ?: remember(owner, state.repository) {
        ViewModelProvider(owner, viewModelFactory {
            initializer { ProjectViewModel(state.repository, createSavedStateHandle(), state.businessClock) }
        })[ProjectViewModel::class.java]
    }
    val projects by model.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Home)
    val routes = normalizeBackStack(backStack.toList())
    var pendingLeave by remember { mutableStateOf<List<NavKey>?>(null) }
    var pendingWarning by remember { mutableStateOf(false) }
    fun navigate(target: List<NavKey>) {
        if (backStack.toList() != target) { backStack.clear(); backStack.addAll(target) }
    }
    fun requestLeave(target: List<NavKey>) {
        when (model.leaveDecision()) {
            LeaveDecision.ALLOW -> { model.discardForm(); navigate(target) }
            LeaveDecision.CONFIRM_DISCARD -> pendingLeave = target
            LeaveDecision.BLOCK_PENDING -> pendingWarning = true
        }
    }
    val onBack: () -> Unit = { if (routes.size > 1) requestLeave(routes.dropLast(1)) }
    SideEffect { if (backStack.toList() != routes) navigate(routes) }
    LaunchedEffect(routes.last()) {
        when (val route = routes.last()) {
            ProjectList -> model.loadList()
            CreateProject -> model.startCreate()
            is ProjectDetail -> model.openDetail(route.id)
            is ProjectEdit -> model.startEdit(route.id)
            is ProjectMoneyEntry -> model.startMoney(route.id, route.direction)
            else -> Unit
        }
    }
    LaunchedEffect(projects.committedProjectId) {
        projects.committedProjectId?.let { id ->
            navigate(canonicalBackStack(ProjectDetail(id)))
            model.consumeCommitted()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, model) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { model.refresh(); onRetryLedger() } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val currentPrimary = when (routes.last()) { Review -> Review; Settings -> Settings; else -> Home }
    Scaffold(modifier = modifier.fillMaxSize().testTag("primary_navigation"), contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val destinations: List<Pair<NavKey, String>> = listOf(Home to "现在", Review to "回看", Settings to "设置")
                destinations.forEach { (destination, label) ->
                    NavigationBarItem(selected = currentPrimary == destination, onClick = { requestLeave(canonicalBackStack(destination)) },
                        icon = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(when (destination) { Home -> "tab-now"; Review -> "tab-review"; else -> "tab-settings" }))
                }
            }
        }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
            state.readError?.let { error ->
                Text(error, Modifier.padding(16.dp).testTag("ledger-refresh-error"))
                TextButton(onClick = onRetryLedger, modifier = Modifier.heightIn(min = 48.dp).testTag("ledger-retry-read")) { Text("重试读取") }
            }
            NavDisplay(backStack = routes, modifier = Modifier.weight(1f), onBack = onBack,
                entryProvider = entryProvider<NavKey> {
                    entry<Home> { HomeShell(state.projectCount, onProjectList = { navigate(canonicalBackStack(ProjectList)) }, onCreateProject = { navigate(listOf(Home, CreateProject)) }) }
                    entry<Review> { ReviewShell(state.projectCount) }
                    entry<Settings> { SettingsShell(state.recordZone) }
                    entry<ProjectList> {
                        ProjectListScreen(projects.list, onBack, onCreate = { navigate(listOf(Home, ProjectList, CreateProject)) },
                            onOpenProject = { navigate(canonicalBackStack(ProjectDetail(it))) }, onLoadMore = model::loadMore, onRetry = model::retryRead)
                    }
                    entry<ProjectDetail> { route ->
                        ProjectDetailScreen(projects.detail, onBack, onEdit = { navigate(canonicalBackStack(ProjectEdit(route.id))) },
                            onAddMoney = { navigate(canonicalBackStack(ProjectMoneyEntry(route.id, it))) }, onRetry = model::retryRead)
                    }
                    entry<CreateProject> {
                        projects.editor?.let { editor -> ProjectEditorScreen(editor, onBack, model::changeName, model::changeStartedOn, model::changeInitialAmount,
                            model::changeInitialPaidOn, model::changeCategory, model::changeNote, model::save, onRetryRead = model::retryRead) }
                    }
                    entry<ProjectEdit> {
                        projects.editor?.let { editor -> ProjectEditorScreen(editor, onBack, model::changeName, model::changeStartedOn, model::changeInitialAmount,
                            model::changeInitialPaidOn, model::changeCategory, model::changeNote, model::save, onRetryRead = model::retryRead) }
                    }
                    entry<ProjectMoneyEntry> {
                        projects.moneyEditor?.let { editor -> MoneyEventEditor(editor, onBack, model::changeDirection, model::changeAmount,
                            model::changeOccurredOn, model::changeDescription, model::save, onRetryRead = model::retryRead) }
                    }
                })
        }
    }
    pendingLeave?.let { target ->
        AlertDialog(onDismissRequest = { pendingLeave = null }, modifier = Modifier.testTag("discard-dialog"), title = { Text("放弃未保存修改？") },
            text = { Text("输入尚未保存。继续编辑会保留这些内容。") },
            confirmButton = { TextButton(onClick = { pendingLeave = null; model.discardForm(); navigate(target) }, modifier = Modifier.testTag("discard-confirm")) { Text("放弃修改") } },
            dismissButton = { TextButton(onClick = { pendingLeave = null }, modifier = Modifier.testTag("discard-keep")) { Text("继续编辑") } })
    }
    if (pendingWarning) {
        AlertDialog(onDismissRequest = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-dialog"), title = { Text("请先核对保存结果") },
            text = { Text("提交正在进行或结果尚未确认。原始输入已保留，请返回表单核对并重试同一提交。") },
            confirmButton = { TextButton(onClick = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-keep")) { Text("返回表单") } })
    }
}
