package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalDirectoryLabelsTest {
    private val north = ExternalCongregation("n", "c1", "Nord")
    private val south = ExternalCongregation("s", "c2", "Süd")
    private val directory = ExternalDirectory(
        circuits = listOf(ExternalCircuit("c1", "609a"), ExternalCircuit("c2", "610b")),
        congregations = listOf(north, south),
        speakers = listOf(
            VisitingSpeaker("1", "n", "Muster", "Max"),
            VisitingSpeaker("2", "n", "Berg"),
            VisitingSpeaker("3", "s", "Lind", "Eva"),
        ),
    )

    @Test
    fun `the congregation count uses the singular for one`() {
        assertEquals("0 Versammlungen", congregationCountLabel(0))
        assertEquals("1 Versammlung", congregationCountLabel(1))
        assertEquals("2 Versammlungen", congregationCountLabel(2))
        assertEquals("2 Redner", speakerCountLabel(2))
    }

    @Test
    fun `congregations and speakers are listed under their parent`() {
        assertEquals(listOf(north), directory.congregationsIn("c1"))
        assertEquals(listOf("1", "2"), directory.speakersIn("n").map { it.id })
        assertEquals(emptyList(), directory.speakersIn("unknown"))
    }

    @Test
    fun `speaker names leave out a missing first name`() {
        assertEquals("Muster, Max", directory.speakers[0].displayName())
        assertEquals("Berg", directory.speakers[1].displayName())
    }

    @Test
    fun `congregations are labelled with their circuit`() {
        assertEquals("Nord (609a)", directory.congregationLabel(north))
        assertEquals("Süd (610b)", directory.congregationLabel("s"))
        assertEquals("", directory.congregationLabel("unknown"))
        assertEquals("", directory.circuitName("unknown"))
    }

    @Test
    fun `filter summaries name the choice or count it`() {
        assertEquals("Alle Kreise", directory.circuitFilterSummary(DirectoryFilter()))
        assertEquals("Alle Versammlungen", directory.congregationFilterSummary(DirectoryFilter()))

        val filter = DirectoryFilter(circuitIds = setOf("c1"), congregationIds = setOf("n", "s"))
        assertEquals("609a", directory.circuitFilterSummary(filter))
        // Mit gewähltem Kreis zählen nur dessen Versammlungen.
        assertEquals("Nord", directory.congregationFilterSummary(filter))
        assertEquals(
            "2 Versammlungen gewählt",
            directory.congregationFilterSummary(DirectoryFilter(congregationIds = setOf("n", "s"))),
        )
    }
}
