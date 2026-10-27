package com.cooper.lithic.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.cooper.lithic.core.design.LithicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production Navigation3 presentation only, with synthetic entries and no storage owner.
 * Ordinary forward/pop and background checks do not stand in for real predictive gestures. */
class C5NavigationPresentationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun light_scene_fills_its_bounds_with_an_opaque_theme_background() = checkBackground(false)

    @Test fun dark_scene_fills_its_bounds_with_an_opaque_theme_background() = checkBackground(true)

    @Test fun deep_forward_and_pop_keep_one_readable_page_and_the_original_saveable_state() {
        val stack = mutableStateListOf<NavKey>(Home, Review)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LithicTheme {
                NavDisplay(backStack = stack, onBack = { stack.removeLastOrNull() },
                    transitionSpec = { c5PageTransition() },
                    popTransitionSpec = { c5PageTransition() },
                    predictivePopTransitionSpec = { _ -> c5PageTransition() },
                    entryProvider = c5SceneEntryProvider(entryProvider<NavKey> {
                        entry<Home> { Text("合成首页") }
                        entry<Review> {
                            var count by rememberSaveable { mutableIntStateOf(0) }
                            TextButton(onClick = { count++ }, modifier = Modifier.testTag("synthetic-count")) {
                                Text("合成回看$count")
                            }
                        }
                        entry<TotalCostTrend> { Text("合成完整趋势") }
                    }))
            }
        }
        advanceFrame()
        compose.onNodeWithTag("synthetic-count").performClick()
        advanceFrame()
        compose.onNodeWithText("合成回看1").assertIsDisplayed()
        compose.runOnUiThread { stack.add(TotalCostTrend) }
        advanceFrame()
        compose.onNodeWithText("合成完整趋势").assertIsDisplayed()
        compose.onNodeWithText("合成回看1").assertDoesNotExist()
        compose.runOnUiThread { stack.removeLastOrNull() }
        advanceFrame()
        compose.onNodeWithText("合成回看1").assertIsDisplayed()
        compose.onNodeWithText("合成完整趋势").assertDoesNotExist()
    }

    private fun checkBackground(dark: Boolean) {
        var expected = Color.Unspecified
        compose.setContent {
            LithicTheme(darkTheme = dark) {
                expected = MaterialTheme.colorScheme.background.copy(alpha = 1f)
                Box(Modifier.size(240.dp).background(Color.Magenta)) {
                    c5SceneEntryProvider { route: NavKey -> NavEntry(route) { Text("合成页面") } }(Home).Content()
                }
            }
        }
        val scene = compose.onNodeWithTag("navigation-scene")
        scene.assertWidthIsEqualTo(240.dp).assertHeightIsEqualTo(240.dp)
        val pixels = scene.captureToImage().toPixelMap()
        assertEquals(expected.toArgb(), pixels[pixels.width - 1, pixels.height - 1].toArgb())
        assertEquals(1f, pixels[pixels.width - 1, pixels.height - 1].alpha, 0f)
    }

    private fun advanceFrame() {
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
    }
}
