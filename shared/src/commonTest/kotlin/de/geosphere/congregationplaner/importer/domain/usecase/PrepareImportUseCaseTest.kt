package de.geosphere.congregationplaner.importer.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
import de.geosphere.congregationplaner.importer.data.UnavailableImportFileSource
import de.geosphere.congregationplaner.importer.domain.model.ImportError
import de.geosphere.congregationplaner.importer.domain.model.ImportReport
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrepareImportUseCaseTest {
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val weekEvents = FakeWeekEventRepository(listOf(WeekEvent("e9", "Kreiskongress", cancelsTalk = true)))
    private val catalog = FakeTalkCatalogRepository(listOf(Talk(12, "Zwölf")))
    private val schedule = FakeTalkScheduleRepository()
    private val directory = FakeExternalSpeakerDirectoryRepository()
    private val planning = TalkSchedulePermissionPolicy()
    private val reference = TalkReferenceDataPolicy()
    private val meta =
        CongregationMetadata(id = testCongregation.congregationId, weekendMeetingTime = LocalTime(10, 15))

    private fun prepare(congregations: List<CongregationMetadata> = listOf(meta)) = PrepareImportUseCase(
        weekEvents,
        catalog,
        ObserveExternalDirectoryUseCase(directory),
        schedule,
        FakeCongregationMetadataRepository(congregations),
        ImportReferenceDataUseCase(SaveWeekEventUseCase(weekEvents, reference), SaveTalkUseCase(catalog, reference)),
        ImportSpeakerDirectoryUseCase(
            SaveExternalCircuitUseCase(directory, planning),
            SaveExternalCongregationUseCase(directory, planning),
            SaveVisitingSpeakerUseCase(directory, planning),
        ),
        ImportTalkScheduleUseCase(SaveTalkSlotUseCase(schedule, planning)),
    )

    private val scheduleTable = ImportTable(
        emptyList(),
        listOf(
            ImportRow(2, mapOf("Datum" to "44927", "ID_Wochenereignis" to "5", "Ereignis" to "kreiskongress")),
            ImportRow(3, mapOf("Datum" to "44934")),
        ),
    )

    @Test
    fun `schedule uses the meeting time of the year and maps the event by name`() = runBlocking {
        val prepared = prepare()(planner, ImportSource.SCHEDULE, scheduleTable).getOrThrow()
        assertEquals(2, prepared.summaries.first().create)

        val report = prepared.run()

        assertEquals(2, report.created)
        val first = schedule.savedSlots.first()
        assertEquals(LocalDate(2023, 1, 1), first.date)
        assertEquals(LocalTime(10, 15), first.time)
        assertEquals(TalkSlot(first.date, first.time, "e9", "Kreiskongress", talkCancelled = true), first)
    }

    @Test
    fun `schedule fails without the meeting data of the congregation`() = runBlocking {
        val result = prepare(emptyList())(planner, ImportSource.SCHEDULE, scheduleTable)
        assertTrue(result.isFailure)
    }

    @Test
    fun `talk catalog preview counts creates and skips`() = runBlocking {
        val table = ImportTable(
            emptyList(),
            listOf(
                ImportRow(2, mapOf("Nummer" to "12", "Titel" to "Zwölf", "Aktiv" to "1")),
                ImportRow(3, mapOf("Nummer" to "13", "Titel" to "Dreizehn", "Aktiv" to "0")),
            ),
        )
        val prepared = prepare()(planner, ImportSource.TALK_CATALOG, table).getOrThrow()
        val summary = prepared.summaries.single()
        assertEquals(1, summary.create)
        assertEquals(1, summary.skip)
        assertEquals("Vorträge: 1 anlegen, 0 ändern, 1 unverändert", summary.description)
    }

    private val admin = AppUser(uid = "a", approved = true, isGlobalAdmin = true)

    @Test
    fun `week events preview reuses an existing event and writes new ones`() = runBlocking {
        val table = ImportTable(
            emptyList(),
            listOf(
                ImportRow(2, mapOf("ID_Wochenereignis" to "5", "Ereignis" to "Kreiskongress")),
                ImportRow(3, mapOf("ID_Wochenereignis" to "6", "Ereignis" to "Dienstwoche")),
            ),
        )
        val prepared = prepare()(admin, ImportSource.WEEK_EVENTS, table).getOrThrow()
        assertEquals(PlanSummary("Wochenereignisse", 1, 0, 1), prepared.summaries.single())

        prepared.run()

        assertEquals(listOf("Dienstwoche"), weekEvents.savedEvents.map { it.name })
    }

    @Test
    fun `speaker list preview counts circuits congregations and speakers and writes them`() = runBlocking {
        val row = mapOf(
            "ID_Kreis" to "7",
            "tblKreis_Name" to "Nord",
            "ID_Versammlung" to "20",
            "tblVersammlung_Name" to "Musterstadt",
            "ID_Redner" to "5",
            "Nachname" to "Berg",
            "Nummer" to "12",
        )
        val prepared = prepare()(planner, ImportSource.SPEAKERS, ImportTable(emptyList(), listOf(ImportRow(2, row))))
            .getOrThrow()
        assertEquals(listOf("Kreise", "Versammlungen", "Redner"), prepared.summaries.map { it.label })

        val report = prepared.run()

        assertEquals(3, report.created)
        assertEquals(listOf("5"), directory.speakers.value.map { it.id })
    }

    @Test
    fun `an unavailable file source and the plan texts describe themselves`() = runBlocking {
        assertTrue(!UnavailableImportFileSource.isAvailable)
        assertTrue(UnavailableImportFileSource.pickTable("x").isFailure)
        assertEquals("Zeile 4: kaputt", ImportError(4, "kaputt").description)
        assertEquals("Hinweis", ImportError(0, "Hinweis").description)
        assertEquals(
            "1 angelegt, 2 geändert, 3 unverändert, 1 Fehler",
            ImportReport(1, 2, 3, listOf("x")).description,
        )
    }
}
