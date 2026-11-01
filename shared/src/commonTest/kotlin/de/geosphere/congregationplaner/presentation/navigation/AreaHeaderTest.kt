package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class AreaHeaderTest {
    private val congregation = SelectedCongregation(
        ref = CongregationRef("emea", "europe", "central", "609a", "berlin-mitte"),
        name = "Berlin-Mitte",
    )

    @Test
    fun `date is written in german with weekday and month`() {
        assertEquals("Sonntag, 1. November", germanDayAndMonth(LocalDate(2026, 11, 1)))
        assertEquals("Montag, 15. März", germanDayAndMonth(LocalDate(2027, 3, 15)))
    }

    @Test
    fun `home header shows the congregation in capitals`() {
        val header = homeHeader(congregation, LocalDate(2026, 11, 1))
        assertEquals(AreaHeader("BERLIN-MITTE", listOf("Sonntag, 1. November"), "Diese Woche"), header)
    }

    @Test
    fun `congregation header shows circuit and name`() {
        assertEquals(AreaHeader("KREIS 609a", listOf("Versammlung"), "Berlin-Mitte"), congregationHeader(congregation))
    }

    @Test
    fun `global admin without congregation gets empty lines`() {
        assertEquals("", homeHeader(null, LocalDate(2026, 11, 1)).stepLabel)
        assertEquals(AreaHeader("", listOf("Versammlung"), ""), congregationHeader(null))
    }

    @Test
    fun `global header has no previous line`() {
        assertEquals(AreaHeader("GLOBAL", emptyList(), "Globale Verwaltung"), globalHeader())
    }
}
