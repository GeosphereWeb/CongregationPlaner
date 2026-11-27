package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadOnlyViewTest {
    private val north = ExternalCircuit("c1", "Nord")
    private val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
    private val directory = ExternalDirectory(listOf(north), listOf(altdorf), emptyList())

    @Test
    fun `a circuit shows its name`() {
        assertEquals(
            ReadOnlyView("Kreis", listOf("Nord")),
            readOnlyView(ExternalDirectoryEditor.Circuit(north, north), directory),
        )
    }

    @Test
    fun `a congregation shows its circuit`() {
        assertEquals(
            ReadOnlyView("Versammlung", listOf("Altdorf", "Kreis: Nord")),
            readOnlyView(ExternalDirectoryEditor.Congregation(altdorf, altdorf), directory),
        )
    }

    @Test
    fun `a speaker shows every filled detail`() {
        val speaker = VisitingSpeaker(
            id = "s1",
            congregationId = "g1",
            lastName = "Berg",
            mobilePhone = "0170 1",
            landlinePhone = "08092 2",
            email = "lea@example.org",
        )

        val view = readOnlyView(ExternalDirectoryEditor.Speaker(speaker, "", speaker), directory)

        assertEquals(
            listOf("Berg", "Versammlung: Altdorf", "Handy: 0170 1", "Festnetz: 08092 2", "E-Mail: lea@example.org"),
            view.lines,
        )
    }

    @Test
    fun `a speaker without details and congregation shows only the name`() {
        val speaker = VisitingSpeaker(id = "s1", congregationId = "unknown", lastName = "Berg")

        val view = readOnlyView(ExternalDirectoryEditor.Speaker(speaker, "", speaker), directory)

        assertEquals(listOf("Berg"), view.lines)
    }

    @Test
    fun `a speaker shows only the filled details and never the rating`() {
        val speaker = VisitingSpeaker(
            id = "s1",
            congregationId = "g1",
            lastName = "Berg",
            firstName = "Lea",
            mobilePhone = "0170 1",
            talkNumbers = listOf(12, 45),
            ranking = 3,
        )

        val view = readOnlyView(ExternalDirectoryEditor.Speaker(speaker, "12, 45", speaker), directory)

        assertEquals("Redner", view.title)
        assertEquals(
            // The rating is the planner's own judgement; the read-only view (talk helpers) never shows it.
            listOf("Berg, Lea", "Versammlung: Altdorf", "Handy: 0170 1", "Vorträge: 12, 45"),
            view.lines,
        )
    }

    @Test
    fun `an excluded speaker says so`() {
        val speaker = VisitingSpeaker(id = "s1", congregationId = "g1", lastName = "Berg", excludedFromPlanning = true)

        val view = readOnlyView(ExternalDirectoryEditor.Speaker(speaker, "", speaker), directory)

        assertEquals(listOf("Berg", "Versammlung: Altdorf", "Von der Planung ausgeschlossen"), view.lines)
    }
}
