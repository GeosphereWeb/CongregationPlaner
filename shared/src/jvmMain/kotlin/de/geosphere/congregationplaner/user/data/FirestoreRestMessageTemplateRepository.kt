package de.geosphere.congregationplaner.user.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

actual fun createMessageTemplateRepository(): MessageTemplateRepository = FirestoreRestMessageTemplateRepository()

internal class FirestoreRestMessageTemplateRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : MessageTemplateRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing user data.")

    override fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>> =
        pollingFlow {
            client.requestOrNull("GET", documentUrl(congregation, uid))
                ?.let { parseDocument(it).fields.templates() }
                .orEmpty()
        }

    override suspend fun saveTemplates(
        congregation: CongregationRef,
        uid: String,
        templates: List<MessageTemplate>,
    ): Result<Unit> = runCatchingIo {
        // PATCH ohne updateMask legt das Dokument an oder ersetzt es vollständig.
        val values = JsonArray().apply {
            templates.forEach { template ->
                val fields = JsonObject().apply {
                    add("name", firestoreString(template.name))
                    add("text", firestoreString(template.text))
                }
                add(JsonObject().apply { add("mapValue", JsonObject().apply { add("fields", fields) }) })
            }
        }
        val fields = JsonObject().apply {
            add("templates", JsonObject().apply { add("arrayValue", JsonObject().apply { add("values", values) }) })
        }
        client.request("PATCH", documentUrl(congregation, uid), firestoreFields(fields))
        Unit
    }

    private fun documentUrl(congregation: CongregationRef, uid: String): String =
        client.documentUrl(congregation.memberSettingsPath(uid), MESSAGE_TEMPLATES_DOCUMENT_ID)
}

// Einträge ohne Name oder Text werden übersprungen.
private fun JsonObject.templates(): List<MessageTemplate> = get("templates")?.takeIf { it.isJsonObject }?.asJsonObject
    ?.getAsJsonObject("arrayValue")
    ?.getAsJsonArray("values")
    ?.mapNotNull { value ->
        val fields = value.asJsonObject.getAsJsonObject("mapValue")?.getAsJsonObject("fields")
        val name = fields?.optionalStringField("name")
        val text = fields?.optionalStringField("text")
        if (name != null && text != null) MessageTemplate(name, text) else null
    }
    .orEmpty()
