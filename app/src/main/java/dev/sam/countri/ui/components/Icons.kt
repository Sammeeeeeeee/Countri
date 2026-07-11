package dev.sam.countri.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn 24x24 line icons, 2.2 stroke — the app's entire icon set,
 * weighted like Revolut's product glyphs and tinted by Icon().
 */
object CountriIcons {
    val Atlas: ImageVector by lazy {
        ImageVector.Builder("atlas", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 3f)
                arcTo(9f, 9f, 0f, true, true, 11.99f, 3f)
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(3f, 12f); lineTo(21f, 12f)
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(12f, 3f)
                curveTo(14.5f, 5.5f, 14.5f, 18.5f, 12f, 21f)
                moveTo(12f, 3f)
                curveTo(9.5f, 5.5f, 9.5f, 18.5f, 12f, 21f)
            }
        }.build()
    }

    val Passport: ImageVector by lazy {
        ImageVector.Builder("passport", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(7f, 3f)
                lineTo(17f, 3f)
                arcTo(2f, 2f, 0f, false, true, 19f, 5f)
                lineTo(19f, 19f)
                arcTo(2f, 2f, 0f, false, true, 17f, 21f)
                lineTo(7f, 21f)
                arcTo(2f, 2f, 0f, false, true, 5f, 19f)
                lineTo(5f, 5f)
                arcTo(2f, 2f, 0f, false, true, 7f, 3f)
                close()
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f) {
                moveTo(12f, 7.4f)
                arcTo(2.6f, 2.6f, 0f, true, true, 11.99f, 7.4f)
            }
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(9f, 16f); lineTo(15f, 16f)
            }
        }.build()
    }

    val Stats: ImageVector by lazy {
        ImageVector.Builder("stats", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round) {
                moveTo(5f, 20f); lineTo(5f, 11f)
                moveTo(10.5f, 20f); lineTo(10.5f, 4f)
                moveTo(16f, 20f); lineTo(16f, 13f)
                moveTo(21f, 20f); lineTo(3f, 20f)
            }
        }.build()
    }

    val Wishlist: ImageVector by lazy {
        ImageVector.Builder("wishlist", 24.dp, 24.dp, 24f, 24f).apply {
            path(stroke = SolidColor(Color.White), strokeLineWidth = 2.2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
                moveTo(6f, 3f)
                lineTo(18f, 3f)
                arcTo(1f, 1f, 0f, false, true, 19f, 4f)
                lineTo(19f, 21f)
                lineTo(12f, 17f)
                lineTo(5f, 21f)
                lineTo(5f, 4f)
                arcTo(1f, 1f, 0f, false, true, 6f, 3f)
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
