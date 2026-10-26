package com.cooper.lithic.ui

import android.app.Application
import android.graphics.Bitmap
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.C2PrimaryNavigationBar
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.Review
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.review.ReviewShell
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Future Android capture entry only. Compilation does not produce screenshots or visual acceptance.
 * Requires the actual test Manifest to use M13IsolatedRunner BEFORE process launch.
 * This ComponentActivity is not MainActivity. A-E override local density; F retains device configuration.
 * Only the dedicated c2visual target/test packages are permitted; only their own APKs are hashed.
 */
@RunWith(AndroidJUnit4::class)
class C2ProductionVisualGalleryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var canvasDensity = 1f
    private var canvasWidthDp = 0f
    private var canvasFontScale = 1f
    private var canvasDark = false
    private lateinit var appApkSha256: String
    private lateinit var testApkSha256: String
    private var appUid = -1
    private var testUid = -1
    private var observedActions = 0

    @Before fun requireIsolatedApplication() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assertEquals(TARGET_PACKAGE, instrumentation.targetContext.packageName)
        assertEquals(TEST_PACKAGE, instrumentation.context.packageName)
        assertEquals("com.cooper.lithic.app.M13IsolatedRunner", instrumentation.javaClass.name)
        assertEquals(Application::class.java, instrumentation.targetContext.applicationContext.javaClass)
        val appInfo = instrumentation.targetContext.applicationInfo
        val testInfo = instrumentation.context.applicationInfo
        assertEquals(TARGET_PACKAGE, appInfo.packageName)
        assertEquals(TEST_PACKAGE, testInfo.packageName)
        assertEquals(android.os.Process.myUid(), appInfo.uid)
        assertTrue("Dedicated target and Test require different UIDs", appInfo.uid != testInfo.uid)
        appUid = appInfo.uid
        testUid = testInfo.uid
        appApkSha256 = ownApkSha256(appInfo, TARGET_PACKAGE)
        testApkSha256 = ownApkSha256(testInfo, TEST_PACKAGE)
        // These are post-launch assertions. Manifest/pre-install byte gates remain mandatory.
        compose.runOnUiThread { compose.activity.enableEdgeToEdge() }
    }

    @Test fun A_dark_400_confirmed_empty_now() = scene("A", Home, true, false, 400, 1f)
    @Test fun B_dark_400_confirmed_empty_review() = scene("B", Review, true, false, 400, 1f)
    @Test fun C_light_400_synthetic_project_subscription_now() = scene("C", Home, false, true, 400, 1f)
    @Test fun D_light_400_synthetic_history_review() = scene("D", Review, false, true, 400, 1f)
    @Test fun E1_dark_360_local_200percent_now() = scene("E1", Home, true, false, 360, 2f)
    @Test fun E2_dark_360_local_200percent_review() = scene("E2", Review, true, false, 360, 2f)

    @Test fun F1_device_configuration_empty_now() = scene("F1", Home, null, false, null, null)
    @Test fun F2_device_configuration_empty_review() = scene("F2", Review, null, false, null, null)

    private fun scene(name: String, initial: NavKey, dark: Boolean?, populated: Boolean, width: Int?, scale: Float?) {
        val localOverride = width != null
        require(localOverride == (scale != null))
        val facts = C2VisualFixtures(populated)
        assertEquals(if (populated) 1L else 0L, facts.overview.projectCount)
        assertEquals(if (populated) 1L else 0L, facts.overview.subscriptionCount)
        assertEquals(populated, facts.overview.hasRecords)
        assertEquals(0, facts.overview.totalRate.compareTo(checkNotNull(facts.trend.selectedPoint).totalRate))
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val deviceDensity = LocalDensity.current
                canvasDensity = if (localOverride) constraints.maxWidth / checkNotNull(width).toFloat() else deviceDensity.density
                canvasWidthDp = if (localOverride) checkNotNull(width).toFloat() else constraints.maxWidth / deviceDensity.density
                canvasFontScale = if (localOverride) checkNotNull(scale) else deviceDensity.fontScale
                canvasDark = dark ?: isSystemInDarkTheme()
                val height = minOf(800f, constraints.maxHeight / canvasDensity).dp
                val content: @Composable () -> Unit = {
                    LithicTheme(darkTheme = canvasDark) {
                        var page by remember { mutableStateOf(initial) }
                        val canvas = if (localOverride) Modifier.requiredSize(checkNotNull(width).dp, height) else Modifier.fillMaxSize()
                        Box(canvas.testTag("c2-gallery-canvas")) {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                Scaffold(contentWindowInsets = WindowInsets.safeDrawing,
                                    bottomBar = { C2PrimaryNavigationBar(page) { destination ->
                                        if (destination == Home || destination == Review) page = destination
                                        else observedActions++
                                    } }) { padding ->
                                    Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
                                        if (page == Home) HomeShell(facts.overview.projectCount,
                                            { observedActions++ }, { observedActions++ }, facts.overview.subscriptionCount,
                                            { observedActions++ }, { observedActions++ }, overview = facts.overview,
                                            onSearch = { observedActions++ }, projectPreview = facts.projects,
                                            subscriptionPreview = facts.subscriptions, onOpenProject = { observedActions++ },
                                            onOpenSubscription = { observedActions++ })
                                        else ReviewShell(facts.overview.projectCount, subscriptionCount = facts.overview.subscriptionCount,
                                            trendState = facts.trend, onProjectHistory = { observedActions++ },
                                            onSubscriptionHistory = { observedActions++ }, onLongTermReview = { observedActions++ },
                                            onTotalCostTrend = { observedActions++ }, onCategoryAnalysis = { observedActions++ },
                                            onAnnualReview = { observedActions++ }, onAddRecord = { page = Home })
                                    }
                                }
                            }
                        }
                    }
                }
                if (localOverride) CompositionLocalProvider(LocalDensity provides Density(canvasDensity, canvasFontScale)) { content() }
                else content() // F1/F2 do not replace LocalDensity, fontScale, width, height or system settings.
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag(if (initial == Home) "tab-now" else "tab-review").assertIsSelected()
        if (initial == Home) {
            val fab = compose.onNodeWithTag("home-open-add").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            assertEquals(56f * canvasDensity, fab.width, 1.5f)
            compose.onNodeWithTag("home-first-record").assertDoesNotExist()
            compose.onNodeWithTag("home-total-rate").assertTextContains(if (populated) "¥" else "—", substring = true)
            compose.onNodeWithTag("home-project-record-count").assertTextEquals(if (populated) "1" else "0")
        } else if (populated) {
            compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed()
            compose.onNodeWithTag("review-empty-state").assertDoesNotExist()
        } else {
            compose.onNodeWithTag("review-empty-identity").assertIsDisplayed()
            compose.onNodeWithTag("review-add-record").performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            compose.onNodeWithTag("total-trend-canvas").assertDoesNotExist()
        }
        val root = compose.onNodeWithTag("c2-gallery-canvas").fetchSemanticsNode().boundsInRoot
        val primary = compose.onNodeWithTag(if (initial == Home) "home-open-add" else if (populated) "review-information" else "review-add-record")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(primary.left >= root.left && primary.right <= root.right + 1f)
        assertEquals("Capture does not create or mutate any business record", 0, observedActions)
        capture(name, initial == Home, populated, localOverride, facts, scrollToRequested = initial == Review && !populated)
        if (initial == Home && populated) {
            compose.onNodeWithTag("home-project-preview-${facts.projects.items.single().project.id}").performScrollTo().assertIsDisplayed()
            compose.onNodeWithTag("home-subscription-preview-${facts.subscriptions.items.single().subscription.id}").performScrollTo().assertIsDisplayed()
            capture("$name-records-scrolled", true, populated, localOverride, facts, scrollToRequested = true)
        }
    }

    private fun capture(name: String, home: Boolean, populated: Boolean, localOverride: Boolean, facts: C2VisualFixtures, scrollToRequested: Boolean) {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals(TARGET_PACKAGE, context.packageName)
        val run = UUID.randomUUID().toString()
        val directory = File(context.filesDir, "ui-c2-c3-gallery-$run").canonicalFile
        assertEquals(context.filesDir.canonicalFile, directory.parentFile)
        check(directory.mkdir())
        writeExclusive(File(directory, "owner.json"), JSONObject().put("kind", "C2_SYNTHETIC_ANDROID_CAPTURE")
            .put("runId", run).put("package", context.packageName).toString(2).toByteArray(Charsets.UTF_8))
        val image = compose.onNodeWithTag("c2-gallery-canvas").captureToImage().asAndroidBitmap()
        val png = File(directory, "$name.png");check(png.createNewFile())
        FileOutputStream(png).use { stream -> check(image.compress(Bitmap.CompressFormat.PNG, 100, stream));stream.fd.sync() }
        check(png.length() in 1..20L * 1024 * 1024)
        val sha = MessageDigest.getInstance("SHA-256").digest(png.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
        val metadata = JSONObject().put("case", name).put("runId", run).put("host", "isolated ComponentActivity")
            .put("package", context.packageName).put("testPackage", TEST_PACKAGE)
            .put("appUid", appUid).put("testUid", testUid)
            .put("runner", InstrumentationRegistry.getInstrumentation().javaClass.name)
            .put("applicationClass", context.applicationContext.javaClass.name)
            .put("hostClass", compose.activity.javaClass.name)
            .put("appApkSha256", appApkSha256).put("testApkSha256", testApkSha256)
            .put("apkHashSource", "streamed own target/test ApplicationInfo.sourceDir only; compare with desktop preflight before acceptance")
            .put("mainActivity", false).put("source", "production HomeShell/ReviewShell/C2PrimaryNavigationBar")
            .put("page", if (home) "now" else "review").put("dark", canvasDark).put("syntheticFacts", true)
            .put("scrollToRequested", scrollToRequested)
            .put("projectCount", facts.overview.projectCount).put("subscriptionCount", facts.overview.subscriptionCount)
            .put("fixtureConfirmation", "frozen engines over known immutable facts; no Room or default ledger opened")
            .put("canvasWidthDp", canvasWidthDp).put("effectiveFontScale", canvasFontScale).put("canvasDensity", canvasDensity)
            .put("hostSystemFontScale", compose.activity.resources.configuration.fontScale)
            .put("hostSystemDensityDpi", compose.activity.resources.configuration.densityDpi)
            .put("hostScreenWidthDp", compose.activity.resources.configuration.screenWidthDp)
            .put("mode", if (localOverride) "CONTROLLED_LOCAL_DENSITY" else "ACTUAL_HOST_CONFIGURATION")
            .put("localDensityOverride", localOverride).put("systemSettingsChanged", false).put("navigationGuardJourneyTested", false)
            .put("storageOwnerCreated", false).put("widthPx", image.width).put("heightPx", image.height)
            .put("bytes", png.length()).put("sha256", sha)
        writeExclusive(File(directory, "capture.json"), metadata.toString(2).toByteArray(Charsets.UTF_8))
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("c3_capture_directory", directory.name)
            putString("c3_capture_png", png.name)
            putString("c3_capture_sha256", sha)
        })
        // Retain only this new UUID's bounded synthetic evidence; no cleanup of any other directory.
    }

    private fun ownApkSha256(info: ApplicationInfo, expectedPackage: String): String {
        check(info.packageName == expectedPackage && info.uid >= 0)
        check(info.splitSourceDirs.isNullOrEmpty()) { "Unexpected split APK identity" }
        val apk = File(info.sourceDir).canonicalFile
        check(apk.isFile && apk.length() in 1..64L * 1024 * 1024) { "Own base APK is unavailable or exceeds the read bound" }
        val digest = MessageDigest.getInstance("SHA-256")
        apk.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            var readTotal = 0L
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                readTotal += count
                check(readTotal <= 64L * 1024 * 1024)
                digest.update(buffer, 0, count)
            }
            check(readTotal == apk.length()) { "Own APK length changed while hashing" }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.cooper.lithic.c2visual.debug"
        const val TEST_PACKAGE = "$TARGET_PACKAGE.test"
    }

    private fun writeExclusive(file: File, bytes: ByteArray) {
        check(file.createNewFile());FileOutputStream(file).use { it.write(bytes);it.fd.sync() }
    }
}
