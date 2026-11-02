package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.usecase.YearSlotsResult
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
}
