package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SlotLinksTest {
    private val slot = TalkSlot(
        LocalDate(2026, 11, 22),
        LocalTime(17, 30),
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
    )

    @Test
    fun `a saved slot links to the talk history and the external speaker`() {
        val editor = TalkSlotEditorState(slot, slot)

        assertEquals(12, editor.historyTalkNumber(isSaving = false))
        assertEquals("e1", editor.directorySpeakerId(isSaving = false))
    }

    @Test
    fun `unsaved changes or a running save allow no jump`() {
        val dirty = TalkSlotEditorState(slot.copy(note = "neu"), slot)
        val saved = TalkSlotEditorState(slot, slot)

        assertNull(dirty.historyTalkNumber(isSaving = false))
        assertNull(dirty.directorySpeakerId(isSaving = false))
        assertNull(saved.historyTalkNumber(isSaving = true))
        assertNull(saved.directorySpeakerId(isSaving = true))
    }

    @Test
    fun `members and slots without talk number have no link`() {
        val member = slot.copy(
            externalSpeakerId = null,
            speakerCongregation = null,
            speakerMemberId = "m1",
            talkNumber = null,
        )
        val editor = TalkSlotEditorState(member, member)

        assertNull(editor.historyTalkNumber(isSaving = false))
        assertNull(editor.directorySpeakerId(isSaving = false))
    }

    @Test
    fun `the link hint is added below an existing supporting text`() {
        assertEquals("Hinweis\n$TALK_LINK_HINT", withLinkHint("Hinweis", TALK_LINK_HINT))
        assertEquals(SPEAKER_LINK_HINT, withLinkHint(null, SPEAKER_LINK_HINT))
        assertEquals("Hinweis", withLinkHint("Hinweis", null))
        assertNull(withLinkHint(null, null))
    }
}
