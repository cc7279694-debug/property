package com.cooper.lithic.feature.home

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.report.OverviewCategory
import com.cooper.lithic.domain.report.OverviewEngine
import com.cooper.lithic.domain.report.OverviewKind
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.OverviewResult
import com.cooper.lithic.domain.report.OverviewSnapshot
import com.cooper.lithic.domain.report.OverviewSubscription
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.Renewal
import com.cooper.lithic.domain.subscription.Subscription
import com.cooper.lithic.domain.subscription.SubscriptionSnapshot
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated ComponentActivity screenshots: synthetic domain facts, never MainActivity or a ledger read. */
@RunWith(AndroidJUnit4::class)
class M13HomeScreenshotsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val asOf = LocalDate.of(2026, 1, 20)

    @Test
    fun capture_synthetic_engine_data_in_light_test_host() {
        show(syntheticOverview(), dark = false)
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥160.60")
        capture("data-light")
        assertActionsReachable()
        capture("data-light-lower-entries")
    }

    @Test
    fun capture_empty_overview_in_dark_test_host() {
        show(OverviewEngine.evaluate(OverviewSnapshot(emptyList(), emptyList(), emptyList()), asOf), dark = true)
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        capture("empty-dark")
        assertActionsReachable()
    }

    @Test
    fun capture_360dp_200percent_text_in_dark_test_host() {
        show(syntheticOverview(), dark = true, fontScale = 2f)
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥160.60")
        capture("large-text-360dp-dark")
        assertActionsReachable()
    }

    private fun show(overview: OverviewResult, dark: Boolean, fontScale: Float = 1f) {
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("home-test-viewport")) {
                val canvasDensity = constraints.maxWidth / 360f
                val canvasHeight = minOf(640f, constraints.maxHeight / canvasDensity).dp
                CompositionLocalProvider(LocalDensity provides Density(canvasDensity, fontScale)) {
                    LithicTheme(darkTheme = dark) {
                        Box(Modifier.requiredSize(width = 360.dp, height = canvasHeight).testTag("home-test-canvas")) {
                            Surface(Modifier.fillMaxSize()) {
                                HomeShell(overview.projectCount, {}, {}, overview.subscriptionCount, {}, {}, overview = overview)
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        assertHomeFitsVisibleWidth()
    }

    private fun assertActionsReachable() {
        listOf("open-project-list", "home-create-project", "open-subscription-list", "home-create-subscription")
            .forEach { tag -> compose.onNodeWithTag(tag).performScrollTo().assertIsDisplayed() }
    }

    private fun capture(name: String) {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        compose.waitForIdle()
        assertHomeFitsVisibleWidth()
        val bitmap = compose.onNodeWithTag("home-test-canvas").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.filesDir, "m13-evidence")
        check(directory.isDirectory || directory.mkdir()) { "Cannot create the dedicated screenshot directory" }
        val output = File(directory, "$name-${UUID.randomUUID()}.png")
        check(output.createNewFile()) { "Screenshot evidence must not replace an existing file" }
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) { "Cannot encode test-host screenshot" }
            stream.fd.sync()
        }
        check(output.length() > 0L) { "Test-host screenshot is empty" }
        println("M13_COMPONENT_ACTIVITY_SCREENSHOT=${output.absolutePath}")
    }

    private fun assertHomeFitsVisibleWidth() {
        val viewport = compose.onNodeWithTag("home-test-viewport").fetchSemanticsNode()
        val home = compose.onNodeWithTag("home-test-canvas").fetchSemanticsNode()
        val viewportLeft = viewport.positionInRoot.x
        val viewportRight = viewportLeft + viewport.size.width
        val homeLeft = home.positionInRoot.x
        val homeRight = homeLeft + home.size.width
        // Inspect raw layout pixels, not clipped screenshots or dp values from different densities.
        assertTrue("Home exceeds visible width: home=[$homeLeft, $homeRight], viewport=[$viewportLeft, $viewportRight]",
            homeLeft >= viewportLeft - 0.5f && homeRight <= viewportRight + 0.5f)
    }

    private fun syntheticOverview(): OverviewResult {
        val cameraStart = LocalDate.of(2026, 1, 1)
        val computerStart = LocalDate.of(2025, 12, 22)
        val serviceStart = LocalDate.of(2026, 1, 1)
        val snapshot = OverviewSnapshot(
            categories = listOf(
                OverviewCategory("camera-category", "相机与影像", OverviewKind.PROJECT),
                OverviewCategory("computer-category", "电脑与设备", OverviewKind.PROJECT),
                OverviewCategory("service-category", "固定月付服务", OverviewKind.SUBSCRIPTION)),
            projects = listOf(
                OverviewProject("synthetic-camera", "camera-category", ProjectSnapshot(LongTermProject(cameraStart),
                    listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(120_000L), cameraStart)))),
                OverviewProject("synthetic-computer", "computer-category", ProjectSnapshot(LongTermProject(computerStart),
                    listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(300_000L), computerStart))))),
            subscriptions = listOf(OverviewSubscription("synthetic-monthly", "service-category", SubscriptionSnapshot(
                Subscription("synthetic-monthly"), listOf(Renewal(Money(1_800L), BillingCycle.MONTHLY,
                    serviceStart, serviceStart, LocalDate.of(2026, 2, 1)))))),
        )
        return OverviewEngine.evaluate(snapshot, asOf)
    }
}
