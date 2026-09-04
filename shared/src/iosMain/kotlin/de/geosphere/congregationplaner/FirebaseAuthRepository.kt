package de.geosphere.congregationplaner

actual class FirebaseAuthPlatformService actual constructor() : FirebaseAuthRepository {
    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        // Firebase iOS-Implementierung fehlt derzeit. Platzhalter-Fehler, damit Aufrufer explizit sehen,
        // dass es nicht implementiert ist.
        TODO("Firebase Sign-in für iOS noch nicht implementiert")
    }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? {
        // Platzhalter
        TODO("Benutzererstellung für iOS noch nicht implementiert")
    }

    override suspend fun signOut() {
        // Platzhalter
        TODO("Sign-out für iOS noch nicht implementiert")
    }

    override fun currentUserId(): String? {
        // Platzhalter
        TODO("currentUserId für iOS noch nicht implementiert")
    }

    override fun isSignedIn(): Boolean {
        // Platzhalter
        TODO("isSignedIn für iOS noch nicht implementiert")
    }
}
