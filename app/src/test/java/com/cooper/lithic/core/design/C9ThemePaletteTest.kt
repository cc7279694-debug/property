package com.cooper.lithic.core.design

import androidx.compose.ui.graphics.Color
import com.cooper.lithic.feature.appearance.LithicAppearance
import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class C9ThemePaletteTest {
    @Test fun three_fixed_backgrounds_and_primary_actions_match_the_approved_specification() {
        val expected = listOf(Triple(LithicAppearance.CHARCOAL, 0xFF121212, 0xFFE9E9E9),
            Triple(LithicAppearance.PURE_WHITE, 0xFFFFFFFF, 0xFF232323),
            Triple(LithicAppearance.STONE_MIST, 0xFFF7F6F2, 0xFF536F86))
        expected.forEach { (theme, background, primary) ->
            assertEquals(theme.storageId, Color(background), lithicColorScheme(theme).background)
            assertEquals(theme.storageId, Color(primary), lithicColorScheme(theme).primary)
        }
    }

    @Test fun charcoal_and_white_primary_surfaces_are_neutral_not_green_or_blue() {
        listOf(LithicAppearance.CHARCOAL, LithicAppearance.PURE_WHITE).forEach { theme ->
            val c = lithicColorScheme(theme)
            listOf(c.background, c.surface, c.surfaceContainer, c.primary, c.onPrimary).forEach {
                assertEquals(it.red, it.green, 0.0001f); assertEquals(it.green, it.blue, 0.0001f)
            }
        }
    }

    @Test fun actual_text_action_dialog_and_status_pairs_meet_text_contrast() {
        LithicAppearance.entries.forEach { theme ->
            val c = lithicColorScheme(theme); val v = lithicVisuals(theme)
            listOf(c.onBackground to c.background, c.onSurface to c.surface,
                c.onSurfaceVariant to c.background, c.onSurfaceVariant to c.surfaceContainer,
                c.onPrimary to c.primary, c.onPrimaryContainer to c.primaryContainer,
                c.onErrorContainer to c.errorContainer, c.error to c.surface,
                v.warning to v.warningSurface, v.success to v.successSurface).forEach { (foreground, background) ->
                assertTrue("${theme.storageId}: $foreground on $background", contrast(foreground, background) >= 4.5)
            }
        }
    }

    @Test fun interactive_outlines_and_all_real_chart_roles_meet_non_text_contrast() {
        LithicAppearance.entries.forEach { theme ->
            val c = lithicColorScheme(theme); val v = lithicVisuals(theme)
            listOf(v.chartTotal, v.chartProject, v.chartSubscription, v.chartGrid, c.outline).forEach { line ->
                listOf(c.background, c.surface).forEach { bg ->
                    assertTrue("${theme.storageId}: $line on $bg", contrast(line, bg) >= 3.0)
                }
            }
        }
    }

    @Test fun pressed_focus_layers_and_explicit_disabled_controls_keep_readable_pairs() {
        LithicAppearance.entries.forEach { theme ->
            val c = lithicColorScheme(theme)
            fun layered(base: Color, alpha: Float) = Color(
                c.onSurface.red * alpha + base.red * (1 - alpha),
                c.onSurface.green * alpha + base.green * (1 - alpha),
                c.onSurface.blue * alpha + base.blue * (1 - alpha), 1f)
            val pairs = listOf("primary" to contrast(c.onPrimary, c.primary),
                "pressed-primary" to contrast(c.onPrimary, layered(c.primary, .12f)),
                "focused-row-secondary" to contrast(c.onSurfaceVariant, layered(c.surfaceContainer, .12f)),
                "disabled-text" to contrast(c.onSurfaceVariant, c.surfaceContainer),
                "disabled-outline" to contrast(c.outline, c.surfaceContainer),
                "disabled-checkbox-outline" to contrast(c.onSurfaceVariant, c.surface),
                "disabled-checkbox-glyph" to contrast(c.onPrimary, c.onSurfaceVariant),
                "disabled-popup-text" to contrast(c.onSurfaceVariant, c.surfaceContainer))
            pairs.forEach { (name, ratio) ->
                println("C9_CONTRAST ${theme.storageId} $name $ratio")
                assertTrue("${theme.storageId}/$name=$ratio", ratio >= if (name == "disabled-outline") 3.0 else 4.5)
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        fun luminance(c: Color): Double {
            fun linear(v: Float) = if (v <= 0.04045f) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
            return 0.2126 * linear(c.red) + 0.7152 * linear(c.green) + 0.0722 * linear(c.blue)
        }
        val first = luminance(a); val second = luminance(b)
        return (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
    }
}
