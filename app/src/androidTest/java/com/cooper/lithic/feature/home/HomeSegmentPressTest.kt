package com.cooper.lithic.feature.home

import android.os.SystemClock
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.ui.C2VisualFixtures
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises the production home using synthetic records and simulated font scaling. */
class HomeSegmentPressTest {
    @get:Rule val compose = createComposeRule()

    private enum class Tab(val key: String, val label: String, val allLabel: String) {
        PROJECT("project", "项目", "查看全部长期项目"),
        SUBSCRIPTION("subscription", "订阅", "查看全部订阅服务"),
    }

    private data class Display(val appearance: LithicAppearance, val fontScale: Float)

    private val facts = C2VisualFixtures(populated = true)

    @Test fun held_feedback_stays_inside_the_capsule_and_clears_in_each_theme_at_both_font_scales() {
        val display = mutableStateOf(Display(LithicAppearance.STONE_MIST, 1f))
        render(display = { display.value })

        for (appearance in LithicAppearance.entries) {
            for (fontScale in listOf(1f, 2f)) {
                val configuration = Display(appearance, fontScale)
                compose.runOnIdle { display.value = configuration }
                assertSelection(Tab.PROJECT)
                assertPress(Tab.SUBSCRIPTION, cancelled = true, display = configuration)
                assertSelection(Tab.PROJECT)
                assertPress(Tab.PROJECT, cancelled = false, display = configuration)
                assertSelection(Tab.PROJECT)

                tap(Tab.SUBSCRIPTION)
                assertSelection(Tab.SUBSCRIPTION)
                waitForRipple(500)
                assertPress(Tab.PROJECT, cancelled = true, display = configuration)
                assertSelection(Tab.SUBSCRIPTION)
                assertPress(Tab.SUBSCRIPTION, cancelled = false, display = configuration)
                assertSelection(Tab.SUBSCRIPTION)
                tap(Tab.PROJECT)
                waitForRipple(500)
            }
        }
    }

    @Test fun switching_rapidly_and_reselecting_keeps_tab_semantics_and_matching_preview_and_list() {
        val openedLists = mutableListOf<Tab>()
        val display = mutableStateOf(Display(LithicAppearance.STONE_MIST, 1f))
        render(display = { display.value }) { openedLists += it }
        for (fontScale in listOf(1f, 2f)) {
            compose.runOnIdle {
                display.value = display.value.copy(fontScale = fontScale)
                openedLists.clear()
            }
            assertBrowse(Tab.PROJECT)
            tap(Tab.SUBSCRIPTION)
            assertBrowse(Tab.SUBSCRIPTION)
            tap(Tab.SUBSCRIPTION)
            assertBrowse(Tab.SUBSCRIPTION)
            tap(Tab.PROJECT)
            assertBrowse(Tab.PROJECT)
            tap(Tab.PROJECT)
            assertBrowse(Tab.PROJECT)
            waitForRipple(500)
            val before = Tab.entries.associateWith { node(it).performScrollTo().captureToImage().toPixelMap() }

            rapidTap(listOf(Tab.SUBSCRIPTION, Tab.PROJECT, Tab.SUBSCRIPTION, Tab.SUBSCRIPTION))
            assertBrowse(Tab.SUBSCRIPTION)
            rapidTap(listOf(Tab.PROJECT, Tab.SUBSCRIPTION, Tab.PROJECT, Tab.PROJECT))
            assertBrowse(Tab.PROJECT)
            waitForRipple(500)
            for ((tab, baseline) in before) {
                val recovered = node(tab).performScrollTo().captureToImage().toPixelMap()
                assertRestored(baseline, recovered, "$fontScale / rapid / $tab")
            }

            compose.runOnIdle {
                assertEquals(listOf(Tab.PROJECT, Tab.SUBSCRIPTION, Tab.SUBSCRIPTION, Tab.PROJECT, Tab.PROJECT,
                    Tab.SUBSCRIPTION, Tab.PROJECT), openedLists)
            }
        }
    }

    private fun render(display: () -> Display, onListOpen: (Tab) -> Unit = {}) {
        compose.setContent {
            val current = display()
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, current.fontScale)) {
                LithicTheme(appearance = current.appearance) {
                    HomeShell(
                        projectCount = facts.overview.projectCount,
                        onProjectList = { onListOpen(Tab.PROJECT) },
                        onCreateProject = {},
                        subscriptionCount = facts.overview.subscriptionCount,
                        onSubscriptionList = { onListOpen(Tab.SUBSCRIPTION) },
                        onCreateSubscription = {},
                        overview = facts.overview,
                        projectPreview = facts.projects,
                        subscriptionPreview = facts.subscriptions,
                    )
                }
            }
        }
    }

    private fun node(tab: Tab) = compose.onNodeWithTag("home-${tab.key}-tab")

    private fun preview(tab: Tab) = compose.onNodeWithTag(when (tab) {
        Tab.PROJECT -> "home-project-preview-${facts.projects.items.single().project.id}"
        Tab.SUBSCRIPTION -> "home-subscription-preview-${facts.subscriptions.items.single().subscription.id}"
    })

    private fun assertSelection(selected: Tab) {
        node(selected).performScrollTo()
        for (tab in Tab.entries) {
            val target = node(tab).assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
                .assertHasClickAction().assertTextEquals(tab.label)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            if (tab == selected) target.assertIsSelected() else target.assertIsNotSelected()
        }
    }

    private fun assertBrowse(selected: Tab) {
        assertSelection(selected)
        val inactive = Tab.entries.single { it != selected }
        preview(inactive).assertDoesNotExist()
        compose.onNodeWithTag("open-${inactive.key}-list").assertDoesNotExist()
        preview(selected).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("open-${selected.key}-list").performScrollTo().assertIsDisplayed()
            .assertContentDescriptionEquals(selected.allLabel).performTouchInput { click() }
    }

    private fun tap(tab: Tab) {
        node(tab).performScrollTo().performTouchInput { click() }
    }

    private fun rapidTap(sequence: List<Tab>) {
        node(sequence.first()).performScrollTo()
        val viewport = compose.onNodeWithTag("home-scroll-viewport")
        val origin = viewport.fetchSemanticsNode().boundsInRoot.topLeft
        val centers = sequence.map { node(it).fetchSemanticsNode().boundsInRoot.center - origin }
        viewport.performTouchInput {
            for (point in centers) {
                click(point)
                advanceEventTime(24)
            }
        }
    }

    private fun assertPress(tab: Tab, cancelled: Boolean, display: Display) {
        val target = node(tab).performScrollTo()
        val before = target.captureToImage().toPixelMap()
        val edgeY = with(compose.density) { 3.dp.toPx() }
        val context = "$display / $tab / ${if (cancelled) "cancel" else "release"}"
        // This is inside the 48dp hit target but outside the vertically inset painted capsule.
        target.performTouchInput { down(Offset(center.x, edgeY)) }
        try {
            waitForRipple(350)
            assertClipped(before, target.captureToImage().toPixelMap(), context)
        } finally {
            target.performTouchInput { if (cancelled) cancel() else up() }
        }
        waitForRipple(500)
        assertRestored(before, target.captureToImage().toPixelMap(), context)
    }

    private fun waitForRipple(milliseconds: Long) {
        // Android RippleDrawable can animate independently of the Compose test clock.
        val until = SystemClock.uptimeMillis() + milliseconds
        compose.waitUntil(timeoutMillis = 5_000) { SystemClock.uptimeMillis() >= until }
    }

    private fun assertClipped(before: PixelMap, held: PixelMap, context: String) {
        assertSameSize(before, held, context)
        val inset = with(compose.density) { 7.dp.roundToPx() }
        val tolerance = with(compose.density) { 2.dp.toPx() }
        val radius = (held.height / 2f - inset).coerceAtMost(held.width / 2f)
        val halfStraightWidth = held.width / 2f - radius
        var changedInterior = false
        for (y in 0 until held.height) {
            for (x in 0 until held.width) {
                if (!changed(before[x, y], held[x, y])) continue
                val dx = (abs(x + 0.5f - held.width / 2f) - halfStraightWidth).coerceAtLeast(0f)
                val dy = y + 0.5f - held.height / 2f
                val squaredDistance = dx * dx + dy * dy
                val outerLimit = radius + tolerance
                val innerLimit = radius - tolerance
                assertTrue("$context: feedback escaped the capsule at ($x, $y)",
                    squaredDistance < outerLimit * outerLimit)
                if (squaredDistance < innerLimit * innerLimit) changedInterior = true
            }
        }
        assertTrue("$context: the held touch must draw visible feedback inside the capsule", changedInterior)
    }

    private fun assertRestored(before: PixelMap, actual: PixelMap, context: String) {
        assertSameSize(before, actual, context)
        for (y in 0 until before.height) {
            for (x in 0 until before.width) {
                if (changed(before[x, y], actual[x, y])) {
                    throw AssertionError("$context: feedback did not clear at ($x, $y)")
                }
            }
        }
    }

    private fun assertSameSize(before: PixelMap, actual: PixelMap, context: String) {
        assertEquals("$context: width", before.width, actual.width)
        assertEquals("$context: height", before.height, actual.height)
    }

    private fun changed(before: Color, actual: Color): Boolean =
        maxOf(abs(before.red - actual.red), abs(before.green - actual.green), abs(before.blue - actual.blue)) > 3f / 255f
}
