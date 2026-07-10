package dev.sam.countri.data.cities

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class City(val name: String, val iso2: String, val lat: Float, val lon: Float)

/**
 * The bundled city catalog (GeoNames-derived, see tools/generate_cities.py).
 * Cities are stored sorted by country then population, so per-country
 * queries are contiguous slices and "first N" means "biggest N".
 */
class CityData(private val cities: List<City>) {

    private val byCountry: Map<String, List<City>> = cities.groupBy { it.iso2 }

    fun citiesOf(iso2: String): List<City> = byCountry[iso2] ?: emptyList()

    /** Prefix-first autocomplete within one country. */
    fun search(iso2: String, query: String, limit: Int = 6): List<City> {
        val q = query.trim()
        if (q.isEmpty()) return citiesOf(iso2).take(limit)
        val all = citiesOf(iso2)
        val prefix = all.filter { it.name.startsWith(q, ignoreCase = true) }
        val contains = all.filter {
            !it.name.startsWith(q, ignoreCase = true) && it.name.contains(q, ignoreCase = true)
        }
        return (prefix + contains).take(limit)
    }

    fun find(iso2: String, name: String): City? =
        citiesOf(iso2).firstOrNull { it.name.equals(name, ignoreCase = true) }
}

object CityCatalog {

    fun parse(bytes: ByteArray): CityData {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(
            buf.get() == 'C'.code.toByte() && buf.get() == 'C'.code.toByte() &&
                buf.get() == 'I'.code.toByte() && buf.get() == 'T'.code.toByte()
        ) { "cities.bin: bad magic" }
        val version = buf.get().toInt()
        require(version == 1) { "cities.bin: unsupported version $version" }
        val count = buf.short.toInt() and 0xFFFF
        val cities = ArrayList<City>(count)
        val nameBytes = ByteArray(255)
        repeat(count) {
            val iso2 = "${buf.get().toInt().toChar()}${buf.get().toInt().toChar()}"
            val lat = buf.short / 100f
            val lon = buf.short / 100f
            val len = buf.get().toInt() and 0xFF
            buf.get(nameBytes, 0, len)
            cities.add(City(String(nameBytes, 0, len, Charsets.UTF_8), iso2, lat, lon))
        }
        return CityData(cities)
    }
}
