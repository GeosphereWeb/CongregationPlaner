package de.geosphere.congregationplaner.importer.domain.planning

import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.model.PlanEntry
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkScheduleImportTest {
    private val sunday = LocalDate(2023, 1, 1) // serial 44927
    private val time = LocalTime(9, 30)
    private val berg = VisitingSpeaker("5", "20", "Berg", "Lea", talkNumbers = listOf(12))
    private val congregation = ExternalCongregation("20", "7", "Musterstadt")
    private val directory = ExternalDirectory(listOf(ExternalCircuit("7", "Nord")), listOf(congregation), listOf(berg))
    private val kongress = WeekEvent("e2", "Kreiskongress", cancelsTalk = true)

    private fun context(existing: List<TalkSlot> = emptyList()) = ScheduleImportContext(
        existingSlots = existing,
        catalog = listOf(Talk(12, "Zwölf"), Talk(45, "Fünfundvierzig")),
        weekEvents = listOf(kongress),
        weekEventIdBySourceId = mapOf("2" to "e2"),
        directory = directory,
        timeFor = { time },
    )

    private fun row(number: Int, vararg cells: Pair<String, String>) = ImportRow(number, cells.toMap())

    private fun table(vararg rows: ImportRow) = ImportTable(emptyList(), rows.toList())

    private fun full(
        number: Int = 2,
        serial: String = "44927",
        speaker: String = "5",
        talk: String = "12",
        event: String = "",
    ) = row(
        number,
        "Datum" to serial,
        "ID_Wochenereignis" to event,
        "ID_Redner" to speaker,
        "Nachname" to "Berg",
        "Vorname" to "Lea",
        "ID_Versammlung" to "20",
        "Name" to "Musterstadt",
        "Nummer" to talk,
        "rednerIstInformiert" to "1",
    )

    @Test
    fun rowWithKnownSpeakerAndTalkBecomesAConfirmedSlot() {
        val slot = planTalkSchedule(table(full()), context()).slots.entries.single()
        assertEquals(ImportKind.CREATE, slot.kind)
        assertEquals(
            TalkSlot(
                date = sunday,
                time = time,
                talkNumber = 12,
                talkTitle = "Zwölf",
                externalSpeakerId = "5",
                speakerLastName = "Berg",
                speakerFirstName = "Lea",
                speakerCongregation = "Musterstadt",
                confirmed = true,
            ),
            slot.item,
        )
    }

    @Test
    fun eventSetsNameAndCancelFlagAndRowWithoutSpeakerIsAnEmptySlot() {
        val empty = row(2, "Datum" to "44927", "ID_Wochenereignis" to "2", "Ereignis" to "Kreiskongress")
        val slot = planTalkSchedule(table(empty), context()).slots.entries.single().item
        assertEquals(TalkSlot(sunday, time, "e2", "Kreiskongress", talkCancelled = true), slot)
    }

    @Test
    fun speakerOnlyInTheScheduleIsCreatedFromTheRowAndWarnsAboutSameName() {
        val result = planTalkSchedule(table(full(speaker = "99", talk = "45")), context())
        assertEquals(
            VisitingSpeaker("99", "20", "Berg", "Lea", talkNumbers = listOf(45)),
            result.newSpeakers.single(),
        )
        assertEquals("99", result.slots.entries.single().item.externalSpeakerId)
        assertEquals(listOf(2), result.warnings.map { it.row })
    }

    @Test
    fun invalidRowsAreReportedAndDoNotStopTheImport() {
        val unknownTalk = row(5, "Datum" to "44934", "Nummer" to "999")
        val unknownSpeaker =
            row(6, "Datum" to "44941", "ID_Redner" to "77", "Nachname" to "X", "ID_Versammlung" to "404")
        val result = planTalkSchedule(
            table(row(2, "Datum" to "abc"), full(3), row(4, "Datum" to "44927"), unknownTalk, unknownSpeaker),
            context(),
        )
        assertEquals(listOf(2, 4, 5, 6), result.slots.errors.map { it.row })
        assertEquals(
            listOf(sunday, LocalDate(2023, 1, 8), LocalDate(2023, 1, 15)),
            result.slots.entries.map { it.item.date },
        )
    }

    @Test
    fun onlyEmptyExistingSlotsAreFilledAndKeepTheirTime() {
        val emptySlot = TalkSlot(sunday, LocalTime(11, 0), note = "n")
        val planned = TalkSlot(LocalDate(2023, 1, 8), time, talkNumber = 45, talkTitle = "Fünfundvierzig")
        val rows = table(full(), full(serial = "44934"))
        val entries = planTalkSchedule(rows, context(listOf(emptySlot, planned))).slots.entries
        assertEquals(ImportKind.UPDATE, entries[0].kind)
        assertEquals(LocalTime(11, 0), entries[0].item.time)
        assertEquals("n", entries[0].item.note)
        assertEquals(12, entries[0].item.talkNumber)
        assertEquals(PlanEntry(planned, ImportKind.SKIP), entries[1])
    }

    @Test
    fun secondRunChangesNothing() {
        val first = planTalkSchedule(table(full()), context()).slots.entries.map { it.item }
        val second = planTalkSchedule(table(full()), context(first)).slots
        assertEquals(listOf(ImportKind.SKIP), second.entries.map { it.kind })
    }

    @Test
    fun historyCollectsHeldDaysWithSpeakerAndIgnoresCancelledAndSkippedSlots() {
        val slots = planTalkSchedule(
            table(full(), full(serial = "44934", event = "2"), full(serial = "44941")),
            context(),
        ).slots
        val history = planTalkHistory(slots, listOf(TalkHistoryEntry(12, listOf(LocalDate(2020, 1, 5)))))
        val entry = history.entries.single()
        assertEquals(ImportKind.UPDATE, entry.kind)
        assertEquals(listOf(LocalDate(2020, 1, 5), sunday, LocalDate(2023, 1, 15)), entry.item.heldOn)
        assertEquals(listOf(sunday, LocalDate(2023, 1, 15)), entry.item.plannedOn)
        assertEquals("Berg, Lea (Musterstadt)", entry.item.speakers[sunday])
        assertNull(entry.item.speakers[LocalDate(2023, 1, 8)])
        val skipped = slots.copy(entries = slots.entries.map { it.copy(kind = ImportKind.SKIP) })
        assertTrue(planTalkHistory(skipped, emptyList()).entries.isEmpty())
    }

    @Test
    fun skippedSlotStillGetsItsMissingScheduleOnlySpeakerOnRepeat() {
        val first = planTalkSchedule(table(full(speaker = "99", talk = "45")), context())
        val saved = first.slots.entries.map { it.item }

        val again = planTalkSchedule(table(full(speaker = "99", talk = "45")), context(saved))

        assertEquals(listOf(ImportKind.SKIP), again.slots.entries.map { it.kind })
        assertEquals(listOf("99"), again.newSpeakers.map { it.id })
    }

    @Test
    fun skippedSlotDoesNotRecreateASpeakerThatAlreadyExists() {
        val saved = planTalkSchedule(table(full()), context()).slots.entries.map { it.item }

        val again = planTalkSchedule(table(full()), context(saved))

        assertEquals(emptyList(), again.newSpeakers)
    }
}
