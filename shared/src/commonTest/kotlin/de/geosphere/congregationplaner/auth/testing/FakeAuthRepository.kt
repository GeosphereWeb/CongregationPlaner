package de.geosphere.congregationplaner.auth.testing

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser

internal data class FakeAuthUser(
    override val uid: String = "uid-1",
    override val email: String? = "max@example.com",
    override val displayName: String? = "Max Muster",
    override val idToken: String? = null,
    override val emailVerified: Boolean = true,
) : FirebaseUser

/** Hält die Sitzung im Speicher; `null`-Ergebnisse stehen wie bei Firebase für einen Fehlschlag. */
internal class FakeAuthRepository(var user: FirebaseUser? = null) : FirebaseAuthRepository {
    var signInResult: FirebaseUser? = FakeAuthUser()
    var registerResult: FirebaseUser? = FakeAuthUser(emailVerified = false)
    var googleResult: FirebaseUser? = null
    var refreshedUser: FirebaseUser? = null
    var signedIn: Boolean = user != null
    val signIns = mutableListOf<Pair<String, String>>()
    val registrations = mutableListOf<Triple<String, String, String?>>()
    var signOutCount = 0
    var resendCount = 0

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        signIns += email to password
        return signInResult?.also(::startSession)
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String?,
    ): FirebaseUser? {
        registrations += Triple(email, password, displayName)
        return registerResult
    }

    override suspend fun signInWithGoogle(): FirebaseUser? = googleResult?.also(::startSession)

    override suspend fun signOut() {
        signOutCount++
        user = null
        signedIn = false
    }

    override fun currentUserId(): String? = user?.uid

    override fun currentUser(): FirebaseUser? = user

    override suspend fun refreshCurrentUser(): FirebaseUser? = refreshedUser?.also { user = it }

    override suspend fun resendVerificationEmail(): Boolean {
        resendCount++
        return true
    }

    override fun isSignedIn(): Boolean = signedIn

    private fun startSession(signedInUser: FirebaseUser) {
        user = signedInUser
        signedIn = true
    }
}
