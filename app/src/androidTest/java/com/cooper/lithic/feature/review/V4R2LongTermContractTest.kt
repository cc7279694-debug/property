package com.cooper.lithic.feature.review

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.repository.ReviewRankItem
import com.cooper.lithic.data.repository.ReviewRanking
import com.cooper.lithic.domain.report.OverviewProject
import com.cooper.lithic.domain.report.RankingEngine
import com.cooper.lithic.feature.appearance.LithicAppearance
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class V4R2LongTermContractTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 11, 23)

    @Test fun preserve_real_ranks_identity_paging_and_selection_across_themes_widths_and_font_scales() {
        val appearance = mutableStateOf(LithicAppearance.entries.first())
        val width = mutableStateOf(320)
        val fontScale = mutableStateOf(1f)
        var opened: String? = null
        val selections = mutableListOf<ReviewRanking>()
        var pages = 0
        val projectNames = mapOf("ranked" to "有资格的长期项目", "future" to "还未开始的长期项目")
        val projects = projectNames.keys.map { id ->
            val start = if (id == "ranked") day.minusDays(5) else day.plusDays(1)
            OverviewProject(id, "category", ProjectSnapshot(LongTermProject(start), emptyList()))
        }
        val items = RankingEngine.projectRanks(projects, day).map { project ->
            ReviewRankItem(CategoryType.PROJECT, project.id, projectNames.getValue(project.id), "合成器材", project = project)
        }
        val state = mutableStateOf(ReviewUiState(
            items = items, totalCount = 3, rankableCount = 1, asOf = day, hasMore = true,
        ))
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val density = constraints.maxWidth / width.value.toFloat()
                val measuredHeight = minOf(780f, constraints.maxHeight / density).dp
                CompositionLocalProvider(LocalDensity provides Density(density, fontScale.value)) {
                    LithicTheme(appearance.value) {
                        Box(Modifier.requiredSize(width.value.dp, measuredHeight)) {
                            LongTermReviewScreen(state.value, {}, { ranking ->
                                selections += ranking
                                state.value = state.value.copy(ranking = ranking,
                                    items = if (ranking == ReviewRanking.SUBSCRIPTION_TIME) emptyList() else items)
                            }, {}, { pages++ }, { opened = it }, {})
                        }
                    }
                }
            }
        }
        for (theme in LithicAppearance.entries) {
            for (dp in listOf(320, 360, 400)) {
                for (scale in listOf(1f, 2f)) {
                    compose.runOnIdle { appearance.value = theme; width.value = dp; fontScale.value = scale }
                    scroll("review-type-project").assertIsSelected().assertHeightIsAtLeast(48.dp)
                    scroll("review-row-ranked").assertTextContains("1. 有资格的长期项目", substring = true)
                    scroll("review-row-future").assertTextContains("未排名", substring = true)
                        .assertTextContains("尚无已过去的持有日期", substring = true)
                    scroll("review-ranks-load-more").assertHeightIsAtLeast(48.dp)
                }
            }
        }
        scroll("review-row-ranked").performClick()
        scroll("review-ranks-load-more").performClick()
        scroll("review-rank-PROJECT_COST").performClick()
        scroll("review-rank-PROJECT_COST").assertIsSelected()
        scroll("review-rank-SUBSCRIPTION_TIME").performClick()
        scroll("review-rank-SUBSCRIPTION_TIME").assertIsSelected()
        scroll("review-subscription-time-heading").assertIsDisplayed()
        scroll("review-type-project").performClick()
        compose.runOnIdle {
            assertEquals("ranked", opened)
            assertEquals(1, pages)
            assertEquals(listOf(ReviewRanking.PROJECT_COST, ReviewRanking.SUBSCRIPTION_TIME,
                ReviewRanking.PROJECT_TIME), selections)
        }
    }

    private fun scroll(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("review-ranks-screen").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }
}
