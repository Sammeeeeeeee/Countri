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

/** Renders one visit as a monochrome share card. */
object VisitCardRenderer {
    fun render(context: Context, entry: CountryWithState, visit: Visit): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inter = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.typeface = inter
        paint.textSize = 30f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("COUNTRI · TRIP", MARGIN, 150f, paint)

        paint.textSize = 130f
        paint.letterSpacing = 0f
        canvas.drawText(flagEmoji(entry.country.iso2), MARGIN, 340f, paint)

        paint.color = 0xFF1F1F1F.toInt()
        paint.textSize = 88f
        paint.letterSpacing = -0.02f
        canvas.drawText(entry.country.name, MARGIN, 480f, paint)

        paint.textSize = 40f
        paint.letterSpacing = 0f
        paint.color = 0xFF4C4C4C.toInt()
        canvas.drawText(
            "${visit.start.format(dateFormat)} → ${visit.end.format(dateFormat)}",
            MARGIN, 560f, paint,
        )
        paint.color = 0xFF717173.toInt()
        canvas.drawText(if (visit.days == 1) "1 day" else "${visit.days} days", MARGIN, 620f, paint)

        // Cities as a stacked list inside a mist panel.
        if (visit.cities.isNotEmpty()) {
            paint.color = 0xFFF2F2F4.toInt()
            val panelTop = 690f
            val rowH = 86f
            val panelBottom = (panelTop + 40f + visit.cities.size.coerceAtMost(6) * rowH)
                .coerceAtMost(H - 160f)
            canvas.drawRoundRect(
                RectF(MARGIN, panelTop, W - MARGIN, panelBottom), 40f, 40f, paint,
            )
            paint.textSize = 38f
            visit.cities.take(6).forEachIndexed { i, city ->
                val y = panelTop + 90f + i * rowH
                if (y < panelBottom - 12f) {
                    paint.color = 0xFF1F1F1F.toInt()
                    canvas.drawText(city, MARGIN + 44f, y, paint)
                }
            }
        }

        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("MADE WITH COUNTRI", MARGIN, H - 64f, paint)
        return bitmap
    }
}

/** Renders the stats overview as a monochrome share card. */
object StatsCardRenderer {
    fun render(context: Context, stats: AtlasStats): Bitmap {
        val bitmap = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val inter = ResourcesCompat.getFont(context, R.font.inter) ?: Typeface.DEFAULT
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.typeface = inter

        paint.textSize = 30f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
        canvas.drawText("COUNTRI · STATS", MARGIN, 150f, paint)

        paint.color = 0xFF1F1F1F.toInt()
        paint.textSize = 200f
        paint.letterSpacing = -0.02f
        canvas.drawText("${stats.percentOfWorld}%", MARGIN, 380f, paint)
        paint.textSize = 44f
        paint.letterSpacing = 0f
        paint.color = 0xFF4C4C4C.toInt()
        canvas.drawText("of the world explored", MARGIN, 450f, paint)

        paint.color = 0xFF1F1F1F.toInt()
        paint.textSize = 48f
        canvas.drawText(
            "${stats.visitedCount} of 195 countries · ${stats.continentsVisited} of 7 continents",
            MARGIN, 550f, paint,
        )

        // Continent bars.
        val barTop = 640f
        val rowH = 96f
        stats.byContinent.forEachIndexed { i, stat ->
            val y = barTop + i * rowH
            paint.textSize = 36f
            paint.color = 0xFF4C4C4C.toInt()
            canvas.drawText(stat.continent.displayName, MARGIN, y, paint)
            paint.textAlign = Paint.Align.RIGHT
            paint.color = 0xFF717173.toInt()
            canvas.drawText("${stat.visited} / ${stat.continent.total}", W - MARGIN, y, paint)
            paint.textAlign = Paint.Align.LEFT
            paint.color = 0xFFF2F2F4.toInt()
            canvas.drawRoundRect(RectF(MARGIN, y + 20f, W - MARGIN, y + 36f), 8f, 8f, paint)
            val frac = (stat.fraction * 3f).coerceAtMost(1f)
            if (frac > 0f) {
                paint.color = 0xFF1F1F1F.toInt()
                canvas.drawRoundRect(
                    RectF(MARGIN, y + 20f, MARGIN + (W - 2 * MARGIN) * frac, y + 36f),
                    8f, 8f, paint,
                )
            }
        }

        paint.textSize = 28f
        paint.letterSpacing = 0.12f
        paint.color = 0xFF717173.toInt()
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
