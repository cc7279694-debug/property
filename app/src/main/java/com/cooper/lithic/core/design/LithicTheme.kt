package com.cooper.lithic.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
    primary = Color(0xFF485E68), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E4E7), onPrimaryContainer = Color(0xFF183038),
    secondary = Color(0xFF586166), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDEE3E5), onSecondaryContainer = Color(0xFF252E32),
    tertiary = Color(0xFF59645D), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDEE6DF), onTertiaryContainer = Color(0xFF26332A),
    background = Color(0xFFF8F9F7), onBackground = Color(0xFF1B1D1B),
    surface = Color(0xFFF8F9F7), onSurface = Color(0xFF1B1D1B),
    surfaceVariant = Color(0xFFE1E5E2), onSurfaceVariant = Color(0xFF444A46),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF2F4F2),
    surfaceContainer = Color(0xFFEDF0ED), surfaceContainerHigh = Color(0xFFE8ECE8),
    surfaceContainerHighest = Color(0xFFE2E7E2),
    outline = Color(0xFF747B76), outlineVariant = Color(0xFFC4CBC5),
    error = Color(0xFF9A3D34), onError = Color.White,
    errorContainer = Color(0xFFF9DDD8), onErrorContainer = Color(0xFF3D0D07),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB0C9D3), onPrimary = Color(0xFF183038),
    primaryContainer = Color(0xFF304851), onPrimaryContainer = Color(0xFFD9E4E7),
    secondary = Color(0xFFBEC9CE), onSecondary = Color(0xFF293237),
    secondaryContainer = Color(0xFF404A4F), onSecondaryContainer = Color(0xFFDEE3E5),
    tertiary = Color(0xFFBDCBBF), onTertiary = Color(0xFF29352D),
    tertiaryContainer = Color(0xFF404D44), onTertiaryContainer = Color(0xFFDEE6DF),
    background = Color(0xFF141715), onBackground = Color(0xFFE1E6E1),
    surface = Color(0xFF141715), onSurface = Color(0xFFE1E6E1),
    surfaceVariant = Color(0xFF424A44), onSurfaceVariant = Color(0xFFC2CBC3),
    surfaceContainerLowest = Color(0xFF0E110F), surfaceContainerLow = Color(0xFF1C201D),
    surfaceContainer = Color(0xFF202521), surfaceContainerHigh = Color(0xFF2B302C),
    surfaceContainerHighest = Color(0xFF363C37),
    outline = Color(0xFF8C978E), outlineVariant = Color(0xFF424A44),
    error = Color(0xFFF2B6AC), onError = Color(0xFF5B1A13),
    errorContainer = Color(0xFF783027), onErrorContainer = Color(0xFFF9DDD8),
)

private val PageTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
    fontSize = 24.sp, lineHeight = 32.sp)
private val Body = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp)
private val Auxiliary = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp)
private val LithicTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium,
        fontSize = 38.sp, lineHeight = 46.sp),
    headlineLarge = PageTitle, headlineMedium = PageTitle, titleLarge = PageTitle,
    bodyLarge = Body, bodyMedium = Auxiliary, bodySmall = Auxiliary,
    labelLarge = Body.copy(fontWeight = FontWeight.Medium),
    labelMedium = Auxiliary, labelSmall = Auxiliary,
)

@Composable
fun LithicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = LithicTypography, content = content)
}
