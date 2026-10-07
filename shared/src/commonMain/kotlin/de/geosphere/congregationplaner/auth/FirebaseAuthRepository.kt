package de.geosphere.congregationplaner.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface FirebaseUser {
    val uid: String
    val email: String?
    val displayName: String?
    val idToken: String?
}

interface FirebaseAuthRepository {
    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser?
    suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser?
    suspend fun signInWithGoogle(): FirebaseUser? = null
    fun isGoogleSignInAvailable(): Boolean = false
    suspend fun signOut()
    fun currentUserId(): String?
    fun isSignedIn(): Boolean
}

expect fun createFirebaseAuthPlatformService(): FirebaseAuthRepository

internal var firebaseAuthPlatformServiceFactory: () -> FirebaseAuthRepository = { createFirebaseAuthPlatformService() }

object FirebaseAuthManager : FirebaseAuthRepository {
    private val service: FirebaseAuthRepository by lazy { firebaseAuthPlatformServiceFactory() }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.Default) {
            service.signInWithEmailAndPassword(email, password)
        }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.Default) {
            service.createUserWithEmailAndPassword(email, password)
        }

    override suspend fun signInWithGoogle(): FirebaseUser? = withContext(Dispatchers.Default) {
        service.signInWithGoogle()
    }

    override fun isGoogleSignInAvailable(): Boolean = service.isGoogleSignInAvailable()

    override suspend fun signOut() = withContext(Dispatchers.Default) { service.signOut() }

    override fun currentUserId(): String? = service.currentUserId()

    override fun isSignedIn(): Boolean = service.isSignedIn()
}
