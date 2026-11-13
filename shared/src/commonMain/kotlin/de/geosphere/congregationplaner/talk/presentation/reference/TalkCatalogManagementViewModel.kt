package de.geosphere.congregationplaner.talk.presentation.reference

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.presentation.components.ListDetailState
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import de.geosphere.congregationplaner.talk.domain.permission.TalkReferenceDataPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.EditTalkTitleChangesUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkCatalogUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.SaveTalkUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.TalkTitleChangeEdit
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

/**
 * Der gerade bearbeitete Vortrag. Die Nummer ist die Dokument-ID und nur bei einem neuen Vortrag änderbar;
 * [original] ist `null`, solange ein neuer Vortrag entsteht.
 */
data class TalkEditorState(
    val numberText: String,
    val title: String,
    val active: Boolean,
    val original: Talk? = null,
    /** Spätere Titel mit dem Tag, ab dem sie gelten; [title] ist der ursprüngliche Titel. */
    val titleChanges: List<TalkTitleChange> = original?.titleChanges.orEmpty(),
    /** Ob sich noch eine Änderung hinzufügen lässt; entscheidet der [EditTalkTitleChangesUseCase]. */
    val canAddTitleChange: Boolean = false,
) {
    val isNew: Boolean get() = original == null
    val talk: Talk? get() = numberText.toIntOrNull()?.let { Talk(it, title, active, titleChanges) }
    val isDirty: Boolean get() = isNew || talk != original
}

/** Ändert die Titelhistorie des offenen Vortrags über den [EditTalkTitleChangesUseCase] (ADR-0025). */
class TalkTitleChangesHolder(
    private val state: MutableStateFlow<TalkCatalogUiState>,
    private val editTitleChanges: EditTalkTitleChangesUseCase,
) {
    fun edit(edit: TalkTitleChangeEdit) = state.update { current ->
        val editor = current.editor
        if (current.isSaving || editor == null) {
            current
        } else {
            val changes = editTitleChanges(Talk(0, editor.title, editor.active, editor.titleChanges), edit)
            current.copy(editor = withFlags(editor.copy(titleChanges = changes)))
        }
    }

    /** Der Editor mit den Angaben, die der UseCase über ihn entscheidet. */
    fun withFlags(editor: TalkEditorState): TalkEditorState =
        editor.copy(canAddTitleChange = editTitleChanges.canAdd(editor.titleChanges))
}

data class TalkCatalogUiState(
    val talks: List<Talk> = emptyList(),
    /** Heute; die Liste zeigt den Titel, der heute gilt. */
    val today: LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
    val isLoading: Boolean = true,
    val canManage: Boolean = false,
    val editor: TalkEditorState? = null,
    override val errorMessage: String? = null,
    override val isSaving: Boolean = false,
) : ListDetailState {
    override val hasDetail: Boolean get() = editor != null
}

/** Pflege der globalen Vortragsliste durch globale Administratoren. */
@Inject
class TalkCatalogManagementViewModel(
    private val observeCatalog: ObserveTalkCatalogUseCase,
    private val saveTalk: SaveTalkUseCase,
    private val deleteTalk: DeleteTalkUseCase,
    private val policy: TalkReferenceDataPolicy,
    editTitleChanges: EditTalkTitleChangesUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(TalkCatalogUiState())
    val uiState = mutableUiState.asStateFlow()

    val titleChanges = TalkTitleChangesHolder(mutableUiState, editTitleChanges)

    private var currentUser: AppUser? = null
    private var observeJob: Job? = null

    fun onCurrentUserChanged(user: AppUser) {
        currentUser = user
        val canManage = policy.canManage(user)
        mutableUiState.update { it.copy(canManage = canManage, editor = it.editor?.takeIf { canManage }) }
        if (canManage) {
            observe()
        } else {
            observeJob?.cancel()
            observeJob = null
            mutableUiState.update { it.copy(talks = emptyList(), isLoading = false) }
        }
    }

    fun newTalk() {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val editor = titleChanges.withFlags(TalkEditorState("", "", active = true))
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    fun editTalk(talk: Talk) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val editor = titleChanges.withFlags(TalkEditorState(talk.number.toString(), talk.title, talk.active, talk))
        mutableUiState.update { it.copy(editor = editor, errorMessage = null) }
    }

    /** Die Nummer besteht aus höchstens vier Ziffern und bleibt bei einem vorhandenen Vortrag unverändert. */
    fun updateEditor(editor: TalkEditorState) {
        mutableUiState.update { state ->
            if (state.isSaving) return@update state
            val number = if (editor.isNew) editor.numberText.filter(Char::isDigit).take(NUMBER_DIGITS) else null
            val next = editor.copy(numberText = number ?: editor.original?.number.toString())
            state.copy(editor = titleChanges.withFlags(next))
        }
    }

    fun cancelEditing() {
        if (mutableUiState.value.isSaving) return
        mutableUiState.update { it.copy(editor = null, errorMessage = null) }
    }

    fun save() {
        val state = mutableUiState.value
        val editor = state.editor ?: return
        if (state.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        val talk = editor.talk
        val error = when {
            talk == null -> "Bitte gib eine Vortragsnummer ein."
            editor.isNew && state.talks.any { it.number == talk.number } -> "Die Nummer ${talk.number} gibt es schon."
            else -> null
        }
        if (talk == null || error != null) {
            mutableUiState.update { it.copy(errorMessage = error) }
            return
        }
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            saveTalk(user, talk)
                .onSuccess { saved ->
                    val next = titleChanges.withFlags(
                        TalkEditorState(saved.number.toString(), saved.title, saved.active, saved),
                    )
                    // Show the result at once; the snapshot flow can lag behind, especially on desktop.
                    mutableUiState.update {
                        val talks = (it.talks.filterNot { t -> t.number == saved.number } + saved)
                            .sortedBy { t -> t.number }
                        it.copy(talks = talks, editor = next, isSaving = false)
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Vortrag konnte nicht gespeichert werden.") }
                }
        }
    }

    fun delete(talk: Talk) {
        if (mutableUiState.value.isSaving || !checkAllowed()) return
        val user = currentUser ?: return
        mutableUiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            deleteTalk(user, talk.number)
                .onSuccess {
                    mutableUiState.update { state ->
                        state.copy(
                            talks = state.talks.filterNot { it.number == talk.number },
                            editor = state.editor?.takeUnless { it.original?.number == talk.number },
                            isSaving = false,
                        )
                    }
                }.onFailure { exception ->
                    mutableUiState.update { it.failed(exception, "Vortrag konnte nicht gelöscht werden.") }
                }
        }
    }

    /** Schließt die Fehlermeldung; war es ein Ladefehler, wird dabei erneut geladen. */
    fun dismissError() {
        mutableUiState.update { it.copy(errorMessage = null) }
        if (currentUser?.let(policy::canManage) == true) observe()
    }

    private fun observe() {
        if (observeJob?.isActive == true) return
        mutableUiState.update { it.copy(isLoading = true) }
        observeJob = viewModelScope.launch {
            observeCatalog()
                .catch { exception ->
                    val message = exception.message ?: "Vortragsliste konnte nicht geladen werden."
                    mutableUiState.update { it.copy(isLoading = false, errorMessage = message) }
                }.collect { talks ->
                    mutableUiState.update { it.copy(talks = talks, isLoading = false) }
                }
        }
    }

    private fun checkAllowed(): Boolean {
        val allowed = currentUser?.let(policy::canManage) == true
        if (!allowed) mutableUiState.update { it.copy(errorMessage = MISSING_PERMISSION_MESSAGE) }
        return allowed
    }

    private companion object {
        const val NUMBER_DIGITS = 4
        const val MISSING_PERMISSION_MESSAGE = "Dafür fehlt dir die Berechtigung."
    }
}

private fun TalkCatalogUiState.failed(exception: Throwable, fallback: String) =
    copy(isSaving = false, errorMessage = exception.message ?: fallback)
