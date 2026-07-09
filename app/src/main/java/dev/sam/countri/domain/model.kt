package dev.sam.countri.domain

import dev.sam.countri.data.catalog.Country

enum class CountryStatus { VISITED, WISHLIST }

data class CountryWithState(
    val country: Country,
    val status: CountryStatus? = null,
    val firstVisitYear: Int? = null,
    val note: String? = null,
    val cities: List<String> = emptyList(),
    val trips: Int = 0,
) {
    val isVisited get() = status == CountryStatus.VISITED
    val isWishlist get() = status == CountryStatus.WISHLIST
}
