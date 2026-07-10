package dev.sam.countri.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import dev.sam.countri.R
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.components.flagEmoji
import dev.sam.countri.ui.passport.stampRotation
import kotlin.math.ceil
import kotlin.math.min

/**
 * The three looks a shared passport card can wear. [accents] is one ink per
 * continent (catalog Continent ordinal order) — each stamp wears its own.
 */
enum class ShareStyle(
    val displayName: String,
    val background: Int,
    val ink: Int,
    val accent: Int,
    val faint: Int,
    val accents: IntArray,
) {
    Light(
        displayName = "Light",
        background = 0xFFFFFFFF.toInt(),
        ink = 0xFF1F1F1F.toInt(),
        accent = 0xFF1F1F1F.toInt(),
        faint = 0xFF717173.toInt(),
        accents = IntArray(6) { 0xFF1F1F1F.toInt() },
    ),
    Dark(
        displayName = "Dark",
        background = 0xFF1F1F1F.toInt(),
        ink = 0xFFFFFFFF.toInt(),
        accent = 0xFFFFFFFF.toInt(),
        faint = 0xFF9A9A9E.toInt(),
        accents = IntArray(6) { 0xFFFFFFFF.toInt() },
    ),
    Mist(
        displayName = "Mist",
        background = 0xFFF7F7F7.toInt(),
        ink = 0xFF1F1F1F.toInt(),
        accent = 0xFF4C4C4C.toInt(),
        faint = 0xFF717173.toInt(),
        accents = IntArray(6) { 0xFF4C4C4C.toInt() },
    ),
}

/**
 * Draws the passport stamp grid as a 1080x1350 share card — pure Canvas,
 * no composition, so it can render off-screen at export size.
 */
object PassportCardRenderer {

    const val WIDTH = 1080
    const val HEIGHT = 1350
    private const val COLUMNS = 4
    private const val MARGIN = 84f

    fun render(context: Context, stamps: List<CountryWithState>, style: ShareStyle): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val display = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
        val mono = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT

        canvas.drawColor(style.background)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // ---- header ----
        paint.typeface = mono
        paint.textSize = 30f
        paint.letterSpacing = 0.32f
        paint.color = style.accent
        canvas.drawText("COUNTRI · PASSPORT", MARGIN, 150f, paint)

        paint.typeface = display
        paint.textSize = 96f
        paint.letterSpacing = -0.02f
        paint.color = style.ink
        val years = stamps.mapNotNull { it.firstYear }
        val title = when (stamps.size) {
            1 -> "1 country"
            else -> "${stamps.size} countries"
        }
        canvas.drawText(title, MARGIN, 268f, paint)

        paint.typeface = mono
        paint.textSize = 34f
        paint.letterSpacing = 0.06f
        paint.color = style.faint
        val range = when {
            years.isEmpty() -> "a record of everywhere"
            years.min() == years.max() -> "in ${years.min()}"
            else -> "${years.min()} — ${years.max()}"
        }
        canvas.drawText(range, MARGIN, 328f, paint)

        // ---- stamp grid ----
        val gridTop = 420f
        val gridBottom = HEIGHT - 130f
        val cell = (WIDTH - MARGIN * 2f) / COLUMNS
        val rows = ceil(stamps.size / COLUMNS.toFloat()).toInt().coerceAtLeast(1)
        val cellH = min(cell, (gridBottom - gridTop) / rows)

        stamps.forEachIndexed { i, entry ->
            val col = i % COLUMNS
            val row = i / COLUMNS
            val cx = MARGIN + cell * col + cell / 2f
            val cy = gridTop + cellH * row + cellH / 2f
            if (cy + cellH / 2f > gridBottom + 1f) return@forEachIndexed
            drawStamp(canvas, entry, cx, cy, min(cell, cellH) / 2f - 14f, style, display, mono)
        }

        // ---- footer ----
        paint.typeface = mono
        paint.textSize = 28f
        paint.letterSpacing = 0.24f
        paint.color = style.faint
        canvas.drawText("MADE WITH COUNTRI", MARGIN, HEIGHT - 64f, paint)

        return bitmap
    }

    private fun drawStamp(
        canvas: Canvas,
        entry: CountryWithState,
        cx: Float,
        cy: Float,
        radius: Float,
        style: ShareStyle,
        display: Typeface,
        mono: Typeface,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val ink = style.accents[entry.country.continent.ordinal]
        canvas.save()
        canvas.rotate(stampRotation(entry.country.iso2), cx, cy)

        paint.style = Paint.Style.STROKE
        paint.color = ink
        paint.alpha = 135
        paint.strokeWidth = 3.6f
        canvas.drawCircle(cx, cy, radius, paint)

        paint.alpha = 80
        paint.strokeWidth = 2.4f
        paint.pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        canvas.drawCircle(cx, cy, radius - 14f, paint)
        paint.pathEffect = null

        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.DEFAULT // emoji flags come from the system font
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = radius * 0.56f
        paint.color = ink
        canvas.drawText(flagEmoji(entry.country.iso2), cx, cy + radius * 0.16f, paint)
        paint.typeface = mono
        paint.letterSpacing = 0.04f

        paint.textSize = radius * 0.2f
        paint.letterSpacing = 0.1f
        paint.alpha = 190
        canvas.drawText(
            entry.firstYear?.toString() ?: "·",
            cx,
            cy + radius * 0.46f,
            paint,
        )
        canvas.restore()
    }
}
