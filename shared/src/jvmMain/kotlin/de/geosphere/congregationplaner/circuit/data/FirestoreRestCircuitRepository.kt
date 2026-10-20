package de.geosphere.congregationplaner.circuit.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import de.geosphere.congregationplaner.auth.DesktopEnv
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
import de.geosphere.congregationplaner.circuit.domain.repository.CircuitRepository
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

internal class FirestoreRestCircuitRepository(
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build(),
    private val idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : CircuitRepository {
    override fun observeAll(): Flow<List<Circuit>> = flow {
        do {
            emit(withContext(Dispatchers.IO) { fetchAll() })
            delay(FIRESTORE_POLL_INTERVAL_MILLIS)
        } while (currentCoroutineContext().isActive)
    }

    override suspend fun create(circuit: Circuit): Result<Circuit> = runCatchingIo {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to create a circuit." }
        try {
            // POST mit documentId schlägt mit 409 fehl, falls die Kreisnummer bereits vergeben ist.
            request("POST", "${collectionUrl()}?documentId=${encode(circuit.id)}", circuit.toFirestoreJson())
        } catch (exception: FirestoreHttpException) {
            if (exception.status == HTTP_CONFLICT) throw CircuitException.AlreadyExists(circuit.id)
            throw exception
        }
        circuit
    }

    override suspend fun update(circuit: Circuit): Result<Circuit> = runCatchingIo {
        require(circuit.id.isNotBlank()) { "A circuit ID is required to update a circuit." }
        val updateMask = FIRESTORE_FIELD_NAMES.joinToString("&") { field -> "updateMask.fieldPaths=${encode(field)}" }
        // currentDocument.exists verhindert, dass ein PATCH auf einen gelöschten Kreis ihn neu anlegt.
        request(
            method = "PATCH",
            url = "${documentUrl(circuit.id)}?$updateMask&currentDocument.exists=true",
            body = circuit.toFirestoreJson(),
        )
        circuit
    }

    override suspend fun delete(circuitId: String): Result<Unit> = runCatchingIo {
        require(circuitId.isNotBlank()) { "A circuit ID is required to delete a circuit." }
        // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Versammlungen zurück.
        val congregations = request("GET", "${documentUrl(circuitId)}/$CONGREGATION_SUBCOLLECTION?pageSize=1")
        if (JsonParser.parseString(congregations).asJsonObject.getAsJsonArray("documents")?.isEmpty == false) {
            throw CircuitException.HasCongregations(circuitId)
        }
        request("DELETE", documentUrl(circuitId))
        Unit
    }

    private suspend fun <T> runCatchingIo(block: () -> T): Result<T> = try {
        Result.success(withContext(Dispatchers.IO) { block() })
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    private fun fetchAll(): List<Circuit> {
        val response = request("GET", "${collectionUrl()}?pageSize=1000")
        val documents = JsonParser.parseString(response).asJsonObject.getAsJsonArray("documents") ?: return emptyList()
        return documents.map { element ->
            val document = element.asJsonObject
            val fields = document.getAsJsonObject("fields") ?: JsonObject()
            val documentName = document.get("name")?.asString
                ?: throw IOException("Firestore returned a document without a name.")
            Circuit(
                id = documentName.substringAfterLast('/'),
                name = fields.stringField("name"),
                overseerName = fields.stringField("overseer_name"),
            )
        }
    }

    private fun request(method: String, url: String, body: String? = null): String {
        val token = idTokenProvider()?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Sign in before accessing circuit data.")
        val request = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("Authorization", "Bearer $token")
            .apply {
                if (method == "GET") {
                    GET()
                } else {
                    if (body != null) header("Content-Type", "application/json")
                    method(method, body?.let(HttpRequest.BodyPublishers::ofString) ?: HttpRequest.BodyPublishers.noBody())
                }
            }.build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) throw FirestoreHttpException(response.statusCode())
        return response.body()
    }

    private fun collectionUrl(): String = "$documentsUrl/$CIRCUIT_COLLECTION_PATH"

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

    private fun Circuit.toFirestoreJson(): String {
        val fields = JsonObject()
        fields.add("name", firestoreString(name))
        fields.add("overseer_name", firestoreString(overseerName))
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

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private class FirestoreHttpException(val status: Int) :
        IOException("Firestore request failed with HTTP $status.")

    private companion object {
        const val DEFAULT_FIRESTORE_EMULATOR_HOST = "127.0.0.1:8080"
        const val FIRESTORE_POLL_INTERVAL_MILLIS = 15_000L
        const val HTTP_CONFLICT = 409
        val FIRESTORE_FIELD_NAMES = listOf("name", "overseer_name")
    }
}
