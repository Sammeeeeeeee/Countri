package dev.sam.countri.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import dev.sam.countri.R
import dev.sam.countri.domain.AtlasStats
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.domain.Visit
import dev.sam.countri.ui.components.flagEmoji
import java.io.File
import java.time.format.DateTimeFormatter

private const val W = 1080
private const val H = 1350
private const val MARGIN = 84f
private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Inter at a real weight — cards must match the app's type exactly. */
fun interTypeface(context: Context, weight: Int): Typeface {
    val base = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
    return Typeface.create(base, weight, false)
}

private class CardPaints(context: Context) {
    val display: Typeface = interTypeface(context, 500)
    val body: Typeface = interTypeface(context, 400)
    val label: Typeface = interTypeface(context, 600)
}

/** Whole-country share card: flag, name, the story so far. */
object CountryCardRenderer {
    fun render(context: Context, entry: CountryWithState): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fonts = CardPaints(context)
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.typeface = fonts.label
        paint.textSize = 30f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("COUNTRI", MARGIN, 150f, paint)

        paint.typeface = fonts.body
        paint.textSize = 130f
        paint.letterSpacing = 0f
        canvas.drawText(flagEmoji(entry.country.iso2), MARGIN, 340f, paint)

        paint.typeface = fonts.display
        paint.color = 0xFF1F1F1F.toInt()
        paint.textSize = 88f
        paint.letterSpacing = -0.02f
        canvas.drawText(entry.country.name, MARGIN, 480f, paint)

        paint.typeface = fonts.body
        paint.textSize = 40f
        paint.letterSpacing = 0f
        paint.color = 0xFF4C4C4C.toInt()
        val trips = entry.tripCount
        val line = buildString {
            append(if (entry.isVisited) "Visited" else "On the wishlist")
            entry.firstYear?.let { append(" · since $it") }
            if (trips > 0) append(" · $trips ${if (trips == 1) "trip" else "trips"}")
        }
        canvas.drawText(line, MARGIN, 560f, paint)

        val cities = entry.allCities
        if (cities.isNotEmpty()) {
            paint.color = 0xFFF2F2F4.toInt()
            val panelTop = 640f
            val rowH = 86f
            val shown = cities.take(7)
            val panelBottom = (panelTop + 40f + shown.size * rowH).coerceAtMost(H - 160f)
            canvas.drawRoundRect(RectF(MARGIN, panelTop, W - MARGIN, panelBottom), 40f, 40f, paint)
            shown.forEachIndexed { i, city ->
                val y = panelTop + 90f + i * rowH
                if (y < panelBottom - 12f) {
                    paint.typeface = fonts.body
                    paint.textSize = 38f
                    paint.color = 0xFF1F1F1F.toInt()
                    canvas.drawText(city, MARGIN + 44f, y, paint)
                }
            }
        }

        paint.typeface = fonts.label
        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("MADE WITH COUNTRI", MARGIN, H - 64f, paint)
        return bitmap
    }
}

/** One visit as a share card. */
object VisitCardRenderer {
    fun render(context: Context, entry: CountryWithState, visit: Visit): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fonts = CardPaints(context)
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.typeface = fonts.label
        paint.textSize = 30f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("COUNTRI · TRIP", MARGIN, 150f, paint)

        paint.typeface = fonts.body
        paint.textSize = 130f
        paint.letterSpacing = 0f
        canvas.drawText(flagEmoji(entry.country.iso2), MARGIN, 340f, paint)

        paint.typeface = fonts.display
        paint.color = 0xFF1F1F1F.toInt()
        paint.textSize = 88f
        paint.letterSpacing = -0.02f
        canvas.drawText(entry.country.name, MARGIN, 480f, paint)

        paint.typeface = fonts.body
        paint.textSize = 40f
        paint.letterSpacing = 0f
        paint.color = 0xFF4C4C4C.toInt()
        canvas.drawText(
            "${visit.start.format(dateFormat)} → ${visit.end.format(dateFormat)}",
            MARGIN, 560f, paint,
        )
        paint.color = 0xFF717173.toInt()
        canvas.drawText(if (visit.days == 1) "1 day" else "${visit.days} days", MARGIN, 620f, paint)

        if (visit.cities.isNotEmpty()) {
            paint.color = 0xFFF2F2F4.toInt()
            val panelTop = 690f
            val rowH = 86f
            val shown = visit.cities.take(6)
            val panelBottom = (panelTop + 40f + shown.size * rowH).coerceAtMost(H - 160f)
            canvas.drawRoundRect(RectF(MARGIN, panelTop, W - MARGIN, panelBottom), 40f, 40f, paint)
            shown.forEachIndexed { i, city ->
                val y = panelTop + 90f + i * rowH
                if (y < panelBottom - 12f) {
                    paint.typeface = fonts.body
                    paint.textSize = 38f
                    paint.color = 0xFF1F1F1F.toInt()
                    canvas.drawText(city, MARGIN + 44f, y, paint)
                }
            }
        }

        paint.typeface = fonts.label
        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("MADE WITH COUNTRI", MARGIN, H - 64f, paint)
        return bitmap
    }
}

/** What goes on a stats card — chosen in the share sheet. */
data class StatsCardOptions(
    val style: ShareStyle = ShareStyle.Light,
    val showContinents: Boolean = true,
    val showTimeline: Boolean = false,
)

/** The stats overview, rendered to taste. */
object StatsCardRenderer {
    fun render(context: Context, stats: AtlasStats, options: StatsCardOptions): Bitmap {
        val style = options.style
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val fonts = CardPaints(context)
        canvas.drawColor(style.background)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val panel = if (style == ShareStyle.Mist) 0xFFFFFFFF.toInt() else
            if (style == ShareStyle.Dark) 0xFF2C2C30.toInt() else 0xFFF2F2F4.toInt()

        paint.typeface = fonts.label
        paint.textSize = 30f
        paint.letterSpacing = 0.12f
        paint.color = style.faint
        canvas.drawText("COUNTRI · STATS", MARGIN, 150f, paint)

        paint.typeface = fonts.display
        paint.color = style.ink
        paint.textSize = 200f
        paint.letterSpacing = -0.02f
        canvas.drawText("${stats.percentOfWorld}%", MARGIN, 380f, paint)
        paint.typeface = fonts.body
        paint.textSize = 44f
        paint.letterSpacing = 0f
        paint.color = style.accent
        canvas.drawText("of the world explored", MARGIN, 450f, paint)

        paint.color = style.ink
        paint.typeface = fonts.display
        paint.textSize = 48f
        canvas.drawText(
            "${stats.visitedCount} of 195 countries · ${stats.continentsVisited} of 7 continents",
            MARGIN, 550f, paint,
        )

        var y = 640f
        if (options.showContinents) {
            val rowH = 88f
            stats.byContinent.forEach { stat ->
                paint.typeface = fonts.body
                paint.textSize = 34f
                paint.color = style.accent
                canvas.drawText(stat.continent.displayName, MARGIN, y, paint)
                paint.textAlign = Paint.Align.RIGHT
                paint.color = style.faint
                canvas.drawText("${stat.visited} / ${stat.continent.total}", W - MARGIN, y, paint)
                paint.textAlign = Paint.Align.LEFT
                paint.color = panel
                canvas.drawRoundRect(RectF(MARGIN, y + 18f, W - MARGIN, y + 34f), 8f, 8f, paint)
                val frac = (stat.fraction * 3f).coerceAtMost(1f)
                if (frac > 0f) {
                    paint.color = style.ink
                    canvas.drawRoundRect(
                        RectF(MARGIN, y + 18f, MARGIN + (W - 2 * MARGIN) * frac, y + 34f),
                        8f, 8f, paint,
                    )
                }
                y += rowH
            }
            y += 24f
        }

        if (options.showTimeline) {
            stats.timeline.take(if (options.showContinents) 4 else 8).forEach { group ->
                if (y < H - 160f) {
                    paint.typeface = fonts.label
                    paint.textSize = 34f
                    paint.color = style.ink
                    canvas.drawText(group.year.toString(), MARGIN, y, paint)
                    paint.typeface = fonts.body
                    paint.color = style.accent
                    val flags = group.isoCodes.take(8).joinToString(" ") { flagEmoji(it) }
                    val days = if (group.totalDays > 0) "  ·  ${group.totalDays} days" else ""
                    canvas.drawText("$flags$days", MARGIN + 130f, y, paint)
                    y += 74f
                }
            }
        }

        paint.typeface = fonts.label
        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        paint.color = style.faint
        canvas.drawText("MADE WITH COUNTRI", MARGIN, H - 64f, paint)
        return bitmap
    }
}

/** Writes the bitmap to the share cache and opens the system share sheet. */
fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, title: String) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, fileName)
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "dev.sam.countri.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}
