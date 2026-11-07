package de.geosphere.congregationplaner.organization.presentation

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.usecase.CreateCongregationUseCase
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
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
    private val berlinSelection = SelectedCongregation(ref, "Berlin", "Kreis Nord")
    private val repository = FakeOrganizationRepository(
        mapOf(
            "region" to listOf(OrgUnit("EMEA")),
            "region/EMEA/subregion" to listOf(OrgUnit("europe", "Europa")),
            "region/EMEA/subregion/europe/branch" to listOf(OrgUnit("central-europe")),
            "region/EMEA/subregion/europe/branch/central-europe/circuit" to listOf(OrgUnit("609a", "Kreis Nord")),
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

    private val congregations = FakeCongregationRepository()

    private fun createViewModel() = CongregationSelectionViewModel(
        LoadOrganizationUnitsUseCase(repository),
        store,
        CreateCongregationUseCase(congregations),
    )

    private val authUser = object : FirebaseUser {
        override val uid = "uid-1"
        override val email = "max@example.com"
        override val displayName = "Max Muster"
        override val idToken: String? = null
        override val emailVerified = true
    }

    private fun CongregationSelectionViewModel.selectCircuit() {
        selectUnit(OrgLevel.REGION, "EMEA")
        selectUnit(OrgLevel.SUBREGION, "europe")
        selectUnit(OrgLevel.BRANCH, "central-europe")
        selectUnit(OrgLevel.CIRCUIT, "609a")
    }

    @Test
    fun `a new congregation can only be started once the circuit is chosen`() {
        val viewModel = createViewModel()
        viewModel.startNewCongregation()
        assertNull(viewModel.uiState.value.newCongregation)
        assertFalse(viewModel.uiState.value.canStartNewCongregation)

        viewModel.selectUnit(OrgLevel.REGION, "EMEA")
        viewModel.startNewCongregation()
        assertNull(viewModel.uiState.value.newCongregation)

        viewModel.selectUnit(OrgLevel.SUBREGION, "europe")
        viewModel.selectUnit(OrgLevel.BRANCH, "central-europe")
        viewModel.selectUnit(OrgLevel.CIRCUIT, "609a")
        assertTrue(viewModel.uiState.value.canStartNewCongregation)
        viewModel.startNewCongregation()

        assertEquals(
            NewCongregationDraft(listOf("EMEA", "europe", "central-europe", "609a"), "Kreis Nord"),
            viewModel.uiState.value.newCongregation,
        )
    }

    @Test
    fun `the draft is ready with a valid name and can be cancelled`() {
        val viewModel = createViewModel()
        viewModel.selectCircuit()
        viewModel.startNewCongregation()
        assertFalse(viewModel.uiState.value.isNewCongregationReady)

        viewModel.onNewCongregationNameChanged("Nord/Süd")
        assertFalse(viewModel.uiState.value.isNewCongregationReady)

        viewModel.onNewCongregationNameChanged("Grafing Süd")
        assertTrue(viewModel.uiState.value.isNewCongregationReady)

        viewModel.onNewCongregationNameChanged("x".repeat(500))
        assertEquals(CongregationName.MAX_LENGTH, viewModel.uiState.value.newCongregation?.name?.length)

        viewModel.cancelNewCongregation()
        assertNull(viewModel.uiState.value.newCongregation)
        assertFalse(viewModel.uiState.value.isNewCongregationReady)
    }

    @Test
    fun `a valid name only leads to sign in after confirming`() {
        val viewModel = createViewModel()
        viewModel.selectCircuit()
        viewModel.startNewCongregation()

        viewModel.confirmNewCongregation()
        assertFalse(viewModel.uiState.value.isNewCongregationConfirmed)

        viewModel.onNewCongregationNameChanged("Grafing Süd")
        assertFalse(viewModel.uiState.value.isNewCongregationConfirmed)

        viewModel.confirmNewCongregation()
        assertTrue(viewModel.uiState.value.isNewCongregationConfirmed)

        viewModel.onNewCongregationNameChanged("Nord/Süd")
        assertFalse(viewModel.uiState.value.isNewCongregationConfirmed)

        viewModel.onNewCongregationNameChanged("Grafing Süd")
        viewModel.confirmNewCongregation()
        viewModel.editNewCongregation()
        assertFalse(viewModel.uiState.value.isNewCongregationConfirmed)
        assertEquals("Grafing Süd", viewModel.uiState.value.newCongregation?.name)
    }

    @Test
    fun `creating selects the new congregation and makes the user its first admin`() {
        val viewModel = createViewModel()
        viewModel.selectCircuit()
        viewModel.startNewCongregation()
        viewModel.onNewCongregationNameChanged("  Grafing Süd ")

        viewModel.createNewCongregation(authUser)

        val state = viewModel.uiState.value
        val expectedRef = CongregationRef("EMEA", "europe", "central-europe", "609a", "new-congregation")
        assertEquals(SelectedCongregation(expectedRef, "Grafing Süd", "Kreis Nord"), state.selected)
        assertEquals(state.selected, store.load())
        assertNull(state.newCongregation)
        assertFalse(state.isCreating)
        assertFalse(state.isPicking)
        assertEquals("uid-1", congregations.createdBy)
        assertEquals(listOf("EMEA", "europe", "central-europe", "609a"), congregations.circuit)
        val admin = checkNotNull(congregations.firstAdmin)
        assertEquals("Max", admin.firstName)
        assertEquals("Muster", admin.lastName)
        assertEquals("max@example.com", admin.email)
        assertTrue(admin.emailVerified)
    }

    @Test
    fun `creating reports an unconfirmed e-mail address and a taken name and keeps the draft`() {
        val viewModel = createViewModel()
        viewModel.selectCircuit()
        viewModel.startNewCongregation()
        viewModel.onNewCongregationNameChanged("Grafing Süd")

        viewModel.createNewCongregation(
            object : FirebaseUser by authUser {
                override val emailVerified = false
            },
        )
        assertEquals(CongregationException.EmailNotVerified().message, viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isCreating)

        congregations.nameOwner = "other"
        viewModel.createNewCongregation(authUser)
        assertEquals(CongregationException.NameTaken().message, viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.selected)
        assertEquals("Grafing Süd", viewModel.uiState.value.newCongregation?.name)
        assertNull(congregations.createdBy)

        viewModel.onNewCongregationNameChanged("Grafing Nord")
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `a draft overrides the stored selection until it is cancelled or created`() {
        store.save(berlinSelection)
        val viewModel = createViewModel()
        assertEquals(berlinSelection, viewModel.uiState.value.activeSelection)

        // „Ändern“ im Login: die gespeicherte Auswahl bleibt, bis eine andere feststeht.
        viewModel.startPicking()
        viewModel.selectCircuit()
        assertEquals(berlinSelection, viewModel.uiState.value.activeSelection)

        viewModel.startNewCongregation()
        viewModel.onNewCongregationNameChanged("Grafing Süd")
        assertEquals(berlinSelection, viewModel.uiState.value.selected)
        assertNull(viewModel.uiState.value.activeSelection)

        viewModel.cancelNewCongregation()
        assertEquals(berlinSelection, viewModel.uiState.value.activeSelection)

        viewModel.startNewCongregation()
        viewModel.onNewCongregationNameChanged("Grafing Süd")
        viewModel.createNewCongregation(authUser)
        assertEquals("Grafing Süd", viewModel.uiState.value.activeSelection?.name)
        assertEquals("Grafing Süd", store.load()?.name)
    }

    @Test
    fun `creating without a draft or while another run is active does nothing`() {
        val viewModel = createViewModel()

        viewModel.createNewCongregation(authUser)

        assertNull(viewModel.uiState.value.selected)
        assertNull(congregations.createdBy)
    }

    private class FakeCongregationRepository : CongregationMetadataRepository {
        var nameOwner: String? = null
        var createdBy: String? = null
        var firstAdmin: NewCongregationAdmin? = null
        var circuit: List<String>? = null

        override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> = emptyFlow()

        override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> =
            Result.success(nameOwner)

        override suspend fun create(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
            createdBy: String,
            firstAdmin: NewCongregationAdmin?,
        ): Result<CongregationMetadata> {
            circuit = circuitIds
            this.createdBy = createdBy
            this.firstAdmin = firstAdmin
            return Result.success(congregation.copy(id = "new-congregation"))
        }

        override suspend fun save(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
        ): Result<CongregationMetadata> = Result.success(congregation)

        override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun `without stored selection the regions are loaded`() {
        val state = createViewModel().uiState.value

        assertNull(state.selected)
        assertTrue(state.isPicking)
        assertEquals(listOf(OrgUnit("EMEA")), state.levels.single().options)
    }

    @Test
    fun `stored selection is restored without loading`() {
        store.save(berlinSelection)

        val state = createViewModel().uiState.value

        assertEquals(berlinSelection, state.selected)
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
        assertEquals(berlinSelection, state.selected)
        assertFalse(state.isPicking)
        assertEquals(berlinSelection, store.load())
    }

    @Test
    fun `the circuit name is kept with the selection from the levels and from an invite link`() {
        val viewModel = createViewModel()
        viewModel.selectUnit(OrgLevel.REGION, "EMEA")
        viewModel.selectUnit(OrgLevel.SUBREGION, "europe")
        viewModel.selectUnit(OrgLevel.BRANCH, "central-europe")
        viewModel.selectUnit(OrgLevel.CIRCUIT, "609a")
        viewModel.selectUnit(OrgLevel.CONGREGATION, "berlin")
        assertEquals("Kreis Nord", viewModel.uiState.value.selected?.circuitName)

        viewModel.clearSelection()
        viewModel.applyInviteLink(ref.toInviteLink())
        assertEquals("Kreis Nord", viewModel.uiState.value.selected?.circuitName)
        assertEquals("Kreis Nord", store.load()?.circuitName)
    }

    @Test
    fun `an invite link still selects the congregation when the circuit name is unknown`() {
        repository.units = repository.units - "region/EMEA/subregion/europe/branch/central-europe/circuit"
        val viewModel = createViewModel()

        viewModel.applyInviteLink(ref.toInviteLink())

        assertEquals(SelectedCongregation(ref, "Berlin", ""), viewModel.uiState.value.selected)
        assertEquals("609a", viewModel.uiState.value.selected?.circuitDisplayName)
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

        assertEquals(berlinSelection, viewModel.uiState.value.selected)
        assertEquals("", viewModel.uiState.value.inviteLinkInput)
        assertEquals(berlinSelection, store.load())
    }

    @Test
    fun `invite link from the clipboard selects the congregation`() {
        val viewModel = createViewModel()

        viewModel.pasteInviteLink(ref.toInviteLink())

        assertEquals(berlinSelection, viewModel.uiState.value.selected)
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
        assertEquals(berlinSelection, viewModel.uiState.value.selected)

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
