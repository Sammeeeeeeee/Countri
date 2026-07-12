package dev.sam.countri.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
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

internal const val CARD_W = 1080
internal const val CARD_H = 1350
private const val CARD_INSET = 44f
private const val CARD_RADIUS = 64f
internal const val PAD = 116f

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Inter at a real weight — cards must match the app's type exactly. */
fun interTypeface(context: Context, weight: Int): Typeface {
    val base = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
    return Typeface.create(base, weight, false)
}

/**
 * Shared drawing kit for every card: the gray canvas with one white card
 * floating on it, recessed wells, flag badges, the wordmark — the app's
 * surface language, reproduced stroke for stroke at export size.
 */
internal class CardScope(context: Context, val canvas: Canvas, val style: ShareStyle) {
    val display: Typeface = interTypeface(context, 500)
    val body: Typeface = interTypeface(context, 400)
    val label: Typeface = interTypeface(context, 600)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun frame() {
        canvas.drawColor(style.canvasColor)
        paint.color = style.card
        canvas.drawRoundRect(
            RectF(CARD_INSET, CARD_INSET, CARD_W - CARD_INSET, CARD_H - CARD_INSET),
            CARD_RADIUS, CARD_RADIUS, paint,
        )
    }

    fun wordmark() {
        text("countri", PAD, CARD_H - 118f, label, 40f, style.ink, tracking = -0.01f)
    }

    fun text(
        s: String,
        x: Float,
        baseline: Float,
        tf: Typeface,
        size: Float,
        color: Int,
        tracking: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
    ): Float {
        paint.typeface = tf
        paint.textSize = size
        paint.letterSpacing = tracking
        paint.color = color
        paint.textAlign = align
        canvas.drawText(s, x, baseline, paint)
        val w = paint.measureText(s)
        paint.textAlign = Paint.Align.LEFT
        paint.letterSpacing = 0f
        return w
    }

    fun measure(s: String, tf: Typeface, size: Float, tracking: Float = 0f): Float {
        paint.typeface = tf
        paint.textSize = size
        paint.letterSpacing = tracking
        val w = paint.measureText(s)
        paint.letterSpacing = 0f
        return w
    }

    /** Emoji flags come from the system color font, never Inter. */
    fun flag(iso2: String, cx: Float, baseline: Float, size: Float) {
        paint.typeface = Typeface.DEFAULT
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(flagEmoji(iso2), cx, baseline, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    /** The app's CodeBadge: a recessed circle with the flag sitting in it. */
    fun flagBadge(iso2: String, cx: Float, cy: Float, r: Float) {
        paint.color = style.well
        canvas.drawCircle(cx, cy, r, paint)
        flag(iso2, cx, cy + r * 0.36f, r * 0.95f)
    }

    fun well(rect: RectF, radius: Float = 48f) {
        paint.color = style.well
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    fun divider(x0: Float, x1: Float, y: Float) {
        paint.color = style.hairline
        canvas.drawRect(x0, y, x1, y + 2f, paint)
    }

    /** The single chromatic element the design system allows: cobalt. */
    fun cobaltPill(textValue: String, left: Float, top: Float): Float {
        val tw = measure(textValue, label, 34f)
        val rect = RectF(left, top, left + tw + 88f, top + 74f)
        paint.shader = LinearGradient(
            rect.left, 0f, rect.right, 0f,
            0xFF1227FD.toInt(), 0xFF6FA0FF.toInt(), Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(rect, 37f, 37f, paint)
        paint.shader = null
        text(textValue, rect.left + 44f, rect.top + 49f, label, 34f, 0xFFFFFFFF.toInt())
        return rect.right
    }
}

/** Whole-country share card: flag, name, the story so far. */
object CountryCardRenderer {
    fun render(context: Context, entry: CountryWithState): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = CardScope(context, Canvas(bitmap), ShareStyle.Light)
        c.frame()

        c.flagBadge(entry.country.iso2, PAD + 76f, 236f, 76f)
        c.text(entry.country.name, PAD, 468f, c.display, 92f, c.style.ink, tracking = -0.022f)

        val trips = entry.tripCount
        val caption = buildString {
            append(if (entry.isVisited) "Visited" else "On the wishlist")
            entry.firstYear?.let { append("  ·  since $it") }
            if (trips > 0) append("  ·  $trips ${if (trips == 1) "trip" else "trips"}")
        }
        c.text(caption, PAD, 536f, c.body, 42f, c.style.secondary)

        val cities = entry.allCities
        if (cities.isNotEmpty()) {
            c.text("CITIES", PAD, 656f, c.label, 28f, c.style.faint, tracking = 0.15f)
            val shown = cities.take(4)
            val extra = cities.size - shown.size
            val rows = shown.size + if (extra > 0) 1 else 0
            val rowH = 92f
            val top = 688f
            c.well(RectF(PAD, top, CARD_W - PAD, top + 36f + rows * rowH))
            var y = top + 76f
            shown.forEachIndexed { i, city ->
                c.text(city, PAD + 48f, y, c.body, 40f, c.style.ink)
                if (i < rows - 1) c.divider(PAD + 48f, CARD_W - PAD - 48f, y + 28f)
                y += rowH
            }
            if (extra > 0) c.text("+ $extra more", PAD + 48f, y, c.body, 40f, c.style.faint)
        }

        c.wordmark()
        return bitmap
    }
}

/** One visit as a share card: dates up top, the days writ large. */
object VisitCardRenderer {
    fun render(context: Context, entry: CountryWithState, visit: Visit): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = CardScope(context, Canvas(bitmap), ShareStyle.Light)
        c.frame()

        c.flagBadge(entry.country.iso2, PAD + 76f, 236f, 76f)
        c.text(entry.country.name, PAD, 460f, c.display, 84f, c.style.ink, tracking = -0.022f)
        c.text(
            "${visit.start.format(dateFormat)} → ${visit.end.format(dateFormat)}",
            PAD, 526f, c.body, 42f, c.style.secondary,
        )

        val numberW = c.text(
            visit.days.toString(), PAD, 810f, c.display, 190f, c.style.ink, tracking = -0.02f,
        )
        c.text(
            if (visit.days == 1) "day" else "days",
            PAD + numberW + 28f, 810f, c.body, 46f, c.style.faint,
        )

        // Cities as pills, flowing like chips do in the app.
        var px = PAD
        var py = 908f
        val pillH = 78f
        for ((i, city) in visit.cities.withIndex()) {
            val tw = c.measure(city, c.body, 36f)
            val pw = tw + 72f
            if (px + pw > CARD_W - PAD) {
                px = PAD
                py += pillH + 22f
            }
            if (py + pillH > 1160f) {
                c.text("+ ${visit.cities.size - i} more", px + 8f, py + 50f, c.body, 36f, c.style.faint)
                break
            }
            c.well(RectF(px, py, px + pw, py + pillH), pillH / 2f)
            c.text(city, px + 36f, py + 50f, c.body, 36f, c.style.ink)
            px += pw + 20f
        }

        c.wordmark()
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
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = CardScope(context, Canvas(bitmap), options.style)
        val style = options.style
        c.frame()

        c.cobaltPill("${stats.percentOfWorld}% of the world", PAD, 120f)

        val countW = c.text(
            stats.visitedCount.toString(), PAD, 434f, c.display, 200f, style.ink, tracking = -0.02f,
        )
        c.text(
            if (stats.visitedCount == 1) "country" else "countries",
            PAD + countW + 30f, 434f, c.body, 46f, style.faint,
        )
        c.text("${stats.continentsVisited} of 7 continents", PAD, 506f, c.body, 42f, style.secondary)

        var y = 596f
        val limit = 1160f

        if (options.showContinents) {
            c.text("CONTINENTS", PAD, y, c.label, 28f, style.faint, tracking = 0.15f)
            y += 40f
            val stride = if (options.showTimeline) 64f else 78f
            val barTop = if (options.showTimeline) 44f else 48f
            // Leave the timeline room to actually exist when it's toggled on.
            val sectionLimit = if (options.showTimeline) limit - 224f else limit
            val rows = stats.byContinent.filter { it.visited > 0 }
                .ifEmpty { stats.byContinent.take(3) }
            var shown = 0
            for (stat in rows) {
                if (y + stride > sectionLimit) break
                c.text(stat.continent.displayName, PAD, y + 34f, c.body, 36f, style.ink)
                c.text(
                    "${stat.visited} / ${stat.continent.total}",
                    CARD_W - PAD, y + 34f, c.body, 32f, style.faint, align = Paint.Align.RIGHT,
                )
                c.paint.color = style.well
                c.canvas.drawRoundRect(
                    RectF(PAD, y + barTop, CARD_W - PAD, y + barTop + 12f), 6f, 6f, c.paint,
                )
                val frac = (stat.fraction * 3f).coerceAtMost(1f)
                if (frac > 0f) {
                    c.paint.color = style.ink
                    c.canvas.drawRoundRect(
                        RectF(PAD, y + barTop, PAD + (CARD_W - 2 * PAD) * frac, y + barTop + 12f),
                        6f, 6f, c.paint,
                    )
                }
                y += stride
                shown++
            }
            if (shown < rows.size) {
                c.text("+ ${rows.size - shown} more", PAD, y + 30f, c.body, 32f, style.faint)
                y += 44f
            }
            y += 36f
        }

        if (options.showTimeline) {
            c.text("TIMELINE", PAD, y, c.label, 28f, style.faint, tracking = 0.15f)
            y += 12f
            var shown = 0
            for (group in stats.timeline) {
                if (y + 58f > limit) break
                c.text(group.year.toString(), PAD, y + 42f, c.label, 36f, style.ink)
                val flags = group.isoCodes.take(6)
                var fx = PAD + 140f
                flags.forEach { iso ->
                    c.flag(iso, fx + 24f, y + 42f, 40f)
                    fx += 58f
                }
                if (group.isoCodes.size > flags.size) {
                    c.text("+${group.isoCodes.size - flags.size}", fx + 6f, y + 42f, c.body, 32f, style.faint)
                }
                if (group.totalDays > 0) {
                    c.text(
                        if (group.totalDays == 1) "1 day" else "${group.totalDays} days",
                        CARD_W - PAD, y + 42f, c.body, 34f, style.faint, align = Paint.Align.RIGHT,
                    )
                }
                y += 58f
                shown++
            }
            if (shown < stats.timeline.size) {
                c.text(
                    "+ ${stats.timeline.size - shown} more ${if (stats.timeline.size - shown == 1) "year" else "years"}",
                    PAD, y + 38f, c.body, 32f, style.faint,
                )
            }
        }

        c.wordmark()
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
