package de.geosphere.congregationplaner

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
    suspend fun signOut()
    fun currentUserId(): String?
    fun isSignedIn(): Boolean
}

expect class FirebaseAuthPlatformService() : FirebaseAuthRepository

object FirebaseAuthManager {
    private val service: FirebaseAuthRepository by lazy { FirebaseAuthPlatformService() }

    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.Default) {
            service.signInWithEmailAndPassword(email, password)
        }

    suspend fun signOut() = withContext(Dispatchers.Default) { service.signOut() }

    fun currentUserId(): String? = service.currentUserId()

    fun isSignedIn(): Boolean = service.isSignedIn()
}
