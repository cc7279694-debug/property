package com.cooper.lithic.feature.project

import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cooper.lithic.app.*
import com.cooper.lithic.core.model.MoneyDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectLifecycleRoutesTest {
    private val id = "a0a0a0a0-1111-4444-8888-aaaaaaaaaaaa"
    private val eventId = "e0e0e0e0-1111-4444-8888-eeeeeeeeeeee"

    @Test fun history_has_review_parent_and_at_most_five_entries_for_every_editor() {
        val detail = ProjectDetail(id, ProjectOrigin.HISTORY)
        assertEquals(listOf(Home, Review, ProjectHistory), canonicalBackStack(ProjectHistory))
        assertEquals(listOf(Home, Review, ProjectHistory, detail), canonicalBackStack(detail))
        val editors: List<NavKey> = listOf(
            ProjectEdit(id, ProjectOrigin.HISTORY),
            ProjectMoneyEntry(id, MoneyDirection.RECOVERY, ProjectOrigin.HISTORY),
            ProjectEnd(id, ProjectOrigin.HISTORY),
            ProjectUpdateEnd(id, ProjectOrigin.HISTORY),
            ProjectMoneyCorrection(id, eventId, ProjectOrigin.HISTORY),
            ProjectDelete(id, ProjectOrigin.HISTORY),
        )
        editors.forEach { editor ->
            val expected = listOf(Home, Review, ProjectHistory, detail, editor)
            assertEquals(expected, canonicalBackStack(editor))
            assertEquals(expected, normalizeBackStack(expected))
            assertEquals(5, expected.size)
        }
    }

    @Test fun current_lifecycle_routes_keep_current_parent_and_four_entries() {
        val editors = listOf(ProjectEnd(id), ProjectUpdateEnd(id), ProjectMoneyCorrection(id, eventId), ProjectDelete(id))
        editors.forEach { editor ->
            val expected = listOf(Home, ProjectList, ProjectDetail(id), editor)
            assertEquals(expected, canonicalBackStack(editor))
            assertEquals(expected, normalizeBackStack(expected))
            assertTrue(expected.size <= 4)
        }
        assertEquals(ProjectOrigin.CURRENT, ProjectDetail(id).origin)
        assertEquals(ProjectOrigin.CURRENT, ProjectEdit(id).origin)
        assertEquals(ProjectOrigin.CURRENT, ProjectMoneyEntry(id).origin)
    }

    @Test fun mismatched_source_identity_invalid_event_and_oversized_history_restore_home() {
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, ProjectHistory, ProjectDetail(id), ProjectDelete(id, ProjectOrigin.HISTORY))))
        assertEquals(listOf(Home), normalizeBackStack(listOf(Home, Review, ProjectHistory, ProjectDetail(id, ProjectOrigin.HISTORY), ProjectEnd(id))))
        assertEquals(listOf(Home), canonicalBackStack(ProjectMoneyCorrection(id, "invalid", ProjectOrigin.HISTORY)))
        assertEquals(listOf(Home), canonicalBackStack(ProjectDelete("invalid")))
        val valid = canonicalBackStack(ProjectDelete(id, ProjectOrigin.HISTORY))
        assertEquals(listOf(Home), normalizeBackStack(valid + ProjectDelete(id, ProjectOrigin.HISTORY)))
    }
}
