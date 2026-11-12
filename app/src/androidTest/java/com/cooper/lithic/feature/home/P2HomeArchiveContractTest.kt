package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.test.espresso.Espresso.pressBack
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.Settings
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReadFailure
import com.cooper.lithic.data.media.CoverReadResult
import com.cooper.lithic.data.media.CoverReader
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.media.ProvideCoverPreviews
import com.cooper.lithic.ui.C2VisualFixtures
import java.math.BigDecimal
import java.util.concurrent.ConcurrentLinkedQueue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Production Home with synthetic facts. Local density and callback spies do not emulate Android system settings or Nav3. */
class P2HomeArchiveContractTest {
    @get:Rule val compose = createComposeRule()
    private val facts = C2VisualFixtures(populated = true)
    private val empty = C2VisualFixtures(populated = false)
    private val project = facts.projects.items.single()
    private val subscription = facts.subscriptions.items.single()

    @Test fun current_history_empty_and_both_failures_keep_their_own_snapshot_contract() {
        val scenario = mutableStateOf(Scenario.CURRENT)
        val older = facts.overview.copy(asOf = facts.day.minusDays(1), projectCount = 8, subscriptionCount = 5,
            totalInvestmentCents = 15_123L)
        compose.setContent {
            TestCanvas {
                val overview = when (scenario.value) {
                    Scenario.CURRENT -> facts.overview.copy(totalInvestmentCents = 24_045L)
                    Scenario.HISTORY -> empty.overview.copy(projectCount = 4, subscriptionCount = 3, totalInvestmentCents = 0L)
                    Scenario.EMPTY, Scenario.RESTORE_UNKNOWN -> empty.overview.copy(totalInvestmentCents = 0L)
                    Scenario.NO_INVESTMENT -> facts.overview
                    Scenario.RETAINED -> older
                    Scenario.FAILED -> null
                }
                HomeShell(99, {}, {}, 98, {}, {}, overview = overview,
                    hasReadNotice = scenario.value in setOf(Scenario.RETAINED, Scenario.FAILED, Scenario.RESTORE_UNKNOWN),
                    readError = when (scenario.value) {
                        Scenario.RETAINED -> "本地记录暂时无法刷新，当前显示上次读取结果。"
                        Scenario.FAILED -> "本地记录暂时无法刷新。"
                        else -> null
                    })
            }
        }
        assertSummary("¥10.52/天", "1", "1", facts.day.toString(), investment = "¥240.45")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()

        compose.runOnIdle { scenario.value = Scenario.NO_INVESTMENT }
        assertSummary("¥10.52/天", "1", "1", facts.day.toString())
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()

        compose.runOnIdle { scenario.value = Scenario.HISTORY }
        assertSummary("¥0.00/天", "4", "3", facts.day.toString(), investment = "¥0.00")
        compose.onNodeWithTag("home-no-active-records").assertExists()
        compose.onNodeWithTag("open-project-list").assertExists()
        click("home-subscription-tab")
        compose.onNodeWithTag("open-subscription-list").assertExists()

        compose.runOnIdle { scenario.value = Scenario.EMPTY }
        assertSummary("—", "0", "0", facts.day.toString())
        compose.onNodeWithTag("home-empty-guidance").assertExists()
        compose.onNodeWithTag("home-category-disclosure").assertDoesNotExist()
        compose.onNodeWithTag("open-project-list").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertDoesNotExist()

        compose.runOnIdle { scenario.value = Scenario.RESTORE_UNKNOWN }
        assertSummary("—", "0", "0", facts.day.toString())
        compose.onNodeWithTag("home-retained-notice").assertTextEquals("当前记录状态尚未确认")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-overview-read-error").assertDoesNotExist()

        compose.runOnIdle { scenario.value = Scenario.RETAINED }
        assertSummary("¥10.52/天", "8", "5", older.asOf.toString(), investment = "¥151.23")
        compose.onNodeWithText(facts.day.toString()).assertDoesNotExist()
        compose.onNodeWithTag("home-retained-notice").assertTextEquals("上次取得的结果")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()

        compose.runOnIdle { scenario.value = Scenario.FAILED }
        assertSummary("—", "—", "—", null)
        compose.onNodeWithTag("home-overview-read-error").assertTextEquals("暂时无法取得成本数据")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-retained-notice").assertDoesNotExist()
        compose.onAllNodesWithTag("home-open-add-top").assertCountEquals(1)
        compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
    }

    @Test fun archive_limits_each_type_and_only_the_associated_unavailable_cover_is_read() {
        val cover = "96000000-0000-0000-0000-000000000008"
        val projectRows = (1..3).map { index ->
            project.copy(project = project.project.copy(id = "project-$index", name = "合成项目 $index",
                coverAssetId = cover.takeIf { index == 2 }))
        }
        val subscriptions = (1..3).map { index ->
            subscription.copy(subscription = subscription.subscription.copy(id = "subscription-$index",
                name = "合成订阅 $index"))
        }
        val reads = ConcurrentLinkedQueue<CoverReference>()
        val reader = object : CoverReader {
            override val generationId = "p2-contract-test"
            override val ownerKey = Any()
            override fun isCurrent() = true
            override suspend fun read(reference: CoverReference): CoverReadResult {
                reads.add(reference)
                return CoverReadResult.Unavailable(CoverReadFailure.INTEGRITY)
            }
        }
        compose.setContent {
            TestCanvas {
                ProvideCoverPreviews(reader) {
                    HomeShell(4, {}, {}, 5, {}, {}, overview = facts.overview.copy(projectCount = 4, subscriptionCount = 5),
                        projectPreview = facts.projects.copy(items = projectRows, totalCount = 4),
                        subscriptionPreview = facts.subscriptions.copy(items = subscriptions, totalCount = 5))
                }
            }
        }
        fun assertColumns(type: String) {
            val first = compose.onNodeWithTag("home-$type-preview-$type-1").performScrollTo().fetchSemanticsNode()
            val second = compose.onNodeWithTag("home-$type-preview-$type-2").fetchSemanticsNode()
            assertEquals(first.positionInRoot.y, second.positionInRoot.y, 1f)
            assertTrue(first.positionInRoot.x + first.size.width <= second.positionInRoot.x + 1f)
            compose.onNodeWithTag("home-$type-preview-$type-3").assertDoesNotExist()
        }
        assertColumns("project")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-unavailable").fetchSemanticsNodes().size == 1 }
        assertTouch("record-cover-unavailable")
        compose.onNodeWithTag("record-cover-unavailable").assertContentDescriptionEquals("封面暂不可用")
        val expectedReads = listOf(CoverReference(CoverOwnerType.PROJECT, "project-2", cover))
        assertEquals(expectedReads, reads.toList())
        click("home-subscription-tab")
        assertColumns("subscription")
        compose.onAllNodesWithTag("record-cover-unavailable").assertCountEquals(0)
        compose.runOnIdle { assertEquals(expectedReads, reads.toList()) }
        assertSummary("¥10.52/天", "4", "5", facts.day.toString())
    }

    @Test fun a_wrong_date_waits_and_the_other_preview_error_does_not_replace_the_ledger_summary() {
        val failed = mutableStateOf(false)
        var projectRetries = 0
        var subscriptionRetries = 0
        compose.setContent {
            TestCanvas {
                HomeShell(1, {}, {}, 1, {}, {}, overview = facts.overview,
                    projectPreview = facts.projects.copy(items = listOf(project.copy(asOf = facts.day.minusDays(1)))),
                    subscriptionPreview = facts.subscriptions.copy(error = if (failed.value) "订阅预览读取失败" else null),
                    onRetryProjectPreview = { projectRetries++ }, onRetrySubscriptionPreview = { subscriptionRetries++ })
            }
        }
        compose.onNodeWithTag(projectTag()).assertDoesNotExist()
        compose.onNodeWithText("预览日期正在更新").assertExists()
        click("home-subscription-tab")
        compose.onNodeWithTag(subscriptionTag()).assertExists()
        compose.runOnIdle { failed.value = true }
        compose.onNodeWithTag(subscriptionTag()).assertExists()
        compose.onAllNodesWithText("以下保留上次读取结果").assertCountEquals(1)
        compose.onNodeWithTag("home-retry-project-preview").assertDoesNotExist()
        compose.onNodeWithTag("home-retry-subscription-preview").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, projectRetries); assertEquals(1, subscriptionRetries) }
        compose.onNodeWithTag("ledger-retry-read").assertDoesNotExist()
        assertSummary("¥10.52/天", "1", "1", facts.day.toString())
    }

    @Test fun widths_fonts_and_themes_keep_one_amount_readout_and_reachable_controls() {
        val name = "每天通勤用来读长篇小说与整理批注的便携阅读器"
        val amount = BigDecimal("-987654321098765432109876543210.32")
        val longProject = project.copy(project = project.project.copy(name = name), cost = project.cost.copy(dailyRate = amount))
        val configuration = mutableStateOf(Canvas())
        compose.setContent {
            TestCanvas(configuration.value) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        HomeShell(1, {}, {}, 1, {}, {}, overview = facts.overview,
                            onSearch = {}, projectPreview = facts.projects.copy(items = listOf(longProject)),
                            subscriptionPreview = facts.subscriptions, onOpenProject = {}, onOpenSubscription = {})
                    }
                    Box(Modifier.testTag("p2-bottom-navigation")) { C2PrimaryNavigationBar(Home) {} }
                }
            }
        }
        for (appearance in LithicAppearance.entries) {
            for (width in listOf(320, 360, 400)) {
                for (font in listOf(1f, 2f)) {
                    compose.runOnIdle { configuration.value = Canvas(width, font, appearance) }
                    compose.onNodeWithTag("home-screen")
                        .performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
                    click("home-project-tab")
                    assertTouch("open-name-search")
                    assertTouch("home-open-add-top")
                    assertTouch("home-cost-information")
                    val search = compose.onNodeWithTag("open-name-search").fetchSemanticsNode()
                    val hero = compose.onNodeWithTag("home-total-rate").fetchSemanticsNode()
                    assertTrue("Read brand/search before the summary", search.boundsInRoot.top < hero.boundsInRoot.top)
                    val heroDescription = "当前日均成本，10.52元/天"
                    compose.onAllNodesWithContentDescription(heroDescription, useUnmergedTree = true).assertCountEquals(1)
                    compose.onNodeWithTag("home-total-rate").assertTextEquals("¥10.52/天")

                    val nameNode = compose.onNodeWithText(name, useUnmergedTree = true).performScrollTo()
                    val layout = mutableListOf<TextLayoutResult>()
                    nameNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
                    assertFalse(layout.single().hasVisualOverflow)
                    if (font == 2f) assertTrue(layout.single().lineCount > 1)
                    assertTouch(projectTag())

                    val spoken = "项目日均成本，负${amount.abs().toPlainString()}元/天"
                    compose.onAllNodesWithContentDescription(spoken, useUnmergedTree = true).assertCountEquals(1)
                    compose.onAllNodesWithText("/天", useUnmergedTree = true).assertCountEquals(0)
                    compose.onAllNodesWithText("元/天", useUnmergedTree = true).assertCountEquals(0)
                    val openAmount = compose.onNodeWithTag("amount-open-full", useUnmergedTree = true)
                        .performScrollTo().assertIsDisplayed()
                    openAmount.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                    openAmount.performClick()
                    compose.onNodeWithTag("amount-full-value").assertTextEquals("−¥${amount.abs().toPlainString()}/天")
                    compose.onNodeWithTag("amount-close-full").performClick()

                    val add = compose.onNodeWithTag("home-open-add").assertIsDisplayed()
                    val nav = compose.onNodeWithTag("p2-bottom-navigation").fetchSemanticsNode()
                    val addBounds = add.fetchSemanticsNode().boundsInRoot
                    assertTrue(addBounds.bottom <= nav.boundsInRoot.top + 1f)
                    compose.onAllNodesWithTag("home-open-add-top").assertCountEquals(1)
                    compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
                    add.assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).assertTextEquals("添加")
                    val viewport = compose.onNodeWithTag("home-scroll-viewport").fetchSemanticsNode().boundsInRoot
                    assertTrue("The scrolling viewport must end above the add button", viewport.bottom <= addBounds.top + 1f)
                    for (type in listOf("project", "subscription")) {
                        click("home-$type-tab")
                        val card = if (type == "project") projectTag() else subscriptionTag()
                        for (tag in listOf("open-$type-list", card, "home-category-disclosure")) {
                            val target = compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed().fetchSemanticsNode()
                            assertTrue("$tag must not enter the reserved add area", target.boundsInRoot.bottom <= addBounds.top + 1f)
                        }
                    }
                }
            }
        }
    }

    @Test fun a_wide_parent_centers_a_single_capped_reading_column_and_its_add_button() {
        compose.setContent {
            TestCanvas(Canvas(width = 760)) {
                HomeShell(0, {}, {}, 0, {}, {}, overview = empty.overview)
            }
        }
        val canvas = compose.onNodeWithTag("p2-home-canvas").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("home-scroll-viewport").assertWidthIsEqualTo(560.dp)
            .fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithTag("home-open-add").fetchSemanticsNode().boundsInRoot
        assertTrue(canvas.width > viewport.width)
        assertEquals(canvas.center.x, viewport.center.x, 1f)
        assertTrue(add.right <= viewport.right + 1f && add.left >= viewport.left)
        assertEquals(viewport.center.x, add.center.x, 1f)
    }

    @Test fun home_controls_dispatch_exact_targets_and_tab_bar_keeps_three_targets() {
        val opened = mutableListOf<String>()
        val selected = mutableStateOf<NavKey>(Home)
        val destinations = mutableListOf<NavKey>()
        val categories = mutableListOf<Pair<OverviewKind, String>>()
        compose.setContent {
            TestCanvas {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        HomeShell(1, { opened += "project-list" }, { opened += "create-project" },
                            1, { opened += "subscription-list" }, { opened += "create-subscription" },
                            overview = facts.overview, onSearch = { opened += "search" },
                            onCategoryAnalysis = { kind, id -> categories += kind to id },
                            projectPreview = facts.projects, subscriptionPreview = facts.subscriptions,
                            onOpenProject = { opened += "project:$it" }, onOpenSubscription = { opened += "subscription:$it" },
                            onCalculationExplanation = { opened += "calculation" })
                    }
                    C2PrimaryNavigationBar(selected.value) { destinations += it; selected.value = it }
                }
            }
        }
        click("open-name-search")
        compose.onNodeWithTag("home-project-tab").assertIsSelected()
        compose.onNodeWithTag("open-project-list").assertContentDescriptionEquals("查看全部长期项目")
        click("open-project-list")
        click(projectTag())
        compose.onNodeWithTag(subscriptionTag()).assertDoesNotExist()
        click("home-subscription-tab")
        compose.onNodeWithTag("home-subscription-tab").assertIsSelected()
        compose.onNodeWithTag(projectTag()).assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertContentDescriptionEquals("查看全部订阅服务")
        click("open-subscription-list")
        click(subscriptionTag())
        val browse = listOf("search", "project-list", "project:${project.project.id}", "subscription-list",
            "subscription:${subscription.subscription.id}")
        compose.runOnIdle { assertEquals(browse, opened) }

        click("home-category-disclosure")
        val projectCategory = facts.overview.projectCategories.single().categoryId
        val subscriptionCategory = facts.overview.subscriptionCategories.single().categoryId
        click("home-project-category-open-$projectCategory")
        click("home-category-disclosure")
        click("home-subscription-category-open-$subscriptionCategory")
        compose.runOnIdle {
            assertEquals(listOf(OverviewKind.PROJECT to projectCategory, OverviewKind.SUBSCRIPTION to subscriptionCategory), categories)
        }
        click("home-cost-information")
        compose.onNodeWithTag("home-count-scope")
            .assertTextContains("含以旧换新非现金折抵", substring = true)
            .assertTextContains("订阅实际付款（按实付日）", substring = true)
            .assertTextContains("不扣除收回", substring = true)
        pressBack()
        compose.runOnIdle { assertEquals(browse, opened) }
        click("home-cost-information")
        click("home-full-calculation-explanation", scroll = false)

        click("home-open-add-top")
        assertTouch("home-create-project", scroll = false)
        assertTouch("home-create-subscription", scroll = false)
        pressBack()
        compose.runOnIdle { assertEquals(browse + "calculation", opened) }
        click("home-open-add", scroll = false)
        click("home-create-project", scroll = false)
        click("home-open-add-top")
        click("home-create-subscription", scroll = false)
        compose.runOnIdle { assertEquals(browse + listOf("calculation", "create-project", "create-subscription"), opened) }

        for (tab in listOf("tab-review", "tab-settings", "tab-now")) {
            click(tab, scroll = false)
            compose.onNodeWithTag(tab).assertIsSelected()
        }
        compose.runOnIdle { assertEquals(listOf(Review, Settings, Home), destinations) }
    }

    private fun projectTag() = "home-project-preview-${project.project.id}"
    private fun subscriptionTag() = "home-subscription-preview-${subscription.subscription.id}"

    private fun assertSummary(amount: String, projects: String, subscriptions: String, cutoff: String?,
        investment: String = "—",
    ) {
        compose.onNodeWithTag("home-total-rate").assertTextEquals(amount)
        compose.onNodeWithTag("home-total-investment").assertTextEquals(investment)
        for ((kind, count) in listOf("project" to projects, "subscription" to subscriptions)) {
            compose.onNodeWithTag("home-$kind-record-count").assertTextEquals(count).assert(hasClickAction().not())
        }
        if (cutoff != null) compose.onNodeWithTag("home-cutoff-date").assertTextEquals(cutoff)
        else {
            compose.onNodeWithTag("home-cutoff-date").assertDoesNotExist()
            compose.onNodeWithText("非今日扣款", substring = true).assertDoesNotExist()
        }
    }

    private fun assertTouch(tag: String, scroll: Boolean = true): SemanticsNodeInteraction {
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        return node.assertIsDisplayed().assertHasClickAction().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
    }

    private fun click(tag: String, scroll: Boolean = true) = assertTouch(tag, scroll).performClick()

    @Composable
    private fun TestCanvas(configuration: Canvas = Canvas(), content: @Composable () -> Unit) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = constraints.maxWidth / configuration.width.toFloat()
            val height = minOf(820f, constraints.maxHeight / density).dp
            CompositionLocalProvider(LocalDensity provides Density(density, configuration.fontScale)) {
                LithicTheme(appearance = configuration.appearance) {
                    Box(Modifier.requiredSize(configuration.width.dp, height).testTag("p2-home-canvas")) { content() }
                }
            }
        }
    }

    private data class Canvas(val width: Int = 360, val fontScale: Float = 1f,
        val appearance: LithicAppearance = LithicAppearance.STONE_MIST)

    private enum class Scenario { CURRENT, HISTORY, EMPTY, RESTORE_UNKNOWN, RETAINED, FAILED, NO_INVESTMENT }
}
