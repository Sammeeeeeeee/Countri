package dev.sam.countri.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Countri's bespoke palette. Two moods, one geometry:
 * Midnight — near-black with a cool green undertone, visited countries glow lime.
 * Paper — warm off-white, visited countries in deep moss.
 * Flat by principle: no shadows anywhere; depth is surface steps + hairlines.
 */
@Immutable
data class CountriPalette(
    val isDark: Boolean,
    val canvas: Color,
    val surface1: Color,
    val surface2: Color,
    val hairline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textFaint: Color,
    val visited: Color,
    val visitedDim: Color,
    val onVisited: Color,
    val wishlist: Color,
    val wishlistDim: Color,
    val onWishlist: Color,
    val mapLand: Color,
    val mapBorder: Color,
    val globeShade: Color,
)

val MidnightPalette = CountriPalette(
    isDark = true,
    canvas = Color(0xFF0C110F),
    surface1 = Color(0xFF121A16),
    surface2 = Color(0xFF172019),
    hairline = Color(0x12FFFFFF),
    textPrimary = Color(0xFFE9EFE8),
    textSecondary = Color(0x8CE9EFE8),
    textFaint = Color(0x61E9EFE8),
    visited = Color(0xFFCEF79E),
    visitedDim = Color(0x24CEF79E),
    onVisited = Color(0xFF0C110F),
    wishlist = Color(0xFFC9B8F5),
    wishlistDim = Color(0x24C9B8F5),
    onWishlist = Color(0xFF0C110F),
    mapLand = Color(0xFF1C2621),
    mapBorder = Color(0x14FFFFFF),
    globeShade = Color(0xFF101713),
)

val PaperPalette = CountriPalette(
    isDark = false,
    canvas = Color(0xFFF7F7F5),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDEFE8),
    hairline = Color(0x161A211E),
    textPrimary = Color(0xFF1A211E),
    textSecondary = Color(0x8C1A211E),
    textFaint = Color(0x611A211E),
    visited = Color(0xFF4C7A1F),
    visitedDim = Color(0x1F4C7A1F),
    onVisited = Color(0xFFF7F7F5),
    wishlist = Color(0xFF7C63C8),
    wishlistDim = Color(0x1F7C63C8),
    onWishlist = Color(0xFFF7F7F5),
    mapLand = Color(0xFFE4E7DE),
    mapBorder = Color(0x1F1A211E),
    globeShade = Color(0xFFEFF1EC),
)

val LocalCountriPalette = staticCompositionLocalOf { MidnightPalette }
