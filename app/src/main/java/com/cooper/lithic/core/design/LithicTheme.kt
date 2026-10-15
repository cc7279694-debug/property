package com.cooper.lithic.core.design

import androidx.compose.foundation.isSystemInDarkTheme
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

object LithicSpacing {
    val xxSmall = 4.dp
    val xSmall = 8.dp
    val small = 12.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
}

private val LightColors = lightColorScheme(
    primary = Color(0xFF365F4A), onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EFE7), onPrimaryContainer = Color(0xFF365F4A),
    secondary = Color(0xFF596A61), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF0EB), onSecondaryContainer = Color(0xFF3C5144),
    tertiary = Color(0xFF54694F), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE7EDDF), onTertiaryContainer = Color(0xFF344B30),
    background = Color(0xFFF6F5F1), onBackground = Color(0xFF17221D),
    surface = Color.White, onSurface = Color(0xFF17221D),
    surfaceVariant = Color(0xFFEDF0EB), onSurfaceVariant = Color(0xFF596A61),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9F9F5),
    surfaceContainer = Color(0xFFF0F2EC), surfaceContainerHigh = Color(0xFFEBEEE7),
    surfaceContainerHighest = Color(0xFFE5EAE2),
    surfaceTint = Color(0xFF365F4A),
    inverseSurface = Color(0xFF26362C), inverseOnSurface = Color(0xFFF1F3ED),
    inversePrimary = Color(0xFF9EBCAA),
    outline = Color(0xFF647C6D), outlineVariant = Color(0xFFDFE4DE),
    error = Color(0xFF9A3D34), onError = Color.White,
    errorContainer = Color(0xFFF9DDD8), onErrorContainer = Color(0xFF3D0D07),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EBCAA), onPrimary = Color(0xFF17221D),
    primaryContainer = Color(0xFF2D4135), onPrimaryContainer = Color(0xFFC3D9CA),
    secondary = Color(0xFFB4BFB7), onSecondary = Color(0xFF17221D),
    secondaryContainer = Color(0xFF303E34), onSecondaryContainer = Color(0xFFD5E1D7),
    tertiary = Color(0xFFB5C9AA), onTertiary = Color(0xFF1E2D1A),
    tertiaryContainer = Color(0xFF33452D), onTertiaryContainer = Color(0xFFD3E4C8),
    background = Color(0xFF111815), onBackground = Color(0xFFF1F3ED),
    surface = Color(0xFF1B2520), onSurface = Color(0xFFF1F3ED),
    surfaceVariant = Color(0xFF303E34), onSurfaceVariant = Color(0xFFB4BFB7),
    surfaceContainerLowest = Color(0xFF0C130F), surfaceContainerLow = Color(0xFF17211B),
    surfaceContainer = Color(0xFF1B2520), surfaceContainerHigh = Color(0xFF26332B),
    surfaceContainerHighest = Color(0xFF303E34),
    surfaceTint = Color(0xFF9EBCAA),
    inverseSurface = Color(0xFFE5EAE2), inverseOnSurface = Color(0xFF26362C),
    inversePrimary = Color(0xFF365F4A),
    outline = Color(0xFF667D70), outlineVariant = Color(0xFF32433A),
    error = Color(0xFFF2B6AC), onError = Color(0xFF5B1A13),
    errorContainer = Color(0xFF783027), onErrorContainer = Color(0xFFF9DDD8),
)

private val Heading = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium)
private val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp)
private val Auxiliary = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp)
private val LithicTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 38.sp, lineHeight = 46.sp),
    headlineLarge = Heading.copy(fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = Heading.copy(fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = Heading.copy(fontSize = 22.sp, lineHeight = 30.sp),
    titleLarge = Heading.copy(fontSize = 20.sp, lineHeight = 28.sp),
    titleMedium = Heading.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleSmall = Heading.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = Body, bodyMedium = Auxiliary, bodySmall = Auxiliary,
    labelLarge = Body.copy(fontWeight = FontWeight.Medium),
    labelMedium = Auxiliary, labelSmall = Auxiliary,
)

private val LithicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun LithicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LithicTypography, shapes = LithicShapes, content = content)
}
