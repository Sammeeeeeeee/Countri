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
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.MonoFamily
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale
import java.time.Year

enum class EditTarget { YearOfVisit, Trips, Note, Cities }

/** One sheet, four editors — every detail edit shares this frame. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailEditSheet(
    target: EditTarget,
    entry: CountryWithState,
    onDismiss: () -> Unit,
    onSaveYear: (Int) -> Unit,
    onSaveTrips: (Int) -> Unit,
    onSaveNote: (String) -> Unit,
    onSaveCities: (List<String>) -> Unit,
) {
    val palette = Countri.palette
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                EditTarget.Note -> NoteEditor(entry, onSaveNote)
                EditTarget.Cities -> CitiesEditor(entry, onSaveCities)
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
            .clip(RoundedCornerShape(14.dp))
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

@Composable
private fun NoteEditor(entry: CountryWithState, onSave: (String) -> Unit) {
    val palette = Countri.palette
    var note by rememberSaveable { mutableStateOf(entry.note ?: "") }

    SheetTitle("A line to remember it by")
    BasicTextField(
        value = note,
        onValueChange = { if (it.length <= 120) note = it },
        textStyle = CountriType.body.copy(color = palette.textPrimary),
        cursorBrush = SolidColor(palette.visited),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.surface1)
            .hairline(RoundedCornerShape(14.dp))
            .padding(14.dp),
        minLines = 2,
        maxLines = 4,
        decorationBox = { inner ->
            Box {
                if (note.isEmpty()) {
                    Text(
                        "Balconies, night trains, that one café…",
                        style = CountriType.body,
                        color = palette.textFaint,
                    )
                }
                inner()
            }
        },
    )
    SaveButton { onSave(note.trim()) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CitiesEditor(entry: CountryWithState, onSave: (List<String>) -> Unit) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    var cities by rememberSaveable { mutableStateOf(entry.cities) }
    var draft by rememberSaveable { mutableStateOf("") }

    fun addDraft() {
        val name = draft.trim()
        if (name.isNotEmpty() && cities.none { it.equals(name, ignoreCase = true) }) {
            cities = cities + name
            haptics.tick()
        }
        draft = ""
    }

    SheetTitle("Cities")
    if (cities.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(bottom = 14.dp),
        ) {
            cities.forEach { city ->
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(palette.surface1)
                        .hairline(CircleShape)
                        .padding(start = 14.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(city, style = CountriType.bodySmall, color = palette.textPrimary)
                    Icon(
                        CountriIcons.Close,
                        contentDescription = "Remove $city",
                        tint = palette.textFaint,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(14.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                cities = cities - city
                                haptics.tick()
                            },
                    )
                }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        BasicTextField(
            value = draft,
            onValueChange = { if (it.length <= 40) draft = it },
            singleLine = true,
            textStyle = CountriType.body.copy(color = palette.textPrimary),
            cursorBrush = SolidColor(palette.visited),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surface1)
                .hairline(RoundedCornerShape(14.dp))
                .padding(14.dp),
            decorationBox = { inner ->
                Box {
                    if (draft.isEmpty()) {
                        Text("Add a city", style = CountriType.body, color = palette.textFaint)
                    }
                    inner()
                }
            },
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .pressScale(0.9f)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.visitedDim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { addDraft() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                CountriIcons.Plus,
                contentDescription = "Add city",
                tint = palette.visited,
                modifier = Modifier.size(18.dp),
            )
        }
    }
    SaveButton {
        addDraft()
        onSave(cities)
    }
}
