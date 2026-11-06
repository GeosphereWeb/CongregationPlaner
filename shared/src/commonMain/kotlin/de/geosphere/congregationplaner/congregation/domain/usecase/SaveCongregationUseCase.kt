package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject

/** Speichert Änderungen einer bestehenden Versammlung; ein neuer Name muss im Kreis noch frei sein. */
@Inject
class SaveCongregationUseCase(private val repository: CongregationMetadataRepository) {
    suspend operator fun invoke(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
    ): Result<CongregationMetadata> {
        require(congregation.id.isNotBlank()) { "A new congregation is created with CreateCongregationUseCase." }
        CongregationName.validate(congregation.name)?.let { return Result.failure(it) }
        val trimmed = congregation.copy(name = congregation.name.trim())
        val owner = repository.findNameOwner(circuitIds, trimmed.name).getOrElse { return Result.failure(it) }
        if (owner != null && owner != trimmed.id) return Result.failure(CongregationException.NameTaken())
        return repository.save(circuitIds, trimmed)
    }
}
