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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Compile-only R2 assertions. No Android layout/interaction result is claimed. */
class C2R2ProjectHierarchyTest {
    @get:Rule val compose = createComposeRule()
    private val eventId = "83000000-0000-0000-0000-000000000004"

    @Test fun no_cover_identity_is_compact_and_metrics_keep_the_exact_fact_values() {
        show()
        compose.onNodeWithTag("project-detail-category").assertTextContains("数码")
        compose.onNodeWithTag("project-detail-status").assertTextContains("持有中")
        compose.onNodeWithTag("project-core-metrics").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-daily").assertTextContains("¥10.00")
        compose.onNodeWithTag("project-detail-held-days").assertTextContains("10 天")
        val identity = compose.onNodeWithTag("project-identity-symbol").fetchSemanticsNode().boundsInRoot
        assertTrue("No photo must not create an empty hero frame", identity.height <= with(compose.density) { 65.dp.toPx() })
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-financial-totals"))
        compose.onNodeWithTag("project-detail-net").assertTextContains("¥100.00")
    }

    @Test fun dark_hierarchy_keeps_the_original_fact_correction_and_primary_action() {
        var corrected: String? = null
        show(dark = true, onCorrect = { corrected = it })
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-correct-event-$eventId"))
        compose.onNodeWithTag("project-correct-event-$eventId").performClick()
        assertEquals(eventId, corrected)
        compose.onNodeWithTag("project-record-money").assertIsDisplayed()
    }

    @Test fun an_unreadable_referenced_cover_stays_explicit_and_is_never_a_fake_photo() {
        show(missingCover = true)
        compose.onNodeWithTag("project-detail-cover-unavailable").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-cover").assertDoesNotExist()
    }

    private fun show(dark: Boolean = false, missingCover: Boolean = false, onCorrect: (String) -> Unit = {}) {
        val day = LocalDate.of(2026, 1, 1)
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity("83000000-0000-0000-0000-000000000002", CategoryType.PROJECT, "数码", "数码", false)
        val id = "83000000-0000-0000-0000-000000000001"
        val event = ProjectMoneyEventEntity(eventId, id, MoneyDirection.OUTLAY, 10000, day,
            role = ProjectMoneyEventRole.INITIAL, operationId = "83000000-0000-0000-0000-000000000005")
        val detail = ProjectDetail(LongTermProjectEntity(id, "随身耳机", category.id, day, note = "工作与通勤",
            coverAssetId = if (missingCover) "83000000-0000-0000-0000-000000000006" else null,
            createdAt = instant, updatedAt = instant), category, listOf(event),
            ProjectCostResult(10, 10000, 0, 10000, BigDecimal.TEN, true, null), day.plusDays(9))
        compose.setContent { LithicTheme(darkTheme = dark) { Box(Modifier.requiredSize(360.dp, 800.dp)) {
            ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, {}, {},
                onCorrectMoney = onCorrect, onCostHistory = {})
        } } }
    }
}
