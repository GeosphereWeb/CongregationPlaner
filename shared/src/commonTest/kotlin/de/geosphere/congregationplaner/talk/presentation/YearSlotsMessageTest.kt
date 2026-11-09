package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.usecase.YearSlotsResult
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class YearSlotsMessageTest {
    @Test
    fun `all slots are new`() {
        assertEquals("52 Termine für 2026 angelegt.", yearSlotsMessage(2026, YearSlotsResult(52, 0)))
        assertEquals("1 Termin für 2026 angelegt.", yearSlotsMessage(2026, YearSlotsResult(1, 0)))
    }

    @Test
    fun `some slots existed already`() {
        assertEquals(
            "51 Termine für 2026 angelegt, 1 waren schon vorhanden.",
            yearSlotsMessage(2026, YearSlotsResult(51, 1)),
        )
    }

    @Test
    fun `nothing is new`() {
        assertEquals("Für 2026 waren schon alle 52 Termine angelegt.", yearSlotsMessage(2026, YearSlotsResult(0, 52)))
        assertEquals("Für 2026 waren schon alle 1 Termin angelegt.", yearSlotsMessage(2026, YearSlotsResult(0, 1)))
    }

    @Test
    fun `year time message names the count and the time`() {
        val time = LocalTime(9, 30)
        assertEquals("Uhrzeit von 52 Terminen in 2027 auf 09:30 Uhr geändert.", yearTimeMessage(2027, 52, time))
        assertEquals("Uhrzeit von 1 Termin in 2027 auf 09:30 Uhr geändert.", yearTimeMessage(2027, 1, time))
        assertEquals("Alle Termine 2027 hatten schon 09:30 Uhr.", yearTimeMessage(2027, 0, time))
    }
}
