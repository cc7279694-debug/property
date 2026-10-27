package com.cooper.lithic.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.*
import com.cooper.lithic.feature.project.*
import com.cooper.lithic.feature.subscription.*
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production components and immutable test-only facts; device execution remains NOT_RUN. */
class C5BrowseExperienceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unknown_categories_use_generic_identity_and_one_complete_metric() {
        var opened = 0
        compose.setContent { LithicTheme {
            Column(Modifier.width(360.dp)) {
                EntryCard("手机", "持有中", "持有121天", BigDecimal("-1.25"),
                    Modifier.testTag("c5-project-card"), onClick = { opened++ }, category = "自定义类别",
                    metricLabel = "当前日均成本")
                EntryCard("网站", "已暂停", "最近真实付款2026-10-01", BigDecimal.ZERO,
                    kind = C2Glyph.Subscription, category = "未知类别", metricLabel = "历史平均日成本")
            }
        } }
        compose.onNodeWithText("手").assertDoesNotExist()
        compose.onNodeWithText("网").assertDoesNotExist()
        compose.onNodeWithContentDescription(moneyMetricDescription(BigDecimal("-1.25"), label = "当前日均成本"),
            useUnmergedTree = true).assertTextContains("−¥1.25/天")
        compose.onNodeWithContentDescription(moneyMetricDescription(BigDecimal.ZERO, label = "历史平均日成本"),
            useUnmergedTree = true).assertTextContains("¥0.00/天")
        compose.onNodeWithTag("c5-project-card").performClick()
        assertEquals(1, opened)
    }

    @Test fun project_detail_keeps_exact_result_and_source_back_path_at_large_type() {
        val detail = C2VisualFixtures(true).projects.items.single()
        var backs = 0
        var writes = 0
        compose.setContent { LithicTheme {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                ProjectDetailScreen(ProjectDetailUiState(detail, loading = false),
                    onBack = { backs++ }, onEdit = { writes++ }, onAddMoney = { writes++ }, onRetry = {},
                    modifier = Modifier.width(360.dp), backLabel = "返回项目历史")
            }
        } }
        compose.onNodeWithTag("project-detail-daily").performScrollTo()
            .assertTextContains(moneyMetricDisplay(detail.cost.dailyRate))
        compose.onNodeWithTag("project-record-money").assertIsDisplayed().assertIsEnabled()
        compose.onNodeWithTag("project-detail-back").assertContentDescriptionEquals("返回项目历史")
            .assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, backs)
        assertEquals(0, writes)
    }

    @Test fun payment_menu_retains_exact_renewal_identity_and_current_historical_metrics() {
        val detail = C2VisualFixtures(true).subscriptions.items.single()
        val renewalId = detail.renewals.single().id
        var corrected: String? = null
        var deleted: String? = null
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                history = RenewalHistoryUiState(detail.renewals, 1, loading = false), loading = false),
                {}, {}, {}, {}, modifier = Modifier.width(400.dp),
                onCorrectRenewal = { corrected = it }, onDeleteRenewal = { deleted = it })
        } }
        compose.onNodeWithTag("subscription-detail-current-rate").performScrollTo()
            .assertTextContains(moneyMetricDisplay(detail.cost.currentRate))
        compose.onNodeWithTag("subscription-detail-historical-average").performScrollTo()
            .assertTextContains(moneyMetricDisplay(detail.cost.historicalAverage))
        compose.onNodeWithTag("subscription-renewal-actions-$renewalId").performScrollTo().performClick()
        compose.onNodeWithTag("subscription-renewal-correct-$renewalId").performClick()
        assertEquals(renewalId, corrected)
        assertEquals(null, deleted)
        compose.onNodeWithTag("subscription-renewal-actions-$renewalId").performClick()
        compose.onNodeWithTag("subscription-renewal-delete-$renewalId").performClick()
        assertEquals(renewalId, deleted)
    }
}
