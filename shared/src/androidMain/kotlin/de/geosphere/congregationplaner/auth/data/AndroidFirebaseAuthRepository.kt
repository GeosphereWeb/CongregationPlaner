package de.geosphere.congregationplaner.auth.data

import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
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

    override suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String?,
    ): FirebaseUser? = suspendCancellableCoroutine { continuation ->
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    logAuthFailure("createUserWithEmailAndPassword", task.exception)
                }
                val created = taskToUser(task)
                if (created == null) {
                    continuation.resume(null)
                    return@addOnCompleteListener
                }
                storeDisplayName(displayName) { stored ->
                    // Nach dem Speichern des Namens neu lesen, damit der Nutzer ihn schon trägt.
                    val user = if (stored) auth.currentUser?.toFirebaseUser() ?: created else created
                    sendVerificationEmail(user, continuation::resume)
                }
            }
    }

    // Scheitert nur das Speichern des Namens, bleibt das Konto bestehen; der Name lässt sich im Profil nachtragen.
    private fun storeDisplayName(displayName: String?, onDone: (stored: Boolean) -> Unit) {
        val name = displayName?.trim().orEmpty()
        val currentUser = if (name.isEmpty()) null else auth.currentUser
        if (currentUser == null) {
            onDone(false)
            return
        }
        val request = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        currentUser.updateProfile(request).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                logAuthFailure("updateProfile", task.exception)
            }
            onDone(task.isSuccessful)
        }
    }

    private fun sendVerificationEmail(user: FirebaseUser?, onResult: (FirebaseUser?) -> Unit) {
        if (user == null || FirebaseAndroidContextHolder.useAuthEmulator) {
            onResult(user)
            return
        }
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
            if (verificationTask.isSuccessful) {
                onResult(user)
            } else {
                logAuthFailure("sendEmailVerification", verificationTask.exception)
                onResult(null)
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

    override fun isGoogleSignInAvailable(): Boolean = FirebaseAndroidContextHolder.activity != null

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun refreshCurrentUser(): FirebaseUser? = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        user.reload().addOnCompleteListener { reload ->
            if (!reload.isSuccessful) {
                logAuthFailure("reload", reload.exception)
            }
            // Erst der neue Token trägt die bestätigte E-Mail-Adresse (`email_verified`) in die Firestore-Regeln.
            val refreshed = auth.currentUser?.getIdToken(true)
            if (refreshed == null) {
                continuation.resume(auth.currentUser?.toFirebaseUser())
                return@addOnCompleteListener
            }
            refreshed.addOnCompleteListener { token ->
                if (!token.isSuccessful) {
                    logAuthFailure("getIdToken", token.exception)
                }
                continuation.resume(auth.currentUser?.toFirebaseUser())
            }
        }
    }

    override suspend fun resendVerificationEmail(): Boolean = suspendCancellableCoroutine { continuation ->
        val user = auth.currentUser
        if (user == null) {
            continuation.resume(false)
            return@suspendCancellableCoroutine
        }
        user.sendEmailVerification().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                logAuthFailure("sendEmailVerification", task.exception)
            }
            continuation.resume(task.isSuccessful)
        }
    }

    override fun currentUserId(): String? = auth.currentUser?.uid

    override fun currentUser(): FirebaseUser? = auth.currentUser?.toFirebaseUser()

    override fun isSignedIn(): Boolean = auth.currentUser != null

    private fun taskToUser(
        task: com.google.android.gms.tasks.Task<com.google.firebase.auth.AuthResult>,
    ): FirebaseUser? {
        if (!task.isSuccessful) {
            return null
        }

        val user = task.result?.user ?: auth.currentUser ?: return null
        return user.toFirebaseUser()
    }

    private fun com.google.firebase.auth.FirebaseUser.toFirebaseUser(): FirebaseUser {
        val user = this
        return object : FirebaseUser {
            override val uid: String = user.uid
            override val email: String? = user.email
            override val displayName: String? = user.displayName
            override val idToken: String? = null
            override val emailVerified: Boolean = user.isEmailVerified
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
