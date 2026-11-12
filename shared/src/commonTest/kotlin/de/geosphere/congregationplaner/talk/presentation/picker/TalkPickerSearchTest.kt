package de.geosphere.congregationplaner.talk.presentation.picker

import de.geosphere.congregationplaner.talk.domain.model.Talk
import kotlin.test.Test
import kotlin.test.assertEquals

class TalkPickerSearchTest {
    private val talks = listOf(
        Talk(12, "Der Weg zum Glück"),
        Talk(120, "Hoffnung für die Zukunft"),
        Talk(31, "Glück im Alltag"),
        Talk(40, "Pausierter Vortrag", active = false),
    )

    @Test
    fun `empty query lists every active talk`() {
        assertEquals(listOf(12, 120, 31), talks.matching("  ").map { it.number })
    }

    @Test
    fun `number matches the start and the title matches anywhere ignoring case`() {
        assertEquals(listOf(12, 120), talks.matching("12").map { it.number })
        assertEquals(listOf(12, 31), talks.matching("glück").map { it.number })
    }

    @Test
    fun `inactive talks are never offered`() {
        assertEquals(emptyList(), talks.matching("40"))
        assertEquals(emptyList(), talks.matching("pausiert"))
    }

    @Test
    fun `the query becomes a free entry when it is not blank and short enough`() {
        assertEquals("Sondervortrag", freeEntryOf("  Sondervortrag ", maxLength = 20))
        assertEquals(null, freeEntryOf("   ", maxLength = 20))
        assertEquals(null, freeEntryOf("x".repeat(21), maxLength = 20))
        assertEquals("x".repeat(20), freeEntryOf("x".repeat(20), maxLength = 20))
    }
}
