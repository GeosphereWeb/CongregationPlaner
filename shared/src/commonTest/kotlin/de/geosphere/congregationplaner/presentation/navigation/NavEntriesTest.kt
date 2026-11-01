package de.geosphere.congregationplaner.presentation.navigation

import de.geosphere.congregationplaner.ui.chip.StatusTone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavEntriesTest {
    private val all = CongregationSection.entries

    @Test
    fun `phone shows the talk planning as one entry`() {
        val entries = congregationEntries(all, CongregationSection.EXTERNAL_SPEAKERS, isWide = false) {}

        assertEquals(
            listOf("Allgemeine Versammlungsdaten", "Userverwaltung", "Planung unter der Woche", "Vortragsplanung"),
            entries.map { it.label },
        )
        val talk = entries.last()
        assertTrue(talk.selected)
        assertEquals(StatusTone.Mint, talk.tone)
        assertEquals("Vortragsplaner · Wochenende, Redner, Historie", talk.supportingText)
    }

    @Test
    fun `phone entry for the talk planning opens its first part`() {
        var opened: CongregationSection? = null
        congregationEntries(all, null, isWide = false) { opened = it }.last().onClick()

        assertEquals(CongregationSection.WEEKEND_TALKS, opened)
    }

    @Test
    fun `wide window shows a heading with the three parts nested below`() {
        val entries = congregationEntries(all, CongregationSection.TALK_HISTORY, isWide = true) {}

        val heading = entries.single { it.heading }
        assertEquals("Vortragsplanung", heading.label)
        val parts = entries.dropWhile { !it.heading }.drop(1)
        assertEquals(CongregationSection.talkPlanning.map { it.label }, parts.map { it.label })
        assertTrue(parts.all { it.nested })
        assertEquals(listOf(false, false, true), parts.map { it.selected })
        assertFalse(heading.selected)
    }

    @Test
    fun `only the visible parts of the talk planning appear`() {
        val sections = listOf(CongregationSection.EXTERNAL_SPEAKERS)

        assertEquals(listOf("Vortragsplanung"), congregationEntries(sections, null, isWide = false) {}.map { it.label })
        assertEquals(
            listOf("Vortragsplanung", "Externe Redner"),
            congregationEntries(sections, null, isWide = true) {}.map { it.label },
        )
    }

    @Test
    fun `without a talk planner there is no talk entry`() {
        val sections = listOf(CongregationSection.GENERAL_DATA, CongregationSection.USER_MANAGEMENT)

        assertEquals(2, congregationEntries(sections, null, isWide = true) {}.size)
    }

    @Test
    fun `supporting texts and tones follow the design`() {
        val entries = congregationEntries(all, null, isWide = false) {}.associateBy { it.label }

        assertEquals("Name, Zusammenkunftszeiten", entries.getValue("Allgemeine Versammlungsdaten").supportingText)
        assertEquals("Mitglieder, Rollen, Freigaben", entries.getValue("Userverwaltung").supportingText)
        assertEquals(StatusTone.Sky, entries.getValue("Planung unter der Woche").tone)
        assertEquals("Aufgabenplaner", entries.getValue("Planung unter der Woche").supportingText)
        assertTrue(entries.getValue("Userverwaltung").nested)
        assertEquals(StatusTone.Gold, CongregationGroup.ADMINISTRATION.tone)
        assertNull(CongregationGroup.PLANNING.tone)
    }

    @Test
    fun `global entries mark the selection`() {
        val entries = globalEntries(GlobalSection.entries, GlobalSection.CONGREGATIONS) {}

        assertEquals(listOf(false, true, false), entries.map { it.selected })
    }

    @Test
    fun `open approvals show as a badge on the user management`() {
        val entries = congregationEntries(all, null, isWide = false, pendingApprovals = 3) {}.associateBy { it.label }

        assertEquals("3 offen", entries.getValue("Userverwaltung").badge)
        assertNull(entries.getValue("Allgemeine Versammlungsdaten").badge)
    }

    @Test
    fun `without open approvals there is no badge`() {
        val entries = congregationEntries(all, null, isWide = false, pendingApprovals = 0) {}

        assertTrue(entries.all { it.badge == null })
    }
}
