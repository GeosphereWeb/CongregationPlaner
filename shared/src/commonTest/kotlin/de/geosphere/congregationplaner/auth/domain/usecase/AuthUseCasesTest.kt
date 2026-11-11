package de.geosphere.congregationplaner.auth.domain.usecase

import de.geosphere.congregationplaner.auth.domain.model.AuthException
import de.geosphere.congregationplaner.auth.domain.model.Registration
import de.geosphere.congregationplaner.auth.testing.FakeAuthRepository
import de.geosphere.congregationplaner.auth.testing.FakeAuthUser
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthUseCasesTest {
    private val repository = FakeAuthRepository()

    @Test
    fun `sign in trims the e-mail address and reports a failed login`() = runBlocking<Unit> {
        assertEquals("uid-1", SignInUseCase(repository)(" max@example.com ", " pw ").getOrThrow().uid)
        assertEquals(listOf("max@example.com" to " pw "), repository.signIns)

        repository.signInResult = null
        assertIs<AuthException.SignInFailed>(SignInUseCase(repository)("max@example.com", "pw").exceptionOrNull())
    }

    @Test
    fun `registration needs both names and stores them as display name`() = runBlocking<Unit> {
        val register = RegisterUseCase(repository)

        assertIs<AuthException.MissingName>(register(registration(firstName = " ")).exceptionOrNull())
        assertIs<AuthException.MissingName>(register(registration(lastName = "")).exceptionOrNull())
        assertTrue(repository.registrations.isEmpty())

        register(registration(email = " a@b.de ", firstName = " Max ", lastName = " Muster ")).getOrThrow()
        assertEquals(listOf(Triple<String, String, String?>("a@b.de", "pw", "Max Muster")), repository.registrations)

        repository.registerResult = null
        assertIs<AuthException.RegistrationFailed>(register(registration()).exceptionOrNull())
    }

    @Test
    fun `registration needs the same e-mail address and password twice`() = runBlocking<Unit> {
        val register = RegisterUseCase(repository)

        assertIs<AuthException.EmailMismatch>(register(registration(emailConfirmation = "a@c.de")).exceptionOrNull())
        assertIs<AuthException.PasswordMismatch>(
            register(registration(passwordConfirmation = "pW")).exceptionOrNull(),
        )
        assertIs<AuthException.PasswordMismatch>(
            register(registration(passwordConfirmation = "pw ")).exceptionOrNull(),
        )
        assertTrue(repository.registrations.isEmpty())

        // Blanks around and the case of the e-mail address do not matter.
        register(registration(email = "A@b.de", emailConfirmation = " a@B.de ")).getOrThrow()
        assertEquals("A@b.de", repository.registrations.single().first)
    }

    private fun registration(
        email: String = "a@b.de",
        emailConfirmation: String = email.trim(),
        passwordConfirmation: String = "pw",
        firstName: String = "Max",
        lastName: String = "Muster",
    ) = Registration(email, emailConfirmation, "pw", passwordConfirmation, firstName, lastName)

    @Test
    fun `google sign in reports a cancelled login`() = runBlocking<Unit> {
        assertIs<AuthException.GoogleSignInFailed>(SignInWithGoogleUseCase(repository)().exceptionOrNull())

        repository.googleResult = FakeAuthUser(uid = "g")
        assertEquals("g", SignInWithGoogleUseCase(repository)().getOrThrow().uid)
    }

    @Test
    fun `a session without user is ended`() = runBlocking<Unit> {
        assertNull(GetSignedInUserUseCase(repository)())
        assertEquals(0, repository.signOutCount)

        repository.signedIn = true
        assertNull(GetSignedInUserUseCase(repository)())
        assertEquals(1, repository.signOutCount)

        val user = FakeAuthUser()
        assertEquals(user, GetSignedInUserUseCase(FakeAuthRepository(user))())
    }

    @Test
    fun `refresh resend and sign out delegate to the repository`() = runBlocking<Unit> {
        repository.refreshedUser = FakeAuthUser(emailVerified = true)

        assertEquals(repository.refreshedUser, RefreshCurrentUserUseCase(repository)())
        assertTrue(ResendVerificationEmailUseCase(repository)())
        SignOutUseCase(repository)()

        assertEquals(1, repository.resendCount)
        assertEquals(1, repository.signOutCount)
    }
}
