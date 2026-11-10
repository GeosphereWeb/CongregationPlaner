package de.geosphere.congregationplaner.congregation.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.model.decodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.model.encodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.data.toFirestoreFields
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime

actual fun createCongregationMetadataRepository(): CongregationMetadataRepository =
    FirestoreCongregationMetadataRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreCongregationMetadataRepository(private val firestore: FirebaseFirestore) :
    CongregationMetadataRepository {
    override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> =
        observeDocuments(firestore.collection(congregationCollectionPath(circuitIds)), "congregation", ::toDomain)

    override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> =
        runFirestoreCatching("find congregation name") {
            val snapshot = firestore.collection(uniqueCongregationNamesCollectionPath(circuitIds))
                .document(CongregationName.key(name))
                .get(Source.SERVER)
                .awaitResult()
            if (snapshot.exists()) snapshot.getString("congregationId") else null
        }

    override suspend fun create(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
        createdBy: String,
        firstAdmin: NewCongregationAdmin?,
    ): Result<CongregationMetadata> = runFirestoreCatching("create congregation") {
        // document() ohne Pfad vergibt eine zufällige ID.
        val document = firestore.collection(congregationCollectionPath(circuitIds)).document()
        val nameDocument = firestore.collection(uniqueCongregationNamesCollectionPath(circuitIds))
            .document(CongregationName.key(congregation.name))
        // Ein Batch: entweder entstehen Versammlung, Namensdokument und Admin zusammen oder gar nichts.
        val batch = firestore.batch()
        batch.set(document, congregation.toFirestoreFields() + ("createdBy" to createdBy))
        batch.set(nameDocument, mapOf("congregationId" to document.id))
        firstAdmin?.let { admin ->
            batch.set(
                document.collection(CongregationRef.MEMBERS_SUBCOLLECTION).document(admin.uid),
                admin.toMember().toFirestoreFields(),
            )
        }
        batch.commit().awaitResult()
        congregation.copy(id = document.id)
    }

    override suspend fun save(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> = runFirestoreCatching("save congregation") {
        require(congregation.id.isNotBlank()) { "A congregation ID is required to save a congregation." }
        val reference = firestore.collection(congregationCollectionPath(circuitIds)).document(congregation.id)
        val names = firestore.collection(uniqueCongregationNamesCollectionPath(circuitIds))
        val oldKey = reference.get(Source.SERVER).awaitResult().getString("name")?.let(CongregationName::key)
        val newKey = CongregationName.key(congregation.name)
        if (oldKey == null || oldKey == newKey) {
            reference.update(congregation.toFirestoreFields()).awaitResult()
        } else {
            // Das Namensdokument zieht mit um: der alte Name wird frei, der neue vergeben.
            val batch = firestore.batch()
            batch.update(reference, congregation.toFirestoreFields())
            batch.delete(names.document(oldKey))
            batch.set(names.document(newKey), mapOf("congregationId" to congregation.id))
            batch.commit().awaitResult()
        }
        congregation
    }

    override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> =
        runFirestoreCatching("delete congregation") {
            require(congregationId.isNotBlank()) { "A congregation ID is required to delete a congregation." }
            val reference = firestore.collection(congregationCollectionPath(circuitIds)).document(congregationId)
            val name = reference.get(Source.SERVER).awaitResult().getString("name")
            val names = firestore.collection(uniqueCongregationNamesCollectionPath(circuitIds))
            val batch = firestore.batch()
            batch.delete(reference)
            name?.let { batch.delete(names.document(CongregationName.key(it))) }
            batch.commit().awaitResult()
        }
}

private fun toDomain(document: DocumentSnapshot): CongregationMetadata = CongregationMetadata(
    id = document.id,
    name = document.stringField("name"),
    midweekMeetingTime = document.timeField("midweek_meeting_time"),
    midweekMeetingWeekday = document.intField("midweek_meeting_weekday"),
    weekendMeetingTime = document.timeField("weekend_meeting_time"),
    weekendMeetingWeekday = document.intField("weekend_meeting_weekday"),
    yearlyMeetingTimes = (document.get("yearly_meeting_times") as? List<*>).orEmpty()
        .map { it as? String }.decodeYearlyMeetingTimes(),
)

private fun weekdayMessage(name: String) = "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday)."

private fun timeMessage(name: String) = "Firestore field '$name' must be an ISO time string such as '19:00'."

private fun DocumentSnapshot.intField(name: String): Int = when (val value = get(name)) {
    null -> 1
    is Number -> value.toInt().also { weekday ->
        require(value.toDouble() == weekday.toDouble() && weekday in 1..7) { weekdayMessage(name) }
    }
    else -> throw IllegalArgumentException(weekdayMessage(name))
}

private fun DocumentSnapshot.timeField(name: String): LocalTime = when (val value = get(name)) {
    null -> LocalTime(0, 0)
    is String -> try {
        LocalTime.parse(value)
    } catch (exception: IllegalArgumentException) {
        throw IllegalArgumentException(timeMessage(name), exception)
    }
    else -> throw IllegalArgumentException(timeMessage(name))
}

private fun CongregationMetadata.toFirestoreFields(): Map<String, Any> = mapOf(
    "name" to name,
    "midweek_meeting_time" to midweekMeetingTime.toString(),
    "midweek_meeting_weekday" to midweekMeetingWeekday,
    "weekend_meeting_time" to weekendMeetingTime.toString(),
    "weekend_meeting_weekday" to weekendMeetingWeekday,
    "yearly_meeting_times" to yearlyMeetingTimes.encodeYearlyMeetingTimes(),
)
