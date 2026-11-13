package de.geosphere.congregationplaner.importer.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
import de.geosphere.congregationplaner.importer.data.ImportFileSource
import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import de.geosphere.congregationplaner.importer.domain.usecase.ImportReferenceDataUseCase
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSource
import de.geosphere.congregationplaner.importer.domain.usecase.ImportSpeakerDirectoryUseCase
import de.geosphere.congregationplaner.importer.domain.usecase.ImportTalkScheduleUseCase
import de.geosphere.congregationplaner.importer.domain.usecase.PrepareImportUseCase
import de.geosphere.congregationplaner.talk.domain.model.Talk
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {
    private val globalAdmin = AppUser(uid = "admin", approved = true, isGlobalAdmin = true)
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "member", approved = true, congregation = testCongregation)

    private class FakeFileSource(var result: Result<ImportTable?>, override val isAvailable: Boolean = true) :
        ImportFileSource {
        override suspend fun pickTable(title: String) = result
    }

    private val catalog = FakeTalkCatalogRepository()
    private val schedule = FakeTalkScheduleRepository()
    private val directory = FakeExternalSpeakerDirectoryRepository()
    private val reference = TalkReferenceDataPolicy()
    private val planning = TalkSchedulePermissionPolicy()
    private val talkTable = ImportTable(
        emptyList(),
        listOf(ImportRow(2, mapOf("Nummer" to "1", "Titel" to "Eins", "Aktiv" to "1"))),
    )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(source: ImportFileSource): ImportViewModel {
        val events = FakeWeekEventRepository()
        val prepare = PrepareImportUseCase(
            events,
            catalog,
            ObserveExternalDirectoryUseCase(directory),
            schedule,
            FakeCongregationMetadataRepository(listOf(CongregationMetadata(id = testCongregation.congregationId))),
            ImportReferenceDataUseCase(SaveWeekEventUseCase(events, reference), SaveTalkUseCase(catalog, reference)),
            ImportSpeakerDirectoryUseCase(
                SaveExternalCircuitUseCase(directory, planning),
                SaveExternalCongregationUseCase(directory, planning),
                SaveVisitingSpeakerUseCase(directory, planning),
            ),
            ImportTalkScheduleUseCase(SaveTalkSlotUseCase(schedule, planning)),
        )
        return ImportViewModel(source, prepare, reference, planning)
    }

    @Test
    fun `sources follow the role`() {
        val source = FakeFileSource(Result.success(null))

        val admin = viewModel(source).apply { onCurrentUserChanged(globalAdmin) }
        val plannerModel = viewModel(source).apply { onCurrentUserChanged(planner) }
        val plain = viewModel(source).apply { onCurrentUserChanged(member) }

        assertEquals(listOf(ImportSource.WEEK_EVENTS, ImportSource.TALK_CATALOG), admin.uiState.value.allowedSources)
        assertEquals(listOf(ImportSource.SPEAKERS, ImportSource.SCHEDULE), plannerModel.uiState.value.allowedSources)
        assertTrue(plain.uiState.value.allowedSources.isEmpty())
    }

    @Test
    fun `without a file dialog nothing is offered`() {
        val model = viewModel(FakeFileSource(Result.success(null), isAvailable = false))
        model.onCurrentUserChanged(globalAdmin)

        assertTrue(model.uiState.value.allowedSources.isEmpty())
    }

    @Test
    fun `choosing a file shows a preview and confirming writes and reports`() {
        val model = viewModel(FakeFileSource(Result.success(talkTable)))
        model.onCurrentUserChanged(globalAdmin)

        model.choose(ImportSource.TALK_CATALOG)
        val pending = assertNotNull(model.uiState.value.pending)
        assertEquals(1, pending.prepared.summaries.single().create)
        assertTrue(catalog.savedTalks.isEmpty()) // nothing is written before the confirmation

        model.confirm()
        assertEquals(listOf(Talk(1, "Eins")), catalog.savedTalks)
        assertEquals(1, model.uiState.value.report?.created)
        assertNull(model.uiState.value.pending)
    }

    @Test
    fun `cancel and dismiss only close their own step`() {
        val model = viewModel(FakeFileSource(Result.success(talkTable)))
        model.onCurrentUserChanged(globalAdmin)
        model.choose(ImportSource.TALK_CATALOG)

        model.dismissReport() // no report yet: the preview stays
        assertNotNull(model.uiState.value.pending)

        model.confirm()
        model.cancel() // no preview any more: the report stays
        assertEquals(ImportSource.TALK_CATALOG, model.uiState.value.reportSource)

        model.dismissReport()
        assertEquals(ImportStep.Idle, model.uiState.value.step)
    }

    @Test
    fun `cancelling a choice or a preview writes nothing`() {
        val source = FakeFileSource(Result.success(null))
        val model = viewModel(source)
        model.onCurrentUserChanged(globalAdmin)

        model.choose(ImportSource.TALK_CATALOG) // dialog cancelled
        assertNull(model.uiState.value.pending)
        assertTrue(!model.uiState.value.isBusy)

        source.result = Result.success(talkTable)
        model.choose(ImportSource.TALK_CATALOG)
        model.cancel()
        assertNull(model.uiState.value.pending)
        assertTrue(catalog.savedTalks.isEmpty())
    }

    @Test
    fun `an unreadable file is an error and a source without permission is ignored`() {
        val model = viewModel(FakeFileSource(Result.failure(IllegalStateException("kaputt"))))
        model.onCurrentUserChanged(globalAdmin)

        model.choose(ImportSource.TALK_CATALOG)
        assertTrue(model.uiState.value.errorMessage.orEmpty().contains("kaputt"))
        model.dismissError()
        assertNull(model.uiState.value.errorMessage)

        model.choose(ImportSource.SCHEDULE) // the global admin may not plan talks
        assertNull(model.uiState.value.pending)
        assertNull(model.uiState.value.errorMessage)
    }
}
