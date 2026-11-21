package de.geosphere.congregationplaner.importer.domain.export

import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveExternalDirectoryUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExportTableUseCaseTest {
    private val planner = AppUser(
        uid = "p",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val helper = planner.copy(roles = setOf(CongregationRole.TALK_HELPER))
    private val schedule = FakeTalkScheduleRepository(listOf(TalkSlot(LocalDate(2026, 3, 8), LocalTime(10, 0))))
    private val catalog = FakeTalkCatalogRepository(listOf(Talk(12, "Zwölf")))

    private val export = ExportTableUseCase(
        TalkSchedulePermissionPolicy(),
        schedule,
        ObserveExternalDirectoryUseCase(FakeExternalSpeakerDirectoryRepository()),
        catalog,
    )

    @Test
    fun `only talk planners may export`() = runTest {
        assertIs<TalkScheduleException.NotAllowed>(export(helper, ExportKind.TALK_CATALOG, 2026).exceptionOrNull())
    }

    @Test
    fun `schedule export reads the year and marks the date column`() = runTest {
        val file = export(planner, ExportKind.SCHEDULE, 2026).getOrThrow()

        assertEquals("Export_Vortragsplanung_2026.xlsx", file.fileName)
        assertEquals(2026, schedule.requestedYear)
        assertEquals(setOf("Datum"), file.dateHeaders)
        assertEquals(1, file.table.rows.size)
    }

    @Test
    fun `talk list export reads the list of the own country`() = runTest {
        val file = export(planner, ExportKind.TALK_CATALOG, 2026).getOrThrow()

        assertEquals(listOf(testCongregation.country), catalog.countries)
        assertEquals("Zwölf", file.table.rows.single().text("Titel"))
    }
}
