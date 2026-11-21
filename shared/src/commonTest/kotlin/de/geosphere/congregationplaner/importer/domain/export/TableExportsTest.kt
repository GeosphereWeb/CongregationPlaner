package de.geosphere.congregationplaner.importer.domain.export

import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.importer.domain.model.ImportKind
import de.geosphere.congregationplaner.importer.domain.model.ImportPlan
import de.geosphere.congregationplaner.importer.domain.planning.ScheduleImportContext
import de.geosphere.congregationplaner.importer.domain.planning.dateToExcelSerial
import de.geosphere.congregationplaner.importer.domain.planning.eventTypeIdsBySource
import de.geosphere.congregationplaner.importer.domain.planning.excelSerialToDate
import de.geosphere.congregationplaner.importer.domain.planning.planSpeakerDirectory
import de.geosphere.congregationplaner.importer.domain.planning.planTalkCatalog
import de.geosphere.congregationplaner.importer.domain.planning.planTalkSchedule
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** An exported file goes back into the import unchanged: every entry is planned as "skip", without errors. */
class TableExportsTest {
    private val time = LocalTime(10, 0)
    private val kongress = EventType("k1", "Kreiskongress", cancelsTalk = true)
    private val talks = listOf(
        Talk(12, "Zwölf"),
        Talk(13, "Dreizehn", active = false, titleChanges = listOf(TalkTitleChange(LocalDate(2026, 1, 1), "Neu"))),
    )
    private val directory = ExternalDirectory(
        circuits = listOf(ExternalCircuit("c1", "Kreis Süd"), ExternalCircuit("c2", "Leerer Kreis")),
        congregations = listOf(
            ExternalCongregation("v1", "c1", "Musterstadt"),
            ExternalCongregation("v2", "c1", "Leer"),
        ),
        speakers = listOf(
            VisitingSpeaker(
                id = "r1",
                congregationId = "v1",
                lastName = "Berg",
                firstName = "Lea",
                mobilePhone = "0170 123",
                email = "lea@example.org",
                talkNumbers = listOf(12, 13),
                ranking = 3,
                excludedFromPlanning = true,
            ),
            VisitingSpeaker(id = "r2", congregationId = "v1", lastName = "Ohne Vortrag"),
        ),
    )

    private fun <T> assertAllSkipped(plan: ImportPlan<T>) {
        assertTrue(plan.errors.isEmpty(), "errors: ${plan.errors}")
        assertEquals(List(plan.entries.size) { ImportKind.SKIP }, plan.entries.map { it.kind })
    }

    @Test
    fun `schedule export goes back into the import unchanged`() {
        val slots = listOf(
            TalkSlot(
                LocalDate(2026, 3, 8),
                time,
                talkNumber = 12,
                talkTitle = "Zwölf",
                externalSpeakerId = "r1",
                speakerLastName = "Berg",
                speakerFirstName = "Lea",
                speakerCongregation = "Musterstadt",
                confirmed = true,
            ),
            TalkSlot(LocalDate(2026, 3, 1), time, kongress.id, kongress.name, talkCancelled = true),
            TalkSlot(LocalDate(2026, 3, 15), time, talkTitle = "Frei eingetragen", speakerLastName = "Gast"),
        )
        val table = exportTalkSchedule(slots)
        val context = ScheduleImportContext(
            existingSlots = slots,
            catalog = talks,
            eventTypes = listOf(kongress),
            eventTypeIdBySourceId = eventTypeIdsBySource(table, listOf(kongress)),
            directory = directory,
            timeFor = { time },
        )

        val import = planTalkSchedule(table, context)

        assertAllSkipped(import.slots)
        val dates = listOf(LocalDate(2026, 3, 1), LocalDate(2026, 3, 8), LocalDate(2026, 3, 15))
        assertEquals(dates, import.slots.entries.map { it.item.date })
        assertEquals("1", table.rows[1].text("rednerIstInformiert"))
    }

    @Test
    fun `speaker list export goes back into the import unchanged and keeps the exclusion`() {
        val table = exportSpeakerDirectory(directory)

        val import = planSpeakerDirectory(table, directory, knownTalkNumbers = setOf(12, 13))

        assertAllSkipped(import.circuits)
        assertAllSkipped(import.congregations)
        assertAllSkipped(import.speakers)
        // Two talks of Berg, one row without talk, one congregation without speakers; the empty circuit is left out.
        assertEquals(4, table.rows.size)
        assertEquals(listOf("c1"), import.circuits.entries.map { it.item.id })
    }

    @Test
    fun `talk list export goes back into the import unchanged`() {
        val table = exportTalkCatalog(talks.reversed())

        assertAllSkipped(planTalkCatalog(table, talks))
        assertEquals(listOf("12", "13"), table.rows.map { it.text("Nummer") })
        assertEquals("0", table.rows[1].text("Aktiv"))
    }

    @Test
    fun `excel serial dates convert both ways`() {
        assertEquals(44927, dateToExcelSerial(LocalDate(2023, 1, 1)))
        val date = LocalDate(2026, 11, 22)
        assertEquals(date, excelSerialToDate(dateToExcelSerial(date).toDouble()))
    }
}
