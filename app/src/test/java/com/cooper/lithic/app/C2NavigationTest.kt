package com.cooper.lithic.app

import com.cooper.lithic.core.model.MoneyDirection
import org.junit.Assert.*
import org.junit.Test

class C2NavigationTest {
    private val id = "82000000-0000-0000-0000-000000000001"
    @Test fun home_preview_detail_and_money_form_keep_home_return_context() {
        val detail = ProjectDetail(id, ProjectOrigin.HOME)
        val money = ProjectMoneyEntry(id, MoneyDirection.RECOVERY, ProjectOrigin.HOME)
        assertEquals(listOf(Home, detail), canonicalBackStack(detail))
        assertEquals(listOf(Home, detail, money), normalizeBackStack(canonicalBackStack(money)))
        assertEquals(Home, projectBrowseReturnAfterDelete(canonicalBackStack(ProjectDelete(id, ProjectOrigin.HOME))))
    }
    @Test fun embedded_review_events_keep_review_return_context() {
        val detail = ProjectDetail(id, ProjectOrigin.REVIEW_HOME)
        assertEquals(listOf(Home, Review, detail), normalizeBackStack(canonicalBackStack(detail)))
        val subscription = SubscriptionDetail(id, SubscriptionOrigin.REVIEW_HOME)
        assertEquals(listOf(Home, Review, subscription), normalizeBackStack(canonicalBackStack(subscription)))
    }
    @Test fun bottom_navigation_only_appears_at_the_three_main_destinations() {
        listOf(Home, Review, Settings).forEach { assertTrue(c2ShowPrimaryNavigation(it)) }
        listOf(CreateProject, ProjectDetail(id), ProjectList, NameSearch).forEach { assertFalse(c2ShowPrimaryNavigation(it)) }
    }
    @Test fun home_cost_explanation_keeps_home_as_its_return_path() {
        assertEquals(listOf(Home, CalculationExplanation), normalizeBackStack(listOf(Home, CalculationExplanation)))
    }
    @Test fun invalid_preview_identity_still_returns_home() {
        assertEquals(listOf(Home), canonicalBackStack(ProjectDetail("../bad", ProjectOrigin.HOME)))
    }
}
