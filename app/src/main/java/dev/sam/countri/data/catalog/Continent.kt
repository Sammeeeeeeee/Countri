package dev.sam.countri.data.catalog

enum class Continent(val displayName: String, val total: Int) {
    AFRICA("Africa", 54),
    ASIA("Asia", 48),
    EUROPE("Europe", 44),
    N_AMERICA("N. America", 23),
    S_AMERICA("S. America", 12),
    OCEANIA("Oceania", 14),
}

const val WORLD_COUNTRY_COUNT = 195
