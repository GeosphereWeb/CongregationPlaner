package de.geosphere.congregationplaner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

actual fun createFirebaseAuthPlatformService(): FirebaseAuthRepository = FirebaseAuthPlatformService()

class FirebaseAuthPlatformService : FirebaseAuthRepository {
    private var currentUser: FirebaseUser? = null

    override suspend fun signInWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.IO) {
            performDesktopAuthRequest(
                endpoint = "accounts:signInWithPassword",
                email = email,
                password = password,
            )
        }

    override suspend fun createUserWithEmailAndPassword(email: String, password: String): FirebaseUser? =
        withContext(Dispatchers.IO) {
            performDesktopAuthRequest(
                endpoint = "accounts:signUp",
                email = email,
                password = password,
            )
        }

    override suspend fun signOut() {
        currentUser = null
    }

    override fun currentUserId(): String? = currentUser?.uid

    override fun isSignedIn(): Boolean = currentUser != null

    private fun performDesktopAuthRequest(endpoint: String, email: String, password: String): FirebaseUser? {
        val apiKey = resolveDesktopApiKey() ?: return null
        val body = """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}",
              "returnSecureToken": true
            }
        """.trimIndent()

        val url = URL("https://identitytoolkit.googleapis.com/v1/$endpoint?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(StandardCharsets.UTF_8))
        }

        val responseCode = connection.responseCode
        val responseText = if (responseCode in 200..299) {
            connection.inputStream.bufferedReader().readText()
        } else {
            connection.errorStream?.bufferedReader()?.readText() ?: ""
        }

        if (responseCode !in 200..299) {
            return null
        }

        val uid = extractJsonString(responseText, "localId")
        val emailValue = extractJsonString(responseText, "email") ?: email
        val displayName = extractJsonString(responseText, "displayName")
        val idToken = extractJsonString(responseText, "idToken")

        if (uid.isNullOrBlank()) {
            return null
        }

        currentUser = object : FirebaseUser {
            override val uid: String = uid
            override val email: String? = emailValue
            override val displayName: String? = displayName
            override val idToken: String? = idToken
        }
        return currentUser
    }
}

private fun resolveDesktopApiKey(): String? {
    return DesktopEnvLoader.getValue(
        "FIREBASE_WEB_API_KEY",
        "firebase.webApiKey",
        "FIREBASE_API_KEY",
        "firebase.apiKey",
    )
}

private fun escapeJson(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

private fun extractJsonString(json: String, key: String): String? {
    val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
    return pattern.find(json)?.groupValues?.getOrNull(1)?.let { raw ->
        raw.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
