package de.geosphere.congregationplaner.congregation.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.decodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.model.encodeYearlyMeetingTimes
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringArrayField
import de.geosphere.congregationplaner.firestore.stringField
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalTime
import java.net.http.HttpClient

internal class FirestoreRestCongregationMetadataRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : CongregationMetadataRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing congregation data.")

    override fun observeAll(): Flow<List<CongregationMetadata>> = pollingFlow {
        parseDocuments(client.request("GET", "${client.collectionUrl(CONGREGATION_COLLECTION_PATH)}?pageSize=1000"))
            .map { it.toCongregation() }
    }

    override suspend fun save(congregation: CongregationMetadata): Result<CongregationMetadata> = runCatchingIo {
        val id = if (congregation.id.isBlank()) {
            val url = client.collectionUrl(CONGREGATION_COLLECTION_PATH)
            parseDocument(client.request("POST", url, congregation.toFirestoreJson())).id
        } else {
            client.request(
                method = "PATCH",
                url = "${client.documentUrl(CONGREGATION_COLLECTION_PATH, congregation.id)}?" +
                    client.updateMask(FIRESTORE_FIELD_NAMES),
                body = congregation.toFirestoreJson(),
            )
            congregation.id
        }
        congregation.copy(id = id)
    }

    override suspend fun delete(congregationId: String): Result<Unit> = runCatchingIo {
        require(congregationId.isNotBlank()) { "A congregation ID is required to delete a congregation." }
        client.request("DELETE", client.documentUrl(CONGREGATION_COLLECTION_PATH, congregationId))
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

    private fun CongregationMetadata.toFirestoreJson(): String = firestoreFields(
        JsonObject().apply {
            add("name", firestoreString(name))
            add("midweek_meeting_time", firestoreString(midweekMeetingTime.toString()))
            add("midweek_meeting_weekday", firestoreInteger(midweekMeetingWeekday))
            add("weekend_meeting_time", firestoreString(weekendMeetingTime.toString()))
            add("weekend_meeting_weekday", firestoreInteger(weekendMeetingWeekday))
            add("yearly_meeting_times", firestoreStringArray(yearlyMeetingTimes.encodeYearlyMeetingTimes()))
        },
    )

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
