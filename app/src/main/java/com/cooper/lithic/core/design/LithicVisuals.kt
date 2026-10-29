package com.cooper.lithic.core.design

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.cooper.lithic.feature.appearance.LithicAppearance

internal data class LithicPalette(
    val background: Long, val surface: Long, val group: Long, val text: Long, val muted: Long,
    val primary: Long, val onPrimary: Long, val outline: Long, val divider: Long,
    val error: Long, val onError: Long, val errorSurface: Long, val onErrorSurface: Long,
    val warning: Long, val warningSurface: Long, val success: Long, val successSurface: Long,
    val chartTotal: Long, val chartProject: Long, val chartSubscription: Long, val chartGrid: Long,
)

/** One source for material surfaces, state feedback, and the existing real charts. */
internal fun lithicPalette(appearance: LithicAppearance): LithicPalette = when (appearance) {
    LithicAppearance.CHARCOAL -> LithicPalette(
        0xFF121212, 0xFF1E1E1E, 0xFF292929, 0xFFF3F3F3, 0xFFB2B2B2,
        0xFFE9E9E9, 0xFF161616, 0xFF939393, 0xFF343434,
        0xFFFFB4AB, 0xFF501A16, 0xFF482522, 0xFFFFDAD5,
        0xFFF2C76D, 0xFF3D321C, 0xFFA6D5AE, 0xFF203A27,
        0xFFF3F3F3, 0xFFB2B2B2, 0xFF858585, 0xFF737373,
    )
    LithicAppearance.PURE_WHITE -> LithicPalette(
        0xFFFFFFFF, 0xFFFFFFFF, 0xFFF5F5F5, 0xFF171717, 0xFF5C5C5C,
        0xFF232323, 0xFFFFFFFF, 0xFF767676, 0xFFE6E6E6,
        0xFFA53030, 0xFFFFFFFF, 0xFFFDE9E7, 0xFF75221C,
        0xFF725300, 0xFFFFF1CC, 0xFF28613B, 0xFFE4F1E5,
        0xFF232323, 0xFF686868, 0xFF858585, 0xFF858585,
    )
    LithicAppearance.STONE_MIST -> LithicPalette(
        0xFFF7F6F2, 0xFFFFFFFF, 0xFFE8EEF2, 0xFF29343E, 0xFF4E5D67,
        0xFF536F86, 0xFFFFFFFF, 0xFF607381, 0xFFDDE2E5,
        0xFFA53434, 0xFFFFFFFF, 0xFFFCE9E7, 0xFF75221C,
        0xFF6B531F, 0xFFF7ECCC, 0xFF315F42, 0xFFE5EFE6,
        0xFF536F86, 0xFF687D8D, 0xFF485661, 0xFF74818B,
    )
}

internal data class LithicVisuals(
    val warning: Color, val warningSurface: Color, val success: Color, val successSurface: Color,
    val chartTotal: Color, val chartProject: Color, val chartSubscription: Color, val chartGrid: Color,
    // Home's sole focal surface; keep neutral/white analysis and detail surfaces separate.
    val brandSurface: Color,
    // Generic record identity only, never a replacement for authenticated media.
    val identitySurface: Color, val identityForeground: Color,
)

internal fun lithicVisuals(appearance: LithicAppearance): LithicVisuals = lithicPalette(appearance).let {
    LithicVisuals(Color(it.warning), Color(it.warningSurface), Color(it.success), Color(it.successSurface),
        Color(it.chartTotal), Color(it.chartProject), Color(it.chartSubscription), Color(it.chartGrid),
        brandSurface = Color(when (appearance) {
            LithicAppearance.CHARCOAL -> it.surface
            LithicAppearance.PURE_WHITE -> it.background
            LithicAppearance.STONE_MIST -> it.group
        }),
        identitySurface = Color(it.group), identityForeground = Color(it.primary))
}

internal val LocalLithicVisuals = staticCompositionLocalOf { lithicVisuals(LithicAppearance.STONE_MIST) }
