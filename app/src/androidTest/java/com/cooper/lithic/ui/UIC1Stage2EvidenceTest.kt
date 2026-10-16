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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.media.CoverCommand
import com.cooper.lithic.data.media.CoverTarget
import com.cooper.lithic.data.media.CoverTargetType
import com.cooper.lithic.data.media.M10ImageSourceFixtures
import com.cooper.lithic.data.media.MediaRepository
import com.cooper.lithic.data.media.MediaResult
import com.cooper.lithic.data.media.SyntheticImageSource
import com.cooper.lithic.data.repository.ConfirmRenewalCommand
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.EndProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.SetSubscriptionStateCommand
import com.cooper.lithic.data.repository.SubscriptionLifecycleResult
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionRepository
import com.cooper.lithic.data.repository.facts
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.feature.media.CoverViewModel
import com.cooper.lithic.feature.project.ProjectViewModel
import com.cooper.lithic.feature.subscription.RenewalHistoryUiState
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
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
 * Final-input evidence producer. Normal/history/operation pages use real navigation over a unique
 * Room generation. Read-error and missing-cover displays are separately labelled injected UI states;
 * they do not simulate storage corruption or modify the default ledger/pointer.
 */
class UIC1Stage2EvidenceTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = FixedBusinessClock(LocalDate.of(2026, 10, 15))
    private val timestamp = Instant.parse("2026-10-15T08:00:00Z")
    private val models = ViewModelStore()
    private val jobs = mutableListOf<Job>()
    private val sources = mutableListOf<SyntheticImageSource>()
    private val captures = JSONArray()
    private var hostActivity = "NOT_CAPTURED"

    @Test
    fun isolated_subscription_history_operations_and_error_states_supply_stage2_evidence() = runBlocking<Unit> {
        val actualPointer = File(context.noBackupFilesDir, "active-generation")
        val activePointerBefore = actualPointer.takeIf(File::isFile)?.readBytes()
        val root = File(context.noBackupFilesDir, "ui-c1-stage2-isolated-${uuid()}")
        assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
        val generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        try {
            val projects = ProjectRepository(generation.database, generation.coordinator, clock)
            val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
            val media = MediaRepository(generation.database, generation.coordinator, generation.images, clock, context.contentResolver)
            project(projects, media, "随身相机", LocalDate.of(2024, 10, 1), 499_900, 2)
            val endedId = project(projects, media, "旧通勤耳机", LocalDate.of(2024, 1, 1), 99_900, 1)
            val ended = checkNotNull(projects.loadProjectDetail(endedId)).project
            val endedResult = projects.endProject(EndProjectCommand(uuid(), ended, LocalDate.of(2026, 9, 30),
                "自然结束使用，保留真实成本记录", null, timestamp.plusSeconds(3)))
            assertTrue("Historical project uses the production end protocol: $endedResult", endedResult is MutationResult.LifecycleApplied)
            subscription(subscriptions, "音乐年卡", BillingCycle.YEARLY, 19_800,
                LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1))
            subscription(subscriptions, "云盘空间", BillingCycle.MONTHLY, 1_800,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1))
            subscription(subscriptions, "阅读月卡", BillingCycle.MONTHLY, 2_500,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1))
            subscription(subscriptions, "旧健身订阅", BillingCycle.MONTHLY, 9_900,
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1))
            val identities = subscriptions.loadSubscriptionWindow(1).items.associate { it.subscription.name to it.subscription.id }
            val musicId = checkNotNull(identities["音乐年卡"])
            attachSubscriptionCover(media, musicId, 1)
            attachSubscriptionCover(media, checkNotNull(identities["云盘空间"]), 0)
            val futureOperation = uuid()
            val future = SubscriptionRenewalEntity(uuid(), musicId, LocalDate.of(2026, 10, 10),
                LocalDate.of(2027, 1, 1), LocalDate.of(2028, 1, 1), LocalDate.of(2027, 12, 29),
                21_900, BillingCycle.YEARLY, BillingCycle.YEARLY.standardDays, futureOperation)
            val futureResult = subscriptions.confirmRenewal(ConfirmRenewalCommand(futureOperation, future))
            assertTrue("Prepaid future coverage uses the real payment protocol: $futureResult", futureResult is SubscriptionMutationResult.Success)
            changeState(subscriptions, checkNotNull(identities["云盘空间"]), SubscriptionStateAction.PAUSE)
            changeState(subscriptions, checkNotNull(identities["旧健身订阅"]), SubscriptionStateAction.STOP_AT_END)

            val before = projects.loadProjectWindow(1)
            val beforeSubscriptions = subscriptions.loadSubscriptionWindow(1)
            val zoneBefore = generation.settings.recordZone()
            val assetIds = before.items.mapNotNull { it.project.coverAssetId } +
                beforeSubscriptions.items.mapNotNull { it.subscription.coverAssetId }
            val coverHashesBefore = assetIds.associateWith { sha256(success(media.openAsset(it)).bytes) }
            val management = subscriptions.loadManagementWindow(false, 1)
            assertEquals(setOf(SubscriptionDisplayStatus.ACTIVE, SubscriptionDisplayStatus.PAUSED, SubscriptionDisplayStatus.EXPIRED),
                management.items.map { it.cost.displayStatus }.toSet())
            val music = checkNotNull(subscriptions.loadSubscriptionDetail(musicId))
            val musicHistory = subscriptions.loadRenewalHistoryWindow(musicId, 1)
            assertEquals(730L, music.cost.standardPurchasedDays)
            assertEquals(LocalDate.of(2026, 1, 1), music.currentRenewal?.effectiveFrom)
            val missingCover = music.copy(subscription = music.subscription.copy(coverAssetId = uuid()))
            val read = readHomeOverview(projects, subscriptions, clock.today())
            val state = LedgerUiState.Ready(read.projectCount, zoneBefore, projects, clock, subscriptions,
                read.subscriptionCount, mediaRepository = media, homePreview = read.preview)
            lateinit var projectModel: ProjectViewModel
            lateinit var subscriptionModel: SubscriptionViewModel
            lateinit var coverModel: CoverViewModel
            onMain {
                projectModel = retain("project", ProjectViewModel(projects, SavedStateHandle(), clock) { timestamp })
                subscriptionModel = retain("subscription", SubscriptionViewModel(subscriptions, SavedStateHandle(), clock) { timestamp })
                coverModel = retain("cover", CoverViewModel(media, SavedStateHandle()) { timestamp.plusSeconds(2) })
            }
            var dark by mutableStateOf(false)
            var display by mutableStateOf("navigation")
            compose.setContent {
                LithicTheme(darkTheme = dark) {
                    val view = LocalView.current
                    SideEffect {
                        val activity = checkNotNull(view.context.activity())
                        hostActivity = activity.componentName.flattenToShortString()
                        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                        WindowCompat.getInsetsController(activity.window, view).apply {
                            isAppearanceLightStatusBars = !dark
                            isAppearanceLightNavigationBars = !dark
                        }
                    }
                    if (display == "navigation") Surface {
                        LithicNavigation(state, projectViewModel = projectModel,
                            subscriptionViewModel = subscriptionModel, coverViewModel = coverModel)
                    } else Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                        when (display) {
                            "stale-list" -> SubscriptionListScreen(SubscriptionListUiState(management.items, management.totalCount,
                                loading = false, error = "暂时无法刷新，当前仍保留上次读取的订阅记录。"), {}, {}, {}, {}, {}, mediaRepository = media)
                            "read-error" -> SubscriptionListScreen(SubscriptionListUiState(loading = false,
                                error = "订阅暂时无法读取，尚未确认记录数量，请重试；这不表示没有记录。"), {}, {}, {}, {}, {}, mediaRepository = media)
                            else -> SubscriptionDetailScreen(SubscriptionDetailUiState(missingCover,
                                history = RenewalHistoryUiState(musicHistory.items, musicHistory.totalCount, loading = false),
                                loading = false), {}, {}, {}, {}, mediaRepository = media)
                        }
                    }
                }
            }
            for (theme in listOf("light", "dark")) {
                compose.runOnIdle { dark = theme == "dark"; display = "navigation" }
                compose.onNodeWithTag("home-screen").assertIsDisplayed()
                compose.onNodeWithTag("open-subscription-list").performScrollTo().performClick()
                awaitCover("subscription-$musicId-cover-image")
                compose.onNodeWithTag("subscription-list-screen").assertIsDisplayed()
                capture("subscription-list-$theme.png", "订阅服务列表：真实订阅中/暂停/到期分组", theme, generation)
                compose.onNodeWithTag("subscription-row-$musicId").performScrollTo().performClick()
                awaitCover("subscription-cover-image")
                compose.onNodeWithTag("subscription-detail-historical-average").assertIsDisplayed()
                capture("subscription-detail-$theme.png", "订阅详情：历史平均与预付未来周期", theme, generation)
                if (theme == "light") {
                    compose.onNodeWithTag("subscription-detail-content")
                        .performScrollToNode(hasTestTag("subscription-renewal-${future.id}"))
                    compose.onNodeWithTag("subscription-renewal-future-${future.id}").assertIsDisplayed()
                    capture("subscription-payment-history-light.png", "真实付款历史：未来周期尚未生效", theme, generation)
                    compose.onNodeWithTag("subscription-renewal-delete-${future.id}").performScrollTo().performClick()
                    compose.waitUntil(10_000) { subscriptionModel.state.value.stateEditor?.loading == false }
                    compose.onNodeWithTag("subscription-delete-payment-explanation").performScrollTo().assertIsDisplayed()
                    compose.onNodeWithTag("subscription-state-confirm").performScrollTo().assertIsDisplayed()
                    capture("subscription-delete-payment-light.png", "错误付款删除确认：未确认、未写入", theme, generation)
                    compose.onNodeWithTag("subscription-state-back").performScrollTo().performClick()
                    compose.onNodeWithTag("subscription-detail-back").performScrollTo().performClick()
                    compose.onNodeWithTag("subscription-list-back").performScrollTo().performClick()
                    compose.onNodeWithTag("tab-review").performClick()
                    compose.onNodeWithTag("review-subscription-history").performScrollTo().performClick()
                    compose.waitUntil(10_000) { !subscriptionModel.state.value.stoppedHistory.loading }
                    compose.onNodeWithTag("subscription-history-row-${identities["旧健身订阅"]}").assertIsDisplayed()
                    capture("subscription-history-light.png", "已停止订阅历史：真实付款及停止事实", theme, generation)
                    compose.onNodeWithTag("subscription-history-back").performScrollTo().performClick()
                    compose.onNodeWithTag("review-project-history").performScrollTo().performClick()
                    compose.waitUntil(10_000) { !projectModel.state.value.history.loading }
                    awaitCover("project-history-$endedId-cover-image")
                    compose.onNodeWithTag("project-history-row-$endedId").assertIsDisplayed()
                    capture("project-history-light.png", "已结束项目历史档案：真实领域结果", theme, generation)
                    compose.onNodeWithTag("project-history-back").performScrollTo().performClick()
                    compose.onNodeWithTag("tab-now").performClick()
                } else {
                    compose.onNodeWithTag("subscription-detail-back").performScrollTo().performClick()
                    compose.onNodeWithTag("subscription-list-back").performScrollTo().performClick()
                }
                compose.runOnIdle { display = "stale-list" }
                compose.onNodeWithTag("subscription-list-error").assertIsDisplayed()
                awaitCover("subscription-$musicId-cover-image")
                capture("subscription-stale-error-$theme.png", "读取失败：保留上次真实订阅记录", theme, generation,
                    "ISOLATED_PRESENTATION_HOST", "INJECTED_ERROR_WITH_REPOSITORY_ROWS")
                compose.runOnIdle { display = "read-error" }
                compose.onNodeWithTag("subscription-list-error").assertIsDisplayed()
                compose.onNodeWithTag("subscription-list-count").assertDoesNotExist()
                compose.onNodeWithText("暂无订阅服务").assertDoesNotExist()
                capture("subscription-read-error-$theme.png", "首次读取失败：数量未知，不宣称空账本", theme, generation,
                    "ISOLATED_PRESENTATION_HOST", "INJECTED_ERROR_WITH_NO_CONFIRMED_ROWS")
                compose.runOnIdle { display = "cover-error" }
                awaitCover("subscription-cover-error")
                compose.onNodeWithTag("subscription-cover-error").assertIsDisplayed()
                capture("subscription-cover-error-$theme.png", "有封面引用但无法读取：显示明确错误", theme, generation,
                    "ISOLATED_PRESENTATION_HOST", "UNPERSISTED_MISSING_COVER_REFERENCE_ON_REPOSITORY_DETAIL")
            }
            assertEquals("Opening/cancelling operations must not edit fixture facts", before, projects.loadProjectWindow(1))
            assertEquals(beforeSubscriptions, subscriptions.loadSubscriptionWindow(1))
            assertEquals(zoneBefore, generation.settings.recordZone())
            assetIds.forEach { assertEquals(coverHashesBefore[it], sha256(success(media.openAsset(it)).bytes)) }
            assertEquals(14, captures.length())
            val payments = JSONArray()
            beforeSubscriptions.items.forEach { detail -> payments.put(JSONObject()
                .put("name", detail.subscription.name).put("status", detail.cost.displayStatus.name)
                .put("historicalAverageExact", detail.cost.historicalAverage?.toPlainString())
                .put("standardPurchasedDays", detail.cost.standardPurchasedDays)
                .put("elapsedPaidDays", detail.cost.elapsedPaidDays)
                .put("currentRateExact", detail.cost.currentRate.toPlainString())
                .put("paymentCount", detail.renewals.size).put("stateFactCount", detail.stateChanges.size)) }
            val report = JSONObject().put("source", "ISOLATED_ANDROID_TEST_HOST")
                .put("productionRecordSource", false).put("fixtureUsesActiveLedger", false).put("fixtureWritesActiveLedger", false)
                .put("applicationOwnerNote", "Normal Application startup may open the default owner; fixture writes use only the isolated UUID root")
                .put("isolatedRootName", root.name).put("observationDay", clock.today().toString())
                .put("recordZone", zoneBefore.id).put("subscriptions", payments).put("captures", captures)
                .put("covers", "Original runtime Canvas illustrations reused from the earlier isolated evidence pattern; androidTest only")
                .put("costSource", "Production repositories and frozen domain engines; no hand-entered derived costs")
                .put("injectedDisplayOnlyCases", "Read-error states and missing cover UUID are never persisted; storage failure/corruption is not claimed")
                .put("fixtureRowsCoverBytesAndRecordZoneUnchangedAfterCaptures", true)
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

    private suspend fun attachSubscriptionCover(media: MediaRepository, id: String, illustration: Int) {
        val source = M10ImageSourceFixtures.source(cover(illustration)).also(sources::add)
        val prepared = success(media.prepareImport(source.uri, uuid()))
        val baseline = success(media.loadTarget(CoverTarget(CoverTargetType.SUBSCRIPTION, id)))
        success(media.commit(CoverCommand(uuid(), baseline, prepared.asset, timestamp.plusSeconds(1))))
    }

    private suspend fun changeState(repository: SubscriptionRepository, id: String, action: SubscriptionStateAction) {
        val expected = checkNotNull(repository.loadSubscriptionDetail(id)).facts()
        val operation = uuid()
        val value = if (action == SubscriptionStateAction.PAUSE) SubscriptionOperationState.PAUSED else SubscriptionOperationState.STOPPED
        val result = repository.setState(SetSubscriptionStateCommand(operation, expected,
            SubscriptionStateChangeEntity(uuid(), id, clock.today(), value, "隔离视觉验收的状态事实", null, operation), action))
        assertTrue("Fixture states must use the production lifecycle protocol: $result", result is SubscriptionLifecycleResult.Saved)
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

    private fun capture(fileName: String, page: String, theme: String, generation: ManagedGeneration,
        source: String = "ISOLATED_NAVIGATION_HOST", scenario: String = "REPOSITORY_FACTS") {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        compose.waitUntil(10_000) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString() == context.packageName
        }
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val output = output(fileName)
        output.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        captures.put(JSONObject().put("file", fileName).put("page", page).put("theme", theme)
            .put("source", source).put("scenario", scenario).put("actualHostActivity", hostActivity)
            .put("widthPixels", bitmap.width)
            .put("heightPixels", bitmap.height).put("sha256", sha256(output.readBytes()))
            .put("sdkInt", android.os.Build.VERSION.SDK_INT)
            .put("densityDpi", context.resources.displayMetrics.densityDpi)
            .put("widthDp", bitmap.width / context.resources.displayMetrics.density.toDouble())
            .put("fontScale", context.resources.configuration.fontScale.toDouble())
            .put("generationId", generation.id))
        bitmap.recycle()
    }

    private fun awaitCover(tag: String) = compose.waitUntil(10_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun output(name: String): File = File(context.noBackupFilesDir, "ui-c1-stage2-visual/$name").also {
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
