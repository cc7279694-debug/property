package com.cooper.lithic.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LithicApplication
import com.cooper.lithic.app.MainActivity
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.feature.appearance.AppearanceProvider
import com.cooper.lithic.feature.appearance.LithicAppearance
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
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

/** Uses the production activity, owner, navigation, appearance and cover reader on a disposable AVD. */
@RunWith(AndroidJUnit4::class)
class HomeAndroidAcceptanceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var attemptId: String
    private lateinit var runId: String
    private lateinit var method: String
    private var fixture: HomeAndroidAcceptanceFixture.Session? = null

    @get:Rule val rules: TestRule = RuleChain.outerRule(TestRule { base, description ->
        object : Statement() {
            override fun evaluate() {
                HomeAndroidAcceptanceFixture.requireDisposableEmulator()
                attemptId = requireNotNull(InstrumentationRegistry.getArguments().getString("homeAcceptanceRunId"))
                require(UUID.fromString(attemptId).toString() == attemptId) { "Invalid acceptance attempt ID" }
                runId = UUID.randomUUID().toString()
                method = requireNotNull(description.methodName)
                report("start")
                base.evaluate()
            }
        }
    }).around(compose)

    @Before fun prepareEmptyLedger() {
        fixture = runBlocking { HomeAndroidAcceptanceFixture.prepare(application(), runId) }
        waitForTag("home-empty-guidance")
    }

    @After fun releaseSyntheticLedger() {
        val ownedFixture = fixture ?: return
        runBlocking { ownedFixture.close() }
        report("fixture-cleaned")
    }

    @Test fun projects_subscriptions_and_theme_gallery() {
        requireConfiguration(1f)
        selectTheme(LithicAppearance.PURE_WHITE)
        waitForTag("home-empty-guidance")
        compose.onNodeWithTag("home-total-investment").assertTextEquals("—")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("0")
        capture("empty")

        val seeded = runBlocking { checkNotNull(fixture).seed() }
        waitForInvestment(seeded.investmentCents)
        waitFor(hasTestTag("home-total-rate") and hasText("¥16.56/天"))
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("2")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("2")
        for ((theme, filePart) in listOf(
            LithicAppearance.PURE_WHITE to "pure-white",
            LithicAppearance.STONE_MIST to "stone-mist",
            LithicAppearance.CHARCOAL to "charcoal",
        )) {
            selectTheme(theme)
            selectSection("project")
            seeded.projectIds.forEach { id -> waitForTag(project(id)) }
            val photos = seeded.projectIds.map { id -> coverIn(project(id), "record-cover-image") }
            photos.forEach { matcher -> waitFor(matcher); assertFourByThree(matcher) }
            val projectFrames = photos.map { compose.onNode(it, useUnmergedTree = true).fetchSemanticsNode() }
            val (firstFrame, secondFrame) = projectFrames.map { it.positionInRoot }
            assertTrue("Both project covers must occupy one row", abs(firstFrame.y - secondFrame.y) < 2f)
            assertTrue("Project covers must occupy separate columns", abs(firstFrame.x - secondFrame.x) > 10f)
            capture("projects-$filePart")

            selectSection("subscription")
            seeded.subscriptionIds.forEach { id -> waitForTag(subscription(id)) }
            val unavailable = hasTestTag("record-cover-unavailable")
            waitFor(unavailable)
            compose.onAllNodes(unavailable, useUnmergedTree = true).assertCountEquals(1)
            compose.onAllNodesWithTag("record-cover-image", useUnmergedTree = true).assertCountEquals(0)
            compose.onAllNodesWithTag("record-cover-loading", useUnmergedTree = true).assertCountEquals(0)
            assertFourByThree(unavailable)
            capture("subscriptions-$filePart")
        }
    }

    @Test fun home_routes_open_and_back() {
        requireConfiguration(1f)
        val seeded = runBlocking { checkNotNull(fixture).seed() }
        waitForInvestment(seeded.investmentCents)
        selectSection("project")
        openAndBack("open-project-list", "project-list-screen", scroll = true)
        openAndBack(project(seeded.projectIds.first()), "project-detail-screen", scroll = true)

        selectSection("subscription")
        openAndBack("open-subscription-list", "subscription-list-screen", scroll = true)
        selectSection("subscription")
        openAndBack(subscription(seeded.subscriptionIds.first()), "subscription-detail-screen", scroll = true)

        scrollHomeToTop()
        click("home-open-add-top")
        waitForTag("home-create-project")
        waitForTag("home-create-subscription")
        backToHome("home-create-project")
        click("home-open-add-top")
        click("home-create-project")
        waitForTag("project-editor-screen")
        backToHome("project-editor-screen")

        click("home-open-add")
        waitForTag("home-create-project")
        waitForTag("home-create-subscription")
        backToHome("home-create-subscription")
        click("home-open-add")
        click("home-create-subscription")
        waitForTag("subscription-editor-screen")
        backToHome("subscription-editor-screen")

        scrollHomeToTop()
        click("home-category-disclosure", scroll = true)
        waitForTag("home-cost-composition")
        backToHome("home-cost-composition")
        click("tab-review")
        waitForTag("review-screen")
        compose.onNodeWithTag("tab-review").assertIsSelected()
        click("tab-settings")
        waitForTag("settings-screen")
        compose.onNodeWithTag("tab-settings").assertIsSelected()
        click("tab-now")
        waitForTag("home-screen")
        compose.onNodeWithTag("tab-now").assertIsSelected()
    }

    @Test fun large_font_uses_android_configuration() {
        requireConfiguration(2f)
        val baselineMeasurement = InstrumentationRegistry.getArguments()
            .getString("homeAcceptanceBaselineMeasurement") == "true"
        val seeded = runBlocking { checkNotNull(fixture).seed() }
        waitForInvestment(seeded.investmentCents)
        selectTheme(LithicAppearance.PURE_WHITE)
        selectSection("project")
        scrollHomeToTop()
        assertAddBelowViewport()
        assertAmount(hasTestTag("home-total-investment"), AmountFormatter.formatCny(BigDecimal.valueOf(seeded.investmentCents, 2)))
        assertAmount(hasTestTag("home-total-rate"), "¥16.56/天")
        scrollHomeToTop()
        capture("projects-font-200-summary")
        val projects = seeded.projectIds.map(::project)
        checkLargeFontCard(projects[0], "合成横向相机", "持有中", "¥9.92/天", baselineMeasurement)
        checkLargeFontCard(projects[1], "合成纵向背包", "持有中", "¥3.32/天")
        captureLargeFontCards(projects, "projects-font-200-cards")

        selectSection("subscription")
        val subscriptions = seeded.subscriptionIds.map(::subscription)
        checkLargeFontCard(subscriptions[0], "合成阅读服务", "订阅中", "¥2.48/天")
        checkLargeFontCard(subscriptions[1], "合成云盘服务", "订阅中", "¥0.84/天")
        captureLargeFontCards(subscriptions, "subscriptions-font-200-cards")
    }

    private fun checkLargeFontCard(tag: String, name: String, status: String, amount: String, baselineMeasurement: Boolean = false) {
        waitForTag(tag)
        compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        val insideCard = hasAnyAncestor(hasTestTag(tag))
        val category = "其他"
        val categoryLabel = insideCard and hasText(category)
        val statusLabel = insideCard and hasText(status)
        reportLabelLayout(tag, name, "category", categoryLabel)
        reportLabelLayout(tag, name, "status", statusLabel)
        if (baselineMeasurement) capture("projects-font-200-baseline-cards")
        assertUnclippedText(insideCard and hasText(name), name)
        assertUnclippedText(statusLabel, status)
        assertUnclippedText(categoryLabel, category)
        assertAmount(insideCard and hasText(amount), amount)
        assertAddBelowViewport()
        val card = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        val add = compose.onNodeWithTag("home-open-add").fetchSemanticsNode().boundsInRoot
        assertTrue("The visible card cannot overlap the add control", card.bottom <= add.top + 1f)
    }

    private fun reportLabelLayout(cardTag: String, cardName: String, role: String, label: SemanticsMatcher) {
        val card = compose.onNodeWithTag(cardTag, useUnmergedTree = true).fetchSemanticsNode()
        val group = compose.onNode(coverIn(cardTag, "home-card-labels"), useUnmergedTree = true).fetchSemanticsNode()
        val node = compose.onNode(label, useUnmergedTree = true)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val result = layouts.single()
        val input = result.layoutInput
        report("label-layout") {
            putString("homeAcceptanceCard", cardName)
            putString("homeAcceptanceLabelRole", role)
            putString("homeAcceptanceLabelText", input.text.text)
            putString("homeAcceptanceCardSizePx", card.size.toString())
            putString("homeAcceptanceGroupSizePx", group.size.toString())
            putString("homeAcceptanceLabelSizePx", node.fetchSemanticsNode().size.toString())
            putString("homeAcceptanceTextSizePx", result.size.toString())
            putString("homeAcceptanceTextConstraintsPx", input.constraints.toString())
            putFloat("homeAcceptanceDensity", input.density.density)
            putFloat("homeAcceptanceFontScale", input.density.fontScale)
            putString("homeAcceptanceFontSize", input.style.fontSize.toString())
            putString("homeAcceptanceLineHeight", input.style.lineHeight.toString())
            with(input.density) {
                putFloat("homeAcceptanceFontSizePx", input.style.fontSize.toPx())
                putFloat("homeAcceptanceLineHeightPx", input.style.lineHeight.toPx())
            }
            putInt("homeAcceptanceLineCount", result.lineCount)
            putString("homeAcceptanceLineWidthsPx", (0 until result.lineCount).joinToString(",") {
                result.multiParagraph.getLineWidth(it).toString()
            })
            putFloat("homeAcceptanceParagraphWidthPx", result.multiParagraph.width)
            putFloat("homeAcceptanceIntrinsicWidthPx", result.multiParagraph.maxIntrinsicWidth)
            putBoolean("homeAcceptanceExceededMaxLines", result.multiParagraph.didExceedMaxLines)
            putBoolean("homeAcceptanceOverflowWidth", result.didOverflowWidth)
            putBoolean("homeAcceptanceOverflowHeight", result.didOverflowHeight)
            putBoolean("homeAcceptanceVisualOverflow", result.hasVisualOverflow)
        }
    }

    private fun captureLargeFontCards(cards: List<String>, screenshot: String) {
        val nodes = cards.map { compose.onNodeWithTag(it) }
        nodes.maxBy { it.fetchSemanticsNode().size.height }.performScrollTo()
        val viewport = compose.onNodeWithTag("home-scroll-viewport").fetchSemanticsNode().boundsInRoot
        for ((tag, node) in cards.zip(nodes)) {
            val card = node.assertIsDisplayed().fetchSemanticsNode()
            val bounds = card.boundsInRoot
            assertTrue("$tag must fit wholly in the viewport for $screenshot",
                bounds.width >= card.size.width - 1f && bounds.height >= card.size.height - 1f &&
                    bounds.top >= viewport.top - 1f && bounds.bottom <= viewport.bottom + 1f)
        }
        assertAddBelowViewport()
        capture(screenshot)
    }

    private fun assertAmount(matcher: SemanticsMatcher, expected: String) {
        waitFor(matcher)
        compose.onNode(matcher, useUnmergedTree = true).assertTextEquals(expected)
        assertUnclippedText(hasAnyAncestor(matcher) and SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult), expected)
    }

    private fun assertUnclippedText(matcher: SemanticsMatcher, expected: String) {
        val node = compose.onNode(matcher, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(expected, layout.layoutInput.text.text.replace("\u2009", ""))
        assertFalse("$expected must not overflow its text layout", layout.hasVisualOverflow)
        val semantics = node.fetchSemanticsNode()
        assertTrue("$expected must not be clipped by its viewport",
            semantics.boundsInRoot.width >= semantics.size.width - 1f &&
                semantics.boundsInRoot.height >= semantics.size.height - 1f)
    }

    private fun assertAddBelowViewport() {
        val add = compose.onNodeWithTag("home-open-add").assertIsDisplayed().assertTextEquals("添加")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("home-scroll-viewport").fetchSemanticsNode().boundsInRoot
        val tab = compose.onNodeWithTag("tab-now").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("tab-review").assertIsDisplayed()
        compose.onNodeWithTag("tab-settings").assertIsDisplayed()
        assertTrue("Add control must be below scrolling content", viewport.bottom <= add.top + 1f)
        assertTrue("Navigation must remain below the add control", add.bottom <= tab.top + 1f)
    }

    private fun selectTheme(theme: LithicAppearance) {
        click("tab-settings")
        waitForTag("settings-screen")
        val controller = AppearanceProvider.get(application())
        compose.waitUntil(WAIT_MILLIS) { controller.state.value.readable && !controller.state.value.loading }
        click("appearance-open", scroll = true)
        waitForTag("appearance-sheet")
        click("appearance-${theme.storageId}", scroll = true)
        compose.waitUntil(WAIT_MILLIS) { controller.state.value.let { it.selected == theme && it.readable && !it.saving && it.error == null } }
        compose.onNodeWithTag("appearance-${theme.storageId}").assertIsSelected()
        pressBack()
        waitForAbsent("appearance-sheet")
        click("tab-now")
        waitForTag("home-screen")
    }

    private fun selectSection(kind: String) {
        scrollHomeToTop()
        click("home-$kind-tab", scroll = true)
        compose.onNodeWithTag("home-$kind-tab").assertIsSelected()
    }

    private fun scrollHomeToTop() {
        waitForTag("home-screen")
        compose.onNodeWithTag("home-screen").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
        compose.waitForIdle()
    }

    private fun waitForInvestment(cents: Long) {
        val amount = AmountFormatter.formatCny(BigDecimal.valueOf(cents, 2))
        waitFor(hasTestTag("home-total-investment") and hasText(amount))
        compose.onNodeWithTag("home-total-investment").assertTextEquals(amount)
    }

    private fun openAndBack(from: String, to: String, scroll: Boolean) {
        click(from, scroll)
        waitForTag(to)
        backToHome(to)
    }

    private fun backToHome(closed: String) {
        pressBack()
        waitForAbsent(closed)
        waitForTag("home-screen")
    }

    private fun click(tag: String, scroll: Boolean = false) {
        waitForTag(tag)
        val node = compose.onNodeWithTag(tag)
        if (scroll) node.performScrollTo()
        node.assertIsDisplayed().performClick()
    }

    private fun waitForTag(tag: String) = waitFor(hasTestTag(tag))

    private fun waitFor(matcher: SemanticsMatcher) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForAbsent(tag: String) {
        compose.waitUntil(WAIT_MILLIS) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun assertFourByThree(matcher: SemanticsMatcher) {
        val frame = compose.onNode(matcher, useUnmergedTree = true).fetchSemanticsNode().size
        assertTrue("Cover frame must measure 4:3, got $frame", frame.width > 0 && frame.height > 0 && abs(frame.width * .75f - frame.height) < 2f)
    }

    private fun requireConfiguration(fontScale: Float) {
        compose.activityRule.scenario.onActivity { activity ->
            val configuration = activity.resources.configuration
            val setting = Settings.System.getString(activity.contentResolver, Settings.System.FONT_SCALE)
            report("configuration") {
                putString("homeAcceptanceSystemFontScale", setting ?: "null")
                putInt("homeAcceptanceScreenWidthDp", configuration.screenWidthDp)
                putFloat("homeAcceptanceFontScale", configuration.fontScale)
            }
            assertEquals("Android system font_scale must be explicitly set for this group", fontScale,
                setting?.toFloatOrNull() ?: Float.NaN, 0.01f)
            assertEquals("Actual Android screen width in dp", 360, configuration.screenWidthDp)
            assertEquals("Actual Android font scale", fontScale, configuration.fontScale, 0.01f)
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val relativeDirectory = "home-android-acceptance/$attemptId/$runId"
        val directory = File(instrumentation.targetContext.filesDir, relativeDirectory)
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create the evidence directory" }
        val file = File(directory, "$name.png")
        check(file.createNewFile()) { "Refusing to replace a screenshot" }
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "Android screenshot not available" }
        try {
            FileOutputStream(file).use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Cannot encode Android screenshot" }
                stream.fd.sync()
            }
            check(file.length() > 0L)
            report("screenshot") {
                putString("homeAcceptancePng", "files/$relativeDirectory/${file.name}")
                putInt("homeAcceptanceWidthPx", bitmap.width)
                putInt("homeAcceptanceHeightPx", bitmap.height)
                putInt("homeAcceptanceScreenWidthDp", compose.activity.resources.configuration.screenWidthDp)
                putFloat("homeAcceptanceFontScale", compose.activity.resources.configuration.fontScale)
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun report(phase: String, details: Bundle.() -> Unit = {}) {
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("homeAcceptanceAttemptId", attemptId)
            putString("homeAcceptanceRunId", runId)
            putString("homeAcceptanceMethod", method)
            putString("homeAcceptancePhase", phase)
            details()
        })
    }

    private fun application() = compose.activity.application as LithicApplication
    private fun project(id: String) = "home-project-preview-$id"
    private fun subscription(id: String) = "home-subscription-preview-$id"
    private fun coverIn(card: String, cover: String) = hasTestTag(cover) and hasAnyAncestor(hasTestTag(card))

    private companion object { const val WAIT_MILLIS = 12_000L }
}
