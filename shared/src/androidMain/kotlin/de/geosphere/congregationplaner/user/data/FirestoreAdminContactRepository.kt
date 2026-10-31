package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository

internal class FirestoreAdminContactRepository(private val firestore: FirebaseFirestore) : AdminContactRepository {
    override suspend fun getAdminContacts(congregation: CongregationRef): Result<List<AdminContact>> =
        runFirestoreCatching("get admin contacts") {
            // Source.SERVER umgeht den Cache, damit neue Admins sofort erscheinen.
            firestore.collection(congregation.adminContactsPath).get(Source.SERVER).awaitResult().documents.map {
                AdminContact(it.id, it.getString("firstName").orEmpty(), it.getString("lastName").orEmpty())
            }
        }

    override suspend fun putAdminContact(congregation: CongregationRef, contact: AdminContact): Result<Unit> =
        runFirestoreCatching("put admin contact") {
            firestore.collection(congregation.adminContactsPath).document(contact.uid)
                .set(mapOf("firstName" to contact.firstName, "lastName" to contact.lastName))
                .awaitResult()
            Unit
        }

    override suspend fun deleteAdminContact(congregation: CongregationRef, uid: String): Result<Unit> =
        runFirestoreCatching("delete admin contact") {
            firestore.collection(congregation.adminContactsPath).document(uid).delete().awaitResult()
            Unit
        }
}
