package com.cooper.lithic.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.domain.report.OverviewResult
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Component assertions only; compilation does not establish on-device behavior. */
@RunWith(AndroidJUnit4::class)
class P01HomeShellSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unavailable_cost_preserves_the_verified_record_count_without_inventing_zero() {
        compose.setContent { LithicTheme {
            HomeShell(2L, {}, {}, 0L, {}, {})
        } }
        compose.onNodeWithTag("home-total-rate").assertTextContains("—")
        compose.onNodeWithText("当前日均成本尚未取得").assertIsDisplayed()
        compose.onNodeWithText("已录入 2 条长期项目").assertIsDisplayed()
        compose.onNodeWithText("¥0.00").assertDoesNotExist()
    }

    @Test fun subscription_only_summary_does_not_lead_with_zero_projects() {
        val overview = OverviewResult(LocalDate.of(2026, 10, 25), BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, 0L, 1L, 0L, 1L, emptyList(), emptyList())
        compose.setContent { LithicTheme {
            HomeShell(0L, {}, {}, 1L, {}, {}, overview = overview)
        } }
        compose.onNodeWithTag("home-subscription-record-count")
            .assertTextContains("已录入 1 条订阅服务").assertIsDisplayed()
        compose.onNodeWithText("已录入 0 条长期项目").assertDoesNotExist()
        compose.onNodeWithTag("home-total-rate").assertTextContains("¥0.00")
    }
}
