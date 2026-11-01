package de.geosphere.congregationplaner.congregation.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.usecase.DeleteCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.ObserveCongregationsUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.SaveCongregationUseCase
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.permission.CongregationPermissionPolicy
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
class CongregationMetadataViewModelTest {
    private val admin = AppUser(uid = "admin", approved = true, isGlobalAdmin = true)
    private val planer = AppUser(uid = "planer", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
    private val berlin = CongregationMetadata(id = "609a", name = "Berlin")
    private val hamburg = CongregationMetadata(id = "610b", name = "Hamburg")

    private lateinit var repository: FakeCongregationRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeCongregationRepository(listOf(berlin, hamburg))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads congregations and denies everything without current user`() {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertEquals(listOf(berlin, hamburg), state.congregations)
        assertFalse(state.isLoading)
        assertEquals(CongregationPermissions(), state.permissions)

        viewModel.createCongregation()
        assertNull(viewModel.uiState.value.editor)
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `shows error when congregations cannot be loaded`() {
        repository.observeError = IllegalStateException("offline")

        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("offline", state.errorMessage)
    }

    @Test
    fun `admin creates and saves a new congregation`() {
        val viewModel = createViewModel(admin)
        assertEquals(CongregationPermissions(canCreate = true, canManage = true), viewModel.uiState.value.permissions)

        viewModel.createCongregation()
        assertTrue(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.saveCongregation()
        assertEquals("Bitte gib einen Versammlungsnamen ein.", viewModel.uiState.value.errorMessage)

        viewModel.updateEditor(CongregationMetadata(name = "  München  "))
        viewModel.saveCongregation()

        val state = viewModel.uiState.value
        assertEquals("München", repository.saved.single().name)
        assertEquals(CongregationMetadata(id = "new-id", name = "München"), state.editor)
        assertEquals(listOf(berlin, hamburg, state.editor), state.congregations)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun `admin updates an existing congregation in place`() {
        val viewModel = createViewModel(admin)

        viewModel.editCongregation(berlin)
        viewModel.updateEditor(berlin.copy(name = "Berlin-Mitte"))
        viewModel.saveCongregation()

        assertEquals(listOf(berlin.copy(name = "Berlin-Mitte"), hamburg), viewModel.uiState.value.congregations)
    }

    @Test
    fun `save without editor does nothing`() {
        val viewModel = createViewModel(admin)

        viewModel.saveCongregation()

        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `save failure is shown to the user`() {
        repository.saveResult = Result.failure(IllegalStateException("permission denied"))
        val viewModel = createViewModel(admin)

        viewModel.editCongregation(berlin)
        viewModel.saveCongregation()

        assertEquals("permission denied", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `non admin may view but not modify congregations`() {
        val viewModel = createViewModel(planer)

        viewModel.editCongregation(berlin)
        assertEquals(berlin, viewModel.uiState.value.editor)
        assertFalse(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.updateEditor(berlin.copy(name = "Hack"))
        assertEquals(berlin, viewModel.uiState.value.editor)

        viewModel.saveCongregation()
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)

        viewModel.deleteCongregation(berlin)
        assertEquals(MISSING_PERMISSION, viewModel.uiState.value.errorMessage)
        assertTrue(repository.saved.isEmpty())
        assertTrue(repository.deletedIds.isEmpty())
    }

    @Test
    fun `permissions follow user changes`() {
        val viewModel = createViewModel(admin)
        viewModel.editCongregation(berlin)
        assertTrue(viewModel.uiState.value.permissions.canEditSelection)

        viewModel.onCurrentUserChanged(planer)
        assertEquals(CongregationPermissions(), viewModel.uiState.value.permissions)

        viewModel.onCurrentUserChanged(null)
        assertEquals(CongregationPermissions(), viewModel.uiState.value.permissions)
    }

    @Test
    fun `admin deletes congregation and selection is cleared`() {
        val viewModel = createViewModel(admin)
        viewModel.editCongregation(berlin)

        viewModel.deleteCongregation(berlin)

        val state = viewModel.uiState.value
        assertEquals(listOf("609a"), repository.deletedIds)
        assertEquals(listOf(hamburg), state.congregations)
        assertNull(state.editor)
        assertFalse(state.isDeleting)
    }

    @Test
    fun `delete keeps other selection`() {
        val viewModel = createViewModel(admin)
        viewModel.editCongregation(hamburg)

        viewModel.deleteCongregation(berlin)

        assertEquals(hamburg, viewModel.uiState.value.editor)
    }

    @Test
    fun `delete ignores unsaved congregations and reports failures`() {
        repository.deleteResult = Result.failure(IllegalStateException("not found"))
        val viewModel = createViewModel(admin)

        viewModel.deleteCongregation(CongregationMetadata(name = "Neu"))
        assertTrue(repository.deletedIds.isEmpty())

        viewModel.deleteCongregation(berlin)
        assertEquals("not found", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `cancel editing clears editor and error`() {
        val viewModel = createViewModel(admin)
        viewModel.createCongregation()
        viewModel.saveCongregation()

        viewModel.cancelEditing()

        assertNull(viewModel.uiState.value.editor)
        assertNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.permissions.canEditSelection)
    }

    @Test
    fun `editor is dirty only after a change and clean again after saving`() {
        val viewModel = createViewModel(admin)

        viewModel.editCongregation(berlin)
        assertFalse(viewModel.uiState.value.isEditorDirty)

        viewModel.updateEditor(berlin.copy(name = "Berlin-Mitte"))
        assertTrue(viewModel.uiState.value.isEditorDirty)

        viewModel.saveCongregation()
        assertFalse(viewModel.uiState.value.isEditorDirty)
    }

    @Test
    fun `discard changes keeps the selected congregation but resets edits`() {
        val viewModel = createViewModel(admin)
        viewModel.editCongregation(berlin)
        viewModel.updateEditor(berlin.copy(name = "Berlin-Mitte"))

        viewModel.discardChanges()

        assertEquals(berlin, viewModel.uiState.value.editor)
        assertFalse(viewModel.uiState.value.isEditorDirty)
    }

    @Test
    fun `detail is open exactly while an editor exists`() {
        val viewModel = createViewModel(admin)
        assertFalse(viewModel.uiState.value.hasDetail)

        viewModel.editCongregation(berlin)
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
    fun `discard changes closes a new congregation`() {
        val viewModel = createViewModel(admin)
        viewModel.createCongregation()

        viewModel.discardChanges()

        assertNull(viewModel.uiState.value.editor)
    }

    private fun createViewModel(user: AppUser? = null) = CongregationMetadataViewModel(
        ObserveCongregationsUseCase(repository),
        SaveCongregationUseCase(repository),
        DeleteCongregationUseCase(repository),
        CongregationPermissionPolicy(),
    ).also { viewModel -> user?.let(viewModel::onCurrentUserChanged) }

    private class FakeCongregationRepository(congregations: List<CongregationMetadata>) :
        CongregationMetadataRepository {
        private val congregations = MutableStateFlow(congregations)
        var observeError: Throwable? = null
        var saveResult: Result<CongregationMetadata>? = null
        var deleteResult: Result<Unit> = Result.success(Unit)
        val saved = mutableListOf<CongregationMetadata>()
        val deletedIds = mutableListOf<String>()

        override fun observeAll(): Flow<List<CongregationMetadata>> = observeError?.let { error ->
            flow { throw error }
        } ?: congregations

        override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> {
            saved += congregation
            return saveResult ?: Result.success(congregation.copy(id = congregation.id.ifBlank { "new-id" }))
        }

        override suspend fun delete(congregationId: String): Result<Unit> {
            deletedIds += congregationId
            return deleteResult
        }
    }

    @Test
    fun `own only shows just the congregation of the user`() {
        val viewModel = createViewModel()
        val own = CongregationRef("r", "s", "b", "c", "609a")

        viewModel.onCurrentUserChanged(planer.copy(congregation = own), ownOnly = true)
        assertEquals(listOf(berlin), viewModel.uiState.value.congregations)

        viewModel.onCurrentUserChanged(planer.copy(congregation = own), ownOnly = false)
        assertEquals(listOf(berlin, hamburg), viewModel.uiState.value.congregations)

        viewModel.onCurrentUserChanged(planer, ownOnly = true)
        assertTrue(viewModel.uiState.value.congregations.isEmpty())
    }

    private companion object {
        const val MISSING_PERMISSION = "Dafür fehlt dir die Berechtigung."
    }
}
