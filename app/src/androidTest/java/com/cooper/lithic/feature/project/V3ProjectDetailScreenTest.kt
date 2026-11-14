package com.cooper.lithic.feature.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.AmountUnit
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.core.design.moneyMetricDisplay
import com.cooper.lithic.core.model.LongTermProject
import com.cooper.lithic.core.model.Money
import com.cooper.lithic.core.model.MoneyDirection
import com.cooper.lithic.core.model.ProjectMoneyEvent
import com.cooper.lithic.core.model.ProjectSnapshot
import com.cooper.lithic.data.local.CategoryEntity
import com.cooper.lithic.data.local.CategoryType
import com.cooper.lithic.data.local.LongTermProjectEntity
import com.cooper.lithic.data.local.MoneyBasis
import com.cooper.lithic.data.local.ProjectMoneyEventEntity
import com.cooper.lithic.data.local.ProjectMoneyEventRole
import com.cooper.lithic.data.repository.ProjectDetail
import com.cooper.lithic.domain.project.ProjectCostEngine
import com.cooper.lithic.feature.appearance.LithicAppearance
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class V3ProjectDetailScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun exactly_three_shows_every_event_without_an_expand_control() {
        val value = detail(3)
        compose.setContent { LithicTheme { ProjectDetailScreen(ready(value), {}, {}, {}, {}) } }

        value.events.forEach { event -> scrollTo("project-event-${event.id}").assertIsDisplayed() }
        compose.onNodeWithTag("project-money-all").assertDoesNotExist()
        compose.onNodeWithTag("project-event-dialog").assertDoesNotExist()
    }

    @Test fun four_events_expand_in_place_and_reset_for_same_id_in_new_generation_or_another_project() {
        val initial = detail(4)
        val active = mutableStateOf(initial)
        val generation = mutableStateOf(Any())
        val mutations = mutableListOf<String>()
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ready(active.value), {}, { mutations += "edit" }, { mutations += "add" }, {},
                onCorrectMoney = { mutations += it }, onUpdateEnd = { mutations += "end" },
                onDelete = { mutations += "delete" }, presentationIdentity = generation.value)
        } }

        val last = initial.events.last()
        val lastTag = "project-event-${last.id}"
        scrollTo("project-money-all").assertTextContains("全部")
        compose.onNodeWithTag(lastTag).assertDoesNotExist()
        compose.onNodeWithTag("project-money-all").performClick().assertTextContains("收起")
        scrollTo(lastTag).assertIsDisplayed()
        assertEquals(1, compose.onAllNodes(hasTestTag(lastTag)).fetchSemanticsNodes().size)
        compose.onNodeWithTag("project-event-dialog").assertDoesNotExist()
        scrollTo("project-money-all").performClick().assertTextContains("全部")
        compose.onNodeWithTag(lastTag).assertDoesNotExist()

        scrollTo("project-money-all").performClick().assertTextContains("收起")
        scrollTo("project-event-details-${last.id}").performClick()
        compose.onNodeWithTag("project-event-dialog").assertIsDisplayed()
        compose.runOnIdle { generation.value = Any() }
        compose.onNodeWithTag("project-event-dialog").assertDoesNotExist()
        scrollTo("project-money-all").assertTextContains("全部")
        compose.onNodeWithTag(lastTag).assertDoesNotExist()

        scrollTo("project-money-all").performClick().assertTextContains("收起")
        compose.runOnIdle { active.value = detail(4, projectId = "other") }
        scrollTo("project-money-all").assertTextContains("全部")
        compose.onNodeWithTag("project-event-${active.value.events.last().id}").assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<String>(), mutations) }
    }

    @Test fun linked_credit_keeps_its_fact_note_and_guarded_correction_after_read_only_view() {
        val value = detail(4)
        val event = value.events[1]
        val corrections = mutableListOf<String>()
        var backCount = 0
        compose.setContent { LithicTheme {
            ProjectDetailScreen(ready(value), { backCount++ }, {}, {}, {}, onCorrectMoney = { corrections += it })
        } }

        scrollTo("project-event-details-${event.id}").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithTag("project-event-basis-${event.id}", useUnmergedTree = true).assertTextEquals("非现金折抵")
        compose.onNodeWithTag("project-event-date-${event.id}", useUnmergedTree = true).assertTextEquals(event.occurredOn.toString())
        compose.onNodeWithTag("project-event-details-${event.id}").performClick()
        compose.onNodeWithTag("project-event-dialog").assertIsDisplayed()
        compose.onNodeWithText(requireNotNull(event.description)).assertIsDisplayed()
        compose.onNodeWithText("金额及日期不能单独修改", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("project-correct-event-${event.id}").assertTextContains("修改关联说明")
        compose.runOnIdle { assertEquals(emptyList<String>(), corrections) }

        pressBack()
        compose.onNodeWithTag("project-event-dialog").assertDoesNotExist()
        compose.onNodeWithTag("project-detail-screen").assertIsDisplayed()
        assertEquals(0, backCount)
        scrollTo("project-event-details-${event.id}").performClick()
        compose.onNodeWithTag("project-correct-event-${event.id}").performClick()
        compose.runOnIdle { assertEquals(listOf(event.id), corrections) }
    }

    @Test fun regular_and_large_font_preserve_all_financial_facts_and_keep_the_action_outside_the_scroll_area() {
        val value = detail(4, credit = false)
        val appearance = mutableStateOf(LithicAppearance.PURE_WHITE)
        val fontScale = mutableStateOf(1f)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                LithicTheme(appearance = appearance.value) {
                    Box(Modifier.width(360.dp).fillMaxHeight()) {
                        ProjectDetailScreen(ready(value), {}, {}, {}, {})
                    }
                }
            }
        }

        val outlay = scrollTo("project-detail-outlay").fetchSemanticsNode().positionInRoot
        val net = scrollTo("project-detail-net").fetchSemanticsNode().positionInRoot
        assertTrue("Regular-font amounts occupy separate columns", net.x > outlay.x)
        compose.runOnIdle { fontScale.value = 2f }
        LithicAppearance.entries.forEach { selected ->
            compose.runOnIdle { appearance.value = selected }
            scrollTo("project-detail-name").assertTextEquals(value.project.name)
            scrollTo("project-detail-daily").assertTextEquals(moneyMetricDisplay(value.cost.dailyRate))
            scrollTo("project-detail-outlay").assertTextEquals(display(value.cost.grossOutlayCents))
            scrollTo("project-detail-recovery").assertTextEquals(display(value.cost.recoveryCents))
            scrollTo("project-detail-net").assertTextEquals(display(value.cost.netCents))
            val content = compose.onNodeWithTag("project-detail-content").fetchSemanticsNode().boundsInRoot
            val primary = compose.onNodeWithTag("project-record-money").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
                .fetchSemanticsNode().boundsInRoot
            assertTrue("The fixed action cannot hide scrolled content", content.bottom <= primary.top)
        }
    }

    private fun scrollTo(tag: String) = compose.onNodeWithTag("project-detail-content")
        .performScrollToNode(hasTestTag(tag)).let { compose.onNodeWithTag(tag) }

    private fun ready(detail: ProjectDetail) = ProjectDetailUiState(detail, loading = false)

    private fun display(cents: Long) = moneyMetricDisplay(BigDecimal.valueOf(cents, 2), AmountUnit.YUAN)

    private fun detail(count: Int, projectId: String = "camera", credit: Boolean = true): ProjectDetail {
        val firstDay = LocalDate.of(2026, 7, 16)
        val observed = LocalDate.of(2026, 11, 13)
        val timestamp = Instant.parse("2026-07-16T00:00:00Z")
        val category = CategoryEntity("project-category", CategoryType.PROJECT, "数码", "数码", false)
        val project = LongTermProjectEntity(projectId, "合成横向相机与旅行备用镜头套装", category.id, firstDay,
            createdAt = timestamp, updatedAt = timestamp)
        val events = listOf(
            ProjectMoneyEventEntity("$projectId-1", projectId, MoneyDirection.OUTLAY, 70_000, firstDay,
                role = ProjectMoneyEventRole.INITIAL, operationId = "$projectId-init"),
            ProjectMoneyEventEntity("$projectId-2", projectId, MoneyDirection.OUTLAY, 30_000, firstDay,
                description = "旧机身折抵三百元", role = ProjectMoneyEventRole.REGULAR,
                operationId = "$projectId-exchange", basis = if (credit) MoneyBasis.TRADE_IN_CREDIT else MoneyBasis.CASH,
                exchangeGroupId = if (credit) "exchange-group" else null),
            ProjectMoneyEventEntity("$projectId-3", projectId, MoneyDirection.OUTLAY, 40_000, LocalDate.of(2026, 8, 1),
                description = "镜头维修", role = ProjectMoneyEventRole.REGULAR, operationId = "$projectId-outlay"),
            ProjectMoneyEventEntity("$projectId-4", projectId, MoneyDirection.RECOVERY, 20_000, LocalDate.of(2026, 10, 26),
                description = "配件收回", role = ProjectMoneyEventRole.REGULAR, operationId = "$projectId-recovery"),
        ).take(count)
        val cost = ProjectCostEngine.evaluate(ProjectSnapshot(LongTermProject(firstDay), events.map {
            ProjectMoneyEvent(it.direction, Money(it.amountCents), it.occurredOn)
        }), observed)
        return ProjectDetail(project, category, events, cost, observed)
    }
}
