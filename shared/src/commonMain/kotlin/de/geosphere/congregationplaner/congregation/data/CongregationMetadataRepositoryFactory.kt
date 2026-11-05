package de.geosphere.congregationplaner.congregation.data

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

expect fun createCongregationMetadataRepository(): CongregationMetadataRepository

/** Die Namensdokumente der Versammlungen unter dem Kreis [circuitIds] (siehe `CongregationName`). */
internal fun congregationNamesCollectionPath(circuitIds: List<String>): String =
    "${OrgLevel.CIRCUIT.collectionPath(circuitIds.dropLast(1))}/${circuitIds.last()}/congregationNames"

/** Das Mitgliedsdokument des ersten Admins: freigegeben, Rolle Versammlungsadmin, Namen aus dem Auth-Profil. */
internal fun NewCongregationAdmin.toMember(): AppUser = AppUser(
    uid = uid,
    email = email,
    displayName = displayName,
    roles = setOf(CongregationRole.CONGREGATION_ADMIN),
    approved = true,
    profile = MemberProfile(firstName = firstName, lastName = lastName),
)

/** Die Versammlungs-Collection unter dem Kreis [circuitIds]. */
internal fun congregationCollectionPath(circuitIds: List<String>): String =
    OrgLevel.CONGREGATION.collectionPath(circuitIds)

fun unavailableCongregationMetadataRepository(platform: String): CongregationMetadataRepository =
    object : CongregationMetadataRepository {
        override fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>> = flow {
            throw UnsupportedOperationException("Firestore is not configured for $platform.")
        }

        override suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override suspend fun create(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
            createdBy: String,
            firstAdmin: NewCongregationAdmin?,
        ): Result<CongregationMetadata> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override suspend fun save(
            circuitIds: List<String>,
            congregation: CongregationMetadata,
        ): Result<CongregationMetadata> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))

        override suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit> =
            Result.failure(UnsupportedOperationException("Firestore is not configured for $platform."))
    }
