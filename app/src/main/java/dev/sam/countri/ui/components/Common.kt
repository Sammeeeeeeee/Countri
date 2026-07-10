package dev.sam.countri.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType

/** ISO2 → regional-indicator emoji flag: "FR" → 🇫🇷 */
fun flagEmoji(iso2: String): String {
    if (iso2.length != 2) return iso2
    val first = String(Character.toChars(0x1F1E6 + (iso2[0].uppercaseChar() - 'A')))
    val second = String(Character.toChars(0x1F1E6 + (iso2[1].uppercaseChar() - 'A')))
    return first + second
}

/**
 * Rounded-square flag badge — the app's monogram of a country.
 * Visited badges wear their continent's hue; wishlist ones a dashed border.
 */
@Composable
fun CodeBadge(
    iso2: String,
    status: CountryStatus?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    val palette = Countri.palette
    val continentHue = CountryCatalog.byIso2[iso2]
        ?.let { palette.continentColor(it.continent) }
        ?: palette.visited
    val accent = when (status) {
        CountryStatus.VISITED -> continentHue
        CountryStatus.WISHLIST -> palette.wishlist
        null -> palette.textFaint
    }
    val fill = when (status) {
        CountryStatus.VISITED -> continentHue.copy(alpha = 0.16f)
        CountryStatus.WISHLIST -> Color.Transparent
        null -> Color.Transparent
    }
    val shape = RoundedCornerShape(11.dp)
    val borderModifier = if (status == CountryStatus.WISHLIST) {
        Modifier.dashedBorder(accent.copy(alpha = 0.6f), shape = 11.dp)
    } else {
        Modifier.border(1.dp, accent.copy(alpha = 0.4f), shape)
    }
    Box(
        modifier = modifier
            .size(size)
            .background(fill, shape)
            .then(borderModifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            flagEmoji(iso2),
            style = CountriType.body.copy(fontSize = (size.value * 0.48f).sp),
        )
    }
}

/** 1dp dashed rounded-rect border — the wishlist signature. */
fun Modifier.dashedBorder(color: Color, shape: Dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(shape.toPx()),
        style = Stroke(
            width = 1.2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(4.dp.toPx(), 3.dp.toPx()),
            ),
        ),
    )
}

/** Tracked-out mono section label: RECENT JOURNEYS, BY CONTINENT... */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = CountriType.sectionLabel,
        color = Countri.palette.textFaint,
        modifier = modifier,
    )
}

/** Outlined pill: VISITED / ON THE WISHLIST / SOMEDAY. */
@Composable
fun StatusPill(text: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .border(1.dp, accent.copy(alpha = 0.4f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = CountriType.monoSmall, color = accent)
    }
}
