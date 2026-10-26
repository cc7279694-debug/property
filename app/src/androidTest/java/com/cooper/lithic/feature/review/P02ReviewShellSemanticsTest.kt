package com.cooper.lithic.feature.review

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled Compose assertions only; Android execution must be recorded separately from compilation. */
@RunWith(AndroidJUnit4::class)
class P02ReviewShellSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun subscription_only_summary_reports_both_real_counts_and_history_scope() {
        compose.setContent { LithicTheme { ReviewShell(0L, subscriptionCount = 2L) } }
        compose.onNodeWithTag("review-subscription-count")
            .assertTextContains("2 条订阅服务", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("review-project-count").assertTextContains("0 条长期项目", substring = true)
        compose.onNodeWithTag("review-count-scope").assertTextContains("当前和历史", substring = true)
        compose.onNodeWithText("暂无长期项目或订阅记录").assertDoesNotExist()
    }

    @Test fun two_verified_zero_counts_have_a_distinct_empty_state() {
        compose.setContent { LithicTheme { ReviewShell(0L, subscriptionCount = 0L) } }
        compose.onNodeWithTag("review-record-summary")
            .assertTextContains("记下一件物品后，在这里看见成本变化").assertIsDisplayed()
        compose.onNodeWithTag("review-record-notice").assertDoesNotExist()
    }

    @Test fun read_notice_qualifies_zero_snapshot_without_inventing_empty_current_data() {
        compose.setContent { LithicTheme {
            ReviewShell(0L, subscriptionCount = 0L, hasReadNotice = true)
        } }
        compose.onNodeWithTag("review-record-notice")
            .assertTextContains("读取或恢复提示", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("review-record-summary")
            .assertTextContains("已取得的记录快照", substring = true)
        compose.onNodeWithText("暂无长期项目或订阅记录").assertDoesNotExist()
    }

    @Test fun all_existing_review_and_history_entries_keep_their_callbacks() {
        val opened = mutableListOf<String>()
        compose.setContent { LithicTheme {
            ReviewShell(1L, subscriptionCount = 1L,
                onProjectHistory = { opened += "projects" },
                onSubscriptionHistory = { opened += "subscriptions" },
                onLongTermReview = { opened += "ranks" },
                onTotalCostTrend = { opened += "trend" },
                onCategoryAnalysis = { opened += "categories" },
                onAnnualReview = { opened += "annual" })
        } }
        listOf("review-category-analysis", "review-annual", "review-total-cost-trend",
            "review-long-term", "review-project-history", "review-subscription-history").forEach { tag ->
            compose.onNodeWithTag(tag).performScrollTo().assertIsEnabled().performClick()
        }
        assertEquals(listOf("categories", "annual", "trend", "ranks", "projects", "subscriptions"), opened)
    }
}
