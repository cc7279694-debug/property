package com.cooper.lithic.data.media.photolite

import com.cooper.lithic.data.local.LongTermProjectEntity
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure admission/one-shot policy. Room CAS and Android lifecycle are separate runtime gates. */
class PhotoLiteCoverChangePolicyTest {
    @Test fun add_requires_no_existing_reference() {
        assertNull(PhotoLiteCoverChange.ADD.admissionFailure(null))
        assertEquals(PhotoLiteFailure.ALREADY_HAS_COVER, PhotoLiteCoverChange.ADD.admissionFailure(OLD))
    }

    @Test fun replace_and_remove_require_an_existing_reference() {
        for (change in listOf(PhotoLiteCoverChange.REPLACE, PhotoLiteCoverChange.REMOVE)) {
            assertEquals(PhotoLiteFailure.NO_COVER, change.admissionFailure(null))
            assertNull(change.admissionFailure(OLD))
        }
    }

    @Test fun old_add_call_and_target_default_remain_add_only() {
        val target = PhotoLiteTarget("generation", Any(), project(null))
        assertEquals(PhotoLiteCoverChange.ADD, target.change)
        assertNull(target.originalCoverAssetId)
        assertTrue(target.matchesProject(project(null)))
        assertFalse(target.matchesProject(project(OLD)))
    }

    @Test fun target_preserves_the_captured_reference_and_intent() {
        for (change in listOf(PhotoLiteCoverChange.REPLACE, PhotoLiteCoverChange.REMOVE)) {
            val row = project(OLD)
            val target = PhotoLiteTarget("generation", Any(), row, change = change)
            assertEquals(OLD, target.originalCoverAssetId)
            assertEquals(change, target.change)
            assertTrue(target.matchesProject(row))
            assertFalse(target.matchesProject(row.copy(coverAssetId = NEW)))
            assertFalse(target.matchesProject(row.copy(coverAssetId = null)))
        }
    }

    @Test fun matching_cover_does_not_allow_a_stale_business_row() {
        val row = project(OLD)
        val target = PhotoLiteTarget("generation", Any(), row, change = PhotoLiteCoverChange.REPLACE)
        val changed = listOf(row.copy(name = "changed"), row.copy(note = "changed"),
            row.copy(startedOn = row.startedOn.plusDays(1)), row.copy(endedOn = row.startedOn),
            row.copy(categoryId = NEW), row.copy(updatedAt = row.updatedAt.plusNanos(1)),
            row.copy(createdAt = row.createdAt.plusNanos(1)), row.copy(id = NEW),
            row.copy(endReason = "changed"))
        changed.forEach { assertFalse(target.matchesProject(it)) }
        assertFalse(target.matchesProject(null))
    }

    @Test fun mismatched_intent_never_acquires_write_eligibility() {
        assertFalse(PhotoLiteTarget("generation", Any(), project(OLD)).matchesProject(project(OLD)))
        for (change in listOf(PhotoLiteCoverChange.REPLACE, PhotoLiteCoverChange.REMOVE)) {
            assertFalse(PhotoLiteTarget("generation", Any(), project(null), change = change).matchesProject(project(null)))
        }
    }

    @Test fun one_target_can_only_be_claimed_once_for_every_intent() {
        PhotoLiteCoverChange.entries.forEach { change ->
            val target = PhotoLiteTarget("generation", Any(), project(if (change == PhotoLiteCoverChange.ADD) null else OLD), change = change)
            assertTrue(target.claim())
            assertFalse(target.claim())
            assertFalse(target.claim())
        }
    }

    private fun project(cover: String?): LongTermProjectEntity {
        val at = Instant.parse("2026-10-01T00:00:00Z")
        return LongTermProjectEntity("00000000-0000-4000-8000-000000000301", "合成物品", NEW,
            LocalDate.parse("2026-10-01"), coverAssetId = cover, note = "保留业务事实", createdAt = at, updatedAt = at)
    }

    private companion object {
        const val OLD = "00000000-0000-4000-8000-000000000302"
        const val NEW = "00000000-0000-4000-8000-000000000303"
    }
}
