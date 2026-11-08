package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeakerDirectoryImportTest {
    private fun line(speaker: String, talk: String, ranking: String = "3", last: String = "Berg") = mapOf(
        "ID_Kreis" to "7.0",
        "tblKreis_Name" to "Nord",
        "ID_Versammlung" to "20",
        "tblVersammlung_Name" to "Musterstadt",
        "ID_Redner" to speaker,
        "Nachname" to last,
        "Vorname" to "Lea",
        "Telefon" to "0170",
        "Telefon2" to "08092",
        "E-Mail" to "lea@example.org",
        "Ranking" to ranking,
        "Nummer" to talk,
    )

    private fun table(vararg rows: Map<String, String>) =
        ImportTable(emptyList(), rows.mapIndexed { i, cells -> ImportRow(i + 2, cells) })

    @Test
    fun rowsAreGroupedBySpeakerWithSortedTalkNumbers() {
        val result = planSpeakerDirectory(
            table(line("5", "45"), line("5", "12"), line("6", "12", last = "Tal")),
            ExternalDirectory(),
            setOf(12, 45),
        )
        assertEquals(
            listOf(
                VisitingSpeaker("5", "20", "Berg", "Lea", "0170", "08092", "lea@example.org", listOf(12, 45), 3),
                VisitingSpeaker("6", "20", "Tal", "Lea", "0170", "08092", "lea@example.org", listOf(12), 3),
            ),
            result.speakers.entries.map { it.item },
        )
        assertEquals(listOf(ExternalCircuit("7", "Nord")), result.circuits.entries.map { it.item })
        assertEquals("7", result.congregations.entries.single().item.circuitId)
    }

    @Test
    fun unknownTalkNumberIsAnErrorButTheSpeakerIsKept() {
        val result = planSpeakerDirectory(table(line("5", "12"), line("5", "999")), ExternalDirectory(), setOf(12))
        assertEquals(listOf(3), result.speakers.errors.map { it.row })
        assertEquals(listOf(12), result.speakers.entries.single().item.talkNumbers)
    }

    @Test
    fun secondRunSkipsUnchangedEntriesAndUpdatesChangedOnes() {
        val first = planSpeakerDirectory(table(line("5", "12")), ExternalDirectory(), setOf(12))
        val existing = ExternalDirectory(
            circuits = first.circuits.entries.map { it.item },
            congregations = first.congregations.entries.map { it.item },
            speakers = first.speakers.entries.map { it.item.copy(ranking = 1) },
        )
        val second = planSpeakerDirectory(table(line("5", "12")), existing, setOf(12))
        assertEquals(ImportKind.SKIP, second.circuits.entries.single().kind)
        assertEquals(ImportKind.SKIP, second.congregations.entries.single().kind)
        assertEquals(ImportKind.UPDATE, second.speakers.entries.single().kind)
    }

    @Test
    fun invalidRowsAreReported() {
        val broken = line("9", "12") - "ID_Versammlung"
        val result = planSpeakerDirectory(
            table(broken, line("8", "12", ranking = "9"), line("7", "12", last = ""), line("6", "12")),
            ExternalDirectory(),
            setOf(12),
        )
        assertEquals(listOf(2, 3, 4), result.speakers.errors.map { it.row })
        assertEquals(listOf("6"), result.speakers.entries.map { it.item.id })
    }

    @Test
    fun congregationWithoutSpeakersIsStillPlanned() {
        val result = planSpeakerDirectory(table(line("", "")), ExternalDirectory(), emptySet())
        assertEquals(1, result.congregations.entries.size)
        assertEquals(emptyList(), result.speakers.entries)
    }
}
