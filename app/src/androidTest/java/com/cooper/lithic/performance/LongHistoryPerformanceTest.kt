package com.cooper.lithic.performance

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.app.AppContainer
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LedgerUiStateHolder
import com.cooper.lithic.app.LithicApp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.review.TotalCostTrendScreen
import com.cooper.lithic.feature.review.TotalCostTrendUiState
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Room 5 / production repositories and engines. This is a Debug emulator workload,
 * not a Release benchmark, physical-device guarantee, or system-level accessibility result.
 * The full 2,000 identities and 30,000 payment facts are never reduced to the visible page. */
@RunWith(AndroidJUnit4::class)
class LongHistoryPerformanceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 23)
    private val first = today.minusYears(20)
    private val clock = FixedBusinessClock(today)
    private val time = Instant.parse("2026-10-23T04:00:00Z")
    private val runId = UUID.randomUUID().toString()
    private val modelStore = ViewModelStore()
    private val chartScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var chartSelection: Job? = null
    private var chartSelectionTicket = 0L
    private val show = mutableStateOf(false)
    private val chartState = mutableStateOf<TotalCostTrendUiState?>(null)
    private val evidence = JSONObject().put("module", "M22-T03").put("runId", runId)
        .put("seed", SEED).put("observationDate", today).put("firstBusinessDate", first)
        .put("source", "new synthetic Room5 generation; deterministic name UUIDs; no personal data")
        .put("performanceScope", "Debug API36 emulator; sampled PSS/RSS are observations, not a memory hard limit")
    private val phases = JSONArray()
    private lateinit var root: File
    private lateinit var evidenceRoot: File
    private lateinit var owner: AppContainer
    private lateinit var generation: ManagedGeneration
    private lateinit var holder: LedgerUiStateHolder
    private var currentPhase = "before"
    private var success = false
    private var ownerClosed = false
    private val pulseRunning = AtomicBoolean(false)
    private val pulseCount = AtomicLong()
    private val maxPulseGapMillis = AtomicLong()
    private val handler = Handler(Looper.getMainLooper())
    private var pulseLast = 0L
    private val pulse = object : Runnable {
        override fun run() {
            if (!pulseRunning.get()) return
            val now = SystemClock.elapsedRealtime()
            if (pulseLast != 0L) maxPulseGapMillis.updateAndGet { old -> maxOf(old, now - pulseLast) }
            pulseLast = now; pulseCount.incrementAndGet(); handler.postDelayed(this, 100)
        }
    }

    @Before fun before(): Unit = runBlocking {
        assertEquals("Use the plain Application isolated runner", Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m22-long-history-$runId").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        File(root, "owner.txt").writeText("M22:$runId:LongHistoryPerformanceTest\n")
        evidenceRoot = File(context.filesDir, "m22-long-history-evidence-$runId").canonicalFile
        check(evidenceRoot.parentFile == context.filesDir.canonicalFile && evidenceRoot.mkdir())
        File(evidenceRoot, "owner.txt").writeText("M22:$runId:LongHistoryPerformanceTest evidence\n")
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai")); generation = owner.openStorage()
        evidence.put("generationId", generation.id).put("pid", Process.myPid()).put("uid", Process.myUid())
            .put("systemFontScale", context.resources.configuration.fontScale)
            .put("densityDpi", context.resources.displayMetrics.densityDpi)
            .put("screenWidthDp", context.resources.configuration.screenWidthDp)
            .put("api", android.os.Build.VERSION.SDK_INT)
        pulseRunning.set(true); handler.post(pulse)
        compose.setContent {
            LithicTheme {
                Surface(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize().testTag("m22-long-canvas")) {
                        val chart = chartState.value
                        if (chart != null) {
                            TotalCostTrendScreen(chart, select@ { date ->
                                val captured = chartState.value ?: return@select
                                val snapshot = checkNotNull(captured.snapshot)
                                val timeline = checkNotNull(captured.timeline)
                                val ticket = ++chartSelectionTicket
                                chartSelection?.cancel()
                                chartState.value = captured.copy(selectedDate = date, selectedPoint = null, events = emptyList())
                                chartSelection = chartScope.launch {
                                    try {
                                        val point = withContext(Dispatchers.Default) { CostTimelineEngine.point(snapshot, date) }
                                        val events = timeline.events.filter { it.date == date }
                                        val current = chartState.value
                                        if (ticket == chartSelectionTicket && current != null && current.snapshot === snapshot &&
                                            current.timeline === timeline && current.selectedDate == date) {
                                            chartState.value = current.copy(selectedPoint = point, events = events)
                                        }
                                    } catch (cancelled: CancellationException) { throw cancelled }
                                    catch (failure: Exception) {
                                        evidence.put("chartSelectionFailure", failure.javaClass.name)
                                        if (ticket == chartSelectionTicket) chartState.value = chartState.value?.copy(error = "合成宿主选日计算失败")
                                    }
                                }
                            }, { chartState.value = chart.copy(showBreakdown = !chart.showBreakdown) }, {}, { _, _ -> },
                                { chartSelectionTicket++; chartSelection?.cancel(); chartState.value = null })
                        } else if (show.value) {
                            val state by holder.state.collectAsState()
                            LithicApp(state, {}, onRetryLedger = { holder.retryRead() })
                        }
                    }
                }
            }
        }
    }

    @After fun after(): Unit = runBlocking {
        pulseRunning.set(false); handler.removeCallbacks(pulse)
        withContext(Dispatchers.Main) {
            chartScope.cancel()
            show.value = false; chartState.value = null
            modelStore.clear(); compose.activity.viewModelStore.clear()
        }
        compose.waitForIdle()
        var closeFailure: Throwable? = null
        if (::owner.isInitialized) {
            try { owner.close(); ownerClosed = !::generation.isInitialized || !generation.database.isOpen }
            catch (failure: Throwable) { closeFailure = failure; evidence.put("ownerCloseFailure", failure.javaClass.name) }
        }
        evidence.put("result", if (success && ownerClosed) "PASS" else "FAIL_OR_INCOMPLETE")
            .put("lastPhase", currentPhase).put("phases", phases)
            .put("mainThreadPulseCount", pulseCount.get()).put("maximumObservedMainThreadPulseGapMs", maxPulseGapMillis.get())
            .put("mainThreadPulseScope", "100ms Handler probes; observed scheduling gaps, not frame-jank or ANR-stack proof")
            .put("frameJank", "NOT_CAPTURED").put("ownerClosed", ownerClosed)
            .put("syntheticRoot", if (::root.isInitialized) root.path else "NOT_CREATED")
            .put("syntheticRootPreserved", !success || !ownerClosed)
            .put("lastProcessMemory", memory()).put("realTalkBack", "NOT_RUN")
        if (::evidenceRoot.isInitialized) {
            val output = File(evidenceRoot, "result.json"); check(output.createNewFile())
            FileOutputStream(output).use { it.write(evidence.toString(2).toByteArray()); it.fd.sync() }
            InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
                putString("M22_LONG_HISTORY_RESULT", output.absolutePath)
            })
        }
        // Only this exclusively created, marked M22 root; historical caches / .lck files elsewhere are untouched.
        if (::root.isInitialized && root.exists() && success && ownerClosed) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile)
            check(File(root, "owner.txt").readText() == "M22:$runId:LongHistoryPerformanceTest\n")
            check(root.walkTopDown().all { it.absoluteFile == it.canonicalFile })
            check(root.deleteRecursively())
        }
        closeFailure?.let { throw it }
    }

    @Test fun full_twenty_year_dataset_reads_exact_global_facts_and_remains_responsive(): Unit = runBlocking {
        try {
            timed("prepare-room-facts") { seed() }
            val db = generation.database
            val counts = timed("verify-real-table-counts") {
                val counts = JSONObject()
                for (table in listOf("long_term_projects", "project_money_events", "subscriptions", "subscription_renewals", "subscription_state_changes")) {
                    val count = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM $table").use { it.moveToFirst(); it.getLong(0) }
                    counts.put(table, count)
                }
                assertEquals(1_000L, counts.getLong("long_term_projects"))
                assertEquals(20_000L, counts.getLong("project_money_events"))
                assertEquals(1_000L, counts.getLong("subscriptions"))
                assertEquals(10_000L, counts.getLong("subscription_renewals"))
                db.openHelper.readableDatabase.query("SELECT projectId FROM project_money_events WHERE role='INITIAL' GROUP BY projectId HAVING COUNT(*)!=1").use { assertFalse(it.moveToFirst()) }
                counts
            }
            evidence.put("actualCounts", counts)
            val before = timed("capture-facts-before-browsing") { generation.backupSnapshotSource().capture() }
            val reports = ReportRepository(db, generation.coordinator, clock)
            val home = timed("home-read-and-frozen-engine") { reports.loadOverview(today) }
            assertEquals(1_000L, home.projectCount); assertEquals(1_000L, home.subscriptionCount)
            assertTrue(home.activeProjectCount in 1L..999L); assertTrue(home.activeSubscriptionCount in 1L..999L)
            val projects = ProjectRepository(db, generation.coordinator, clock)
            val subscriptions = SubscriptionRepository(db, generation.coordinator, clock)
            timed("project-20-pages-two-pass-stable") {
                suspend fun read(): List<String> {
                    val ids = mutableListOf<String>(); var cursor: ProjectCursor? = null
                    do { val page = projects.loadProjectPage(cursor, 50, today); assertEquals(1_000L, page.totalCount)
                        ids += page.items.map { it.project.id }; cursor = page.nextCursor
                    } while (cursor != null)
                    assertEquals(1_000, ids.size); assertEquals(1_000, ids.toSet().size); return ids
                }
                assertEquals(read(), read())
            }
            timed("subscription-20-pages-two-pass-stable") {
                suspend fun read(): List<String> {
                    val ids = mutableListOf<String>(); var cursor: SubscriptionCursor? = null
                    do { val page = subscriptions.loadSubscriptionPage(cursor, 50, today); assertEquals(1_000L, page.totalCount)
                        ids += page.items.map { it.subscription.id }; cursor = page.nextCursor
                    } while (cursor != null)
                    assertEquals(1_000, ids.size); assertEquals(1_000, ids.toSet().size); return ids
                }
                assertEquals(read(), read())
            }
            timed("search-40-pages-no-omissions") {
                val search = SearchRepository(db, generation.coordinator, clock)
                var cursor: SearchCursor? = null; val ids = mutableSetOf<String>(); var rows = 0
                do { val page = search.searchNames("M22合成", 50, cursor, today); assertEquals(2_000L, page.totalCount)
                    for (row in page.items) { assertTrue(ids.add("${row.type}:${row.id}")); rows++ }; cursor = page.nextCursor
                } while (cursor != null)
                assertEquals(2_000, rows)
            }
            val snapshot = timed("complete-ledger-read") { reports.loadLedgerSnapshot(today) }
            assertEquals(2_000, snapshot.recordIdentities.size); assertEquals(30_000, checkNotNull(snapshot.financialFacts).size)
            assertEquals(20_000, snapshot.overview.projects.sumOf { it.snapshot.moneyEvents.size })
            assertEquals(10_000, snapshot.overview.subscriptions.sumOf { it.snapshot.renewals.size })
            val ranking = RankingRepository(db, generation.coordinator, clock)
            timed("three-global-rankings-before-display-window") {
                for (order in ReviewRanking.entries) {
                    val firstPage = ranking.loadWindow(order, 1, today)
                    val all = ranking.loadWindow(order, 20, today)
                    assertEquals(1_000L, all.totalCount); assertEquals(1_000, all.items.size)
                    assertEquals(firstPage.items, all.items.take(50)); assertEquals(1_000, all.items.map { it.id }.toSet().size)
                }
            }
            timed("category-and-all-recorded-annual-years") {
                val categories = CategoryReportEngine.evaluate(snapshot, today)
                val total = categories.fold(java.math.BigDecimal.ZERO) { sum, category -> sum + category.currentRate }
                assertEquals(0, total.compareTo(home.totalRate))
                val years = AnnualReportEngine.years(snapshot, today)
                assertEquals(2006, years.last()); assertEquals(2026, years.first())
                val annual = years.map { AnnualReportEngine.evaluate(snapshot, it, today) }
                assertEquals(20_000L, annual.sumOf { it.amounts.projectFactCount })
                assertEquals(10_000L, annual.sumOf { it.amounts.subscriptionPaymentCount })
            }
            timed("single-project-and-subscription-chart-read") {
                val reader = ItemTimelineRepository(projects, subscriptions, clock)
                val project = checkNotNull(reader.loadProject(id("project", 0), today))
                val subscription = checkNotNull(reader.loadSubscription(id("subscription", 0), today))
                assertTrue(project.timeline.holdingPoints.isNotEmpty())
                assertTrue(subscription.timeline.points.isNotEmpty())
            }
            val cancellationProbes = AtomicInteger()
            val cancellationStart = SystemClock.elapsedRealtime()
            val cancelled = async(Dispatchers.Default) {
                val coroutine = currentCoroutineContext()
                TimelineSampler.sample(snapshot, today, cancellationCheck = {
                    cancellationProbes.incrementAndGet(); coroutine.ensureActive()
                })
            }
            // Preparation also checks individual facts. The probe count only establishes
            // cancellation during full-size preparation or evaluation, not a day-scan position.
            // The separate complete sample below must still evaluate every calendar day.
            withTimeout(5_000) { while (cancellationProbes.get() < 10_000 && !cancelled.isCompleted) delay(1) }
            cancelled.cancelAndJoin()
            assertTrue(cancelled.isCancelled)
            evidence.put("cancelledOldComputationMs", SystemClock.elapsedRealtime() - cancellationStart)
                .put("cancellationProbeCount", cancellationProbes.get())
                .put("cancellationScope", "Cooperative cancellation during full-size preparation or evaluation; probe count is not calendar progress or a universal worst-case bound")
            val timeline = timed("total-trend-7306-day-full-computation-budget-120s") {
                withTimeout(120_000) {
                    withContext(Dispatchers.Default) {
                        val coroutine = currentCoroutineContext()
                        TimelineSampler.sample(snapshot, today, cancellationCheck = { coroutine.ensureActive() })
                    }
                }
            }
            assertEquals(first, timeline.from)
            assertEquals(ChronoUnit.DAYS.between(first, today) + 1, timeline.evaluatedDayCount)
            val point = timed("arbitrary-unshown-exact-date-and-same-day-home") {
                val exact = CostTimelineEngine.point(snapshot, today)
                assertEquals(0, home.totalRate.compareTo(exact.totalRate))
                assertEquals(0, home.projectRate.compareTo(exact.projectRate))
                assertEquals(0, home.subscriptionRate.compareTo(exact.subscriptionRate))
                val arbitrary = today.minusDays(1_473)
                val original = OverviewEngine.evaluate(snapshot.overview, arbitrary)
                assertEquals(0, original.totalRate.compareTo(CostTimelineEngine.point(snapshot, arbitrary).totalRate))
                exact
            }
            evidence.put("fullEvaluatedDayCount", timeline.evaluatedDayCount).put("retainedDisplayPointCount", timeline.points.size)
                .put("totalRelatedEvents", timeline.events.size).put("homeTotalExact", home.totalRate.toPlainString())

            timed("real-lithic-app-home-list-detail-return-render") {
                withContext(Dispatchers.Main) {
                    holder = LedgerUiStateHolder(owner.storageState) { clock }.also { modelStore.put("m22-long", it) }
                    show.value = true
                }
                withTimeout(20_000) { holder.state.first { it is LedgerUiState.Ready && it.overview != null } }
                waitNode("home-screen")
                compose.onNodeWithTag("home-screen").performScrollToNode(hasTestTag("open-project-list"))
                compose.onNodeWithTag("open-project-list").performClick(); waitNode("project-list-screen")
                val row = projects.loadCurrentProjectWindow(1, today).items.first()
                waitNode("project-row-${row.project.id}")
                compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-row-${row.project.id}"))
                capture("long-project-list")
                compose.onNodeWithTag("project-row-${row.project.id}").performClick(); waitNode("project-detail-screen")
                compose.onNodeWithTag("project-detail-content").performScrollToIndex(0)
                waitNode("project-detail-name")
                compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-name"))
                compose.onNodeWithTag("project-detail-name").assertTextEquals(row.project.name)
                compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-back"))
                compose.onNodeWithTag("project-detail-back").performClick(); waitNode("project-list-screen")
                compose.onNodeWithTag("project-list-content").performScrollToNode(hasTestTag("project-list-back"))
                compose.onNodeWithTag("project-list-back").performClick(); waitNode("home-screen")
            }
            timed("production-total-trend-component-render-and-capture") {
                compose.runOnIdle { chartState.value = TotalCostTrendUiState(snapshot, timeline, today, point,
                    timeline.events.filter { it.date == today }, showBreakdown = true) }
                waitNode("total-cost-trend-screen")
                compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-canvas"))
                compose.onNodeWithTag("total-trend-canvas").assertIsDisplayed(); capture("twenty-year-total-trend")
            }
            timed("production-trend-exact-date-control-background-query") {
                val requested = today.minusYears(10)
                compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-date-input"))
                compose.onNodeWithTag("total-trend-date-input").performTextReplacement(requested.toString())
                compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-date-apply"))
                compose.onNodeWithTag("total-trend-date-apply").performClick()
                compose.waitUntil(20_000) { chartState.value?.selectedPoint?.date == requested }
                val original = withContext(Dispatchers.Default) { OverviewEngine.evaluate(snapshot.overview, requested) }
                compose.runOnIdle {
                    val selected = checkNotNull(chartState.value!!.selectedPoint)
                    assertEquals(requested, selected.date)
                    assertEquals(0, original.totalRate.compareTo(selected.totalRate))
                    assertEquals(0, original.projectRate.compareTo(selected.projectRate))
                    assertEquals(0, original.subscriptionRate.compareTo(selected.subscriptionRate))
                    assertTrue(chartState.value!!.events.all { it.date == requested })
                }
                compose.onNodeWithTag("total-cost-trend-screen").performScrollToNode(hasTestTag("total-trend-back"))
                compose.onNodeWithTag("total-trend-back").assertIsDisplayed().performClick(); waitNode("home-screen")
            }
            val after = timed("capture-facts-after-browsing") { generation.backupSnapshotSource().capture() }
            assertEquals("Read, ranking, chart, cancellation and navigation may not mutate any business/settings fact", before, after)
            assertTrue("Independent UI heartbeat must actually run during the workload", pulseCount.get() > 5)
            // An observed >2s main-thread stall is a failed responsiveness gate, not hidden in averages.
            assertTrue("Observed main-thread scheduling gap was ${maxPulseGapMillis.get()}ms", maxPulseGapMillis.get() < 2_000)
            evidence.put("businessFactsUnchanged", true).put("screenshots", "isolated ComponentActivity; real LithicApp list and production trend component; inherited system font")
            success = true
        } catch (failure: Throwable) {
            evidence.put("failureType", failure.javaClass.name).put("failureMessage", failure.message ?: "")
            throw failure
        }
    }

    /** Direct insert is setup only, within the same owner/coordinator transaction. Every read
     * under measurement is the production repository / frozen engine, not a test reimplementation. */
    private suspend fun seed() = withContext(Dispatchers.IO) {
        generation.coordinator.withWrite { generation.database.withTransaction {
            val db = generation.database
            repeat(4) { categoryIndex ->
                for (type in CategoryType.entries) {
                    val name = "M22合成${type.name}类别$categoryIndex"
                    db.categoryDao().insert(CategoryEntity(id("category-${type.name}", categoryIndex), type,
                        name, CategoryNames.normalized(name), false))
                }
            }
            repeat(1_000) { index ->
                val start = first.plusDays((index.toLong() * 37L) % 2_000L)
                val ended = if (index % 3 == 1) today.minusYears(5) else null
                val projectId = id("project", index)
                db.projectDao().insertProject(LongTermProjectEntity(projectId, "M22合成长项目${index.toString().padStart(4, '0')}",
                    id("category-${CategoryType.PROJECT.name}", index % 4), start, ended, ended?.let { "合成已结束" }, createdAt = time, updatedAt = time))
                val days = ChronoUnit.DAYS.between(start, today)
                repeat(20) { eventIndex ->
                    val recovery = eventIndex > 0 && eventIndex % 4 == 0
                    val cents = when {
                        eventIndex == 0 && index % 10 == 0 -> 0L
                        recovery && index % 10 == 0 -> 200_000L + index
                        eventIndex == 0 -> 100_000L + index
                        else -> 100L + index + eventIndex
                    }
                    db.projectDao().insertEvent(ProjectMoneyEventEntity(id("event-$index", eventIndex), projectId,
                        if (recovery) MoneyDirection.RECOVERY else MoneyDirection.OUTLAY, cents,
                        start.plusDays(days * eventIndex / 19L), "M22合成真实资金事件$eventIndex",
                        if (eventIndex == 0) ProjectMoneyEventRole.INITIAL else ProjectMoneyEventRole.REGULAR,
                        id("event-operation-$index", eventIndex)))
                }
                val subscriptionId = id("subscription", index)
                db.subscriptionDao().insertSubscription(SubscriptionEntity(subscriptionId,
                    "M22合成订阅${index.toString().padStart(4, '0')}", id("category-${CategoryType.SUBSCRIPTION.name}", index % 4),
                    createdAt = time, updatedAt = time))
                repeat(10) { renewalIndex ->
                    val from = when {
                        renewalIndex == 9 && index % 5 in 0..2 -> today.minusDays(9)
                        renewalIndex == 9 && index % 5 == 4 -> today.plusDays(9)
                        else -> first.plusYears(renewalIndex * 2L)
                    }
                    val paidOn = minOf(from, today)
                    val until = from.plusYears(1)
                    db.subscriptionDao().insertRenewal(SubscriptionRenewalEntity(id("renewal-$index", renewalIndex),
                        subscriptionId, paidOn, from, until, until, 36_500L + index,
                        BillingCycle.YEARLY, 365, id("renewal-operation-$index", renewalIndex)))
                }
                // Disjoint paid years leave real free gaps. Pause and stop are independent saved facts.
                if (index % 5 in 1..2) db.subscriptionDao().insertStateChange(SubscriptionStateChangeEntity(
                    id("state", index), subscriptionId, today.minusDays(3),
                    if (index % 5 == 1) SubscriptionOperationState.PAUSED else SubscriptionOperationState.STOPPED,
                    "M22合成状态", operationId = id("state-operation", index)))
            }
        } }
        evidence.put("generationRules", "For project i: start=2006-10-23+(i*37 mod2000) days,20 evenly spaced events through cutoff; every4th regular event recovery; every10th net negative; i mod3=1 ended5yr ago, later settlements retained. For subscription i:10 disjoint yearly payments with starts normally spaced2yr apart (about1yr uncovered gaps); last period ACTIVE/PAUSED/STOPPED/EXPIRED/prepaid by i mod5; paidOn<=cutoff; no overlap.")
    }

    private suspend fun <T> timed(name: String, body: suspend () -> T): T {
        currentPhase = name
        val wall = SystemClock.elapsedRealtime(); val cpu = Process.getElapsedCpuTime()
        val before = memory()
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M22_PHASE", name) })
        return try { body() } finally {
            phases.put(JSONObject().put("name", name).put("wallMs", SystemClock.elapsedRealtime() - wall)
                .put("processCpuMs", Process.getElapsedCpuTime() - cpu).put("beforeMemory", before).put("afterMemory", memory()))
        }
    }

    private fun memory(): JSONObject {
        val memory = Debug.MemoryInfo(); Debug.getMemoryInfo(memory)
        val proc = File("/proc/self/status").readLines().filter { it.startsWith("VmRSS:") || it.startsWith("VmHWM:") }
        return JSONObject().put("sampledPssKiB", memory.totalPss).put("procStatus", JSONArray(proc))
            .put("javaHeapUsedBytes", Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
            .put("scope", "PSS/RSS at phase boundaries only; VmHWM is whole test-process lifetime, not exclusive workload peak")
    }

    private fun waitNode(tag: String) = compose.waitUntil(20_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun capture(name: String) {
        compose.waitForIdle()
        val output = File(evidenceRoot, "$name.png"); check(output.createNewFile())
        val image = compose.onNodeWithTag("m22-long-canvas").captureToImage().asAndroidBitmap()
        FileOutputStream(output).use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        val sha = MessageDigest.getInstance("SHA-256").digest(output.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
        evidence.put("screenshot-$name", JSONObject().put("path", output.absolutePath).put("sha256", sha)
            .put("width", image.width).put("height", image.height).put("host", "isolated ComponentActivity, not MainActivity"))
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M22_LONG_HISTORY_SCREENSHOT", output.absolutePath) })
    }

    private fun id(kind: String, index: Int): String = UUID.nameUUIDFromBytes("M22:$SEED:$kind:$index".toByteArray()).toString()
    private companion object { const val SEED = 20_261_023L }
}
