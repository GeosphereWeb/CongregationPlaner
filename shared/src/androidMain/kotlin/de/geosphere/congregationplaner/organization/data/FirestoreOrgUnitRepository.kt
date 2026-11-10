package de.geosphere.congregationplaner.organization.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.auth.data.AndroidFirestoreProvider
import de.geosphere.congregationplaner.firestore.awaitResult
import de.geosphere.congregationplaner.firestore.observeDocuments
import de.geosphere.congregationplaner.firestore.runFirestoreCatching
import de.geosphere.congregationplaner.firestore.stringField
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import kotlinx.coroutines.flow.Flow

actual fun createOrgUnitRepository(): OrgUnitRepository = FirestoreOrgUnitRepository(AndroidFirestoreProvider.firestore)

internal class FirestoreOrgUnitRepository(private val firestore: FirebaseFirestore) : OrgUnitRepository {
    override fun observe(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> =
        observeDocuments(firestore.collection(level.collectionPath(parentIds)), level.collection, ::toOrgUnit)

    override suspend fun create(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        runOrgUnitCatching("create ${level.collection}") {
            // document() ohne Pfad vergibt eine zufällige ID.
            val document = firestore.collection(level.collectionPath(parentIds)).document()
            document.set(unit.firestoreFields(level)).awaitResult()
            unit.copy(id = document.id)
        }

    override suspend fun update(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit> =
        runOrgUnitCatching("update ${level.collection}") {
            require(unit.id.isNotBlank()) { "An ID is required to update a ${level.collection}." }
            firestore.collection(level.collectionPath(parentIds)).document(unit.id)
                .update(unit.firestoreFields(level))
                .awaitResult()
            unit
        }

    override suspend fun delete(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit> =
        runOrgUnitCatching("delete ${level.collection}") {
            require(unit.id.isNotBlank()) { "An ID is required to delete a ${level.collection}." }
            val document = firestore.collection(level.collectionPath(parentIds)).document(unit.id)
            // Firestore löscht Subcollections nicht mit; ohne diese Prüfung blieben verwaiste Einheiten zurück.
            val childCollection = checkNotNull(level.child).collection
            if (!document.collection(childCollection).limit(1).get().awaitResult().isEmpty) {
                throw OrgUnitException.HasChildren(unit.displayName)
            }
            document.delete().awaitResult()
            Unit
        }

    // Fachliche Fehler (OrgUnitException) werden nicht als Firestore-Fehler geloggt.
    private suspend fun <T> runOrgUnitCatching(operation: String, block: suspend () -> T): Result<T> =
        runFirestoreCatching(operation, isExpected = { it is OrgUnitException }, block = block)
}

private fun toOrgUnit(document: DocumentSnapshot): OrgUnit = OrgUnit(
    id = document.id,
    name = document.stringField(ORG_UNIT_NAME_FIELD),
    overseerName = document.stringField(ORG_UNIT_OVERSEER_FIELD),
)
