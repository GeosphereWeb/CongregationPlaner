package de.geosphere.congregationplaner.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.auth.domain.usecase.RegisterUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInWithGoogleUseCase
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

data class LoginForm(
    val email: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
)

/** Eingaben und Meldungen der Anmeldung; [signedInUser] meldet eine geglückte Anmeldung an die Sitzung. */
data class LoginUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    /** Globale Admins melden sich ohne Versammlung an (ADR-0013). */
    val globalLogin: Boolean = false,
    val form: LoginForm = LoginForm(),
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val isBusy: Boolean = false,
    val signedInUser: FirebaseUser? = null,
) {
    val isLogin: Boolean get() = mode == AuthMode.LOGIN

    /** Zum Registrieren braucht es Vor- und Nachnamen. */
    val hasRequiredNames: Boolean get() = isLogin || (form.firstName.isNotBlank() && form.lastName.isNotBlank())
}

@Inject
class LoginViewModel(
    private val signIn: SignInUseCase,
    private val register: RegisterUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = mutableUiState.asStateFlow()

    fun onFormChange(form: LoginForm) = mutableUiState.update { it.copy(form = form) }

    fun toggleMode() = mutableUiState.update {
        val mode = if (it.isLogin) AuthMode.REGISTER else AuthMode.LOGIN
        it.copy(mode = mode, errorMessage = null, infoMessage = null)
    }

    fun setGlobalLogin(global: Boolean) = mutableUiState.update { it.copy(globalLogin = global) }

    /** Meldet an oder legt das Konto an, je nach [LoginUiState.mode]. */
    fun submit() {
        val state = startAction() ?: return
        val form = state.form
        viewModelScope.launch {
            if (state.isLogin) {
                signIn(form.email, form.password).fold(::onSignedIn, ::onFailure)
            } else {
                register(form.email, form.password, form.firstName, form.lastName).fold({ onRegistered() }, ::onFailure)
            }
        }
    }

    fun signInWithGoogle() {
        startAction() ?: return
        viewModelScope.launch { signInWithGoogleUseCase().fold(::onSignedIn, ::onFailure) }
    }

    /** Die Sitzung hat den Nutzer übernommen; die Anmeldung beginnt beim nächsten Mal von vorn. */
    fun onSignInHandled() = mutableUiState.update { LoginUiState() }

    private fun startAction(): LoginUiState? {
        val state = mutableUiState.value
        if (state.isBusy) return null
        mutableUiState.update { it.copy(isBusy = true, errorMessage = null) }
        return state
    }

    private fun onSignedIn(user: FirebaseUser) = mutableUiState.update {
        it.copy(signedInUser = user, form = LoginForm(), isBusy = false)
    }

    // Angemeldet wird erst, nachdem die E-Mail-Adresse bestätigt ist.
    private fun onRegistered() = mutableUiState.update {
        it.copy(mode = AuthMode.LOGIN, form = LoginForm(), infoMessage = REGISTERED_MESSAGE, isBusy = false)
    }

    private fun onFailure(exception: Throwable) = mutableUiState.update {
        it.copy(isBusy = false, errorMessage = exception.message ?: "Anmeldung fehlgeschlagen.")
    }

    private companion object {
        const val REGISTERED_MESSAGE =
            "Registrierung erfolgreich. Bitte prüfe dein E-Mail-Postfach und bestätige deine E-Mail-Adresse."
    }
}
