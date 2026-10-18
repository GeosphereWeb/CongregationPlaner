package de.geosphere.congregationplaner.user.data

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.DesktopEnv
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

internal class FirestoreRestUserRepository(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
    private val idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : UserRepository {
    override suspend fun getOrCreateUser(authUser: FirebaseUser): Result<AppUser> = try {
        val user = withContext(Dispatchers.IO) {
            fetchUser(authUser.uid) ?: createUser(authUser.toNewAppUser())
        }
        Result.success(user)
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    override fun observeUser(uid: String): Flow<AppUser?> = flow {
        do {
            emit(withContext(Dispatchers.IO) { fetchUser(uid) })
            delay(FIRESTORE_POLL_INTERVAL_MILLIS)
        } while (currentCoroutineContext().isActive)
    }

    private fun fetchUser(uid: String): AppUser? {
        val response = send("GET", documentUrl(uid))
        if (response.statusCode() == HTTP_NOT_FOUND) return null
        return parseUser(response.successBody())
    }

    private fun createUser(user: AppUser): AppUser {
        val response = send("POST", "${collectionUrl()}?documentId=${encode(user.uid)}", user.toFirestoreJson())
        return parseUser(response.successBody())
    }

    private fun send(method: String, url: String, body: String? = null): HttpResponse<String> {
        val token = idTokenProvider()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Sign in before accessing user data.")
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .apply {
                if (body != null) header("Content-Type", "application/json")
                method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
            }.build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private fun HttpResponse<String>.successBody(): String {
        if (statusCode() !in 200..299) {
            throw IOException("Firestore request failed with HTTP ${statusCode()}.")
        }
        return body()
    }

    private fun parseUser(response: String): AppUser {
        val document = JsonParser.parseString(response).asJsonObject
        val fields = document.getAsJsonObject("fields") ?: JsonObject()
        val documentName = document.get("name")?.asString
            ?: throw IOException("Firestore returned a document without a name.")
        return AppUser(
            uid = documentName.substringAfterLast('/'),
            email = fields.optionalString("email"),
            displayName = fields.optionalString("displayName"),
            approved = fields.booleanField("approved"),
            role = UserRole.fromValue(fields.optionalString("role")),
        )
    }

    private fun collectionUrl(): String = "$documentsUrl/$USERS_COLLECTION_PATH"

    private fun documentUrl(id: String): String = "${collectionUrl()}/${encode(id)}"

    private val documentsUrl: String
        get() {
            val projectId = DesktopEnv.getValue("FIREBASE_PROJECT_ID", "firebase.projectId")
                ?.takeIf(String::isNotBlank)
                ?: throw IllegalStateException("FIREBASE_PROJECT_ID is required to access Firestore.")
            return "http://${emulatorHost()}/v1/projects/$projectId/databases/(default)/documents"
        }

    private fun emulatorHost(): String {
        val host = DesktopEnv.getValue(
            "FIRESTORE_EMULATOR_HOST",
            "FIREBASE_FIRESTORE_EMULATOR_HOST",
            "firebase.firestoreEmulatorHost",
        ) ?: DEFAULT_FIRESTORE_EMULATOR_HOST
        val normalizedHost = host.removePrefix("http://").removePrefix("https://").trimEnd('/')
        return if (':' in normalizedHost.substringAfterLast(']')) normalizedHost else "$normalizedHost:8080"
    }

    private fun AppUser.toFirestoreJson(): String {
        val fields = JsonObject()
        fields.add("email", firestoreString(email))
        fields.add("displayName", firestoreString(displayName))
        fields.add("approved", JsonObject().apply { addProperty("booleanValue", approved) })
        fields.add("role", firestoreString(role.name))
        return JsonObject().apply { add("fields", fields) }.toString()
    }

    private fun firestoreString(value: String?): JsonObject = JsonObject().apply {
        if (value == null) add("nullValue", JsonNull.INSTANCE) else addProperty("stringValue", value)
    }

    private fun JsonObject.optionalString(name: String): String? =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject?.get("stringValue")?.asString

    private fun JsonObject.booleanField(name: String): Boolean =
        get(name)?.takeIf { it.isJsonObject }?.asJsonObject?.get("booleanValue")?.asBoolean ?: false

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val DEFAULT_FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080"
        const val FIRESTORE_POLL_INTERVAL_MILLIS = 15_000L
        const val HTTP_NOT_FOUND = 404
    }
}
