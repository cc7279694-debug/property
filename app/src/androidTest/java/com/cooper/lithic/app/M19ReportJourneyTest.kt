package com.cooper.lithic.app

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.AmountFormatter
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.*
import com.cooper.lithic.data.repository.*
import com.cooper.lithic.domain.report.*
import com.cooper.lithic.domain.subscription.BillingCycle
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Actual Room and LithicApp/Nav3 in a disposable plain Application / ComponentActivity.
 * LocalDensity 360dp/200% does not establish system-font, MainActivity or TalkBack coverage. */
@RunWith(AndroidJUnit4::class)
class M19ReportJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 10, 21)
    private val clock = FixedBusinessClock(today)
    private val timestamp = Instant.parse("2026-10-21T04:00:00Z")
    private val runId = UUID.randomUUID().toString()
    private val showing = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private val models = ViewModelStore()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private val ready get() = holder.state.value as LedgerUiState.Ready

    @Before fun before(): Unit = runBlocking {
        assertEquals(Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m19-ui-$runId").canonicalFile
        check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.mkdir())
        owner = AppContainer(DataGenerationStore(context, File(root, "ledger")), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(ZoneId.of("Asia/Shanghai")); owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { clock }.also { models.put("m19-ledger", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = constraints.maxWidth / 360f
                val height = minOf(720f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, height).testTag("m19-canvas")) {
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

    @After fun after(): Unit = runBlocking {
        compose.runOnIdle { showing.value = false; models.clear(); compose.activity.viewModelStore.clear() }
        compose.waitForIdle()
        if (::owner.isInitialized) owner.close()
        if (::root.isInitialized && root.exists()) {
            check(root.parentFile == context.noBackupFilesDir.canonicalFile && root.name == "m19-ui-$runId")
            check(root.deleteRecursively())
        }
    }

    @Test fun real_category_and_annual_reports_keep_typed_money_navigation_guards_and_readonly_facts(): Unit = runBlocking {
        val generation = owner.openStorage()
        val categoryId = id()
        assertTrue(ready.categoryRepository!!.create(CategoryType.PROJECT, "合成设备", categoryId) is CategoryMutationResult.Applied)
        val computer = seed("合成长期电脑", LocalDate.of(2024, 2, 1), 800_000, categoryId)
        append(computer.id, MoneyDirection.OUTLAY, 80_000, LocalDate.of(2026, 4, 1), "合成维修")
        append(computer.id, MoneyDirection.RECOVERY, 1_000_000, LocalDate.of(2025, 7, 1), "合成真实收回")
        seed("合成零元记录", LocalDate.of(2025, 2, 1), 0, categoryId)
        val old = seed("合成折抵旧机", LocalDate.of(2024, 1, 1), 900_000, categoryId)
        val oldDetail = checkNotNull(ready.repository.loadProjectDetail(old.id))
        val operation = id(); val revision = timestamp.plusSeconds(1)
        val next = LongTermProjectEntity(TradeInIdentities.newProjectId(operation), "合成折抵新机", categoryId,
            today, createdAt = revision, updatedAt = revision)
        assertTrue(ready.replacementRepository!!.recordTradeIn(TradeInCommand(operation, old, oldDetail.events,
            emptyList(), next, today, 500_000, 300_000, true, revision)) is ReplacementMutationResult.TradeInRecorded)
        val service = SubscriptionEntity(id(), "合成跨年服务", LithicDatabase.SUBSCRIPTION_FALLBACK_ID,
            createdAt = timestamp, updatedAt = timestamp)
        val paymentId = id()
        val payment = SubscriptionRenewalEntity(id(), service.id, LocalDate.of(2025, 12, 20),
            LocalDate.of(2026, 1, 1), LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 1),
            36_500, BillingCycle.YEARLY, 365, paymentId)
        assertTrue(ready.subscriptionRepository.createWithRenewal(CreateSubscriptionCommand(paymentId, service, payment)) is SubscriptionMutationResult.Success)
        val snapshot = ready.totalCostTimelineRepository!!.loadSnapshot(today)
        val before = generation.backupSnapshotSource().capture()
        val category = CategoryReportEngine.evaluate(snapshot, today).single { it.category.id == categoryId }
        val overview = OverviewEngine.evaluate(snapshot.overview, today)
        assertEquals(0, category.currentRate.compareTo(overview.projectCategories.single { it.categoryId == categoryId }.dailyRate))
        val annual2026 = AnnualReportEngine.evaluate(snapshot, 2026, today)
        assertEquals(880_000L, annual2026.amounts.projectOutlayCents)
        assertEquals(300_000L, annual2026.amounts.projectRecoveryCents)
        assertEquals(580_000L, annual2026.amounts.projectNetCents)
        assertEquals(0L, annual2026.amounts.subscriptionPaidCents)
        assertTrue(annual2026.amounts.containsNonCash)
        val annual2025 = AnnualReportEngine.evaluate(snapshot, 2025, today)
        assertEquals(36_500L, annual2025.amounts.subscriptionPaidCents)
        assertEquals(-1_000_000L, annual2025.amounts.projectNetCents)

        // Entry is the actual home category identity, not a generic unrelated review route.
        val homeCategory = "home-project-category-open-$categoryId"
        waitNode(homeCategory)
        compose.onNodeWithTag(homeCategory).performScrollTo().performClick()
        waitNode(CATEGORY)
        scrollTo(CATEGORY, "category-analysis-rate")
        waitText("category-analysis-rate", AmountFormatter.formatDailyRate(category.currentRate))
        capture("01-category-current-and-history")
        scrollTo(CATEGORY, "category-analysis-year-2025")
        capture("02-category-annual-trend-and-record-entry")
        scrollClick(CATEGORY, "category-analysis-open-${computer.id}")
        waitText("project-detail-name", computer.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals("返回类别分析")
        click("project-detail-back"); waitNode(CATEGORY)
        scrollTo(CATEGORY, "category-analysis-name")
        waitText("category-analysis-name", "合成设备")
        scrollClick(CATEGORY, "category-analysis-back"); waitNode("review-screen")
        scrollClick("review-screen", "review-annual")
        waitNode(ANNUAL); waitText("annual-selected-year", "2026")
        scrollTo(ANNUAL, "annual-outlay")
        waitText("annual-outlay", "8800.00")
        capture("03-annual-project-outlay-recovery-net")
        scrollTo(ANNUAL, "annual-net")
        waitText("annual-net", "5800.00")
        scrollTo(ANNUAL, "annual-noncash")
        compose.onNodeWithTag("annual-noncash").assertTextContains("含非现金折抵", substring = true)
        capture("04-noncash-trade-in-and-subscription-separate")
        scrollClick(ANNUAL, "annual-open-${computer.id}")
        waitText("project-detail-name", computer.name)
        compose.onNodeWithTag("project-detail-back").assertTextEquals("返回年度回顾")
        scrollClick("project-detail-content", "project-edit")
        waitNode("project-editor-screen")
        compose.onNodeWithTag("project-name").performTextReplacement("未保存年度来源草稿")
        click("tab-settings"); waitNode("discard-dialog"); click("discard-keep")
        compose.onNodeWithTag("project-name").assertTextContains("未保存年度来源草稿", substring = true)
        click("project-editor-back"); waitNode("discard-dialog"); click("discard-confirm")
        waitText("project-detail-name", computer.name); click("project-detail-back"); waitNode(ANNUAL)

        selectYear(2025)
        scrollTo(ANNUAL, "annual-net"); waitText("annual-net", "10000.00")
        scrollTo(ANNUAL, "annual-paid"); waitText("annual-paid", "365.00")
        capture("05-negative-net-and-real-payment-year")
        selectYear(2023)
        compose.onNodeWithTag("annual-empty-year").assertTextContains("未录入相关记录", substring = true)
        capture("06-empty-year-is-not-zero-spend")

        compose.runOnIdle { fontScale.value = 2f }
        scrollTo(ANNUAL, "annual-year-input")
        compose.onNodeWithTag("annual-year-input").assertIsDisplayed()
        capture("07-360dp-font200-year-input")
        scrollTo(ANNUAL, "annual-review-back")
        compose.onNodeWithTag("annual-review-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        capture("08-360dp-font200-back")
        assertEquals(before, generation.backupSnapshotSource().capture())
        assertEquals(computer.name, ready.repository.loadProjectDetail(computer.id)!!.project.name)
    }

    private suspend fun seed(name: String, start: LocalDate, cents: Long, category: String): LongTermProjectEntity {
        val project = LongTermProjectEntity(id(), name, category, start, createdAt = timestamp, updatedAt = timestamp)
        val command = id()
        val event = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, cents, start,
            role = ProjectMoneyEventRole.INITIAL, operationId = command)
        assertTrue(ready.repository.createProject(CreateProjectCommand(command, project, event)) is MutationResult.Success)
        return project
    }
    private suspend fun append(projectId: String, direction: MoneyDirection, cents: Long, date: LocalDate, description: String) {
        val event = ProjectMoneyEventEntity(id(), projectId, direction, cents, date, description,
            role = ProjectMoneyEventRole.REGULAR, operationId = id())
        assertTrue(ready.repository.appendMoneyEvent(AddProjectMoneyEventCommand(event.operationId, event)) is MutationResult.Success)
    }
    private fun selectYear(year: Int) {
        scrollTo(ANNUAL, "annual-year-input")
        compose.onNodeWithTag("annual-year-input").performTextReplacement(year.toString())
        scrollClick(ANNUAL, "annual-year-apply")
        scrollTo(ANNUAL, "annual-selected-year")
        waitText("annual-selected-year", year.toString())
    }
    private fun click(tag: String) {
        Espresso.closeSoftKeyboard(); compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
    }
    private fun scrollTo(container: String, tag: String) = compose.onNodeWithTag(container).performScrollToNode(hasTestTag(tag))
    private fun scrollClick(container: String, tag: String) { scrollTo(container, tag); click(tag) }
    private fun waitNode(tag: String) = compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    private fun waitText(tag: String, text: String) = compose.waitUntil(15_000) {
        compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().any { textOf(it).contains(text) }
    }
    private fun textOf(node: SemanticsNode): String = buildString {
        if (node.config.contains(SemanticsProperties.Text)) append(node.config[SemanticsProperties.Text].joinToString(" ") { it.text })
        node.children.forEach { append(' '); append(textOf(it)) }
    }
    private fun capture(name: String) {
        compose.waitForIdle(); assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        val files = InstrumentationRegistry.getInstrumentation().targetContext.filesDir.canonicalFile
        val directory = File(files, "m19-evidence").canonicalFile
        check(directory.parentFile == files && (directory.isDirectory || directory.mkdir()))
        val output = File(directory, "$name-$runId.png"); check(output.createNewFile())
        val bitmap = compose.onNodeWithTag("m19-canvas").captureToImage().asAndroidBitmap()
        FileOutputStream(output).use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)); it.fd.sync() }
        check(output.length() > 0)
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply { putString("M19_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath) })
    }
    private fun id() = UUID.randomUUID().toString()
    private companion object {
        const val CATEGORY = "category-analysis-screen"
        const val ANNUAL = "annual-review-screen"
    }
}
