package com.cooper.lithic.feature.project

import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.text.TextLayoutResult
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
                assertFinancialFacts()
                compose.onNodeWithTag("project-detail-content").performScrollToIndex(0)
                capture(screenshot)
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
            assertFinancialFacts()
            scrollTo("project-detail-daily")
            capture("project-detail-font-200-money")
        }
        backToHome()
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

    private fun assertFinancialFacts() {
        val name = "合成横向相机"
        val title = scrollTo("project-detail-name").assertTextEquals(name)
        val layouts = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(name, layout.layoutInput.text.text)
        assertFalse("The project title overflows at ${expectedFontScale}x", layout.hasVisualOverflow)
        val titleNode = title.fetchSemanticsNode()
        assertTrue("The project title is clipped", titleNode.boundsInRoot.height >= titleNode.size.height - 1f)
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
}
