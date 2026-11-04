package de.geosphere.congregationplaner.organization.presentation

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.usecase.DeleteOrgUnitUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.ObserveOrgUnitsUseCase
import de.geosphere.congregationplaner.organization.domain.usecase.SaveOrgUnitUseCase
import de.geosphere.congregationplaner.organization.testing.FakeOrgUnitRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.OrganizationPermissionPolicy
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
class OrganizationViewModelTest {
    private val admin = AppUser(uid = "a", isGlobalAdmin = true)
    private val emea = OrgUnit("r1", "EMEA")
    private val europe = OrgUnit("s1", "Europa")
    private val central = OrgUnit("b1", "Mitteleuropa")
    private val north = OrgUnit("c1", "Kreis Nord", "Max")

    private val repository = FakeOrgUnitRepository(
        mapOf(
            "region" to listOf(emea),
            "region/r1/subregion" to listOf(europe),
            "region/r1/subregion/s1/branch" to listOf(central),
            "region/r1/subregion/s1/branch/b1/circuit" to listOf(north),
        ),
    )
    private lateinit var viewModel: OrganizationViewModel

    private val state get() = viewModel.uiState.value

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val policy = OrganizationPermissionPolicy()
        viewModel = OrganizationViewModel(
            ObserveOrgUnitsUseCase(repository),
            SaveOrgUnitUseCase(repository, policy),
            DeleteOrgUnitUseCase(repository, policy),
            policy,
        )
        viewModel.onCurrentUserChanged(admin)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with the regions`() {
        assertEquals(OrgLevel.REGION, state.level)
        assertEquals(listOf(emea), state.units)
        assertFalse(state.isLoading)
        assertTrue(state.canManage)
    }

    @Test
    fun `opening units walks down to the congregations of a circuit`() {
        viewModel.open(emea)
        viewModel.open(europe)
        viewModel.open(central)
        assertEquals(OrgLevel.CIRCUIT, state.level)
        assertEquals(listOf(north), state.units)

        viewModel.open(north)
        assertEquals(OrgLevel.CONGREGATION, state.level)
        assertEquals(listOf("r1", "s1", "b1", "c1"), state.parentIds)

        // Below a circuit there are no further units to open.
        viewModel.open(OrgUnit("k1", "Berlin"))
        assertEquals(4, state.path.size)
    }

    @Test
    fun `the path bar and back go up again`() {
        viewModel.open(emea)
        viewModel.open(europe)

        viewModel.goTo(1)
        assertEquals(listOf(emea), state.path)
        assertEquals(listOf(europe), state.units)

        assertTrue(viewModel.back())
        assertEquals(OrgLevel.REGION, state.level)
        assertFalse(viewModel.back())
    }

    @Test
    fun `a new unit is created on the current level and stays open`() {
        viewModel.open(emea)
        viewModel.create()
        assertTrue(checkNotNull(state.editor).isNew)

        viewModel.updateEditor(OrgUnit(name = "Afrika"))
        viewModel.save()

        assertEquals(OrgUnitEditorState(OrgUnit("new-1", "Afrika")), state.editor)
        assertEquals(listOf("Afrika", "Europa"), state.units.map { it.name })
        assertEquals(listOf(europe, OrgUnit("new-1", "Afrika")), repository.units.value["region/r1/subregion"])
    }

    @Test
    fun `editing keeps the id and discarding restores the unit`() {
        viewModel.edit(emea)
        viewModel.updateEditor(OrgUnit("other", "Neu"))
        assertEquals(OrgUnit("r1", "Neu"), state.editor?.unit)
        assertTrue(checkNotNull(state.editor).isDirty)

        viewModel.discardChanges()
        assertEquals(OrgUnitEditorState(emea), state.editor)

        viewModel.cancelEditing()
        assertNull(state.editor)
    }

    @Test
    fun `units with entries below cannot be deleted`() {
        viewModel.edit(emea)
        viewModel.delete(emea)

        assertTrue(state.errorMessage.orEmpty().contains("EMEA"))
        assertEquals(listOf(emea), state.units)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `an empty unit is deleted and its editor closed`() {
        viewModel.open(emea)
        viewModel.open(europe)
        viewModel.open(central)
        viewModel.edit(north)

        viewModel.delete(north)

        assertTrue(state.units.isEmpty())
        assertNull(state.editor)
    }

    @Test
    fun `failed saves show the error and keep the input`() {
        viewModel.create()
        viewModel.save()

        assertEquals("Bitte gib einen Namen ein.", state.errorMessage)
        assertTrue(checkNotNull(state.editor).isNew)
        assertFalse(state.isSaving)
    }

    @Test
    fun `members cannot create units`() {
        viewModel.onCurrentUserChanged(AppUser(uid = "m", approved = true))

        viewModel.create()

        assertFalse(state.canManage)
        assertNull(state.editor)
    }
}
