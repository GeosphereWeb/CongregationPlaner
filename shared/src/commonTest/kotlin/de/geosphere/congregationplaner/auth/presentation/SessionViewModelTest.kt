package de.geosphere.congregationplaner.auth.presentation

import de.geosphere.congregationplaner.auth.domain.usecase.GetSignedInUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.RefreshCurrentUserUseCase
import de.geosphere.congregationplaner.auth.domain.usecase.ResendVerificationEmailUseCase
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
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

    private fun createViewModel() = SessionViewModel(
        GetSignedInUserUseCase(repository),
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
    fun `a signed in user starts the session`() {
        val viewModel = createViewModel()
        assertNull(viewModel.uiState.value.user)

        viewModel.onSignedIn(FakeAuthUser(emailVerified = true))

        assertEquals("uid-1", viewModel.uiState.value.user?.uid)
        assertTrue(viewModel.uiState.value.emailVerified)
        assertEquals(false, viewModel.uiState.value.globalLogin)

        viewModel.onSignedIn(FakeAuthUser(), globalLogin = true)
        assertTrue(viewModel.uiState.value.globalLogin)
    }

    @Test
    fun `signing out keeps only the firebase status`() {
        repository = FakeAuthRepository(FakeAuthUser())
        val viewModel = createViewModel()
        viewModel.onFirebaseInitialized(ready = true)

        viewModel.signOut()

        assertEquals(SessionUiState(firebaseStatus = "Firebase verfügbar"), viewModel.uiState.value)
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
