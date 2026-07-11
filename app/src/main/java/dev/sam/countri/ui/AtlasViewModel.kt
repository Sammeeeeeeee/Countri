package dev.sam.countri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.sam.countri.CountriApp
import dev.sam.countri.data.catalog.CountryCatalog
import dev.sam.countri.data.cities.CityData
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.data.prefs.OnboardingPrefs
import dev.sam.countri.data.repo.AtlasRepository
import dev.sam.countri.domain.AtlasStats
import dev.sam.countri.domain.CountryStatus
import dev.sam.countri.domain.CountryWithState
import dev.sam.countri.domain.StatsCalculator
import dev.sam.countri.ui.map.MapMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Year

/**
 * One activity-scoped store for the whole app — every screen reads the same
 * catalog+state flow, so an add on one screen glows on all of them.
 */
class AtlasViewModel(
    private val repository: AtlasRepository,
    private val onboardingPrefs: OnboardingPrefs,
    val worldMap: WorldMapData,
    val cities: CityData,
) : ViewModel() {

    private val emptyAtlas = CountryCatalog.all.map { CountryWithState(it) }

    val countries: StateFlow<List<CountryWithState>> = repository.countries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyAtlas)

    val stats: StateFlow<AtlasStats> = countries
        .map { StatsCalculator.calculate(it, Year.now().value) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            StatsCalculator.calculate(emptyAtlas, Year.now().value),
        )

    val onboardingSeen: Flow<Boolean> = onboardingPrefs.seen

    /** Atlas map view state survives tab hops. */
    var mapMode by mutableStateOf(MapMode.Flat)

    /** The globe-unwrap opening plays once per process. */
    var introPlayed: Boolean = false

    /** Country that just became visited — the map plays a pulse on it. */
    val justAdded: StateFlow<String?> get() = _justAdded
    private val _justAdded = MutableStateFlow<String?>(null)
    private var justAddedJob: Job? = null

    fun byIso(iso2: String): CountryWithState? =
        countries.value.firstOrNull { it.country.iso2 == iso2 }

    fun setStatus(iso2: String, status: CountryStatus) {
        val existing = byIso(iso2)
        viewModelScope.launch {
            repository.setStatus(iso2, status, existing)
            if (status == CountryStatus.VISITED) pulse(iso2)
        }
    }

    /** Records a trip; the country becomes visited if it wasn't already. */
    fun addVisit(iso2: String, start: LocalDate, end: LocalDate, visitCities: List<String>) {
        val existing = byIso(iso2)
        viewModelScope.launch {
            repository.addVisit(iso2, start, end, visitCities, existing)
            pulse(iso2)
        }
    }

    fun updateVisit(id: Long, iso2: String, start: LocalDate, end: LocalDate, visitCities: List<String>) {
        viewModelScope.launch { repository.updateVisit(id, iso2, start, end, visitCities) }
    }

    fun deleteVisit(id: Long) {
        viewModelScope.launch { repository.deleteVisit(id) }
    }

    fun updateDetails(
        iso2: String,
        firstVisitYear: Int? = null,
        note: String? = null,
        places: List<String>? = null,
        trips: Int? = null,
    ) {
        val current = byIso(iso2) ?: return
        viewModelScope.launch {
            repository.updateDetails(
                current = current,
                firstVisitYear = firstVisitYear ?: current.firstVisitYear,
                note = note ?: current.note,
                places = places ?: current.places,
                trips = trips ?: current.trips,
            )
        }
    }

    fun clear(iso2: String) {
        viewModelScope.launch { repository.clear(iso2) }
    }

    fun markOnboardingSeen() {
        viewModelScope.launch { onboardingPrefs.markSeen() }
    }

    suspend fun exportBackup(): String = repository.exportBackup()

    suspend fun importBackup(text: String): Boolean =
        runCatching { repository.importBackup(text) }.isSuccess

    private fun pulse(iso2: String) {
        justAddedJob?.cancel()
        _justAdded.value = iso2
        justAddedJob = viewModelScope.launch {
            delay(2_800)
            _justAdded.value = null
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CountriApp
                AtlasViewModel(
                    repository = app.container.repository,
                    onboardingPrefs = app.container.onboardingPrefs,
                    worldMap = app.container.worldMap,
                    cities = app.container.cities,
                )
            }
        }
    }
}
