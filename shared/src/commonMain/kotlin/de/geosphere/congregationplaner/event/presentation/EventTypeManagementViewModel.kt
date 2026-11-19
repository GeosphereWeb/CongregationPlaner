package de.geosphere.congregationplaner.event.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.usecase.AdoptEventTypeBlueprintUseCase
import de.geosphere.congregationplaner.event.domain.usecase.DeleteEventTypeUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.SaveEventTypeUseCase
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die gerade bearbeitete Ereignisart; [original] ist `null`, solange eine neue entsteht. */
data class EventTypeEditorState(val type: EventType, val original: EventType? = null) {
    val isNew: Boolean get() = original == null
    val isDirty: Boolean get() = isNew || type != original
}

data class EventTypeUiState(
    val types: List<EventType> = emptyList(),
    val isLoading: Boolean = true,
    val canManage: Boolean = false,
    val editor: EventTypeEditorState? = null,
    /** Die Art, deren Löschen gerade bestätigt werden soll. */
    val pendingDelete: EventType? = null,
    val infoMessage: String? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** Hinweis vor dem Löschen: Verweise bleiben, verlieren aber ihr Icon (ADR-0036). */
internal fun deleteEventTypeHint(type: EventType): String =
    "„${type.name}“ wird gelöscht. Termine und Ereignisse, die diese Art nutzen, behalten ihren Namen, " +
        "zeigen aber das Standard-Icon."

/** Rückmeldung nach „Vorlage übernehmen“. */
internal fun blueprintMessage(added: Int): String = when (added) {
    0 -> "Alle Arten der Vorlage sind schon vorhanden."
    1 -> "1 Art aus der Vorlage ergänzt."
    else -> "$added Arten aus der Vorlage ergänzt."
}

/** Pflege der Ereignisarten durch den Versammlungsadmin (ADR-0036). */
@Inject
class EventTypeManagementViewModel(
    private val observeTypes: ObserveEventTypesUseCase,
    private val saveType: SaveEventTypeUseCase,
    private val deleteType: DeleteEventTypeUseCase,
    private val adopt: AdoptEventTypeBlueprintUseCase,
    private val policy: EventTypePolicy,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(EventTypeUiState())
    val uiState = mutableUiState.asStateFlow()

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        val changed = user.uid != currentUser?.uid || user.congregation != currentUser?.congregation
        currentUser = user
        val canManage = policy.canManage(user)
        mutableUiState.update { it.copy(canManage = canManage, editor = it.editor?.takeIf { canManage }) }
        if (changed || observeJob == null) observe(user)
    }

    /** Öffnet [type] im Editor; `null` beginnt eine neue Art. */
    fun editType(type: EventType?) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val editor = EventTypeEditorState(type ?: EventType(), type)
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    /** Übernimmt die Eingaben; `null` schließt den Editor und seine Fehlermeldung. */
    fun updateEditor(editor: EventTypeEditorState?) {
        mutableUiState.update {
            when {
                it.isSaving -> it
                editor == null -> it.copy(editor = null, errorMessage = null)
                else -> it.copy(editor = editor)
            }
        }
    }

    fun save() {
        val editor = mutableUiState.value.editor?.takeUnless { mutableUiState.value.isSaving } ?: return
        val user = currentUser ?: return
        if (!checkAllowed()) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveType(user, editor.type)
                .onSuccess { saved ->
                    // Show the result at once; the snapshot flow can lag behind, especially on desktop.
                    mutableUiState.update {
                        val types = (
                            it.types.filterNot { t ->
                                t.id == saved.id
                            } + saved
                            ).sortedBy { t -> t.name.lowercase() }
                        it.copy(types = types, editor = EventTypeEditorState(saved, saved), isSaving = false)
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Ereignisart konnte nicht gespeichert werden.") }
                }
        }
    }

    /** Fragt vor dem Löschen von [type] nach; `null` schließt die Rückfrage. */
    fun requestDelete(type: EventType?) {
        if (type != null && (mutableUiState.value.isSaving || !checkAllowed())) return
        mutableUiState.update { it.copy(pendingDelete = type, errorMessage = null) }
    }

    fun confirmDelete() {
        val type = mutableUiState.value.pendingDelete ?: return
        val user = currentUser ?: return
        mutableUiState.update { it.copy(pendingDelete = null, isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteType(user, type.id)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            types = state.types.filterNot { it.id == type.id },
                            editor = state.editor?.takeUnless { it.original?.id == type.id },
                            isSaving = false,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Ereignisart konnte nicht gelöscht werden.") }
                }
        }
    }

    fun adoptBlueprint() {
        val user = currentUser ?: return
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null, infoMessage = null) }
        viewModelScope.launch {
            adopt(user)
                .onSuccess { added ->
                    mutableUiState.update { it.copy(isSaving = false, infoMessage = blueprintMessage(added)) }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Die Vorlage konnte nicht übernommen werden.") }
                }
        }
    }

    /** Schließt Hinweis und Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissMessages() {
        mutableUiState.update { it.copy(errorMessage = null, infoMessage = null) }
        if (observeJob?.isActive != true) currentUser?.let(::observe)
    }

    private fun observe(user: AppUser) {
        observeJob?.cancel()
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeTypes(user)
                .catch { exception ->
                    val message = exception.message ?: "Ereignisarten konnten nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { types ->
                    mutableUiState.update { it.copy(types = types, isLoading = false) }
                }
        }
    }

    private fun checkAllowed(): Boolean {
        val allowed = currentUser?.let(policy::canManage) == true
        if (!allowed) mutableUiState.update { it.copy(errorMessage = "Dafür fehlt dir die Berechtigung.") }
        return allowed
    }
}

private fun EventTypeUiState.failed(exception: Throwable, fallback: String) =
    copy(isSaving = false, errorMessage = exception.message ?: fallback)
