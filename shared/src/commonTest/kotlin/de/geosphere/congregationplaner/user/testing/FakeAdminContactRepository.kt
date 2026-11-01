package de.geosphere.congregationplaner.user.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository

/** In-Memory-[AdminContactRepository]; jeder Aufruf landet in [calls], Fehler lassen sich einstellen. */
internal class FakeAdminContactRepository(
    var contacts: List<AdminContact> = emptyList(),
    private val log: MutableList<String> = mutableListOf(),
) : AdminContactRepository {
    var getResult: Result<List<AdminContact>>? = null
    var putResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    val calls: List<String> get() = log

    override suspend fun getAdminContacts(congregation: CongregationRef): Result<List<AdminContact>> {
        log += "get"
        return getResult ?: Result.success(contacts)
    }

    override suspend fun putAdminContact(congregation: CongregationRef, contact: AdminContact): Result<Unit> {
        log += "put ${contact.uid} ${contact.fullName}"
        return putResult
    }

    override suspend fun deleteAdminContact(congregation: CongregationRef, uid: String): Result<Unit> {
        log += "delete $uid"
        return deleteResult
    }
}
