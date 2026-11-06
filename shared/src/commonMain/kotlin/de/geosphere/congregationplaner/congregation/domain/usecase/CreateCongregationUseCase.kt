package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject

/**
 * Legt eine Versammlung im Kreis [circuitIds] an. Der Name muss im Kreis noch frei sein. Mit [firstAdmin] wird der
 * Ersteller zum ersten, freigegebenen Admin der Versammlung; das setzt eine bestätigte E-Mail-Adresse voraus.
 * Globale Admins legen ohne [firstAdmin] an und werden nicht Mitglied (ADR-0013).
 */
@Inject
class CreateCongregationUseCase(private val repository: CongregationMetadataRepository) {
    suspend operator fun invoke(
        circuitIds: List<String>,
        name: String,
        createdBy: String,
        firstAdmin: NewCongregationAdmin? = null,
    ): Result<CongregationMetadata> {
        CongregationName.validate(name)?.let { return Result.failure(it) }
        if (firstAdmin != null && !firstAdmin.emailVerified) {
            return Result.failure(CongregationException.EmailNotVerified())
        }
        val trimmed = name.trim()
        val owner = repository.findNameOwner(circuitIds, trimmed).getOrElse { return Result.failure(it) }
        if (owner != null) return Result.failure(CongregationException.NameTaken())
        return repository.create(circuitIds, CongregationMetadata(name = trimmed), createdBy, firstAdmin)
    }
}
