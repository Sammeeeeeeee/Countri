package dev.sam.countri.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import dev.sam.countri.data.cities.CityData
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.domain.Visit
import dev.sam.countri.ui.components.CountriIcons
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.SectionLabel
import dev.sam.countri.ui.components.tapTarget
import dev.sam.countri.ui.share.VisitCardRenderer
import dev.sam.countri.ui.share.shareBitmap
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.pressScale
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/**
 * Add or edit one visit: which cities, when, how long. Cities lead the
 * sheet so the suggestion list always sits above the keyboard, and the
 * country's biggest cities show before a single letter is typed.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVisitSheet(
    entry: CountryWithState,
    cityData: CityData,
    onDismiss: () -> Unit,
    onSave: (start: LocalDate, end: LocalDate, cities: List<String>) -> Unit,
    existing: Visit? = null,
) {
    val palette = Countri.palette
    val haptics = LocalHaptics.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val iso2 = entry.country.iso2

    var startEpochDay by remember(existing) {
        mutableStateOf((existing?.start ?: LocalDate.now()).toEpochDay())
    }
    val start = LocalDate.ofEpochDay(startEpochDay)
    var days by remember(existing) { mutableIntStateOf(existing?.days ?: 7) }
    val end = start.plusDays((days - 1).toLong())
    var showDatePicker by remember { mutableStateOf(false) }
    var cities by remember(existing) { mutableStateOf(existing?.cities ?: emptyList()) }
    var draft by remember { mutableStateOf("") }
    // Guards against a double-tap on Save firing two inserts before the sheet
    // has had a frame to dismiss.
    var saving by remember { mutableStateOf(false) }

    // Big cities appear immediately; typing narrows them.
    val suggestions = remember(draft, cities) {
        cityData.search(iso2, draft).filter { c ->
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.surface2,
        contentWindowInsets = { WindowInsets(0) },
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp)
                .imePadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (existing != null) "Edit visit" else "A trip to ${entry.country.name}",
                    style = CountriType.title,
                    color = palette.textPrimary,
                )
                if (existing != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .pressScale(0.9f)
                            .clip(CircleShape)
                            .background(palette.recessed)
                            .tapTarget(onClickLabel = "Share this visit") {
                                haptics.tick()
                                shareBitmap(
                                    context,
                                    VisitCardRenderer.render(context, entry, existing),
                                    "countri-visit.png",
                                    "Share your trip",
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            CountriIcons.Share,
                            contentDescription = "Share visit",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            // ---- cities first: field + live suggestions stay above the keyboard ----
            SectionLabel("Cities")
            if (cities.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    cities.forEach { city ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(palette.recessed)
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
                                    .tapTarget(onClickLabel = "Remove $city") { cities = cities - city },
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
                cursorBrush = SolidColor(palette.visited),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(palette.recessed)
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
                        .clip(RoundedCornerShape(20.dp))
                        .background(palette.recessed)
                ) {
                    suggestions.take(4).forEach { city ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tapTarget(onClickLabel = "Add ${city.name}") { addCity(city.name) }
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
                                tint = palette.textSecondary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }

            // ---- when ----
            SectionLabel("When", Modifier.padding(top = 20.dp))
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
                        .background(palette.recessed)
                        .tapTarget(onClickLabel = "Change start date") { showDatePicker = true }
                        .padding(14.dp),
                ) {
                    Text(start.format(dateFormat), style = CountriType.subtitle, color = palette.textPrimary)
                    Text(
                        "to ${end.format(dateFormat)}",
                        style = CountriType.bodySmall,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−", "One fewer day") { if (days > 1) { days--; haptics.tick() } }
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
                    StepButton("+", "One more day") { if (days < 365) { days++; haptics.tick() } }
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
                    .background(palette.visited)
                    .tapTarget(onClickLabel = if (existing != null) "Save visit" else "Save trip") {
                        if (saving) return@tapTarget
                        saving = true
                        if (draft.isNotBlank()) addCity(draft)
                        haptics.confirm()
                        onSave(start, end, cities)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (existing != null) "Save" else "Stamp it",
                    style = CountriType.subtitle,
                    color = palette.onVisited,
                )
            }
        }
    }

    if (showDatePicker) {
        // A trip can't start in the future — bound the picker to today and back.
        val nowMillis = System.currentTimeMillis()
        val thisYear = Year.now().value
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= nowMillis
                override fun isSelectableYear(year: Int): Boolean = year <= thisYear
            },
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
                    Text("Set", color = palette.textPrimary)
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
private fun StepButton(glyph: String, clickLabel: String, onClick: () -> Unit) {
    val palette = Countri.palette
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(0.9f)
            .clip(CircleShape)
            .background(palette.recessed)
            .tapTarget(onClickLabel = clickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, style = CountriType.subtitle, color = palette.textPrimary)
    }
}
