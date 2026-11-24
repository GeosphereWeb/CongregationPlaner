package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreNullableInteger
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalIntField
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.net.http.HttpClient

internal class FirestoreRestSpeakerRequestRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : SpeakerRequestRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing speaker requests.")

    // Ein ungültiges Dokument wird übersprungen, damit es nicht die ganze Übersicht blockiert.
    override fun observeRequests(congregation: CongregationRef): Flow<List<SpeakerRequest>> = pollingFlow {
        client.listAllDocuments(congregation.talkRequestsPath).mapNotNull { runCatching { it.toRequest() }.getOrNull() }
    }

    override suspend fun commit(congregation: CongregationRef, write: ScheduleWrite): Result<Unit> = runCatchingIo {
        val slots = congregation.talkSchedulePath
        val requests = congregation.talkRequestsPath
        client.commit(
            write.saveSlots.map { client.writeUpdate("$slots/${it.id}", it.toFields()) } +
                write.deleteSlotDates.map { client.writeDelete("$slots/$it") } +
                write.saveRequests.map { client.writeUpdate("$requests/${it.id}", it.toFields()) } +
                write.deleteRequestIds.map { client.writeDelete("$requests/$it") },
        )
    }
}

private fun FirestoreRestDocument.toRequest(): SpeakerRequest = SpeakerRequest(
    id = id,
    createdOn = LocalDate.parse(fields.optionalStringField("createdOn").orEmpty()),
    note = fields.optionalStringField("note"),
    contactAttempts = fields.contactAttempts(),
    speakerMemberId = fields.optionalStringField("speakerMemberId"),
    speakerLastName = fields.optionalStringField("speakerLastName"),
    speakerFirstName = fields.optionalStringField("speakerFirstName"),
    externalSpeakerId = fields.optionalStringField("externalSpeakerId"),
    speakerCongregation = fields.optionalStringField("speakerCongregation"),
    talkNumber = fields.optionalIntField("talkNumber"),
    talkTitle = fields.optionalStringField("talkTitle"),
)

private fun JsonObject.contactAttempts(): List<ContactAttempt> = get("contactAttempts")?.takeIf { it.isJsonObject }
    ?.asJsonObject
    ?.getAsJsonObject("arrayValue")
    ?.getAsJsonArray("values")
    ?.mapNotNull { value ->
        value.asJsonObject.getAsJsonObject("mapValue")?.getAsJsonObject("fields")?.let { fields ->
            contactAttemptOrNull(
                fields.optionalStringField("date"),
                fields.optionalStringField("kind"),
                fields.optionalStringField("note"),
            )
        }
    }
    .orEmpty()

private fun ContactAttempt.toMapValue(): JsonObject {
    val fields = JsonObject().apply {
        add("date", firestoreString(date.toString()))
        add("kind", firestoreString(kind.name))
        add("note", firestoreNullableString(note))
    }
    return JsonObject().apply { add("mapValue", JsonObject().apply { add("fields", fields) }) }
}

private fun SpeakerRequest.toFields(): JsonObject = JsonObject().apply {
    add("createdOn", firestoreString(createdOn.toString()))
    add("note", firestoreNullableString(note))
    val attempts = JsonArray().apply { contactAttempts.forEach { add(it.toMapValue()) } }
    add("contactAttempts", JsonObject().apply { add("arrayValue", JsonObject().apply { add("values", attempts) }) })
    add("speakerMemberId", firestoreNullableString(speakerMemberId))
    add("speakerLastName", firestoreNullableString(speakerLastName))
    add("speakerFirstName", firestoreNullableString(speakerFirstName))
    add("externalSpeakerId", firestoreNullableString(externalSpeakerId))
    add("speakerCongregation", firestoreNullableString(speakerCongregation))
    add("talkNumber", firestoreNullableInteger(talkNumber))
    add("talkTitle", firestoreNullableString(talkTitle))
}
