package com.cooper.lithic.feature.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Compile-only in UI-C2 A: production Composables, no Owner, database or media access. */
@RunWith(AndroidJUnit4::class)
class C2ProjectDetailSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun closing_record_choice_does_not_invoke_a_business_route() {
        val selections = mutableListOf<MoneyDirection>()
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, {}, { selections += it }, {})
        } }
        compose.onNodeWithTag("project-add-outlay").assertDoesNotExist()
        compose.onNodeWithTag("project-record-money").performClick()
        compose.onNodeWithTag("project-money-sheet-close").performClick()
        compose.onNodeWithTag("project-money-sheet").assertDoesNotExist()
        assertEquals(emptyList<MoneyDirection>(), selections)
    }

    @Test fun record_choice_invokes_only_the_original_selected_route() {
        val selections = mutableListOf<MoneyDirection>()
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, {}, { selections += it }, {})
        } }
        compose.onNodeWithTag("project-record-money").performClick()
        compose.onNodeWithTag("project-add-recovery").performClick()
        assertEquals(listOf(MoneyDirection.RECOVERY), selections)
        compose.onNodeWithTag("project-money-sheet").assertDoesNotExist()
    }

    @Test fun more_menu_keeps_original_routes_and_does_not_delete_while_opening() {
        var edits = 0
        var deletes = 0
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false), {}, { edits++ }, {}, {},
                onDelete = { deletes++ }, onReplacement = {}, onTradeIn = {})
        } }
        compose.onNodeWithTag("project-detail-more").performClick()
        compose.onNodeWithTag("project-end").assertIsDisplayed()
        compose.onNodeWithTag("project-replacement").assertIsDisplayed()
        compose.onNodeWithTag("project-trade-in").assertIsDisplayed()
        compose.onNodeWithTag("project-delete").assertIsDisplayed()
        assertEquals(0, deletes)
        compose.onNodeWithTag("project-edit").performClick()
        assertEquals(1, edits)
        assertEquals(0, deletes)
    }

    @Test fun stale_read_error_and_real_negative_rate_stay_visible() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(), loading = false, error = "读取失败"), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-detail-error").assertTextContains("读取失败")
        compose.onNodeWithTag("project-detail-stale").assertTextContains("上次读取记录", substring = true)
        compose.onNodeWithTag("project-detail-daily").assertTextContains("−¥12.50")
    }

    @Test fun registered_unread_cover_is_not_presented_as_a_valid_image_or_no_cover() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(detail(cover = true), loading = false), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-detail-cover-unavailable").assertTextContains("封面暂不可用")
        compose.onNodeWithTag("project-detail-cover").assertDoesNotExist()
    }

    private fun detail(cover: Boolean = false): ProjectDetail {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity("72000000-0000-0000-0000-000000000002", CategoryType.PROJECT,
            "日常设备", "日常设备", false)
        return ProjectDetail(LongTermProjectEntity("72000000-0000-0000-0000-000000000001", "随身电脑", category.id,
            LocalDate.of(2026, 1, 1), coverAssetId = if (cover) "72000000-0000-0000-0000-000000000003" else null,
            createdAt = timestamp, updatedAt = timestamp), category, emptyList(),
            ProjectCostResult(8, 10000, 20000, -10000, BigDecimal("-12.50"), true, null), LocalDate.of(2026, 1, 8))
    }
}
