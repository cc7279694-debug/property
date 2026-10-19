package com.cooper.lithic.data.media

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/** Class loading and JUnit's reflection boundary only: no instances, stages, or decoders. */
@RunWith(AndroidJUnit4::class)
class M10Api26TestClassLoadingPreflight {
    @Test fun natural_campaign_declared_methods_are_loadable() {
        for (name in arrayOf(
            "com.cooper.lithic.data.media.M10WorkerRetirementReviewTest",
            "com.cooper.lithic.data.media.M10JpegIntegrityTest",
            "com.cooper.lithic.data.media.M10JpegOldCoverProtectionTest",
            "com.cooper.lithic.data.media.M10StageV2CompatibilityTest",
            "com.cooper.lithic.data.media.M10Api26TestClassLoadingPreflight",
        )) {
            Class.forName(name).getDeclaredMethods()
        }
    }
}
