package dev.sam.countri

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.sam.countri.ui.components.LocalHaptics
import dev.sam.countri.ui.components.rememberHaptics
import dev.sam.countri.ui.nav.CountriRoot
import dev.sam.countri.ui.theme.CountriTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CountriTheme {
                CompositionLocalProvider(LocalHaptics provides rememberHaptics()) {
                    CountriRoot()
                }
            }
        }
    }
}
