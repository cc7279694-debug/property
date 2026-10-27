package com.cooper.lithic.feature.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.ui.C2VisualFixtures
import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production Compose with immutable test facts; no database, media or default owner is opened. */
class C4HomePreviewSemanticsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun generic_identity_keeps_record_name_negative_cost_and_exact_open_callback() {
        val facts = C2VisualFixtures(populated = true)
        val original = facts.projects.items.single()
        val row = original.copy(project = original.project.copy(name = "手机"),
            category = original.category.copy(name = "其他"),
            cost = original.cost.copy(dailyRate = BigDecimal("-1.25")))
        var opened: String? = null
        compose.setContent { LithicTheme {
            HomeShell(1, {}, {}, 1, {}, {}, overview = facts.overview,
                projectPreview = facts.projects.copy(items = listOf(row)),
                subscriptionPreview = facts.subscriptions, onOpenProject = { opened = it })
        } }
        compose.onNodeWithTag("home-project-preview-${row.project.id}").performScrollTo()
        compose.onNodeWithText("手机", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("手", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("当前日均成本，−¥1.25，元/天", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("日均 · 元/天", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("home-project-preview-${row.project.id}").performClick()
        assertEquals(row.project.id, opened)
    }

    @Test fun empty_subscription_is_compact_and_all_records_still_opens_original_list() {
        val facts = C2VisualFixtures(populated = true)
        var lists = 0
        compose.setContent { LithicTheme {
            HomeShell(1, {}, {}, 0, { lists++ }, {}, overview = facts.overview.copy(subscriptionCount = 0,
                activeSubscriptionCount = 0, subscriptionRate = BigDecimal.ZERO,
                totalRate = facts.overview.projectRate, subscriptionCategories = emptyList()),
                projectPreview = facts.projects,
                subscriptionPreview = facts.subscriptions.copy(items = emptyList(), totalCount = 0))
        } }
        compose.onNodeWithTag("home-subscription-empty-compact").performScrollTo().assertTextEquals("暂无当前订阅")
        compose.onNodeWithText("还没有当前订阅").assertDoesNotExist()
        compose.onNodeWithTag("open-subscription-list").performClick()
        assertEquals(1, lists)
    }
}
