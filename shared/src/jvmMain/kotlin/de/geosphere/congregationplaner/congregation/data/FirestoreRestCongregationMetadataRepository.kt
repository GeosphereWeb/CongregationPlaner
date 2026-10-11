package de.geosphere.congregationplaner.congregation.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.DesktopEnv
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
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

internal class FirestoreRestCongregationMetadataRepository(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
    private val idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : CongregationMetadataRepository {
    override fun observeAll(): Flow<List<CongregationMetadata>> = flow {
        do {
            emit(withContext(Dispatchers.IO) { fetchAll() })
            delay(FIRESTORE_POLL_INTERVAL_MILLIS)
        } while (currentCoroutineContext().isActive)
    }

    override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> = try {
        val id = withContext(Dispatchers.IO) {
            if (congregation.id.isBlank()) {
                documentId(request("POST", collectionUrl(), congregation.toFirestoreJson()))
            } else {
                val updateMask = FIRESTORE_FIELD_NAMES.joinToString("&") { field ->
                    "updateMask.fieldPaths=${encode(field)}"
                }
                request(
                    method = "PATCH",
                    url = "${documentUrl(congregation.id)}?$updateMask",
                    body = congregation.toFirestoreJson(),
                )
                congregation.id
            }
        }
        Result.success(congregation.copy(id = id))
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun fetchAll(): List<CongregationMetadata> {
        val response = request("GET", "${collectionUrl()}?pageSize=1000")
        val documents = JsonParser.parseString(response).asJsonObject.getAsJsonArray("documents") ?: return emptyList()
        return documents.map { element ->
            val document = element.asJsonObject
            val fields = document.getAsJsonObject("fields") ?: JsonObject()
            val documentName = document.get("name")?.asString
                ?: throw IOException("Firestore returned a document without a name.")
            CongregationMetadata(
                id = documentName.substringAfterLast('/'),
                name = fields.stringField("name"),
                midweekMeetingTime = fields.stringField("midweek_meeting_time"),
                midweekMeetingWeekday = fields.stringField("midweek_meeting_weekday"),
                weekendMeetingTime = fields.stringField("weekend_meeting_time"),
                weekendMeetingWeekday = fields.stringField("weekend_meeting_weekday"),
            )
        }
    }

    private fun request(method: String, url: String, body: String? = null): String {
        val token = idTokenProvider()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Sign in before accessing congregation data.")
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .apply {
                if (body == null) {
                    GET()
                } else {
                    header("Content-Type", "application/json")
                    method(method, HttpRequest.BodyPublishers.ofString(body))
                }
            }.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IOException("Firestore request failed with HTTP ${response.statusCode()}.")
        }
        return response.body()
    }

    private fun collectionUrl(): String = "$documentsUrl/$CONGREGATION_COLLECTION_PATH"

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

    private fun CongregationMetadata.toFirestoreJson(): String {
        val fields = JsonObject()
        fields.add("name", firestoreString(name))
        fields.add("midweek_meeting_time", firestoreString(midweekMeetingTime))
        fields.add("midweek_meeting_weekday", firestoreString(midweekMeetingWeekday))
        fields.add("weekend_meeting_time", firestoreString(weekendMeetingTime))
        fields.add("weekend_meeting_weekday", firestoreString(weekendMeetingWeekday))
        return JsonObject().apply { add("fields", fields) }.toString()
    }

    private fun firestoreString(value: String): JsonObject = JsonObject().apply {
        addProperty("stringValue", value)
    }

    private fun JsonObject.stringField(name: String): String {
        val value = get(name) ?: return ""
        if (!value.isJsonObject || !value.asJsonObject.has("stringValue")) {
            throw IllegalArgumentException("Firestore field '$name' must be a string.")
        }
        return value.asJsonObject.get("stringValue").asString
    }

    private fun documentId(response: String): String {
        val name = JsonParser.parseString(response).asJsonObject.get("name")?.asString
            ?: throw IOException("Firestore returned a document without a name.")
        return name.substringAfterLast('/')
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private companion object {
        const val DEFAULT_FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080"
        const val FIRESTORE_POLL_INTERVAL_MILLIS = 15_000L
        val FIRESTORE_FIELD_NAMES = listOf(
            "name",
            "midweek_meeting_time",
            "midweek_meeting_weekday",
            "weekend_meeting_time",
            "weekend_meeting_weekday",
        )
    }
}
