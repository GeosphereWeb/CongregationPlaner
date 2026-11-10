package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.presentation.picker.label
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalDirectoryFilterTest {
    private val north = ExternalCircuit("c1", "Nord")
    private val south = ExternalCircuit("c2", "Süd")
    private val east = ExternalCircuit("c3", "Ost")
    private val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
    private val berg = ExternalCongregation("g2", "c1", "Bergheim")
    private val zell = ExternalCongregation("g3", "c2", "Altdorf") // same name in another circuit
    private val dorn = ExternalCongregation("g4", "c3", "Dornach")
    private val lea = VisitingSpeaker("s1", "g1", "Berg", "Lea")
    private val tim = VisitingSpeaker("s2", "g2", "Zorn", "Tim")
    private val eva = VisitingSpeaker("s3", "g3", "Alt", "Eva")
    private val ida = VisitingSpeaker("s4", "g4", "Dorn", "Ida")
    private val directory = ExternalDirectory(
        listOf(north, south, east),
        listOf(altdorf, berg, zell, dorn),
        listOf(lea, tim, eva, ida),
    )

    @Test
    fun `no filter shows everything`() {
        assertEquals(directory, directory.filtered(DirectoryFilter()))
        assertFalse(DirectoryFilter().isActive)
    }

    @Test
    fun `several circuits show everything that belongs to any of them`() {
        val shown = directory.filtered(DirectoryFilter(circuitIds = setOf("c1", "c3")))

        assertEquals(listOf(north, east), shown.circuits)
        assertEquals(listOf(altdorf, berg, dorn), shown.congregations)
        assertEquals(listOf(lea, tim, ida), shown.speakers)
    }

    @Test
    fun `several congregations without a circuit show their circuits`() {
        val shown = directory.filtered(DirectoryFilter(congregationIds = setOf("g3", "g4")))

        assertEquals(listOf(south, east), shown.circuits)
        assertEquals(listOf(zell, dorn), shown.congregations)
        assertEquals(listOf(eva, ida), shown.speakers)
    }

    @Test
    fun `circuits and congregations together narrow to those congregations in those circuits`() {
        val shown = directory.filtered(DirectoryFilter(setOf("c1"), setOf("g2")))

        assertEquals(listOf(north), shown.circuits)
        assertEquals(listOf(berg), shown.congregations)
        assertEquals(listOf(tim), shown.speakers)
    }

    @Test
    fun `IDs that no longer exist are ignored`() {
        assertEquals(directory, directory.filtered(DirectoryFilter(setOf("gone"), setOf("gone-too"))))
    }

    @Test
    fun `without a circuit all congregations are offered and named with their circuit`() {
        val options = directory.congregationOptions(DirectoryFilter(), "")

        assertEquals(listOf("g1", "g2", "g3", "g4"), options.map { it.id })
        assertEquals("Altdorf (Nord)", options[0].label)
        assertEquals("Altdorf (Süd)", options[2].label)
    }

    @Test
    fun `with circuits only their congregations are offered`() {
        val options = directory.congregationOptions(DirectoryFilter(circuitIds = setOf("c1", "c3")), "")

        assertEquals(listOf("g1", "g2", "g4"), options.map { it.id })
        assertEquals("Altdorf", options[0].label)
    }

    @Test
    fun `the name search starts with the second letter and ignores case`() {
        assertEquals(4, directory.congregationOptions(DirectoryFilter(), "a").size)
        assertEquals(listOf("g1", "g3"), directory.congregationOptions(DirectoryFilter(), "al").map { it.id })
        assertEquals(listOf("g2"), directory.congregationOptions(DirectoryFilter(), " BERG ").map { it.id })
        assertEquals(emptyList(), directory.congregationOptions(DirectoryFilter(), "xyz"))
    }

    @Test
    fun `chosen congregations stay in the list while searching`() {
        val options = directory.congregationOptions(DirectoryFilter(congregationIds = setOf("g4")), "al")

        assertEquals(listOf("g1", "g3", "g4"), options.map { it.id })
        assertEquals(listOf(false, false, true), options.map { it.selected })
    }

    @Test
    fun `circuit options mark the chosen ones`() {
        val options = directory.circuitOptions(DirectoryFilter(circuitIds = setOf("c2")))

        assertEquals(listOf("c1", "c2", "c3"), options.map { it.id })
        assertEquals(listOf(false, true, false), options.map { it.selected })
    }

    @Test
    fun `summary shows all or the single choice or the count`() {
        assertEquals("Alle Kreise", filterSummary(emptyList(), "Alle Kreise", "Kreise"))
        assertEquals("Nord", filterSummary(listOf("Nord"), "Alle Kreise", "Kreise"))
        assertEquals("2 Kreise gewählt", filterSummary(listOf("Nord", "Süd"), "Alle Kreise", "Kreise"))
    }

    @Test
    fun `toggling a circuit drops congregations of circuits that are not chosen`() {
        val filter = DirectoryFilter(congregationIds = setOf("g1", "g3"))

        val one = filter.toggleCircuit(directory, "c1")
        assertEquals(DirectoryFilter(setOf("c1"), setOf("g1")), one)

        val two = one.toggleCircuit(directory, "c2")
        assertEquals(DirectoryFilter(setOf("c1", "c2"), setOf("g1")), two)

        val onlySouth = two.toggleCircuit(directory, "c1") // g1 belongs to Nord, so it goes
        assertEquals(DirectoryFilter(setOf("c2"), emptySet()), onlySouth)

        val none = onlySouth.toggleCircuit(directory, "c2")
        assertFalse(none.isActive)

        val kept = DirectoryFilter(
            congregationIds = setOf("g1"),
        ).toggleCircuit(directory, "c1").toggleCircuit(directory, "c1")
        assertEquals(DirectoryFilter(emptySet(), setOf("g1")), kept)
        assertTrue(kept.isActive)
    }

    @Test
    fun `toggling a congregation checks and unchecks it`() {
        val on = DirectoryFilter().toggleCongregation("g2")
        assertEquals(setOf("g2"), on.congregationIds)
        assertEquals(emptySet(), on.toggleCongregation("g2").congregationIds)
    }
}
