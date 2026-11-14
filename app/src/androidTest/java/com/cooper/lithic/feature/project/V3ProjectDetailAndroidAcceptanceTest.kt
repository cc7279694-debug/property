package com.cooper.lithic.feature.project

import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.feature.appearance.AppearanceProvider
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.ui.HomeAndroidAcceptanceFixture
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

/** MainActivity, production navigation and Owner over the existing guarded disposable fixture. */
@RunWith(AndroidJUnit4::class)
class V3ProjectDetailAndroidAcceptanceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var attemptId: String
    private var expectedFontScale: Float = 0f
    private var fixture: HomeAndroidAcceptanceFixture.Session? = null
    private var priorAppearance: LithicAppearance? = null

    @get:Rule val rules: TestRule = RuleChain.outerRule(TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                HomeAndroidAcceptanceFixture.requireDisposableEmulator()
                val arguments = InstrumentationRegistry.getArguments()
                attemptId = requireNotNull(arguments.getString("projectDetailAcceptanceRunId"))
                require(UUID.fromString(attemptId).toString() == attemptId)
                expectedFontScale = requireNotNull(arguments.getString("projectDetailAcceptanceExpectedFontScale")).toFloat()
                require(expectedFontScale == 1f || expectedFontScale == 2f)
                report("start")
                base.evaluate()
            }
        }
    }).around(compose)

    @Before fun prepareFixture() {
        compose.activityRule.scenario.onActivity { activity ->
            val configuration = activity.resources.configuration
            val persisted = Settings.System.getString(activity.contentResolver, Settings.System.FONT_SCALE)
            report("configuration") {
                putString("projectDetailAcceptanceSystemFontScale", persisted ?: "missing")
                putInt("projectDetailAcceptanceScreenWidthDp", configuration.screenWidthDp)
                putFloat("projectDetailAcceptanceFontScale", configuration.fontScale)
            }
            assertEquals("Actual Android screen width in dp", 360, configuration.screenWidthDp)
            assertEquals("Actual Android font scale", expectedFontScale, configuration.fontScale, .01f)
            assertEquals("System font_scale must be set for this run", expectedFontScale,
                persisted?.toFloatOrNull() ?: Float.NaN, .01f)
        }
        fixture = runBlocking { HomeAndroidAcceptanceFixture.prepare(application(), attemptId) }
        val appearance = AppearanceProvider.get(application())
        compose.waitUntil(12_000) { appearance.state.value.readable && !appearance.state.value.loading }
        priorAppearance = appearance.state.value.selected
        waitFor("home-empty-guidance")
    }

    @After fun releaseFixture() {
        val owned = fixture ?: return
        try {
            priorAppearance?.let(::selectTheme)
        } finally {
            runBlocking { owned.close() }
            report("fixture-cleanup") { putString("projectDetailAcceptanceFixtureCleanup", "PASS") }
        }
    }

    @Test fun production_composable_gallery_uses_system_configuration() {
        val seeded = runBlocking { checkNotNull(fixture).seed() }
        val camera = seeded.projectIds.first()
        val portrait = seeded.projectIds.last()
        val galleryFailures = mutableListOf<Throwable>()
        selectTheme(LithicAppearance.PURE_WHITE)
        if (expectedFontScale == 1f) {
            val gallery = listOf(
                LithicAppearance.PURE_WHITE to "project-detail-pure-white",
                LithicAppearance.STONE_MIST to "project-detail-stone-mist",
                LithicAppearance.CHARCOAL to "project-detail-charcoal",
            )
            gallery.forEach { (theme, screenshot) ->
                selectTheme(theme)
                openDetail(camera)
                assertCover()
                compose.onNodeWithTag("project-detail-content").performScrollToIndex(0)
                capture(screenshot)
                keepAssertionFailure(galleryFailures) { assertTitleFits(CAMERA_NAME, theme) }
                keepAssertionFailure(galleryFailures, ::assertFinancialFacts)
                if (theme == LithicAppearance.PURE_WHITE) checkFullCoverAndClose()
                backToHome()
            }
            selectTheme(LithicAppearance.PURE_WHITE)
            openDetail(portrait)
            assertCover()
            capture("project-detail-portrait")
            checkFullCoverAndClose()
        } else {
            openDetail(camera)
            assertCover()
            compose.onNodeWithTag("project-detail-content").performScrollToIndex(0)
            capture("project-detail-font-200")
            keepAssertionFailure(galleryFailures) { assertTitleFits(CAMERA_NAME, LithicAppearance.PURE_WHITE) }
            keepAssertionFailure(galleryFailures, ::assertFinancialFacts)
            scrollTo("project-detail-net")
            capture("project-detail-font-200-money")
        }
        backToHome()
        if (galleryFailures.isNotEmpty()) {
            throw AssertionError(galleryFailures.joinToString("\n") { it.message.orEmpty() }).apply {
                galleryFailures.forEach(::addSuppressed)
            }
        }
    }

    @Test fun long_project_title_wraps_without_clipping_at_system_font_scale() {
        val seeded = runBlocking { checkNotNull(fixture).seed(cameraName = LONG_PROJECT_NAME) }
        selectTheme(LithicAppearance.PURE_WHITE)
        openDetail(seeded.projectIds.first())
        assertCover()
        val content = compose.onNodeWithTag("project-detail-content")
        content.performScrollToIndex(0)
        scrollTo("project-detail-name").assertIsDisplayed()
        capture(if (expectedFontScale == 1f) "project-detail-long-title-100" else "project-detail-long-title-200")
        val title = assertTitleFits(LONG_PROJECT_NAME, LithicAppearance.PURE_WHITE, minimumLines = 2)
        val viewport = content.fetchSemanticsNode().boundsInRoot
        val actionBefore = compose.onNodeWithTag("project-record-money").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val category = assertMetadataFits("project-detail-category", "其他", viewport)
        val status = assertMetadataFits("project-detail-status", "持有中", viewport)
        val held = assertMetadataFits("project-detail-held-days", "121天", viewport)
        val metadata = listOf(category, status, held)
        assertTrue("The title overlaps project metadata", metadata.all { title.bottom <= it.top + PIXEL_TOLERANCE })
        assertTrue("Category overlaps status", !category.overlaps(status))
        assertTrue("Held duration overlaps category or status", !held.overlaps(category) && !held.overlaps(status))
        assertTrue("Record action overlaps the content viewport", viewport.bottom <= actionBefore.top + PIXEL_TOLERANCE)

        // Physically scroll the production list, then verify the cost card and end of the list are reachable.
        content.performTouchInput {
            swipe(start = center, end = center.copy(y = center.y - height * .21f), durationMillis = 350)
        }
        val shiftedTitle = compose.onAllNodes(hasTestTag("project-detail-name"), useUnmergedTree = true)
            .fetchSemanticsNodes().singleOrNull()
        assertTrue("Long-title page did not respond to a vertical swipe",
            shiftedTitle == null || shiftedTitle.positionInRoot.y < title.top - PIXEL_TOLERANCE)

        val costNode = scrollTo("project-core-metrics").assertIsDisplayed().fetchSemanticsNode()
        compose.onNodeWithTag("project-detail-daily").assertIsDisplayed().assertTextEquals("¥9.92/天")
        val costBounds = costNode.actualBoundsInRoot()
        val headerBounds = listOf("project-detail-name", "project-detail-category", "project-detail-status", "project-detail-held-days")
            .flatMap { compose.onAllNodes(hasTestTag(it), useUnmergedTree = true).fetchSemanticsNodes() }
        assertTrue("Cost card overlaps visible identity information", headerBounds.none { it.boundsInRoot.overlaps(costBounds) })
        assertTrue("The cost card has no usable bounds", costBounds.height > 0 && costBounds.width > 0)
        assertInside("Unclipped cost card", costBounds, costNode.boundsInRoot)
        assertInside("Cost card fits the viewport", costBounds, viewport)
        assertFinancialFacts()
        scrollTo("project-cost-history").assertIsDisplayed()
        val actionAfter = compose.onNodeWithTag("project-record-money").assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        assertEquals("Record action moved with scrolling", actionBefore, actionAfter)
        report("long-title-neighbors") {
            putString("projectDetailAcceptanceLongTitleCategoryBoundsPx", category.toString())
            putString("projectDetailAcceptanceLongTitleStatusBoundsPx", status.toString())
            putString("projectDetailAcceptanceLongTitleHeldBoundsPx", held.toString())
            putString("projectDetailAcceptanceLongTitleCostBoundsAfterScrollPx", costBounds.toString())
            putString("projectDetailAcceptanceLongTitleActionBoundsPx", actionAfter.toString())
        }
        backToHome()
    }

    private fun assertMetadataFits(tag: String, text: String, viewport: Rect): Rect {
        val interaction = compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().assertTextEquals(text)
        val layouts = mutableListOf<TextLayoutResult>()
        interaction.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val node = interaction.fetchSemanticsNode()
        val layout = layouts.single()
        assertTextInsideNode(tag, text, layout, node)
        val actualBounds = node.actualBoundsInRoot()
        assertInside(tag, actualBounds, node.boundsInRoot)
        assertInside(tag, actualBounds, viewport)
        return actualBounds
    }

    private fun openDetail(id: String) {
        waitFor("home-project-tab")
        compose.onNodeWithTag("home-project-tab").performScrollTo().performClick()
        val tag = "home-project-preview-$id"
        waitFor(tag)
        compose.onNodeWithTag(tag).performScrollTo().performClick()
        waitFor("project-detail-name")
        waitFor("record-cover-image")
    }

    private fun assertCover() {
        val cover = scrollTo("record-cover-image").assertIsDisplayed().assertHasClickAction().fetchSemanticsNode()
        assertTrue("The registered cover uses a 4:3 frame: ${cover.size}",
            cover.size.width > 0 && abs(cover.size.width * .75f - cover.size.height) < 2f)
        compose.onNodeWithTag("record-cover-unavailable").assertDoesNotExist()
        compose.onNodeWithTag("record-cover-loading").assertDoesNotExist()
    }

    private fun checkFullCoverAndClose() {
        scrollTo("record-cover-image").performClick()
        compose.onNodeWithTag("project-cover-full-image").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("project-cover-full-view").assertDoesNotExist()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
    }

    private fun assertTitleFits(name: String, theme: LithicAppearance, minimumLines: Int = 1): Rect {
        val title = scrollTo("project-detail-name").assertIsDisplayed().assertTextEquals(name)
        val layouts = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val input = layout.layoutInput
        val titleNode = title.fetchSemanticsNode()
        val parentBounds = compose.runOnIdle {
            titleNode.layoutInfo.parentInfo?.coordinates?.takeIf { it.isAttached }?.boundsInRoot()
        }
        val viewport = compose.onNodeWithTag("project-detail-content").fetchSemanticsNode()
        val activityFontScale = compose.activity.resources.configuration.fontScale
        val realScale = Settings.System.getString(compose.activity.contentResolver, Settings.System.FONT_SCALE)
        val lines = 0 until layout.lineCount
        val ellipsized = lines.any(layout::isLineEllipsized)
        val lastVisibleEnd = if (layout.lineCount > 0) layout.getLineEnd(layout.lineCount - 1, visibleEnd = true) else 0
        val bounds = titleNode.boundsInRoot
        val actualBounds = titleNode.actualBoundsInRoot()
        val clipsTitleNode = bounds.height + 1f < titleNode.size.height || bounds.width + 1f < titleNode.size.width
        report("title-layout") {
            putString("projectDetailAcceptanceTitleTheme", theme.name)
            putString("projectDetailAcceptanceTitleText", input.text.text)
            putString("projectDetailAcceptanceTitleSizePx", layout.size.toString())
            putString("projectDetailAcceptanceTitleConstraintsPx", input.constraints.toString())
            putFloat("projectDetailAcceptanceTitleParagraphWidthPx", layout.multiParagraph.width)
            putFloat("projectDetailAcceptanceTitleParagraphHeightPx", layout.multiParagraph.height)
            putBoolean("projectDetailAcceptanceTitleOverflowWidth", layout.didOverflowWidth)
            putBoolean("projectDetailAcceptanceTitleOverflowHeight", layout.didOverflowHeight)
            putBoolean("projectDetailAcceptanceTitleVisualOverflow", layout.hasVisualOverflow)
            putBoolean("projectDetailAcceptanceTitleExceededMaxLines", layout.multiParagraph.didExceedMaxLines)
            putInt("projectDetailAcceptanceTitleLineCount", layout.lineCount)
            putString("projectDetailAcceptanceTitleLineWidthsPx", lines.joinToString(",") { layout.multiParagraph.getLineWidth(it).toString() })
            putString("projectDetailAcceptanceTitleLineBottomsPx", lines.joinToString(",") { layout.getLineBottom(it).toString() })
            putString("projectDetailAcceptanceTitleGlyphBoundsPx", input.text.indices.joinToString(";") {
                layout.getBoundingBox(it).toString()
            })
            putInt("projectDetailAcceptanceTitleVisibleEnd", lastVisibleEnd)
            putBoolean("projectDetailAcceptanceTitleEllipsized", ellipsized)
            putString("projectDetailAcceptanceTitleOverflowPolicy", input.overflow.toString())
            putInt("projectDetailAcceptanceTitleMaxLines", input.maxLines)
            putString("projectDetailAcceptanceTitleBoundsInRootPx", bounds.toString())
            putString("projectDetailAcceptanceTitleUnclippedBoundsInRootPx", actualBounds.toString())
            putString("projectDetailAcceptanceTitleNodeSizePx", titleNode.size.toString())
            putString("projectDetailAcceptanceTitleParentBoundsInRootPx", parentBounds.toString())
            putString("projectDetailAcceptanceTitleViewportBoundsInRootPx", viewport.boundsInRoot.toString())
            putBoolean("projectDetailAcceptanceTitleNodeClips", clipsTitleNode)
            putFloat("projectDetailAcceptanceTitleDensityFontScale", input.density.fontScale)
            putFloat("projectDetailAcceptanceTitleFontScale", activityFontScale)
            putString("projectDetailAcceptanceTitleSystemFontScale", realScale)
            putString("projectDetailAcceptanceTitleFontSize", input.style.fontSize.toString())
            putString("projectDetailAcceptanceTitleLineHeight", input.style.lineHeight.toString())
            with(input.density) {
                putFloat("projectDetailAcceptanceTitleFontSizePx", input.style.fontSize.toPx())
                putFloat("projectDetailAcceptanceTitleLineHeightPx", input.style.lineHeight.toPx())
            }
        }
        val location = "${theme.name} at ${expectedFontScale}x ($name)"
        assertTextInsideNode(location, name, layout, titleNode)
        assertEquals("Text layout font scale at $location", expectedFontScale, input.density.fontScale, .01f)
        assertTrue("Unexplained Compose overflow at $location",
            !layout.hasVisualOverflow ||
                (layout.didOverflowWidth && !layout.didOverflowHeight &&
                    layout.multiParagraph.width == input.constraints.maxWidth.toFloat() &&
                    layout.multiParagraph.width > layout.size.width))
        assertTrue("Title needs at least $minimumLines lines at $location", layout.lineCount >= minimumLines)
        assertInside("Unclipped title at $location", actualBounds, bounds)
        assertInside("Parent at $location", actualBounds, checkNotNull(parentBounds))
        assertInside("Scroll viewport at $location", actualBounds, viewport.boundsInRoot)
        return actualBounds
    }

    // Layout semantics can rebuild a paragraph at the full constraint width. Only the real
    // node, line and character extents are compared against the available space.
    private fun assertTextInsideNode(label: String, expected: String, layout: TextLayoutResult, node: SemanticsNode) {
        val text = layout.layoutInput.text.text
        assertEquals(label, expected, text)
        assertTrue("$label has no usable text bounds", node.size.width > 0 && node.size.height > 0 &&
            layout.size.width > 0 && layout.size.height > 0 && layout.lineCount > 0)
        val space = Rect(0f, 0f, minOf(node.size.width, layout.size.width).toFloat(),
            minOf(node.size.height, layout.size.height).toFloat())
        assertFalse("$label overflows vertically", layout.didOverflowHeight)
        assertFalse("$label exceeds the maximum line count", layout.multiParagraph.didExceedMaxLines)
        assertFalse("$label uses an ellipsis policy", layout.layoutInput.overflow == TextOverflow.Ellipsis)
        var nextOffset = 0
        for (line in 0 until layout.lineCount) {
            assertEquals("$label line $line skips text", nextOffset, layout.getLineStart(line))
            nextOffset = layout.getLineEnd(line)
            assertFalse("$label line $line has an ellipsis", layout.isLineEllipsized(line))
            assertTrue("$label line $line is too wide", layout.multiParagraph.getLineWidth(line) <= space.width + PIXEL_TOLERANCE)
            assertInside("$label line $line",
                Rect(layout.getLineLeft(line), layout.getLineTop(line), layout.getLineRight(line), layout.getLineBottom(line)),
                space)
        }
        assertEquals("$label is missing input characters", expected.length, nextOffset)
        assertEquals("$label is missing visible characters", expected.length,
            layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
        expected.indices.forEach { offset ->
            val box = layout.getBoundingBox(offset)
            assertTrue("$label character $offset has no bounds", box.width > 0f && box.height > 0f)
            assertInside("$label character $offset", box, space)
        }
    }

    private fun SemanticsNode.actualBoundsInRoot(): Rect = Rect(
        positionInRoot.x, positionInRoot.y, positionInRoot.x + size.width, positionInRoot.y + size.height,
    )

    private fun assertInside(label: String, inner: Rect, outer: Rect) {
        assertTrue("$label: $inner is outside $outer",
            inner.left >= outer.left - PIXEL_TOLERANCE && inner.top >= outer.top - PIXEL_TOLERANCE &&
                inner.right <= outer.right + PIXEL_TOLERANCE && inner.bottom <= outer.bottom + PIXEL_TOLERANCE)
    }

    private fun keepAssertionFailure(failures: MutableList<Throwable>, check: () -> Unit) {
        try {
            check()
        } catch (failure: AssertionError) {
            failures += failure
        }
    }

    private fun assertFinancialFacts() {
        scrollTo("project-detail-daily").assertTextEquals("¥9.92/天")
        scrollTo("project-detail-outlay").assertTextEquals("¥50000.00")
        scrollTo("project-detail-recovery").assertTextEquals("¥48799.68")
        scrollTo("project-detail-net").assertTextEquals("¥1200.32")
        val viewport = compose.onNodeWithTag("project-detail-content").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("project-record-money").assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        assertTrue("Record action overlaps scrolling content", viewport.bottom <= action.top + 1f)
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag("project-detail-content")
        .performScrollToNode(hasTestTag(tag)).let { compose.onNodeWithTag(tag) }

    private fun backToHome() {
        pressBack()
        waitFor("home-screen")
        compose.onNodeWithTag("project-detail-screen").assertDoesNotExist()
    }

    private fun waitFor(tag: String) {
        compose.waitUntil(12_000) {
            compose.onAllNodes(hasTestTag(tag), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun selectTheme(theme: LithicAppearance) {
        val controller = AppearanceProvider.get(application())
        if (controller.state.value.selected != theme) {
            compose.runOnIdle { controller.select(theme) }
            compose.waitUntil(12_000) {
                controller.state.value.let { it.selected == theme && it.readable && !it.saving && it.error == null }
            }
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val relativeDirectory = "project-detail-acceptance/$attemptId"
        val directory = File(instrumentation.targetContext.filesDir, relativeDirectory)
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, "$name.png")
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            check(file.createNewFile()) { "Refusing to overwrite the screenshot" }
            FileOutputStream(file).use { stream ->
                check(screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream))
                stream.fd.sync()
            }
            check(file.length() > 0)
            report("screenshot") {
                putString("projectDetailAcceptancePng", "files/$relativeDirectory/${file.name}")
                putInt("projectDetailAcceptanceWidthPx", screenshot.width)
                putInt("projectDetailAcceptanceHeightPx", screenshot.height)
                putInt("projectDetailAcceptanceScreenWidthDp", compose.activity.resources.configuration.screenWidthDp)
                putFloat("projectDetailAcceptanceFontScale", compose.activity.resources.configuration.fontScale)
            }
        } finally {
            screenshot.recycle()
        }
    }

    private fun report(phase: String, details: Bundle.() -> Unit = {}) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("projectDetailAcceptanceAttemptId", attemptId)
            putString("projectDetailAcceptancePhase", phase)
            details()
        })
    }

    private fun application() = compose.activity.application as LithicApplication

    private companion object {
        const val CAMERA_NAME = "合成横向相机"
        const val LONG_PROJECT_NAME = "合成旅行摄影设备与备用镜头套装"
        const val PIXEL_TOLERANCE = 1f
    }
}
