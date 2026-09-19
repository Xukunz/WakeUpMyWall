package com.xukunz.wakeupmywall.core.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ThemeAccentTest {

    private val concreteAccents = ThemeAccent.entries.filter { it != ThemeAccent.Dynamic }

    @Test
    fun `every accent provides readable onAccent color`() {
        concreteAccents.forEach { accent ->
            val palette = accent.palette()
            val ratio = contrastRatio(palette.accent, palette.onAccent)
            assertTrue(ratio >= 4.5, "$accent contrast was $ratio")
        }
    }

    @Test
    fun `online color is distinct from accent for every theme`() {
        concreteAccents.forEach { accent ->
            assertTrue(accent.palette().onlineColor != accent.palette().accent, "$accent")
        }
    }

    @Test
    fun `palettes are pairwise distinct`() {
        val accents = concreteAccents.map { it.palette().accent }
        assertTrue(accents.toSet().size == accents.size, "duplicate accent colors: $accents")
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance()
        val lb = b.luminance()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun Color.luminance(): Double {
        val r = linear(red)
        val g = linear(green)
        val b = linear(blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linear(channel: Float): Double {
        val v = channel.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }
}
