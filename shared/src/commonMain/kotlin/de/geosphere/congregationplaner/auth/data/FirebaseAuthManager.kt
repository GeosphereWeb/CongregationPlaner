package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect fun createFirebaseAuthPlatformService(): FirebaseAuthRepository

internal var firebaseAuthPlatformServiceFactory: () -> FirebaseAuthRepository = { createFirebaseAuthPlatformService() }

object FirebaseAuthManager {
    private val service: FirebaseAuthRepository by lazy { firebaseAuthPlatformServiceFactory() }

    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.Default) {
            service.signInWithEmailAndPassword(email, password)
        }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null,
    ): FirebaseUser? = withContext(Dispatchers.Default) {
        service.createUserWithEmailAndPassword(email, password, displayName)
    }

    suspend fun signInWithGoogle(): FirebaseUser? = withContext(Dispatchers.Default) {
        service.signInWithGoogle()
    }

    fun isGoogleSignInAvailable(): Boolean = service.isGoogleSignInAvailable()

    suspend fun signOut() = withContext(Dispatchers.Default) { service.signOut() }

    fun currentUserId(): String? = service.currentUserId()

    fun currentUser(): FirebaseUser? = service.currentUser()

    fun currentUserIdToken(): String? = service.currentUserIdToken()

    suspend fun refreshCurrentUser(): FirebaseUser? = withContext(Dispatchers.Default) { service.refreshCurrentUser() }

    suspend fun resendVerificationEmail(): Boolean = withContext(Dispatchers.Default) {
        service.resendVerificationEmail()
    }

    fun isSignedIn(): Boolean = service.isSignedIn()
}
