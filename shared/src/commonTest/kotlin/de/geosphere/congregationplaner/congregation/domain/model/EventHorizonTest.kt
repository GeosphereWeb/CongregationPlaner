package de.geosphere.congregationplaner.congregation.domain.model

import de.geosphere.congregationplaner.congregation.domain.model.EventHorizon.Unit.DAYS
import de.geosphere.congregationplaner.congregation.domain.model.EventHorizon.Unit.MONTHS
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventHorizonTest {
    @Test
    fun `the stored value round trips and broken values fall back to three months`() {
        assertEquals("3M", EventHorizon().encode())
        assertEquals(EventHorizon(30, DAYS), EventHorizon.decode("30D"))
        assertEquals(EventHorizon(24, MONTHS), EventHorizon.decode(EventHorizon(24, MONTHS).encode()))
        listOf(null, "", "M", "25M", "0D", "366D", "3X", "xD").forEach {
            assertEquals(EventHorizon(), EventHorizon.decode(it), "value $it")
        }
    }

    @Test
    fun `the last day counts days or calendar months from today`() {
        val today = LocalDate(2027, 1, 31)

        assertEquals(LocalDate(2027, 2, 14), EventHorizon(14, DAYS).lastDay(today))
        assertEquals(LocalDate(2027, 2, 28), EventHorizon(1, MONTHS).lastDay(today))
    }

    @Test
    fun `the limits depend on the unit`() {
        assertTrue(EventHorizon(365, DAYS).isValid)
        assertFalse(EventHorizon(366, DAYS).isValid)
        assertTrue(EventHorizon(24, MONTHS).isValid)
        assertFalse(EventHorizon(25, MONTHS).isValid)
        assertFalse(EventHorizon(0, MONTHS).isValid)
    }

    @Test
    fun `the amount field keeps at most three digits and an empty field is invalid`() {
        assertEquals(EventHorizon(12), EventHorizon().withAmountText("1a2"))
        assertEquals(EventHorizon(123, DAYS), EventHorizon(1, DAYS).withAmountText("12345"))
        assertEquals("", EventHorizon().withAmountText("").amountText)
        assertEquals("3", EventHorizon().amountText)
    }
}
