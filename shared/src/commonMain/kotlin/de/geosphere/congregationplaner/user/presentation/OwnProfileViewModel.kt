package de.geosphere.congregationplaner.user.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.usecase.UpdateOwnProfileUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die eigenen Angaben unter „Einstellungen“; [lastName] und [firstName] sind die Eingaben, [saved] der Stand. */
data class OwnProfileUiState(
    val lastName: String = "",
    val firstName: String = "",
    val saved: Pair<String, String> = "" to "",
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
) {
    val isDirty: Boolean get() = lastName to firstName != saved
    val canSave: Boolean get() = isDirty && !isSaving && lastName.isNotBlank() && firstName.isNotBlank()
}

@Inject
class OwnProfileViewModel(private val updateOwnProfile: UpdateOwnProfileUseCase) : ViewModel() {
    private var user: AppUser? = null
    private val mutableUiState = MutableStateFlow(OwnProfileUiState())
    val uiState = mutableUiState.asStateFlow()

    /** Übernimmt den gespeicherten Stand; ungespeicherte Eingaben gehen dabei nicht verloren. */
    fun onUserChanged(newUser: AppUser) {
        user = newUser
        val saved = newUser.profile.lastName to newUser.profile.firstName
        mutableUiState.update { state ->
            if (state.isDirty) {
                state.copy(saved = saved)
            } else {
                state.copy(lastName = saved.first, firstName = saved.second, saved = saved)
            }
        }
    }

    fun onLastNameChange(value: String) = mutableUiState.update { it.copy(lastName = value, message = null) }

    fun onFirstNameChange(value: String) = mutableUiState.update { it.copy(firstName = value, message = null) }

    fun save() {
        val current = user ?: return
        val state = mutableUiState.value
        if (!state.canSave) return
        mutableUiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            updateOwnProfile(current, state.lastName, state.firstName).fold(
                onSuccess = { saved ->
                    val names = saved.profile.lastName to saved.profile.firstName
                    mutableUiState.update {
                        it.copy(
                            lastName = names.first,
                            firstName = names.second,
                            saved = names,
                            isSaving = false,
                            message = "Gespeichert.",
                            isError = false,
                        )
                    }
                },
                onFailure = { error ->
                    val message = error.message ?: "Speichern fehlgeschlagen."
                    mutableUiState.update { it.copy(isSaving = false, message = message, isError = true) }
                },
            )
        }
    }
}
