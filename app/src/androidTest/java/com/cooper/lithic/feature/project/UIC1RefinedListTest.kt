package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.domain.project.ProjectCostEngine
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UIC1RefinedListTest {
    @get:Rule val compose = createComposeRule()

    @Test fun compact_header_exposes_first_record_without_scrolling_and_retains_create_and_back() {
        var created = 0
        var backed = 0
        var pixelsPerDp = 1f
        compose.setContent {
            pixelsPerDp = LocalDensity.current.density
            LithicTheme {
                ProjectListScreen(ProjectListUiState(listOf(detail()), 1, loading = false),
                    { backed++ }, { created++ }, {}, {}, {})
            }
        }
        val first = compose.onNodeWithTag("project-row-$PROJECT_ID").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val page = compose.onNodeWithTag("project-list-content").fetchSemanticsNode().boundsInRoot
        assertTrue("A compact header must put the first real record within the first 160dp", first.top - page.top <= 160f * pixelsPerDp)
        compose.onNodeWithTag("project-list-create").assertContentDescriptionEquals("新增长期项目")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        compose.onNodeWithTag("project-list-back").assertContentDescriptionEquals("返回现在")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        assertEquals(1, created)
        assertEquals(1, backed)
    }

    @Test fun narrow_double_font_header_keeps_title_unclipped_and_both_icon_actions_reachable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                LithicTheme {
                    Surface {
                        Box(Modifier.width(360.dp).fillMaxHeight()) {
                            ProjectListScreen(ProjectListUiState(listOf(detail()), 1, loading = false), {}, {}, {}, {}, {})
                        }
                    }
                }
            }
        }
        val titleLayouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("长期项目", useUnmergedTree = true).assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(titleLayouts) }
        assertTrue(titleLayouts.isNotEmpty())
        assertFalse("The title must wrap or fit at the user's chosen font scale", titleLayouts.single().hasVisualOverflow)
        compose.onNodeWithTag("project-list-back").assertIsDisplayed().assertContentDescriptionEquals("返回现在")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        compose.onNodeWithTag("project-list-create").assertIsDisplayed().assertContentDescriptionEquals("新增长期项目")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    private fun detail(): ProjectDetail {
        val startedOn = LocalDate.of(2026, 1, 1)
        val asOf = LocalDate.of(2026, 1, 10)
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val category = CategoryEntity(CATEGORY_ID, CategoryType.PROJECT, "数码", "数码", false)
        return ProjectDetail(
            LongTermProjectEntity(PROJECT_ID, "随身相机", CATEGORY_ID, startedOn, createdAt = now, updatedAt = now),
            category, emptyList(), ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(startedOn),
                listOf(ProjectMoneyEvent(MoneyDirection.OUTLAY, Money(100000), startedOn))), asOf), asOf)
    }

    companion object {
        private const val PROJECT_ID = "c1000000-0000-0000-0000-000000000001"
        private const val CATEGORY_ID = "c1000000-0000-0000-0000-000000000002"
    }
}
