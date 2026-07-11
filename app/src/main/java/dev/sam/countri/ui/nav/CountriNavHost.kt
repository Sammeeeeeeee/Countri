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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.sam.countri.ui.AtlasViewModel
import dev.sam.countri.ui.add.AddCountryScreen
import dev.sam.countri.ui.atlas.AtlasScreen
import dev.sam.countri.ui.detail.CountryDetailScreen
import dev.sam.countri.ui.onboarding.OnboardingScreen
import dev.sam.countri.ui.passport.PassportScreen
import dev.sam.countri.ui.stats.StatsScreen
import dev.sam.countri.ui.theme.Countri
import dev.sam.countri.ui.theme.CountriType
import dev.sam.countri.ui.theme.Springs
import dev.sam.countri.ui.wishlist.WishlistScreen

@Composable
fun CountriRoot(startAtOnboarding: Boolean = false) {
    val viewModel: AtlasViewModel = viewModel(factory = AtlasViewModel.Factory)
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
                    // On the Atlas the recent-journeys panel runs into the bar.
                    topStripColor = if (currentTab == CountriTab.Atlas) {
                        Countri.palette.surface1
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                )
            }
        },
    ) { padding ->
        CountriNavHost(
            navController = navController,
            viewModel = viewModel,
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
    viewModel: AtlasViewModel,
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
            OnboardingScreen(
                viewModel = viewModel,
                onDone = {
                    viewModel.markOnboardingSeen()
                    navController.navigate(AtlasRoute) {
                        popUpTo<OnboardingRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<AtlasRoute> {
            AtlasScreen(
                viewModel = viewModel,
                onCountryClick = { iso -> navController.navigate(DetailRoute(iso)) },
                onSeePassport = { navController.navigateToTab(CountriTab.Passport) },
            )
        }
        composable<PassportRoute> {
            PassportScreen(
                viewModel = viewModel,
                onCountryClick = { iso -> navController.navigate(DetailRoute(iso)) },
            )
        }
        composable<StatsRoute> { StatsScreen(viewModel = viewModel) }
        composable<WishlistRoute> {
            WishlistScreen(
                viewModel = viewModel,
                onCountryClick = { iso -> navController.navigate(DetailRoute(iso)) },
            )
        }
        composable<AddRoute>(
            enterTransition = { slideInVertically(Springs.SmoothOffset) { it } + fadeIn() },
            exitTransition = { fadeOut(tween(90)) },
            popExitTransition = { slideOutVertically(Springs.SmoothOffset) { it } + fadeOut() },
        ) {
            AddCountryScreen(
                viewModel = viewModel,
                onClose = { navController.popBackStack() },
                onAdded = { iso ->
                    navController.navigate(DetailRoute(iso)) {
                        popUpTo<AddRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<DetailRoute>(
            enterTransition = { slideInHorizontally(Springs.SmoothOffset) { it / 3 } + fadeIn() },
            popExitTransition = { slideOutHorizontally(Springs.SmoothOffset) { it / 3 } + fadeOut() },
        ) { entry ->
            val route = entry.toRoute<DetailRoute>()
            CountryDetailScreen(
                viewModel = viewModel,
                iso2 = route.iso2,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, style = CountriType.title, color = Countri.palette.textSecondary)
    }
}
