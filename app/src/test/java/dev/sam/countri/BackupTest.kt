package dev.sam.countri

import dev.sam.countri.data.backup.Backup
import dev.sam.countri.data.db.CountryStateEntity
import dev.sam.countri.data.db.VisitEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupTest {

    @Test
    fun `encode then decode round-trips states and visits`() {
        val states = listOf(
            CountryStateEntity("FR", "VISITED", 2018, "lovely", listOf("Louvre"), 2),
            CountryStateEntity("NO", "WISHLIST", null, null, emptyList(), 0),
        )
        val visits = listOf(VisitEntity(1, "FR", 100L, 107L, listOf("Paris")))

        val decoded = Backup.decode(Backup.encode(states, visits))

        assertEquals(Backup.CURRENT_VERSION, decoded.version)
        assertEquals(listOf("FR", "NO"), decoded.states.map { it.iso2 })
        assertEquals("VISITED", decoded.states.first().status)
        assertEquals(listOf("Paris"), decoded.visits.single().cities)
        assertEquals(100L, decoded.visits.single().startDay)
    }

    @Test
    fun `a newer, unsupported version is rejected`() {
        val future = """{"version":999,"states":[],"visits":[]}"""
        assertThrows(IllegalArgumentException::class.java) { Backup.decode(future) }
    }

    @Test
    fun `a foreign json without a version is rejected rather than read as empty`() {
        // The exact hazard: valid JSON, wrong shape. It must throw so a restore
        // aborts before deleting anything — not decode to an empty atlas.
        assertThrows(Exception::class.java) { Backup.decode("""{"hello":"world"}""") }
    }

    @Test
    fun `invalid text is rejected`() {
        assertThrows(Exception::class.java) { Backup.decode("not json at all") }
    }
}
