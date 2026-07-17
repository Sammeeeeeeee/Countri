package dev.sam.countri.ui.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.flagEmoji
import dev.sam.countri.ui.components.tapTarget
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.pressScale
import dev.sam.countri.ui.theme.staggeredEnter

@Composable
fun WishlistScreen(
    viewModel: AtlasViewModel,
    onCountryClick: (String) -> Unit,
) {
    val palette = Countri.palette
    val countries by viewModel.countries.collectAsState()
    val wishlist = remember(countries) {
        countries.filter { it.isWishlist }.sortedBy { it.country.name }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 22.dp).padding(top = 16.dp, bottom = 8.dp)) {
            Text("Wishlist", style = CountriType.title, color = palette.textPrimary)
            Text(
                text = when (wishlist.size) {
                    0 -> "Nowhere yet"
                    1 -> "1 place on the list"
                    else -> "${wishlist.size} places on the list"
                },
                style = CountriType.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 5.dp),
            )
        }

        if (wishlist.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    CountriIcons.Wishlist,
                    contentDescription = null,
                    tint = palette.textFaint,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    "Add somewhere you keep thinking about.",
                    style = CountriType.body,
                    color = palette.textFaint,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 18.dp, end = 18.dp, top = 14.dp, bottom = 110.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(wishlist, key = { _, it -> it.country.iso2 }) { index, entry ->
                    Row(
                        modifier = Modifier
                            .staggeredEnter(index)
                            .fillMaxWidth()
                            .pressScale(0.98f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.surface1)
                            .tapTarget { onCountryClick(entry.country.iso2) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(palette.recessed, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                flagEmoji(entry.country.iso2),
                                style = CountriType.body.copy(fontSize = 22.sp),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.country.name,
                                style = CountriType.subtitle,
                                color = palette.textPrimary,
                            )
                            Text(
                                // A visited country can still sit on the list
                                // — say so instead of hiding it.
                                if (entry.isVisited)
                                    "${entry.country.continent.displayName}  ·  Visited"
                                else entry.country.continent.displayName,
                                style = CountriType.bodySmall,
                                color = palette.textFaint,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
