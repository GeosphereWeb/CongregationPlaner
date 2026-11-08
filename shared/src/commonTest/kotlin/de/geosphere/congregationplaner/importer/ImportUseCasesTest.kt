package de.geosphere.congregationplaner.importer

import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCircuitUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveExternalCongregationUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkHistoryEntryUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkSlotUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveWeekEventUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkHistoryRepository
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

class ImportUseCasesTest {
    private val planner = AppUser(
        uid = "1",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val globalAdmin = AppUser(uid = "2", approved = true, isGlobalAdmin = true)
    private val member = AppUser(uid = "3", approved = true, congregation = testCongregation)

    private fun <T> plan(vararg entries: Pair<T, ImportKind>) =
        ImportPlan(entries.map { PlanEntry(it.first, it.second) }, emptyList())

    @Test
    fun referenceDataIsWrittenByGlobalAdminAndSkipsAreCounted() = runBlocking {
        val events = FakeWeekEventRepository()
        val talks = FakeTalkCatalogRepository()
        val useCase = ImportReferenceDataUseCase(
            SaveWeekEventUseCase(events, TalkReferenceDataPolicy()),
            SaveTalkUseCase(talks, TalkReferenceDataPolicy()),
        )

        val report = useCase(
            globalAdmin,
            plan(WeekEvent("2", "Kongress", true) to ImportKind.CREATE),
            plan(
                Talk(1, "Eins") to ImportKind.CREATE,
                Talk(2, "Zwei") to ImportKind.SKIP,
                Talk(0, "x") to ImportKind.UPDATE,
            ),
        )

        assertEquals(listOf(WeekEvent("2", "Kongress", true)), events.savedEvents)
        assertEquals(listOf(Talk(1, "Eins")), talks.savedTalks)
        assertEquals(2, report.created)
        assertEquals(1, report.skipped)
        assertEquals(1, report.failures.size) // number 0 is rejected by the save use case, the run goes on
    }

    @Test
    fun withoutPermissionTheStepStopsAfterOneFailureAndWritesNothing() = runBlocking {
        val talks = FakeTalkCatalogRepository()
        val useCase = ImportReferenceDataUseCase(
            SaveWeekEventUseCase(FakeWeekEventRepository(), TalkReferenceDataPolicy()),
            SaveTalkUseCase(talks, TalkReferenceDataPolicy()),
        )

        val report = useCase(
            planner,
            plan(WeekEvent("2", "Kongress") to ImportKind.CREATE),
            plan(Talk(1, "Eins") to ImportKind.CREATE, Talk(2, "Zwei") to ImportKind.CREATE),
        )

        assertTrue(talks.savedTalks.isEmpty())
        assertEquals(0, report.created)
        assertEquals(2, report.failures.size) // one for the events, one for the talks, not one per entry
    }

    @Test
    fun speakerDirectoryIsWrittenInDependencyOrderByPlanner() = runBlocking {
        val repository = FakeExternalSpeakerDirectoryRepository()
        val policy = TalkSchedulePermissionPolicy()
        val useCase = ImportSpeakerDirectoryUseCase(
            SaveExternalCircuitUseCase(repository, policy),
            SaveExternalCongregationUseCase(repository, policy),
            SaveVisitingSpeakerUseCase(repository, policy),
        )

        val report = useCase(
            planner,
            SpeakerDirectoryImport(
                circuits = plan(ExternalCircuit("7", "Nord") to ImportKind.CREATE),
                congregations = plan(ExternalCongregation("20", "7", "Musterstadt") to ImportKind.CREATE),
                speakers = plan(VisitingSpeaker("5", "20", "Berg") to ImportKind.CREATE),
            ),
        )

        assertEquals(3, report.created)
        assertEquals(emptyList(), report.failures)
        assertEquals(listOf("5"), repository.speakers.value.map { it.id })
    }

    @Test
    fun scheduleWritesHistoryOnlyForSlotsThatWereSaved() = runBlocking {
        val directory = FakeExternalSpeakerDirectoryRepository(
            circuits = listOf(ExternalCircuit("7", "Nord")),
            congregations = listOf(ExternalCongregation("20", "7", "Musterstadt")),
        )
        val schedule = FakeTalkScheduleRepository()
        val history = FakeTalkHistoryRepository()
        val policy = TalkSchedulePermissionPolicy()
        val useCase = ImportTalkScheduleUseCase(
            SaveVisitingSpeakerUseCase(directory, policy),
            SaveTalkSlotUseCase(schedule, policy),
            SaveTalkHistoryEntryUseCase(history, policy),
        )
        val good = TalkSlot(LocalDate(2023, 1, 1), LocalTime(9, 30), talkNumber = 12, talkTitle = "Zwölf")
        val bad = TalkSlot(LocalDate(2023, 1, 8), LocalTime(9, 30), talkNumber = 45, talkTitle = "") // invalid

        val report = useCase(
            planner,
            TalkScheduleImport(
                slots = plan(good to ImportKind.CREATE, bad to ImportKind.CREATE),
                newSpeakers = listOf(VisitingSpeaker("99", "20", "Neu")),
                warnings = emptyList(),
            ),
            existingHistory = emptyList(),
        )

        assertEquals(listOf(good), schedule.savedSlots)
        assertEquals(listOf(12), history.savedEntries.map { it.talkNumber })
        assertEquals(3, report.created) // speaker, slot, history
        assertEquals(1, report.failures.size)
    }

    @Test
    fun scheduleWithoutPlannerRoleWritesNothing() = runBlocking {
        val schedule = FakeTalkScheduleRepository()
        val policy = TalkSchedulePermissionPolicy()
        val useCase = ImportTalkScheduleUseCase(
            SaveVisitingSpeakerUseCase(FakeExternalSpeakerDirectoryRepository(), policy),
            SaveTalkSlotUseCase(schedule, policy),
            SaveTalkHistoryEntryUseCase(FakeTalkHistoryRepository(), policy),
        )
        val slot = TalkSlot(LocalDate(2023, 1, 1), LocalTime(9, 30))

        val report =
            useCase(member, TalkScheduleImport(plan(slot to ImportKind.CREATE), emptyList(), emptyList()), emptyList())

        assertTrue(schedule.savedSlots.isEmpty())
        assertEquals(0, report.created)
        assertEquals(1, report.failures.size)
    }
}
