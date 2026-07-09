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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType

/** Rounded-square mono ISO badge — the app's monogram of a country. */
@Composable
fun CodeBadge(
    iso2: String,
    status: CountryStatus?,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
) {
    val palette = Countri.palette
    val accent = when (status) {
        CountryStatus.VISITED -> palette.visited
        CountryStatus.WISHLIST -> palette.wishlist
        null -> palette.textFaint
    }
    val fill = when (status) {
        CountryStatus.VISITED -> palette.visitedDim
        CountryStatus.WISHLIST -> palette.wishlistDim
        null -> Color.Transparent
    }
    Box(
        modifier = modifier
            .size(size)
            .background(fill, RoundedCornerShape(11.dp))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(11.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(iso2, style = CountriType.mono, color = accent)
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

/** Small status dot used in list rows. */
@Composable
fun StatusDot(status: CountryStatus?, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val palette = Countri.palette
    val color = when (status) {
        CountryStatus.VISITED -> palette.visited
        CountryStatus.WISHLIST -> palette.wishlist
        null -> palette.textPrimary.copy(alpha = 0.14f)
    }
    Box(modifier.size(size).background(color, CircleShape))
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
