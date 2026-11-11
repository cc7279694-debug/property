package com.cooper.lithic.p2a2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.Settings
import com.cooper.lithic.core.design.C2Glyph
import com.cooper.lithic.core.design.C2RecordIdentity
import com.cooper.lithic.core.design.LithicSpacing
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.lithicColorScheme
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.feature.appearance.AppearanceActions
import com.cooper.lithic.feature.appearance.AppearanceThemePicker
import com.cooper.lithic.feature.appearance.AppearanceUiState
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.appearance.LocalAppearanceActions
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.home.SearchScreen
import com.cooper.lithic.feature.media.ProvideCoverPreviews
import com.cooper.lithic.feature.media.RecordCover
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectListScreen
import com.cooper.lithic.feature.review.ReviewShell
import com.cooper.lithic.feature.review.TotalCostTrendScreen
import com.cooper.lithic.feature.settings.CalculationExplanationScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import java.time.LocalDate

class P2A2ExperienceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(packageName == "com.cooper.lithic.p2a2.debug")
        enableEdgeToEdge()
        val fixtures = P2A2Fixtures()
        setContent {
            var appearanceId by rememberSaveable { mutableStateOf(LithicAppearance.STONE_MIST.storageId) }
            val appearance = requireNotNull(LithicAppearance.fromStorage(appearanceId))
            SideEffect { updateSystemBars(appearance) }
            val appearanceActions = AppearanceActions(
                AppearanceUiState(selected = appearance, loading = false, readable = true),
                select = { appearanceId = it.storageId },
                retryRead = {},
            )
            CompositionLocalProvider(LocalAppearanceActions provides appearanceActions) {
                LithicTheme(appearance) {
                    ProvideCoverPreviews(fixtures.coverReader) { Experience(fixtures) }
                }
            }
        }
    }

    private fun updateSystemBars(appearance: LithicAppearance) {
        val light = appearance != LithicAppearance.CHARCOAL
        val background = lithicColorScheme(appearance).background.toArgb()
        val style = if (light) SystemBarStyle.light(background, background) else SystemBarStyle.dark(background)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }
}

@Composable
private fun Experience(fixtures: P2A2Fixtures) {
    var routes by rememberSaveable { mutableStateOf(arrayListOf("home")) }
    var query by rememberSaveable { mutableStateOf("") }
    var unavailableAction by rememberSaveable { mutableStateOf<String?>(null) }
    var trendDay by rememberSaveable { mutableStateOf(fixtures.day.toString()) }
    var breakdown by rememberSaveable { mutableStateOf(false) }
    val route = routes.last()
    val primary = when (route) {
        "home" -> Home
        "review" -> Review
        "settings" -> Settings
        else -> null
    }
    val goBack: () -> Unit = { if (routes.size > 1) routes = ArrayList(routes.dropLast(1)) }
    fun open(destination: String) {
        routes = ArrayList(routes + destination)
    }
    fun unavailable(action: String) {
        unavailableAction = action
    }
    fun openRecord(kind: OverviewKind, id: String) {
        open(if (kind == OverviewKind.PROJECT) "project/$id" else "subscription/$id")
    }

    BackHandler(enabled = routes.size > 1, onBack = goBack)
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            if (primary != null) C2PrimaryNavigationBar(primary) { destination ->
                routes = when (destination) {
                    Review -> arrayListOf("home", "review")
                    Settings -> arrayListOf("home", "settings")
                    else -> arrayListOf("home")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    "体验模式 · 仅使用合成记录",
                    Modifier.padding(horizontal = LithicSpacing.page, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                key(route) {
                    when (route.substringBefore('/')) {
                        "home" -> HomeShell(
                            projectCount = fixtures.overview.projectCount,
                            onProjectList = { open("projects") },
                            onCreateProject = { unavailable("新增长期项目") },
                            subscriptionCount = fixtures.overview.subscriptionCount,
                            onSubscriptionList = { open("subscriptions") },
                            onCreateSubscription = { unavailable("新增订阅") },
                            overview = fixtures.overview,
                            onSearch = { query = ""; open("search") },
                            projectPreview = fixtures.projects,
                            subscriptionPreview = fixtures.subscriptions,
                            onOpenProject = { openRecord(OverviewKind.PROJECT, it) },
                            onOpenSubscription = { openRecord(OverviewKind.SUBSCRIPTION, it) },
                            onCalculationExplanation = { open("calculation") },
                        )
                        "projects" -> ProjectListScreen(
                            fixtures.projects, goBack, { unavailable("新增长期项目") },
                            { openRecord(OverviewKind.PROJECT, it) }, {}, {},
                        )
                        "subscriptions" -> SubscriptionListScreen(
                            fixtures.subscriptions, goBack, { unavailable("新增订阅") },
                            { openRecord(OverviewKind.SUBSCRIPTION, it) }, {}, {},
                        )
                        "project" -> {
                            val state = fixtures.project(route.substringAfter('/'))
                            ProjectDetailScreen(
                                state = state,
                                onBack = goBack,
                                onEdit = { unavailable("编辑项目") },
                                onAddMoney = { unavailable("记录金额") },
                                onRetry = {},
                                onEnd = { unavailable("结束持有") },
                                onUpdateEnd = { unavailable("更正结束日期") },
                                onCorrectMoney = { unavailable("更正金额") },
                                onDelete = { unavailable("删除项目") },
                                backLabel = "返回上一页",
                                coverContent = {
                                    state.detail?.let { detail ->
                                        RecordCover(
                                            CoverReference(CoverOwnerType.PROJECT, detail.project.id, detail.project.coverAssetId),
                                            Modifier.fillMaxSize(),
                                        ) { C2RecordIdentity(detail.project.name, detail.category.name, C2Glyph.Project) }
                                    }
                                },
                            )
                        }
                        "subscription" -> {
                            val state = fixtures.subscription(route.substringAfter('/'))
                            SubscriptionDetailScreen(
                                state = state,
                                onBack = goBack,
                                onRenewal = { unavailable("记录续费") },
                                onLoadMoreHistory = {},
                                onRetry = {},
                                onPause = { unavailable("暂停订阅") },
                                onResume = { unavailable("恢复订阅") },
                                onStop = { unavailable("停止订阅") },
                                onReopen = { unavailable("重新启用订阅") },
                                onCorrectState = { unavailable("更正订阅状态") },
                                onCancelStop = { unavailable("取消停止订阅") },
                                onCorrectRenewal = { unavailable("更正付款") },
                                onDeleteRenewal = { unavailable("删除付款") },
                                backLabel = "返回上一页",
                                coverContent = {
                                    state.detail?.let { detail ->
                                        RecordCover(
                                            CoverReference(CoverOwnerType.SUBSCRIPTION, detail.subscription.id, detail.subscription.coverAssetId),
                                            Modifier.fillMaxSize(),
                                        ) { C2RecordIdentity(detail.subscription.name, detail.category.name, C2Glyph.Subscription) }
                                    }
                                },
                            )
                        }
                        "search" -> SearchScreen(
                            state = remember(query) { fixtures.search(query) },
                            onBack = goBack,
                            onQueryChange = { query = it },
                            onRetry = {},
                            onLoadMore = {},
                            onOpen = {
                                val kind = if (it.type == CategoryType.PROJECT) OverviewKind.PROJECT else OverviewKind.SUBSCRIPTION
                                openRecord(kind, it.id)
                            },
                        )
                        "review" -> ReviewShell(
                            projectCount = fixtures.overview.projectCount,
                            subscriptionCount = fixtures.overview.subscriptionCount,
                            trendState = remember(trendDay, breakdown) { fixtures.trend(LocalDate.parse(trendDay), breakdown) },
                            onSelectTrendDate = { trendDay = it.toString() },
                            onToggleTrendBreakdown = { breakdown = !breakdown },
                            onOpenTrendRecord = ::openRecord,
                            onTotalCostTrend = { open("trend") },
                            onProjectHistory = { unavailable("项目历史档案") },
                            onSubscriptionHistory = { unavailable("订阅历史档案") },
                            onCategoryAnalysis = { unavailable("类别分析") },
                            onLongTermReview = { unavailable("长期复盘") },
                            onAnnualReview = { unavailable("年度回顾") },
                        )
                        "trend" -> TotalCostTrendScreen(
                            state = remember(trendDay, breakdown) { fixtures.trend(LocalDate.parse(trendDay), breakdown) },
                            onSelectDate = { trendDay = it.toString() },
                            onToggleBreakdown = { breakdown = !breakdown },
                            onRetry = {},
                            onOpenRecord = ::openRecord,
                            onBack = goBack,
                        )
                        "calculation" -> CalculationExplanationScreen(goBack)
                        "settings" -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                                .padding(horizontal = LithicSpacing.page, vertical = 16.dp),
                        ) {
                            Text(
                                "设置",
                                Modifier.semantics { heading() }.padding(bottom = 16.dp),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            AppearanceThemePicker()
                            Text(
                                "主题仅在本次体验中保留。",
                                Modifier.padding(top = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
    unavailableAction?.let { action ->
        AlertDialog(
            onDismissRequest = { unavailableAction = null },
            title = { Text(action) },
            text = { Text("体验模式，不会保存真实记录。") },
            confirmButton = { TextButton({ unavailableAction = null }) { Text("知道了") } },
        )
    }
}
