package de.geosphere.congregationplaner.congregation.presentation

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.usecase.DeleteCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.ObserveCongregationsUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.SaveCongregationUseCase
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
    fun `loads congregations`() {
        val state = createViewModel().uiState.value

        assertEquals(listOf(berlin, hamburg), state.congregations)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `shows error when congregations cannot be loaded`() {
        repository.observeError = IllegalStateException("offline")

        val state = createViewModel().uiState.value

        assertFalse(state.isLoading)
        assertEquals("offline", state.errorMessage)
    }

    @Test
    fun `creates and saves a new congregation`() {
        val viewModel = createViewModel()

        viewModel.createCongregation()
        assertEquals(CongregationMetadata(), viewModel.uiState.value.editor)

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
    fun `updates an existing congregation in place`() {
        val viewModel = createViewModel()

        viewModel.editCongregation(berlin)
        viewModel.updateEditor(berlin.copy(name = "Berlin-Mitte"))
        viewModel.saveCongregation()

        assertEquals(listOf(berlin.copy(name = "Berlin-Mitte"), hamburg), viewModel.uiState.value.congregations)
    }

    @Test
    fun `save without editor does nothing`() {
        val viewModel = createViewModel()

        viewModel.saveCongregation()

        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun `save failure is shown to the user`() {
        repository.saveResult = Result.failure(IllegalStateException("permission denied"))
        val viewModel = createViewModel()

        viewModel.editCongregation(berlin)
        viewModel.saveCongregation()

        assertEquals("permission denied", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `deletes congregation and clears selection`() {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.editCongregation(hamburg)

        viewModel.deleteCongregation(berlin)

        assertEquals(hamburg, viewModel.uiState.value.editor)
    }

    @Test
    fun `delete ignores unsaved congregations and reports failures`() {
        repository.deleteResult = Result.failure(IllegalStateException("not found"))
        val viewModel = createViewModel()

        viewModel.deleteCongregation(CongregationMetadata(name = "Neu"))
        assertTrue(repository.deletedIds.isEmpty())

        viewModel.deleteCongregation(berlin)
        assertEquals("not found", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `cancel editing clears editor and error`() {
        val viewModel = createViewModel()
        viewModel.createCongregation()
        viewModel.saveCongregation()

        viewModel.cancelEditing()

        assertNull(viewModel.uiState.value.editor)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    private fun createViewModel() = CongregationMetadataViewModel(
        ObserveCongregationsUseCase(repository),
        SaveCongregationUseCase(repository),
        DeleteCongregationUseCase(repository),
    )

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
}
