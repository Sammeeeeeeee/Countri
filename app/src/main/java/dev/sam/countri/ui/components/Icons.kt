package dev.sam.countri.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn 24x24 icons: filled Revolut-style glyphs for the tabs,
 * 2.2-stroke linework for utilities — the app's entire icon set,
 * weighted like Revolut's product glyphs and tinted by Icon().
 */
object CountriIcons {
    // ---- tab glyphs: filled, Revolut-style ----

    val Atlas: ImageVector by lazy {
        ImageVector.Builder("atlas", 24.dp, 24.dp, 24f, 24f).apply {
            // Solid globe with punched equator and meridian slits.
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                moveTo(12f, 2.6f)
                arcTo(9.4f, 9.4f, 0f, true, true, 11.99f, 2.6f)
                close()
                // equator slit
                moveTo(2.8f, 11.1f)
                lineTo(21.2f, 11.1f)
                lineTo(21.2f, 12.9f)
                lineTo(2.8f, 12.9f)
                close()
                // meridian slit
                moveTo(11.1f, 2.8f)
                curveTo(8.2f, 5.8f, 8.2f, 18.2f, 11.1f, 21.2f)
                lineTo(12.9f, 21.2f)
                curveTo(10f, 18.2f, 10f, 5.8f, 12.9f, 2.8f)
                close()
            }
        }.build()
    }

    val Passport: ImageVector by lazy {
        ImageVector.Builder("passport", 24.dp, 24.dp, 24f, 24f).apply {
            // Solid booklet, punched portrait circle and name line.
            path(fill = SolidColor(Color.White), pathFillType = PathFillType.EvenOdd) {
                moveTo(7f, 2.6f)
                lineTo(17f, 2.6f)
                arcTo(2.4f, 2.4f, 0f, false, true, 19.4f, 5f)
                lineTo(19.4f, 19f)
                arcTo(2.4f, 2.4f, 0f, false, true, 17f, 21.4f)
                lineTo(7f, 21.4f)
                arcTo(2.4f, 2.4f, 0f, false, true, 4.6f, 19f)
                lineTo(4.6f, 5f)
                arcTo(2.4f, 2.4f, 0f, false, true, 7f, 2.6f)
                close()
                moveTo(12f, 6.6f)
                arcTo(3.1f, 3.1f, 0f, true, true, 11.99f, 6.6f)
                close()
                moveTo(8.6f, 15.4f)
                lineTo(15.4f, 15.4f)
                lineTo(15.4f, 17.2f)
                lineTo(8.6f, 17.2f)
                close()
            }
        }.build()
    }

    val Stats: ImageVector by lazy {
        ImageVector.Builder("stats", 24.dp, 24.dp, 24f, 24f).apply {
            // Three solid rounded bars.
            path(fill = SolidColor(Color.White)) {
                moveTo(4.4f, 11f)
                lineTo(7.6f, 11f)
                arcTo(0.9f, 0.9f, 0f, false, true, 8.5f, 11.9f)
                lineTo(8.5f, 19.6f)
                arcTo(0.9f, 0.9f, 0f, false, true, 7.6f, 20.5f)
                lineTo(4.4f, 20.5f)
                arcTo(0.9f, 0.9f, 0f, false, true, 3.5f, 19.6f)
                lineTo(3.5f, 11.9f)
                arcTo(0.9f, 0.9f, 0f, false, true, 4.4f, 11f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(10.4f, 3.5f)
                lineTo(13.6f, 3.5f)
                arcTo(0.9f, 0.9f, 0f, false, true, 14.5f, 4.4f)
                lineTo(14.5f, 19.6f)
                arcTo(0.9f, 0.9f, 0f, false, true, 13.6f, 20.5f)
                lineTo(10.4f, 20.5f)
                arcTo(0.9f, 0.9f, 0f, false, true, 9.5f, 19.6f)
                lineTo(9.5f, 4.4f)
                arcTo(0.9f, 0.9f, 0f, false, true, 10.4f, 3.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(16.4f, 13.5f)
                lineTo(19.6f, 13.5f)
                arcTo(0.9f, 0.9f, 0f, false, true, 20.5f, 14.4f)
                lineTo(20.5f, 19.6f)
                arcTo(0.9f, 0.9f, 0f, false, true, 19.6f, 20.5f)
                lineTo(16.4f, 20.5f)
                arcTo(0.9f, 0.9f, 0f, false, true, 15.5f, 19.6f)
                lineTo(15.5f, 14.4f)
                arcTo(0.9f, 0.9f, 0f, false, true, 16.4f, 13.5f)
                close()
            }
        }.build()
    }

    val Wishlist: ImageVector by lazy {
        ImageVector.Builder("wishlist", 24.dp, 24.dp, 24f, 24f).apply {
            // Solid bookmark.
            path(fill = SolidColor(Color.White)) {
                moveTo(6.4f, 2.6f)
                lineTo(17.6f, 2.6f)
                arcTo(1.8f, 1.8f, 0f, false, true, 19.4f, 4.4f)
                lineTo(19.4f, 21.4f)
                lineTo(12f, 17.1f)
                lineTo(4.6f, 21.4f)
                lineTo(4.6f, 4.4f)
                arcTo(1.8f, 1.8f, 0f, false, true, 6.4f, 2.6f)
                close()
            }
        }.build()
    }

    val Plus: ImageVector by lazy {
        ImageVector.Builder("plus", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.5f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 5f); lineTo(12f, 19f)
                moveTo(5f, 12f); lineTo(19f, 12f)
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder("search", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(11f, 4f)
                arcTo(7f, 7f, 0f, true, true, 10.99f, 4f)
                moveTo(20.5f, 20.5f); lineTo(16.2f, 16.2f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder("close", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(6.5f, 6.5f); lineTo(17.5f, 17.5f)
                moveTo(17.5f, 6.5f); lineTo(6.5f, 17.5f)
            }
        }.build()
    }

    val Back: ImageVector by lazy {
        ImageVector.Builder("back", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(14.5f, 6f); lineTo(8.5f, 12f); lineTo(14.5f, 18f)
            }
        }.build()
    }

    val Chevron: ImageVector by lazy {
        ImageVector.Builder("chevron", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(9.5f, 6f); lineTo(15.5f, 12f); lineTo(9.5f, 18f)
            }
        }.build()
    }

    val Share: ImageVector by lazy {
        ImageVector.Builder("share", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(12f, 3.5f); lineTo(12f, 14f)
                moveTo(8.5f, 6.5f); lineTo(12f, 3f); lineTo(15.5f, 6.5f)
                moveTo(6f, 11f); lineTo(5f, 11f); lineTo(5f, 20f); lineTo(19f, 20f); lineTo(19f, 11f); lineTo(18f, 11f)
            }
        }.build()
    }

    val Edit: ImageVector by lazy {
        ImageVector.Builder("edit", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(4f, 20f); lineTo(8f, 19f); lineTo(19.5f, 7.5f)
                curveTo(20.3f, 6.7f, 20.3f, 5.4f, 19.5f, 4.6f)
                curveTo(18.7f, 3.8f, 17.4f, 3.8f, 16.6f, 4.6f)
                lineTo(5f, 16f)
                close()
            }
        }.build()
    }
}
