package com.cooper.lithic.feature.home

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewEngine
import com.cooper.lithic.domain.subscription.SubscriptionOperationState
import com.cooper.lithic.domain.subscription.SubscriptionStateChange
import com.cooper.lithic.ui.C2VisualFixtures
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Component assertions only; compilation does not establish on-device behavior. */
@RunWith(AndroidJUnit4::class)
class P01HomeShellSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missing_overview_keeps_amount_and_both_counts_unknown() {
        var recordActions = 0
        compose.setContent { LithicTheme {
            HomeShell(2L, { recordActions++ }, { recordActions++ }, 0L, { recordActions++ }, { recordActions++ })
        } }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("—")
            .assertContentDescriptionEquals("当前日均成本，暂无数据").assertIsDisplayed()
        compose.onNodeWithTag("home-cutoff-date").assertDoesNotExist()
        compose.onNodeWithTag("home-project-record-count").performScrollTo()
            .assertTextEquals("—").assertContentDescriptionEquals("长期项目，数量尚未取得").assertIsDisplayed()
        compose.onNodeWithTag("home-subscription-record-count").performScrollTo()
            .assertTextEquals("—").assertContentDescriptionEquals("订阅服务，数量尚未取得").assertIsDisplayed()
        compose.onNodeWithText("已录入 2 条长期项目").assertDoesNotExist()
        compose.onNodeWithText("¥0.00", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("home-empty-guidance").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, recordActions) }
    }

    @Test fun subscription_history_summary_keeps_one_record_and_verified_zero() {
        val ledger = C2VisualFixtures(populated = true).ledger.overview
        val subscription = ledger.subscriptions.single()
        val stoppedOn = subscription.snapshot.renewals.single().paidUntilExclusive
        val stopped = subscription.copy(snapshot = subscription.snapshot.copy(
            stateChanges = listOf(SubscriptionStateChange(stoppedOn, SubscriptionOperationState.STOPPED)),
        ))
        val overview = OverviewEngine.evaluate(ledger.copy(projects = emptyList(), subscriptions = listOf(stopped)), stoppedOn)

        var recordActions = 0
        compose.setContent { LithicTheme {
            HomeShell(2L, { recordActions++ }, { recordActions++ }, 7L, { recordActions++ }, { recordActions++ },
                overview = overview)
        } }
        compose.onNodeWithTag("home-total-rate").assertTextEquals("¥0.00/天")
            .assertContentDescriptionEquals("当前日均成本，0.00元/天").assertIsDisplayed()
        compose.onNodeWithTag("home-no-active-records").assertIsDisplayed()
        compose.onNodeWithTag("home-subscription-record-count").performScrollTo()
            .assertTextEquals("1").assertContentDescriptionEquals("订阅服务，已录入数量1，包含历史").assertIsDisplayed()
        compose.onNodeWithTag("home-project-record-count").performScrollTo()
            .assertTextEquals("0").assertContentDescriptionEquals("长期项目，已录入数量0，包含历史").assertIsDisplayed()
        compose.onNodeWithText("已录入 0 条长期项目").assertDoesNotExist()
        compose.onNodeWithTag("home-cost-information").performScrollTo().performClick()
        compose.onNodeWithTag("home-count-scope").assertTextContains("已停止的订阅", substring = true)
            .assertTextContains("不是今天实际扣款", substring = true)
        compose.runOnIdle { assertEquals(0, recordActions) }
    }
}
