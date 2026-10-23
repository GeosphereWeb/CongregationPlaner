package de.geosphere.congregationplaner.organization.presentation

import de.geosphere.congregationplaner.organization.data.InMemoryKeyValueStorage
import de.geosphere.congregationplaner.organization.data.KeyValueCongregationSelectionStore
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.OrganizationRepository
import de.geosphere.congregationplaner.organization.domain.usecase.LoadOrganizationUnitsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CongregationSelectionViewModelTest {
    private val ref = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val repository = FakeOrganizationRepository(
        mapOf(
            "region" to listOf(OrgUnit("EMEA")),
            "region/EMEA/subregion" to listOf(OrgUnit("europe", "Europa")),
            "region/EMEA/subregion/europe/branch" to listOf(OrgUnit("central-europe")),
            "region/EMEA/subregion/europe/branch/central-europe/circuit" to listOf(OrgUnit("609a")),
            "region/EMEA/subregion/europe/branch/central-europe/circuit/609a/congregation" to listOf(
                OrgUnit("hamburg", "Hamburg"),
                OrgUnit("berlin", "Berlin"),
            ),
        ),
    )
    private val store = KeyValueCongregationSelectionStore(InMemoryKeyValueStorage())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = CongregationSelectionViewModel(LoadOrganizationUnitsUseCase(repository), store)

    @Test
    fun `without stored selection the regions are loaded`() {
        val state = createViewModel().uiState.value

        assertNull(state.selected)
        assertTrue(state.isPicking)
        assertEquals(listOf(OrgUnit("EMEA")), state.levels.single().options)
    }

    @Test
    fun `stored selection is restored without loading`() {
        store.save(SelectedCongregation(ref, "Berlin"))

        val state = createViewModel().uiState.value

        assertEquals(SelectedCongregation(ref, "Berlin"), state.selected)
        assertFalse(state.isPicking)
        assertTrue(repository.requestedPaths.isEmpty())
    }

    @Test
    fun `selecting every level down to the congregation selects and stores it`() {
        val viewModel = createViewModel()

        viewModel.selectUnit(OrgLevel.REGION, "EMEA")
        viewModel.selectUnit(OrgLevel.SUBREGION, "europe")
        viewModel.selectUnit(OrgLevel.BRANCH, "central-europe")
        viewModel.selectUnit(OrgLevel.CIRCUIT, "609a")
        // Versammlungen werden nach Namen sortiert.
        assertEquals(listOf("berlin", "hamburg"), viewModel.uiState.value.levels.last().options.map { it.id })

        viewModel.selectUnit(OrgLevel.CONGREGATION, "berlin")

        val state = viewModel.uiState.value
        assertEquals(SelectedCongregation(ref, "Berlin"), state.selected)
        assertFalse(state.isPicking)
        assertEquals(SelectedCongregation(ref, "Berlin"), store.load())
    }

    @Test
    fun `changing an upper level discards lower levels`() {
        val viewModel = createViewModel()
        viewModel.selectUnit(OrgLevel.REGION, "EMEA")
        viewModel.selectUnit(OrgLevel.SUBREGION, "europe")

        viewModel.selectUnit(OrgLevel.REGION, "EMEA")

        assertEquals(listOf(OrgLevel.REGION, OrgLevel.SUBREGION), viewModel.uiState.value.levels.map { it.level })
        assertNull(viewModel.uiState.value.levels.last().selectedId)
    }

    @Test
    fun `unknown units and levels are ignored`() {
        val viewModel = createViewModel()

        viewModel.selectUnit(OrgLevel.REGION, "APAC")
        viewModel.selectUnit(OrgLevel.CIRCUIT, "609a")

        assertEquals(1, viewModel.uiState.value.levels.size)
        assertNull(viewModel.uiState.value.levels.single().selectedId)
    }

    @Test
    fun `load failures and empty levels are reported and can be retried`() {
        repository.failure = IllegalStateException("offline")
        val viewModel = createViewModel()
        assertEquals("offline", viewModel.uiState.value.errorMessage)

        repository.failure = null
        viewModel.retry()
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(listOf(OrgUnit("EMEA")), viewModel.uiState.value.levels.single().options)

        repository.units = emptyMap()
        viewModel.selectUnit(OrgLevel.REGION, "EMEA")
        assertEquals("Keine Einträge für Subregion gefunden.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `valid invite link selects the existing congregation`() {
        val viewModel = createViewModel()

        viewModel.onInviteLinkChanged(ref.toInviteLink())
        viewModel.applyInviteLink()

        assertEquals(SelectedCongregation(ref, "Berlin"), viewModel.uiState.value.selected)
        assertEquals("", viewModel.uiState.value.inviteLinkInput)
        assertEquals(SelectedCongregation(ref, "Berlin"), store.load())
    }

    @Test
    fun `invite link from the clipboard selects the congregation`() {
        val viewModel = createViewModel()

        viewModel.pasteInviteLink(ref.toInviteLink())

        assertEquals(SelectedCongregation(ref, "Berlin"), viewModel.uiState.value.selected)
        assertEquals("", viewModel.uiState.value.inviteLinkInput)
    }

    @Test
    fun `empty or foreign clipboard content is reported`() {
        val viewModel = createViewModel()

        viewModel.pasteInviteLink(null)
        assertEquals("Die Zwischenablage enthält keinen Text.", viewModel.uiState.value.errorMessage)

        viewModel.pasteInviteLink("Hallo Welt")
        assertEquals("Der Einladungslink ist ungültig.", viewModel.uiState.value.errorMessage)
        assertEquals("Hallo Welt", viewModel.uiState.value.inviteLinkInput)
        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun `invalid or unknown invite links are rejected`() {
        val viewModel = createViewModel()

        viewModel.applyInviteLink("https://example.com")
        assertEquals("Der Einladungslink ist ungültig.", viewModel.uiState.value.errorMessage)

        viewModel.applyInviteLink(ref.copy(congregationId = "muenchen").toInviteLink())
        assertEquals(
            "Die Versammlung aus dem Einladungslink wurde nicht gefunden.",
            viewModel.uiState.value.errorMessage,
        )
        assertNull(viewModel.uiState.value.selected)

        repository.failure = IllegalStateException("offline")
        viewModel.applyInviteLink(ref.toInviteLink())
        assertEquals("offline", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isResolvingLink)
    }

    @Test
    fun `picking can be cancelled only with an existing selection and selection can be cleared`() {
        val viewModel = createViewModel()
        viewModel.cancelPicking()
        assertTrue(viewModel.uiState.value.isPicking)

        viewModel.applyInviteLink(ref.toInviteLink())
        viewModel.startPicking()
        viewModel.cancelPicking()
        assertFalse(viewModel.uiState.value.isPicking)
        assertEquals(SelectedCongregation(ref, "Berlin"), viewModel.uiState.value.selected)

        viewModel.clearSelection()
        assertNull(viewModel.uiState.value.selected)
        assertNull(store.load())
        assertTrue(viewModel.uiState.value.isPicking)
    }

    private class FakeOrganizationRepository(var units: Map<String, List<OrgUnit>>) : OrganizationRepository {
        var failure: Throwable? = null
        val requestedPaths = mutableListOf<String>()

        override suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>> {
            val path = level.collectionPath(parentIds)
            requestedPaths += path
            return failure?.let { Result.failure(it) } ?: Result.success(units[path].orEmpty())
        }
    }
}
