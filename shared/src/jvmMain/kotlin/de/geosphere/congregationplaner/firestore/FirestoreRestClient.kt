package de.geosphere.congregationplaner.firestore

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.DesktopEnv
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

/**
 * Gemeinsamer Zugriff der Desktop-Repositories auf die Firestore-REST-API des Emulators.
 *
 * @param signInRequiredMessage Fehlermeldung, falls kein ID-Token vorhanden ist.
 */
internal class FirestoreRestClient(
    private val httpClient: HttpClient,
    private val idTokenProvider: () -> String?,
    private val signInRequiredMessage: String,
) {
    /** Sendet die Anfrage und liefert den Body; Antworten außerhalb von 2xx werfen [FirestoreHttpException]. */
    fun request(method: String, url: String, body: String? = null): String {
        val token = idTokenProvider()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException(signInRequiredMessage)
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .apply {
                if (method == "GET") {
                    GET()
                } else {
                    if (body != null) header("Content-Type", "application/json")
                    val publisher = body?.let(HttpRequest.BodyPublishers::ofString)
                        ?: HttpRequest.BodyPublishers.noBody()
                    method(method, publisher)
                }
            }.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw FirestoreHttpException(response.statusCode())
        return response.body()
    }

    /** Wie [request], liefert für nicht existierende Dokumente (HTTP 404) aber `null`. */
    fun requestOrNull(method: String, url: String): String? = try {
        request(method, url)
    } catch (exception: FirestoreHttpException) {
        if (exception.status == HTTP_NOT_FOUND) null else throw exception
    }

    fun collectionUrl(collectionPath: String): String = "${firestoreDocumentsUrl()}/$collectionPath"

    fun documentUrl(collectionPath: String, id: String): String = "${collectionUrl(collectionPath)}/${encode(id)}"

    /** Query-Parameter, mit denen ein PATCH nur die angegebenen Felder überschreibt. */
    fun updateMask(fieldNames: List<String>): String =
        fieldNames.joinToString("&") { field -> "updateMask.fieldPaths=${encode(field)}" }

    companion object {
        private const val HTTP_NOT_FOUND = 404

        fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
    }
}

internal class FirestoreHttpException(val status: Int) : IOException("Firestore request failed with HTTP $status.")

internal fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

private const val DEFAULT_FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080"

internal fun firestoreDocumentsUrl(): String {
    val projectId = DesktopEnv.getValue("FIREBASE_PROJECT_ID", "firebase.projectId")
        ?.takeIf(String::isNotBlank)
        ?: throw IllegalStateException("FIREBASE_PROJECT_ID is required to access Firestore.")
    return "http://${firestoreEmulatorHost()}/v1/projects/$projectId/databases/(default)/documents"
}

private fun firestoreEmulatorHost(): String {
    val host = DesktopEnv.getValue(
        "FIRESTORE_EMULATOR_HOST",
        "FIREBASE_FIRESTORE_EMULATOR_HOST",
        "firebase.firestoreEmulatorHost",
    ) ?: DEFAULT_FIRESTORE_EMULATOR_HOST
    val normalizedHost = host.removePrefix("http://").removePrefix("https://").trimEnd('/')
    return if (':' in normalizedHost.substringAfterLast(']')) normalizedHost else "$normalizedHost:8080"
}

/** Ein Firestore-Dokument aus einer REST-Antwort: die Dokument-ID und dessen `fields`. */
internal data class FirestoreRestDocument(val id: String, val fields: JsonObject)

internal fun parseDocument(document: JsonObject): FirestoreRestDocument {
    val name = document.get("name")?.asString ?: throw IOException("Firestore returned a document without a name.")
    return FirestoreRestDocument(name.substringAfterLast('/'), document.getAsJsonObject("fields") ?: JsonObject())
}

internal fun parseDocument(response: String): FirestoreRestDocument =
    parseDocument(JsonParser.parseString(response).asJsonObject)

/** Liest die Dokumente einer List-Antwort; eine leere Collection liefert kein `documents`-Feld. */
internal fun parseDocuments(response: String): List<FirestoreRestDocument> =
    JsonParser.parseString(response).asJsonObject.getAsJsonArray("documents")
        ?.map { parseDocument(it.asJsonObject) }
        .orEmpty()

internal fun firestoreString(value: String): JsonObject = JsonObject().apply { addProperty("stringValue", value) }

internal fun firestoreNullableString(value: String?): JsonObject =
    value?.let(::firestoreString) ?: JsonObject().apply { add("nullValue", JsonNull.INSTANCE) }

internal fun firestoreBoolean(value: Boolean): JsonObject = JsonObject().apply { addProperty("booleanValue", value) }

internal fun JsonObject.optionalStringField(name: String): String? =
    get(name)?.takeIf { it.isJsonObject }?.asJsonObject?.get("stringValue")?.asString

internal fun JsonObject.booleanField(name: String): Boolean =
    get(name)?.takeIf { it.isJsonObject }?.asJsonObject?.get("booleanValue")?.asBoolean ?: false

internal fun firestoreFields(fields: JsonObject): String = JsonObject().apply { add("fields", fields) }.toString()

internal fun JsonObject.stringField(name: String): String {
    val value = get(name) ?: return ""
    if (!value.isJsonObject || !value.asJsonObject.has("stringValue")) {
        throw IllegalArgumentException("Firestore field '$name' must be a string.")
    }
    return value.asJsonObject.get("stringValue").asString
}

private const val FIRESTORE_POLL_INTERVAL_MILLIS = 15_000L

/** Die REST-API kennt keine Listener; Änderungen werden daher regelmäßig abgefragt. */
internal fun <T> pollingFlow(fetch: () -> T): Flow<T> = flow {
    do {
        emit(withContext(Dispatchers.IO) { fetch() })
        delay(FIRESTORE_POLL_INTERVAL_MILLIS)
    } while (currentCoroutineContext().isActive)
}

/** Führt [block] auf [Dispatchers.IO] aus und bildet Fehler (außer Abbruch) auf [Result.failure] ab. */
internal suspend fun <T> runCatchingIo(block: () -> T): Result<T> = try {
    Result.success(withContext(Dispatchers.IO) { block() })
} catch (exception: CancellationException) {
    throw exception
} catch (exception: Exception) {
    Result.failure(exception)
}
