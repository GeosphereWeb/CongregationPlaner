package de.geosphere.congregationplaner.congregation.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.model.decodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.model.encodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.newDocumentId
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringArrayField
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.data.allFields
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import java.net.http.HttpClient

internal class FirestoreRestCongregationMetadataRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : CongregationMetadataRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing congregation data.")

    override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> = pollingFlow {
        val url = client.collectionUrl(congregationCollectionPath(circuitIds))
        parseDocuments(client.request("GET", "$url?pageSize=1000")).map { it.toCongregation() }
    }

    override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> = runCatchingIo {
        val path = congregationNamesCollectionPath(circuitIds)
        client.requestOrNull("GET", client.documentUrl(path, CongregationName.key(name)))
            ?.let { parseDocument(it).fields.optionalStringField("congregationId") }
    }

    override suspend fun create(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
        createdBy: String,
        firstAdmin: NewCongregationAdmin?,
    ): Result<CongregationMetadata> = runCatchingIo {
        val id = newDocumentId()
        val documentPath = "${congregationCollectionPath(circuitIds)}/$id"
        val namePath = "${congregationNamesCollectionPath(circuitIds)}/${CongregationName.key(congregation.name)}"
        val fields = congregation.toFirestoreFields().apply { add("createdBy", firestoreString(createdBy)) }
        // Ein atomarer Schreibzugriff: Versammlung, Namensdokument und erster Admin entstehen zusammen oder gar nicht.
        val writes = buildList {
            add(client.writeUpdate(documentPath, fields))
            add(client.writeUpdate(namePath, JsonObject().apply { add("congregationId", firestoreString(id)) }))
            firstAdmin?.let { admin ->
                add(
                    client.writeUpdate(
                        "$documentPath/${CongregationRef.MEMBERS_SUBCOLLECTION}/${admin.uid}",
                        admin.toMember().allFields(),
                    ),
                )
            }
        }
        client.commit(writes)
        congregation.copy(id = id)
    }

    override suspend fun save(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> = runCatchingIo {
        require(congregation.id.isNotBlank()) { "A congregation ID is required to save a congregation." }
        val path = congregationCollectionPath(circuitIds)
        val documentUrl = client.documentUrl(path, congregation.id)
        val oldKey = client.requestOrNull("GET", documentUrl)
            ?.let { parseDocument(it).fields.optionalStringField("name") }?.let(CongregationName::key)
        val newKey = CongregationName.key(congregation.name)
        if (oldKey == null || oldKey == newKey) {
            client.request(
                method = "PATCH",
                url = "$documentUrl?${client.updateMask(FIRESTORE_FIELD_NAMES)}",
                body = firestoreFields(congregation.toFirestoreFields()),
            )
        } else {
            // Das Namensdokument zieht mit um: der alte Name wird frei, der neue vergeben.
            val namesPath = congregationNamesCollectionPath(circuitIds)
            client.commit(
                listOf(
                    client.writeUpdate(
                        "$path/${congregation.id}",
                        congregation.toFirestoreFields(),
                        FIRESTORE_FIELD_NAMES,
                    ),
                    client.writeDelete("$namesPath/$oldKey"),
                    client.writeUpdate(
                        "$namesPath/$newKey",
                        JsonObject().apply { add("congregationId", firestoreString(congregation.id)) },
                    ),
                ),
            )
        }
        congregation
    }

    override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> = runCatchingIo {
        require(congregationId.isNotBlank()) { "A congregation ID is required to delete a congregation." }
        val path = congregationCollectionPath(circuitIds)
        val name = client.requestOrNull("GET", client.documentUrl(path, congregationId))
            ?.let { parseDocument(it).fields.optionalStringField("name") }
        val writes = buildList {
            add(client.writeDelete("$path/$congregationId"))
            name?.let {
                add(client.writeDelete("${congregationNamesCollectionPath(circuitIds)}/${CongregationName.key(it)}"))
            }
        }
        client.commit(writes)
    }

    private fun FirestoreRestDocument.toCongregation(): CongregationMetadata = CongregationMetadata(
        id = id,
        name = fields.stringField("name"),
        midweekMeetingTime = fields.timeField("midweek_meeting_time"),
        midweekMeetingWeekday = fields.intField("midweek_meeting_weekday"),
        weekendMeetingTime = fields.timeField("weekend_meeting_time"),
        weekendMeetingWeekday = fields.intField("weekend_meeting_weekday"),
        yearlyMeetingTimes = fields.stringArrayField("yearly_meeting_times").decodeYearlyMeetingTimes(),
    )

    private fun CongregationMetadata.toFirestoreFields(): JsonObject = JsonObject().apply {
        add("name", firestoreString(name))
        add("midweek_meeting_time", firestoreString(midweekMeetingTime.toString()))
        add("midweek_meeting_weekday", firestoreInteger(midweekMeetingWeekday))
        add("weekend_meeting_time", firestoreString(weekendMeetingTime.toString()))
        add("weekend_meeting_weekday", firestoreInteger(weekendMeetingWeekday))
        add("yearly_meeting_times", firestoreStringArray(yearlyMeetingTimes.encodeYearlyMeetingTimes()))
    }

    private fun firestoreInteger(value: Int): JsonObject = JsonObject().apply {
        addProperty("integerValue", value)
    }

    private fun JsonObject.intField(name: String): Int {
        val value = get(name) ?: return 1
        val integer = value.asJsonObject.takeIf { it.has("integerValue") }
            ?.get("integerValue")?.asString?.toIntOrNull()
            ?: throw IllegalArgumentException(
                "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday).",
            )
        require(integer in 1..7) {
            "Firestore field '$name' must be an integer from 1 (Monday) to 7 (Sunday)."
        }
        return integer
    }

    private fun JsonObject.timeField(name: String): LocalTime {
        val value = stringField(name)
        return try {
            LocalTime.parse(value)
        } catch (exception: IllegalArgumentException) {
            val message = "Firestore field '$name' must be an ISO time string such as '19:00'."
            throw IllegalArgumentException(message, exception)
        }
    }

    private companion object {
        val FIRESTORE_FIELD_NAMES = listOf(
            "name",
            "midweek_meeting_time",
            "midweek_meeting_weekday",
            "weekend_meeting_time",
            "weekend_meeting_weekday",
            "yearly_meeting_times",
        )
    }
}
