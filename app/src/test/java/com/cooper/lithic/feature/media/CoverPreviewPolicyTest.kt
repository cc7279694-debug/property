package com.cooper.lithic.feature.media

import com.cooper.lithic.data.media.CoverOwnerType
import com.cooper.lithic.data.media.CoverReference
import org.junit.Assert.*
import org.junit.Test

class CoverPreviewPolicyTest {
    private val reference = CoverReference(CoverOwnerType.PROJECT, "a", "image")

    @Test fun targets_follow_actual_pixels_and_clamp_each_dimension_without_overflow() {
        assertNull(coverPreviewTarget(0, 64))
        assertNull(coverPreviewTarget(64, -1))
        assertEquals(CoverPreviewTarget(192, 144), coverPreviewTarget(192, 144))
        assertEquals(CoverPreviewTarget(1024, 1024), coverPreviewTarget(Int.MAX_VALUE, Int.MAX_VALUE))
    }

    @Test fun sampling_bounds_both_dimensions_and_pixel_payload_at_power_of_two() {
        val tiny = coverSamplePlan(1600, 1600, CoverPreviewTarget(192, 192))
        assertEquals(16, tiny.sampleSize)
        assertEquals(100, tiny.maxWidth)
        assertEquals(100, tiny.maxHeight)
        assertEquals(40_000L, tiny.reservedBytes)
        val full = coverSamplePlan(1600, 1599, CoverPreviewTarget(1024, 1024))
        assertEquals(2, full.sampleSize)
        assertTrue(full.reservedBytes <= 4L * 1024 * 1024)
        assertTrue(full.minimumWidth <= full.maxWidth && full.minimumHeight <= full.maxHeight)
        val narrow = coverSamplePlan(1, 1600, CoverPreviewTarget(1, 1))
        assertEquals(1, narrow.maxWidth)
        assertEquals(1, narrow.maxHeight)
    }

    @Test fun invalid_source_dimensions_are_rejected_before_native_allocation() {
        listOf(0 to 1, 1 to -1, 1601 to 1, Int.MAX_VALUE to Int.MAX_VALUE).forEach { (w, h) ->
            try { coverSamplePlan(w, h, CoverPreviewTarget(64, 64)); fail("Invalid stored dimensions admitted") }
            catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun active_pixel_reservations_fail_without_waiting_and_release_only_once() {
        val budget = CoverPixelBudget(16)
        val first = requireNotNull(budget.reserve(12))
        assertNull(budget.reserve(5))
        assertNull(budget.reserve(Long.MAX_VALUE))
        assertNull(budget.reserve(0))
        first.close(); first.close()
        val all = requireNotNull(budget.reserve(16))
        assertNull(budget.reserve(1))
        all.close()
        assertNotNull(budget.reserve(16))
    }

    @Test fun owner_identity_generation_reference_and_size_all_guard_a_request() {
        val owner = String(charArrayOf('o'))
        val equalButDifferentOwner = String(charArrayOf('o'))
        val size = CoverPreviewTarget(64, 64)
        val ticket = CoverPreviewTicket("generation", owner, reference, size)
        assertTrue(ticket.matches("generation", owner, reference, size, true))
        assertFalse(ticket.matches("generation", equalButDifferentOwner, reference, size, true))
        assertFalse(ticket.matches("restored", owner, reference, size, true))
        assertFalse(ticket.matches("generation", owner, reference.copy(assetId = "replaced"), size, true))
        assertFalse(ticket.matches("generation", owner, reference.copy(type = CoverOwnerType.SUBSCRIPTION), size, true))
        assertFalse(ticket.matches("generation", owner, reference, CoverPreviewTarget(128, 128), true))
        assertFalse(ticket.matches("generation", owner, reference, size, false))
    }

    @Test fun retirement_blocks_late_delivery_and_releases_an_attached_reservation_once() {
        val owner = Any()
        val size = CoverPreviewTarget(64, 64)
        val ticket = CoverPreviewTicket("generation", owner, reference, size)
        val budget = CoverPixelBudget(12)
        assertTrue(ticket.attach(requireNotNull(budget.reserve(12))))
        ticket.retire(); ticket.retire()
        assertFalse(ticket.matches("generation", owner, reference, size, true))
        val next = requireNotNull(budget.reserve(12))
        assertFalse(ticket.attach(next))
        next.close()
        assertNotNull(budget.reserve(12))
    }
}
