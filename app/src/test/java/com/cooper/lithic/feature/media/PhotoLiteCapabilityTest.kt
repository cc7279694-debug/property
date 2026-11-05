package com.cooper.lithic.feature.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoLiteCapabilityTest {
    @Test fun writer_requires_an_enabled_build_on_the_admitted_api() {
        assertFalse(PhotoLiteCapability.canCreateWriter(api = 36, buildEnabled = false))
        for (api in 26..35) assertFalse(PhotoLiteCapability.canCreateWriter(api, buildEnabled = true))
        assertTrue(PhotoLiteCapability.canCreateWriter(api = 36, buildEnabled = true))
        assertFalse(PhotoLiteCapability.canCreateWriter(api = 37, buildEnabled = true))
    }

    @Test fun selection_checks_for_a_picker_only_after_build_and_api_admission() {
        assertFalse(PhotoLiteCapability.canPick(api = 36, buildEnabled = false) {
            error("The picker must not be queried in this build")
        })
        assertFalse(PhotoLiteCapability.canPick(api = 35, buildEnabled = true) {
            error("The picker must not be queried on this API")
        })
        assertFalse(PhotoLiteCapability.canPick(api = 36, buildEnabled = true) { false })
        assertTrue(PhotoLiteCapability.canPick(api = 36, buildEnabled = true) { true })
    }
}
