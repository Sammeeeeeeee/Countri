package dev.sam.countri.ui.nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.Springs

@Composable
fun CountriRoot(startAtOnboarding: Boolean = false) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination

    val currentTab = when {
        destination == null -> null
        destination.hasRoute<AtlasRoute>() -> CountriTab.Atlas
        destination.hasRoute<PassportRoute>() -> CountriTab.Passport
        destination.hasRoute<StatsRoute>() -> CountriTab.Stats
        destination.hasRoute<WishlistRoute>() -> CountriTab.Wishlist
        else -> null
    }

    Scaffold(
        containerColor = Countri.palette.canvas,
        bottomBar = {
            AnimatedVisibility(
                visible = currentTab != null,
                enter = slideInVertically(Springs.SmoothOffset) { it } + fadeIn(),
                exit = slideOutVertically(Springs.SmoothOffset) { it } + fadeOut(),
            ) {
                CountriBottomBar(
                    current = currentTab,
                    onTab = { tab -> navController.navigateToTab(tab) },
                    onAdd = { navController.navigate(AddRoute) },
                )
            }
        },
    ) { padding ->
        CountriNavHost(
            navController = navController,
            startAtOnboarding = startAtOnboarding,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

private fun NavHostController.navigateToTab(tab: CountriTab) {
    val route: Any = when (tab) {
        CountriTab.Atlas -> AtlasRoute
        CountriTab.Passport -> PassportRoute
        CountriTab.Stats -> StatsRoute
        CountriTab.Wishlist -> WishlistRoute
    }
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun CountriNavHost(
    navController: NavHostController,
    startAtOnboarding: Boolean,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = if (startAtOnboarding) OnboardingRoute else AtlasRoute,
        modifier = modifier,
        // Tab-to-tab default: quick fade-through with a whisper of scale.
        enterTransition = {
            fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))
        },
        exitTransition = { fadeOut(tween(90)) },
        popEnterTransition = {
            fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220))
        },
        popExitTransition = { fadeOut(tween(90)) },
    ) {
        composable<OnboardingRoute>(
            exitTransition = { fadeOut(tween(500)) },
        ) {
            PlaceholderScreen("Onboarding")
        }
        composable<AtlasRoute> { PlaceholderScreen("Atlas") }
        composable<PassportRoute> { PlaceholderScreen("Passport") }
        composable<StatsRoute> { PlaceholderScreen("Stats") }
        composable<WishlistRoute> { PlaceholderScreen("Wishlist") }
        composable<AddRoute>(
            enterTransition = { slideInVertically(Springs.SmoothOffset) { it } + fadeIn() },
            exitTransition = { fadeOut(tween(90)) },
            popExitTransition = { slideOutVertically(Springs.SmoothOffset) { it } + fadeOut() },
        ) {
            PlaceholderScreen("Add")
        }
        composable<DetailRoute>(
            enterTransition = { slideInHorizontally(Springs.SmoothOffset) { it / 3 } + fadeIn() },
            popExitTransition = { slideOutHorizontally(Springs.SmoothOffset) { it / 3 } + fadeOut() },
        ) {
            PlaceholderScreen("Detail")
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = CountriType.title, color = Countri.palette.textSecondary)
    }
}
