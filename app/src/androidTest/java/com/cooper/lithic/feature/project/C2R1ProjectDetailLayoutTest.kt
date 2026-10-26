package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production screen in a controlled 360x800 host; device execution remains NOT_RUN until authorized. */
class C2R1ProjectDetailLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val eventId = "82000000-0000-0000-0000-000000000004"

    @Test fun timeline_keeps_separate_type_amount_and_date_and_original_correction_callback() {
        var corrected: String? = null
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, {}, {}, {},
                onCorrectMoney = { corrected = it }, onCostHistory = {})
        } } }
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-event-$eventId"))
        compose.onNodeWithTag("project-event-type-$eventId").assertTextContains("初始投入")
        compose.onNodeWithTag("project-event-date-$eventId").assertTextContains("2026-01-01")
        compose.onNodeWithTag("project-event-amount-$eventId").assertTextContains("¥100.00")
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-correct-event-$eventId"))
        compose.onNodeWithTag("project-correct-event-$eventId").performClick()
        assertEquals(eventId, corrected)
    }

    @Test fun secondary_facts_and_history_remain_reachable_after_the_timeline() {
        var history = 0
        compose.setContent { LithicTheme { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, {}, {}, {}, onCostHistory = { history++ })
        } } }
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-secondary-management"))
        compose.onNodeWithTag("project-secondary-management").assertIsDisplayed()
        compose.onNodeWithTag("project-cost-history").performClick()
        assertEquals(1, history)
        compose.onNodeWithTag("project-record-money").assertIsDisplayed()
    }

    private fun detail(): ProjectDetail {
        val day = LocalDate.of(2026, 1, 1)
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity("82000000-0000-0000-0000-000000000002", CategoryType.PROJECT, "数码", "数码", false)
        val id = "82000000-0000-0000-0000-000000000001"
        val event = ProjectMoneyEventEntity(eventId, id, MoneyDirection.OUTLAY, 10000, day,
            role = ProjectMoneyEventRole.INITIAL, operationId = "82000000-0000-0000-0000-000000000005")
        return ProjectDetail(LongTermProjectEntity(id, "随身耳机", category.id, day, note = "工作与通勤",
            createdAt = instant, updatedAt = instant), category, listOf(event),
            ProjectCostResult(10, 10000, 0, 10000, BigDecimal.TEN, true, null), day.plusDays(9))
    }
}
