package com.cooper.lithic.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.Home
import com.cooper.lithic.app.LedgerUiState
import com.cooper.lithic.app.LithicNavigation
import com.cooper.lithic.app.ProjectList
import com.cooper.lithic.app.Review
import com.cooper.lithic.app.Settings
import com.cooper.lithic.app.Skeleton
import com.cooper.lithic.app.canonicalBackStack
import com.cooper.lithic.app.normalizeBackStack
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.time.FixedBusinessClock
import com.cooper.lithic.data.backup.DataGenerationStore
import com.cooper.lithic.data.backup.ManagedGeneration
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.CreateProjectCommand
import com.cooper.lithic.data.repository.LedgerReadRepository
import com.cooper.lithic.data.repository.MutationResult
import com.cooper.lithic.data.repository.ProjectWriteRepository
import com.cooper.lithic.data.repository.ProjectRepository
import com.cooper.lithic.data.repository.ReportRepository
import com.cooper.lithic.data.repository.SubscriptionRepository
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Serializable
private data object UnsupportedTestRoute : NavKey

@RunWith(AndroidJUnit4::class)
class NavigationShellTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var root: File
    private lateinit var generation: ManagedGeneration
    private lateinit var state: LedgerUiState.Ready

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        root = File(context.noBackupFilesDir, "m05-navigation-${UUID.randomUUID()}")
        check(root.mkdirs())
        generation = DataGenerationStore(context, root).open(ZoneId.of("Asia/Shanghai"))
        state = readReady()
    }

    @After
    fun tearDown() = runBlocking {
        if (::generation.isInitialized) generation.close()
        if (::root.isInitialized) check(root.deleteRecursively())
    }

    @Test
    fun only_three_primary_destinations() {
        render()
        assertThreePrimaryTabs()
        compose.onNodeWithTag("tab-now").assertTextEquals("现在")
        compose.onNodeWithTag("tab-review").assertTextEquals("回看")
        compose.onNodeWithTag("tab-settings").assertTextEquals("设置")
    }

    @Test
    fun first_launch_starts_at_home() {
        render()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").assertIsSelected()
    }

    @Test
    fun primary_selection_matches_its_visible_page() {
        render()
        compose.onNodeWithTag("tab-review").performClick().assertIsSelected()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-settings").performClick().assertIsSelected()
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").performClick().assertIsSelected()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    @Test
    fun repeated_primary_selection_does_not_build_history() {
        render()
        repeat(8) { compose.onNodeWithTag("tab-review").performClick() }
        systemBack()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").assertIsSelected()
    }

    @Test
    fun primary_system_back_returns_home() {
        render()
        for ((tag, page) in listOf("tab-review" to "review-screen", "tab-settings" to "settings-screen")) {
            compose.onNodeWithTag(tag).performClick().assertIsSelected()
            compose.onNodeWithTag(page).assertIsDisplayed()
            compose.waitForIdle()
            systemBack()
            compose.onNodeWithTag("home-screen").assertIsDisplayed()
            compose.onNodeWithTag("tab-now").assertIsSelected()
        }
    }

    @Test
    fun secondary_toolbar_back_returns_home() {
        render()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").assertDoesNotExist()
        compose.onNodeWithTag("project-list-back").assertIsDisplayed().performClick()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-now").assertIsSelected()
    }

    @Test
    fun secondary_system_back_returns_home() {
        render()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
        systemBack()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    @Test
    fun empty_screen_has_no_fake_metrics(): Unit = runBlocking {
        val snapshot = LedgerReadRepository(generation.database, generation.coordinator).snapshot()
        assertEquals(0L, state.projectCount)
        assertTrue(snapshot.projects.isEmpty())
        assertEquals(2, snapshot.categories.size)
        assertTrue(snapshot.moneyEvents.isEmpty())
        state = readReady(withOverview = true)
        render()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
        compose.onNodeWithTag("home-empty-guidance").assertIsDisplayed()
        compose.onNodeWithTag("home-total-rate").assertTextEquals("—")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("0")
        compose.onAllNodes(hasText("¥0.00", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("open-project-list").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertDoesNotExist()
        assertThreePrimaryTabs()
        compose.onNodeWithTag("home-open-add").assertIsDisplayed()
        compose.onNodeWithTag("tab-review").performClick()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        compose.onNodeWithTag("review-empty-state").assertIsDisplayed()
        compose.onNodeWithTag("trend-chart").assertDoesNotExist()
        compose.onAllNodes(hasText("¥", substring = true)).assertCountEquals(0)
    }

    @Test
    fun real_project_is_not_misrepresented_as_an_empty_ledger(): Unit = runBlocking {
        addSyntheticZeroProject()
        state = readReady(withOverview = true)
        val snapshot = LedgerReadRepository(generation.database, generation.coordinator).snapshot()
        assertEquals(1L, state.projectCount)
        assertEquals(1, snapshot.projects.size)
        assertEquals(0L, snapshot.moneyEvents.single().amountCents)
        render()
        compose.onNodeWithTag("home-project-record-count").performScrollTo().assertIsDisplayed().assertTextEquals("1")
            .assertContentDescriptionEquals("长期项目，已录入数量1，包含历史")
        compose.onNodeWithTag("home-subscription-record-count").performScrollTo().assertTextEquals("0")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-total-rate").performScrollTo().assertTextEquals("¥0.00/天")
            .assertContentDescriptionEquals("当前日均成本，0.00元/天")
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("合成零元项目")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("合成零元项目").assertIsDisplayed()
        compose.onNodeWithTag("project-row-${snapshot.projects.single().id}").performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("合成零元项目")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-name").assertIsDisplayed()
    }

    @Test
    fun future_recorded_end_date_does_not_pretend_the_project_has_already_ended(): Unit = runBlocking {
        val futureEnd = LocalDate.of(2026, 10, 20)
        addSyntheticZeroProject(endedOn = futureEnd)
        state = readReady()
        val project = state.repository.loadProjectPage().items.single().project
        assertEquals(futureEnd, project.endedOn)
        render()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodes(hasText("合成零元项目")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("合成零元项目").assertIsDisplayed()
        compose.onNodeWithText("已结束").assertDoesNotExist()
        compose.onNodeWithText("已记录结束日期").assertIsDisplayed()
        compose.onNodeWithTag("project-row-${project.id}").assertIsDisplayed()
            .assertTextContains("结束日期 $futureEnd", substring = true)
    }

    @Test
    fun settings_reads_the_persisted_zone_without_fake_controls() {
        var exportRequests = 0
        var restoreSelectionRequests = 0
        var overwriteRequests = 0
        compose.setContent {
            LithicTheme {
                LithicNavigation(state, onExportBackup = { exportRequests++ },
                    onRestoreBackup = { restoreSelectionRequests++ }, onConfirmRestore = { overwriteRequests++ })
            }
        }
        compose.onNodeWithTag("tab-settings").performClick()
        compose.onNodeWithText("Asia/Shanghai").assertIsDisplayed()
        compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)).assertCountEquals(0)
        compose.onNodeWithText("恢复功能尚未开放").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(0, exportRequests)
            assertEquals(0, restoreSelectionRequests)
            assertEquals(0, overwriteRequests)
        }
        compose.onNodeWithTag("export-complete-backup").performScrollTo().assertIsDisplayed().assertIsEnabled()
            .assertTextContains("导出完整备份").performClick()
        compose.onNodeWithTag("restore-complete-backup").performScrollTo().assertIsDisplayed().assertIsEnabled()
            .assertTextContains("选择完整备份").assertTextContains("完整覆盖，不合并；先预览再确认").performClick()
        compose.onNodeWithTag("confirm-backup-restore").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, exportRequests)
            assertEquals(1, restoreSelectionRequests)
            assertEquals(0, overwriteRequests)
        }
    }

    @Test
    fun back_stack_restores() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LithicTheme { LithicNavigation(state) } }
        compose.onNodeWithTag("tab-review").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("review-screen").assertIsDisplayed()
        compose.onNodeWithTag("tab-review").assertIsSelected()
        compose.onNodeWithTag("tab-now").performClick()
        compose.onNodeWithTag("open-project-list").performScrollTo().performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("project-list-screen").assertIsDisplayed()
        compose.onNodeWithTag("project-list-back").performClick()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    @Test
    fun legacy_saved_skeleton_restores_to_the_home_route() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val legacy = rememberNavBackStack(Skeleton)
            val route = normalizeBackStack(legacy).last()
            Text(if (route == Home) "现在" else "旧占位路由", Modifier.testTag("restored-route"))
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("restored-route").assertTextEquals("现在")
    }

    @Test
    fun invalid_restored_stacks_and_primary_routes_have_safe_parents() {
        assertEquals(listOf(Home), normalizeBackStack(emptyList()))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Skeleton)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, UnsupportedTestRoute)))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, Review)))
        assertEquals(listOf(Home, Review), canonicalBackStack(Review))
        assertEquals(listOf(Home, Settings), canonicalBackStack(Settings))
        assertEquals(listOf(Home, ProjectList), canonicalBackStack(ProjectList))
    }

    @Test
    fun large_text_has_no_blocked_action() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme {
                    Box(Modifier.width(360.dp).fillMaxHeight()) { LithicNavigation(state) }
                }
            }
        }
        for (tag in listOf("tab-review", "tab-settings", "tab-now")) {
            compose.onNodeWithTag(tag).assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
            compose.onNodeWithTag(tag).assertIsSelected()
        }
        compose.onNodeWithTag("open-project-list").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("project-list-back").assertIsDisplayed().assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("home-screen").assertIsDisplayed()
    }

    private fun render() {
        compose.setContent { LithicTheme { LithicNavigation(state) } }
    }

    private fun assertThreePrimaryTabs() {
        val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        val homeSegment = hasTestTag("home-project-tab") or hasTestTag("home-subscription-tab")
        compose.onAllNodes(tabs and homeSegment.not()).assertCountEquals(3)
    }

    private fun systemBack() {
        Espresso.pressBack()
        compose.waitUntil(timeoutMillis = 10_000) {
            try {
                compose.onNodeWithTag("home-screen").assertIsDisplayed()
                true
            } catch (_: AssertionError) {
                false
            }
        }
        compose.waitForIdle()
    }

    private suspend fun readReady(withOverview: Boolean = false): LedgerUiState.Ready {
        val clock = FixedBusinessClock(LocalDate.of(2026, 10, 12))
        val repository = ProjectRepository(generation.database, generation.coordinator, clock)
        val subscriptions = SubscriptionRepository(generation.database, generation.coordinator, clock)
        val reports = if (withOverview) ReportRepository(generation.database, generation.coordinator, clock) else null
        return LedgerUiState.Ready(repository.loadSummary().projectCount, generation.settings.recordZone(), repository, clock,
            subscriptions, subscriptions.loadSubscriptionCount(), overview = reports?.loadOverview(), reportRepository = reports)
    }

    private suspend fun addSyntheticZeroProject(endedOn: LocalDate? = null) {
        val id = UUID.randomUUID().toString()
        val operation = UUID.randomUUID().toString()
        val day = LocalDate.of(2026, 10, 10)
        val timestamp = Instant.parse("2026-10-10T10:00:00Z")
        val result = ProjectWriteRepository(generation.database, generation.coordinator,
            FixedBusinessClock(LocalDate.of(2026, 10, 12))).createProject(
            CreateProjectCommand(
                operation,
                LongTermProjectEntity(id, "合成零元项目", LithicDatabase.PROJECT_FALLBACK_ID,
                    day, endedOn = endedOn, endReason = endedOn?.let { "合成结束日期边界" },
                    createdAt = timestamp, updatedAt = timestamp),
                ProjectMoneyEventEntity(UUID.randomUUID().toString(), id, MoneyDirection.OUTLAY,
                    0L, day, role = ProjectMoneyEventRole.INITIAL, operationId = operation),
            ),
        )
        assertTrue(result is MutationResult.Success)
    }
}
