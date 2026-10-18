package de.geosphere.congregationplaner.congregation.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.usecase.DeleteCongregationUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.ObserveCongregationsUseCase
import de.geosphere.congregationplaner.congregation.domain.usecase.SaveCongregationUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CongregationMetadataUiState(
    val congregations: List<CongregationMetadata> = emptyList(),
    val editor: CongregationMetadata? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
)

@Inject
class CongregationMetadataViewModel(
    private val observeCongregations: ObserveCongregationsUseCase,
    private val saveCongregation: SaveCongregationUseCase,
    private val deleteCongregationUseCase: DeleteCongregationUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(CongregationMetadataUiState())
    val uiState = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeCongregations()
                .catch { exception ->
                    mutableUiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.message ?: "Versammlungen konnten nicht geladen werden.",
                        )
                    }
                }.collect { congregations ->
                    mutableUiState.update {
                        it.copy(congregations = congregations, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    fun createCongregation() {
        mutableUiState.update { it.copy(editor = CongregationMetadata(), errorMessage = null) }
    }

    fun editCongregation(congregation: CongregationMetadata) {
        mutableUiState.update { it.copy(editor = congregation, errorMessage = null) }
    }

    fun updateEditor(congregation: CongregationMetadata) {
        mutableUiState.update { it.copy(editor = congregation, errorMessage = null) }
    }

    fun cancelEditing() {
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    fun saveCongregation() {
        if (mutableUiState.value.isSaving || mutableUiState.value.isDeleting) return
        val congregation = mutableUiState.value.editor ?: return
        if (congregation.name.isBlank()) {
            mutableUiState.update { it.copy(errorMessage = "Bitte gib einen Versammlungsnamen ein.") }
            return
        }

        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveCongregation(congregation.copy(name = congregation.name.trim()))
                .onSuccess { savedCongregation ->
                    mutableUiState.update { state ->
                        val existingIndex = state.congregations.indexOfFirst { it.id == savedCongregation.id }
                        val congregations = if (existingIndex == -1) {
                            state.congregations + savedCongregation
                        } else {
                            state.congregations.map { current ->
                                if (current.id == savedCongregation.id) savedCongregation else current
                            }
                        }
                        state.copy(
                            congregations = congregations,
                            editor = savedCongregation,
                            isSaving = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = exception.message ?: "Versammlung konnte nicht gespeichert werden.",
                        )
                    }
                }
        }
    }

    fun deleteCongregation(congregation: CongregationMetadata) {
        if (congregation.id.isBlank() || mutableUiState.value.isDeleting || mutableUiState.value.isSaving) return

        mutableUiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            deleteCongregationUseCase(congregation.id)
                .onSuccess {
                    mutableUiState.update {
                        it.copy(
                            congregations = it.congregations.filterNot { item -> item.id == congregation.id },
                            editor = it.editor?.takeUnless { editor -> editor.id == congregation.id },
                            isDeleting = false,
                            errorMessage = null,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update {
                        it.copy(
                            isDeleting = false,
                            errorMessage = exception.message ?: "Versammlung konnte nicht gelöscht werden.",
                        )
                    }
                }
        }
    }
}
