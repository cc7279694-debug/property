package com.cooper.lithic.data.media.photolite

import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class PhotoLiteLimitsTest {
    @Test fun first_experiment_requires_api36_and_an_actual_system_picker() {
        for (api in listOf(26, 28, 29, 35, 37)) assertFalse(PhotoLiteLimits.supports(api, true))
        assertFalse(PhotoLiteLimits.supports(36, false))
        assertTrue(PhotoLiteLimits.supports(36, true))
    }

    @Test fun normalization_preserves_ratio_and_never_enlarges_small_content() {
        assertEquals(320 to 240, PhotoLiteLimits.normalizedSize(320, 240))
        assertEquals(640 to 240, PhotoLiteLimits.normalizedSize(1600, 600))
        assertEquals(240 to 640, PhotoLiteLimits.normalizedSize(600, 1600))
        assertEquals(1 to 640, PhotoLiteLimits.normalizedSize(1, 1600))
    }

    @Test fun returned_bitmap_bounds_allocation_and_software_identity_are_required() {
        assertTrue(PhotoLiteLimits.acceptsThumbnail(1024, 768, 1024L * 768 * 4, true))
        assertFalse(PhotoLiteLimits.acceptsThumbnail(1601, 10, 64040, true))
        assertFalse(PhotoLiteLimits.acceptsThumbnail(0, 10, 1, true))
        assertFalse(PhotoLiteLimits.acceptsThumbnail(512, 512, 0, true))
        assertFalse(PhotoLiteLimits.acceptsThumbnail(512, 512, PhotoLiteLimits.MAX_SOURCE_RASTER_BYTES + 1, true))
        assertFalse(PhotoLiteLimits.acceptsThumbnail(512, 512, 1024L * 1024, false))
    }

    @Test fun encoding_stream_rejects_the_limit_before_writing_and_retains_prior_bytes() {
        val stream = PhotoLitePngOutput(maxBytesExclusive = 10)
        stream.write(ByteArray(9) { 7 })
        try { stream.write(1); fail("The exclusive limit must not be emitted") }
        catch (_: IOException) { }
        assertArrayEquals(ByteArray(9) { 7 }, stream.toByteArray())
    }

    @Test fun custom_encoding_budget_cannot_expand_the_frozen_product_limit() {
        for (limit in listOf(-1, 0, PhotoLiteLimits.MAX_PNG_BYTES_EXCLUSIVE + 1)) {
            try { PhotoLitePngOutput(limit); fail("An expanded or invalid encoding budget must be rejected") }
            catch (_: IllegalArgumentException) { }
        }
    }
}
