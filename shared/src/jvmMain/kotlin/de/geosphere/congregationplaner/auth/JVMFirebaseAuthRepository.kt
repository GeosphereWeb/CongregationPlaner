package de.geosphere.congregationplaner.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

actual fun createFirebaseAuthPlatformService(): FirebaseAuthRepository = FirebaseAuthPlatformService()

class FirebaseAuthPlatformService(
    private val apiKeyProvider: () -> String? = ::resolveDesktopApiKey,
    private val connectionFactory: (String) -> HttpURLConnection = { requestUrl ->
        URL(requestUrl).openConnection() as HttpURLConnection
    },
) : FirebaseAuthRepository {
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
            val user = performDesktopAuthRequest(
                endpoint = "accounts:signUp",
                email = email,
                password = password,
            ) ?: return@withContext null

            val idToken = user.idToken ?: return@withContext user
            if (sendVerificationEmailRequest(idToken)) {
                user
            } else {
                null
            }
        }

    override suspend fun signOut() {
        currentUser = null
    }

    override fun currentUserId(): String? = currentUser?.uid

    override fun isSignedIn(): Boolean = currentUser != null

    private fun performDesktopAuthRequest(endpoint: String, email: String, password: String): FirebaseUser? {
        val apiKey = apiKeyProvider() ?: return null
        val body = """
            {
              "email": "${escapeJson(email)}",
              "password": "${escapeJson(password)}",
              "returnSecureToken": true
            }
        """.trimIndent()

        val url = buildDesktopAuthUrl(endpoint, apiKey)
        val connection = connectionFactory(url)
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

    private fun sendVerificationEmailRequest(idToken: String): Boolean {
        val apiKey = apiKeyProvider() ?: return false
        val body = """
            {
              "requestType": "VERIFY_EMAIL",
              "idToken": "${escapeJson(idToken)}"
            }
        """.trimIndent()

        val url = buildDesktopAuthUrl("accounts:sendOobCode", apiKey)
        val connection = connectionFactory(url)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(StandardCharsets.UTF_8))
        }

        return connection.responseCode in 200..299
    }
}

private fun buildDesktopAuthUrl(endpoint: String, apiKey: String): String {
    val baseUrl = if (isFirebaseAuthEmulatorEnabled()) {
        val host = DesktopEnv.getValue(
            "FIREBASE_AUTH_EMULATOR_HOST",
            "firebase.authEmulatorHost",
        ) ?: "localhost"
        val port = DesktopEnv.getValue(
            "FIREBASE_AUTH_EMULATOR_PORT",
            "firebase.authEmulatorPort",
        )?.toIntOrNull() ?: 9099
        "http://$host:$port/identitytoolkit.googleapis.com/v1"
    } else {
        "https://identitytoolkit.googleapis.com/v1"
    }
    return "$baseUrl/$endpoint?key=$apiKey"
}

private fun isFirebaseAuthEmulatorEnabled(): Boolean {
    val explicitEnabled = DesktopEnv.getValue(
        "FIREBASE_AUTH_EMULATOR_ENABLED",
        "firebase.authEmulatorEnabled",
    )
    if (explicitEnabled != null) {
        return explicitEnabled.equals("true", ignoreCase = true)
    }

    val host = DesktopEnv.getValue("FIREBASE_AUTH_EMULATOR_HOST", "firebase.authEmulatorHost")
    val port = DesktopEnv.getValue("FIREBASE_AUTH_EMULATOR_PORT", "firebase.authEmulatorPort")
    return !host.isNullOrBlank() || !port.isNullOrBlank()
}

private fun desktopAuthUrl(endpoint: String, apiKey: String): String {
    val configuredHost = DesktopEnv.getValue("FIREBASE_AUTH_EMULATOR_HOST", "firebase.authEmulatorHost")
    val configuredPort = DesktopEnv.getValue("FIREBASE_AUTH_EMULATOR_PORT", "firebase.authEmulatorPort")
    return buildDesktopAuthUrl(endpoint, apiKey, configuredHost, configuredPort)
}

internal fun buildDesktopAuthUrl(
    endpoint: String,
    apiKey: String,
    configuredHost: String?,
    configuredPort: String?,
): String {
    val host = configuredHost ?: DEFAULT_FIREBASE_AUTH_EMULATOR_HOST
    val normalizedHost = host
        .removePrefix("http://")
        .removePrefix("https://")
        .trimEnd('/')
        .let { host ->
            if (':' in host.substringAfterLast('/')) {
                host
            } else {
                val port = configuredPort ?: DEFAULT_FIREBASE_AUTH_EMULATOR_PORT
                "$host:$port"
            }
        }

    return "http://$normalizedHost/identitytoolkit.googleapis.com/v1/$endpoint?key=$apiKey"
}

private const val DEFAULT_FIREBASE_AUTH_EMULATOR_HOST = "127.0.0.1:9099"
private const val DEFAULT_FIREBASE_AUTH_EMULATOR_PORT = "9099"

private fun resolveDesktopApiKey(): String? = DesktopEnv.getValue(
    "FIREBASE_WEB_API_KEY",
    "firebase.webApiKey",
    "FIREBASE_API_KEY",
    "firebase.apiKey",
)

internal fun escapeJson(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")

internal fun extractJsonString(json: String, key: String): String? {
    val pattern = Regex("\"${Regex.escape(key)}\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
    return pattern.find(json)?.groupValues?.getOrNull(1)?.let { raw ->
        raw.replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\\", "\\")
    }
}
