package com.cooper.lithic.feature.media

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.ProjectCostResult
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LithicDatabase
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.subscription.M09SubscriptionFixtures
import com.cooper.lithic.feature.subscription.SubscriptionDetailScreen
import com.cooper.lithic.feature.subscription.SubscriptionDetailUiState
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** A missing formal detail entry is the behavior RED, without a new API or fake host. */
@RunWith(AndroidJUnit4::class)
class M10CoverEntryScreensTest {
    @get:Rule val compose = createComposeRule()

    @Test fun project_without_cover_exposes_safe_placeholder_and_add_cover_entry() {
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ProjectDetailUiState(project(), loading = false), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-cover-empty"))
        compose.onNodeWithTag("project-cover-empty").assertIsDisplayed()
        compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-cover-edit"))
        compose.onNodeWithTag("project-cover-edit").assertIsDisplayed()
            .assertTextContains("添加封面")
    }

    @Test fun subscription_without_cover_exposes_safe_placeholder_and_add_cover_entry() {
        val detail = M09SubscriptionFixtures.detail(M09SubscriptionFixtures.facts())
        compose.setContent { LithicTheme {
            SubscriptionDetailScreen(SubscriptionDetailUiState(detail = detail, loading = false), {}, {}, {}, {})
        } }
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-cover-empty"))
        compose.onNodeWithTag("subscription-cover-empty").assertIsDisplayed()
        compose.onNodeWithTag("subscription-detail-content").performScrollToNode(hasTestTag("subscription-cover-edit"))
        compose.onNodeWithTag("subscription-cover-edit").assertIsDisplayed()
            .assertTextContains("添加封面")
    }

    private fun project(): ProjectDetail {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity(LithicDatabase.PROJECT_FALLBACK_ID, CategoryType.PROJECT, "其他", "其他", true)
        return ProjectDetail(
            LongTermProjectEntity("10000000-0000-0000-0000-000000000901", "本地合成封面项目", category.id,
                LocalDate.of(2026, 1, 1), createdAt = timestamp, updatedAt = timestamp),
            category, emptyList(), ProjectCostResult(10, 15000, 0, 15000, BigDecimal("15"), true, null),
            LocalDate.of(2026, 1, 10),
        )
    }
}
