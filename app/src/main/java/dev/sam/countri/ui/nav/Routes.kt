package dev.sam.countri.ui.nav

import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute
@Serializable data object AtlasRoute
@Serializable data object PassportRoute
@Serializable data object StatsRoute
@Serializable data object WishlistRoute
@Serializable data object AddRoute
@Serializable data class DetailRoute(val iso2: String)
