package de.geosphere.congregationplaner

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
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

    override suspend fun signInWithGoogle(): FirebaseUser? {
        val activity = FirebaseAndroidContextHolder.activity ?: return null
        val clientIdResource = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName,
        )
        if (clientIdResource == 0) {
            Log.e("FirebaseAuth", "Google Sign-In client ID is missing from google-services.json")
            return null
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(activity.getString(clientIdResource))
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val credential = CredentialManager
                .create(activity)
                .getCredential(activity, request)
                .credential
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)

            suspendCancellableCoroutine { continuation ->
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            logAuthFailure("signInWithGoogle", task.exception)
                        }
                        continuation.resume(taskToUser(task))
                    }
            }
        } catch (exception: GetCredentialException) {
            logAuthFailure("signInWithGoogle", exception)
            null
        } catch (exception: GoogleIdTokenParsingException) {
            logAuthFailure("signInWithGoogle", exception)
            null
        }
    }

    override fun isGoogleSignInAvailable(): Boolean {
        return FirebaseAndroidContextHolder.activity != null
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
