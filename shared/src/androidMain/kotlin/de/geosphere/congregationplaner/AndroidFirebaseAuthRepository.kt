package de.geosphere.congregationplaner

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual class FirebaseAuthPlatformService actual constructor() : FirebaseAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user == null) {
                            continuation.resume(null)
                            return@addOnCompleteListener
                        }

                        continuation.resume(
                            object : FirebaseUser {
                                override val uid: String = user.uid
                                override val email: String? = user.email
                                override val displayName: String? = user.displayName
                                override val idToken: String? = null
                            },
                        )
                    } else {
                        continuation.resume(null)
                    }
                }
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun isSignedIn(): Boolean = auth.currentUser != null
}
