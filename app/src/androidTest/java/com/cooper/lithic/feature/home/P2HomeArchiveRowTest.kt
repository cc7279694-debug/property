package com.cooper.lithic.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.media.CoverReadFailure
import com.cooper.lithic.data.media.CoverReadResult
import com.cooper.lithic.data.media.CoverReader
import com.cooper.lithic.data.media.CoverReference
import com.cooper.lithic.feature.media.ProvideCoverPreviews
import com.cooper.lithic.ui.C2VisualFixtures
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Only synthetic rows and a read stub. No production ledger or user media is opened. */
class P2HomeArchiveRowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun previews_keep_each_type_order_limit_and_distinct_costs_without_reading_absent_covers() {
        val facts = C2VisualFixtures(true)
        val project = facts.projects.items.single()
        val subscription = facts.subscriptions.items.single()
        val projects = (1..3).map {
            project.copy(project = project.project.copy(id = "project-$it", name = "物品 $it"))
        }
        val subscriptions = (1..3).map {
            subscription.copy(subscription = subscription.subscription.copy(id = "subscription-$it", name = "服务 $it"))
        }
        val reader = UnavailableReader()
        compose.setContent {
            LithicTheme {
                ProvideCoverPreviews(reader) {
                    HomeShell(3, {}, {}, 3, {}, {}, overview = facts.overview,
                        projectPreview = facts.projects.copy(items = projects, totalCount = 3),
                        subscriptionPreview = facts.subscriptions.copy(items = subscriptions, totalCount = 3))
                }
            }
        }
        for (type in listOf("project", "subscription")) {
            val first = compose.onNodeWithTag("home-$type-preview-$type-1").assert(hasClickAction().not())
            val second = compose.onNodeWithTag("home-$type-preview-$type-2")
            first.performScrollTo()
            assertTrue(first.fetchSemanticsNode().positionInRoot.y < second.fetchSemanticsNode().positionInRoot.y)
            compose.onNodeWithTag("home-$type-preview-$type-3").assertDoesNotExist()
        }
        compose.onAllNodesWithText("数码", useUnmergedTree = true)
            .filterToOne(hasAnyAncestor(hasTestTag("home-project-preview-project-1"))).assertExists()
        compose.onAllNodesWithContentDescription("当前日均成本，9.92元/天", useUnmergedTree = true)
            .filterToOne(hasAnyAncestor(hasTestTag("home-project-preview-project-1"))).assertExists()
        compose.onAllNodesWithContentDescription("历史平均日成本，0.60元/天", useUnmergedTree = true)
            .filterToOne(hasAnyAncestor(hasTestTag("home-subscription-preview-subscription-1"))).assertExists()
        compose.onAllNodesWithTag("record-cover-unavailable", useUnmergedTree = true).assertCountEquals(0)
        compose.runOnIdle { assertEquals(0, reader.reads.get()) }
    }

    @Test fun unavailable_cover_action_and_details_are_separate() {
        val facts = C2VisualFixtures(true)
        val project = facts.projects.items.single()
        val linked = project.copy(project = project.project.copy(coverAssetId = "synthetic-cover"))
        val reader = UnavailableReader()
        val opens = mutableListOf<String>()
        var writes = 0
        compose.setContent {
            LithicTheme {
                ProvideCoverPreviews(reader) {
                    HomeShell(1, {}, { writes++ }, 1, {}, { writes++ }, overview = facts.overview,
                        projectPreview = facts.projects.copy(items = listOf(linked)),
                        subscriptionPreview = facts.subscriptions, onOpenProject = { opens.add(it) })
                }
            }
        }
        val row = compose.onNodeWithTag("home-project-preview-${project.project.id}").performScrollTo()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("record-cover-unavailable").fetchSemanticsNodes().size == 1 }
        val error = compose.onNodeWithTag("record-cover-unavailable").assertContentDescriptionEquals("封面暂不可用")
        error.assertWidthIsAtLeast(80.dp).assertHeightIsAtLeast(60.dp)
        error.assert(hasAnyAncestor(hasTestTag("home-project-preview-${project.project.id}")).not())
        error.performClick()
        compose.onNodeWithText("封面暂不可用").assertIsDisplayed()
        compose.onNodeWithText("知道了").performClick()
        compose.runOnIdle { assertTrue(opens.isEmpty()); assertEquals(0, writes) }
        row.performClick()
        compose.runOnIdle { assertEquals(listOf(project.project.id), opens); assertEquals(1, reader.reads.get()) }
    }

    @Test fun long_name_wraps_and_long_amount_moves_below_facts_at_local_double_font_scale() {
        val facts = C2VisualFixtures(true)
        val original = facts.projects.items.single()
        val name = "每天通勤用来读长篇小说与整理批注的便携阅读器"
        val row = original.copy(project = original.project.copy(name = name),
            cost = original.cost.copy(dailyRate = BigDecimal("-9876543210987654.32")))
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val scale = constraints.maxWidth / 320f
                val height = minOf(800f, constraints.maxHeight / scale).dp
                CompositionLocalProvider(LocalDensity provides Density(scale, 2f)) {
                    LithicTheme {
                        Box(Modifier.requiredSize(320.dp, height)) {
                            HomeShell(1, {}, {}, 1, {}, {}, overview = facts.overview,
                                projectPreview = facts.projects.copy(items = listOf(row)),
                                subscriptionPreview = facts.subscriptions)
                        }
                    }
                }
            }
        }
        val nameNode = compose.onNodeWithText(name, useUnmergedTree = true).performScrollTo()
        val layout = mutableListOf<TextLayoutResult>()
        nameNode.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertTrue(layout.single().lineCount > 1)
        assertFalse(layout.single().hasVisualOverflow)
        val time = compose.onNodeWithText("持有 ${row.cost.heldDays} 天", useUnmergedTree = true).fetchSemanticsNode()
        val amount = compose.onNodeWithContentDescription("当前日均成本，负9876543210987654.32元/天", useUnmergedTree = true)
        assertTrue(amount.fetchSemanticsNode().positionInRoot.y >= time.positionInRoot.y + time.size.height - 1f)
        compose.onNodeWithTag("amount-open-full").performScrollTo().performClick()
        compose.onNodeWithTag("amount-full-value").assertTextEquals("−¥9876543210987654.32/天")
    }

    private class UnavailableReader : CoverReader {
        override val generationId = "synthetic-generation"
        override val ownerKey = Any()
        val reads = AtomicInteger()
        override fun isCurrent() = true
        override suspend fun read(reference: CoverReference): CoverReadResult {
            reads.incrementAndGet()
            return CoverReadResult.Unavailable(CoverReadFailure.INTEGRITY)
        }
    }
}
