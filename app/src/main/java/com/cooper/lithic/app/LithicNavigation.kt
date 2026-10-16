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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.cooper.lithic.core.design.LithicIcon
import com.cooper.lithic.feature.media.CoverPreviewProvider
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
import com.cooper.lithic.feature.project.CoverPicker
import com.cooper.lithic.feature.media.CoverViewModel
import com.cooper.lithic.feature.media.CoverUiState
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.feature.subscription.SubscriptionViewModel
import com.cooper.lithic.feature.subscription.SubscriptionEditorKind
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import com.cooper.lithic.feature.subscription.SubscriptionEditorScreen
import com.cooper.lithic.feature.subscription.RenewalEditorScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionHistoryScreen
import com.cooper.lithic.feature.subscription.SubscriptionStateDialog
import com.cooper.lithic.feature.subscription.SubscriptionUiState
import com.cooper.lithic.feature.subscription.SubscriptionStateEditorKind
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.settings.SettingsShell
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    coverViewModel: CoverViewModel? = null,
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
    val coverModel = coverViewModel ?: state.mediaRepository?.let { repository ->
        remember(owner, repository) {
            ViewModelProvider(owner, viewModelFactory {
                initializer { CoverViewModel(repository, createSavedStateHandle()) }
            })[CoverViewModel::class.java]
        }
    }
    val covers = coverModel?.state?.collectAsStateWithLifecycle()?.value ?: CoverUiState()
    val cleanupScope = rememberCoroutineScope()
    var cleanupMessage by remember { mutableStateOf<String?>(null) }
    fun cleanupMedia() {
        val repository = state.mediaRepository ?: return
        cleanupScope.launch {
            try {
                when (val result = withContext(Dispatchers.IO) { repository.removeUnreferenced() }) {
                    is MediaResult.Success -> cleanupMessage = if (result.value.failures.isEmpty()) null
                        else "记录已保存；部分未使用封面暂未清理，可稍后重试。"
                    is MediaResult.Failure -> cleanupMessage = "记录已保存；未使用封面暂未清理，可稍后重试。"
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { cleanupMessage = "记录已保存；未使用封面暂未清理，可稍后重试。" }
        }
    }
    val backStack = rememberNavBackStack(Home)
    val observedRoutes = backStack.toList()
    val routes = normalizeBackStack(observedRoutes)
    var pendingLeave by remember { mutableStateOf<List<NavKey>?>(null) }
    var pendingWarning by remember { mutableStateOf(false) }
    fun navigate(target: List<NavKey>) {
        if (backStack.toList() != target) { backStack.clear(); backStack.addAll(target) }
    }
    fun requestLeave(target: List<NavKey>) {
        when (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision(), coverModel?.leaveDecision() ?: LeaveDecision.ALLOW)) {
            LeaveDecision.ALLOW -> { model.discardForm(); subscriptionModel.discardForm(); coverModel?.discardForm(); navigate(target) }
            LeaveDecision.CONFIRM_DISCARD -> pendingLeave = target
            LeaveDecision.BLOCK_PENDING -> pendingWarning = true
        }
    }
    val onBack: () -> Unit = { if (routes.size > 1) requestLeave(routes.dropLast(1)) }
    SideEffect {
        // Repair only the stack this composition observed. A commit effect may already
        // have navigated elsewhere; an older normalization must not overwrite it.
        if (observedRoutes != routes && backStack.toList() == observedRoutes) navigate(routes)
    }
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
            is ProjectCover -> coverModel?.start(CoverTarget(CoverTargetType.PROJECT, route.id), route.origin.name)
            SubscriptionList -> subscriptionModel.loadList()
            SubscriptionHistory -> subscriptionModel.loadStoppedHistory()
            CreateSubscription -> subscriptionModel.startCreate()
            is SubscriptionDetail -> subscriptionModel.openDetail(route.id)
            is ConfirmSubscriptionRenewal -> subscriptionModel.startRenewal(route.id, route.origin.name)
            is ChangeSubscriptionState -> subscriptionModel.startStateAction(route.id, when (route.action) {
                SubscriptionAction.PAUSE -> SubscriptionStateAction.PAUSE
                SubscriptionAction.RESUME -> SubscriptionStateAction.RESUME
                SubscriptionAction.REOPEN -> SubscriptionStateAction.REOPEN
                SubscriptionAction.STOP -> SubscriptionStateAction.STOP_AT_END
            }, route.origin.name)
            is CorrectSubscriptionState -> subscriptionModel.startCorrectState(route.id, route.stateId, route.origin.name)
            is CancelSubscriptionStop -> subscriptionModel.startCancelStop(route.id, route.stateId, route.origin.name)
            is CorrectSubscriptionRenewal -> subscriptionModel.startCorrectRenewal(route.id, route.renewalId, route.origin.name)
            is DeleteSubscriptionRenewal -> subscriptionModel.startDeleteRenewal(route.id, route.renewalId, route.origin.name)
            is SubscriptionCover -> coverModel?.start(CoverTarget(CoverTargetType.SUBSCRIPTION, route.id), route.origin.name)
            else -> Unit
        }
    }
    val projectLeave = model.leaveDecision()
    val subscriptionLeave = subscriptionModel.leaveDecision()
    val coverLeave = coverModel?.leaveDecision() ?: LeaveDecision.ALLOW
    BackHandler(enabled = routes == listOf(Home) && globalLeaveDecision(projectLeave, subscriptionLeave, coverLeave) != LeaveDecision.ALLOW) {
        requestLeave(routes)
    }
    LaunchedEffect(projects.committedProjectId, subscriptionLeave, coverLeave) {
        projects.committedProjectId?.let { id ->
            val other = globalLeaveDecision(LeaveDecision.ALLOW, subscriptionLeave, coverLeave)
            if (other == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val target = canonicalBackStack(ProjectDetail(id, projectOrigin(routes.last())))
            if (other == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            model.consumeCommitted()
        }
    }
    LaunchedEffect(projects.deletedProjectId, subscriptionLeave, coverLeave) {
        projects.deletedProjectId?.let {
            val other = globalLeaveDecision(LeaveDecision.ALLOW, subscriptionLeave, coverLeave)
            if (other == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val target = canonicalBackStack(if (projectOrigin(routes.last()) == ProjectOrigin.HISTORY) ProjectHistory else ProjectList)
            if (other == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            model.consumeDeleted()
            cleanupMedia()
        }
    }
    LaunchedEffect(subscriptions.committedSubscriptionId, projectLeave, coverLeave) {
        subscriptions.committedSubscriptionId?.let { id ->
            val other = globalLeaveDecision(projectLeave, LeaveDecision.ALLOW, coverLeave)
            if (other == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val origin = SubscriptionOrigin.entries.firstOrNull { it.name == subscriptions.committedOrigin } ?: SubscriptionOrigin.CURRENT
            val target = canonicalBackStack(SubscriptionDetail(id, origin))
            if (other == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            subscriptionModel.consumeCommitted()
        }
    }
    LaunchedEffect(covers.committedTarget, projectLeave, subscriptionLeave) {
        covers.committedTarget?.let { committed ->
            val other = globalLeaveDecision(projectLeave, subscriptionLeave)
            if (other == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val target = canonicalBackStack(if (committed.type == CoverTargetType.PROJECT)
                ProjectDetail(committed.id, ProjectOrigin.entries.firstOrNull { it.name == covers.committedOrigin } ?: ProjectOrigin.CURRENT)
                else SubscriptionDetail(committed.id, SubscriptionOrigin.entries.firstOrNull { it.name == covers.committedOrigin } ?: SubscriptionOrigin.CURRENT))
            if (other == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target); coverModel?.consumeCommitted()
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
    CoverPreviewProvider(state.mediaRepository) {
    Scaffold(modifier = modifier.fillMaxSize().testTag("primary_navigation"), contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                val destinations: List<Pair<NavKey, String>> = listOf(Home to "现在", Review to "回看", Settings to "设置")
                destinations.forEach { (destination, label) ->
                    NavigationBarItem(selected = currentPrimary == destination, onClick = { requestLeave(canonicalBackStack(destination)) },
                        icon = { LithicIcon(when (destination) { Home -> LithicIcon.Now; Review -> LithicIcon.Review; else -> LithicIcon.Settings },
                            null, tint = if (currentPrimary == destination) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) },
                        label = { Text(label, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(when (destination) { Home -> "tab-now"; Review -> "tab-review"; else -> "tab-settings" }))
                }
            }
        }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
            state.readError?.let { error ->
                Text(error, Modifier.padding(16.dp).testTag("ledger-refresh-error"))
                TextButton(onClick = onRetryLedger, modifier = Modifier.heightIn(min = 48.dp).testTag("ledger-retry-read")) { Text("重试读取") }
            }
            cleanupMessage?.let { message ->
                Text(message, Modifier.padding(16.dp).testTag("media-cleanup-warning"))
                TextButton({ cleanupMedia() }, Modifier.heightIn(min = 48.dp).testTag("media-cleanup-retry")) { Text("重试清理未使用封面") }
            }
            NavDisplay(backStack = routes, modifier = Modifier.weight(1f), onBack = onBack,
                entryProvider = entryProvider<NavKey> {
                    entry<Home> { HomeShell(state.projectCount, onProjectList = { requestLeave(canonicalBackStack(ProjectList)) }, onCreateProject = { requestLeave(listOf(Home, CreateProject)) },
                        subscriptionCount = state.subscriptionCount, onSubscriptionList = { requestLeave(canonicalBackStack(SubscriptionList)) }, onCreateSubscription = { requestLeave(listOf(Home, CreateSubscription)) },
                        preview = state.homePreview, refreshing = state.isRefreshing, hasReadError = state.readError != null,
                        onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it))) },
                        onOpenSubscription = { requestLeave(canonicalBackStack(SubscriptionDetail(it))) },
                        onProjectHistory = { requestLeave(canonicalBackStack(ProjectHistory)) },
                        onSubscriptionHistory = { requestLeave(canonicalBackStack(SubscriptionHistory)) },
                        mediaRepository = state.mediaRepository) }
                    entry<Review> { ReviewShell(state.projectCount, onProjectHistory = { requestLeave(canonicalBackStack(ProjectHistory)) },
                        onSubscriptionHistory = { requestLeave(canonicalBackStack(SubscriptionHistory)) }) }
                    entry<Settings> { SettingsShell(state.recordZone) }
                    entry<ProjectList> {
                        ProjectListScreen(projects.list, onBack, onCreate = { requestLeave(listOf(Home, ProjectList, CreateProject)) },
                            onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it))) }, onLoadMore = model::loadMore, onRetry = model::retryRead, mediaRepository = state.mediaRepository)
                    }
                    entry<ProjectDetail> { route ->
                        ProjectDetailScreen(projects.detail, onBack, onEdit = { requestLeave(canonicalBackStack(ProjectEdit(route.id, route.origin))) },
                            onAddMoney = { requestLeave(canonicalBackStack(ProjectMoneyEntry(route.id, it, route.origin))) }, onRetry = model::retryRead,
                            onEnd = { requestLeave(canonicalBackStack(ProjectEnd(route.id, route.origin))) },
                            onUpdateEnd = { requestLeave(canonicalBackStack(ProjectUpdateEnd(route.id, route.origin))) },
                            onCorrectMoney = { requestLeave(canonicalBackStack(ProjectMoneyCorrection(route.id, it, route.origin))) },
                            onDelete = { requestLeave(canonicalBackStack(ProjectDelete(route.id, route.origin))) },
                            backLabel = if (route.origin == ProjectOrigin.HISTORY) "返回历史档案" else "返回项目列表",
                            mediaRepository = state.mediaRepository,
                            onEditCover = { if (coverModel != null) requestLeave(canonicalBackStack(ProjectCover(route.id, route.origin))) })
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
                        ProjectHistoryScreen(projects.history, onBack, { requestLeave(canonicalBackStack(ProjectDetail(it, ProjectOrigin.HISTORY))) }, model::loadMoreHistory, model::retryRead, mediaRepository = state.mediaRepository)
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
                    entry<ProjectCover> {
                        covers.editor?.let { editor -> coverModel?.let { CoverPicker(editor, it, state.mediaRepository, onBack) } }
                    }
                    entry<SubscriptionList> {
                        SubscriptionListScreen(subscriptions.list, onBack, { requestLeave(listOf(Home, SubscriptionList, CreateSubscription)) },
                            { requestLeave(canonicalBackStack(SubscriptionDetail(it))) }, subscriptionModel::loadMore, subscriptionModel::retryRead, mediaRepository = state.mediaRepository)
                    }
                    entry<CreateSubscription> {
                        subscriptions.editor?.let { editor -> SubscriptionEditorScreen(editor, onBack, subscriptionModel::changeName,
                            subscriptionModel::changeCategory, subscriptionModel::changeNote, subscriptionModel::changeAmount, subscriptionModel::changeCycle,
                            subscriptionModel::changePaidOn, subscriptionModel::changeEffectiveFrom, subscriptionModel::changePaidUntil,
                            subscriptionModel::changeReminderDue, subscriptionModel::save, subscriptionModel::retryRead) }
                    }
                    entry<SubscriptionHistory> {
                        SubscriptionHistoryScreen(subscriptions.stoppedHistory, onBack,
                            { requestLeave(canonicalBackStack(SubscriptionDetail(it, SubscriptionOrigin.HISTORY))) },
                            subscriptionModel::loadMoreStoppedHistory, subscriptionModel::retryRead, mediaRepository = state.mediaRepository)
                    }
                    entry<SubscriptionDetail> { route ->
                        fun stateAction(action: SubscriptionAction) = requestLeave(canonicalBackStack(ChangeSubscriptionState(route.id, action, route.origin)))
                        SubscriptionDetailScreen(subscriptions.detail, onBack, { requestLeave(canonicalBackStack(ConfirmSubscriptionRenewal(route.id, route.origin))) },
                            subscriptionModel::loadMoreHistory, subscriptionModel::retryRead,
                            onPause = { stateAction(SubscriptionAction.PAUSE) }, onResume = { stateAction(SubscriptionAction.RESUME) },
                            onStop = { stateAction(SubscriptionAction.STOP) }, onReopen = { stateAction(SubscriptionAction.REOPEN) },
                            onCorrectState = { requestLeave(canonicalBackStack(CorrectSubscriptionState(route.id, it, route.origin))) },
                            onCancelStop = { requestLeave(canonicalBackStack(CancelSubscriptionStop(route.id, it, route.origin))) },
                            onCorrectRenewal = { requestLeave(canonicalBackStack(CorrectSubscriptionRenewal(route.id, it, route.origin))) },
                            onDeleteRenewal = { requestLeave(canonicalBackStack(DeleteSubscriptionRenewal(route.id, it, route.origin))) },
                            backLabel = if (route.origin == SubscriptionOrigin.HISTORY) "返回已停止订阅" else "返回订阅列表",
                            mediaRepository = state.mediaRepository,
                            onEditCover = { if (coverModel != null) requestLeave(canonicalBackStack(SubscriptionCover(route.id, route.origin))) })
                    }
                    entry<ConfirmSubscriptionRenewal> {
                        subscriptions.editor?.let { editor -> RenewalEditorScreen(editor, onBack, subscriptionModel::changeAmount, subscriptionModel::changeCycle,
                            subscriptionModel::changePaidOn, subscriptionModel::changeEffectiveFrom, subscriptionModel::changePaidUntil,
                            subscriptionModel::changeReminderDue, subscriptionModel::save, subscriptionModel::retryRead) }
                    }
                    entry<ChangeSubscriptionState> { SubscriptionOperationContent(subscriptions, subscriptionModel, onBack) }
                    entry<CorrectSubscriptionState> { SubscriptionOperationContent(subscriptions, subscriptionModel, onBack) }
                    entry<CancelSubscriptionStop> { SubscriptionOperationContent(subscriptions, subscriptionModel, onBack) }
                    entry<CorrectSubscriptionRenewal> { SubscriptionOperationContent(subscriptions, subscriptionModel, onBack) }
                    entry<DeleteSubscriptionRenewal> { SubscriptionOperationContent(subscriptions, subscriptionModel, onBack) }
                    entry<SubscriptionCover> {
                        covers.editor?.let { editor -> coverModel?.let { CoverPicker(editor, it, state.mediaRepository, onBack) } }
                    }
                })
        }
    }
    } // Cover previews share the current repository lifetime across NavDisplay entries.
    pendingLeave?.let { target ->
        AlertDialog(onDismissRequest = { pendingLeave = null }, modifier = Modifier.testTag("discard-dialog"), title = { Text("放弃未保存修改？") },
            text = { Text("输入尚未保存。继续编辑会保留这些内容。", Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = {
                pendingLeave = null
                if (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision(), coverModel?.leaveDecision() ?: LeaveDecision.ALLOW) == LeaveDecision.BLOCK_PENDING) pendingWarning = true
                else { val deletedCommitted = projects.deletedProjectId != null
                    model.consumeCommitted(); subscriptionModel.consumeCommitted(); model.consumeDeleted()
                    coverModel?.consumeCommitted(); model.discardForm(); subscriptionModel.discardForm(); coverModel?.discardForm(); navigate(target)
                    if (deletedCommitted) cleanupMedia() }
            }, modifier = Modifier.heightIn(min = 48.dp).testTag("discard-confirm")) { Text("放弃修改") } },
            dismissButton = { TextButton(onClick = { pendingLeave = null }, modifier = Modifier.heightIn(min = 48.dp).testTag("discard-keep")) { Text("继续编辑") } })
    }
    if (pendingWarning) {
        AlertDialog(onDismissRequest = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-dialog"), title = { Text("请先核对保存结果") },
            text = { Text("提交正在进行或结果尚未确认。原始输入已保留，请返回原操作核对并重试同一保存。", Modifier.verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { pendingWarning = false }, modifier = Modifier.heightIn(min = 48.dp).testTag("pending-submit-keep")) { Text("继续核对") } },
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
                val subscriptionRecovery: NavKey? = if (subscriptionLeave == LeaveDecision.BLOCK_PENDING) subscriptionRecoveryRoute(subscriptions) else null
                val coverRecovery: NavKey? = if (coverLeave == LeaveDecision.BLOCK_PENDING) covers.editor?.let { editor ->
                    if (editor.target.type == CoverTargetType.PROJECT) ProjectCover(editor.target.id,
                        ProjectOrigin.entries.firstOrNull { it.name == editor.origin } ?: ProjectOrigin.CURRENT)
                    else SubscriptionCover(editor.target.id, SubscriptionOrigin.entries.firstOrNull { it.name == editor.origin } ?: SubscriptionOrigin.CURRENT)
                } else null
                val recoveryTargets = listOfNotNull(projectRecovery, subscriptionRecovery, coverRecovery).filter { it != routes.last() }
                Column { recoveryTargets.forEach { recovery ->
                    TextButton(onClick = { pendingWarning = false; navigate(canonicalBackStack(recovery)) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag(if (recoveryTargets.size == 1) "pending-submit-return-to-form" else if (recovery == coverRecovery) "pending-submit-return-to-cover" else if (recovery == subscriptionRecovery) "pending-submit-return-to-subscription" else "pending-submit-return-to-project")) {
                        Text(if (recovery == coverRecovery) "返回待核对封面" else if (recovery == subscriptionRecovery) "返回待核对订阅记录" else "返回待核对项目")
                    }
                } }
            })
    }
}

@Composable
private fun SubscriptionOperationContent(state: SubscriptionUiState, model: SubscriptionViewModel, onBack: () -> Unit) {
    state.editor?.let { editor ->
        RenewalEditorScreen(editor, onBack, model::changeAmount, model::changeCycle, model::changePaidOn, model::changeEffectiveFrom,
            model::changePaidUntil, model::changeReminderDue, model::save, model::retryRead)
    }
    state.stateEditor?.let { editor ->
        SubscriptionStateDialog(editor, onBack, model::changeStateEffectiveOn, model::changeStateReason, model::changeStateNote,
            model::changeStateValue, model::changeStopEarly, model::save, model::retryRead)
    }
}

private fun subscriptionRecoveryRoute(state: SubscriptionUiState): NavKey? {
    state.stateEditor?.let { editor ->
        val origin = SubscriptionOrigin.entries.firstOrNull { it.name == editor.origin } ?: SubscriptionOrigin.CURRENT
        return when (editor.kind) {
            SubscriptionStateEditorKind.STATE -> ChangeSubscriptionState(editor.subscriptionId, when (editor.action) {
                "PAUSE" -> SubscriptionAction.PAUSE; "RESUME" -> SubscriptionAction.RESUME; "REOPEN" -> SubscriptionAction.REOPEN; else -> SubscriptionAction.STOP
            }, origin)
            SubscriptionStateEditorKind.CORRECT_STATE -> CorrectSubscriptionState(editor.subscriptionId, editor.targetFactId, origin)
            SubscriptionStateEditorKind.CANCEL_STOP -> CancelSubscriptionStop(editor.subscriptionId, editor.targetFactId, origin)
            SubscriptionStateEditorKind.DELETE_RENEWAL -> DeleteSubscriptionRenewal(editor.subscriptionId, editor.targetFactId, origin)
        }
    }
    return state.editor?.let { editor ->
        val origin = SubscriptionOrigin.entries.firstOrNull { it.name == editor.origin } ?: SubscriptionOrigin.CURRENT
        when (editor.kind) {
            SubscriptionEditorKind.CREATE -> CreateSubscription
            SubscriptionEditorKind.RENEWAL -> ConfirmSubscriptionRenewal(editor.subscriptionId, origin)
            SubscriptionEditorKind.CORRECT_RENEWAL -> CorrectSubscriptionRenewal(editor.subscriptionId, editor.renewalId, origin)
            SubscriptionEditorKind.REENABLE -> ChangeSubscriptionState(editor.subscriptionId,
                if (editor.reenableAction == "RESUME") SubscriptionAction.RESUME else SubscriptionAction.REOPEN, origin)
        }
    }
}
