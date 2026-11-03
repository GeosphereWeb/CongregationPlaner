package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class FirestoreMemberPrivateRepository(private val firestore: FirebaseFirestore) : MemberPrivateRepository {
    override fun observe(congregation: CongregationRef, uid: String): Flow<MemberPrivateDetails?> = callbackFlow {
        val registration = firestore.collection(congregation.memberPrivatePath)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreFailure("observe member private details", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.let(::toDetails))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun save(congregation: CongregationRef, uid: String, details: MemberPrivateDetails): Result<Unit> =
        runFirestoreCatching("save member private details") {
            firestore.collection(congregation.memberPrivatePath).document(uid).set(details.toFirestoreFields())
                .awaitResult()
            Unit
        }

    override suspend fun delete(congregation: CongregationRef, uid: String): Result<Unit> =
        runFirestoreCatching("delete member private details") {
            firestore.collection(congregation.memberPrivatePath).document(uid).delete().awaitResult()
            Unit
        }
}

// Einträge der Liste, die keine Map sind, werden übersprungen; fehlende Felder sind leer.
private fun toDetails(document: DocumentSnapshot): MemberPrivateDetails = MemberPrivateDetails(
    street = document.getString("street").orEmpty(),
    postalCode = document.getString("postalCode").orEmpty(),
    city = document.getString("city").orEmpty(),
    emergencyContacts = (document.get("emergencyContacts") as? List<*>).orEmpty()
        .mapNotNull { it as? Map<*, *> }
        .map { contact ->
            EmergencyContact(
                name = contact["name"] as? String ?: "",
                relation = contact["relation"] as? String ?: "",
                phone = contact["phone"] as? String ?: "",
            )
        },
)

// Eine leere Beziehung wird weggelassen; die Rules erlauben sie nur als optionales Feld.
private fun MemberPrivateDetails.toFirestoreFields(): Map<String, Any> = mapOf(
    "street" to street,
    "postalCode" to postalCode,
    "city" to city,
    "emergencyContacts" to emergencyContacts.map { contact ->
        buildMap {
            put("name", contact.name)
            if (contact.relation.isNotEmpty()) put("relation", contact.relation)
            put("phone", contact.phone)
        }
    },
)
