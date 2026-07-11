package dev.sam.countri.ui.add

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.components.CodeBadge
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.map.MapMode
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale
import dev.sam.countri.ui.theme.staggeredEnter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCountryScreen(
    viewModel: AtlasViewModel,
    onClose: () -> Unit,
    onAdded: (String) -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val countries by viewModel.countries.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    var quickIso by rememberSaveable { mutableStateOf<String?>(null) }

    val filtered = remember(countries, query) {
        val q = query.trim()
        countries
            .filter {
                q.isEmpty() ||
                    it.country.name.contains(q, ignoreCase = true) ||
                    it.country.iso2.equals(q, ignoreCase = true)
            }
            .sortedWith(
                compareBy<CountryWithState> {
                    when (it.status) {
                        CountryStatus.VISITED -> 0
                        CountryStatus.WISHLIST -> 1
                        null -> 2
                    }
                }.thenBy { it.country.name }
            )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(palette.canvas)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 16.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Add a country", style = CountriType.title, color = palette.textPrimary)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .pressScale(0.9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface1)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CountriIcons.Close,
                    contentDescription = "Close",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Search field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(48.dp)
                .clip(CircleShape)
                .background(palette.surface1),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                CountriIcons.Search,
                contentDescription = null,
                tint = palette.textFaint,
                modifier = Modifier
                    .padding(start = 14.dp)
                    .size(16.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = CountriType.body.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(palette.visited),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                "Search countries",
                                style = CountriType.body,
                                color = palette.textFaint,
                            )
                        }
                        inner()
                    }
                },
            )
        }

        Text(
            text = "${filtered.size} of 195",
            style = CountriType.mono,
            color = palette.textFaint,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 14.dp, end = 14.dp, bottom = 24.dp,
            ),
        ) {
            items(filtered, key = { it.country.iso2 }) { entry ->
                CountryRow(
                    entry = entry,
                    modifier = Modifier.staggeredEnter(
                        index = filtered.indexOf(entry).coerceAtMost(14),
                        key = query.isEmpty(), // stagger once, not per keystroke
                    ),
                ) {
                    haptics.tick()
                    quickIso = entry.country.iso2
                }
            }
        }
    }

    val quickEntry = quickIso?.let { iso -> countries.firstOrNull { it.country.iso2 == iso } }
    if (quickEntry != null) {
        QuickAddSheet(
            entry = quickEntry,
            onDismiss = { quickIso = null },
            onConfirm = { status ->
                viewModel.setStatus(quickEntry.country.iso2, status)
                haptics.confirm()
                quickIso = null
                // Land on the country's own page to add the details.
                onAdded(quickEntry.country.iso2)
            },
        )
    }
}

@Composable
private fun CountryRow(
    entry: CountryWithState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(0.98f)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface1)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CodeBadge(entry.country.iso2, entry.status, size = 34.dp)
        Column(Modifier.weight(1f)) {
            Text(entry.country.name, style = CountriType.body, color = palette.textPrimary)
            Text(
                entry.country.continent.displayName,
                style = CountriType.bodySmall,
                color = palette.textFaint,
            )
        }
        val label = when (entry.status) {
            CountryStatus.VISITED -> "Visited"
            CountryStatus.WISHLIST -> "Wishlist"
            null -> null
        }
        if (label != null) {
            Text(
                label.uppercase(),
                style = CountriType.monoSmall,
                color = if (entry.isVisited) palette.visited else palette.wishlist,
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.visitedDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                CountriIcons.Plus,
                contentDescription = null,
                tint = palette.visited,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    entry: CountryWithState,
    onDismiss: () -> Unit,
    onConfirm: (CountryStatus) -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var chosen by remember(entry.country.iso2) {
        mutableStateOf(if (entry.isWishlist) CountryStatus.WISHLIST else CountryStatus.VISITED)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .navigationBarsPadding()
        ) {
            Text(
                entry.country.name,
                style = CountriType.displaySmall,
                color = palette.textPrimary,
            )
            Text(
                entry.country.continent.displayName,
                style = CountriType.bodySmall,
                color = palette.textSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 22.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusChoice(
                    text = "Been there",
                    selected = chosen == CountryStatus.VISITED,
                    accent = palette.visited,
                    onAccent = palette.onVisited,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.tick()
                    chosen = CountryStatus.VISITED
                }
                StatusChoice(
                    text = "Wishlist",
                    selected = chosen == CountryStatus.WISHLIST,
                    accent = palette.wishlist,
                    onAccent = palette.onWishlist,
                    modifier = Modifier.weight(1f),
                ) {
                    haptics.tick()
                    chosen = CountryStatus.WISHLIST
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(0.97f)
                    .clip(CircleShape)
                    .background(palette.visited)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        scope.launch {
                            sheetState.hide()
                            onConfirm(chosen)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Add to atlas", style = CountriType.subtitle, color = palette.onVisited)
            }
            Text(
                "Years, places and notes live on the country page.",
                style = CountriType.bodySmall,
                color = palette.textFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            )
        }
    }
}

@Composable
private fun StatusChoice(
    text: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onAccent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = Countri.palette
    val bg by animateColorAsState(
        targetValue = if (selected) accent else palette.recessed,
        label = "choiceBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) onAccent else palette.textPrimary,
        label = "choiceFg",
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .pressScale(0.96f)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CountriType.body, color = fg)
    }
}
