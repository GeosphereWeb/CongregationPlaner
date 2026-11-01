package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.booleanField
import de.geosphere.congregationplaner.firestore.firestoreBoolean
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreNullableInteger
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.optionalIntField
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import java.net.http.HttpClient

internal class FirestoreRestTalkScheduleRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : TalkScheduleRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing the talk schedule.")

    // shortcut: die REST-Liste kennt hier keinen ID-Bereich, deshalb wird die ganze Collection seitenweise geladen
    // und nach Jahr gefiltert; bei sehr vielen Terminen (Jahrzehnte) auf eine StructuredQuery umstellen.
    // Ein Dokument mit ungültiger ID oder Zeit wird übersprungen, damit es nicht das ganze Jahr blockiert.
    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> = pollingFlow {
        val range = yearIdRange(year)
        client.listAllDocuments(congregation.talkSchedulePath)
            .filter { it.id in range }
            .sortedBy { it.id }
            .mapNotNull { document -> runCatching { document.toSlot() }.getOrNull() }
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> = runCatchingIo {
        // PATCH ohne updateMask ersetzt das Dokument und legt es bei Bedarf an.
        client.request("PATCH", client.documentUrl(congregation.talkSchedulePath, slot.id), slot.toBody())
        slot
    }

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> =
        runCatchingIo { client.request("DELETE", client.documentUrl(congregation.talkSchedulePath, date.toString())) }
            .map {}

    private fun FirestoreRestDocument.toSlot(): TalkSlot = TalkSlot(
        date = LocalDate.parse(id),
        time = LocalTime.parse(fields.optionalStringField("time").orEmpty()),
        weekEventId = fields.optionalStringField("weekEventId"),
        weekEventName = fields.optionalStringField("weekEventName"),
        talkCancelled = fields.booleanField("talkCancelled"),
        confirmed = fields.booleanField("confirmed"),
        talkNumber = fields.optionalIntField("talkNumber"),
        talkTitle = fields.optionalStringField("talkTitle"),
        speakerMemberId = fields.optionalStringField("speakerMemberId"),
        speakerLastName = fields.optionalStringField("speakerLastName"),
        speakerFirstName = fields.optionalStringField("speakerFirstName"),
        chairmanMemberId = fields.optionalStringField("chairmanMemberId"),
        chairmanLastName = fields.optionalStringField("chairmanLastName"),
        chairmanFirstName = fields.optionalStringField("chairmanFirstName"),
        externalSpeakerId = fields.optionalStringField("externalSpeakerId"),
        speakerCongregation = fields.optionalStringField("speakerCongregation"),
    )

    private fun TalkSlot.toBody(): String = firestoreFields(
        JsonObject().apply {
            add("time", firestoreString(time.toString()))
            add("talkCancelled", firestoreBoolean(talkCancelled))
            // Nur gesetzt schreiben (fehlend = false), damit ältere Rules den Termin weiter annehmen.
            if (confirmed) add("confirmed", firestoreBoolean(true))
            add("weekEventId", firestoreNullableString(weekEventId))
            add("weekEventName", firestoreNullableString(weekEventName))
            add("talkNumber", firestoreNullableInteger(talkNumber))
            add("talkTitle", firestoreNullableString(talkTitle))
            add("speakerMemberId", firestoreNullableString(speakerMemberId))
            add("speakerLastName", firestoreNullableString(speakerLastName))
            add("speakerFirstName", firestoreNullableString(speakerFirstName))
            add("chairmanMemberId", firestoreNullableString(chairmanMemberId))
            add("chairmanLastName", firestoreNullableString(chairmanLastName))
            add("chairmanFirstName", firestoreNullableString(chairmanFirstName))
            add("externalSpeakerId", firestoreNullableString(externalSpeakerId))
            add("speakerCongregation", firestoreNullableString(speakerCongregation))
        },
    )
}
