package de.geosphere.congregationplaner.auth.presentation

import de.geosphere.congregationplaner.auth.domain.usecase.RegisterUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.SignInWithGoogleUseCase
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
class LoginViewModelTest {
    private val repository = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(
            SignInUseCase(repository),
            RegisterUseCase(repository),
            SignInWithGoogleUseCase(repository),
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun `signing in clears the credentials and reports the user`() {
        viewModel.onFormChange(LoginForm(email = " max@example.com ", password = "pw"))

        viewModel.submit()

        assertNotNull(state.signedInUser)
        assertEquals(LoginForm(), state.form)
        assertFalse(state.isBusy)
        assertEquals(listOf("max@example.com" to "pw"), repository.signIns)
    }

    @Test
    fun `a handled sign in starts the login from scratch`() {
        viewModel.setGlobalLogin(true)
        viewModel.submit()

        viewModel.onSignInHandled()

        assertEquals(LoginUiState(), state)
    }

    @Test
    fun `a failed login shows the message`() {
        repository.signInResult = null

        viewModel.submit()

        assertNull(state.signedInUser)
        assertEquals("Login fehlgeschlagen. Bitte E-Mail und Passwort prüfen.", state.errorMessage)
    }

    @Test
    fun `registration returns to the login with a hint and needs both names`() {
        viewModel.toggleMode()
        assertEquals(AuthMode.REGISTER, state.mode)
        assertFalse(state.hasRequiredNames)

        viewModel.onFormChange(LoginForm(firstName = "Max", lastName = "Muster"))
        assertTrue(state.hasRequiredNames)
        viewModel.submit()

        assertEquals(AuthMode.LOGIN, state.mode)
        assertNull(state.signedInUser)
        assertEquals(LoginForm(), state.form)
        assertNotNull(state.infoMessage)
        assertEquals("Max Muster", repository.registrations.single().third)
    }

    @Test
    fun `toggling the mode clears messages`() {
        repository.signInResult = null
        viewModel.submit()

        viewModel.toggleMode()

        assertNull(state.errorMessage)
    }

    @Test
    fun `google sign in and global login`() {
        viewModel.signInWithGoogle()
        assertEquals("Google-Anmeldung fehlgeschlagen oder wurde abgebrochen.", state.errorMessage)

        viewModel.setGlobalLogin(true)
        assertTrue(state.globalLogin)
        viewModel.setGlobalLogin(false)
        assertFalse(state.globalLogin)

        repository.googleResult = FakeAuthUser(uid = "g")
        viewModel.signInWithGoogle()
        assertEquals("g", state.signedInUser?.uid)
    }
}
