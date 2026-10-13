package com.cooper.lithic.app

import androidx.activity.compose.BackHandler
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
import com.cooper.lithic.feature.project.EndProjectDialog
import com.cooper.lithic.feature.project.MoneyEventCorrectionScreen
import com.cooper.lithic.feature.project.DeleteProjectDialog
import com.cooper.lithic.feature.project.ProjectHistoryScreen
import com.cooper.lithic.feature.project.ProjectEditorMode
import com.cooper.lithic.feature.subscription.SubscriptionViewModel
import com.cooper.lithic.feature.subscription.SubscriptionEditorKind
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import com.cooper.lithic.feature.subscription.SubscriptionEditorScreen
import com.cooper.lithic.feature.subscription.RenewalEditorScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
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
    subscriptionViewModel: SubscriptionViewModel? = null,
) {
    val owner = checkNotNull(LocalView.current.findViewTreeViewModelStoreOwner())
    val model = projectViewModel ?: remember(owner, state.repository) {
        ViewModelProvider(owner, viewModelFactory {
            initializer { ProjectViewModel(state.repository, createSavedStateHandle(), state.businessClock) }
        })[ProjectViewModel::class.java]
    }
    val projects by model.state.collectAsStateWithLifecycle()
    val subscriptionModel = subscriptionViewModel ?: remember(owner, state.subscriptionRepository) {
        ViewModelProvider(owner, viewModelFactory {
            initializer { SubscriptionViewModel(state.subscriptionRepository, createSavedStateHandle(), state.businessClock) }
        })[SubscriptionViewModel::class.java]
    }
    val subscriptions by subscriptionModel.state.collectAsStateWithLifecycle()
    val backStack = rememberNavBackStack(Home)
    val routes = normalizeBackStack(backStack.toList())
    var pendingLeave by remember { mutableStateOf<List<NavKey>?>(null) }
    var pendingWarning by remember { mutableStateOf(false) }
    fun navigate(target: List<NavKey>) {
        if (backStack.toList() != target) { backStack.clear(); backStack.addAll(target) }
    }
    fun requestLeave(target: List<NavKey>) {
        when (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision())) {
            LeaveDecision.ALLOW -> { model.discardForm(); subscriptionModel.discardForm(); navigate(target) }
            LeaveDecision.CONFIRM_DISCARD -> pendingLeave = target
            LeaveDecision.BLOCK_PENDING -> pendingWarning = true
        }
    }
    val onBack: () -> Unit = { if (routes.size > 1) requestLeave(routes.dropLast(1)) }
    SideEffect { if (backStack.toList() != routes) navigate(routes) }
    LaunchedEffect(routes.last()) {
        when (val route = routes.last()) {
            ProjectList -> model.loadList()
            ProjectHistory -> model.loadHistory()
            CreateProject -> model.startCreate()
            is ProjectDetail -> model.openDetail(route.id)
            is ProjectEdit -> model.startEdit(route.id)
            is ProjectMoneyEntry -> model.startMoney(route.id, route.direction)
            is ProjectEnd -> model.startEnd(route.id)
            is ProjectUpdateEnd -> model.startUpdateEnd(route.id)
            is ProjectMoneyCorrection -> model.startCorrectMoney(route.id, route.eventId)
            is ProjectDelete -> model.startDelete(route.id)
            SubscriptionList -> subscriptionModel.loadList()
            CreateSubscription -> subscriptionModel.startCreate()
            is SubscriptionDetail -> subscriptionModel.openDetail(route.id)
            is ConfirmSubscriptionRenewal -> subscriptionModel.startRenewal(route.id)
            else -> Unit
        }
    }
    val projectLeave = model.leaveDecision()
    val subscriptionLeave = subscriptionModel.leaveDecision()
    BackHandler(enabled = routes == listOf(Home) && globalLeaveDecision(projectLeave, subscriptionLeave) != LeaveDecision.ALLOW) {
        requestLeave(routes)
    }
    LaunchedEffect(projects.committedProjectId, subscriptionLeave) {
        projects.committedProjectId?.let { id ->
            if (subscriptionLeave == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            navigate(canonicalBackStack(ProjectDetail(id, projectOrigin(routes.last()))))
            model.consumeCommitted()
        }
    }
    LaunchedEffect(projects.deletedProjectId, subscriptionLeave) {
        projects.deletedProjectId?.let {
            if (subscriptionLeave == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            navigate(canonicalBackStack(if (projectOrigin(routes.last()) == ProjectOrigin.HISTORY) ProjectHistory else ProjectList))
            model.consumeDeleted()
        }
    }
    LaunchedEffect(subscriptions.committedSubscriptionId, projectLeave) {
        subscriptions.committedSubscriptionId?.let { id ->
            if (projectLeave == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            navigate(canonicalBackStack(SubscriptionDetail(id)))
            subscriptionModel.consumeCommitted()
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, model, subscriptionModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { model.refresh(); subscriptionModel.refresh(); onRetryLedger() } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val currentPrimary = when {
        Review in routes -> Review
        routes.last() == Settings -> Settings
        else -> Home
    }
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
                    entry<Home> { HomeShell(state.projectCount, onProjectList = { requestLeave(canonicalBackStack(ProjectList)) }, onCreateProject = { requestLeave(listOf(Home, CreateProject)) },
                        subscriptionCount = state.subscriptionCount, onSubscriptionList = { requestLeave(canonicalBackStack(SubscriptionList)) }, onCreateSubscription = { requestLeave(listOf(Home, CreateSubscription)) }) }
                    entry<Review> { ReviewShell(state.projectCount, onProjectHistory = { requestLeave(canonicalBackStack(ProjectHistory)) }) }
                    entry<Settings> { SettingsShell(state.recordZone) }
                    entry<ProjectList> {
                        ProjectListScreen(projects.list, onBack, onCreate = { requestLeave(listOf(Home, ProjectList, CreateProject)) },
                            onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it))) }, onLoadMore = model::loadMore, onRetry = model::retryRead)
                    }
                    entry<ProjectDetail> { route ->
                        ProjectDetailScreen(projects.detail, onBack, onEdit = { requestLeave(canonicalBackStack(ProjectEdit(route.id, route.origin))) },
                            onAddMoney = { requestLeave(canonicalBackStack(ProjectMoneyEntry(route.id, it, route.origin))) }, onRetry = model::retryRead,
                            onEnd = { requestLeave(canonicalBackStack(ProjectEnd(route.id, route.origin))) },
                            onUpdateEnd = { requestLeave(canonicalBackStack(ProjectUpdateEnd(route.id, route.origin))) },
                            onCorrectMoney = { requestLeave(canonicalBackStack(ProjectMoneyCorrection(route.id, it, route.origin))) },
                            onDelete = { requestLeave(canonicalBackStack(ProjectDelete(route.id, route.origin))) },
                            backLabel = if (route.origin == ProjectOrigin.HISTORY) "返回历史档案" else "返回项目列表")
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
                    entry<ProjectHistory> {
                        ProjectHistoryScreen(projects.history, onBack, { requestLeave(canonicalBackStack(ProjectDetail(it, ProjectOrigin.HISTORY))) }, model::loadMoreHistory, model::retryRead)
                    }
                    entry<ProjectEnd> {
                        projects.endEditor?.let { editor -> EndProjectDialog(editor, onBack, model::changeEndedOn, model::changeEndReason,
                            model::changeRecordRecovery, model::changeRecoveryAmount, model::changeRecoveryOn, model::changeRecoveryDescription, model::save, model::retryRead) }
                    }
                    entry<ProjectUpdateEnd> {
                        projects.endEditor?.let { editor -> EndProjectDialog(editor, onBack, model::changeEndedOn, model::changeEndReason,
                            model::changeRecordRecovery, model::changeRecoveryAmount, model::changeRecoveryOn, model::changeRecoveryDescription, model::save, model::retryRead) }
                    }
                    entry<ProjectMoneyCorrection> {
                        projects.moneyCorrection?.let { editor -> MoneyEventCorrectionScreen(editor, onBack, model::changeAmount, model::changeOccurredOn, model::changeDescription, model::save, model::retryRead) }
                    }
                    entry<ProjectDelete> {
                        projects.deleteConfirmation?.let { editor -> DeleteProjectDialog(editor, onBack, model::save, model::retryRead) }
                    }
                    entry<SubscriptionList> {
                        SubscriptionListScreen(subscriptions.list, onBack, { requestLeave(listOf(Home, SubscriptionList, CreateSubscription)) },
                            { requestLeave(canonicalBackStack(SubscriptionDetail(it))) }, subscriptionModel::loadMore, subscriptionModel::retryRead)
                    }
                    entry<CreateSubscription> {
                        subscriptions.editor?.let { editor -> SubscriptionEditorScreen(editor, onBack, subscriptionModel::changeName,
                            subscriptionModel::changeCategory, subscriptionModel::changeNote, subscriptionModel::changeAmount, subscriptionModel::changeCycle,
                            subscriptionModel::changePaidOn, subscriptionModel::changeEffectiveFrom, subscriptionModel::changePaidUntil,
                            subscriptionModel::changeReminderDue, subscriptionModel::save, subscriptionModel::retryRead) }
                    }
                    entry<SubscriptionDetail> {
                        SubscriptionDetailScreen(subscriptions.detail, onBack, { requestLeave(canonicalBackStack(ConfirmSubscriptionRenewal(it.id))) },
                            subscriptionModel::loadMoreHistory, subscriptionModel::retryRead)
                    }
                    entry<ConfirmSubscriptionRenewal> {
                        subscriptions.editor?.let { editor -> RenewalEditorScreen(editor, onBack, subscriptionModel::changeAmount, subscriptionModel::changeCycle,
                            subscriptionModel::changePaidOn, subscriptionModel::changeEffectiveFrom, subscriptionModel::changePaidUntil,
                            subscriptionModel::changeReminderDue, subscriptionModel::save, subscriptionModel::retryRead) }
                    }
                })
        }
    }
    pendingLeave?.let { target ->
        AlertDialog(onDismissRequest = { pendingLeave = null }, modifier = Modifier.testTag("discard-dialog"), title = { Text("放弃未保存修改？") },
            text = { Text("输入尚未保存。继续编辑会保留这些内容。") },
            confirmButton = { TextButton(onClick = {
                pendingLeave = null
                if (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision()) == LeaveDecision.BLOCK_PENDING) pendingWarning = true
                else { model.discardForm(); subscriptionModel.discardForm(); navigate(target) }
            }, modifier = Modifier.testTag("discard-confirm")) { Text("放弃修改") } },
            dismissButton = { TextButton(onClick = { pendingLeave = null }, modifier = Modifier.testTag("discard-keep")) { Text("继续编辑") } })
    }
    if (pendingWarning) {
        AlertDialog(onDismissRequest = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-dialog"), title = { Text("请先核对保存结果") },
            text = { Text("提交正在进行或结果尚未确认。原始输入已保留，请返回表单核对并重试同一提交。") },
            confirmButton = { TextButton(onClick = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-keep")) { Text("继续核对") } },
            dismissButton = {
                val origin = projectOrigin(routes.last())
                val projectRecovery: NavKey? = if (projectLeave == LeaveDecision.BLOCK_PENDING) when {
                    projects.editor != null -> if (projects.editor!!.mode == ProjectEditorMode.CREATE) CreateProject else ProjectEdit(projects.editor!!.projectId, origin)
                    projects.moneyEditor != null -> ProjectMoneyEntry(projects.moneyEditor!!.projectId, projects.moneyEditor!!.direction, origin)
                    projects.endEditor != null -> if (projects.endEditor!!.correcting) ProjectUpdateEnd(projects.endEditor!!.projectId, origin) else ProjectEnd(projects.endEditor!!.projectId, origin)
                    projects.moneyCorrection != null -> ProjectMoneyCorrection(projects.moneyCorrection!!.projectId, projects.moneyCorrection!!.eventId, origin)
                    projects.deleteConfirmation != null -> ProjectDelete(projects.deleteConfirmation!!.projectId, origin)
                    else -> null
                } else null
                val subscriptionRecovery: NavKey? = if (subscriptionLeave == LeaveDecision.BLOCK_PENDING) subscriptions.editor?.let {
                    if (it.kind == SubscriptionEditorKind.CREATE) CreateSubscription else ConfirmSubscriptionRenewal(it.subscriptionId)
                } else null
                val recoveryTargets = listOfNotNull(projectRecovery, subscriptionRecovery).filter { it != routes.last() }
                Column { recoveryTargets.forEach { recovery ->
                    TextButton(onClick = { pendingWarning = false; navigate(canonicalBackStack(recovery)) },
                        modifier = Modifier.testTag(if (recoveryTargets.size == 1) "pending-submit-return-to-form" else if (recovery == subscriptionRecovery) "pending-submit-return-to-subscription" else "pending-submit-return-to-project")) {
                        Text(if (recovery == subscriptionRecovery) "返回待核对付款" else "返回待核对项目")
                    }
                } }
            })
    }
}
