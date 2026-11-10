package de.geosphere.congregationplaner.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.auth.domain.usecase.GetSignedInUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RefreshCurrentUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ResendVerificationEmailUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignOutUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Die Sitzung; [user] ist gesetzt, solange jemand angemeldet ist. */
data class SessionUiState(
    val firebaseStatus: String = "Firebase wird initialisiert...",
    val user: FirebaseUser? = null,
    val emailVerified: Boolean = false,
)

@Inject
class SessionViewModel(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val resendVerification: ResendVerificationEmailUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = mutableUiState.asStateFlow()

    init {
        restoreSession()
    }

    /** Nach dem Start von Firebase: Status anzeigen und eine bestehende Sitzung übernehmen. */
    fun onFirebaseInitialized(ready: Boolean) {
        mutableUiState.update {
            it.copy(firebaseStatus = if (ready) "Firebase verfügbar" else "Firebase nicht konfiguriert")
        }
        restoreSession()
    }

    fun onSignedIn(user: FirebaseUser) = mutableUiState.update {
        it.copy(user = user, emailVerified = user.emailVerified)
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            mutableUiState.update { SessionUiState(firebaseStatus = it.firebaseStatus) }
        }
    }

    /** Liest das Konto neu, damit eine inzwischen bestätigte E-Mail-Adresse erkannt wird. */
    fun refreshEmailVerification() {
        viewModelScope.launch {
            val user = refreshCurrentUser()
            mutableUiState.update { it.copy(user = user ?: it.user, emailVerified = user?.emailVerified == true) }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch { resendVerification() }
    }

    private fun restoreSession() {
        viewModelScope.launch {
            val user = getSignedInUser()
            mutableUiState.update { it.copy(user = user, emailVerified = user?.emailVerified == true) }
        }
    }
}
