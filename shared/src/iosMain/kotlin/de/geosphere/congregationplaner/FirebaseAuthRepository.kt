package de.geosphere.congregationplaner

actual class FirebaseAuthPlatformService actual constructor() : FirebaseAuthRepository {
    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        // iOS-Implementierung fehlt noch — Platzhalter (keine Exception)
        return null
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? {
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
