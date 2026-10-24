package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

actual fun createTalkScheduleRepository(): TalkScheduleRepository =
    FirestoreTalkScheduleRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreTalkScheduleRepository(private val firestore: FirebaseFirestore) : TalkScheduleRepository {
    override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> {
        val range = yearIdRange(year)
        val query = firestore.collection(congregation.talkSchedulePath)
            .whereGreaterThanOrEqualTo(FieldPath.documentId(), range.start)
            .whereLessThanOrEqualTo(FieldPath.documentId(), range.endInclusive)
        return observeDocuments(query, "talk slot", ::toSlotOrNull).map { it.filterNotNull() }
    }

    override suspend fun saveSlot(congregation: CongregationRef, slot: TalkSlot): Result<TalkSlot> =
        runFirestoreCatching("save talk slot") {
            firestore.collection(congregation.talkSchedulePath).document(slot.id)
                .set(slot.toFirestoreFields())
                .awaitResult()
            slot
        }

    override suspend fun deleteSlot(congregation: CongregationRef, date: LocalDate): Result<Unit> =
        runFirestoreCatching("delete talk slot") {
            firestore.collection(congregation.talkSchedulePath).document(date.toString()).delete().awaitResult()
        }
}

// Ein Dokument mit ungültiger ID oder Zeit wird übersprungen, damit es nicht das ganze Jahr blockiert.
private fun toSlotOrNull(document: DocumentSnapshot): TalkSlot? = runCatching { toSlot(document) }
    .onFailure { logFirestoreFailure("parse talk slot ${document.id}", it) }
    .getOrNull()

private fun toSlot(document: DocumentSnapshot): TalkSlot = TalkSlot(
    date = LocalDate.parse(document.id),
    time = LocalTime.parse(document.getString("time").orEmpty()),
    weekEventId = document.getString("weekEventId"),
    weekEventName = document.getString("weekEventName"),
    talkCancelled = document.getBoolean("talkCancelled") ?: false,
    talkNumber = document.getLong("talkNumber")?.toInt(),
    talkTitle = document.getString("talkTitle"),
    speakerMemberId = document.getString("speakerMemberId"),
    speakerLastName = document.getString("speakerLastName"),
    speakerFirstName = document.getString("speakerFirstName"),
    chairmanMemberId = document.getString("chairmanMemberId"),
    chairmanLastName = document.getString("chairmanLastName"),
    chairmanFirstName = document.getString("chairmanFirstName"),
)

// LocalTime.toString() liefert HH:mm, solange keine Sekunden gesetzt sind; die Rules verlangen genau dieses Format.
private fun TalkSlot.toFirestoreFields(): Map<String, Any?> = mapOf(
    "time" to time.toString(),
    "talkCancelled" to talkCancelled,
    "weekEventId" to weekEventId,
    "weekEventName" to weekEventName,
    "talkNumber" to talkNumber,
    "talkTitle" to talkTitle,
    "speakerMemberId" to speakerMemberId,
    "speakerLastName" to speakerLastName,
    "speakerFirstName" to speakerFirstName,
    "chairmanMemberId" to chairmanMemberId,
    "chairmanLastName" to chairmanLastName,
    "chairmanFirstName" to chairmanFirstName,
)
