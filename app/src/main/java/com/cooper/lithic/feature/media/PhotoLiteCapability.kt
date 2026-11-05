package com.cooper.lithic.feature.media

import android.os.Build
import com.cooper.lithic.BuildConfig
import com.cooper.lithic.data.media.photolite.PhotoLiteLimits

internal object PhotoLiteCapability {
    val enabledInBuild: Boolean
        get() = BuildConfig.PHOTO_LITE_PRODUCTION || BuildConfig.PHOTO_LITE_EXPERIMENT

    fun canCreateWriter(
        api: Int = Build.VERSION.SDK_INT,
        buildEnabled: Boolean = enabledInBuild,
    ): Boolean = buildEnabled && PhotoLiteLimits.supportsApi(api)

    fun canPick(
        api: Int = Build.VERSION.SDK_INT,
        buildEnabled: Boolean = enabledInBuild,
        photoPickerAvailable: () -> Boolean,
    ): Boolean = canCreateWriter(api, buildEnabled) && photoPickerAvailable()
}
