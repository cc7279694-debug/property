package com.cooper.lithic.feature.home

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.CategoryDailyCost
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.ui.HomeAndroidAcceptanceFixture
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeOverviewUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unavailable_overview_never_invents_a_zero_cost() {
        compose.setContent { LithicTheme {
            HomeShell(0L, {}, {}, 0L, {}, {})
        } }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("—")
        compose.onNodeWithTag("home-cutoff-date").assertDoesNotExist()
        compose.onNodeWithText("¥0.00/天").assertDoesNotExist()
    }

    @Test
    fun empty_ledger_has_no_cost_even_when_the_aggregate_number_is_zero() {
        show(result())
        compose.onNodeWithTag("home-total-rate").assertTextEquals("—")
        compose.onNodeWithTag("home-open-add").assertIsDisplayed()
        compose.onNodeWithText("¥0.00/天").assertDoesNotExist()
    }

    @Test
    fun recorded_zero_cost_remains_a_real_value_and_an_active_category() {
        show(result(projectCount = 1L, activeProjectCount = 1L,
            projectCategories = listOf(CategoryDailyCost("zero", "零元项目", BigDecimal.ZERO, 1L))))
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥0.00/天")
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        compose.onNodeWithTag("home-project-category-zero").performScrollTo().assertTextEquals("¥0.00/天")
        compose.onNodeWithTag("home-no-active-records").assertDoesNotExist()
    }

    @Test
    fun signed_category_values_and_type_subtotals_use_the_supplied_unrounded_results() {
        show(result(total = "0.245", project = "0.120", subscription = "0.125",
            projectCount = 2L, subscriptionCount = 1L, activeProjectCount = 2L, activeSubscriptionCount = 1L,
            projectCategories = listOf(
                CategoryDailyCost("p", "其他", BigDecimal("0.125"), 1L),
                CategoryDailyCost("negative", "净收回", BigDecimal("-0.005"), 1L)),
            subscriptionCategories = listOf(CategoryDailyCost("s", "其他", BigDecimal("0.125"), 1L))))
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥0.25/天")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("2")
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        compose.onNodeWithTag("home-project-rate").performScrollTo().assertTextEquals("¥0.12/天")
        compose.onNodeWithTag("home-project-category-p").performScrollTo().assertTextEquals("¥0.13/天")
        compose.onNodeWithTag("home-project-category-negative").performScrollTo().assertTextEquals("−<¥0.01/天")
        compose.onNodeWithTag("home-subscription-rate").performScrollTo().assertTextEquals("¥0.13/天")
        compose.onNodeWithTag("home-subscription-category-s").performScrollTo().assertTextEquals("¥0.13/天")
        verifyRoundingNote(
            compose,
            reopen = {
                pressBack()
                compose.onNodeWithTag("home-cost-composition").assertDoesNotExist()
                compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
            },
            capture = ::captureRoundingDiagnostic,
        )
    }

    @Test
    fun inactive_records_are_explained_without_being_reported_as_an_empty_ledger() {
        show(result(projectCount = 1L, subscriptionCount = 1L,
            projectCategories = listOf(CategoryDailyCost("ended", "结束类别", BigDecimal.ZERO, 0L)),
            subscriptionCategories = listOf(CategoryDailyCost("expired", "过期类别", BigDecimal.ZERO, 0L))))
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥0.00/天")
        compose.onNodeWithTag("home-no-active-records").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("暂无记录").assertDoesNotExist()
        compose.onNodeWithTag("home-project-category-ended").assertDoesNotExist()
        compose.onNodeWithTag("home-subscription-category-expired").assertDoesNotExist()
    }

    @Test
    fun all_existing_actions_remain_reachable_in_a_narrow_viewport_with_large_text() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("home-test-viewport")) {
                val canvasDensity = constraints.maxWidth / 360f
                val canvasHeight = minOf(640f, constraints.maxHeight / canvasDensity).dp
                CompositionLocalProvider(LocalDensity provides Density(canvasDensity, fontScale = 2f)) {
                    LithicTheme(darkTheme = true) {
                        Box(Modifier.requiredSize(width = 360.dp, height = canvasHeight).testTag("home-test-canvas")) {
                            HomeShell(0L, { clicks += "projects" }, { clicks += "new-project" },
                                0L, { clicks += "subscriptions" }, { clicks += "new-subscription" },
                                overview = result(projectCount = 1L, activeProjectCount = 1L)
                                    .copy(totalInvestmentCents = Long.MAX_VALUE))
                        }
                    }
                }
            }
        }
        assertHomeFitsVisibleWidth()
        compose.onNodeWithTag("home-total-investment").performScrollTo()
            .assertTextEquals("¥92233720368547758.07")
            .assertContentDescriptionEquals("总投入，92233720368547758.07元")
        compose.onNodeWithTag("amount-open-full", useUnmergedTree = true).performScrollTo().performClick()
        compose.onNodeWithTag("amount-full-value").assertTextEquals("¥92233720368547758.07")
        compose.onNodeWithTag("amount-close-full").performClick()
        compose.onNodeWithTag("open-project-list").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-project").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-subscription-tab").performScrollTo().performClick()
        compose.onNodeWithTag("open-subscription-list").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-create-subscription").assertIsDisplayed().performClick()
        assertEquals(listOf("projects", "new-project", "subscriptions", "new-subscription"), clicks)
    }

    private fun captureRoundingDiagnostic(name: String) {
        val attempt = InstrumentationRegistry.getArguments().getString("homeAcceptanceRunId") ?: return
        require(UUID.fromString(attempt).toString() == attempt)
        HomeAndroidAcceptanceFixture.requireDisposableEmulator()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.filesDir, "home-android-acceptance/$attempt/rounding-note")
        check(directory.isDirectory || directory.mkdirs())
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            val file = File(directory, "$name.png")
            check(file.createNewFile())
            file.outputStream().use { check(screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            screenshot.recycle()
        }
    }

    private fun assertHomeFitsVisibleWidth() {
        val viewport = compose.onNodeWithTag("home-test-viewport").fetchSemanticsNode()
        val home = compose.onNodeWithTag("home-test-canvas").fetchSemanticsNode()
        val viewportLeft = viewport.positionInRoot.x
        val viewportRight = viewportLeft + viewport.size.width
        val homeLeft = home.positionInRoot.x
        val homeRight = homeLeft + home.size.width
        // Raw layout pixels avoid clipped bounds and differences between the two local densities.
        assertTrue("Home exceeds visible width: home=[$homeLeft, $homeRight], viewport=[$viewportLeft, $viewportRight]",
            homeLeft >= viewportLeft - 0.5f && homeRight <= viewportRight + 0.5f)
    }

    private fun show(overview: OverviewResult) {
        compose.setContent { LithicTheme {
            // Counts from the supplied overview must win over stale compatibility arguments.
            HomeShell(99L, {}, {}, 99L, {}, {}, overview = overview)
        } }
    }

    private fun result(
        total: String = "0", project: String = "0", subscription: String = "0",
        projectCount: Long = 0L, subscriptionCount: Long = 0L,
        activeProjectCount: Long = 0L, activeSubscriptionCount: Long = 0L,
        projectCategories: List<CategoryDailyCost> = emptyList(),
        subscriptionCategories: List<CategoryDailyCost> = emptyList(),
    ) = OverviewResult(LocalDate.of(2026, 1, 20), BigDecimal(total), BigDecimal(project), BigDecimal(subscription),
        projectCount, subscriptionCount, activeProjectCount, activeSubscriptionCount,
        projectCategories, subscriptionCategories)
}

internal fun verifyRoundingNote(
    compose: ComposeContentTestRule,
    reopen: (() -> Unit)? = null,
    capture: (String) -> Unit,
) {
    val note = compose.onNodeWithTag("home-rounding-note")
    val panel = compose.onNodeWithTag("home-cost-composition", useUnmergedTree = true)
    val expand = compose.onAllNodes(
        SemanticsMatcher.keyIsDefined(SemanticsActions.Expand), useUnmergedTree = true,
    )
    try {
        if (reopen != null) {
            reopen()
            recordRoundingViewport(compose, "before-semantic-scroll", includeTree = true)
            note.performScrollTo()
            recordRoundingViewport(compose, "after-semantic-scroll")
            capture("cost-composition-before-gesture")
            reopen()
        }
        recordRoundingViewport(compose, "before-user-gesture", includeTree = reopen == null)
        val expandable = expand.fetchSemanticsNodes()
        if (expandable.isNotEmpty()) {
            check(expandable.size == 1) { "Expected one expandable sheet handle" }
            val handle = expand[0]
            val screenTop = handle.fetchSemanticsNode().positionOnScreen.y
            handle.performTouchInput {
                val distance = minOf(400.dp.toPx(), screenTop + center.y - 32.dp.toPx())
                check(distance > 0) { "Sheet handle is outside the visible screen" }
                swipe(center, center - Offset(0f, distance), durationMillis = 500)
            }
            compose.waitForIdle()
            recordRoundingViewport(compose, "after-sheet-drag")
        }
        for (attempt in 1..4) {
            val window = activeWindowBounds()
            check(!window.isEmpty) { "Active sheet window is unavailable" }
            val node = panel.fetchSemanticsNode()
            val rootOrigin = node.positionOnScreen.y - node.positionInRoot.y
            val panelTop = rootOrigin + node.boundsInRoot.top
            panel.performTouchInput {
                val bottom = minOf(height.toFloat(), window.bottom - panelTop)
                val top = maxOf(0f, window.top - panelTop)
                check(bottom > top) { "No visible area for the sheet content gesture" }
                swipe(Offset(center.x, top + (bottom - top) * 0.85f),
                    Offset(center.x, top + (bottom - top) * 0.15f), durationMillis = 400)
            }
            compose.waitForIdle()
            if (note.isDisplayed() && roundingNoteFitsWindow(compose)) break
        }
        recordRoundingViewport(compose, "after-content-swipe")
        note.assertIsDisplayed().assertTextEquals(
            "分项四舍五入，显示金额相加可能有分位差；汇总使用未舍入结果。",
        )
        assertTrue("Rounding note must fit in the visible window", roundingNoteFitsWindow(compose))
        capture("cost-composition-rounding-note")
    } catch (failure: Throwable) {
        if (failure !is Exception && failure !is AssertionError) throw failure
        runCatching { recordRoundingViewport(compose, "assertion-failed", includeTree = true) }
            .onFailure { failure.addSuppressed(it) }
        runCatching { capture("cost-composition-failure") }
            .onFailure { failure.addSuppressed(it) }
        throw failure
    }
}

private fun roundingNoteFitsWindow(compose: ComposeContentTestRule): Boolean {
    val note = compose.onNodeWithTag("home-rounding-note").fetchSemanticsNode()
    val visible = note.boundsInWindow
    val origin = note.positionOnScreen
    val window = activeWindowBounds()
    return note.size.width > 0 && note.size.height > 0 &&
        visible.width >= note.size.width - 1f && visible.height >= note.size.height - 1f &&
        !window.isEmpty && origin.x >= window.left - 1f && origin.y >= window.top - 1f &&
        origin.x + note.size.width <= window.right + 1f &&
        origin.y + note.size.height <= window.bottom + 1f
}

private fun recordRoundingViewport(compose: ComposeContentTestRule, phase: String, includeTree: Boolean = false) {
    val note = compose.onNodeWithTag("home-rounding-note", useUnmergedTree = true).fetchSemanticsNode()
    val panel = compose.onNodeWithTag("home-cost-composition", useUnmergedTree = true).fetchSemanticsNode()
    val ancestors = generateSequence(note.parent) { it.parent }.toList()
    val scroll = ancestors.firstOrNull { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange) != null }
    val range = scroll?.config?.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
    val (position, maximum) = compose.runOnIdle { range?.value?.invoke() to range?.maxValue?.invoke() }
    val expand = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Expand), useUnmergedTree = true)
        .fetchSemanticsNodes()
    val collapse = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.Collapse), useUnmergedTree = true)
        .fetchSemanticsNodes()
    val window = activeWindowBounds()
    val display = InstrumentationRegistry.getInstrumentation().targetContext.resources.displayMetrics
    val visible = compose.onNodeWithTag("home-rounding-note").isDisplayed()
    Log.i("LithicRoundingNote", "$phase note[id=${note.id}, size=${note.size}, root=${note.boundsInRoot}, " +
        "window=${note.boundsInWindow}, screenOrigin=${note.positionOnScreen}, displayed=$visible] " +
        "panel[id=${panel.id}, size=${panel.size}, root=${panel.boundsInRoot}, window=${panel.boundsInWindow}] " +
        "scroll[id=${scroll?.id}, root=${scroll?.boundsInRoot}, window=${scroll?.boundsInWindow}, " +
        "value=$position, max=$maximum] " +
        "expand=${expand.map { it.boundsInWindow }} collapse=${collapse.map { it.boundsInWindow }} " +
        "display=${display.widthPixels}x${display.heightPixels} activeWindow=$window ancestors=${ancestors.map { it.id }}")
    if (includeTree) {
        val roots = compose.onAllNodes(isRoot(), useUnmergedTree = true)
        repeat(roots.fetchSemanticsNodes().size) { index ->
            roots[index].printToString(maxDepth = Int.MAX_VALUE).lineSequence().forEach { line ->
                Log.i("LithicRoundingNote", line)
            }
        }
    }
}

private fun activeWindowBounds(): Rect {
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    return Rect().also { automation.rootInActiveWindow?.getBoundsInScreen(it) }
}
