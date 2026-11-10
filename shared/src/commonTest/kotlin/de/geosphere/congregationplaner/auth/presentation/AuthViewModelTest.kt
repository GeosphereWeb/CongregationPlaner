package de.geosphere.congregationplaner.auth.presentation

import de.geosphere.congregationplaner.auth.domain.usecase.GetSignedInUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RefreshCurrentUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RegisterUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ResendVerificationEmailUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInWithGoogleUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignOutUseCase
import de.geosphere.congregationplaner.auth.testing.FakeAuthRepository
import de.geosphere.congregationplaner.auth.testing.FakeAuthUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var repository: FakeAuthRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeAuthRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AuthViewModel(
        GetSignedInUserUseCase(repository),
        SignInUseCase(repository),
        RegisterUseCase(repository),
        SignInWithGoogleUseCase(repository),
        SignOutUseCase(repository),
        RefreshCurrentUserUseCase(repository),
        ResendVerificationEmailUseCase(repository),
    )

    @Test
    fun `an existing session is restored and the firebase status shown`() {
        repository = FakeAuthRepository(FakeAuthUser(emailVerified = false))
        val viewModel = createViewModel()

        viewModel.onFirebaseInitialized(ready = true)

        val state = viewModel.uiState.value
        assertEquals("uid-1", state.user?.uid)
        assertFalse(state.emailVerified)
        assertEquals("Firebase verfügbar", state.firebaseStatus)
        viewModel.onFirebaseInitialized(ready = false)
        assertEquals("Firebase nicht konfiguriert", viewModel.uiState.value.firebaseStatus)
    }

    @Test
    fun `signing in clears the credentials and keeps the user`() {
        val viewModel = createViewModel()
        viewModel.onEmailChange(" max@example.com ")
        viewModel.onPasswordChange("pw")

        viewModel.submit()

        val state = viewModel.uiState.value
        assertNotNull(state.user)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isBusy)
        assertEquals(listOf("max@example.com" to "pw"), repository.signIns)
    }

    @Test
    fun `a failed login shows the message`() {
        repository.signInResult = null
        val viewModel = createViewModel()

        viewModel.submit()

        assertNull(viewModel.uiState.value.user)
        assertEquals("Login fehlgeschlagen. Bitte E-Mail und Passwort prüfen.", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `registration returns to the login with a hint and needs both names`() {
        val viewModel = createViewModel()
        viewModel.toggleMode()
        assertEquals(AuthMode.REGISTER, viewModel.uiState.value.mode)
        assertFalse(viewModel.uiState.value.hasRequiredNames)

        viewModel.onFirstNameChange("Max")
        viewModel.onLastNameChange("Muster")
        assertTrue(viewModel.uiState.value.hasRequiredNames)
        viewModel.submit()

        val state = viewModel.uiState.value
        assertEquals(AuthMode.LOGIN, state.mode)
        assertNull(state.user)
        assertEquals("", state.firstName)
        assertNotNull(state.infoMessage)
        assertEquals("Max Muster", repository.registrations.single().third)
    }

    @Test
    fun `toggling the mode clears messages`() {
        repository.signInResult = null
        val viewModel = createViewModel()
        viewModel.submit()

        viewModel.toggleMode()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `google sign in and global login`() {
        val viewModel = createViewModel()
        viewModel.signInWithGoogle()
        assertEquals("Google-Anmeldung fehlgeschlagen oder wurde abgebrochen.", viewModel.uiState.value.errorMessage)

        viewModel.startGlobalLogin()
        assertTrue(viewModel.uiState.value.globalLogin)
        viewModel.chooseCongregation()
        assertFalse(viewModel.uiState.value.globalLogin)

        repository.googleResult = FakeAuthUser(uid = "g")
        viewModel.signInWithGoogle()
        assertEquals("g", viewModel.uiState.value.user?.uid)
    }

    @Test
    fun `signing out resets everything but the firebase status`() {
        repository = FakeAuthRepository(FakeAuthUser())
        val viewModel = createViewModel()
        viewModel.onFirebaseInitialized(ready = true)
        viewModel.startGlobalLogin()

        viewModel.signOut()

        assertEquals(LoginUiState(firebaseStatus = "Firebase verfügbar"), viewModel.uiState.value)
        assertEquals(1, repository.signOutCount)
    }

    @Test
    fun `refresh detects a confirmed e-mail address and resend reaches the repository`() {
        repository = FakeAuthRepository(FakeAuthUser(emailVerified = false))
        val viewModel = createViewModel()
        repository.refreshedUser = FakeAuthUser(emailVerified = true)

        viewModel.refreshEmailVerification()
        viewModel.resendVerificationEmail()

        assertTrue(viewModel.uiState.value.emailVerified)
        assertEquals(true, viewModel.uiState.value.user?.emailVerified)
        assertEquals(1, repository.resendCount)
    }
}
