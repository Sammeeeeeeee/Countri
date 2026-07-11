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
 * Revolut-style country avatar: a quiet mist circle with the flag inside.
 * No borders, no tints — status lives in the row's text, not its badge.
 */
@Composable
fun CodeBadge(
    iso2: String,
    status: CountryStatus?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val palette = Countri.palette
    Box(
        modifier = modifier
            .size(size)
            .background(palette.recessed, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            flagEmoji(iso2),
            style = CountriType.body.copy(fontSize = (size.value * 0.46f).sp),
        )
    }
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

/** Revolut badge: mist pill, no outline. VISITED / ON THE WISHLIST / SOMEDAY. */
@Composable
fun StatusPill(text: String, accent: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Countri.palette.recessed, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text.uppercase(), style = CountriType.monoSmall, color = accent)
    }
}
