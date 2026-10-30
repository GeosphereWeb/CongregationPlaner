package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.talk.domain.repository.TalkHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

actual fun createTalkHistoryRepository(): TalkHistoryRepository =
    FirestoreTalkHistoryRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkHistoryRepository(private val firestore: FirebaseFirestore) : TalkHistoryRepository {
    override fun observeAll(congregation: CongregationRef): Flow<List<TalkHistoryEntry>> = observeDocuments(
        firestore.collection(congregation.talkHistoryPath),
        "talk history entry",
        ::toTalkHistoryEntryOrNull,
    ).map { it.filterNotNull() }

    override suspend fun save(congregation: CongregationRef, entry: TalkHistoryEntry): Result<TalkHistoryEntry> =
        runFirestoreCatching("save talk history entry") {
            firestore.collection(congregation.talkHistoryPath).document(entry.talkNumber.toString())
                .set(
                    mapOf(
                        "heldOn" to entry.heldOn.map(LocalDate::toString),
                        "plannedOn" to entry.plannedOn.map(LocalDate::toString),
                        "speakers" to entry.speakers.mapKeys { it.key.toString() },
                    ),
                )
                .awaitResult()
            entry
        }

    override suspend fun delete(congregation: CongregationRef, talkNumber: Int): Result<Unit> =
        runFirestoreCatching("delete talk history entry") {
            firestore.collection(congregation.talkHistoryPath).document(talkNumber.toString()).delete().awaitResult()
        }
}

// Ein Dokument ohne gültige Nummer oder ohne lesbares Datum wird übersprungen, damit es nicht die Liste blockiert.
private fun toTalkHistoryEntryOrNull(document: DocumentSnapshot): TalkHistoryEntry? {
    val number = document.id.toIntOrNull()?.takeIf { it >= 1 }
    val dates = parseDates(document.get("heldOn"))
    val planned = parseDates(document.get("plannedOn") ?: emptyList<String>())
    if (number == null || dates.isNullOrEmpty() || planned == null) {
        logFirestoreFailure("parse talk history ${document.id}", IllegalArgumentException("Invalid talk history."))
        return null
    }
    return TalkHistoryEntry(number, dates.sorted(), planned.sorted(), parseSpeakers(document.get("speakers")))
}

// Einträge ohne lesbares Datum oder Namen werden übersprungen; der Tag bleibt dann ohne Redner.
private fun parseSpeakers(value: Any?): Map<LocalDate, String> = (value as? Map<*, *>).orEmpty()
    .mapNotNull { (day, name) ->
        val date = (day as? String)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (date == null || name !is String) null else date to name
    }.toMap()

// `null`, wenn der Wert keine Liste lesbarer Daten ist.
private fun parseDates(value: Any?): List<LocalDate>? = (value as? List<*>)
    ?.map { (it as? String)?.let { text -> runCatching { LocalDate.parse(text) }.getOrNull() } }
    ?.takeIf { dates -> dates.none { it == null } }
    ?.filterNotNull()
