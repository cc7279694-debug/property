package com.cooper.lithic.feature.reminder

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.core.design.LithicTheme
import com.cooper.lithic.data.repository.MilestoneNoticeRef
import com.cooper.lithic.domain.reminder.MilestoneCandidate
import com.cooper.lithic.domain.reminder.MilestoneOwnerType
import com.cooper.lithic.domain.reminder.MilestonePlanner
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Isolated Compose host and synthetic resolver only; not real notification delivery. */
@RunWith(AndroidJUnit4::class)
class M21MilestoneSourcesUiTest {
    @get:Rule val compose = createComposeRule()
    private val day = LocalDate.of(2026, 10, 22)
    private fun ref(count: Int) = MilestoneNoticeRef("50c79a10-bc1e-4a99-9d7a-314a5e757865", day, "a".repeat(64), count)
    private fun row(number: Int, type: MilestoneOwnerType = MilestoneOwnerType.PROJECT): MilestoneCandidate {
        val id = UUID(0L, number.toLong()).toString()
        return MilestoneCandidate(type, id, "D100", 100, day,
            MilestonePlanner.logicalKey(type, id, "D100"), "合成来源$number", "于${day}达到100日节点")
    }

    @Before fun plainApplicationOnly() {
        assertEquals(Application::class.java, ApplicationProvider.getApplicationContext<Context>().applicationContext.javaClass)
    }

    @Test fun summary_keeps_more_than_50_sources_and_expands_its_display_window() {
        val rows = (1..51).map { row(it) }
        compose.setContent { LithicTheme {
            MilestoneSourcesScreen(ref(51), emptyFlow(), { rows }, false, {}, {}, {})
        } }
        compose.waitUntil { compose.onAllNodes(hasTestTag("milestone-sources-loading")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").assertDoesNotExist()
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasText("继续查看"))
        compose.onNode(hasText("继续查看")).performClick()
        compose.onNodeWithTag("milestone-sources").performScrollToNode(hasTestTag("milestone-source-${rows.last().logicalKey}"))
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").assertIsDisplayed()
    }

    @Test fun same_uuid_cross_type_row_revalidates_and_opens_the_exact_type() {
        val rows = listOf(row(1), row(1, MilestoneOwnerType.SUBSCRIPTION))
        val calls = AtomicInteger()
        val selected = AtomicReference<MilestoneCandidate?>()
        compose.setContent { LithicTheme {
            MilestoneSourcesScreen(ref(2), emptyFlow(), { calls.incrementAndGet(); rows }, false, {}, selected::set, {})
        } }
        compose.waitUntil { calls.get() >= 1 && compose.onAllNodes(hasTestTag("milestone-sources-loading")).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("milestone-source-${rows.last().logicalKey}").performClick()
        compose.waitUntil { selected.get() != null }
        assertEquals(MilestoneOwnerType.SUBSCRIPTION, selected.get()?.ownerType)
        assertEquals(rows.last().ownerId, selected.get()?.ownerId)
        assertTrue(calls.get() >= 2)
    }

    @Test fun stale_or_deleted_summary_is_an_explicit_error_and_never_an_empty_success() {
        compose.setContent { LithicTheme {
            MilestoneSourcesScreen(ref(1), emptyFlow(), { error("Old generation or corrected source") }, false, {}, {}, {})
        } }
        compose.waitUntil { compose.onAllNodes(hasTestTag("milestone-sources-error")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("milestone-sources-error").assertIsDisplayed()
    }

    @Test fun single_source_opens_once_after_revalidation_and_does_not_repeat_on_recomposition() {
        val opens = AtomicInteger()
        compose.setContent { LithicTheme {
            var automatic by remember { mutableStateOf(true) }
            MilestoneSourcesScreen(ref(1), emptyFlow(), { listOf(row(1)) }, automatic,
                { automatic = false }, { opens.incrementAndGet() }, {})
        } }
        compose.waitUntil { opens.get() == 1 }
        compose.waitForIdle()
        assertEquals(1, opens.get())
    }
}
