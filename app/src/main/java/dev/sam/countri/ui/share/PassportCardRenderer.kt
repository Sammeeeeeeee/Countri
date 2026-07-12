package dev.sam.countri.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.passport.stampRotation
import kotlin.math.ceil
import kotlin.math.min

/**
 * The three looks a share card can wear. Each is the app's own surface
 * recipe — a canvas, one card floating on it, recessed wells inside.
 */
enum class ShareStyle(
    val displayName: String,
    val canvasColor: Int,
    val card: Int,
    val well: Int,
    val ink: Int,
    val secondary: Int,
    val faint: Int,
    val hairline: Int,
) {
    Light(
        displayName = "Light",
        canvasColor = 0xFFF2F2F4.toInt(),
        card = 0xFFFFFFFF.toInt(),
        well = 0xFFF2F2F4.toInt(),
        ink = 0xFF1F1F1F.toInt(),
        secondary = 0xFF4C4C4C.toInt(),
        faint = 0xFF9A9A9E.toInt(),
        hairline = 0x141F1F1F,
    ),
    Dark(
        displayName = "Dark",
        canvasColor = 0xFF141416.toInt(),
        card = 0xFF1F1F22.toInt(),
        well = 0xFF2C2C30.toInt(),
        ink = 0xFFF4F4F5.toInt(),
        secondary = 0xFFC9C9CE.toInt(),
        faint = 0xFF8A8A90.toInt(),
        hairline = 0x1FFFFFFF,
    ),
    Mist(
        displayName = "Mist",
        canvasColor = 0xFFFFFFFF.toInt(),
        card = 0xFFF2F2F4.toInt(),
        well = 0xFFFFFFFF.toInt(),
        ink = 0xFF1F1F1F.toInt(),
        secondary = 0xFF4C4C4C.toInt(),
        faint = 0xFF9A9A9E.toInt(),
        hairline = 0x141F1F1F,
    ),
}

/**
 * Draws the passport stamp grid as a 1080x1350 share card — pure Canvas,
 * no composition, so it can render off-screen at export size.
 */
object PassportCardRenderer {

    const val WIDTH = CARD_W
    const val HEIGHT = CARD_H
    private const val COLUMNS = 4

    fun render(context: Context, stamps: List<CountryWithState>, style: ShareStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val c = CardScope(context, Canvas(bitmap), style)
        c.frame()

        // ---- header ----
        c.text("PASSPORT", PAD, 176f, c.label, 28f, style.faint, tracking = 0.15f)
        val title = if (stamps.size == 1) "1 country" else "${stamps.size} countries"
        c.text(title, PAD, 296f, c.display, 96f, style.ink, tracking = -0.022f)

        val years = stamps.mapNotNull { it.firstYear }
        val range = when {
            years.isEmpty() -> "a record of everywhere"
            years.min() == years.max() -> "in ${years.min()}"
            else -> "${years.min()} — ${years.max()}"
        }
        c.text(range, PAD, 362f, c.body, 40f, style.secondary)

        // ---- stamp grid ----
        val gridTop = 430f
        val gridBottom = 1170f
        val gridLeft = PAD - 24f
        val cell = (WIDTH - gridLeft * 2f) / COLUMNS
        val rows = ceil(stamps.size / COLUMNS.toFloat()).toInt().coerceAtLeast(1)
        val cellH = min(cell, (gridBottom - gridTop) / rows)

        stamps.forEachIndexed { i, entry ->
            val col = i % COLUMNS
            val row = i / COLUMNS
            val cx = gridLeft + cell * col + cell / 2f
            val cy = gridTop + cellH * row + cellH / 2f
            if (cy + cellH / 2f > gridBottom + 1f) return@forEachIndexed
            drawStamp(c, entry, cx, cy, min(cell, cellH) / 2f - 14f, style)
        }

        c.wordmark()
        return bitmap
    }

    private fun drawStamp(
        c: CardScope,
        entry: CountryWithState,
        cx: Float,
        cy: Float,
        radius: Float,
        style: ShareStyle,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        c.canvas.save()
        c.canvas.rotate(stampRotation(entry.country.iso2), cx, cy)

        // Same recipe as the on-screen stamp: recessed backing, ink rings.
        paint.style = Paint.Style.FILL
        paint.color = style.well
        c.canvas.drawCircle(cx, cy, radius, paint)

        paint.style = Paint.Style.STROKE
        paint.color = style.ink
        paint.alpha = 120
        paint.strokeWidth = 3.6f
        c.canvas.drawCircle(cx, cy, radius, paint)

        paint.alpha = 66
        paint.strokeWidth = 2.4f
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        c.canvas.drawCircle(cx, cy, radius - 16f, paint)
        paint.pathEffect = null

        c.flag(entry.country.iso2, cx, cy + radius * 0.14f, radius * 0.56f)
        c.text(
            entry.firstYear?.toString() ?: "·",
            cx, cy + radius * 0.52f, c.label, radius * 0.2f, style.faint,
            tracking = 0.08f, align = Paint.Align.CENTER,
        )
        c.canvas.restore()
    }
}
