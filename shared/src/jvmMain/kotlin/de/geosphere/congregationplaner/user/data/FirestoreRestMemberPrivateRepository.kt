package de.geosphere.congregationplaner.user.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

internal class FirestoreRestMemberPrivateRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : MemberPrivateRepository {
    private val client =
        FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing private member details.")

    override fun observe(congregation: CongregationRef, uid: String): Flow<MemberPrivateDetails?> = pollingFlow {
        client.requestOrNull("GET", client.documentUrl(congregation.memberPrivatePath, uid))
            ?.let { parseDocument(it).fields.toDetails() }
    }

    // PATCH ohne updateMask ersetzt das Dokument und legt es bei Bedarf an.
    override suspend fun save(congregation: CongregationRef, uid: String, details: MemberPrivateDetails): Result<Unit> =
        runCatchingIo {
            client.request(
                "PATCH",
                client.documentUrl(congregation.memberPrivatePath, uid),
                firestoreFields(details.toFields()),
            )
            Unit
        }

    override suspend fun delete(congregation: CongregationRef, uid: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.memberPrivatePath, uid))
        Unit
    }
}

// Einträge der Liste, die keine Map sind, werden übersprungen; fehlende Felder sind leer.
private fun JsonObject.toDetails(): MemberPrivateDetails = MemberPrivateDetails(
    street = optionalStringField("street").orEmpty(),
    postalCode = optionalStringField("postalCode").orEmpty(),
    city = optionalStringField("city").orEmpty(),
    emergencyContacts = get("emergencyContacts")?.takeIf { it.isJsonObject }?.asJsonObject
        ?.getAsJsonObject("arrayValue")
        ?.getAsJsonArray("values")
        ?.mapNotNull { value ->
            value.asJsonObject.getAsJsonObject("mapValue")?.getAsJsonObject("fields")?.let { fields ->
                EmergencyContact(
                    name = fields.optionalStringField("name").orEmpty(),
                    relation = fields.optionalStringField("relation").orEmpty(),
                    phone = fields.optionalStringField("phone").orEmpty(),
                )
            }
        }
        .orEmpty(),
)

// Eine leere Beziehung wird weggelassen; die Rules erlauben sie nur als optionales Feld.
private fun MemberPrivateDetails.toFields(): JsonObject = JsonObject().apply {
    add("street", firestoreString(street))
    add("postalCode", firestoreString(postalCode))
    add("city", firestoreString(city))
    val contacts = JsonArray().apply {
        emergencyContacts.forEach { contact ->
            val fields = JsonObject().apply {
                add("name", firestoreString(contact.name))
                if (contact.relation.isNotEmpty()) add("relation", firestoreString(contact.relation))
                add("phone", firestoreString(contact.phone))
            }
            add(JsonObject().apply { add("mapValue", JsonObject().apply { add("fields", fields) }) })
        }
    }
    add("emergencyContacts", JsonObject().apply { add("arrayValue", JsonObject().apply { add("values", contacts) }) })
}
