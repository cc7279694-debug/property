package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.subscription.M09SubscriptionFixtures
import com.cooper.lithic.feature.subscription.RenewalHistoryUiState
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Source prepared only: synthetic immutable facts, no Owner, media, database or Android execution.
 * 360dp/200% is a supplied Compose density, not a device font or visual acceptance result. */
class C9R1DetailSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun long_project_name_and_full_negative_metrics_keep_their_identity_at_large_type() {
        val detail = project(recoveryCents = Money.MAX_CENTS)
        compose.setContent { LithicTheme { SmallLargeTextHost {
            ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, {}, {})
        } } }
        compose.onNodeWithTag("project-detail-name").assertTextEquals(detail.project.name)
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-held-days"))
        compose.onNodeWithTag("project-detail-held-days").assertTextEquals("${detail.cost.heldDays}天")
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-daily"))
        compose.onNodeWithTag("project-detail-daily").assertTextEquals(moneyMetricDisplay(detail.cost.dailyRate))
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-net"))
        compose.onNodeWithTag("project-detail-net").assertTextEquals(
            moneyMetricDisplay(BigDecimal.valueOf(detail.cost.netCents, 2), AmountUnit.YUAN))
        compose.onNodeWithTag("project-record-money").assertIsDisplayed().assertIsEnabled()
    }

    @Test fun theme_switch_keeps_true_zero_and_the_selected_record_identity_without_business_actions() {
        val detail = project(recoveryCents = 100)
        val appearance = mutableStateOf(LithicAppearance.STONE_MIST)
        val selections = mutableListOf<MoneyDirection>()
        compose.setContent { LithicTheme(appearance = appearance.value) {
            ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, { selections += it }, {})
        } }
        LithicAppearance.entries.forEach { selected ->
            compose.runOnIdle { appearance.value = selected }
            compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-daily"))
            compose.onNodeWithTag("project-detail-daily").assertTextEquals(moneyMetricDisplay(BigDecimal.ZERO))
            compose.runOnIdle { assertEquals(emptyList<MoneyDirection>(), selections) }
        }
        compose.onNodeWithTag("project-record-money").performClick()
        compose.onNodeWithTag("project-add-recovery").performClick()
        assertEquals(listOf(MoneyDirection.RECOVERY), selections)
    }

    @Test fun subscription_keeps_average_purchased_days_and_real_future_payment_separate_at_large_type() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts(
            name = "合成长名称阅读服务与个人资料订阅", futurePayment = true))
        val future = detail.renewals.single { it.effectiveFrom.isAfter(detail.asOf) }
        var corrections: String? = null
        var deletions: String? = null
        compose.setContent { LithicTheme { SmallLargeTextHost {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail,
                RenewalHistoryUiState(detail.renewals, detail.renewals.size.toLong(), loading = false), loading = false),
                {}, {}, {}, {}, onCorrectRenewal = { corrections = it }, onDeleteRenewal = { deletions = it })
        } } }
        compose.onNodeWithTag("subscription-detail-name").assertTextEquals(detail.subscription.name)
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-detail-historical-average"))
        compose.onNodeWithTag("subscription-detail-historical-average").assertTextEquals(moneyMetricDisplay(detail.cost.historicalAverage))
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-detail-purchased-days"))
        compose.onNodeWithTag("subscription-detail-purchased-days").assertTextEquals("折算付费时长：60天")
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-detail-current-rate"))
        compose.onNodeWithTag("subscription-detail-current-rate").assertTextEquals(moneyMetricDisplay(detail.cost.currentRate))
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-renewal-paid-on-${future.id}"))
        compose.onNodeWithTag("subscription-renewal-paid-on-${future.id}").assertTextEquals("真实付款日期：${future.paidOn}")
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-renewal-amount-${future.id}"))
        compose.onNodeWithTag("subscription-renewal-amount-${future.id}").assertTextEquals(
            moneyMetricDisplay(BigDecimal.valueOf(future.amountCents, 2), AmountUnit.YUAN))
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-renewal-future-${future.id}"))
        compose.onNodeWithTag("subscription-renewal-future-${future.id}").assertTextEquals("尚未生效")
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-renewal-actions-${future.id}"))
        compose.onNodeWithTag("subscription-renewal-actions-${future.id}").performClick()
        compose.onNodeWithTag("subscription-renewal-correct-${future.id}").performClick()
        assertEquals(future.id, corrections)
        assertEquals(null, deletions)
    }

    private fun project(recoveryCents: Long): ProjectDetail {
        val source = C2VisualFixtures(true).projects.items.single()
        val events = source.events.map { event ->
            event.copy(amountCents = if (event.direction == MoneyDirection.OUTLAY) 100 else recoveryCents)
        }
        val cost = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(source.project.startedOn), events.map {
            ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn)
        }), source.asOf)
        return source.copy(project = source.project.copy(name = "合成长名称随身阅读器与长期拥有的个人物品档案"), events = events, cost = cost)
    }

    @Composable
    private fun SmallLargeTextHost(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
            Box(Modifier.requiredSize(360.dp, 800.dp)) { content() }
        }
    }
}
