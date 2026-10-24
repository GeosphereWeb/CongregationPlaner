package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.booleanField
import de.geosphere.congregationplaner.firestore.firestoreBoolean
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreNullableString
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocuments
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

    // shortcut: die REST-Liste kennt hier keinen ID-Bereich, deshalb wird die Collection (max. 1000 Termine,
    // gut 19 Jahre) geladen und nach Jahr gefiltert; bei mehr Terminen auf eine StructuredQuery umstellen.
    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> = pollingFlow {
        val range = yearIdRange(year)
        parseDocuments(client.request("GET", "${client.collectionUrl(congregation.talkSchedulePath)}?pageSize=1000"))
            .filter { it.id in range }
            .sortedBy { it.id }
            .map { it.toSlot() }
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
    )

    private fun TalkSlot.toBody(): String = firestoreFields(
        JsonObject().apply {
            add("time", firestoreString(time.toString()))
            add("talkCancelled", firestoreBoolean(talkCancelled))
            add("weekEventId", firestoreNullableString(weekEventId))
            add("weekEventName", firestoreNullableString(weekEventName))
        },
    )
}
