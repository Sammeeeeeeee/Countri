package dev.sam.countri.data.catalog

enum class Continent(val displayName: String) {
    AFRICA("Africa"),
    ASIA("Asia"),
    EUROPE("Europe"),
    N_AMERICA("N. America"),
    S_AMERICA("S. America"),
    OCEANIA("Oceania");

    /**
     * How many catalog countries sit on this continent. Derived so the totals
     * can never drift out of step with the catalog itself — add a country and
     * its continent's denominator moves with it. Computed lazily on first use
     * to avoid a class-init cycle with [CountryCatalog], whose rows reference
     * these entries.
     */
    val total: Int get() = CountryCatalog.all.count { it.continent == this }
}

/** The world the app measures against — exactly the catalog it ships with. */
val WORLD_COUNTRY_COUNT: Int get() = CountryCatalog.all.size
