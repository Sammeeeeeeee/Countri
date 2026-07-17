package dev.sam.countri.ui.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.domain.AtlasStats

/**
 * The Atlas hero as a share card: the flat world with every visited country
 * inked in, headline coverage up top. Same silhouettes the app draws, same
 * card language as every other export.
 */
object AtlasCardRenderer {

    // The map keeps the app's flat-projection crop.
    private const val MAX_LAT = 83.5f
    private const val MIN_LAT = -56f

    fun render(
        context: Context,
        data: WorldMapData,
        visited: Set<Int>,
        stats: AtlasStats,
        style: ShareStyle,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
        val c = CardScope(context, Canvas(bitmap), style)
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

        // ---- the world ----
        val left = PAD - 26f
        val right = CARD_W - PAD + 26f
        val top = 600f
        val s = (right - left) / 360f // px per degree, equirect
        val bottom = top + (MAX_LAT - MIN_LAT) * s

        fun px(lon: Float) = left + (lon + 180f) * s
        fun py(lat: Float) = top + (MAX_LAT - lat) * s

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val land = Path()
        val inked = Path()
        for (r in 0 until data.ringCount) {
            val target = if (data.ringCountry[r] in visited) inked else land
            val start = data.ringStart[r]
            val n = data.ringSize[r]
            if (n < 3) continue
            target.moveTo(px(data.lon[start]), py(data.lat[start]))
            for (i in 1 until n) {
                target.lineTo(px(data.lon[start + i]), py(data.lat[start + i]))
            }
            target.close()
        }
        paint.color = style.well
        c.canvas.drawPath(land, paint)
        paint.color = style.ink
        c.canvas.drawPath(inked, paint)

        // Anchor line under the map so the card doesn't just float away.
        c.text(
            "THE WORLD SO FAR",
            PAD, bottom + 64f, c.label, 28f, style.faint, tracking = 0.15f,
        )

        c.wordmark()
        return bitmap
    }
}
