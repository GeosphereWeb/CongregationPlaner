package de.geosphere.congregationplaner.talk.presentation.schedule

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class PlanningCutoffTextsTest {
    @Test
    fun `a cut-off date is shown with its weekday and a missing one says so`() {
        assertEquals("Redner frei seit So, 02.11.2025", cutoffLabel("Redner", LocalDate(2025, 11, 2)))
        assertEquals("Vorträge: kein Stichtag", cutoffLabel("Vorträge", null))
    }
}
