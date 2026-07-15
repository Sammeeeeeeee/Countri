package dev.sam.countri

import dev.sam.countri.domain.Visit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class VisitTest {

    private fun visit(start: String, end: String) =
        Visit(1, LocalDate.parse(start), LocalDate.parse(end), emptyList())

    @Test
    fun `days is inclusive`() {
        assertEquals(1, visit("2020-01-01", "2020-01-01").days)
        assertEquals(8, visit("2020-01-01", "2020-01-08").days)
    }

    @Test
    fun `a reversed range never yields zero or negative days`() {
        // A corrupt or hand-edited backup with end before start must not be
        // able to poison the timeline totals with a negative day count.
        assertEquals(1, visit("2020-01-10", "2020-01-01").days)
    }
}
