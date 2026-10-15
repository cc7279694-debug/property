package com.cooper.lithic.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.view.WindowCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LithicNavigation
import com.cooper.lithic.app.readHomeOverview
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.SyntheticImageSource
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.feature.media.CoverViewModel
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.subscription.SubscriptionViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Evidence producer, not a production demo mode. Every row and original Canvas cover is owned by
 * one temporary Room generation beneath a distinct noBackup child. Production repositories write
 * the inputs and the frozen domain engines calculate every cost displayed by real navigation.
 */
class UIC1RefinedVisualEvidenceTest {
    @get:Rule val compose = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 10, 15))
    private val timestamp = Instant.parse("2026-10-15T08:00:00Z")
    private val models = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private val sources = mutableListOf<SyntheticImageSource>()
    private val captures = JSONArray()

    @Test fun isolated_synthetic_records_and_original_covers_render_three_pages_in_both_themes() = runBlocking {
        val actualPointer = File(context.noBackupFilesDir, "active-generation")
        val activePointerBefore = actualPointer.takeIf(File::isFile)?.readBytes()
        val root = File(context.noBackupFilesDir, "ui-c1-refined-isolated-${uuid()}")
        assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
        val generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        try {
            val projects = ProjectRepository(generation.database, generation.coordinator, clock)
            val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
            val media = MediaRepository(generation.database, generation.coordinator, generation.images, clock, context.contentResolver)
            val notebook = project(projects, media, "轻薄笔记本", LocalDate.of(2025, 1, 1), 799_900, 0)
            project(projects, media, "通勤耳机", LocalDate.of(2024, 12, 1), 99_900, 1)
            project(projects, media, "旅行相机", LocalDate.of(2024, 10, 1), 499_900, 2)
            subscription(subscriptions, "音乐年卡", BillingCycle.YEARLY, 19_800,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))
            subscription(subscriptions, "云盘空间", BillingCycle.MONTHLY, 1_800,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))

            val read = readHomeOverview(projects, subscriptions, clock.today())
            assertEquals(3L, read.projectCount)
            assertEquals(2L, read.subscriptionCount)
            val before = projects.loadCurrentProjectWindow(1, clock.today())
            val beforeSubscriptions = subscriptions.loadSubscriptionWindow(1, clock.today())
            val coverHashesBefore = before.items.associate { detail ->
                val asset = success(media.openAsset(checkNotNull(detail.project.coverAssetId)))
                detail.project.id to sha256(asset.bytes)
            }
            val state = LedgerUiState.Ready(read.projectCount, generation.settings.recordZone(), projects,
                clock, subscriptions, read.subscriptionCount, mediaRepository = media, homePreview = read.preview)
            lateinit var projectModel: ProjectViewModel
            lateinit var subscriptionModel: SubscriptionViewModel
            lateinit var coverModel: CoverViewModel
            onMain {
                projectModel = retain("project", ProjectViewModel(projects, SavedStateHandle(), clock) { timestamp })
                subscriptionModel = retain("subscription", SubscriptionViewModel(subscriptions, SavedStateHandle(), clock) { timestamp })
                coverModel = retain("cover", CoverViewModel(media, SavedStateHandle()) { timestamp.plusSeconds(2) })
            }
            var dark by mutableStateOf(false)
            compose.setContent {
                LithicTheme(darkTheme = dark) {
                    val view = LocalView.current
                    SideEffect {
                        val window = checkNotNull(view.context.activity()).window
                        // Match MainActivity's edge-to-edge geometry; only this test host is changed.
                        WindowCompat.setDecorFitsSystemWindows(window, false)
                        WindowCompat.getInsetsController(window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    Surface { LithicNavigation(state, projectViewModel = projectModel,
                        subscriptionViewModel = subscriptionModel, coverViewModel = coverModel) }
                }
            }

            for (theme in listOf("light", "dark")) {
                compose.runOnIdle { dark = theme == "dark" }
                compose.onNodeWithTag("home-screen").assertIsDisplayed()
                awaitCover("home-project-$notebook-cover-image")
                capture("synthetic-now-$theme.png", "现在", theme, generation)

                compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
                compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
                awaitCover("project-$notebook-cover-image")
                capture("synthetic-project-list-$theme.png", "长期项目列表", theme, generation)

                compose.onNodeWithTag("project-row-$notebook").performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithTag("project-detail-name").fetchSemanticsNodes().isNotEmpty()
                }
                awaitCover("project-cover-image")
                compose.onNodeWithTag("project-detail-daily").assertIsDisplayed()
                capture("synthetic-project-detail-$theme.png", "长期项目详情", theme, generation)

                compose.onNodeWithTag("project-detail-back").performScrollTo().performClick()
                compose.onNodeWithTag("project-list-back").performScrollTo().performClick()
            }

            assertEquals("Read-only navigation must not edit the isolated fixture", before,
                projects.loadCurrentProjectWindow(1, clock.today()))
            assertEquals(beforeSubscriptions, subscriptions.loadSubscriptionWindow(1, clock.today()))
            before.items.forEach { detail ->
                val bytes = success(media.openAsset(checkNotNull(detail.project.coverAssetId))).bytes
                assertEquals(coverHashesBefore[detail.project.id], sha256(bytes))
            }
            val facts = JSONArray()
            before.items.forEach { detail -> facts.put(JSONObject()
                .put("name", detail.project.name).put("startedOn", detail.project.startedOn.toString())
                .put("heldDays", detail.cost.heldDays).put("grossOutlayCents", detail.cost.grossOutlayCents)
                .put("recoveryCents", detail.cost.recoveryCents).put("netCents", detail.cost.netCents)
                .put("dailyRateExact", detail.cost.dailyRate?.toPlainString())
                .put("coverSha256", coverHashesBefore[detail.project.id])) }
            val payments = JSONArray()
            beforeSubscriptions.items.forEach { detail -> payments.put(JSONObject()
                .put("name", detail.subscription.name).put("historicalAverageExact", detail.cost.historicalAverage?.toPlainString())
                .put("standardPurchasedDays", detail.cost.standardPurchasedDays)
                .put("status", detail.cost.displayStatus.name)) }
            val report = JSONObject().put("source", "ISOLATED_ANDROID_TEST_HOST")
                .put("productionRecordSource", false).put("fixtureUsesActiveLedger", false).put("fixtureWritesActiveLedger", false)
                .put("applicationOwnerNote", "Normal Application startup may open the default owner; fixture facts are written only in its isolated root")
                .put("isolatedRootName", root.name).put("observationDay", clock.today().toString())
                .put("recordZone", state.recordZone.id).put("projectCount", state.projectCount)
                .put("subscriptionCount", state.subscriptionCount)
                .put("covers", "Original geometric illustrations drawn at runtime by android.graphics.Canvas in androidTest only; no reference photos or private media")
                .put("costSource", "Existing ProjectRepository / SubscriptionRepository using frozen production domain engines")
                .put("projects", facts).put("subscriptions", payments).put("captures", captures)
                .put("fixtureRowsAndCoverBytesUnchangedAfterCaptures", true)
            output("fixture-source.json").writeText(report.toString(2))
        } finally {
            onMain { models.clear() }
            jobs.forEach { it.join() }
            generation.close()
            sources.forEach { assertTrue("Only a URI created by this fixture may be removed", it.delete()) }
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            assertTrue("Only the unique isolated fixture root is removed", root.deleteRecursively())
            if (activePointerBefore == null) assertTrue(!actualPointer.exists())
            else assertArrayEquals("The real ledger's active pointer is unchanged", activePointerBefore, actualPointer.readBytes())
        }
    }

    private suspend fun project(projects: ProjectRepository, media: MediaRepository, name: String,
        started: LocalDate, amountCents: Long, illustration: Int): String {
        val operation = uuid()
        val project = LongTermProjectEntity(uuid(), name, LithicDatabase.PROJECT_FALLBACK_ID, started,
            note = "仅用于隔离视觉验收的合成记录", createdAt = timestamp, updatedAt = timestamp)
        val event = ProjectMoneyEventEntity(uuid(), project.id, MoneyDirection.OUTLAY, amountCents, started,
            "合成测试初始投入", ProjectMoneyEventRole.INITIAL, operation)
        val created = projects.createProject(CreateProjectCommand(operation, project, event))
        assertTrue("Fixture creation must use the production write path: $created", created is MutationResult.Success)
        val source = M10ImageSourceFixtures.source(cover(illustration)).also(sources::add)
        val prepared = success(media.prepareImport(source.uri, uuid()))
        val baseline = success(media.loadTarget(CoverTarget(CoverTargetType.PROJECT, project.id)))
        success(media.commit(CoverCommand(uuid(), baseline, prepared.asset, timestamp.plusSeconds(1))))
        return project.id
    }

    private suspend fun subscription(repository: SubscriptionRepository, name: String, cycle: BillingCycle,
        cents: Long, from: LocalDate, until: LocalDate) {
        val operation = uuid()
        val identity = SubscriptionEntity(uuid(), name, LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            note = "仅用于隔离视觉验收的合成订阅", createdAt = timestamp, updatedAt = timestamp)
        val renewal = SubscriptionRenewalEntity(uuid(), identity.id, from, from, until, until.minusDays(3),
            cents, cycle, cycle.standardDays, operation)
        val saved = repository.createWithRenewal(CreateSubscriptionCommand(operation, identity, renewal))
        assertTrue("Fixture payment must use the production write path: $saved", saved is SubscriptionMutationResult.Success)
    }

    private fun capture(fileName: String, page: String, theme: String, generation: ManagedGeneration) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName
        }
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val output = output(fileName)
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        captures.put(JSONObject().put("file", fileName).put("page", page).put("theme", theme)
            .put("source", "ISOLATED_ANDROID_TEST_HOST").put("widthPixels", bitmap.width)
            .put("heightPixels", bitmap.height).put("sha256", sha256(output.readBytes()))
            .put("sdkInt", android.os.Build.VERSION.SDK_INT)
            .put("densityDpi", context.resources.displayMetrics.densityDpi)
            .put("fontScale", context.resources.configuration.fontScale.toDouble())
            .put("generationId", generation.id))
        bitmap.recycle()
    }

    private fun awaitCover(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun output(name: String): File = File(context.noBackupFilesDir, "ui-c1-refined-visual/$name").also {
        check(it.parentFile!!.mkdirs() || it.parentFile!!.isDirectory)
    }

    private fun <T : ViewModel> retain(key: String, model: T): T = model.also {
        models.put(key, it)
        jobs += checkNotNull(it.viewModelScope.coroutineContext[Job])
    }

    private fun onMain(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

    private fun <T> success(result: MediaResult<T>): T {
        assertTrue("Fixture input must pass the original media protocol: $result", result is MediaResult.Success)
        return (result as MediaResult.Success<T>).value
    }

    private fun uuid() = UUID.randomUUID().toString()
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }

    /** Original neutral still-life geometry; these are illustrations, not photographs. */
    private fun cover(kind: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(960, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun rect(color: Int, left: Float, top: Float, right: Float, bottom: Float, radius: Float = 0f) {
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, paint)
        }
        fun oval(color: Int, left: Float, top: Float, right: Float, bottom: Float) {
            paint.color = color
            paint.style = Paint.Style.FILL
            canvas.drawOval(RectF(left, top, right, bottom), paint)
        }
        val cream = Color.rgb(222, 218, 204)
        val charcoal = Color.rgb(41, 48, 44)
        val silver = Color.rgb(178, 185, 179)
        val green = Color.rgb(108, 135, 118)
        canvas.drawColor(if (kind == 1) Color.rgb(211, 220, 209) else cream)
        oval(Color.argb(24, 45, 48, 41), 150f, 445f, 825f, 535f)
        when (kind) {
            0 -> {
                rect(charcoal, 243f, 118f, 716f, 414f, 14f)
                rect(green, 259f, 134f, 700f, 394f, 3f)
                paint.color = Color.rgb(137, 159, 142)
                canvas.drawPath(Path().apply { moveTo(259f, 394f); lineTo(435f, 172f); lineTo(584f, 394f); close() }, paint)
                paint.color = Color.rgb(83, 111, 96)
                canvas.drawPath(Path().apply { moveTo(438f, 394f); lineTo(629f, 190f); lineTo(700f, 394f); close() }, paint)
                paint.color = silver
                canvas.drawPath(Path().apply { moveTo(242f, 414f); lineTo(717f, 414f); lineTo(799f, 483f)
                    lineTo(160f, 483f); close() }, paint)
                rect(Color.rgb(145, 154, 148), 160f, 480f, 799f, 494f, 6f)
                rect(Color.rgb(151, 160, 153), 409f, 435f, 550f, 468f, 4f)
            }
            1 -> {
                paint.color = charcoal
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 60f
                canvas.drawArc(RectF(300f, 110f, 660f, 515f), 180f, 180f, false, paint)
                paint.style = Paint.Style.FILL
                rect(charcoal, 271f, 282f, 378f, 487f, 47f)
                rect(Color.rgb(64, 73, 66), 295f, 296f, 377f, 472f, 37f)
                rect(charcoal, 582f, 282f, 689f, 487f, 47f)
                rect(Color.rgb(64, 73, 66), 583f, 296f, 665f, 472f, 37f)
                rect(Color.rgb(112, 125, 112), 321f, 167f, 348f, 306f, 10f)
                rect(Color.rgb(112, 125, 112), 612f, 167f, 639f, 306f, 10f)
            }
            else -> {
                rect(Color.rgb(107, 112, 100), 230f, 219f, 734f, 461f, 28f)
                rect(charcoal, 230f, 272f, 734f, 461f, 24f)
                rect(charcoal, 327f, 183f, 484f, 239f, 11f)
                rect(Color.rgb(143, 151, 138), 263f, 193f, 309f, 220f, 7f)
                oval(Color.rgb(22, 29, 26), 374f, 241f, 638f, 505f)
                oval(Color.rgb(84, 98, 89), 399f, 266f, 613f, 480f)
                oval(Color.rgb(27, 48, 43), 419f, 286f, 593f, 460f)
                oval(Color.rgb(45, 80, 69), 451f, 307f, 565f, 421f)
                oval(Color.rgb(142, 171, 153), 476f, 322f, 507f, 353f)
                rect(Color.rgb(216, 219, 205), 660f, 234f, 704f, 258f, 3f)
            }
        }
        val bytes = ByteArrayOutputStream()
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes))
        bitmap.recycle()
        return bytes.toByteArray()
    }
}
