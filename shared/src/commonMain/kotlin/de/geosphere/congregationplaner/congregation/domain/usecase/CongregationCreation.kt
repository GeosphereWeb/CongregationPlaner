package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository

/** Legt die Versammlung an, sofern der Name gültig und im Kreis [circuitIds] noch frei ist. */
internal suspend fun CongregationMetadataRepository.createWithFreeName(
    circuitIds: List<String>,
    name: String,
    createdBy: String,
    firstAdmin: NewCongregationAdmin?,
): Result<CongregationMetadata> {
    CongregationName.validate(name)?.let { return Result.failure(it) }
    val trimmed = name.trim()
    return findNameOwner(circuitIds, trimmed).fold(
        onSuccess = { owner ->
            if (owner != null) {
                Result.failure(CongregationException.NameTaken())
            } else {
                create(circuitIds, CongregationMetadata(name = trimmed), createdBy, firstAdmin)
            }
        },
        onFailure = { Result.failure(it) },
    )
}
