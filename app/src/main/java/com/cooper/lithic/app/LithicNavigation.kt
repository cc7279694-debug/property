package com.cooper.lithic.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.home.SearchScreen
import com.cooper.lithic.feature.home.SearchViewModel
import com.cooper.lithic.feature.category.CategoryManagementScreen
import com.cooper.lithic.feature.category.CategoryViewModel
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.feature.project.LeaveDecision
import com.cooper.lithic.feature.project.MoneyEventEditor
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectListScreen
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.project.ReplacementViewModel
import com.cooper.lithic.feature.project.ReplacementComparisonScreen
import com.cooper.lithic.feature.project.TradeInEditorScreen
import com.cooper.lithic.feature.project.ReplacementFormKind
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
import com.cooper.lithic.feature.subscription.SubscriptionHistoryScreen
import com.cooper.lithic.feature.subscription.SubscriptionStateDialog
import com.cooper.lithic.feature.subscription.SubscriptionUiState
import com.cooper.lithic.feature.subscription.SubscriptionStateEditorKind
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.feature.review.LongTermReviewScreen
import com.cooper.lithic.feature.review.ReviewViewModel
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.review.TotalCostTrendScreen
import com.cooper.lithic.feature.review.AnnualReviewScreen
import com.cooper.lithic.feature.review.ReportAnalysisViewModel
import com.cooper.lithic.feature.category.CategoryAnalysisScreen
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.feature.review.TotalCostTrendViewModel
import com.cooper.lithic.feature.settings.SettingsShell
import com.cooper.lithic.feature.settings.AboutLithicEntry
import com.cooper.lithic.feature.settings.CalculationExplanationScreen
import com.cooper.lithic.core.design.UiFeedbackText
import com.cooper.lithic.feature.settings.NotificationSettingsEntry
import com.cooper.lithic.feature.settings.NotificationSettingsViewModel
import com.cooper.lithic.platform.notification.ReminderScheduling
import androidx.compose.ui.platform.LocalContext
import com.cooper.lithic.feature.settings.BackupExportUiState
import com.cooper.lithic.feature.settings.BackupRestoreUiState
import com.cooper.lithic.feature.settings.RestoreUiPhase
import com.cooper.lithic.feature.report.ItemTimelineViewModel
import com.cooper.lithic.feature.project.ProjectCostChart
import com.cooper.lithic.feature.subscription.SubscriptionPriceChart
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import com.cooper.lithic.domain.reminder.MilestoneCandidate
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.feature.reminder.MilestoneSourcesScreen
import com.cooper.lithic.feature.reminder.InvalidMilestoneNoticeScreen
import kotlinx.coroutines.flow.merge
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
    backupState: BackupExportUiState = BackupExportUiState(),
    onExportBackup: (() -> Unit)? = null,
    onCancelBackup: () -> Unit = {},
    restoreState: BackupRestoreUiState = BackupRestoreUiState(),
    onRestoreBackup: (() -> Unit)? = null,
    onCancelRestore: () -> Unit = {},
    onConfirmRestore: () -> Unit = {},
    onDiscardRestore: () -> Unit = {},
    milestoneRequest: MilestoneOpenRequest? = null,
    resolveMilestone: (suspend (MilestoneNoticeRef) -> List<MilestoneCandidate>)? = null,
) {
    val owner = checkNotNull(LocalView.current.findViewTreeViewModelStoreOwner())
    val generationScope = remember(owner) {
        ViewModelProvider(owner, viewModelFactory {
            initializer { GenerationUiScopeViewModel(createSavedStateHandle()) }
        })[GenerationUiScopeViewModel::class.java]
    }
    val session = generationScope.bind(state.generationId, state.repository)
    val model = projectViewModel ?: remember(generationScope, session, state.repository) {
        ViewModelProvider(generationScope, viewModelFactory {
            initializer { ProjectViewModel(state.repository, generationScope.savedState, state.businessClock) }
        })[ProjectViewModel::class.java]
    }
    val projects by model.state.collectAsStateWithLifecycle()
    val subscriptionModel = subscriptionViewModel ?: remember(generationScope, session, state.subscriptionRepository) {
        ViewModelProvider(generationScope, viewModelFactory {
            initializer { SubscriptionViewModel(state.subscriptionRepository, generationScope.savedState, state.businessClock) }
        })[SubscriptionViewModel::class.java]
    }
    val subscriptions by subscriptionModel.state.collectAsStateWithLifecycle()
    val categoryModel = state.categoryRepository?.let { repository ->
        remember(generationScope, session, repository) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { CategoryViewModel(repository) }
            })[CategoryViewModel::class.java]
        }
    }
    val categoryState = categoryModel?.state?.collectAsStateWithLifecycle()?.value
    val searchModel = state.searchRepository?.let { repository ->
        remember(generationScope, session, repository) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { SearchViewModel(repository) }
            })[SearchViewModel::class.java]
        }
    }
    val searchState = searchModel?.state?.collectAsStateWithLifecycle()?.value
    val replacementModel = state.replacementRepository?.let { repository ->
        remember(generationScope, session, repository, state.repository) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { ReplacementViewModel(repository, state.repository, generationScope.savedState, state.businessClock) }
            })[ReplacementViewModel::class.java]
        }
    }
    val replacements = replacementModel?.state?.collectAsStateWithLifecycle()?.value
    val backStack = key(session) { rememberNavBackStack(Home) }
    val routes = normalizeBackStack(backStack.toList())
    val context = LocalContext.current
    val notificationModel = if (NotificationSettings in routes) state.settingsStore?.let { store ->
        remember(generationScope, session, store) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { NotificationSettingsViewModel(store) {
                    if (context.applicationContext is LithicApplication) ReminderScheduling.request(context)
                } }
            })[NotificationSettingsViewModel::class.java]
        }
    } else null
    // Avoid full-ledger ranking until the user first opens review. The generation scope owns
    // the model; owner replacement clears it before any old result can be presented.
    val reviewModel = if (LongTermReview in routes) state.rankingRepository?.let { repository ->
        remember(generationScope, session, repository) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { ReviewViewModel(repository) }
            })[ReviewViewModel::class.java]
        }
    } else null
    val reviewState = reviewModel?.state?.collectAsStateWithLifecycle()?.value
    // Only an actual first visit creates the model. Keep that existing generation-owned
    // reference after leaving: an outgoing Scene must not receive a temporary null input.
    val trendReference = state.totalCostTimelineRepository?.let { repository ->
        remember(generationScope, session, repository) {
            lazy(LazyThreadSafetyMode.NONE) {
                ViewModelProvider(generationScope, viewModelFactory {
                    initializer { TotalCostTrendViewModel(repository, state.businessClock) }
                })[TotalCostTrendViewModel::class.java]
            }
        }
    }
    val trendModel = trendReference?.let { reference ->
        if (Review in routes || TotalCostTrend in routes || reference.isInitialized()) reference.value else null
    }
    val trendState = trendModel?.state?.collectAsStateWithLifecycle()?.value
    DisposableEffect(trendModel) { onDispose { trendModel?.pause() } }
    val analysisModel = if (routes.any { it == AnnualReview || it is CategoryAnalysis }) state.totalCostTimelineRepository?.let { repository ->
        remember(generationScope, session, repository) {
            ViewModelProvider(generationScope, viewModelFactory {
                initializer { ReportAnalysisViewModel(repository, state.businessClock) }
            })[ReportAnalysisViewModel::class.java]
        }
    } else null
    val analysisState = analysisModel?.state?.collectAsStateWithLifecycle()?.value
    DisposableEffect(analysisModel) { onDispose { analysisModel?.pause() } }
    val chartModel = if (routes.last() is ProjectCostHistory || routes.last() is SubscriptionPriceHistory) {
        state.itemTimelineRepository?.let { repository ->
            remember(generationScope, session, repository) {
                ViewModelProvider(generationScope, viewModelFactory {
                    initializer { ItemTimelineViewModel(repository, state.businessClock) }
                })[ItemTimelineViewModel::class.java]
            }
        }
    } else null
    val chartState = chartModel?.state?.collectAsStateWithLifecycle()?.value
    DisposableEffect(chartModel) { onDispose { chartModel?.pause() } }
    var pendingLeave by remember(session) { mutableStateOf<List<NavKey>?>(null) }
    var pendingWarning by remember(session) { mutableStateOf(false) }
    var handledNotice by remember(session) { mutableStateOf<Long?>(null) }
    var autoOpenNotice by remember(session) { mutableStateOf<MilestoneSources?>(null) }
    val milestoneChanges = remember(state.repository, state.subscriptionRepository) {
        merge(state.repository.changes, state.subscriptionRepository.changes)
    }
    fun navigate(target: List<NavKey>) {
        val current = normalizeBackStack(backStack.toList())
        val next = milestoneAwareBackStack(current, browsingAwareBackStack(current, target))
        if (backStack.toList() != next) { backStack.clear(); backStack.addAll(next) }
    }
    fun requestLeave(target: List<NavKey>) {
        if (!navigationAvailable(restoreState.phase)) return
        when (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision(), categoryModel?.leaveDecision() ?: LeaveDecision.ALLOW, replacementModel?.leaveDecision() ?: LeaveDecision.ALLOW)) {
            LeaveDecision.ALLOW -> { model.discardForm(); subscriptionModel.discardForm(); categoryModel?.discardForm(); replacementModel?.discardForm(); navigate(target) }
            LeaveDecision.CONFIRM_DISCARD -> pendingLeave = target
            LeaveDecision.BLOCK_PENDING -> pendingWarning = true
        }
    }
    LaunchedEffect(milestoneRequest?.identity, restoreState.phase) {
        val request = milestoneRequest ?: return@LaunchedEffect
        if (handledNotice == request.identity || restoreState.phase == RestoreUiPhase.RESTORING) return@LaunchedEffect
        handledNotice = request.identity
        val reference = request.reference
        val target = if (!request.invalid && reference != null) MilestoneSources(reference.generationId,
            reference.publicationOn.toString(), reference.digest, reference.count) else InvalidMilestoneNotice
        autoOpenNotice = target as? MilestoneSources
        // Resolve only after this existing guard has allowed entering the temporary
        // source page. An intent never discards a draft or bypasses pending writes.
        requestLeave(canonicalBackStack(target))
    }
    val onBack: () -> Unit = { if (routes.size > 1) requestLeave(routes.dropLast(1)) }
    SideEffect { if (backStack.toList() != routes) navigate(routes) }
    LaunchedEffect(routes.last(), chartModel, trendModel, analysisModel) {
        if (routes.last() != Review && routes.last() != TotalCostTrend) trendModel?.pause()
        if (routes.last() != AnnualReview && routes.last() !is CategoryAnalysis) analysisModel?.pause()
        when (val route = routes.last()) {
            CategoryManagement -> categoryModel?.refresh()
            NameSearch -> searchModel?.refresh()
            LongTermReview -> reviewModel?.refresh()
            Home -> { model.loadList(); subscriptionModel.loadList() }
            Review, TotalCostTrend -> trendModel?.open()
            AnnualReview -> analysisModel?.open()
            is CategoryAnalysis -> {
                route.categoryId?.let { analysisModel?.selectCategory(if (route.subscription) OverviewKind.SUBSCRIPTION else OverviewKind.PROJECT, it) }
                analysisModel?.open()
            }
            ProjectList -> model.loadList()
            ProjectHistory -> model.loadHistory()
            CreateProject -> model.startCreate()
            is ReplacementComparison -> replacementModel?.openComparison(route.id)
            is TradeInProject -> replacementModel?.startTradeIn(route.id)
            is ProjectCostHistory -> chartModel?.openProject(route.id)
            is SubscriptionPriceHistory -> chartModel?.openSubscription(route.id)
            is ProjectDetail -> model.openDetail(route.id)
            is ProjectEdit -> model.startEdit(route.id)
            is ProjectMoneyEntry -> model.startMoney(route.id, route.direction)
            is ProjectEnd -> model.startEnd(route.id)
            is ProjectUpdateEnd -> model.startUpdateEnd(route.id)
            is ProjectMoneyCorrection -> model.startCorrectMoney(route.id, route.eventId)
            is ProjectDelete -> model.startDelete(route.id)
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
            else -> Unit
        }
    }
    val projectLeave = model.leaveDecision()
    val subscriptionLeave = subscriptionModel.leaveDecision()
    val categoryLeave = categoryModel?.leaveDecision() ?: LeaveDecision.ALLOW
    val replacementLeave = replacementModel?.leaveDecision() ?: LeaveDecision.ALLOW
    BackHandler(enabled = routes == listOf(Home) && globalLeaveDecision(projectLeave, subscriptionLeave, categoryLeave, replacementLeave) != LeaveDecision.ALLOW) {
        requestLeave(routes)
    }
    LaunchedEffect(projects.committedProjectId, subscriptionLeave, categoryLeave, replacementLeave) {
        projects.committedProjectId?.let { id ->
            val guard = globalLeaveDecision(subscriptionLeave, categoryLeave, replacementLeave)
            if (guard == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val target = canonicalBackStack(ProjectDetail(id, projectOrigin(routes.last()), projectComparisonId(routes.last())))
            if (guard == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            model.consumeCommitted()
        }
    }
    LaunchedEffect(projects.deletedProjectId, subscriptionLeave, categoryLeave, replacementLeave) {
        projects.deletedProjectId?.let {
            val guard = globalLeaveDecision(subscriptionLeave, categoryLeave, replacementLeave)
            if (guard == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val target = canonicalBackStack(projectBrowseReturnAfterDelete(routes))
            if (guard == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            model.consumeDeleted()
        }
    }
    LaunchedEffect(subscriptions.committedSubscriptionId, projectLeave, categoryLeave, replacementLeave) {
        subscriptions.committedSubscriptionId?.let { id ->
            val guard = globalLeaveDecision(projectLeave, categoryLeave, replacementLeave)
            if (guard == LeaveDecision.BLOCK_PENDING) { pendingWarning = true; return@let }
            val origin = SubscriptionOrigin.entries.firstOrNull { it.name == subscriptions.committedOrigin } ?: SubscriptionOrigin.CURRENT
            val target = canonicalBackStack(SubscriptionDetail(id, origin))
            if (guard == LeaveDecision.CONFIRM_DISCARD) { pendingLeave = target; return@let }
            navigate(target)
            subscriptionModel.consumeCommitted()
        }
    }
    LaunchedEffect(replacements?.committedProjectId, projectLeave, subscriptionLeave, categoryLeave) {
        replacements?.committedProjectId?.let { id ->
            when (globalLeaveDecision(projectLeave, subscriptionLeave, categoryLeave)) {
                LeaveDecision.BLOCK_PENDING -> pendingWarning = true
                LeaveDecision.CONFIRM_DISCARD -> pendingLeave = canonicalBackStack(ProjectDetail(id, projectOrigin(routes.last()), projectComparisonId(routes.last())))
                LeaveDecision.ALLOW -> {
                    navigate(canonicalBackStack(ProjectDetail(id, projectOrigin(routes.last()), projectComparisonId(routes.last()))))
                    replacementModel?.consumeCommitted()
                }
            }
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dateObserver = remember(state.businessClock, state.generationId) {
        BusinessDateObserver(state.overview?.asOf ?: state.businessClock.today())
    }
    val latestRetry by androidx.compose.runtime.rememberUpdatedState(onRetryLedger)
    LaunchedEffect(lifecycle, dateObserver, state.recordZone, reviewModel, chartModel, trendModel, analysisModel) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            dateObserver.observe(state.businessClock, state.recordZone, {
                model.refresh()
                subscriptionModel.refresh()
                searchModel?.refresh()
                reviewModel?.refresh()
                replacementModel?.refresh()
                chartModel?.refresh()
                trendModel?.refresh()
                analysisModel?.refresh()
                latestRetry()
            })
        }
    }
    DisposableEffect(lifecycle, model, subscriptionModel, categoryModel, searchModel, reviewModel, replacementModel, chartModel, trendModel, analysisModel) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) {
            model.refresh(); subscriptionModel.refresh(); categoryModel?.refresh(); searchModel?.refresh(); reviewModel?.refresh(); replacementModel?.refresh(); chartModel?.refresh(); trendModel?.refresh(); analysisModel?.refresh(); latestRetry()
        } }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    // Keep a restore outcome that has not been confirmed out of the verified-empty presentation.
    // This reads the existing controller state; it does not grant navigation or write eligibility.
    val hasSnapshotNotice = state.readError != null || state.overview == null ||
        restoreState.feedback == com.cooper.lithic.feature.settings.RestoreFeedbackKind.OUTCOME_UNKNOWN ||
        restoreState.phase == com.cooper.lithic.feature.settings.RestoreUiPhase.RESTORING
    val currentPrimary = when {
        Review in routes -> Review
        Settings in routes -> Settings
        else -> Home
    }
    Scaffold(modifier = modifier.fillMaxSize().testTag("primary_navigation"), contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (c2ShowPrimaryNavigation(routes.last())) C2PrimaryNavigationBar(currentPrimary) { destination ->
                requestLeave(canonicalBackStack(destination))
            }
        }) { padding ->
        Column(Modifier.padding(padding).consumeWindowInsets(padding)) {
            if (routes.last() != Settings) restoreState.message?.let { feedback ->
                UiFeedbackText(feedback, restoreState.messageTone, "backup-restore-global-feedback", Modifier.padding(16.dp))
            }
            state.readError?.let { error ->
                Text(error, Modifier.padding(16.dp).testTag("ledger-refresh-error"))
                TextButton(onClick = onRetryLedger, modifier = Modifier.heightIn(min = 48.dp).testTag("ledger-retry-read")) { Text("重试读取") }
            }
            NavDisplay(backStack = routes, modifier = Modifier.weight(1f), onBack = onBack,
                transitionSpec = { c5PageTransition() },
                popTransitionSpec = { c5PageTransition() },
                // Navigation3 1.0.1 has a separate gesture-progress transition path.
                // Only its visual transform changes; its original onBack/guards remain.
                predictivePopTransitionSpec = { _ -> c5PageTransition() },
                entryProvider = c5SceneEntryProvider(entryProvider<NavKey> {
                    entry<Home>(metadata = mapOf(C4_PRIMARY_DESTINATION to Home)) { HomeShell(state.projectCount, onProjectList = { requestLeave(canonicalBackStack(ProjectList)) }, onCreateProject = { requestLeave(listOf(Home, CreateProject)) },
                        subscriptionCount = state.subscriptionCount, onSubscriptionList = { requestLeave(canonicalBackStack(SubscriptionList)) }, onCreateSubscription = { requestLeave(listOf(Home, CreateSubscription)) }, overview = state.overview,
                        onSearch = if (searchModel != null) ({ requestLeave(canonicalBackStack(NameSearch)) }) else null,
                        onCategoryAnalysis = if (state.totalCostTimelineRepository != null) ({ kind, id -> requestLeave(canonicalBackStack(CategoryAnalysis(id, kind == OverviewKind.SUBSCRIPTION))) }) else null,
                        projectPreview = projects.list, subscriptionPreview = subscriptions.list,
                        onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it, ProjectOrigin.HOME))) },
                        onOpenSubscription = { requestLeave(canonicalBackStack(SubscriptionDetail(it, SubscriptionOrigin.HOME))) },
                        onCalculationExplanation = { requestLeave(listOf(Home, CalculationExplanation)) },
                        onRetryPreview = { model.retryRead(); subscriptionModel.retryRead() }, hasReadNotice = hasSnapshotNotice) }
                    entry<Review>(metadata = mapOf(C4_PRIMARY_DESTINATION to Review)) { ReviewShell(state.projectCount, onProjectHistory = { requestLeave(canonicalBackStack(ProjectHistory)) },
                        onSubscriptionHistory = { requestLeave(canonicalBackStack(SubscriptionHistory)) },
                        onLongTermReview = if (state.rankingRepository != null) ({ requestLeave(canonicalBackStack(LongTermReview)) }) else null,
                        onTotalCostTrend = if (state.totalCostTimelineRepository != null) ({ requestLeave(canonicalBackStack(TotalCostTrend)) }) else null,
                        onCategoryAnalysis = if (state.totalCostTimelineRepository != null) ({ requestLeave(canonicalBackStack(CategoryAnalysis())) }) else null,
                        onAnnualReview = if (state.totalCostTimelineRepository != null) ({ requestLeave(canonicalBackStack(AnnualReview)) }) else null,
                        subscriptionCount = state.subscriptionCount, hasReadNotice = hasSnapshotNotice,
                        trendState = trendState, onSelectTrendDate = { trendModel?.selectDate(it) },
                        onToggleTrendBreakdown = { trendModel?.toggleBreakdown() }, onRetryTrend = { trendModel?.refresh() },
                        onAddRecord = { requestLeave(canonicalBackStack(Home)) },
                        onOpenTrendRecord = { kind, id -> requestLeave(canonicalBackStack(
                            if (kind == OverviewKind.PROJECT) ProjectDetail(id, ProjectOrigin.REVIEW_HOME)
                            else SubscriptionDetail(id, SubscriptionOrigin.REVIEW_HOME))) }) }
                    entry<CategoryAnalysis> {
                        if (analysisModel != null && analysisState != null) CategoryAnalysisScreen(analysisState,
                            analysisModel::selectCategory, analysisModel::refresh,
                            onOpenRecord = { kind, id ->
                                val selectedParent = CategoryAnalysis(analysisState.selectedCategoryId,
                                    analysisState.selectedKind == OverviewKind.SUBSCRIPTION)
                                val selectedSource = listOf(Home, Review, selectedParent)
                                requestLeave(browsingAwareBackStack(selectedSource, canonicalBackStack(
                                    if (kind == OverviewKind.PROJECT) ProjectDetail(id, ProjectOrigin.CATEGORY)
                                    else SubscriptionDetail(id, SubscriptionOrigin.CATEGORY))))
                            }, onBack = onBack)
                    }
                    entry<AnnualReview> {
                        if (analysisModel != null && analysisState != null) AnnualReviewScreen(analysisState,
                            analysisModel::selectYear, analysisModel::refresh,
                            onOpenRecord = { kind, id -> requestLeave(canonicalBackStack(
                                if (kind == OverviewKind.PROJECT) ProjectDetail(id, ProjectOrigin.ANNUAL)
                                else SubscriptionDetail(id, SubscriptionOrigin.ANNUAL))) }, onBack = onBack)
                    }
                    entry<TotalCostTrend> {
                        if (trendModel != null && trendState != null) TotalCostTrendScreen(trendState,
                            trendModel::selectDate, trendModel::toggleBreakdown, trendModel::refresh,
                            onOpenRecord = { kind, id -> requestLeave(canonicalBackStack(
                                if (kind == com.cooper.lithic.domain.report.OverviewKind.PROJECT) ProjectDetail(id, ProjectOrigin.TREND)
                                else SubscriptionDetail(id, SubscriptionOrigin.TREND))) }, onBack = onBack)
                    }
                    entry<LongTermReview> {
                        if (reviewModel != null && reviewState != null) LongTermReviewScreen(reviewState, onBack,
                            reviewModel::changeRanking, reviewModel::refresh, reviewModel::loadMore,
                            onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it, ProjectOrigin.REVIEW))) },
                            onOpenSubscription = { requestLeave(canonicalBackStack(SubscriptionDetail(it, SubscriptionOrigin.REVIEW))) })
                    }
                    entry<Settings>(metadata = mapOf(C4_PRIMARY_DESTINATION to Settings)) { SettingsShell(state.recordZone, backupState = backupState,
                        onExportBackup = onExportBackup, onCancelBackup = onCancelBackup,
                        restoreState = restoreState, onRestoreBackup = onRestoreBackup, onCancelRestore = onCancelRestore,
                        onConfirmRestore = onConfirmRestore, onDiscardRestore = onDiscardRestore,
                        onManageCategories = if (categoryModel != null) ({ requestLeave(canonicalBackStack(CategoryManagement)) }) else null,
                        onNotificationClick = if (state.settingsStore != null) ({ requestLeave(canonicalBackStack(NotificationSettings)) }) else null,
                        onCalculationExplanation = { requestLeave(canonicalBackStack(CalculationExplanation)) },
                        onAboutLithic = { requestLeave(canonicalBackStack(AboutLithic)) }) }
                    entry<CalculationExplanation> { CalculationExplanationScreen(onBack) }
                    entry<AboutLithic> { AboutLithicEntry(onBack) }
                    entry<NotificationSettings> {
                        if (notificationModel != null) NotificationSettingsEntry(notificationModel, onBack)
                    }
                    entry<InvalidMilestoneNotice> { InvalidMilestoneNoticeScreen(onBack) }
                    entry<MilestoneSources> { route ->
                        if (resolveMilestone == null) InvalidMilestoneNoticeScreen(onBack)
                        else MilestoneSourcesScreen(route.reference(), milestoneChanges, resolveMilestone,
                            openSingle = autoOpenNotice == route, onSingleHandled = { autoOpenNotice = null },
                            onOpen = { item -> requestLeave(canonicalBackStack(
                                if (item.ownerType == MilestoneOwnerType.PROJECT) ProjectDetail(item.ownerId)
                                else SubscriptionDetail(item.ownerId))) }, onBack = onBack)
                    }
                    entry<CategoryManagement> {
                        if (categoryModel != null && categoryState != null) CategoryManagementScreen(
                            categoryState, onBack, categoryModel::selectType, categoryModel::startCreate,
                            categoryModel::startRename, categoryModel::startMerge, categoryModel::changeName,
                            categoryModel::selectTarget, categoryModel::previewMerge, categoryModel::save,
                            categoryModel::confirmMerge, categoryModel::dismissEditor, categoryModel::refresh)
                    }
                    entry<NameSearch> {
                        if (searchModel != null && searchState != null) SearchScreen(searchState, onBack,
                            searchModel::changeQuery, searchModel::refresh, searchModel::loadMore, onOpen = { item ->
                                // Reuse the established detail/history stacks and their write guards.
                                val target: NavKey = if (item.type == CategoryType.PROJECT)
                                    ProjectDetail(item.id, if (item.isHistory) ProjectOrigin.HISTORY else ProjectOrigin.CURRENT)
                                else SubscriptionDetail(item.id, if (item.isHistory) SubscriptionOrigin.HISTORY else SubscriptionOrigin.CURRENT)
                                requestLeave(canonicalBackStack(target))
                            })
                    }
                    entry<ProjectList> {
                        ProjectListScreen(projects.list, onBack, onCreate = { requestLeave(listOf(Home, ProjectList, CreateProject)) },
                            onOpenProject = { requestLeave(canonicalBackStack(ProjectDetail(it))) }, onLoadMore = model::loadMore, onRetry = model::retryRead)
                    }
                    entry<ProjectDetail> { route ->
                        ProjectDetailScreen(projects.detail, onBack, onEdit = { requestLeave(canonicalBackStack(ProjectEdit(route.id, route.origin, route.comparisonId))) },
                            onAddMoney = { requestLeave(canonicalBackStack(ProjectMoneyEntry(route.id, it, route.origin, route.comparisonId))) }, onRetry = model::retryRead,
                            onEnd = { requestLeave(canonicalBackStack(ProjectEnd(route.id, route.origin, route.comparisonId))) },
                            onUpdateEnd = { requestLeave(canonicalBackStack(ProjectUpdateEnd(route.id, route.origin, route.comparisonId))) },
                            onCorrectMoney = { requestLeave(canonicalBackStack(ProjectMoneyCorrection(route.id, it, route.origin, route.comparisonId))) },
                            onDelete = { requestLeave(canonicalBackStack(ProjectDelete(route.id, route.origin, route.comparisonId))) },
                            onCostHistory = if (state.itemTimelineRepository != null) ({ requestLeave(canonicalBackStack(ProjectCostHistory(route.id, route.origin, route.comparisonId))) }) else null,
                            onReplacement = if (replacementModel != null) ({ requestLeave(canonicalBackStack(ReplacementComparison(route.id, route.origin))) }) else null,
                            onTradeIn = if (replacementModel != null) ({ requestLeave(canonicalBackStack(TradeInProject(route.id, route.origin, route.comparisonId))) }) else null,
                            backLabel = if (route.comparisonId != null) "返回替换对比" else if (routes.getOrNull(1) == NameSearch) "返回搜索结果" else if (routes.getOrNull(1) is MilestoneSources) "返回里程碑来源" else when (route.origin) {
                                ProjectOrigin.HOME -> "返回现在"
                                ProjectOrigin.REVIEW_HOME -> "返回回看"
                                ProjectOrigin.CURRENT -> "返回项目列表"
                                ProjectOrigin.HISTORY -> "返回历史档案"
                                ProjectOrigin.REVIEW -> "返回长期复盘"
                                ProjectOrigin.TREND -> "返回成本趋势"
                                ProjectOrigin.CATEGORY -> "返回类别分析"
                                ProjectOrigin.ANNUAL -> "返回年度回顾"
                            })
                    }
                    entry<ProjectCostHistory> { route ->
                        val chart = chartState?.takeIf { it.kind == com.cooper.lithic.feature.report.ItemChartKind.PROJECT && it.id == route.id }
                        if (chartModel != null) ProjectCostChart(chart?.title.orEmpty(),
                            chart?.project?.timeline, chart?.selectedProject, chart?.events.orEmpty(),
                            chart?.loading ?: true, chart?.error, chart?.missing ?: false,
                            chartModel::selectDate, chartModel::refresh, onBack, requestedDate = chart?.selectedDate)
                    }
                    entry<SubscriptionPriceHistory> { route ->
                        val chart = chartState?.takeIf { it.kind == com.cooper.lithic.feature.report.ItemChartKind.SUBSCRIPTION && it.id == route.id }
                        if (chartModel != null) SubscriptionPriceChart(chart?.title.orEmpty(),
                            chart?.subscription?.timeline, chart?.selectedSubscription, chart?.events.orEmpty(),
                            chart?.loading ?: true, chart?.error, chart?.missing ?: false,
                            chartModel::selectDate, chartModel::refresh, onBack, requestedDate = chart?.selectedDate)
                    }
                    entry<ReplacementComparison> { route ->
                        if (replacementModel != null && replacements != null) ReplacementComparisonScreen(replacements, onBack,
                            onOpenProject = { id -> requestLeave(canonicalBackStack(ProjectDetail(id, route.origin, if (id == route.id) null else route.id))) },
                            onLink = { replacementModel.startLink(route.id) }, onUnlink = { replacementModel.startUnlink(route.id, it) },
                            onRetry = replacementModel::retryRead, onSelectTarget = replacementModel::selectTarget,
                            onDirection = replacementModel::changeAsSuccessor, onPreview = replacementModel::preview,
                            onSave = replacementModel::save, onDismissForm = replacementModel::discardForm,
                            onLoadMore = replacementModel::loadMoreChoices)
                    }
                    entry<TradeInProject> {
                        replacements?.form?.takeIf { it.kind == ReplacementFormKind.TRADE_IN }?.let { form ->
                            if (replacementModel != null) TradeInEditorScreen(form, onBack, replacementModel::changeName,
                                replacementModel::changeDate, replacementModel::changeCash, replacementModel::changeCredit,
                                replacementModel::changeCategory, replacementModel::changeNote, replacementModel::changeLink,
                                replacementModel::preview, replacementModel::editConfirmation, replacementModel::save, replacementModel::retryRead)
                        }
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
                    entry<SubscriptionHistory> {
                        SubscriptionHistoryScreen(subscriptions.stoppedHistory, onBack,
                            { requestLeave(canonicalBackStack(SubscriptionDetail(it, SubscriptionOrigin.HISTORY))) },
                            subscriptionModel::loadMoreStoppedHistory, subscriptionModel::retryRead)
                    }
                    entry<SubscriptionDetail> { route ->
                        fun stateAction(action: SubscriptionAction) = requestLeave(canonicalBackStack(ChangeSubscriptionState(route.id, action, route.origin)))
                        SubscriptionDetailScreen(subscriptions.detail, onBack, { requestLeave(canonicalBackStack(ConfirmSubscriptionRenewal(route.id, route.origin))) },
                            subscriptionModel::loadMoreHistory, subscriptionModel::retryRead,
                            onCostHistory = if (state.itemTimelineRepository != null) ({ requestLeave(canonicalBackStack(SubscriptionPriceHistory(route.id, route.origin))) }) else null,
                            onPause = { stateAction(SubscriptionAction.PAUSE) }, onResume = { stateAction(SubscriptionAction.RESUME) },
                            onStop = { stateAction(SubscriptionAction.STOP) }, onReopen = { stateAction(SubscriptionAction.REOPEN) },
                            onCorrectState = { requestLeave(canonicalBackStack(CorrectSubscriptionState(route.id, it, route.origin))) },
                            onCancelStop = { requestLeave(canonicalBackStack(CancelSubscriptionStop(route.id, it, route.origin))) },
                            onCorrectRenewal = { requestLeave(canonicalBackStack(CorrectSubscriptionRenewal(route.id, it, route.origin))) },
                            onDeleteRenewal = { requestLeave(canonicalBackStack(DeleteSubscriptionRenewal(route.id, it, route.origin))) },
                            backLabel = if (routes.getOrNull(1) == NameSearch) "返回搜索结果" else if (routes.getOrNull(1) is MilestoneSources) "返回里程碑来源" else when (route.origin) {
                                SubscriptionOrigin.HOME -> "返回现在"
                                SubscriptionOrigin.REVIEW_HOME -> "返回回看"
                                SubscriptionOrigin.CURRENT -> "返回订阅列表"
                                SubscriptionOrigin.HISTORY -> "返回已停止订阅"
                                SubscriptionOrigin.REVIEW -> "返回长期复盘"
                                SubscriptionOrigin.TREND -> "返回成本趋势"
                                SubscriptionOrigin.CATEGORY -> "返回类别分析"
                                SubscriptionOrigin.ANNUAL -> "返回年度回顾"
                            })
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
                }))
        }
    }
    pendingLeave?.let { target ->
        AlertDialog(onDismissRequest = { pendingLeave = null }, modifier = Modifier.testTag("discard-dialog"), title = { Text("放弃未保存修改？") },
            text = { Text("输入尚未保存。继续编辑会保留这些内容。") },
            confirmButton = { TextButton(onClick = {
                if (navigationAvailable(restoreState.phase)) {
                    pendingLeave = null
                    if (globalLeaveDecision(model.leaveDecision(), subscriptionModel.leaveDecision(), categoryModel?.leaveDecision() ?: LeaveDecision.ALLOW, replacementModel?.leaveDecision() ?: LeaveDecision.ALLOW) == LeaveDecision.BLOCK_PENDING) pendingWarning = true
                    else { model.consumeCommitted(); subscriptionModel.consumeCommitted(); model.consumeDeleted()
                        model.discardForm(); subscriptionModel.discardForm(); categoryModel?.discardForm(); replacementModel?.discardForm(); navigate(target) }
                }
            }, enabled = navigationAvailable(restoreState.phase), modifier = Modifier.testTag("discard-confirm")) { Text("放弃修改") } },
            dismissButton = { TextButton(onClick = { pendingLeave = null }, modifier = Modifier.testTag("discard-keep")) { Text("继续编辑") } })
    }
    if (pendingWarning) {
        AlertDialog(onDismissRequest = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-dialog"), title = { Text("请先核对保存结果") },
            text = { Text("提交正在进行或结果尚未确认。原始输入已保留，请返回表单核对并重试同一提交。") },
            confirmButton = { TextButton(onClick = { pendingWarning = false }, modifier = Modifier.testTag("pending-submit-keep")) { Text("继续核对") } },
            dismissButton = {
                val origin = projectOrigin(routes.last())
                val projectRecovery: NavKey? = if (projectLeave == LeaveDecision.BLOCK_PENDING)
                    projectRecoveryRoute(projects, origin, projectComparisonId(routes.last())) else null
                val subscriptionRecovery: NavKey? = if (subscriptionLeave == LeaveDecision.BLOCK_PENDING) subscriptionRecoveryRoute(subscriptions) else null
                val categoryRecovery: NavKey? = if (categoryLeave == LeaveDecision.BLOCK_PENDING) CategoryManagement else null
                val replacementRecovery: NavKey? = if (replacementLeave == LeaveDecision.BLOCK_PENDING) replacements?.form?.let {
                    if (it.kind == ReplacementFormKind.TRADE_IN) TradeInProject(it.projectId, origin, projectComparisonId(routes.last()))
                    else ReplacementComparison(it.projectId, origin)
                } else null
                val recoveryTargets = listOfNotNull(projectRecovery, subscriptionRecovery, categoryRecovery, replacementRecovery).filter { it != routes.last() }
                Column { recoveryTargets.forEach { recovery ->
                    TextButton(onClick = { if (navigationAvailable(restoreState.phase)) {
                        pendingWarning = false; navigate(canonicalBackStack(recovery))
                    } }, enabled = navigationAvailable(restoreState.phase),
                        modifier = Modifier.testTag(if (recoveryTargets.size == 1) "pending-submit-return-to-form" else if (recovery == subscriptionRecovery) "pending-submit-return-to-subscription" else if (recovery == categoryRecovery) "pending-submit-return-to-category" else if (recovery == replacementRecovery) "pending-submit-return-to-replacement" else "pending-submit-return-to-project")) {
                        Text(if (recovery == subscriptionRecovery) "返回待核对订阅记录" else if (recovery == categoryRecovery) "返回待核对类别" else if (recovery == replacementRecovery) "返回待核对替换交易" else "返回待核对项目")
                    }
                } }
            })
    }
}

/** Wrap each entry inside its Scene without replacing NavDisplay's saved-state decorators.
 * NavEntry's copy constructor preserves the original key, contentKey and metadata. */
internal fun <T : Any> c5SceneEntryProvider(provider: (T) -> NavEntry<T>): (T) -> NavEntry<T> = { route ->
    val original = provider(route)
    NavEntry(original) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 1f))
            .testTag("navigation-scene")) {
            original.Content()
        }
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


/** Pure navigation chrome. The caller retains all route, dirty and pending-operation guards. */
@Composable
internal fun C2PrimaryNavigationBar(currentPrimary: NavKey, onSelect: (NavKey) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        val destinations: List<Pair<NavKey, String>> = listOf(Home to "现在", Review to "回看", Settings to "设置")
        destinations.forEach { (destination, label) ->
            NavigationBarItem(selected = currentPrimary == destination, onClick = { onSelect(destination) },
                icon = { C2Icon(when (destination) { Home -> C2Glyph.Now; Review -> C2Glyph.Review; else -> C2Glyph.Settings }) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.heightIn(min = 48.dp).testTag(when (destination) { Home -> "tab-now"; Review -> "tab-review"; else -> "tab-settings" }))
        }
    }
}
