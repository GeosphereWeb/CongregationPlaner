package de.geosphere.congregationplaner.talk.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.firestoreStringMap
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringArrayField
import de.geosphere.congregationplaner.firestore.stringMapField
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import java.net.http.HttpClient

internal class FirestoreRestTalkHistoryRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : TalkHistoryRepository {
    private val client =
        FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing the talk history.")

    override fun observeAll(congregation: CongregationRef): Flow<List<TalkHistoryEntry>> = pollingFlow {
        client.listAllDocuments(congregation.talkHistoryPath).mapNotNull { it.toTalkHistoryEntryOrNull() }
    }

    override suspend fun save(congregation: CongregationRef, entry: TalkHistoryEntry): Result<TalkHistoryEntry> =
        runCatchingIo {
            val body = firestoreFields(
                JsonObject().apply {
                    add("heldOn", firestoreStringArray(entry.heldOn.map(LocalDate::toString)))
                    add("plannedOn", firestoreStringArray(entry.plannedOn.map(LocalDate::toString)))
                    add("speakers", firestoreStringMap(entry.speakers.mapKeys { it.key.toString() }))
                },
            )
            client.request("PATCH", client.documentUrl(congregation.talkHistoryPath, entry.talkNumber.toString()), body)
            entry
        }

    override suspend fun delete(congregation: CongregationRef, talkNumber: Int): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.talkHistoryPath, talkNumber.toString()))
    }.map {}

    // Ein Dokument ohne gültige Nummer oder ohne lesbares Datum wird übersprungen, damit es nicht die Liste blockiert.
    private fun FirestoreRestDocument.toTalkHistoryEntryOrNull(): TalkHistoryEntry? {
        val number = id.toIntOrNull()?.takeIf { it >= 1 }
        val dates = parseDates("heldOn")
        val planned = parseDates("plannedOn")
        if (number == null || dates.isNullOrEmpty() || planned == null) return null
        // Einträge ohne lesbares Datum werden übersprungen; der Tag bleibt dann ohne Redner.
        val speakers = fields.stringMapField("speakers")
            .mapNotNull { (day, name) -> runCatching { LocalDate.parse(day) }.getOrNull()?.let { it to name } }
            .toMap()
        return TalkHistoryEntry(number, dates.sorted(), planned.sorted(), speakers)
    }

    // `null`, wenn ein Eintrag kein lesbares Datum ist; ein fehlendes Feld ist eine leere Liste.
    private fun FirestoreRestDocument.parseDates(field: String): List<LocalDate>? = fields.stringArrayField(field)
        .map { text -> text?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
        .takeIf { dates -> dates.none { it == null } }
        ?.filterNotNull()
}
