package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreHttpException
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

    override fun observeAll(congregation: CongregationRef): Flow<List<TalkSlot>> = pollingFlow {
        client.listAllDocuments(congregation.talkSchedulePath)
            .sortedBy { it.id }
            .mapNotNull { document -> runCatching { document.toSlot() }.getOrNull() }
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> = runCatchingIo {
        // PATCH ohne updateMask ersetzt das Dokument und legt es bei Bedarf an.
        client.request("PATCH", client.documentUrl(congregation.talkSchedulePath, slot.id), slot.toBody())
        slot
    }

    // `currentDocument.exists=false` lässt Firestore einen vorhandenen Termin mit 409 ablehnen, statt ihn zu ersetzen.
    override suspend fun createMissingSlots(congregation: CongregationRef, slots: List<TalkSlot>): Result<Int> =
        runCatchingIo {
            slots.count { slot ->
                val url = "${client.documentUrl(congregation.talkSchedulePath, slot.id)}?currentDocument.exists=false"
                try {
                    client.request("PATCH", url, slot.toBody())
                    true
                } catch (exception: FirestoreHttpException) {
                    if (exception.status != HTTP_CONFLICT) throw exception
                    false
                }
            }
        }

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> =
        runCatchingIo { client.request("DELETE", client.documentUrl(congregation.talkSchedulePath, date.toString())) }
            .map {}

    // updateMask begrenzt den PATCH auf dieses Feld, currentDocument.exists verhindert, dass ein fehlender Termin
    // entsteht; die Rules erlauben dem Helfer genau das.
    override suspend fun setConfirmed(
        congregation: CongregationRef,
        date: LocalDate,
        confirmed: Boolean,
    ): Result<Unit> = runCatchingIo {
        client.request(
            method = "PATCH",
            url = "${client.documentUrl(congregation.talkSchedulePath, date.toString())}?" +
                "${client.updateMask(listOf("confirmed"))}&currentDocument.exists=true",
            body = firestoreFields(JsonObject().apply { add("confirmed", firestoreBoolean(confirmed)) }),
        )
    }

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
        note = fields.optionalStringField("note"),
    )

    private fun TalkSlot.toBody(): String = firestoreFields(
        JsonObject().apply {
            add("time", firestoreString(time.toString()))
            add("talkCancelled", firestoreBoolean(talkCancelled))
            add("confirmed", firestoreBoolean(confirmed))
            add("note", firestoreNullableString(note))
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

private const val HTTP_CONFLICT = 409
