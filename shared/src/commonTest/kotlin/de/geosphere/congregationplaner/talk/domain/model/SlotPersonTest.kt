package de.geosphere.congregationplaner.talk.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class SlotPersonTest {
    private val day = TalkSlot(LocalDate(2026, 11, 15), LocalTime(10, 0), talkNumber = 12, talkTitle = "T")
    private val newMember = MemberRef("m9", "Berg", "Lea")

    @Test
    fun `an external speaker becomes the member`() {
        val slot = day.copy(
            externalSpeakerId = "e1",
            speakerLastName = "Berg",
            speakerFirstName = "L.",
            speakerCongregation = "Nord",
        )

        assertEquals(
            day.copy(speakerMemberId = "m9", speakerLastName = "Berg", speakerFirstName = "Lea"),
            slot.withPersonReplaced(SlotPerson.External("e1"), newMember),
        )
    }

    @Test
    fun `a member is replaced as speaker and as chairman`() {
        val slot = day.copy(
            speakerMemberId = "p1",
            speakerLastName = "Alt",
            speakerFirstName = "A",
            chairmanMemberId = "p1",
            chairmanLastName = "Alt",
            chairmanFirstName = "A",
        )

        val replaced = slot.withPersonReplaced(SlotPerson.Member("p1"), newMember)

        assertEquals("m9", replaced.speakerMemberId)
        assertEquals("m9", replaced.chairmanMemberId)
        assertEquals("Lea", replaced.chairmanFirstName)
    }

    @Test
    fun `slots that do not name the person stay unchanged`() {
        val other = day.copy(
            externalSpeakerId = "e2",
            speakerLastName = "X",
            speakerFirstName = "",
            speakerCongregation = "S",
        )

        assertEquals(other, other.withPersonReplaced(SlotPerson.External("e1"), newMember))
        // An external ID never matches a member with the same value, and vice versa.
        assertEquals(other, other.withPersonReplaced(SlotPerson.Member("e2"), newMember))
    }
}
