package de.geosphere.congregationplaner.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.auth.domain.usecase.GetSignedInUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ObserveConnectivityUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RefreshCurrentUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ResendVerificationEmailUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignOutUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Die Sitzung; [user] ist gesetzt, solange jemand angemeldet ist. [globalLogin] merkt sich, dass er sich als globaler
 * Admin angemeldet hat.
 */
data class SessionUiState(
    val firebaseStatus: String = firebaseStatusText(ready = null, online = true),
    val user: FirebaseUser? = null,
    val emailVerified: Boolean = false,
    val globalLogin: Boolean = false,
)

@Inject
class SessionViewModel(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val resendVerification: ResendVerificationEmailUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = mutableUiState.asStateFlow()

    private var firebaseReady: Boolean? = null
    private var online = true

    init {
        restoreSession()
        viewModelScope.launch {
            observeConnectivity().collect {
                online = it
                showStatus()
            }
        }
    }

    /** Nach dem Start von Firebase: Status anzeigen und eine bestehende Sitzung übernehmen. */
    fun onFirebaseInitialized(ready: Boolean) {
        firebaseReady = ready
        showStatus()
        restoreSession()
    }

    private fun showStatus() {
        val status = firebaseStatusText(firebaseReady, online)
        mutableUiState.update { it.copy(firebaseStatus = status) }
    }

    // shortcut: globalLogin is not persisted; after an app restart the session falls back to the stored congregation.
    fun onSignedIn(user: FirebaseUser, globalLogin: Boolean = false) = mutableUiState.update {
        it.copy(user = user, emailVerified = user.emailVerified, globalLogin = globalLogin)
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
