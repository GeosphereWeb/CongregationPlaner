package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalDirectoryFilterTest {
    private val north = ExternalCircuit("c1", "Nord")
    private val south = ExternalCircuit("c2", "Süd")
    private val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
    private val berg = ExternalCongregation("g2", "c1", "Bergheim")
    private val zell = ExternalCongregation("g3", "c2", "Altdorf") // same name in another circuit
    private val lea = VisitingSpeaker("s1", "g1", "Berg", "Lea")
    private val tim = VisitingSpeaker("s2", "g2", "Zorn", "Tim")
    private val eva = VisitingSpeaker("s3", "g3", "Alt", "Eva")
    private val directory = ExternalDirectory(listOf(north, south), listOf(altdorf, berg, zell), listOf(lea, tim, eva))

    @Test
    fun `no filter shows everything`() {
        assertEquals(directory, directory.filtered(DirectoryFilter()))
        assertFalse(DirectoryFilter().isActive)
    }

    @Test
    fun `a circuit narrows congregations and speakers`() {
        val shown = directory.filtered(DirectoryFilter(circuitId = "c1"))

        assertEquals(listOf(north), shown.circuits)
        assertEquals(listOf(altdorf, berg), shown.congregations)
        assertEquals(listOf(lea, tim), shown.speakers)
    }

    @Test
    fun `a congregation without a circuit shows just that congregation under its circuit`() {
        val shown = directory.filtered(DirectoryFilter(congregationId = "g3"))

        assertEquals(listOf(south), shown.circuits)
        assertEquals(listOf(zell), shown.congregations)
        assertEquals(listOf(eva), shown.speakers)
    }

    @Test
    fun `circuit and congregation together narrow to the congregation`() {
        val shown = directory.filtered(DirectoryFilter("c1", "g2"))

        assertEquals(listOf(berg), shown.congregations)
        assertEquals(listOf(tim), shown.speakers)
    }

    @Test
    fun `an ID that no longer exists is ignored`() {
        assertEquals(directory, directory.filtered(DirectoryFilter("gone", "gone-too")))
    }

    @Test
    fun `without a circuit all congregations are offered and named with their circuit`() {
        val options = directory.congregationOptions(circuitId = null)

        assertEquals(
            listOf(null, "g1", "g2", "g3"),
            options.map { it.id },
        )
        assertEquals("Altdorf (Nord)", options[1].label)
        assertEquals("Altdorf (Süd)", options[3].label)
        assertEquals("Alle Versammlungen", options.first().label)
    }

    @Test
    fun `with a circuit only its congregations are offered`() {
        val options = directory.congregationOptions(circuitId = "c1")

        assertEquals(listOf(null, "g1", "g2"), options.map { it.id })
        assertEquals("Altdorf", options[1].label)
    }

    @Test
    fun `circuit options start with all circuits`() {
        assertEquals(listOf(null, "c1", "c2"), directory.circuitOptions().map { it.id })
    }

    @Test
    fun `choosing another circuit drops a congregation that does not belong to it`() {
        val filter = DirectoryFilter(congregationId = "g1")

        assertEquals(DirectoryFilter("c1", "g1"), filter.withCircuit(directory, "c1"))
        assertEquals(DirectoryFilter("c2", null), filter.withCircuit(directory, "c2"))
        assertEquals(DirectoryFilter(null, "g1"), filter.withCircuit(directory, null))
        assertTrue(filter.isActive)
    }
}
