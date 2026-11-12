package com.cooper.lithic.feature.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.ui.C2VisualFixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Synthetic facts only. The separate StorageUiStateTest fixture exercises the global Ready retry. */
class P2HomeReadPresentationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun retained_summary_keeps_amount_date_and_recorded_counts_from_one_snapshot() {
        val facts = C2VisualFixtures(populated = true)
        val overview = facts.overview.copy(projectCount = 8, subscriptionCount = 5)
        compose.setContent {
            LithicTheme {
                HomeShell(99, {}, {}, 99, {}, {}, overview = overview, hasReadNotice = true,
                    readError = "本地记录暂时无法刷新，当前显示上次读取结果。")
            }
        }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥10.52/天")
        compose.onNodeWithTag("home-cutoff-date").assertTextEquals(overview.asOf.toString())
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("8")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("5")
        compose.onNodeWithTag("home-retained-notice").assertTextEquals("上次取得的结果")
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-overview-read-error").assertDoesNotExist()
    }

    @Test fun recovery_notice_guards_an_empty_snapshot_without_implying_refresh_failure() {
        val overview = C2VisualFixtures(populated = false).overview
        compose.setContent {
            LithicTheme {
                HomeShell(99, {}, {}, 99, {}, {}, overview = overview, hasReadNotice = true,
                    readError = "当前数据已打开；未能安全清理的旧资源已保留。")
            }
        }
        compose.onNodeWithTag("home-retained-notice").assertTextEquals("当前记录状态尚未确认")
        compose.onNodeWithTag("home-overview-read-error").assertDoesNotExist()
        compose.onNodeWithText("上次取得的结果").assertDoesNotExist()
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("0")
    }

    @Test fun historical_zero_and_confirmed_empty_are_separate() {
        val empty = C2VisualFixtures(populated = false)
        val overview = mutableStateOf(empty.overview.copy(projectCount = 4, subscriptionCount = 3))
        compose.setContent {
            LithicTheme {
                HomeShell(99, {}, {}, 99, {}, {}, overview = overview.value,
                    projectPreview = empty.projects, subscriptionPreview = empty.subscriptions)
            }
        }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥0.00/天")
        compose.onNodeWithTag("home-no-active-records").assertExists()
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("4")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("3")
        compose.onNodeWithTag("open-project-list").assertExists()
        compose.onNodeWithTag("home-subscription-tab").performScrollTo().performClick()
        compose.onNodeWithTag("open-subscription-list").assertExists()
        compose.onNodeWithTag("home-category-disclosure").assertExists()
        compose.runOnIdle { overview.value = empty.overview }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("—")
        compose.onNodeWithTag("home-project-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-subscription-record-count").assertTextEquals("0")
        compose.onNodeWithTag("home-empty-guidance").assertExists()
        compose.onNodeWithTag("home-no-active-records").assertDoesNotExist()
        compose.onNodeWithTag("open-project-list").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").assertDoesNotExist()
        compose.onNodeWithTag("home-category-disclosure").assertDoesNotExist()
        compose.onAllNodesWithTag("home-open-add").assertCountEquals(1)
        compose.onAllNodesWithTag("home-open-add-top").assertCountEquals(1)
    }

    @Test fun preview_retries_are_independent_and_only_the_failed_scope_is_marked_retained() {
        val facts = C2VisualFixtures(populated = true)
        val subscriptionError = mutableStateOf<String?>(null)
        var projectRetries = 0
        var subscriptionRetries = 0
        compose.setContent {
            LithicTheme {
                HomeShell(99, {}, {}, 99, {}, {}, overview = facts.overview,
                    projectPreview = facts.projects.copy(error = "合成项目预览失败"),
                    subscriptionPreview = facts.subscriptions.copy(error = subscriptionError.value,
                        items = if (subscriptionError.value == null) facts.subscriptions.items else emptyList()),
                    onRetryProjectPreview = { projectRetries++ },
                    onRetrySubscriptionPreview = { subscriptionRetries++ })
            }
        }
        val projectId = facts.projects.items.single().project.id
        val subscriptionId = facts.subscriptions.items.single().subscription.id
        compose.onNodeWithTag("home-project-preview-$projectId").assertExists()
        compose.onAllNodesWithText("以下保留上次读取结果").assertCountEquals(1)
        compose.onNodeWithTag("home-retry-subscription-preview").assertDoesNotExist()
        compose.onNodeWithTag("home-retry-project-preview").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, projectRetries)
            assertEquals(0, subscriptionRetries)
        }
        compose.onNodeWithTag("home-subscription-tab").performScrollTo().performClick()
        compose.onNodeWithTag("home-subscription-preview-$subscriptionId").assertExists()
        compose.onNodeWithTag("home-project-preview-$projectId").assertDoesNotExist()
        compose.runOnIdle { subscriptionError.value = "合成订阅预览失败" }
        compose.onNodeWithTag("home-subscription-preview-$subscriptionId").assertDoesNotExist()
        compose.onNodeWithTag("home-retry-subscription-preview").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, projectRetries)
            assertEquals(1, subscriptionRetries)
        }
        compose.onNodeWithTag("home-overview-read-error").assertDoesNotExist()
        compose.onNodeWithTag("home-retained-notice").assertDoesNotExist()
        compose.onNodeWithTag("ledger-retry-read").assertDoesNotExist()
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥10.52/天")
    }
}
