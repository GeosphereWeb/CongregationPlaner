package de.geosphere.congregationplaner.circuit.presentation

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
import de.geosphere.congregationplaner.circuit.domain.usecase.CreateCircuitUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.DeleteCircuitUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.ObserveCircuitsUseCase
import de.geosphere.congregationplaner.circuit.domain.usecase.UpdateCircuitUseCase
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.permission.CircuitPermissionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
class CircuitViewModelTest {
    private val admin = AppUser(uid = "admin", approved = true, isGlobalAdmin = true)
    private val planer = AppUser(uid = "planer", approved = true, role = UserRole.CONGREGATION_ADMIN)
    private val north = Circuit(id = "609a", name = "Nord")
    private val south = Circuit(id = "610b", name = "Süd")

    private lateinit var repository: FakeCircuitRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeCircuitRepository(listOf(north, south))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads circuits and denies everything without current user`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(listOf(north, south), state.circuits)
        assertFalse(state.isLoading)
        assertEquals(CircuitPermissions(), state.permissions)

        viewModel.createCircuit()
        assertNull(viewModel.uiState.value.editor)
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `shows error when circuits cannot be loaded`() {
        repository.observeError = IllegalStateException("offline")

        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("offline", state.errorMessage)
    }

    @Test
    fun `admin creates a new circuit with a chosen id`() {
        val viewModel = createViewModel(admin)
        assertEquals(CircuitPermissions(canCreate = true, canManage = true), viewModel.uiState.value.permissions)

        viewModel.createCircuit()
        assertTrue(viewModel.uiState.value.editor!!.isNew)
        assertTrue(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.saveCircuit()
        assertEquals(CircuitException.InvalidId().message, viewModel.uiState.value.errorMessage)

        viewModel.updateEditor(Circuit(id = " 600C ", name = " Mitte "))
        viewModel.saveCircuit()

        val state = viewModel.uiState.value
        val created = Circuit(id = "600c", name = "Mitte")
        assertEquals(created, repository.created.single())
        assertEquals(CircuitEditorState(created, isNew = false), state.editor)
        assertEquals(listOf(created, north, south), state.circuits)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `duplicate circuit id is reported`() {
        repository.createResult = Result.failure(CircuitException.AlreadyExists("609a"))
        val viewModel = createViewModel(admin)

        viewModel.createCircuit()
        viewModel.updateEditor(Circuit(id = "609a", name = "Doppelt"))
        viewModel.saveCircuit()

        assertEquals(CircuitException.AlreadyExists("609a").message, viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.editor!!.isNew)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `admin updates an existing circuit but cannot change its id`() {
        val viewModel = createViewModel(admin)

        viewModel.editCircuit(north)
        viewModel.updateEditor(north.copy(id = "999z", name = "Nord-Ost", overseerName = "Max"))
        viewModel.saveCircuit()

        val updated = north.copy(name = "Nord-Ost", overseerName = "Max")
        assertTrue(repository.created.isEmpty())
        assertEquals(listOf(updated), repository.updated)
        assertEquals(listOf(updated, south), viewModel.uiState.value.circuits)
    }

    @Test
    fun `save without editor does nothing`() {
        val viewModel = createViewModel(admin)

        viewModel.saveCircuit()

        assertTrue(repository.created.isEmpty())
        assertTrue(repository.updated.isEmpty())
    }

    @Test
    fun `non admin may view but not modify circuits`() {
        val viewModel = createViewModel(planer)

        viewModel.editCircuit(north)
        assertEquals(CircuitEditorState(north, isNew = false), viewModel.uiState.value.editor)
        assertFalse(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.updateEditor(north.copy(name = "Hack"))
        assertEquals(north, viewModel.uiState.value.editor!!.circuit)

        viewModel.saveCircuit()
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)

        viewModel.deleteCircuit(north)
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)
        assertTrue(repository.updated.isEmpty())
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `permissions follow user changes`() {
        val viewModel = createViewModel(admin)
        viewModel.editCircuit(north)
        assertTrue(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.onCurrentUserChanged(planer)
        assertEquals(CircuitPermissions(), viewModel.uiState.value.permissions)
    }

    @Test
    fun `admin deletes circuit and selection is cleared`() {
        val viewModel = createViewModel(admin)
        viewModel.editCircuit(north)

        viewModel.deleteCircuit(north)

        val state = viewModel.uiState.value
        assertEquals(listOf("609a"), repository.deletedIds)
        assertEquals(listOf(south), state.circuits)
        assertNull(state.editor)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `delete keeps other selection and reports failures`() {
        repository.deleteResult = Result.failure(CircuitException.HasCongregations("609a"))
        val viewModel = createViewModel(admin)
        viewModel.editCircuit(south)

        viewModel.deleteCircuit(Circuit(name = "Ohne Nummer"))
        assertTrue(repository.deletedIds.isEmpty())

        viewModel.deleteCircuit(north)
        val state = viewModel.uiState.value
        assertEquals(CircuitException.HasCongregations("609a").message, state.errorMessage)
        assertEquals(listOf(north, south), state.circuits)
        assertEquals(south, state.editor!!.circuit)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `cancel editing clears editor and error`() {
        val viewModel = createViewModel(admin)
        viewModel.createCircuit()
        viewModel.saveCircuit()

        viewModel.cancelEditing()

        assertNull(viewModel.uiState.value.editor)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.permissions.canEditSelection)
    }

    @Test
    fun `editor is dirty only after a change and clean again after saving`() {
        val viewModel = createViewModel(admin)

        viewModel.editCircuit(north)
        assertFalse(viewModel.uiState.value.editor!!.isDirty)

        viewModel.updateEditor(north.copy(name = "Nord-Ost"))
        assertTrue(viewModel.uiState.value.editor!!.isDirty)

        viewModel.saveCircuit()
        assertFalse(viewModel.uiState.value.editor!!.isDirty)
    }

    @Test
    fun `discard changes keeps the selected circuit but resets edits`() {
        val viewModel = createViewModel(admin)
        viewModel.editCircuit(north)
        viewModel.updateEditor(north.copy(name = "Nord-Ost"))

        viewModel.discardChanges()

        assertEquals(CircuitEditorState(north, isNew = false), viewModel.uiState.value.editor)
    }

    @Test
    fun `detail is open exactly while an editor exists`() {
        val viewModel = createViewModel(admin)
        assertFalse(viewModel.uiState.value.hasDetail)

        viewModel.editCircuit(north)
        assertTrue(viewModel.uiState.value.hasDetail)

        viewModel.discardChanges()
        assertTrue(viewModel.uiState.value.hasDetail)

        viewModel.cancelEditing()
        assertFalse(viewModel.uiState.value.hasDetail)
    }

    @Test
    fun `discard changes without editor does nothing`() {
        val viewModel = createViewModel(admin)

        viewModel.discardChanges()

        assertNull(viewModel.uiState.value.editor)
    }

    @Test
    fun `discard changes closes a new circuit`() {
        val viewModel = createViewModel(admin)
        viewModel.createCircuit()

        viewModel.discardChanges()

        assertNull(viewModel.uiState.value.editor)
    }

    private fun createViewModel(user: AppUser? = null) = CircuitViewModel(
        ObserveCircuitsUseCase(repository),
        CreateCircuitUseCase(repository),
        UpdateCircuitUseCase(repository),
        DeleteCircuitUseCase(repository),
        CircuitPermissionPolicy(),
    ).also { viewModel -> user?.let(viewModel::onCurrentUserChanged) }

    private class FakeCircuitRepository(circuits: List<Circuit>) : CircuitRepository {
        private val circuits = MutableStateFlow(circuits)
        var observeError: Throwable? = null
        var createResult: Result<Circuit>? = null
        var deleteResult: Result<Unit> = Result.success(Unit)
        val created = mutableListOf<Circuit>()
        val updated = mutableListOf<Circuit>()
        val deletedIds = mutableListOf<String>()

        override fun observeAll(): Flow<List<Circuit>> = observeError?.let { error -> flow { throw error } } ?: circuits

        override suspend fun create(circuit: Circuit): Result<Circuit> {
            createResult?.let { return it }
            created += circuit
            return Result.success(circuit)
        }

        override suspend fun update(circuit: Circuit): Result<Circuit> {
            updated += circuit
            return Result.success(circuit)
        }

        override suspend fun delete(circuitId: String): Result<Unit> {
            if (deleteResult.isSuccess) deletedIds += circuitId
            return deleteResult
        }
    }

    private companion object {
        const val MISSING_PERMISSION = "Dafür fehlt dir die Berechtigung."
    }
}
