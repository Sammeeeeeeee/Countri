package dev.sam.countri.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.sam.countri.data.wiki.WikiPlace
import dev.sam.countri.data.wiki.WikiSearch
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.MonoFamily
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale
import kotlinx.coroutines.delay
import java.time.Year

enum class EditTarget { YearOfVisit, Trips, Places }

/** One sheet, three editors — every detail edit shares this frame. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEditSheet(
    target: EditTarget,
    entry: CountryWithState,
    onDismiss: () -> Unit,
    onSaveYear: (Int) -> Unit,
    onSaveTrips: (Int) -> Unit,
    onSavePlaces: (List<String>) -> Unit,
) {
    val palette = Countri.palette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            when (target) {
                EditTarget.YearOfVisit -> YearEditor(entry, onSaveYear)
                EditTarget.Trips -> TripsEditor(entry, onSaveTrips)
                EditTarget.Places -> PlacesEditor(entry, onSavePlaces)
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = CountriType.subtitle,
        color = Countri.palette.textPrimary,
        modifier = Modifier.padding(bottom = 18.dp),
    )
}

@Composable
private fun SaveButton(text: String = "Save", onClick: () -> Unit) {
    val palette = Countri.palette
    Box(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .height(52.dp)
            .pressScale(0.97f)
            .clip(CircleShape)
            .background(palette.visited)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = CountriType.body, color = palette.onVisited)
    }
}

@Composable
private fun YearEditor(entry: CountryWithState, onSave: (Int) -> Unit) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val currentYear = Year.now().value
    val years = remember { (currentYear downTo 1960).toList() }
    val selected = entry.firstVisitYear ?: currentYear
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentYear - selected).coerceIn(0, years.lastIndex),
    )

    SheetTitle("First visit")
    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(years) { year ->
            val isSelected = year == selected
            Text(
                year.toString(),
                style = CountriType.body.copy(fontFamily = MonoFamily),
                color = if (isSelected) palette.onVisited else palette.textSecondary,
                modifier = Modifier
                    .pressScale(0.93f)
                    .clip(CircleShape)
                    .background(if (isSelected) palette.visited else palette.surface1)
                    .hairline(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptics.confirm()
                        onSave(year)
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
    Text(
        "Tap the year you first set foot there.",
        style = CountriType.bodySmall,
        color = Countri.palette.textFaint,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun TripsEditor(entry: CountryWithState, onSave: (Int) -> Unit) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    var trips by rememberSaveable { mutableStateOf(entry.trips.coerceAtLeast(1)) }

    SheetTitle("Trips")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton("−") {
            if (trips > 1) {
                trips--
                haptics.tick()
            }
        }
        Text(
            trips.toString(),
            style = CountriType.display,
            color = palette.visited,
            modifier = Modifier.padding(horizontal = 34.dp),
        )
        StepperButton("+") {
            if (trips < 99) {
                trips++
                haptics.tick()
            }
        }
    }
    SaveButton { onSave(trips) }
}

@Composable
private fun StepperButton(glyph: String, onClick: () -> Unit) {
    val palette = Countri.palette
    Box(
        modifier = Modifier
            .size(52.dp)
            .pressScale(0.9f)
            .clip(CircleShape)
            .background(palette.surface1)
            .hairline(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = CountriType.subtitle, color = palette.textPrimary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlacesEditor(entry: CountryWithState, onSave: (List<String>) -> Unit) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val accent = palette.continentColor(entry.country.continent)
    var places by rememberSaveable { mutableStateOf(entry.places) }
    var draft by rememberSaveable { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<WikiPlace>>(emptyList()) }

    // Search-as-you-type against Wikipedia, debounced.
    LaunchedEffect(draft) {
        val q = draft.trim()
        if (q.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(280)
        suggestions = WikiSearch.search(q)
    }

    fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && places.none { it.equals(trimmed, ignoreCase = true) }) {
            places = places + trimmed
            haptics.tick()
        }
        draft = ""
        suggestions = emptyList()
    }

    SheetTitle(if (entry.isVisited) "Places you went" else "Places to see")
    if (places.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            places.forEach { place ->
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surface1)
                        .hairline(CircleShape, accent.copy(alpha = 0.25f))
                        .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(place, style = CountriType.bodySmall, color = palette.textPrimary)
                    Icon(
                        CountriIcons.Close,
                        contentDescription = "Remove $place",
                        tint = palette.textFaint,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(14.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                places = places - place
                                haptics.tick()
                            },
                    )
                }
            }
        }
    }

    BasicTextField(
        value = draft,
        onValueChange = { if (it.length <= 60) draft = it },
        singleLine = true,
        textStyle = CountriType.body.copy(color = palette.textPrimary),
        cursorBrush = SolidColor(accent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.surface1)
            .hairline(RoundedCornerShape(16.dp))
            .padding(14.dp),
        decorationBox = { inner ->
            Box {
                if (draft.isEmpty()) {
                    Text(
                        "Buckingham Palace, Machu Picchu…",
                        style = CountriType.body,
                        color = palette.textFaint,
                    )
                }
                inner()
            }
        },
    )

    // Wikipedia suggestions — tap to add; saved places open their page.
    if (suggestions.isNotEmpty() || draft.trim().length >= 2) {
        Column(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.surface1)
                .hairline(RoundedCornerShape(16.dp))
        ) {
            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { add(suggestion.title) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            suggestion.title,
                            style = CountriType.body,
                            color = palette.textPrimary,
                        )
                        if (!suggestion.description.isNullOrBlank()) {
                            Text(
                                suggestion.description,
                                style = CountriType.bodySmall,
                                color = palette.textFaint,
                                maxLines = 1,
                            )
                        }
                    }
                    Icon(
                        CountriIcons.Plus,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            val exact = suggestions.any { it.title.equals(draft.trim(), ignoreCase = true) }
            if (draft.trim().length >= 2 && !exact) {
                Text(
                    "Add “${draft.trim()}”",
                    style = CountriType.bodySmall,
                    color = accent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { add(draft) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }
    }

    SaveButton {
        if (draft.isNotBlank()) add(draft)
        onSave(places)
    }
}
