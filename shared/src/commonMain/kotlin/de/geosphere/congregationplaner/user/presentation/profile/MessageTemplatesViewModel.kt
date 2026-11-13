package de.geosphere.congregationplaner.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.model.isValidTemplateList
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMessageTemplatesUseCase
import de.geosphere.congregationplaner.user.domain.usecase.SaveMessageTemplatesUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die eigenen WhatsApp-Vorlagen: [form] sind die Eingaben, [saved] der gespeicherte Stand; die erste ist Standard. */
data class MessageTemplatesUiState(
    val form: List<MessageTemplate> = emptyList(),
    val saved: List<MessageTemplate> = emptyList(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val isDirty: Boolean get() = form != saved
    val canSave: Boolean get() = isDirty && !isSaving && form.isValidTemplateList()
    val canAdd: Boolean get() = form.size < MessageTemplate.MAX_TEMPLATES && !isSaving
    val canRemove: Boolean get() = form.size > 1 && !isSaving
}

@Inject
class MessageTemplatesViewModel(
    private val observeTemplates: ObserveMessageTemplatesUseCase,
    private val saveTemplates: SaveMessageTemplatesUseCase,
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
                    state.copy(form = if (state.isDirty) state.form else saved, saved = saved)
                }
            }
        }
    }

    fun onTemplateChange(index: Int, template: MessageTemplate) = edit { form ->
        form.mapIndexed { i, current -> if (i == index) template.withinLimits() else current }
    }

    fun addTemplate() {
        if (!mutableUiState.value.canAdd) return
        edit { it + MessageTemplate("Vorlage ${it.size + 1}", "Hallo {Vorname}, ") }
    }

    fun removeTemplate(index: Int) {
        if (!mutableUiState.value.canRemove) return
        edit { form -> form.filterIndexed { i, _ -> i != index } }
    }

    /** Macht die Vorlage zum Standard, indem sie an die erste Stelle rückt. */
    fun makeDefault(index: Int) = edit { form ->
        form.getOrNull(index)?.let { listOf(it) + form.filterIndexed { i, _ -> i != index } } ?: form
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
                        it.copy(
                            form = saved,
                            saved = saved,
                            isSaving = false,
                            message = "Gespeichert.",
                            isError = false,
                        )
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

    private fun edit(change: (List<MessageTemplate>) -> List<MessageTemplate>) =
        mutableUiState.update { it.copy(form = change(it.form), message = null) }
}

private fun MessageTemplate.withinLimits() =
    MessageTemplate(name.take(MessageTemplate.MAX_NAME_LENGTH), text.take(MessageTemplate.MAX_TEXT_LENGTH))
