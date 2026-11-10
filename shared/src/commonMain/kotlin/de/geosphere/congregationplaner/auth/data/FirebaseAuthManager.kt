package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect fun createFirebaseAuthPlatformService(): FirebaseAuthRepository

internal var firebaseAuthPlatformServiceFactory: () -> FirebaseAuthRepository = { createFirebaseAuthPlatformService() }

/** Führt die Aufrufe der Plattform-Implementierung außerhalb des Main-Threads aus. */
object FirebaseAuthManager : FirebaseAuthRepository {
    private val service: FirebaseAuthRepository by lazy { firebaseAuthPlatformServiceFactory() }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.Default) {
            service.signInWithEmailAndPassword(email, password)
        }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String?,
    ): FirebaseUser? = withContext(Dispatchers.Default) {
        service.createUserWithEmailAndPassword(email, password, displayName)
    }

    override suspend fun signInWithGoogle(): FirebaseUser? = withContext(Dispatchers.Default) {
        service.signInWithGoogle()
    }

    override fun isGoogleSignInAvailable(): Boolean = service.isGoogleSignInAvailable()

    override suspend fun signOut() = withContext(Dispatchers.Default) { service.signOut() }

    override fun currentUserId(): String? = service.currentUserId()

    override fun currentUser(): FirebaseUser? = service.currentUser()

    override fun currentUserIdToken(): String? = service.currentUserIdToken()

    override suspend fun refreshCurrentUser(): FirebaseUser? = withContext(Dispatchers.Default) {
        service.refreshCurrentUser()
    }

    override suspend fun resendVerificationEmail(): Boolean = withContext(Dispatchers.Default) {
        service.resendVerificationEmail()
    }

    override fun isSignedIn(): Boolean = service.isSignedIn()
}
