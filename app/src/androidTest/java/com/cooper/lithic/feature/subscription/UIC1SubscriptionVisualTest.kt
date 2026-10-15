package com.cooper.lithic.feature.subscription

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.SubscriptionDetail
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Synthetic display hosts: existing Room/lifecycle suites retain the real write journeys. */
class UIC1SubscriptionVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compact_management_header_keeps_first_service_visible_and_accessible_actions_working() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts())
        var backs = 0
        var creates = 0
        var pixelsPerDp = 1f
        compose.setContent {
            pixelsPerDp = LocalDensity.current.density
            LithicTheme {
                SubscriptionListScreen(SubscriptionListUiState(listOf(detail), 1, loading = false),
                    { backs++ }, { creates++ }, {}, {}, {})
            }
        }
        val row = compose.onNodeWithTag("subscription-row-${detail.subscription.id}")
            .assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val page = compose.onNodeWithTag("subscription-list-content").fetchSemanticsNode().boundsInRoot
        assertTrue("The first service must remain within the first 200dp, including its status group",
            row.top - page.top <= 200f * pixelsPerDp)
        compose.onNodeWithTag("subscription-list-create").assertContentDescriptionEquals("新增订阅")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("subscription-list-back").assertContentDescriptionEquals("返回现在")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        assertEquals(1, creates)
        assertEquals(1, backs)
    }

    @Test fun referenced_unreadable_cover_is_distinct_from_no_cover_and_keeps_original_identity_clickable() {
        val original = M09SubscriptionFixtures.facts("引用失效封面的服务")
        val broken = M09SubscriptionFixtures.detail(original.copy(
            subscription = original.subscription.copy(coverAssetId = "unreadable-asset")))
        val absent = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts("未设置封面的服务"))
        var opened: String? = null
        compose.setContent { LithicTheme {
            SubscriptionListScreen(SubscriptionListUiState(listOf(broken, absent), 2, loading = false),
                {}, {}, { opened = it }, {}, {})
        } }
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(
            hasTestTag("subscription-row-${broken.subscription.id}"))
        compose.onNodeWithTag("subscription-${broken.subscription.id}-cover-error", useUnmergedTree = true)
            .assertIsDisplayed().assertContentDescriptionEquals("封面无法读取，进入详情可重新选择或移除")
        compose.onNodeWithTag("subscription-${broken.subscription.id}-cover-empty", useUnmergedTree = true)
            .assertDoesNotExist()
        compose.onNodeWithTag("subscription-row-${broken.subscription.id}").performClick()
        assertEquals(broken.subscription.id, opened)
        compose.onNodeWithTag("subscription-list-content").performScrollToNode(
            hasTestTag("subscription-row-${absent.subscription.id}"))
        compose.onNodeWithTag("subscription-${absent.subscription.id}-cover-error", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test fun prepaid_future_period_is_compared_with_elapsed_days_without_replacing_current_plan() {
        val facts = M09SubscriptionFixtures.facts(futurePayment = true)
        val detail = M09SubscriptionFixtures.detail(facts.copy(renewals = facts.renewals.mapIndexed { index, renewal ->
            if (index == 1) renewal.copy(amountCents = 30000) else renewal
        }))
        renderDetail(detail)
        detailNode("subscription-detail-historical-average").assertContentDescriptionEquals("7.50元/天")
        detailNode("subscription-detail-purchased-days").assertTextContains("60天", substring = true)
        // These are separate concepts in the same overview, before the independent current-plan card.
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(
            hasText("实际已过去的付费有效天数：25天") and
                hasAnyAncestor(hasTestTag("subscription-detail-cost-summary")))
        compose.onNodeWithText("实际已过去的付费有效天数：25天").assertIsDisplayed()
        detailNode("subscription-detail-current-rate").assertContentDescriptionEquals("5.00元/天")
        compose.onNodeWithText("实际生效日期：2026-01-01").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(
            hasTestTag("subscription-renewal-future-${detail.renewals.last().id}"))
        compose.onNodeWithTag("subscription-renewal-future-${detail.renewals.last().id}")
            .assertTextContains("尚未生效")
    }

    @Test fun stop_record_action_keeps_provider_cancellation_disclaimer_visible_before_callback() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts())
        var stops = 0
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(detailState(detail), {}, {}, {}, {}, onStop = { stops++ })
        } }
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(
            hasTestTag("subscription-detail-stop"))
        compose.onNodeWithText("停止本App记录不等于替用户向服务商取消订阅。")
            .assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-stop").assertIsEnabled().performClick()
        assertEquals(1, stops)
    }

    @Test fun current_and_stopped_lists_distinguish_loading_failed_empty_and_stale_original_rows() {
        var historyPage by mutableStateOf(false)
        var state by mutableStateOf(SubscriptionListUiState())
        var reads = 0
        var opened: String? = null
        compose.setContent { LithicTheme {
            if (historyPage) {
                SubscriptionHistoryScreen(state, {}, { opened = it }, {}, { reads++ })
            } else {
                SubscriptionListScreen(state, {}, {}, { opened = it }, {}, { reads++ })
            }
        } }
        for (isHistory in listOf(false, true)) {
            val prefix = if (isHistory) "subscription-history" else "subscription-list"
            val contentTag = "$prefix-content"
            val emptyText = if (isHistory) "暂无已停止订阅" else "暂无订阅服务"
            val loadingText = if (isHistory) "正在读取已停止订阅" else "正在读取订阅服务"
            val staleText = if (isHistory) "以下为上次读取记录。" else "暂时无法刷新，以下为上次读取记录。"
            val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(
                operation = if (isHistory) SubscriptionOperationState.STOPPED else null))
            val rowTag = if (isHistory) "subscription-history-row-${detail.subscription.id}"
                else "subscription-row-${detail.subscription.id}"
            compose.runOnIdle {
                historyPage = isHistory
                state = SubscriptionListUiState()
            }
            compose.onNodeWithTag(contentTag).performScrollToNode(hasText(loadingText))
            compose.onNodeWithText(loadingText).assertIsDisplayed()
            compose.onNodeWithText(emptyText).assertDoesNotExist()

            compose.runOnIdle { state = state.copy(loading = false, error = "本地列表读取失败") }
            compose.onNodeWithTag(contentTag).performScrollToNode(hasTestTag("$prefix-error"))
            compose.onNodeWithTag("$prefix-error").assertTextContains("本地列表读取失败")
            compose.onNodeWithText(emptyText).assertDoesNotExist()
            compose.onNodeWithTag("$prefix-retry").performClick()
            assertEquals(if (isHistory) 3 else 1, reads)

            compose.runOnIdle { state = state.copy(totalCount = 0, error = null) }
            compose.onNodeWithTag(contentTag).performScrollToNode(hasText(emptyText))
            compose.onNodeWithText(emptyText).assertIsDisplayed()
            compose.onNodeWithTag("$prefix-error").assertDoesNotExist()

            compose.runOnIdle { state = state.copy(items = listOf(detail), totalCount = 1) }
            compose.onNodeWithTag(contentTag).performScrollToNode(hasTestTag(rowTag))
            compose.onNodeWithTag(rowTag).assertIsDisplayed().performClick()
            assertEquals(detail.subscription.id, opened)
            compose.onNodeWithText(emptyText).assertDoesNotExist()

            compose.runOnIdle { state = state.copy(error = "刷新失败，原记录仍保留") }
            compose.onNodeWithTag(contentTag).performScrollToNode(hasText(staleText))
            compose.onNodeWithText(staleText).assertIsDisplayed()
            compose.onNodeWithTag("$prefix-error").assertTextContains("刷新失败", substring = true)
            compose.onNodeWithTag(contentTag).performScrollToNode(hasTestTag("$prefix-retry"))
            compose.onNodeWithTag("$prefix-retry").assertIsEnabled().performClick()
            assertEquals(if (isHistory) 4 else 2, reads)
            compose.onNodeWithTag(contentTag).performScrollToNode(hasTestTag(rowTag))
            compose.onNodeWithTag(rowTag).assertIsDisplayed().performClick()
            assertEquals(detail.subscription.id, opened)
            compose.onNodeWithText(emptyText).assertDoesNotExist()
        }
    }

    @Test fun detail_distinguishes_loading_error_missing_and_stale_facts_with_mutations_disabled() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts())
        var state by mutableStateOf(SubscriptionDetailUiState())
        var reads = 0
        var backs = 0
        var mutations = 0
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(state, { backs++ }, { mutations++ }, {}, { reads++ },
                onPause = { mutations++ }, onStop = { mutations++ }, onEditCover = { mutations++ },
                onCorrectRenewal = { mutations++ }, onDeleteRenewal = { mutations++ })
        } }
        compose.onNodeWithText("正在读取订阅").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-missing").assertDoesNotExist()
        compose.onNodeWithText("暂无付费记录").assertDoesNotExist()

        compose.runOnIdle { state = state.copy(loading = false, error = "本地订阅读取失败") }
        detailNode("subscription-detail-error").assertTextContains("本地订阅读取失败")
        compose.onNodeWithTag("subscription-detail-missing").assertDoesNotExist()
        detailNode("subscription-detail-retry").assertIsEnabled().performClick()
        assertEquals(1, reads)

        compose.runOnIdle { state = state.copy(error = null, missing = true) }
        detailNode("subscription-detail-missing").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-error").assertDoesNotExist()
        compose.onNodeWithTag("subscription-detail-name").assertDoesNotExist()

        compose.runOnIdle { state = detailState(detail).copy(error = "刷新失败，原订阅仍保留") }
        detailNode("subscription-detail-error").assertTextContains("刷新失败", substring = true)
        compose.onNodeWithText("以下为上次读取记录。").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-missing").assertDoesNotExist()
        detailNode("subscription-detail-name").assertTextContains(detail.subscription.name)
        detailNode("subscription-detail-historical-average").assertContentDescriptionEquals("5.00元/天")
        detailNode("subscription-detail-purchased-days").assertTextContains("折算付费时长：30天")
        for (tag in listOf("subscription-detail-renewal", "subscription-detail-pause", "subscription-detail-stop",
            "subscription-cover-edit", "subscription-renewal-correct-${detail.renewals.single().id}",
            "subscription-renewal-delete-${detail.renewals.single().id}")) {
            detailNode(tag).assertIsNotEnabled()
        }
        detailNode("subscription-detail-retry").assertIsEnabled().performClick()
        assertEquals(2, reads)
        detailNode("subscription-detail-back").assertIsEnabled().performClick()
        assertEquals(1, backs)
        assertEquals(0, mutations)
    }

    private fun renderDetail(detail: SubscriptionDetail) {
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(detailState(detail), {}, {}, {}, {})
        } }
    }

    private fun detailState(detail: SubscriptionDetail) = SubscriptionDetailUiState(detail,
        RenewalHistoryUiState(detail.renewals, detail.renewals.size.toLong(), loading = false), loading = false)

    private fun detailNode(tag: String): androidx.compose.ui.test.SemanticsNodeInteraction {
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
