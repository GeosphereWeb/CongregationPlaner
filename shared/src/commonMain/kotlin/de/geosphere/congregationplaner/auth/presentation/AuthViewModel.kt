package de.geosphere.congregationplaner.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.auth.domain.usecase.GetSignedInUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RefreshCurrentUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RegisterUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ResendVerificationEmailUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInWithGoogleUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignOutUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

/** Sitzung und Eingaben der Anmeldung; [user] ist gesetzt, solange jemand angemeldet ist. */
data class LoginUiState(
    val firebaseStatus: String = "Firebase wird initialisiert...",
    val user: FirebaseUser? = null,
    val emailVerified: Boolean = false,
    val mode: AuthMode = AuthMode.LOGIN,
    /** Globale Admins melden sich ohne Versammlung an (ADR-0013). */
    val globalLogin: Boolean = false,
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isBusy: Boolean = false,
) {
    val isLogin: Boolean get() = mode == AuthMode.LOGIN

    /** Zum Registrieren braucht es Vor- und Nachnamen. */
    val hasRequiredNames: Boolean get() = isLogin || (firstName.isNotBlank() && lastName.isNotBlank())
}

@Inject
class AuthViewModel(
    private val getSignedInUser: GetSignedInUserUseCase,
    private val signIn: SignInUseCase,
    private val register: RegisterUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
    private val resendVerification: ResendVerificationEmailUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

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

    fun onEmailChange(email: String) = mutableUiState.update { it.copy(email = email) }

    fun onPasswordChange(password: String) = mutableUiState.update { it.copy(password = password) }

    fun onFirstNameChange(firstName: String) = mutableUiState.update { it.copy(firstName = firstName) }

    fun onLastNameChange(lastName: String) = mutableUiState.update { it.copy(lastName = lastName) }

    fun toggleMode() = mutableUiState.update {
        val mode = if (it.isLogin) AuthMode.REGISTER else AuthMode.LOGIN
        it.copy(mode = mode, errorMessage = null, infoMessage = null)
    }

    fun startGlobalLogin() = mutableUiState.update { it.copy(globalLogin = true) }

    fun chooseCongregation() = mutableUiState.update { it.copy(globalLogin = false) }

    /** Meldet an oder legt das Konto an, je nach [LoginUiState.mode]. */
    fun submit() {
        val state = startAction() ?: return
        viewModelScope.launch {
            if (state.isLogin) {
                signIn(state.email, state.password).fold(::onSignedIn, ::onFailure)
            } else {
                register(state.email, state.password, state.firstName, state.lastName).fold(
                    onSuccess = {
                        // Angemeldet wird erst, nachdem die E-Mail-Adresse bestätigt ist.
                        mutableUiState.update {
                            it.copy(
                                mode = AuthMode.LOGIN,
                                email = "",
                                password = "",
                                firstName = "",
                                lastName = "",
                                infoMessage = REGISTERED_MESSAGE,
                                isBusy = false,
                            )
                        }
                    },
                    onFailure = ::onFailure,
                )
            }
        }
    }

    fun signInWithGoogle() {
        startAction() ?: return
        viewModelScope.launch { signInWithGoogleUseCase().fold(::onSignedIn, ::onFailure) }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            mutableUiState.update { LoginUiState(firebaseStatus = it.firebaseStatus) }
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

    private fun startAction(): LoginUiState? {
        val state = mutableUiState.value
        if (state.isBusy) return null
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
        return state
    }

    private fun onSignedIn(user: FirebaseUser) = mutableUiState.update {
        it.copy(user = user, emailVerified = user.emailVerified, email = "", password = "", isBusy = false)
    }

    private fun onFailure(exception: Throwable) = mutableUiState.update {
        it.copy(isBusy = false, errorMessage = exception.message ?: "Anmeldung fehlgeschlagen.")
    }

    private companion object {
        const val REGISTERED_MESSAGE =
            "Registrierung erfolgreich. Bitte prüfe dein E-Mail-Postfach und bestätige deine E-Mail-Adresse."
    }
}
