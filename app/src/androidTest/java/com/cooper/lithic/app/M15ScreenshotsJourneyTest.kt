package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.BackupExportResult
import com.cooper.lithic.data.backup.BackupExportService
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.backup.RestoreCoordinator
import com.cooper.lithic.data.backup.RestoreResult
import com.cooper.lithic.data.backup.RestoreSyntheticFixture
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.AddProjectMoneyEventCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.RankingRepository
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionDisplayStatus
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Actual LithicApp/Nav3 and storage owners, hosted by an isolated ComponentActivity with plain
 * Application. All records are synthetic. LocalDensity supplies a 360dp/200% test canvas; these
 * are neither MainActivity screenshots nor proof of a system font or density setting.
 */
@RunWith(AndroidJUnit4::class)
class M15ScreenshotsJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val initialDay = LocalDate.of(2026, 10, 21)
    private val day = AtomicReference(initialDay)
    private val clock = BusinessClock { day.get() }
    private val zone = ZoneId.of("Asia/Shanghai")
    private val models = ViewModelStore()
    private val showing = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private val runId = UUID.randomUUID().toString()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private val ready get() = holder.state.value as LedgerUiState.Ready

    @Before fun setUp(): Unit = runBlocking {
        assertEquals("Only the plain isolated test runner is allowed", Application::class.java,
            context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m15-ui-$runId").canonicalFile
        assertEquals(context.noBackupFilesDir.canonicalFile, root.parentFile)
        check(root.mkdir())
        val storage = File(root, "target-ledger").apply { check(mkdir()) }
        owner = AppContainer(DataGenerationStore(context, storage), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(zone)
        owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { storedZone ->
                assertEquals(zone, storedZone)
                clock
            }.also { models.put("m15-review", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("m15-test-viewport")) {
                val density = constraints.maxWidth / 360f
                val height = minOf(720f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, height).testTag("m15-test-canvas")) {
                            Surface(Modifier.fillMaxSize()) {
                                val ledger by holder.state.collectAsState()
                                if (showing.value) LithicApp(ledger, {}, onRetryLedger = { holder.retryRead() })
                            }
                        }
                    }
                }
            }
        }
        waitNode("home-screen")
    }

    @After fun tearDown(): Unit = runBlocking {
        compose.runOnIdle {
            showing.value = false
            models.clear()
            compose.activity.viewModelStore.clear()
        }
        compose.waitForIdle()
        if (::owner.isInitialized) owner.close()
        if (::root.isInitialized && root.exists()) {
            assertEquals(context.noBackupFilesDir.canonicalFile, root.canonicalFile.parentFile)
            assertEquals("m15-ui-$runId", root.name)
            check(root == root.canonicalFile)
            check(root.deleteRecursively()) { "Only this closed synthetic owner root may be removed" }
        }
    }

    @Test fun mixed_rank_navigation_write_and_day_refresh_with_360dp_font200(): Unit = runBlocking {
        val generation = owner.openStorage()
        val ids = insertReviewFacts(generation, "截图合成")
        openReview()
        row(ids.active.id)
        waitText("review-status-${ids.active.id}", "持有中")
        waitText("review-cost-label-${ids.active.id}", "当前日均成本")
        val activeDays = checkNotNull(ready.repository.loadProjectDetail(ids.active.id)).cost.heldDays
        waitText("review-time-${ids.active.id}", "持有 $activeDays 天")
        capture("01-project-mixed-companionship")

        // A ranked item opens its exact identity, preserving the regular unsaved-change guard.
        click("review-row-${ids.active.id}")
        waitText("project-detail-name", ids.active.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals("返回长期复盘")
        scrollClick("project-detail-content", "project-edit")
        waitNode("project-editor-screen")
        compose.onNodeWithTag("project-name").performScrollTo().performTextReplacement("不应保存的草稿")
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag("project-editor-back").performScrollTo().performClick()
        waitNode("discard-dialog")
        click("discard-confirm")
        waitText("project-detail-name", ids.active.name)
        scrollClick("project-detail-content", "project-detail-back")
        waitNode("review-ranks-screen")
        assertEquals(ids.active.name, checkNotNull(ready.repository.loadProjectDetail(ids.active.id)).project.name)

        rankChoice("PROJECT_COST")
        row(ids.ended.id)
        waitText("review-status-${ids.ended.id}", "已结束")
        waitText("review-cost-label-${ids.ended.id}", "已结束；结果截至今天")
        waitText("review-rate-${ids.ended.id}", "−")
        capture("02-project-negative-final-cost")
        val endedDays = checkNotNull(ready.repository.loadProjectDetail(ids.ended.id)).cost.heldDays
        val money = ProjectMoneyEventEntity(id(), ids.ended.id, MoneyDirection.RECOVERY, 1_000,
            initialDay, "结束后合成真实退款", ProjectMoneyEventRole.REGULAR, id())
        val appended = ready.repository.appendMoneyEvent(AddProjectMoneyEventCommand(money.operationId, money))
        assertTrue("Synthetic append failed: $appended", appended is MutationResult.Success)
        val updated = checkNotNull(ready.repository.loadProjectDetail(ids.ended.id))
        assertEquals(endedDays, updated.cost.heldDays)
        val expectedRate = AmountFormatter.formatDailyRate(updated.cost.dailyRate)
        row(ids.ended.id)
        waitText("review-rate-${ids.ended.id}", expectedRate)

        rankChoice("SUBSCRIPTION_TIME")
        row(ids.annual.id)
        waitText("review-time-${ids.annual.id}", "折算付费时长：365 天")
        waitText("review-cost-label-${ids.annual.id}", "历史平均日成本")
        waitText("review-rate-${ids.annual.id}", "10.00")
        capture("03-subscription-standard-time")
        click("review-row-${ids.annual.id}")
        waitText("subscription-detail-name", ids.annual.name)
        compose.onNodeWithTag("subscription-detail-back").assertTextEquals("返回长期复盘")
        val originalAnnual = checkNotNull(ready.subscriptionRepository.loadSubscriptionDetail(ids.annual.id))
        scrollClick("subscription-detail-content", "subscription-detail-pause")
        waitText("subscription-state-service", ids.annual.name)
        scrollClick("subscription-state-screen", "subscription-state-confirm")
        waitText("subscription-detail-status", "已暂停")
        compose.onNodeWithTag("subscription-detail-back").assertTextEquals("返回长期复盘")
        val pausedAnnual = checkNotNull(ready.subscriptionRepository.loadSubscriptionDetail(ids.annual.id))
        assertEquals(originalAnnual.renewals, pausedAnnual.renewals)
        assertEquals(SubscriptionDisplayStatus.PAUSED, pausedAnnual.cost.displayStatus)
        assertEquals(originalAnnual.cost.historicalAverage, pausedAnnual.cost.historicalAverage)
        assertEquals(0, pausedAnnual.cost.currentRate.signum())
        scrollClick("subscription-detail-content", "subscription-detail-back")
        waitNode("review-ranks-screen")
        row(ids.annual.id)
        waitText("review-rate-${ids.annual.id}", "10.00")

        // Return-to-foreground with a new business date refreshes the already opened review.
        rankChoice("PROJECT_TIME")
        row(ids.active.id)
        day.set(initialDay.plusDays(1))
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        row(ids.active.id)
        waitText("review-time-${ids.active.id}", "持有 ${activeDays + 1} 天")
        rankChoice("PROJECT_COST")
        row(ids.ended.id)
        waitText("review-time-${ids.ended.id}", "持有 $endedDays 天")

        // Browsing and changing a ranking never write facts; the earlier writes were explicit.
        val beforeBrowsing = generation.backupSnapshotSource().capture()
        reviewBack()
        capture("04-history-entry")
        scrollClick("review-screen", "review-project-history")
        waitNode("project-history-screen")
        scrollClick("project-history-content", "project-history-row-${ids.ended.id}")
        waitText("project-detail-name", ids.ended.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals("返回历史档案")
        click("project-detail-back")
        waitNode("project-history-screen")
        capture("05-project-history")
        scrollClick("project-history-content", "project-history-back")
        waitNode("review-screen")
        scrollClick("review-screen", "review-subscription-history")
        waitText("subscription-history-count", "已停止 1 条订阅")
        compose.onNodeWithTag("subscription-history-content").performScrollToNode(hasTestTag("subscription-history-row-${ids.stopped.id}"))
        compose.onNodeWithTag("subscription-history-row-${ids.stopped.id}").assertIsDisplayed()
        compose.onNodeWithTag("subscription-history-row-${ids.paused.id}").assertDoesNotExist()
        compose.onNodeWithTag("subscription-history-row-${ids.expired.id}").assertDoesNotExist()
        compose.onNodeWithTag("subscription-history-row-${ids.futureStop.id}").assertDoesNotExist()
        capture("06-stopped-subscription-history")
        scrollClick("subscription-history-content", "subscription-history-back")
        openReview(fromTab = false)

        compose.runOnIdle { fontScale.value = 2f }
        compose.waitForIdle()
        rankChoice("SUBSCRIPTION_TIME")
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-ranks-back"))
        compose.onNodeWithTag("review-ranks-back").assertIsDisplayed().assertIsEnabled()
        capture("07-review-360dp-font200-header")
        row(ids.annual.id)
        waitText("review-time-${ids.annual.id}", "折算付费时长：365 天")
        capture("08-review-360dp-font200-subscription")
        reviewBack()
        waitNode("review-screen")
        assertEquals(beforeBrowsing, generation.backupSnapshotSource().capture())
    }

    @Test fun restored_generation_replaces_open_review_and_rejects_old_reader(): Unit = runBlocking {
        val oldGeneration = owner.openStorage()
        val oldIds = insertReviewFacts(oldGeneration, "恢复前独有")
        val oldReader = RankingRepository(oldGeneration.database, oldGeneration.coordinator, clock)
        openReview()
        row(oldIds.active.id)
        waitText("review-name-${oldIds.active.id}", oldIds.active.name)
        val sourceRoot = File(root, "source-ledger").apply { check(mkdir()) }
        val source = AppContainer(DataGenerationStore(context, sourceRoot), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        try {
            source.start(zone)
            val sourceGeneration = source.openStorage()
            val pictures = RestoreSyntheticFixture.insert(sourceGeneration)
            val newIds = insertReviewFacts(sourceGeneration, "恢复后独有")
            val expected = sourceGeneration.backupSnapshotSource().capture()
            val expectedRanks = RankingRepository(sourceGeneration.database, sourceGeneration.coordinator, clock)
                .loadWindow(ReviewRanking.PROJECT_TIME)
            val archive = File(root, "review-round-trip.lithicbackup")
            assertTrue(BackupExportService(context, sourceGeneration).export(RestoreSyntheticFixture.target(archive)) is BackupExportResult.Success)
            val restorer = RestoreCoordinator(context, owner)
            val preview = restorer.prepare { archive.inputStream() }
            assertEquals(expected.data.recordCounts(), preview.backupCounts)
            assertEquals(RestoreResult.Success, restorer.confirm(preview.token))
            val newGeneration = owner.openStorage()
            assertNotEquals(oldGeneration.id, newGeneration.id)
            withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready && it.generationId == newGeneration.id } }
            waitNode("home-screen")
            openReview()
            row(newIds.active.id)
            waitText("review-name-${newIds.active.id}", newIds.active.name)
            compose.onNodeWithTag("review-row-${oldIds.active.id}").assertDoesNotExist()
            assertTrue("Revoked generation must reject late review reads",
                runCatching { oldReader.loadWindow(ReviewRanking.PROJECT_TIME) }.isFailure)
            val actualRanks = RankingRepository(newGeneration.database, newGeneration.coordinator, clock)
                .loadWindow(ReviewRanking.PROJECT_TIME)
            assertEquals(expectedRanks, actualRanks)
            assertEquals(expected, newGeneration.backupSnapshotSource().capture())
            pictures.forEach { (path, bytes) ->
                assertTrue(bytes.contentEquals(File(newGeneration.directory, path).readBytes()))
            }
        } finally { source.close() }
    }

    private data class ReviewIds(
        val active: LongTermProjectEntity,
        val ended: LongTermProjectEntity,
        val annual: SubscriptionEntity,
        val paused: SubscriptionEntity,
        val expired: SubscriptionEntity,
        val stopped: SubscriptionEntity,
        val futureStop: SubscriptionEntity,
    )

    private suspend fun insertReviewFacts(generation: ManagedGeneration, prefix: String): ReviewIds {
        val at = Instant.parse("2026-10-01T00:00:00Z")
        val active = LongTermProjectEntity(id(), "$prefix·陪伴八年的电脑", LithicDatabase.PROJECT_FALLBACK_ID,
            initialDay.minusYears(8), createdAt = at, updatedAt = at)
        val ended = LongTermProjectEntity(id(), "$prefix·已结束相机", LithicDatabase.PROJECT_FALLBACK_ID,
            initialDay.minusDays(99), initialDay.minusDays(9), "合成结束", createdAt = at, updatedAt = at)
        fun subscription(name: String) = SubscriptionEntity(id(), "$prefix·$name", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = at, updatedAt = at)
        val annual = subscription("年付工具")
        val paused = subscription("暂停影音")
        val expired = subscription("到期服务")
        val stopped = subscription("已停止服务")
        val futureStop = subscription("未来停止服务")
        generation.coordinator.withWrite {
            generation.database.withTransaction {
                val db = generation.database
                db.projectDao().insertProject(active)
                db.projectDao().insertProject(ended)
                db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), active.id, MoneyDirection.OUTLAY, 0,
                    active.startedOn, "真实0元合成案例", ProjectMoneyEventRole.INITIAL, id()))
                db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), ended.id, MoneyDirection.OUTLAY, 10_000,
                    ended.startedOn, null, ProjectMoneyEventRole.INITIAL, id()))
                db.projectDao().insertEvent(ProjectMoneyEventEntity(id(), ended.id, MoneyDirection.RECOVERY, 12_000,
                    initialDay, "结束后的真实收回", ProjectMoneyEventRole.REGULAR, id()))
                listOf(annual, paused, expired, stopped, futureStop).forEach { db.subscriptionDao().insertSubscription(it) }
                db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), annual.id, initialDay, initialDay,
                    initialDay.plusYears(1), initialDay.plusYears(1), 365_000, BillingCycle.YEARLY, 365, id()))
                listOf(paused, expired, stopped, futureStop).forEach { identity ->
                    val until = if (identity == expired) initialDay else initialDay.plusDays(20)
                    db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id(), identity.id, initialDay.minusDays(10),
                        initialDay.minusDays(10), until, until, 3_000, BillingCycle.MONTHLY, 30, id()))
                }
                db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), paused.id, initialDay,
                    SubscriptionOperationState.PAUSED, "合成暂停", operationId = id()))
                db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), stopped.id, initialDay,
                    SubscriptionOperationState.STOPPED, "合成停止", operationId = id()))
                db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(id(), futureStop.id, initialDay.plusDays(3),
                    SubscriptionOperationState.STOPPED, "未来才停止", operationId = id()))
            }
        }
        return ReviewIds(active, ended, annual, paused, expired, stopped, futureStop)
    }

    private fun openReview(fromTab: Boolean = true) {
        if (fromTab) { click("tab-review"); waitNode("review-screen") }
        scrollClick("review-screen", "review-long-term")
        waitNode("review-ranks-screen")
    }

    private fun reviewBack() = scrollClick("review-ranks-screen", "review-ranks-back")

    private fun rankChoice(kind: String) {
        if (kind != "SUBSCRIPTION_TIME" &&
            compose.onAllNodesWithTag("review-rank-PROJECT_TIME").fetchSemanticsNodes().isEmpty()) {
            scrollClick("review-ranks-screen", "review-type-project")
            waitNode("review-rank-PROJECT_TIME")
        }
        scrollClick("review-ranks-screen", "review-rank-$kind")
    }

    private fun row(id: String) {
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag("review-row-$id"))
        waitNode("review-row-$id")
        compose.onNodeWithTag("review-row-$id").assertIsDisplayed()
    }

    private fun scrollClick(container: String, tag: String) {
        compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag))
        click(tag)
    }

    private fun click(tag: String) {
        Espresso.closeSoftKeyboard()
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
    }

    private fun waitNode(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitText(tag: String, text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().any { node ->
                textOf(node).contains(text)
            }
        }
    }

    private fun textOf(node: SemanticsNode): String = buildString {
        if (node.config.contains(SemanticsProperties.Text)) {
            append(node.config[SemanticsProperties.Text].joinToString(" ") { it.text })
        }
        node.children.forEach { append(' '); append(textOf(it)) }
    }

    private fun capture(name: String) {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("m15-test-viewport").fetchSemanticsNode()
        val canvas = compose.onNodeWithTag("m15-test-canvas").fetchSemanticsNode()
        assertTrue("360dp canvas must fit the visible test viewport",
            canvas.positionInRoot.x >= viewport.positionInRoot.x - 0.5f &&
                canvas.positionInRoot.x + canvas.size.width <= viewport.positionInRoot.x + viewport.size.width + 0.5f)
        val bitmap = compose.onNodeWithTag("m15-test-canvas").captureToImage().asAndroidBitmap()
        val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalFile
        val directory = File(files, "m15-evidence")
        check(directory.canonicalFile.parentFile == files && directory.absoluteFile == directory.canonicalFile)
        check(directory.isDirectory || directory.mkdir())
        val output = File(directory, "$name-$runId.png")
        check(output.createNewFile())
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            stream.fd.sync()
        }
        check(output.length() > 0)
        println("M15_COMPONENT_ACTIVITY_SCREENSHOT=${output.absolutePath}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("M15_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath)
        })
    }

    private fun id(): String = UUID.randomUUID().toString()
}
