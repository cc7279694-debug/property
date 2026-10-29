package com.cooper.lithic.core.design

import androidx.compose.ui.graphics.Color
import com.cooper.lithic.feature.appearance.LithicAppearance
import kotlin.math.pow
import org.junit.Assert.*
import org.junit.Test

class C9R1VisualTokensTest {
    @Test fun home_focal_surface_has_a_scoped_layer_without_tinting_white_theme() {
        val expected = mapOf(LithicAppearance.CHARCOAL to 0xFF1E1E1E,
            LithicAppearance.PURE_WHITE to 0xFFFFFFFF, LithicAppearance.STONE_MIST to 0xFFE8EEF2)
        val mismatches = expected.filter { (theme, value) -> lithicVisuals(theme).brandSurface != Color(value) }
        assertTrue("Wrong home focal surfaces: ${mismatches.keys}", mismatches.isEmpty())
    }

    @Test fun real_hero_text_and_identity_icon_pairs_keep_required_contrast() {
        LithicAppearance.entries.forEach { theme ->
            val c = lithicColorScheme(theme); val v = lithicVisuals(theme)
            listOf(c.onSurface, c.onSurfaceVariant, c.error).forEach { text ->
                val ratio = contrast(text, v.brandSurface)
                println("C9R1_CONTRAST ${theme.storageId} hero-text $ratio")
                assertTrue("${theme.storageId} hero text=$ratio", ratio >= 4.5)
            }
            val icon = contrast(v.identityForeground, v.identitySurface)
            println("C9R1_CONTRAST ${theme.storageId} identity $icon")
            assertTrue("${theme.storageId} identity=$icon", icon >= 3.0)
            val focus = Color(c.onSurface.red * .12f + v.identitySurface.red * .88f,
                c.onSurface.green * .12f + v.identitySurface.green * .88f,
                c.onSurface.blue * .12f + v.identitySurface.blue * .88f, 1f)
            assertTrue("${theme.storageId} focused identity", contrast(v.identityForeground, focus) >= 3.0)
        }
    }

    @Test fun icon_identity_is_uniform_within_each_theme_and_neutral_in_mono_themes() {
        LithicAppearance.entries.forEach { theme ->
            val c = lithicColorScheme(theme); val v = lithicVisuals(theme)
            assertEquals(c.surfaceContainer, v.identitySurface)
            if (theme != LithicAppearance.STONE_MIST) {
                listOf(v.brandSurface, v.identitySurface, v.identityForeground).forEach { color ->
                    assertEquals(color.red, color.green, .0001f)
                    assertEquals(color.green, color.blue, .0001f)
                }
            }
        }
    }

    private fun contrast(a: Color, b: Color): Double {
        fun luminance(c: Color): Double {
            fun linear(v: Float) = if (v <= .04045f) v / 12.92 else ((v + .055) / 1.055).pow(2.4)
            return .2126 * linear(c.red) + .7152 * linear(c.green) + .0722 * linear(c.blue)
        }
        val first = luminance(a); val second = luminance(b)
        return (maxOf(first, second) + .05) / (minOf(first, second) + .05)
    }
}
