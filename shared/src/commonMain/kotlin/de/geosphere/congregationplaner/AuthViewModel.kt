package de.geosphere.congregationplaner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.FirebaseSupport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AuthMode {
    LOGIN,
    REGISTER,
}

data class AuthUiState(
    val firebaseStatus: String = "Firebase wird initialisiert...",
    val isAuthenticated: Boolean = false,
    val authMode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val loginError: String? = null,
    val infoMessage: String? = null,
    val isLoading: Boolean = true,
)

class AuthViewModel(private val authRepository: FirebaseAuthRepository = FirebaseAuthManager) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                FirebaseSupport.initialize()
                _uiState.update {
                    it.copy(
                        isAuthenticated = authRepository.isSignedIn(),
                        firebaseStatus = if (FirebaseSupport.isReady()) {
                            "Firebase verfügbar"
                        } else {
                            "Firebase nicht konfiguriert"
                        },
                        isLoading = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        firebaseStatus = "Firebase nicht erreichbar",
                        loginError = SERVER_UNAVAILABLE_MESSAGE,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun toggleAuthMode() {
        _uiState.update {
            if (it.isLoading) {
                it
            } else {
                it.copy(
                    authMode = if (it.authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                    loginError = null,
                    infoMessage = null,
                )
            }
        }
    }

    fun authenticate() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            val request = _uiState.value
            _uiState.update { it.copy(isLoading = true, loginError = null, infoMessage = null) }
            try {
                val user = if (request.authMode == AuthMode.REGISTER) {
                    authRepository.createUserWithEmailAndPassword(request.email.trim(), request.password)
                } else {
                    authRepository.signInWithEmailAndPassword(request.email.trim(), request.password)
                }

                if (user == null) {
                    _uiState.update {
                        it.copy(
                            loginError = if (request.authMode == AuthMode.REGISTER) {
                                "Registrierung fehlgeschlagen. Bitte prüfe deine Eingaben."
                            } else {
                                "Login fehlgeschlagen. Bitte E-Mail und Passwort prüfen."
                            },
                        )
                    }
                } else if (request.authMode == AuthMode.REGISTER) {
                    _uiState.update {
                        it.copy(
                            authMode = AuthMode.LOGIN,
                            email = "",
                            password = "",
                            infoMessage = REGISTRATION_SUCCESS_MESSAGE,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isAuthenticated = true, email = "", password = "")
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(loginError = SERVER_UNAVAILABLE_MESSAGE) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signInWithGoogle() {
        if (_uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loginError = null) }
            try {
                if (authRepository.signInWithGoogle() != null) {
                    _uiState.update { it.copy(isAuthenticated = true, email = "", password = "") }
                } else {
                    _uiState.update {
                        it.copy(loginError = "Google-Anmeldung fehlgeschlagen oder wurde abgebrochen.")
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(loginError = SERVER_UNAVAILABLE_MESSAGE) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                _uiState.update {
                    it.copy(
                        isAuthenticated = false,
                        authMode = AuthMode.LOGIN,
                        loginError = null,
                        infoMessage = null,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(firebaseStatus = "Abmeldung fehlgeschlagen. Bitte erneut versuchen.") }
            }
        }
    }

    private companion object {
        const val REGISTRATION_SUCCESS_MESSAGE =
            "Registrierung erfolgreich. Bitte prüfe dein E-Mail-Postfach und bestätige deine E-Mail-Adresse."
        const val SERVER_UNAVAILABLE_MESSAGE =
            "Anmeldeserver nicht erreichbar. Bitte prüfe die Verbindung und versuche es erneut."
    }
}
