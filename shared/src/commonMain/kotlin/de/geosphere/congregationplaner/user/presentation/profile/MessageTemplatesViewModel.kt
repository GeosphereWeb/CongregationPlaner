package de.geosphere.congregationplaner.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.usecase.EditMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.MessageTemplateEdit
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveMessageTemplatesUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die eigenen WhatsApp-Vorlagen: [form] sind die Eingaben, [saved] der gespeicherte Stand; die erste ist Standard.
 * Was sich hinzufügen, entfernen und speichern lässt, entscheidet der [EditMessageTemplatesUseCase].
 */
data class MessageTemplatesUiState(
    val form: List<MessageTemplate> = emptyList(),
    val saved: List<MessageTemplate> = emptyList(),
    val isValid: Boolean = false,
    val canAddTemplate: Boolean = false,
    val canRemoveTemplate: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val isDirty: Boolean get() = form != saved
    val canSave: Boolean get() = isDirty && !isSaving && isValid
    val canAdd: Boolean get() = canAddTemplate && !isSaving
    val canRemove: Boolean get() = canRemoveTemplate && !isSaving
}

@Inject
class MessageTemplatesViewModel(
    private val observeTemplates: ObserveMessageTemplatesUseCase,
    private val saveTemplates: SaveMessageTemplatesUseCase,
    private val editTemplates: EditMessageTemplatesUseCase,
) : ViewModel() {
    private var user: AppUser? = null
    private var observeJob: Job? = null
    private val mutableUiState = MutableStateFlow(MessageTemplatesUiState())
    val uiState = mutableUiState.asStateFlow()

    /** Übernimmt den gespeicherten Stand; ungespeicherte Eingaben bleiben erhalten. */
    fun onUserChanged(newUser: AppUser) {
        if (user?.uid == newUser.uid && user?.congregation == newUser.congregation) {
            user = newUser
            return
        }
        user = newUser
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeTemplates(newUser).collect { saved ->
                mutableUiState.update { state ->
                    state.withForm(if (state.isDirty) state.form else saved).copy(saved = saved)
                }
            }
        }
    }

    fun edit(edit: MessageTemplateEdit) = mutableUiState.update { state ->
        if (state.isSaving) state else state.withForm(editTemplates(state.form, edit)).copy(message = null)
    }

    fun save() {
        val current = user ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            saveTemplates(current, state.form).fold(
                onSuccess = { saved ->
                    mutableUiState.update {
                        val done = it.copy(saved = saved, isSaving = false, message = "Gespeichert.", isError = false)
                        done.withForm(saved)
                    }
                },
                onFailure = { error ->
                    mutableUiState.update {
                        it.copy(
                            isSaving = false,
                            message = error.message ?: "Speichern fehlgeschlagen.",
                            isError = true,
                        )
                    }
                },
            )
        }
    }

    private fun MessageTemplatesUiState.withForm(form: List<MessageTemplate>) = copy(
        form = form,
        isValid = editTemplates.isValid(form),
        canAddTemplate = editTemplates.canAdd(form),
        canRemoveTemplate = editTemplates.canRemove(form),
    )
}
