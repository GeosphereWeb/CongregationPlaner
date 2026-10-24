package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerRepository
import kotlinx.coroutines.flow.Flow

actual fun createExternalSpeakerRepository(): ExternalSpeakerRepository =
    FirestoreExternalSpeakerRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreExternalSpeakerRepository(private val firestore: FirebaseFirestore) :
    ExternalSpeakerRepository {
    override fun observeAll(congregation: CongregationRef): Flow<List<ExternalSpeaker>> = observeDocuments(
        firestore.collection(congregation.externalSpeakersPath),
        "external speaker",
        ::toExternalSpeaker,
    )

    override suspend fun save(congregation: CongregationRef, speaker: ExternalSpeaker): Result<ExternalSpeaker> =
        runFirestoreCatching("save external speaker") {
            val collection = firestore.collection(congregation.externalSpeakersPath)
            // document() ohne Pfad vergibt eine zufällige ID.
            val document = if (speaker.id.isBlank()) collection.document() else collection.document(speaker.id)
            val saved = speaker.copy(id = document.id)
            document.set(saved.toFirestoreFields()).awaitResult()
            saved
        }

    override suspend fun delete(congregation: CongregationRef, id: String): Result<Unit> =
        runFirestoreCatching("delete external speaker") {
            firestore.collection(congregation.externalSpeakersPath).document(id).delete().awaitResult()
        }
}

private fun toExternalSpeaker(document: DocumentSnapshot): ExternalSpeaker = ExternalSpeaker(
    id = document.id,
    lastName = document.getString("lastName").orEmpty(),
    firstName = document.getString("firstName").orEmpty(),
    congregationName = document.getString("congregationName").orEmpty(),
)

private fun ExternalSpeaker.toFirestoreFields(): Map<String, Any> = mapOf(
    "lastName" to lastName,
    "firstName" to firstName,
    "congregationName" to congregationName,
)
