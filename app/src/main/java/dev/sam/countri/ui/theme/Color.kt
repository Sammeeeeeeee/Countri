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
    /** Page background — Revolut's recessed gray, cards float white on it. */
    val canvas: Color,
    /** Cards, rows, trays, the nav pill — white on the gray canvas. */
    val surface1: Color,
    /** Sheets and dialogs. */
    val surface2: Color,
    /** Recessed wells inside white surfaces: chips, fields, icon circles. */
    val recessed: Color,
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
    canvas = Color(0xFFF2F2F4),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFFFFFFF),
    recessed = Color(0xFFF2F2F4),
    hairline = Color(0xFFE3E3E6),
    textPrimary = Color(0xFF1F1F1F),
    textSecondary = Color(0xFF4C4C4C),
    textFaint = Color(0xFF717173),
    visited = Color(0xFF1F1F1F),
    visitedDim = Color(0x141F1F1F),
    onVisited = Color(0xFFFFFFFF),
    wishlist = Color(0xFF717173),
    wishlistDim = Color(0x14717173),
    onWishlist = Color(0xFFFFFFFF),
    mapLand = Color(0xFFE3E3E7),
    mapBorder = Color(0xFFC9C9CD),
    globeShade = Color(0xFFEAEAED),
    continents = List(Continent.entries.size) { Color(0xFF1F1F1F) },
    aurora = Cobalt,
)

val MidnightPalette = CountriPalette(
    isDark = true,
    canvas = Color(0xFF141416),
    surface1 = Color(0xFF1F1F22),
    surface2 = Color(0xFF1F1F22),
    recessed = Color(0xFF2C2C30),
    hairline = Color(0xFF323236),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB8B8BC),
    textFaint = Color(0xFF8A8A8E),
    visited = Color(0xFFFFFFFF),
    visitedDim = Color(0x1FFFFFFF),
    onVisited = Color(0xFF141416),
    wishlist = Color(0xFF9A9A9E),
    wishlistDim = Color(0x1F9A9A9E),
    onWishlist = Color(0xFF141416),
    mapLand = Color(0xFF26262A),
    mapBorder = Color(0xFF3C3C41),
    globeShade = Color(0xFF1D1D20),
    continents = List(Continent.entries.size) { Color(0xFFFFFFFF) },
    aurora = Cobalt,
)

val LocalCountriPalette = staticCompositionLocalOf { PaperPalette }
