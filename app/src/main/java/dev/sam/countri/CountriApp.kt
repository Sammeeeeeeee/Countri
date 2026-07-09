package dev.sam.countri

import android.app.Application
import dev.sam.countri.data.db.AppDatabase
import dev.sam.countri.data.map.WorldMapAsset
import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.data.prefs.OnboardingPrefs
import dev.sam.countri.data.repo.AtlasRepository

/**
 * Hand-rolled DI: one container, four lazies. The app is small enough that
 * a framework would only add ceremony.
 */
class AppContainer(private val app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val repository: AtlasRepository by lazy { AtlasRepository(database.countryStateDao()) }
    val onboardingPrefs: OnboardingPrefs by lazy { OnboardingPrefs(app) }
    val worldMap: WorldMapData by lazy {
        app.assets.open("worldmap.bin").use { WorldMapAsset.parse(it.readBytes()) }
    }
}

class CountriApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
