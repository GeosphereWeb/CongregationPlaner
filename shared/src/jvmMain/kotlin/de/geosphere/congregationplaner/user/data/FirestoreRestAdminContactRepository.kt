package de.geosphere.congregationplaner.user.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.optionalStringField
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import java.net.http.HttpClient

internal class FirestoreRestAdminContactRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : AdminContactRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before accessing user data.")

    override suspend fun getAdminContacts(congregation: CongregationRef): Result<List<AdminContact>> = runCatchingIo {
        val url = "${client.collectionUrl(congregation.adminContactsPath)}?pageSize=100"
        parseDocuments(client.request("GET", url))
            .map { document ->
                AdminContact(
                    uid = document.id,
                    firstName = document.fields.optionalStringField("firstName").orEmpty(),
                    lastName = document.fields.optionalStringField("lastName").orEmpty(),
                )
            }
    }

    override suspend fun putAdminContact(congregation: CongregationRef, contact: AdminContact): Result<Unit> =
        runCatchingIo {
            // PATCH ohne updateMask legt das Dokument an oder ersetzt es vollständig.
            val fields = JsonObject().apply {
                add("firstName", firestoreString(contact.firstName))
                add("lastName", firestoreString(contact.lastName))
            }
            client.request(
                "PATCH",
                client.documentUrl(congregation.adminContactsPath, contact.uid),
                firestoreFields(fields),
            )
            Unit
        }

    override suspend fun deleteAdminContact(congregation: CongregationRef, uid: String): Result<Unit> = runCatchingIo {
        client.request("DELETE", client.documentUrl(congregation.adminContactsPath, uid))
        Unit
    }
}
