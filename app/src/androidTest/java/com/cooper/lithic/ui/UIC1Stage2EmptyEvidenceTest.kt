package com.cooper.lithic.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LithicNavigation
import com.cooper.lithic.app.readHomeOverview
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.subscription.SubscriptionViewModel
import java.io.File
import java.security.MessageDigest
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Final-input empty Room evidence; this is an isolated Android test host, not MainActivity. */
class UIC1Stage2EmptyEvidenceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun isolated_empty_room_home_supplies_final_stage2_dual_theme_evidence_without_writes() = runBlocking<Unit> {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val privateRoot = context.noBackupFilesDir.canonicalFile
        val actualPointer = File(privateRoot, "active-generation")
        val activePointerBefore = actualPointer.takeIf(File::isFile)?.readBytes()
        val rootName = "ui-c1-stage2-empty-isolated-${UUID.randomUUID()}"
        val root = File(privateRoot, rootName)
        assertFalse("The isolated generation must be new", root.exists())
        assertEquals(privateRoot, root.canonicalFile.parentFile)
        val outputRoot = File(privateRoot, "ui-c1-stage2-empty-visual")
        check(outputRoot.mkdirs() || outputRoot.isDirectory)
        assertEquals(outputRoot.absoluteFile, outputRoot.canonicalFile)
        val captures = JSONArray()
        val models = ViewModelStore()
        val jobs = mutableListOf<Job>()
        val generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        try {
            val clock = FixedBusinessClock(LocalDate.of(2026, 10, 15))
            val projects = ProjectRepository(generation.database, generation.coordinator, clock)
            val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
            val read = readHomeOverview(projects, subscriptions, clock.today())
            assertEquals(0L, read.projectCount)
            assertEquals(0L, read.subscriptionCount)
            assertTrue(read.preview.projects.isEmpty())
            assertTrue(read.preview.subscriptions.isEmpty())
            val beforeProjects = projects.loadProjectWindow(1)
            val beforeSubscriptions = subscriptions.loadSubscriptionWindow(1)
            val zoneBefore = generation.settings.recordZone()
            val state = LedgerUiState.Ready(read.projectCount, zoneBefore, projects,
                clock, subscriptions, read.subscriptionCount, homePreview = read.preview)
            lateinit var projectModel: ProjectViewModel
            lateinit var subscriptionModel: SubscriptionViewModel
            instrumentation.runOnMainSync {
                projectModel = ProjectViewModel(projects, SavedStateHandle(), clock)
                subscriptionModel = SubscriptionViewModel(subscriptions, SavedStateHandle(), clock)
                models.put("project", projectModel)
                models.put("subscription", subscriptionModel)
                jobs += checkNotNull(projectModel.viewModelScope.coroutineContext[Job])
                jobs += checkNotNull(subscriptionModel.viewModelScope.coroutineContext[Job])
            }
            var dark by mutableStateOf(false)
            var host: Activity? = null
            val apkSha256 = sha256(File(context.applicationInfo.sourceDir).readBytes())
            compose.setContent {
                LithicTheme(darkTheme = dark) {
                    val view = LocalView.current
                    SideEffect {
                        val activity = checkNotNull(view.context.emptyStage2Activity())
                        host = activity
                        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                        WindowCompat.getInsetsController(activity.window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    Surface { LithicNavigation(state, projectViewModel = projectModel,
                        subscriptionViewModel = subscriptionModel) }
                }
            }
            for (theme in listOf("light", "dark")) {
                compose.runOnIdle { dark = theme == "dark" }
                compose.onNodeWithTag("home-screen").assertIsDisplayed()
                compose.onNodeWithTag("home-first-record").assertIsDisplayed()
                compose.onNodeWithTag("home-add-record").assertDoesNotExist()
                compose.waitForIdle()
                instrumentation.waitForIdleSync()
                compose.waitUntil(10_000) {
                    instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName
                }
                val metadata = JSONObject()
                compose.runOnIdle {
                    val activity = checkNotNull(host)
                    assertTrue("The recorded host must own the foreground window", activity.hasWindowFocus())
                    val metrics = activity.resources.displayMetrics
                    val configuration = activity.resources.configuration
                    val hostIdentity = activity.componentName.flattenToShortString()
                    metadata.put("actualHostActivity", hostIdentity)
                        .put("actualForegroundHostActivity", hostIdentity)
                        .put("densityDpi", metrics.densityDpi).put("density", metrics.density.toDouble())
                        .put("fontScale", configuration.fontScale.toDouble())
                        .put("configurationScreenWidthDp", configuration.screenWidthDp)
                        .put("systemNightMode", configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)
                }
                val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                try {
                    val fileName = "empty-home-$theme.png"
                    val output = File(outputRoot, fileName)
                    output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                    captures.put(metadata.put("file", fileName).put("page", "现在")
                        .put("state", "GENUINELY_EMPTY_ROOM").put("theme", theme)
                        .put("themeSource", "EXPLICIT_COMPOSE_THEME_IN_TEST_HOST")
                        .put("source", "ISOLATED_EMPTY_ROOM_NAVIGATION_HOST")
                        .put("scenario", "PRODUCTION_READ_HOME_OVERVIEW_OVER_EMPTY_REPOSITORIES")
                        .put("widthPixels", bitmap.width).put("heightPixels", bitmap.height)
                        .put("widthDp", bitmap.width / metadata.getDouble("density"))
                        .put("sha256", sha256(output.readBytes())).put("installedApkSha256", apkSha256)
                        .put("sdkInt", Build.VERSION.SDK_INT).put("device", Build.DEVICE)
                        .put("model", Build.MODEL).put("manufacturer", Build.MANUFACTURER)
                        .put("generationId", generation.id))
                } finally { bitmap.recycle() }
                compose.onNodeWithTag("home-first-record").performClick()
                compose.onNodeWithTag("add-record-sheet").assertIsDisplayed()
                compose.onNodeWithTag("add-record-dismiss").performClick()
                compose.onNodeWithTag("add-record-sheet").assertDoesNotExist()
                assertEquals(0L, projects.loadSummary().projectCount)
                assertEquals(0L, subscriptions.loadSubscriptionCount())
            }
            assertEquals(beforeProjects, projects.loadProjectWindow(1))
            assertEquals(beforeSubscriptions, subscriptions.loadSubscriptionWindow(1))
            assertEquals(zoneBefore, generation.settings.recordZone())
            assertEquals(2, captures.length())
        } finally {
            instrumentation.runOnMainSync { models.clear() }
            jobs.forEach { it.join() }
            generation.close()
            assertEquals("Only this test's UUID root may be removed", File(privateRoot, rootName), root.canonicalFile)
            assertEquals(privateRoot, root.canonicalFile.parentFile)
            assertTrue("Remove only the isolated empty Room generation", root.deleteRecursively())
            assertFalse(root.exists())
            if (activePointerBefore == null) assertFalse(actualPointer.exists())
            else assertArrayEquals("The real ledger active pointer must remain unchanged", activePointerBefore, actualPointer.readBytes())
        }
        File(outputRoot, "fixture-source.json").writeText(JSONObject()
            .put("source", "ISOLATED_EMPTY_ROOM_NAVIGATION_HOST").put("productionRecordSource", false)
            .put("fixtureUsesActiveLedger", false).put("fixtureWritesActiveLedger", false)
            .put("applicationOwnerNote", "Application startup may open the default owner; no fixture facts are written there")
            .put("readSource", "Production readHomeOverview / ProjectRepository / SubscriptionRepository")
            .put("isolatedRootName", rootName).put("generationId", generation.id)
            .put("projectCount", 0).put("subscriptionCount", 0).put("recordZone", "Asia/Shanghai")
            .put("emptyFactsAndSettingsUnchangedAfterCaptures", true)
            .put("originalActivePointerUnchanged", true).put("ownUuidRootRemoved", true)
            .put("captures", captures).toString(2))
    }

    private fun Context.emptyStage2Activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.emptyStage2Activity()
        else -> null
    }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
