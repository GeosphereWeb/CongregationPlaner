package de.geosphere.congregationplaner.user.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnProfileUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die eigenen Angaben unter „Einstellungen“: [form] sind die Eingaben, [saved] der gespeicherte Stand. */
data class OwnProfileUiState(
    val form: MemberProfile = MemberProfile(),
    val saved: MemberProfile = MemberProfile(),
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val isDirty: Boolean get() = form != saved
    val canSave: Boolean
        get() = isDirty && !isSaving && form.lastName.isNotBlank() && form.firstName.isNotBlank()
}

@Inject
class OwnProfileViewModel(private val updateOwnProfile: UpdateOwnProfileUseCase) : ViewModel() {
    private var user: AppUser? = null
    private val mutableUiState = MutableStateFlow(OwnProfileUiState())
    val uiState = mutableUiState.asStateFlow()

    /** Übernimmt den gespeicherten Stand; ungespeicherte eigene Eingaben gehen dabei nicht verloren. */
    fun onUserChanged(newUser: AppUser) {
        user = newUser
        val saved = newUser.profile
        mutableUiState.update { state ->
            val form = if (state.form == state.saved) saved else saved.withOwnFieldsOf(state.form)
            state.copy(form = form, saved = saved)
        }
    }

    fun onFormChange(form: MemberProfile) = mutableUiState.update { it.copy(form = form, message = null) }

    fun save() {
        val current = user ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            updateOwnProfile(current, state.form).fold(
                onSuccess = { saved ->
                    user = saved
                    mutableUiState.update {
                        it.copy(
                            form = saved.profile,
                            saved = saved.profile,
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
}
