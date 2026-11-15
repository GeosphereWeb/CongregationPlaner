package de.geosphere.congregationplaner.notification.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.firestoreStringArray
import de.geosphere.congregationplaner.firestore.newDocumentId
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import java.net.http.HttpClient

actual fun createNotificationRepository(): NotificationRepository = FirestoreRestNotificationRepository()

internal class FirestoreRestNotificationRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : NotificationRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before sending notifications.")

    // Über `commit`, weil nur dort die Serverzeit (`REQUEST_TIME`) gesetzt werden kann.
    override suspend fun send(
        congregation: CongregationRef,
        senderUid: String,
        notification: AppNotification,
    ): Result<Unit> = runCatchingIo {
        val fields = JsonObject().apply {
            notification.fieldValues(senderUid).forEach { (name, value) ->
                val list = value as? List<*>
                add(name, list?.let { firestoreStringArray(it.map { item -> "$item" }) } ?: firestoreString("$value"))
            }
        }
        val write = client.writeUpdate("${congregation.notificationsPath}/${newDocumentId()}", fields).apply {
            add("currentDocument", JsonObject().apply { addProperty("exists", false) })
            add(
                "updateTransforms",
                JsonArray().apply {
                    add(
                        JsonObject().apply {
                            addProperty("fieldPath", CREATED_AT_FIELD)
                            addProperty("setToServerValue", "REQUEST_TIME")
                        },
                    )
                },
            )
        }
        client.commit(listOf(write))
    }
}
