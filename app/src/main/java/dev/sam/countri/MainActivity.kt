package dev.sam.countri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.rememberHaptics
import dev.sam.countri.ui.nav.CountriRoot
import dev.sam.countri.ui.theme.CountriTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var onboardingSeen by mutableStateOf<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash a beat while the start destination is decided.
        splash.setKeepOnScreenCondition { onboardingSeen == null }
        lifecycleScope.launch {
            onboardingSeen = (application as CountriApp).container.onboardingPrefs.seen.first()
        }

        setContent {
            val seen = onboardingSeen ?: return@setContent
            CountriTheme {
                CompositionLocalProvider(LocalHaptics provides rememberHaptics()) {
                    CountriRoot(startAtOnboarding = !seen)
                }
            }
        }
    }
}
