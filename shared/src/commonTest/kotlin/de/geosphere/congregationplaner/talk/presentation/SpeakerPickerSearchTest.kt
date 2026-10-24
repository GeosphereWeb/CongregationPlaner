package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.Speaker
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerPickerSearchTest {
    private val speakers = listOf(
        Speaker("1", "Muster", "Max"),
        Speaker("2", "Beispiel", "Tim"),
        Speaker("3", "Mustermann", "Tina"),
    )

    @Test
    fun `empty query lists every speaker`() {
        assertEquals(listOf("1", "2", "3"), speakers.matching("  ").map { it.memberId })
    }

    @Test
    fun `query matches last or first name anywhere ignoring case`() {
        assertEquals(listOf("1", "3"), speakers.matching("MUSTER").map { it.memberId })
        assertEquals(listOf("2", "3"), speakers.matching("ti").map { it.memberId })
        assertEquals(emptyList(), speakers.matching("xyz"))
    }
}
