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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.time.BusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.local.SubscriptionEntity
import com.cooper.lithic.data.local.SubscriptionRenewalEntity
import com.cooper.lithic.data.local.SubscriptionStateChangeEntity
import com.cooper.lithic.data.repository.CategoryRepository
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.CreateSubscriptionCommand
import com.cooper.lithic.data.repository.EndProjectCommand
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.SetSubscriptionStateCommand
import com.cooper.lithic.data.repository.SubscriptionLifecycleResult
import com.cooper.lithic.data.repository.SubscriptionMutationResult
import com.cooper.lithic.data.repository.SubscriptionStateAction
import com.cooper.lithic.data.repository.facts
import com.cooper.lithic.domain.subscription.BillingCycle
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Real LithicApp/Nav3, controllers and repositories in an isolated ComponentActivity.
 * Requires M13IsolatedRunner (plain Application); never opens MainActivity/default storage.
 * Only synthetic facts enter a fresh private owner. LocalDensity changes the test canvas,
 * not the device font/density. Screenshots are test-host evidence, not release verification.
 */
@RunWith(AndroidJUnit4::class)
class M14ScreenshotsJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val today = LocalDate.of(2026, 1, 20)
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = BusinessClock { today }
    private val models = ViewModelStore()
    private val showing = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private val runId = UUID.randomUUID().toString()
    private lateinit var root: File
    private lateinit var owner: AppContainer
    private lateinit var holder: LedgerUiStateHolder
    private val ready get() = holder.state.value as LedgerUiState.Ready
    private val categories: CategoryRepository get() = checkNotNull(ready.categoryRepository)

    @Before fun setUp(): Unit = runBlocking {
        assertEquals("M13IsolatedRunner must prevent the default ledger Application from starting",
            Application::class.java, context.applicationContext.javaClass)
        root = File(context.noBackupFilesDir, "m14-ui-screenshots-$runId").canonicalFile
        assertEquals(context.noBackupFilesDir.canonicalFile, root.parentFile)
        check(root.mkdir()) { "Only a new dedicated synthetic root may be used" }
        val storage = File(root, "owned-generation").apply { check(mkdir()) }
        owner = AppContainer(DataGenerationStore(context, storage), CoroutineScope(SupervisorJob() + Dispatchers.IO))
        owner.start(zone)
        owner.openStorage()
        withContext(Dispatchers.Main) {
            holder = LedgerUiStateHolder(owner.storageState) { storedZone ->
                assertEquals(zone, storedZone)
                clock
            }.also { models.put("m14-home", it) }
        }
        withTimeout(15_000) { holder.state.first { it is LedgerUiState.Ready } }
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("m14-test-viewport")) {
                val canvasDensity = constraints.maxWidth / 360f
                val canvasHeight = minOf(640f, constraints.maxHeight / canvasDensity).dp
                CompositionLocalProvider(LocalDensity provides Density(canvasDensity, fontScale.value)) {
                    LithicTheme(darkTheme = false) {
                        Box(Modifier.requiredSize(360.dp, canvasHeight).testTag("m14-test-canvas")) {
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
            assertEquals("m14-ui-screenshots-$runId", root.name)
            check(root == root.canonicalFile)
            check(root.deleteRecursively()) { "Cannot remove this test's closed synthetic owner root" }
        }
    }

    @Test fun settings_category_writes_merge_and_current_history_search_with_360dp_large_text(): Unit = runBlocking {
        compose.onNodeWithTag("tab-settings").performClick()
        click("open-category-management")
        waitNode("category-management-screen")

        val projectSource = createCategory("电子设备", CategoryType.PROJECT)
        renameCategory(projectSource, "影像设备")
        val projectTarget = createCategory("生活工具", CategoryType.PROJECT)
        val projectCurrent = createProject("截图合成·当前相机", projectSource.id, ended = false)
        val projectHistory = createProject("截图合成·历史相机", projectSource.id, ended = true)
        val beforeProjectCurrent = checkNotNull(ready.repository.loadProjectDetail(projectCurrent))
        val beforeProjectHistory = checkNotNull(ready.repository.loadProjectDetail(projectHistory))
        categoryAction("category-row-${projectSource.id}")
        waitCategoryCount(projectSource.id, 2)
        compose.onNodeWithText("影像设备").assertIsDisplayed()
        capture("01-project-category-list")
        mergeCategory(projectSource.id, projectTarget, "影像设备", "02-project-merge-confirmation")
        assertEquals(beforeProjectCurrent.copy(project = beforeProjectCurrent.project.copy(categoryId = projectTarget.id), category = projectTarget),
            ready.repository.loadProjectDetail(projectCurrent))
        assertEquals(beforeProjectHistory.copy(project = beforeProjectHistory.project.copy(categoryId = projectTarget.id), category = projectTarget),
            ready.repository.loadProjectDetail(projectHistory))

        categoryClick("category-type-SUBSCRIPTION")
        val subscriptionSource = createCategory("数字服务", CategoryType.SUBSCRIPTION)
        renameCategory(subscriptionSource, "影音订阅")
        val subscriptionTarget = createCategory("固定服务", CategoryType.SUBSCRIPTION)
        val subscriptionCurrent = createSubscription("截图合成·当前服务", subscriptionSource.id, stopped = false)
        val subscriptionHistory = createSubscription("截图合成·历史服务", subscriptionSource.id, stopped = true)
        val beforeSubscriptionCurrent = checkNotNull(ready.subscriptionRepository.loadSubscriptionDetail(subscriptionCurrent))
        val beforeSubscriptionHistory = checkNotNull(ready.subscriptionRepository.loadSubscriptionDetail(subscriptionHistory))
        categoryAction("category-row-${subscriptionSource.id}")
        waitCategoryCount(subscriptionSource.id, 2)
        capture("03-subscription-category-list")
        mergeCategory(subscriptionSource.id, subscriptionTarget, "影音订阅", "04-subscription-merge-confirmation")
        assertEquals(beforeSubscriptionCurrent.copy(subscription = beforeSubscriptionCurrent.subscription.copy(categoryId = subscriptionTarget.id), category = subscriptionTarget),
            ready.subscriptionRepository.loadSubscriptionDetail(subscriptionCurrent))
        assertEquals(beforeSubscriptionHistory.copy(subscription = beforeSubscriptionHistory.subscription.copy(categoryId = subscriptionTarget.id), category = subscriptionTarget),
            ready.subscriptionRepository.loadSubscriptionDetail(subscriptionHistory))

        compose.onNodeWithTag("tab-now").performClick()
        waitNode("home-screen")
        click("open-name-search")
        waitNode("global-search-screen")
        replace("search-query", "截图合成")
        waitText("search-result-count", "找到 4 条记录")
        searchStatus(projectCurrent, "持有中", "当前管理")
        capture("05-search-current-project")
        searchStatus(projectHistory, "已结束", "历史记录")
        searchStatus(subscriptionCurrent, "订阅中", "当前管理")
        searchStatus(subscriptionHistory, "已停止", "历史记录")
        capture("06-search-history-subscription")

        // Real result opens the exact production detail route, including historical origin.
        compose.onNodeWithTag("search-row-$subscriptionHistory").performClick()
        waitNode("subscription-detail-screen")
        waitText("subscription-detail-name", "截图合成·历史服务")
        compose.onNodeWithTag("subscription-detail-name").assertTextEquals("截图合成·历史服务")
        waitText("subscription-detail-status", "已停止")
        compose.onNodeWithTag("subscription-detail-status").assertTextEquals("已停止")
        compose.onNodeWithTag("subscription-detail-back").assertTextEquals("返回已停止订阅")
        compose.onNodeWithTag("tab-now").performClick()
        waitNode("home-screen")
        click("open-name-search")
        waitNode("global-search-screen")
        compose.runOnIdle { fontScale.value = 2f }
        compose.waitForIdle()
        replace("search-query", "截图合成")
        waitText("search-result-count", "找到 4 条记录")
        compose.onNodeWithTag("global-search-screen").performScrollToNode(hasTestTag("search-query"))
        compose.onNodeWithTag("search-query").assertIsDisplayed()
        capture("07-search-360dp-font200-header")
        searchStatus(subscriptionHistory, "已停止", "历史记录")
        capture("08-search-360dp-font200-history")
        assertEquals(zone, owner.openStorage().settings.recordZone())
    }

    private suspend fun createCategory(name: String, type: CategoryType): CategoryEntity {
        categoryClick("category-create")
        waitNode("category-name")
        replace("category-name", name)
        click("category-save")
        waitEditorClosed()
        return categories.list(type).single { it.category.name == name }.category
    }

    private fun renameCategory(category: CategoryEntity, newName: String) {
        categoryClick("category-rename-${category.id}")
        waitNode("category-name")
        replace("category-name", newName)
        click("category-save")
        waitEditorClosed()
    }

    private suspend fun mergeCategory(sourceId: String, target: CategoryEntity, sourceName: String, screenshot: String) {
        categoryClick("category-merge-$sourceId")
        waitNode("category-target-${target.id}")
        click("category-target-${target.id}")
        click("category-save")
        waitNode("category-merge-confirm")
        compose.onNodeWithText("“$sourceName”的 2 条记录（含历史）将移至“${target.name}”。").assertIsDisplayed()
        capture(screenshot, "category-editor-dialog")
        click("category-merge-confirm")
        waitEditorClosed()
        assertTrue(categories.list(target.type).none { it.category.id == sourceId })
        assertEquals(2L, categories.list(target.type).single { it.category.id == target.id }.recordCount)
    }

    private suspend fun createProject(name: String, categoryId: String, ended: Boolean): String {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val startedOn = today.minusDays(29)
        val project = LongTermProjectEntity(id(), name, categoryId, startedOn, createdAt = timestamp, updatedAt = timestamp)
        val operationId = id()
        val event = ProjectMoneyEventEntity(id(), project.id, MoneyDirection.OUTLAY, 30_000L, startedOn,
            role = ProjectMoneyEventRole.INITIAL, operationId = operationId)
        val created = ready.repository.createProject(CreateProjectCommand(operationId, project, event))
        assertTrue("Synthetic project create failed: $created", created is MutationResult.Success)
        if (ended) {
            val result = ready.repository.endProject(EndProjectCommand(id(), project, today.minusDays(1), "合成截图历史记录", null, timestamp.plusSeconds(1)))
            assertTrue("Synthetic project end failed: $result", result is MutationResult.LifecycleApplied)
        }
        return project.id
    }

    private suspend fun createSubscription(name: String, categoryId: String, stopped: Boolean): String {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val subscription = SubscriptionEntity(id(), name, categoryId, createdAt = timestamp, updatedAt = timestamp)
        val operationId = id()
        val renewal = SubscriptionRenewalEntity(id(), subscription.id, today.minusDays(9), today.minusDays(9), today.plusDays(21),
            today.plusDays(18), 3_000L, BillingCycle.MONTHLY, 30, operationId)
        val created = ready.subscriptionRepository.createWithRenewal(CreateSubscriptionCommand(operationId, subscription, renewal))
        assertTrue("Synthetic subscription create failed: $created", created is SubscriptionMutationResult.Success)
        if (stopped) {
            val expected = checkNotNull(ready.subscriptionRepository.loadSubscriptionDetail(subscription.id)).facts()
            val stopOperation = id()
            val state = SubscriptionStateChangeEntity(id(), subscription.id, today, SubscriptionOperationState.STOPPED,
                reason = "合成截图历史记录", operationId = stopOperation)
            val result = ready.subscriptionRepository.setState(SetSubscriptionStateCommand(stopOperation, expected, state, SubscriptionStateAction.STOP_EARLY))
            assertTrue("Synthetic subscription stop failed: $result", result is SubscriptionLifecycleResult.Saved)
        }
        return subscription.id
    }

    private fun searchStatus(id: String, status: String, origin: String) {
        compose.onNodeWithTag("global-search-screen").performScrollToNode(hasTestTag("search-row-$id"))
        compose.onNodeWithTag("search-status-$id", useUnmergedTree = true)
            .assertIsDisplayed().assertTextEquals("$status · $origin")
    }

    private fun categoryAction(tag: String) {
        compose.onNodeWithTag("category-management-screen").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun categoryClick(tag: String) {
        categoryAction(tag)
        waitEnabled(tag)
        compose.onNodeWithTag(tag).assertIsEnabled().performClick()
    }

    private fun waitCategoryCount(categoryId: String, count: Int) {
        val countInThisRow = hasText("$count 条记录（含历史）") and
            hasAnyAncestor(hasTestTag("category-row-$categoryId"))
        compose.waitUntil(15_000) {
            compose.onAllNodes(countInThisRow, useUnmergedTree = true).fetchSemanticsNodes().size == 1
        }
        compose.onNode(countInThisRow, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun click(tag: String) {
        Espresso.closeSoftKeyboard()
        // Dialog confirmation buttons have no scroll ancestor; these entry/actions are visible.
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled().performClick()
    }

    private fun replace(tag: String, text: String) {
        compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performTextReplacement(text)
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }

    private fun waitNode(tag: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitText(tag: String, text: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { node ->
                node.config[SemanticsProperties.Text].any { it.text.contains(text) }
            }
        }
    }

    private fun waitEnabled(tag: String) {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag(tag).fetchSemanticsNodes().any { node -> !node.config.contains(SemanticsProperties.Disabled) }
        }
    }

    private fun waitEditorClosed() {
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("category-editor-dialog").fetchSemanticsNodes().isEmpty() }
        categoryAction("category-create")
        waitEnabled("category-create")
        compose.waitForIdle()
    }

    private fun capture(name: String, tag: String = "m14-test-canvas") {
        assertEquals(ComponentActivity::class.java, compose.activity.javaClass)
        compose.waitForIdle()
        val viewport = compose.onNodeWithTag("m14-test-viewport").fetchSemanticsNode()
        val canvas = compose.onNodeWithTag("m14-test-canvas").fetchSemanticsNode()
        assertTrue("360dp canvas exceeds the visible viewport",
            canvas.positionInRoot.x >= viewport.positionInRoot.x - 0.5f &&
                canvas.positionInRoot.x + canvas.size.width <= viewport.positionInRoot.x + viewport.size.width + 0.5f)
        val bitmap = compose.onNodeWithTag(tag).captureToImage().asAndroidBitmap()
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val filesRoot = target.filesDir.canonicalFile
        val directory = File(filesRoot, "m14-evidence")
        assertEquals(filesRoot, directory.canonicalFile.parentFile)
        check(directory.absoluteFile == directory.canonicalFile) { "Screenshot directory must not redirect to another location" }
        check(directory.isDirectory || directory.mkdir())
        val output = File(directory, "$name-$runId.png")
        check(output.createNewFile()) { "Screenshot evidence must not replace an existing file" }
        FileOutputStream(output).use { stream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
            stream.fd.sync()
        }
        check(output.length() > 0L)
        println("M14_COMPONENT_ACTIVITY_SCREENSHOT=${output.absolutePath}")
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("M14_COMPONENT_ACTIVITY_SCREENSHOT", output.absolutePath)
        })
    }

    private fun id(): String = UUID.randomUUID().toString()
}
