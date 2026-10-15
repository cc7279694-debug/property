package com.cooper.lithic.feature.project

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cooper.lithic.core.design.LithicTheme
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

class UIC1ProjectSampleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun more_actions_keep_ending_separate_from_deleting_and_use_existing_callbacks() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val detail = ProjectDetail(LongTermProjectEntity("10000000-0000-0000-0000-000000000001", "随身相机",
            "10000000-0000-0000-0000-000000000002", LocalDate.of(2026, 1, 1), createdAt = now, updatedAt = now),
            CategoryEntity("10000000-0000-0000-0000-000000000002", CategoryType.PROJECT, "其他", "其他", true),
            emptyList(), ProjectCostResult(10, 100000, 0, 100000, BigDecimal("100"), true, null), LocalDate.of(2026, 1, 10))
        var ended = 0
        var deleted = 0
        compose.setContent { LithicTheme { ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, {}, {},
            onEnd = { ended++ }, onDelete = { deleted++ }) } }
        compose.onNodeWithTag("project-more").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("project-end").assertIsDisplayed()
        compose.onNodeWithTag("project-delete").assertIsDisplayed()
        assertEquals(0, ended)
        assertEquals(0, deleted)
        compose.onNodeWithTag("project-end").performClick()
        assertEquals(1, ended)
        assertEquals(0, deleted)
    }
}
