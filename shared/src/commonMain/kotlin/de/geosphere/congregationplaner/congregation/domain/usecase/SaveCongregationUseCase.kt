package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.permission.CongregationPermissionPolicy
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** [user] speichert Änderungen einer bestehenden Versammlung; ein neuer Name muss im Kreis noch frei sein. */
@Inject
class SaveCongregationUseCase(
    private val repository: CongregationMetadataRepository,
    private val policy: CongregationPermissionPolicy,
) {
    suspend operator fun invoke(
        user: AppUser,
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> {
        require(congregation.id.isNotBlank()) { "A new congregation is created with CreateCongregationUseCase." }
        if (!policy.canEdit(user, congregation)) return Result.failure(CongregationException.NotAllowed())
        CongregationName.validate(congregation.name)?.let { return Result.failure(it) }
        val trimmed = congregation.copy(name = congregation.name.trim())
        val owner = repository.findNameOwner(circuitIds, trimmed.name).getOrElse { return Result.failure(it) }
        if (owner != null && owner != trimmed.id) return Result.failure(CongregationException.NameTaken())
        return repository.save(circuitIds, trimmed)
    }
}
