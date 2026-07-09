package dev.sam.countri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun CountriTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) MidnightPalette else PaperPalette

    // Material components inherit the bespoke palette; surfaceTint is
    // transparent so nothing ever picks up an elevation wash — the system
    // is flat by principle.
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.visited,
            onPrimary = palette.onVisited,
            secondary = palette.wishlist,
            onSecondary = palette.onWishlist,
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.canvas,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surface1,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.surface1,
            surfaceContainerLow = palette.surface1,
            surfaceContainerHigh = palette.surface2,
            surfaceContainerHighest = palette.surface2,
            outline = palette.hairline,
            outlineVariant = palette.hairline,
            surfaceTint = Color.Transparent,
            scrim = Color(0xE6060907),
        )
    } else {
        lightColorScheme(
            primary = palette.visited,
            onPrimary = palette.onVisited,
            secondary = palette.wishlist,
            onSecondary = palette.onWishlist,
            background = palette.canvas,
            onBackground = palette.textPrimary,
            surface = palette.canvas,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surface1,
            onSurfaceVariant = palette.textSecondary,
            surfaceContainer = palette.surface1,
            surfaceContainerLow = palette.surface1,
            surfaceContainerHigh = palette.surface2,
            surfaceContainerHighest = palette.surface2,
            outline = palette.hairline,
            outlineVariant = palette.hairline,
            surfaceTint = Color.Transparent,
            scrim = Color(0x991A211E),
        )
    }

    val typography = Typography(
        displayLarge = CountriType.hero,
        displayMedium = CountriType.displayLarge,
        displaySmall = CountriType.display,
        headlineLarge = CountriType.displaySmall,
        headlineMedium = CountriType.title,
        headlineSmall = CountriType.subtitle,
        titleLarge = CountriType.title,
        titleMedium = CountriType.subtitle,
        titleSmall = CountriType.body,
        bodyLarge = CountriType.body,
        bodyMedium = CountriType.body,
        bodySmall = CountriType.bodySmall,
        labelLarge = CountriType.body,
        labelMedium = CountriType.mono,
        labelSmall = CountriType.monoSmall,
    )

    CompositionLocalProvider(LocalCountriPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content,
        )
    }
}

/** Convenience accessor: `Countri.palette.visited` etc. */
object Countri {
    val palette: CountriPalette
        @Composable get() = LocalCountriPalette.current
}
