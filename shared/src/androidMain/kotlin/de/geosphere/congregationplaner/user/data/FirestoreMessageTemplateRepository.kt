package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.logFirestoreFailure
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun createMessageTemplateRepository(): MessageTemplateRepository =
    FirestoreMessageTemplateRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreMessageTemplateRepository(private val firestore: FirebaseFirestore) :
    MessageTemplateRepository {
    override fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>> =
        callbackFlow {
            val registration = document(congregation, uid).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logFirestoreFailure("observe message templates", error)
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.takeIf { it.exists() }?.toTemplates().orEmpty())
            }
            awaitClose { registration.remove() }
        }

    override suspend fun saveTemplates(
        congregation: CongregationRef,
        uid: String,
        templates: List<MessageTemplate>,
    ): Result<Unit> = runFirestoreCatching("save message templates") {
        val fields = mapOf("templates" to templates.map { mapOf("name" to it.name, "text" to it.text) })
        document(congregation, uid).set(fields).awaitResult()
        Unit
    }

    private fun document(congregation: CongregationRef, uid: String): DocumentReference =
        firestore.collection(congregation.memberSettingsPath(uid)).document(MESSAGE_TEMPLATES_DOCUMENT_ID)
}

// Einträge ohne Name oder Text werden übersprungen.
private fun DocumentSnapshot.toTemplates(): List<MessageTemplate> = (get("templates") as? List<*>).orEmpty()
    .mapNotNull { it as? Map<*, *> }
    .mapNotNull { template ->
        val name = template["name"] as? String
        val text = template["text"] as? String
        if (name != null && text != null) MessageTemplate(name, text) else null
    }
