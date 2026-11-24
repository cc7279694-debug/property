package com.cooper.lithic.feature.archive

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.project.ProjectHistoryScreen
import com.cooper.lithic.feature.project.ProjectListScreen
import com.cooper.lithic.feature.subscription.M09SubscriptionFixtures
import com.cooper.lithic.feature.subscription.RenewalHistoryUiState
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import com.cooper.lithic.feature.subscription.SubscriptionHistoryScreen
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import com.cooper.lithic.ui.C2VisualFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import kotlin.math.abs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production archive routes rendered with synthetic facts and simulated constraints. */
@RunWith(AndroidJUnit4::class)
class V4R3ArchiveRowsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun all_four_rows_keep_identity_time_and_cost_at_each_phone_width_and_font_scale() {
        val facts = C2VisualFixtures(true)
        val original = facts.projects.items.single()
        val current = original.copy(project = original.project.copy(name = "每天通勤读长篇小说与整理学习批注的便携电子阅读器"))
        val endedProject = original.project.copy(
            id = "94000000-0000-0000-0000-000000000011",
            name = "以前每天通勤读小说与整理学习批注的便携电子阅读器",
            endedOn = facts.day.minusDays(10),
        )
        val endedEvents = original.events.map { it.copy(projectId = endedProject.id) }
        val ended = original.copy(
            project = endedProject,
            events = endedEvents,
            cost = ProjectCostEngine.evaluate(
                ProjectSnapshot(
                    LongTermProject(endedProject.startedOn, endedProject.endedOn),
                    endedEvents.map { ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn) },
                ),
                facts.day,
            ),
        )
        val active = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(
            name = "每天通勤收听外语有声书与阅读教学资料的订阅服务",
        ))
        val stopped = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(
            name = "以前通勤收听外语有声书与阅读教学资料的订阅服务",
            operation = SubscriptionOperationState.STOPPED,
        ))
        val rows = listOf(
            RowFacts("project-list-content", "project-row", current.project.id, current.project.name,
                current.category.name, "持有中", listOf("持有 121天"), "当前日均成本", "9.92"),
            RowFacts("project-history-content", "project-history-row", ended.project.id, ended.project.name,
                ended.category.name, "已结束", listOf("持有 111天", endedProject.endedOn.toString(), "结果截至 ${facts.day}"),
                "项目日均成本", "10.81"),
            RowFacts("subscription-list-content", "subscription-row", active.subscription.id, active.subscription.name,
                active.category.name, "订阅中", listOf("最近实际付款：2026-01-01", "折算付费时长：30天"),
                "历史平均日成本", "5.00"),
            RowFacts("subscription-history-content", "subscription-history-row", stopped.subscription.id, stopped.subscription.name,
                stopped.category.name, "已停止", listOf("折算付费时长：30天"), "历史平均日成本", "5.00"),
        )
        var page by mutableStateOf(0)
        var width by mutableStateOf(320)
        var fontScale by mutableStateOf(1f)
        val opened = mutableListOf<String>()
        val open: (String) -> Unit = { opened.add(it) }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = minOf(constraints.maxWidth / width.toFloat(), constraints.maxHeight / 640f)
                val simulatedFontScale = fontScale
                val configuration = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp = width
                    screenHeightDp = 640
                    this.fontScale = simulatedFontScale
                }
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale),
                    LocalConfiguration provides configuration) {
                    LithicTheme(when (width) {
                        320 -> LithicAppearance.PURE_WHITE
                        360 -> LithicAppearance.STONE_MIST
                        else -> LithicAppearance.CHARCOAL
                    }) {
                        Box(Modifier.requiredSize(width.dp, 640.dp)) {
                            key(page, width, fontScale) {
                                when (page) {
                                    0 -> ProjectListScreen(facts.projects.copy(items = listOf(current)), {}, {}, open, {}, {})
                                    1 -> ProjectHistoryScreen(facts.projects.copy(items = listOf(ended)), {}, open, {}, {})
                                    2 -> SubscriptionListScreen(facts.subscriptions.copy(items = listOf(active)), {}, {}, open, {}, {})
                                    else -> SubscriptionHistoryScreen(facts.subscriptions.copy(items = listOf(stopped)), {}, open, {}, {})
                                }
                            }
                        }
                    }
                }
            }
        }
        for (targetWidth in listOf(320, 360, 400)) for (targetScale in listOf(1f, 2f)) {
            rows.forEachIndexed { index, expected ->
                compose.runOnIdle {
                    page = index
                    width = targetWidth
                    fontScale = targetScale
                }
                val tag = "${expected.tagPrefix}-${expected.id}"
                val inRow = hasAnyAncestor(hasTestTag(tag))
                compose.onNodeWithTag(expected.contentTag).performScrollToNode(hasTestTag(tag))
                compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp).assertHasClickAction()
                listOf(expected.status, expected.category, expected.metric).forEach { text ->
                    compose.onNode(inRow and hasText(text), useUnmergedTree = true).assertIsDisplayed()
                }
                expected.time.forEach { time ->
                    compose.onNode(inRow and hasText(time, substring = true), useUnmergedTree = true).assertIsDisplayed()
                }

                val shownAmount = "¥${expected.amount}/天"
                val costDescription = "${expected.metric}，${expected.amount}元/天"
                val metric = hasContentDescription(costDescription)
                compose.onNode(inRow and metric, useUnmergedTree = true).assertIsDisplayed().assertTextEquals(shownAmount)
                val amount = compose.onNode(
                    inRow and hasAnyAncestor(metric) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                    useUnmergedTree = true,
                )
                assertFullyVisibleText(amount, "¥${expected.amount}\u2009/天", minimumLines = 1)

                val name = compose.onNode(inRow and hasText(expected.name), useUnmergedTree = true)
                assertFullyVisibleText(name, expected.name, minimumLines = 2)
                val amountBounds = amount.fetchSemanticsNode().boundsInRoot
                val nameBounds = name.fetchSemanticsNode().boundsInRoot
                if (targetWidth == 400 && targetScale == 1f) {
                    assertTrue("Daily amount should sit beside the identity", amountBounds.left >= nameBounds.right - 1f)
                } else {
                    assertTrue("Daily amount should sit below the name", amountBounds.top >= nameBounds.bottom - 1f)
                }
                name.performTouchInput { click() }
                compose.runOnIdle { assertEquals(expected.id, opened.last()) }
            }
        }
        assertEquals(24, opened.size)
    }

    @Test fun detail_columns_and_fixed_actions_share_a_centered_width_without_overlay() {
        val fixture = C2VisualFixtures(true)
        val project = fixture.projects.items.single()
        val subscription = fixture.subscriptions.items.single()
        val subscriptionState = SubscriptionDetailUiState(
            detail = subscription,
            history = RenewalHistoryUiState(subscription.renewals, 1, loading = false),
            loading = false,
        )
        var showProject by mutableStateOf(true)
        var width by mutableStateOf(320)
        var fontScale by mutableStateOf(1f)
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = minOf(constraints.maxWidth / width.toFloat(), constraints.maxHeight / 640f)
                val simulatedFontScale = fontScale
                val configuration = Configuration(LocalConfiguration.current).apply {
                    screenWidthDp = width
                    screenHeightDp = 640
                    this.fontScale = simulatedFontScale
                }
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale),
                    LocalConfiguration provides configuration) {
                    LithicTheme {
                        Box(Modifier.requiredSize(width.dp, 640.dp)) {
                            if (showProject) ProjectDetailScreen(ProjectDetailUiState(project, loading = false), {}, {}, {}, {})
                            else SubscriptionDetailScreen(subscriptionState, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        for (projectPage in listOf(true, false)) for (targetWidth in listOf(320, 360, 400, 680)) {
            for (targetScale in listOf(1f, 2f)) {
                compose.runOnIdle { showProject = projectPage; width = targetWidth; fontScale = targetScale }
                val screen = if (projectPage) "project" else "subscription"
                val actionTag = if (projectPage) "project-record-money" else "subscription-detail-renewal"
                val parent = compose.onNodeWithTag("$screen-detail-screen").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                val viewport = compose.onNodeWithTag("$screen-detail-content").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
                val action = compose.onNodeWithTag(actionTag).assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                    .fetchSemanticsNode().boundsInRoot
                val projectedWidth = parent.width * minOf(targetWidth, 560) / targetWidth
                assertTrue("Content width at $targetWidth dp", abs(viewport.width - projectedWidth) <= 2f)
                assertTrue("Content centered at $targetWidth dp", abs(parent.center.x - viewport.center.x) <= 2f)
                assertTrue("Action centered at $targetWidth dp", abs(action.center.x - viewport.center.x) <= 2f)
                assertTrue("Action overlays scroll viewport at $targetWidth dp", viewport.bottom <= action.top + 1f)
            }
        }
    }

    private fun assertFullyVisibleText(interaction: SemanticsNodeInteraction, expected: String, minimumLines: Int) {
        val node = interaction.assertIsDisplayed().fetchSemanticsNode()
        val results = mutableListOf<TextLayoutResult>()
        interaction.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        val bounds = node.boundsInRoot
        assertEquals(expected, layout.layoutInput.text.text)
        assertTrue("Expected at least $minimumLines visible lines in $expected", layout.lineCount >= minimumLines)
        assertFalse("Text overflows vertically: $expected", layout.didOverflowHeight)
        assertTrue("Text node is clipped: $expected",
            bounds.width + 1f >= node.size.width && bounds.height + 1f >= node.size.height)
        for (line in 0 until layout.lineCount) {
            assertFalse("Line $line is ellipsized: $expected", layout.isLineEllipsized(line))
            assertTrue("Line $line exceeds the visible bounds: $expected",
                layout.getLineLeft(line) >= -1f && layout.getLineRight(line) <= bounds.width + 1f &&
                    layout.getLineTop(line) >= -1f && layout.getLineBottom(line) <= bounds.height + 1f)
        }
        assertEquals("Last character must be visible: $expected", expected.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
    }

    private data class RowFacts(
        val contentTag: String,
        val tagPrefix: String,
        val id: String,
        val name: String,
        val category: String,
        val status: String,
        val time: List<String>,
        val metric: String,
        val amount: String,
    )
}
