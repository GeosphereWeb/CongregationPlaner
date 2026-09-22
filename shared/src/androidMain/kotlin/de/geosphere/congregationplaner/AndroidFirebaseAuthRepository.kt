package de.geosphere.congregationplaner

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual fun createFirebaseAuthPlatformService(): FirebaseAuthRepository = FirebaseAuthPlatformService()

private const val FIREBASE_AUTH_EMULATOR_PORT = 9099

class FirebaseAuthPlatformService : FirebaseAuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance().also { auth ->
        if (FirebaseAndroidContextHolder.useAuthEmulator) {
            auth.useEmulator(FirebaseAndroidContextHolder.authEmulatorHost, FIREBASE_AUTH_EMULATOR_PORT)
        }
    }

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        logAuthFailure("signInWithEmailAndPassword", task.exception)
                    }
                    continuation.resume(taskToUser(task))
                }
        }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        suspendCancellableCoroutine { continuation ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        logAuthFailure("createUserWithEmailAndPassword", task.exception)
                    }
                    val user = taskToUser(task)
                    if (user == null) {
                        continuation.resume(null)
                        return@addOnCompleteListener
                    }

                    if (FirebaseAndroidContextHolder.useAuthEmulator) {
                        continuation.resume(user)
                        return@addOnCompleteListener
                    }

                    auth.currentUser?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                continuation.resume(user)
                            } else {
                                logAuthFailure("sendEmailVerification", verificationTask.exception)
                                continuation.resume(null)
                            }
                        }
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

        val user = task.result?.user ?: auth.currentUser ?: return null
        return object : FirebaseUser {
            override val uid: String = user.uid
            override val email: String? = user.email
            override val displayName: String? = user.displayName
            override val idToken: String? = null
        }
    }

    private fun logAuthFailure(operation: String, exception: Exception?) {
        Log.e(
            "FirebaseAuth",
            "$operation failed while using auth emulator " +
                "${FirebaseAndroidContextHolder.useAuthEmulator} at " +
                "${FirebaseAndroidContextHolder.authEmulatorHost}:$FIREBASE_AUTH_EMULATOR_PORT",
            exception,
        )
    }
}
