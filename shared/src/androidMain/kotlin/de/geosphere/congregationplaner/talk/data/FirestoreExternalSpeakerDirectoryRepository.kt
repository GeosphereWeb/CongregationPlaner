package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.repository.ExternalSpeakerDirectoryRepository
import kotlinx.coroutines.flow.Flow

actual fun createExternalSpeakerDirectoryRepository(): ExternalSpeakerDirectoryRepository =
    FirestoreExternalSpeakerDirectoryRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreExternalSpeakerDirectoryRepository(private val firestore: FirebaseFirestore) :
    ExternalSpeakerDirectoryRepository {
    override fun observeCircuits(congregation: CongregationRef): Flow<List<ExternalCircuit>> = observeDocuments(
        firestore.collection(congregation.externalCircuitsPath),
        "external circuit",
    ) { ExternalCircuit(id = it.id, name = it.getString("name").orEmpty()) }

    override fun observeCongregations(congregation: CongregationRef): Flow<List<ExternalCongregation>> =
        observeDocuments(
            firestore.collection(congregation.externalCongregationsPath),
            "external congregation",
        ) {
            ExternalCongregation(
                id = it.id,
                circuitId = it.getString("circuitId").orEmpty(),
                name = it.getString("name").orEmpty(),
            )
        }

    override fun observeSpeakers(congregation: CongregationRef): Flow<List<VisitingSpeaker>> = observeDocuments(
        firestore.collection(congregation.visitingSpeakersPath),
        "visiting speaker",
    ) {
        VisitingSpeaker(
            id = it.id,
            congregationId = it.getString("congregationId").orEmpty(),
            lastName = it.getString("lastName").orEmpty(),
            firstName = it.getString("firstName").orEmpty(),
            phone = it.getString("phone").orEmpty(),
            email = it.getString("email").orEmpty(),
            talkNumbers = (it.get("talkNumbers") as? List<*>).orEmpty().mapNotNull { number ->
                (number as? Number)?.toInt()
            },
        )
    }

    override suspend fun saveCircuit(congregation: CongregationRef, circuit: ExternalCircuit) =
        store("save external circuit", congregation.externalCircuitsPath, circuit.id, mapOf("name" to circuit.name)) {
            circuit.copy(id = it)
        }

    override suspend fun saveCongregation(congregation: CongregationRef, externalCongregation: ExternalCongregation) =
        store(
            "save external congregation",
            congregation.externalCongregationsPath,
            externalCongregation.id,
            mapOf("circuitId" to externalCongregation.circuitId, "name" to externalCongregation.name),
        ) { externalCongregation.copy(id = it) }

    override suspend fun saveSpeaker(congregation: CongregationRef, speaker: VisitingSpeaker) = store(
        "save visiting speaker",
        congregation.visitingSpeakersPath,
        speaker.id,
        mapOf(
            "congregationId" to speaker.congregationId,
            "lastName" to speaker.lastName,
            "firstName" to speaker.firstName,
            "phone" to speaker.phone,
            "email" to speaker.email,
            "talkNumbers" to speaker.talkNumbers,
        ),
    ) { speaker.copy(id = it) }

    override suspend fun deleteCircuit(congregation: CongregationRef, id: String) =
        remove("delete external circuit", congregation.externalCircuitsPath, id)

    override suspend fun deleteCongregation(congregation: CongregationRef, id: String) =
        remove("delete external congregation", congregation.externalCongregationsPath, id)

    override suspend fun deleteSpeaker(congregation: CongregationRef, id: String) =
        remove("delete visiting speaker", congregation.visitingSpeakersPath, id)

    private suspend fun <T> store(
        operation: String,
        path: String,
        id: String,
        fields: Map<String, Any>,
        withId: (String) -> T,
    ): Result<T> = runFirestoreCatching(operation) {
        val collection = firestore.collection(path)
        // document() ohne Pfad vergibt eine zufällige ID.
        val document = if (id.isBlank()) collection.document() else collection.document(id)
        document.set(fields).awaitResult()
        withId(document.id)
    }

    private suspend fun remove(operation: String, path: String, id: String): Result<Unit> =
        runFirestoreCatching(operation) { firestore.collection(path).document(id).delete().awaitResult() }
}
