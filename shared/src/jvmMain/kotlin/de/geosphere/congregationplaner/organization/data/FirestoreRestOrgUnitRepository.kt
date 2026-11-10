package de.geosphere.congregationplaner.organization.data

import com.google.gson.JsonObject
import de.geosphere.congregationplaner.auth.data.FirebaseAuthManager
import de.geosphere.congregationplaner.firestore.FirestoreRestClient
import de.geosphere.congregationplaner.firestore.FirestoreRestDocument
import de.geosphere.congregationplaner.firestore.firestoreFields
import de.geosphere.congregationplaner.firestore.firestoreString
import de.geosphere.congregationplaner.firestore.listAllDocuments
import de.geosphere.congregationplaner.firestore.parseDocument
import de.geosphere.congregationplaner.firestore.parseDocuments
import de.geosphere.congregationplaner.firestore.pollingFlow
import de.geosphere.congregationplaner.firestore.runCatchingIo
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import kotlinx.coroutines.flow.Flow
import java.net.http.HttpClient

actual fun createOrgUnitRepository(): OrgUnitRepository = FirestoreRestOrgUnitRepository()

internal class FirestoreRestOrgUnitRepository(
    httpClient: HttpClient = FirestoreRestClient.defaultHttpClient(),
    idTokenProvider: () -> String? = FirebaseAuthManager::currentUserIdToken,
) : OrgUnitRepository {
    private val client = FirestoreRestClient(httpClient, idTokenProvider, "Sign in before changing the organization.")

    override fun observe(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> = pollingFlow {
        client.listAllDocuments(level.collectionPath(parentIds)).map { it.toOrgUnit() }
    }

    // POST ohne documentId lässt Firestore die ID vergeben.
    override suspend fun create(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        runCatchingIo {
            val response = client.request(
                "POST",
                client.collectionUrl(level.collectionPath(parentIds)),
                unit.toFirestoreJson(level),
            )
            unit.copy(id = parseDocument(response).id)
        }

    // currentDocument.exists verhindert, dass ein PATCH auf eine gelöschte Einheit sie neu anlegt.
    override suspend fun update(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        runCatchingIo {
            require(unit.id.isNotBlank()) { "An ID is required to update a ${level.collection}." }
            val mask = client.updateMask(unit.firestoreFields(level).keys.toList())
            client.request(
                "PATCH",
                "${documentUrl(level, parentIds, unit.id)}?$mask&currentDocument.exists=true",
                unit.toFirestoreJson(level),
            )
            unit
        }

    override suspend fun delete(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit> = runCatchingIo {
        require(unit.id.isNotBlank()) { "An ID is required to delete a ${level.collection}." }
        val documentUrl = documentUrl(level, parentIds, unit.id)
        // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Einheiten zurück.
        val children = client.request("GET", "$documentUrl/${checkNotNull(level.child).collection}?pageSize=1")
        if (parseDocuments(children).isNotEmpty()) throw OrgUnitException.HasChildren(unit.displayName)
        client.request("DELETE", documentUrl)
        Unit
    }

    private fun documentUrl(level: OrgLevel, parentIds: List<String>, id: String): String =
        client.documentUrl(level.collectionPath(parentIds), id)

    private fun FirestoreRestDocument.toOrgUnit(): OrgUnit = OrgUnit(
        id = id,
        name = fields.stringField(ORG_UNIT_NAME_FIELD),
        overseerName = fields.stringField(ORG_UNIT_OVERSEER_FIELD),
    )

    private fun OrgUnit.toFirestoreJson(level: OrgLevel): String = firestoreFields(
        JsonObject().apply { firestoreFields(level).forEach { (name, value) -> add(name, firestoreString(value)) } },
    )
}
