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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.sam.countri.data.cities.CityData
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.hairline
import dev.sam.countri.ui.theme.pressScale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * Every visit starts here: when did it start, how many days, which cities.
 * Cities autocomplete from the bundled catalog so they land on the map.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVisitSheet(
    entry: CountryWithState,
    cityData: CityData,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate, cities: List<String>) -> Unit,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val accent = palette.continentColor(entry.country.continent)
    val iso2 = entry.country.iso2

    // LocalDate isn't Bundle-able; persist the epoch day instead.
    var startEpochDay by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val start = LocalDate.ofEpochDay(startEpochDay)
    var days by rememberSaveable { mutableIntStateOf(7) }
    val end = start.plusDays((days - 1).toLong())
    var showDatePicker by remember { mutableStateOf(false) }
    var cities by rememberSaveable { mutableStateOf(listOf<String>()) }
    var draft by rememberSaveable { mutableStateOf("") }
    val suggestions = remember(draft, cities) {
        if (draft.isBlank()) emptyList()
        else cityData.search(iso2, draft).filter { c ->
            cities.none { it.equals(c.name, ignoreCase = true) }
        }
    }

    fun addCity(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty() && cities.none { it.equals(trimmed, ignoreCase = true) }) {
            cities = cities + trimmed
            haptics.tick()
        }
        draft = ""
    }

    val sheetScroll = rememberScrollState()
    // Keep the city suggestions visible above the keyboard.
    LaunchedEffect(suggestions.size) {
        if (suggestions.isNotEmpty()) sheetScroll.animateScrollTo(sheetScroll.maxValue)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(sheetScroll)
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Text(
                "A trip to ${entry.country.name}",
                style = CountriType.title,
                color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 18.dp),
            )

            // ---- when ----
            SectionLabel("When")
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .pressScale(0.97f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface1)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { showDatePicker = true }
                        .padding(14.dp),
                ) {
                    Text(start.format(dateFormat), style = CountriType.subtitle, color = accent)
                    Text(
                        "to ${end.format(dateFormat)}",
                        style = CountriType.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−") { if (days > 1) { days--; haptics.tick() } }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text(days.toString(), style = CountriType.title, color = palette.textPrimary)
                        Text(
                            if (days == 1) "day" else "days",
                            style = CountriType.monoSmall,
                            color = palette.textFaint,
                        )
                    }
                    StepButton("+") { if (days < 365) { days++; haptics.tick() } }
                }
            }

            // ---- cities ----
            SectionLabel("Cities", Modifier.padding(top = 20.dp, bottom = 10.dp))
            if (cities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    cities.forEach { city ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.surface1)
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
                                    ) { cities = cities - city },
                            )
                        }
                    }
                }
            }
            BasicTextField(
                value = draft,
                onValueChange = { if (it.length <= 40) draft = it },
                singleLine = true,
                textStyle = CountriType.body.copy(color = palette.textPrimary),
                cursorBrush = SolidColor(accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.surface1)
                    .padding(14.dp),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) {
                            Text(
                                "Which cities?",
                                style = CountriType.body,
                                color = palette.textFaint,
                            )
                        }
                        inner()
                    }
                },
            )
            if (suggestions.isNotEmpty()) {
                Column(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(palette.surface1)
                ) {
                    suggestions.forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { addCity(city.name) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                city.name,
                                style = CountriType.body,
                                color = palette.textPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                CountriIcons.Plus,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // ---- save ----
            Box(
                modifier = Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .pressScale(0.97f)
                    .clip(CircleShape)
                    .background(accent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        if (draft.isNotBlank()) addCity(draft)
                        haptics.confirm()
                        onSave(start, end, cities)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("Stamp it", style = CountriType.subtitle, color = palette.onVisited)
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        startEpochDay = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()
                    }
                    showDatePicker = false
                }) {
                    Text("Set", color = accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit) {
    val palette = Countri.palette
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(0.9f)
            .clip(CircleShape)
            .background(palette.surface1)
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
