package dev.sam.countri.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.sam.countri.data.catalog.Continent

/**
 * Revolut-school monochrome: white canvas, near-black ink, ash hairlines,
 * mist recessed groups. No shadows — edges do the work. The entire color
 * budget is one cobalt gradient ([aurora]) that appears exactly once in
 * the app, on the Atlas progress ribbon.
 */
@Immutable
data class CountriPalette(
    val isDark: Boolean,
    val canvas: Color,
    /** Recessed group surface (Revolut's mist #f7f7f7). */
    val surface1: Color,
    /** Card / sheet surface — separated by hairline, not elevation. */
    val surface2: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textFaint: Color,
    /** The ink accent: filled buttons, visited countries, active states. */
    val visited: Color,
    val visitedDim: Color,
    val onVisited: Color,
    /** Wishlist chrome (dashes, pills) — quiet slate. */
    val wishlist: Color,
    val wishlistDim: Color,
    val onWishlist: Color,
    val mapLand: Color,
    val mapBorder: Color,
    val globeShade: Color,
    /** Monochrome system: every continent wears ink. */
    val continents: List<Color>,
    /** The single chromatic element: cobalt promo gradient. Use once. */
    val aurora: List<Color>,
) {
    fun continentColor(continent: Continent): Color = continents[continent.ordinal]
}

private val Cobalt = listOf(Color(0xFF1227FD), Color(0xFF6FA0FF))

val PaperPalette = CountriPalette(
    isDark = false,
    canvas = Color(0xFFFFFFFF),
    surface1 = Color(0xFFF7F7F7),
    surface2 = Color(0xFFFFFFFF),
    hairline = Color(0xFFC9C9CD),
    textPrimary = Color(0xFF1F1F1F),
    textSecondary = Color(0xFF4C4C4C),
    textFaint = Color(0xFF717173),
    visited = Color(0xFF1F1F1F),
    visitedDim = Color(0x141F1F1F),
    onVisited = Color(0xFFFFFFFF),
    wishlist = Color(0xFF717173),
    wishlistDim = Color(0x14717173),
    onWishlist = Color(0xFFFFFFFF),
    mapLand = Color(0xFFEDEDEF),
    mapBorder = Color(0xFFC9C9CD),
    globeShade = Color(0xFFF2F2F4),
    continents = List(Continent.entries.size) { Color(0xFF1F1F1F) },
    aurora = Cobalt,
)

val MidnightPalette = CountriPalette(
    isDark = true,
    canvas = Color(0xFF1F1F1F),
    surface1 = Color(0xFF2A2A2C),
    surface2 = Color(0xFF262628),
    hairline = Color(0xFF3E3E42),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8B8BC),
    textFaint = Color(0xFF8A8A8E),
    visited = Color(0xFFFFFFFF),
    visitedDim = Color(0x1FFFFFFF),
    onVisited = Color(0xFF1F1F1F),
    wishlist = Color(0xFF9A9A9E),
    wishlistDim = Color(0x1F9A9A9E),
    onWishlist = Color(0xFF1F1F1F),
    mapLand = Color(0xFF2E2E31),
    mapBorder = Color(0xFF47474B),
    globeShade = Color(0xFF29292C),
    continents = List(Continent.entries.size) { Color(0xFFFFFFFF) },
    aurora = Cobalt,
)

val LocalCountriPalette = staticCompositionLocalOf { PaperPalette }
