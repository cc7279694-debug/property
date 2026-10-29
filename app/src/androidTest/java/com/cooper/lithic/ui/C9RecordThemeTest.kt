package com.cooper.lithic.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDescription
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.feature.appearance.LithicAppearance
import com.cooper.lithic.feature.home.HomeShell
import com.cooper.lithic.feature.project.ProjectDetailScreen
import com.cooper.lithic.feature.project.ProjectDetailUiState
import com.cooper.lithic.feature.project.ProjectEditorScreen
import com.cooper.lithic.feature.project.ProjectEditorUiState
import com.cooper.lithic.feature.project.SubmissionPhase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Production components with androidTest-only facts. Future execution requires M13IsolatedRunner.
 * No storage/Owner or real system font/IME is exercised; all methods currently NOT_RUN. */
class C9RecordThemeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changing_all_three_themes_keeps_home_disclosure_and_add_choices_without_writing() {
        val facts = C2VisualFixtures(populated = true)
        val appearance = mutableStateOf(LithicAppearance.STONE_MIST)
        val actions = mutableListOf<String>()
        compose.setContent {
            LithicTheme(appearance = appearance.value) {
                HomeShell(1, {}, { actions += "project" }, 1, {}, { actions += "subscription" },
                    overview = facts.overview, projectPreview = facts.projects, subscriptionPreview = facts.subscriptions)
            }
        }
        compose.onNodeWithTag("home-category-disclosure").performScrollTo().performClick()
        LithicAppearance.entries.forEach { theme ->
            compose.runOnIdle { appearance.value = theme }
            compose.onNodeWithTag("home-project-rate").performScrollTo().assertTextContains("¥")
            compose.onNodeWithTag("home-subscription-rate").performScrollTo().assertTextContains("¥")
            compose.onNodeWithTag("home-open-add").assertIsDisplayed().performClick()
            compose.onNodeWithTag("home-create-project").assertIsDisplayed()
            compose.onNodeWithTag("home-create-subscription").assertIsDisplayed()
            compose.runOnIdle { appearance.value = LithicAppearance.entries[(theme.ordinal + 1) % 3] }
            compose.onNodeWithTag("home-create-project").assertIsDisplayed()
            pressBack()
            compose.waitForIdle()
            compose.runOnIdle { assertEquals(emptyList<String>(), actions) }
        }
    }

    @Test fun long_name_negative_cost_and_full_original_facts_survive_local_large_font_theme_changes() {
        val seed = C2VisualFixtures(populated = true).projects.items.single()
        val events = seed.events.mapIndexed { index, event ->
            event.copy(amountCents = if (index == 0) 128_800 else Money.MAX_CENTS)
        }
        val snapshot = ProjectSnapshot(LongTermProject(seed.project.startedOn), events.map {
            ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn)
        })
        val detail = seed.copy(project = seed.project.copy(name = "长名称的个人物品档案与维修收回事实记录"),
            events = events, cost = ProjectCostEngine.evaluate(snapshot, seed.asOf))
        val appearance = mutableStateOf(LithicAppearance.STONE_MIST)
        compose.setContent {
            LocalLargeFontCanvas {
                LithicTheme(appearance = appearance.value) {
                    ProjectDetailScreen(ProjectDetailUiState(detail, loading = false), {}, {}, {}, {})
                }
            }
        }
        LithicAppearance.entries.forEach { theme ->
            compose.runOnIdle { appearance.value = theme }
            compose.onNodeWithTag("project-detail-name").assertTextContains(detail.project.name)
            compose.onNodeWithTag("project-detail-daily").assertContentDescriptionEquals(
                moneyMetricDescription(detail.cost.dailyRate, label = "当前日均成本"))
            compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-detail-net"))
            compose.onNodeWithTag("project-detail-net").assertTextContains("−¥")
            compose.onNodeWithTag("project-record-money").assertIsDisplayed()
            compose.onNodeWithTag("project-detail-content").performScrollToNode(hasTestTag("project-event-amount-${events.last().id}"))
            compose.waitForIdle()
            compose.onNodeWithTag("project-event-amount-${events.last().id}").assertTextContains("¥9999999999.99")
            compose.onNodeWithTag("project-detail-content").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -100_000f) }
            compose.waitForIdle()
        }
    }

    @Test fun local_large_font_theme_switch_preserves_unconfirmed_input_and_does_not_submit() {
        val facts = C2VisualFixtures(populated = true)
        val appearance = mutableStateOf(LithicAppearance.STONE_MIST)
        val state = ProjectEditorUiState(name = "保留原始输入的长名称物品", initialAmount = "148.10",
            startedOn = facts.day.toString(), initialPaidOn = facts.day.toString(), today = facts.day,
            phase = SubmissionPhase.UNCONFIRMED, dirty = true)
        var writes = 0
        compose.setContent {
            LocalLargeFontCanvas {
                LithicTheme(appearance = appearance.value) {
                    ProjectEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, { writes++ }, { writes++ })
                }
            }
        }
        LithicAppearance.entries.forEach { theme ->
            compose.runOnIdle { appearance.value = theme }
            compose.onNodeWithTag("project-name").performScrollTo().assertTextContains(state.name).assertIsNotEnabled()
            compose.onNodeWithTag("project-initial-amount").performScrollTo().assertTextContains("148.10").assertIsNotEnabled()
            compose.onNodeWithTag("project-save").assertIsDisplayed().assertTextContains("核对并重试")
            compose.runOnIdle { assertEquals(0, writes) }
        }
    }
}

@Composable
private fun LocalLargeFontCanvas(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pixelsPerDp = constraints.maxWidth / 360f
        val height = minOf(800f, constraints.maxHeight / pixelsPerDp).dp
        CompositionLocalProvider(LocalDensity provides Density(pixelsPerDp, 2f)) {
            Box(Modifier.requiredSize(360.dp, height)) { content() }
        }
    }
}
