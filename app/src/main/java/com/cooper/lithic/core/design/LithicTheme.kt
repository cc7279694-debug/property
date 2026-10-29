package com.cooper.lithic.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.cooper.lithic.feature.appearance.LithicAppearance

object LithicSpacing {
    val page: androidx.compose.ui.unit.Dp
        @Composable get() = if (LocalConfiguration.current.screenWidthDp <= 360) 16.dp else 24.dp
    val xxSmall = 4.dp
    val xSmall = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
}

internal fun lithicColorScheme(appearance: LithicAppearance): androidx.compose.material3.ColorScheme {
    val p = lithicPalette(appearance)
    val base = if (appearance == LithicAppearance.CHARCOAL) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(p.primary), onPrimary = Color(p.onPrimary),
        primaryContainer = Color(p.group), onPrimaryContainer = Color(p.text),
        secondary = Color(p.muted), onSecondary = Color(p.background),
        secondaryContainer = Color(p.group), onSecondaryContainer = Color(p.text),
        tertiary = Color(p.muted), onTertiary = Color(p.background),
        tertiaryContainer = Color(p.group), onTertiaryContainer = Color(p.text),
        background = Color(p.background), onBackground = Color(p.text),
        surface = Color(p.surface), onSurface = Color(p.text),
        surfaceVariant = Color(p.group), onSurfaceVariant = Color(p.muted),
        surfaceContainerLowest = Color(p.background), surfaceContainerLow = Color(p.surface),
        surfaceContainer = Color(p.group), surfaceContainerHigh = Color(p.group),
        surfaceContainerHighest = Color(p.group), surfaceTint = Color.Transparent,
        inverseSurface = Color(p.text), inverseOnSurface = Color(p.background), inversePrimary = Color(p.group),
        outline = Color(p.outline), outlineVariant = Color(p.divider),
        error = Color(p.error), onError = Color(p.onError),
        errorContainer = Color(p.errorSurface), onErrorContainer = Color(p.onErrorSurface),
    )
}

private val Heading = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
private val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp)
private val Auxiliary = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp)
private val LithicTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 38.sp, lineHeight = 46.sp),
    headlineLarge = Heading.copy(fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = Heading.copy(fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = Heading.copy(fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = Heading.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleMedium = Heading.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleSmall = Heading.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = Body, bodyMedium = Auxiliary, bodySmall = Auxiliary,
    labelLarge = Body.copy(fontWeight = FontWeight.Medium),
    labelMedium = Auxiliary, labelSmall = Auxiliary.copy(fontSize = 12.sp, lineHeight = 16.sp),
)

private val LithicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LithicTheme(appearance: LithicAppearance = LithicAppearance.STONE_MIST, content: @Composable () -> Unit) {
    val scheme = lithicColorScheme(appearance)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLithicVisuals provides lithicVisuals(appearance),
        androidx.compose.material3.LocalRippleConfiguration provides androidx.compose.material3.RippleConfiguration(color = scheme.onSurface),
    ) {
        MaterialTheme(colorScheme = scheme, typography = LithicTypography, shapes = LithicShapes, content = content)
    }
}

/** Compatibility for explicit historical test/preview inputs; not a fourth saved theme. */
@Composable
fun LithicTheme(darkTheme: Boolean, content: @Composable () -> Unit) = LithicTheme(
    appearance = if (darkTheme) LithicAppearance.CHARCOAL else LithicAppearance.PURE_WHITE, content = content,
)
