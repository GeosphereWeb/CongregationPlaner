package de.geosphere.congregationplaner.talk.presentation.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SlotEntryTextsTest {
    @Test
    fun `the speaker hint names the missing permission first`() {
        val hint = speakerSupporting(canViewMembers = false, noSpeakers = true, speakerMissing = true)

        assertTrue(hint.orEmpty().startsWith("Ohne die Berechtigung"))
    }

    @Test
    fun `the speaker hint says when nobody is marked as speaker or where one comes from`() {
        assertEquals(
            "Noch kein Mitglied ist als Redner markiert (Userverwaltung).",
            speakerSupporting(canViewMembers = true, noSpeakers = true, speakerMissing = true),
        )
        assertEquals(
            "Aus der eigenen Versammlung oder aus „Externe Redner“",
            speakerSupporting(canViewMembers = true, noSpeakers = false, speakerMissing = true),
        )
        assertNull(speakerSupporting(canViewMembers = true, noSpeakers = false, speakerMissing = false))
    }

    @Test
    fun `the chairman hint follows permission and the brothers available`() {
        assertTrue(
            chairmanSupporting(false, noChairmen = true, noneSelectable = true).orEmpty().startsWith("Dir fehlt"),
        )
        assertTrue(
            chairmanSupporting(true, noChairmen = true, noneSelectable = true).orEmpty().startsWith("Kein Bruder"),
        )
        assertEquals(
            "Alle Brüder mit Dienstamt sind schon als Redner eingeteilt.",
            chairmanSupporting(true, noChairmen = false, noneSelectable = true),
        )
        assertNull(chairmanSupporting(true, noChairmen = false, noneSelectable = false))
    }
}
