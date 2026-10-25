package com.cooper.lithic.feature.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.feature.subscription.M09SubscriptionFixtures
import com.cooper.lithic.feature.subscription.SubscriptionHistoryScreen
import com.cooper.lithic.feature.subscription.SubscriptionListScreen
import com.cooper.lithic.feature.subscription.SubscriptionListUiState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Rebuildable state only: no Room owner, app storage or system settings are created. */
@RunWith(AndroidJUnit4::class)
class P02BrowseListExperienceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun project_history_unknown_count_is_not_a_confirmed_empty_archive() {
        var state by mutableStateOf(ProjectListUiState(loading = false))
        val reads = AtomicInteger()
        compose.setContent { LithicTheme { ProjectHistoryScreen(state, {}, {}, {}, { reads.incrementAndGet() }) } }
        node("project-history-content", "project-history-unavailable")
            .assertTextContains("尚未取得", substring = true)
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        node("project-history-content", "project-history-retry").performClick()
        assertEquals(1, reads.get())
        compose.runOnIdle { state = state.copy(totalCount = 0) }
        node("project-history-content", "project-history-empty").assertTextContains("暂无已结束项目")
        compose.runOnIdle { state = state.copy(loadingMore = true) }
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(loadingMore = false, error = "合成读取失败") }
        compose.onNodeWithText("暂无已结束项目").assertDoesNotExist()
        node("project-history-content", "project-history-count-snapshot")
            .assertTextContains("上次读取", substring = true)
    }

    @Test fun project_list_preserves_old_rows_and_does_not_offer_duplicate_busy_reads() {
        var state by mutableStateOf(ProjectListUiState(listOf(project()), 51, loading = false, loadingMore = true,
            hasMore = true, error = "合成刷新失败"))
        var opened: String? = null
        val pages = AtomicInteger()
        compose.setContent { LithicTheme {
            ProjectListScreen(state, {}, {}, { opened = it }, { pages.incrementAndGet() }, {})
        } }
        node("project-list-content", "project-list-stale")
            .assertTextContains("上次读取", substring = true)
        node("project-list-content", "project-list-retry").assertIsNotEnabled()
        node("project-list-content", "project-list-load-more").assertIsNotEnabled()
        compose.runOnIdle { state = state.copy(error = null) }
        node("project-list-content", "project-list-refreshing-snapshot")
            .assertTextContains("上次读取", substring = true)
        compose.runOnIdle { state = state.copy(loadingMore = false, error = "合成刷新失败") }
        node("project-list-content", "project-row-${project().project.id}").performClick()
        assertEquals(project().project.id, opened)
        node("project-list-content", "project-list-load-more").assertIsEnabled().performClick()
        assertEquals(1, pages.get()) // An error does not change the existing continuation callback contract.
    }

    @Test fun current_project_unknown_and_confirmed_empty_states_remain_distinct() {
        var state by mutableStateOf(ProjectListUiState(loading = false))
        compose.setContent { LithicTheme { ProjectListScreen(state, {}, {}, {}, {}, {}) } }
        node("project-list-content", "project-list-unavailable").assertTextContains("尚未取得", substring = true)
        compose.onNodeWithText("暂无长期项目").assertDoesNotExist()
        compose.runOnIdle { state = state.copy(totalCount = 0) }
        node("project-list-content", "project-list-empty").assertTextContains("暂无长期项目")
        compose.runOnIdle { state = ProjectListUiState(listOf(project()), 1, loading = false) }
        node("project-list-content", "project-list-page-status").assertTextContains("已到列表末尾", substring = true)
    }

    @Test fun current_subscription_unknown_count_and_failed_read_are_not_empty() {
        var state by mutableStateOf(SubscriptionListUiState(loading = false))
        val reads = AtomicInteger()
        compose.setContent { LithicTheme { SubscriptionListScreen(state, {}, {}, {}, {}, { reads.incrementAndGet() }) } }
        node("subscription-list-content", "subscription-list-unavailable").assertTextContains("尚未取得", substring = true)
        compose.onNodeWithText("暂无订阅服务").assertDoesNotExist()
        node("subscription-list-content", "subscription-list-retry").performClick()
        assertEquals(1, reads.get())
        compose.runOnIdle { state = state.copy(totalCount = 0) }
        node("subscription-list-content", "subscription-list-empty").assertTextContains("暂无订阅服务")
        compose.runOnIdle { state = state.copy(error = "合成读取失败") }
        compose.onNodeWithText("暂无订阅服务").assertDoesNotExist()
        node("subscription-list-content", "subscription-list-count-snapshot")
            .assertTextContains("上次读取", substring = true)
    }

    @Test fun stopped_subscription_history_keeps_stable_rows_and_busy_page_feedback() {
        val facts = M09SubscriptionFixtures.facts(operation = SubscriptionOperationState.STOPPED)
        val detail = M09SubscriptionFixtures.detail(facts)
        var state by mutableStateOf(SubscriptionListUiState(loading = false))
        var opened: String? = null
        val pages = AtomicInteger()
        compose.setContent { LithicTheme {
            SubscriptionHistoryScreen(state, {}, { opened = it }, { pages.incrementAndGet() }, {})
        } }
        node("subscription-history-content", "subscription-history-unavailable")
            .assertTextContains("尚未取得", substring = true)
        compose.onNodeWithTag("subscription-history-empty").assertDoesNotExist()
        compose.runOnIdle { state = SubscriptionListUiState(listOf(detail), 51, loading = false,
            hasMore = true, error = "合成历史刷新失败") }
        node("subscription-history-content", "subscription-history-stale")
            .assertTextContains("上次读取", substring = true)
        node("subscription-history-content", "subscription-history-row-${detail.subscription.id}").performClick()
        assertEquals(detail.subscription.id, opened)
        node("subscription-history-content", "subscription-history-load-more").performClick()
        assertEquals(1, pages.get())
        compose.runOnIdle { state = state.copy(loadingMore = true, error = null) }
        node("subscription-history-content", "subscription-history-load-more").assertIsNotEnabled()
        node("subscription-history-content", "subscription-history-retry").assertDoesNotExist()
        node("subscription-history-content", "subscription-history-page-status")
            .assertTextContains("正在读取下一页", substring = true)
    }

    private fun node(content: String, tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag(content).performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun project(): ProjectDetail {
        val date = LocalDate.of(2026, 1, 10)
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity("other", CategoryType.PROJECT, "其他", "其他", true)
        return ProjectDetail(LongTermProjectEntity("72000000-0000-0000-0000-000000000001", "合成长期项目",
            category.id, LocalDate.of(2026, 1, 1), createdAt = timestamp, updatedAt = timestamp),
            category, emptyList(), ProjectCostResult(10, 10000, 0, 10000, BigDecimal.TEN, true, null), date)
    }
}
