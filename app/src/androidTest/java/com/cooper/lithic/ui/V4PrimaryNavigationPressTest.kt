package com.cooper.lithic.ui

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.Settings
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.appearance.LithicAppearance
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Tests actual Android touch feedback on the production navigation component with no ledger. */
class V4PrimaryNavigationPressTest {
    @get:Rule val compose = createComposeRule()

    private data class Tab(val key: NavKey, val tag: String, val name: String)

    private val home = Tab(Home, "tab-now", "首页")
    private val review = Tab(Review, "tab-review", "回看")
    private val settings = Tab(Settings, "tab-settings", "设置")
    private val tabs = listOf(home, review, settings)

    @Test fun held_feedback_stays_in_circle_and_recovers_after_release_or_cancel_in_each_theme() {
        val theme = mutableStateOf(LithicAppearance.STONE_MIST)
        val clicks = mutableListOf<NavKey>()
        render(theme = { theme.value }, selected = { Home }, onClick = { clicks += it })

        for (appearance in LithicAppearance.entries) {
            compose.runOnIdle {
                theme.value = appearance
                clicks.clear()
            }
            assertSelection(home)

            // Hold an unselected tab by its 48dp touch target outside the 40dp circle.
            val unselected = node(review)
            val beforeCancel = unselected.captureToImage().toPixelMap()
            unselected.performTouchInput { down(center + Offset(height * 0.52f, 0f)) }
            try {
                waitForRipple(350)
                assertHeldFeedback(beforeCancel, unselected.captureToImage().toPixelMap(), "$appearance / cancel")
                compose.runOnIdle { assertTrue("A held touch must not select", clicks.isEmpty()) }
            } finally {
                unselected.performTouchInput { cancel() }
            }
            waitForRipple(500)
            assertRestored(beforeCancel, unselected.captureToImage().toPixelMap(), "$appearance / cancel")
            compose.runOnIdle { assertTrue("Cancelling the gesture must not select", clicks.isEmpty()) }

            // Reselect Home to keep the selected color fixed while comparing the press pixels.
            val selected = node(home)
            val beforeRelease = selected.captureToImage().toPixelMap()
            selected.performTouchInput { down(center) }
            try {
                waitForRipple(350)
                assertHeldFeedback(beforeRelease, selected.captureToImage().toPixelMap(), "$appearance / release")
                compose.runOnIdle { assertTrue("A held touch must not select", clicks.isEmpty()) }
            } finally {
                selected.performTouchInput { up() }
            }
            waitForRipple(500)
            assertRestored(beforeRelease, selected.captureToImage().toPixelMap(), "$appearance / release")
            assertSelection(home)
            compose.runOnIdle { assertEquals(listOf(Home), clicks) }
        }
    }

    @Test fun touch_navigation_keeps_tab_semantics_and_dispatches_rapid_switches_and_reselection_once() {
        val selected = mutableStateOf<NavKey>(Home)
        val clicks = mutableListOf<NavKey>()
        render(theme = { LithicAppearance.STONE_MIST }, selected = { selected.value }) {
            clicks += it
            selected.value = it
        }
        assertSelection(home)
        for (tab in listOf(review, settings, home)) {
            node(tab).performTouchInput { click() }
            assertSelection(tab)
        }
        compose.runOnIdle {
            assertEquals(listOf(Review, Settings, Home), clicks)
            clicks.clear()
        }

        val capsule = compose.onNodeWithTag("lithic-primary-navigation")
        val origin = capsule.fetchSemanticsNode().boundsInRoot.topLeft
        val sequence = listOf(review, settings, home, home)
        val centers = sequence.map { node(it).fetchSemanticsNode().boundsInRoot.center - origin }
        capsule.performTouchInput {
            for (point in centers) {
                click(point)
                advanceEventTime(24)
            }
        }
        assertSelection(home)
        compose.runOnIdle { assertEquals(sequence.map { it.key }, clicks) }
    }

    private fun render(theme: () -> LithicAppearance, selected: () -> NavKey, onClick: (NavKey) -> Unit) {
        compose.setContent {
            LithicTheme(appearance = theme()) {
                Box(
                    Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    C2PrimaryNavigationBar(selected(), onClick)
                }
            }
        }
    }

    private fun node(tab: Tab) = compose.onNodeWithTag(tab.tag)

    private fun assertSelection(selected: Tab) {
        for (tab in tabs) {
            val target: SemanticsNodeInteraction = node(tab)
                .assertIsDisplayed()
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
                .assertHasClickAction()
                .assertContentDescriptionEquals(tab.name)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            if (tab == selected) target.assertIsSelected() else target.assertIsNotSelected()
        }
    }

    private fun waitForRipple(milliseconds: Long) {
        // Platform RippleDrawable animation runs against Android uptime, outside the Compose test clock.
        val until = SystemClock.uptimeMillis() + milliseconds
        compose.waitUntil(timeoutMillis = 5_000) { SystemClock.uptimeMillis() >= until }
    }

    private fun assertHeldFeedback(before: PixelMap, held: PixelMap, context: String) {
        assertSize(before, held, context)
        val centerX = (held.width - 1) / 2f
        val centerY = (held.height - 1) / 2f
        val scale = held.height / 48f
        val innerX = (centerX + 16f * scale).roundToInt()
        val innerY = centerY.roundToInt()
        assertTrue("$context: pressing must change a pixel inside the circle",
            changed(before[innerX, innerY], held[innerX, innerY]))

        // Ignore two dp at the circular edge to avoid antialiasing and subpixel capture differences.
        val radius = 22f * scale
        for (y in 0 until held.height) {
            for (x in 0 until held.width) {
                val dx = x - centerX
                val dy = y - centerY
                if (dx * dx + dy * dy >= radius * radius && changed(before[x, y], held[x, y])) {
                    throw AssertionError("$context: press changed exterior pixel ($x, $y)")
                }
            }
        }
    }

    private fun assertRestored(before: PixelMap, recovered: PixelMap, context: String) {
        assertSize(before, recovered, context)
        for (y in 0 until before.height) {
            for (x in 0 until before.width) {
                if (changed(before[x, y], recovered[x, y])) {
                    throw AssertionError("$context: feedback did not clear at ($x, $y)")
                }
            }
        }
    }

    private fun assertSize(before: PixelMap, after: PixelMap, context: String) {
        assertEquals("$context: width", before.width, after.width)
        assertEquals("$context: height", before.height, after.height)
    }

    private fun changed(first: Color, second: Color): Boolean =
        maxOf(abs(first.red - second.red), abs(first.green - second.green), abs(first.blue - second.blue)) > 3f / 255f
}
