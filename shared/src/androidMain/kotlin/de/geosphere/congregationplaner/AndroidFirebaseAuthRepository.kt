package de.geosphere.congregationplaner

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createFirebaseAuthPlatformService(): FirebaseAuthRepository = FirebaseAuthPlatformService()

class FirebaseAuthPlatformService : FirebaseAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    continuation.resume(taskToUser(task))
                }
        }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    continuation.resume(taskToUser(task))
                }
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun isSignedIn(): Boolean = auth.currentUser != null

    private fun taskToUser(task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>): FirebaseUser? {
        if (!task.isSuccessful) {
            return null
        }

        val user = auth.currentUser ?: return null
        return object : FirebaseUser {
            override val uid: String = user.uid
            override val email: String? = user.email
            override val displayName: String? = user.displayName
            override val idToken: String? = null
        }
    }
}
