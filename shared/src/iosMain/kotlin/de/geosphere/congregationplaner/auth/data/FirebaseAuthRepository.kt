package de.geosphere.congregationplaner.auth.data

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser

actual fun createFirebaseAuthPlatformService(): FirebaseAuthRepository = FirebaseAuthPlatformService()

class FirebaseAuthPlatformService : FirebaseAuthRepository {
    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        // iOS-Implementierung fehlt noch — Platzhalter (keine Exception)
        return null
    }

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String?,
    ): FirebaseUser? {
        // Platzhalter
        return null
    }

    override suspend fun signOut() {
        // Platzhalter: nichts zu tun
    }

    override fun currentUserId(): String? {
        // Platzhalter
        return null
    }

    override fun isSignedIn(): Boolean {
        // Platzhalter
        return false
    }
}
