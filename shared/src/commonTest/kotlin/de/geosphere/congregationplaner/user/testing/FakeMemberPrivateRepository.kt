package de.geosphere.congregationplaner.user.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.repository.MemberPrivateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-Memory-[MemberPrivateRepository] je UID; Fehler lassen sich über die `*Result`-Felder einstellen. */
internal class FakeMemberPrivateRepository(details: Map<String, MemberPrivateDetails> = emptyMap()) :
    MemberPrivateRepository {
    val details = MutableStateFlow(details)

    var saveResult: Result<Unit> = Result.success(Unit)
    var deleteResult: Result<Unit> = Result.success(Unit)

    val savedUids = mutableListOf<String>()
    val deletedUids = mutableListOf<String>()
    var requestedCongregation: CongregationRef? = null
        private set

    override fun observe(congregation: CongregationRef, uid: String): Flow<MemberPrivateDetails?> {
        requestedCongregation = congregation
        return details.map { it[uid] }
    }

    override suspend fun save(congregation: CongregationRef, uid: String, details: MemberPrivateDetails): Result<Unit> {
        requestedCongregation = congregation
        return saveResult.onSuccess {
            savedUids += uid
            this.details.value = this.details.value + (uid to details)
        }
    }

    override suspend fun delete(congregation: CongregationRef, uid: String): Result<Unit> {
        requestedCongregation = congregation
        return deleteResult.onSuccess {
            deletedUids += uid
            this.details.value = this.details.value - uid
        }
    }
}
