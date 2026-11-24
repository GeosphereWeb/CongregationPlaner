package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.talk.domain.repository.SpeakerRequestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

actual fun createSpeakerRequestRepository(): SpeakerRequestRepository =
    FirestoreSpeakerRequestRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreSpeakerRequestRepository(private val firestore: FirebaseFirestore) : SpeakerRequestRepository {
    override fun observeRequests(congregation: CongregationRef): Flow<List<SpeakerRequest>> =
        observeDocuments(firestore.collection(congregation.talkRequestsPath), "speaker request", ::toRequestOrNull)
            .map { it.filterNotNull() }

    override suspend fun commit(congregation: CongregationRef, write: ScheduleWrite): Result<Unit> =
        runFirestoreCatching("commit speaker requests") {
            val slots = firestore.collection(congregation.talkSchedulePath)
            val requests = firestore.collection(congregation.talkRequestsPath)
            val batch = firestore.batch()
            write.saveSlots.forEach { batch.set(slots.document(it.id), it.toFirestoreFields()) }
            write.deleteSlotDates.forEach { batch.delete(slots.document(it.toString())) }
            write.saveRequests.forEach { batch.set(requests.document(it.id), it.toFirestoreFields()) }
            write.deleteRequestIds.forEach { batch.delete(requests.document(it)) }
            batch.commit().awaitResult()
            Unit
        }
}

// Ein ungültiges Dokument wird übersprungen, damit es nicht die ganze Übersicht blockiert.
private fun toRequestOrNull(document: DocumentSnapshot): SpeakerRequest? = runCatching {
    SpeakerRequest(
        id = document.id,
        createdOn = LocalDate.parse(document.getString("createdOn").orEmpty()),
        note = document.getString("note"),
        contactAttempts = (document.get("contactAttempts") as? List<*>).orEmpty().mapNotNull { attempt ->
            (attempt as? Map<*, *>)?.let {
                contactAttemptOrNull(it["date"] as? String, it["kind"] as? String, it["note"] as? String)
            }
        },
        speakerMemberId = document.getString("speakerMemberId"),
        speakerLastName = document.getString("speakerLastName"),
        speakerFirstName = document.getString("speakerFirstName"),
        externalSpeakerId = document.getString("externalSpeakerId"),
        speakerCongregation = document.getString("speakerCongregation"),
        talkNumber = document.getLong("talkNumber")?.toInt(),
        talkTitle = document.getString("talkTitle"),
    )
}.onFailure { logFirestoreFailure("parse speaker request ${document.id}", it) }.getOrNull()

private fun SpeakerRequest.toFirestoreFields(): Map<String, Any?> = mapOf(
    "createdOn" to createdOn.toString(),
    "note" to note,
    "contactAttempts" to contactAttempts.map {
        mapOf("date" to it.date.toString(), "kind" to it.kind.name, "note" to it.note)
    },
    "speakerMemberId" to speakerMemberId,
    "speakerLastName" to speakerLastName,
    "speakerFirstName" to speakerFirstName,
    "externalSpeakerId" to externalSpeakerId,
    "speakerCongregation" to speakerCongregation,
    "talkNumber" to talkNumber,
    "talkTitle" to talkTitle,
)
