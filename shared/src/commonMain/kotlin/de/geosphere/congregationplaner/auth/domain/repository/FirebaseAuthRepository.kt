package de.geosphere.congregationplaner.auth.domain.repository

interface FirebaseUser {
    val uid: String
    val email: String?
    val displayName: String?
    val idToken: String?

    /** `true`, sobald die E-Mail-Adresse über den Link in der Bestätigungsmail bestätigt wurde. */
    val emailVerified: Boolean get() = false
}

interface FirebaseAuthRepository {
    suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser?

    /** Legt das Konto an; [displayName] („Vorname Nachname“) wird im Auth-Profil gespeichert. */
    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null,
    ): FirebaseUser?
    suspend fun signInWithGoogle(): FirebaseUser? = null
    fun isGoogleSignInAvailable(): Boolean = false
    suspend fun signOut()
    fun currentUserId(): String?
    fun currentUser(): FirebaseUser? = null
    fun currentUserIdToken(): String? = null

    /** Liest das Konto neu und holt einen frischen Token, z. B. nachdem die E-Mail-Adresse bestätigt wurde. */
    suspend fun refreshCurrentUser(): FirebaseUser? = currentUser()

    /** Schickt die Bestätigungsmail erneut; `false`, wenn das nicht möglich war. */
    suspend fun resendVerificationEmail(): Boolean = false
    fun isSignedIn(): Boolean
}
