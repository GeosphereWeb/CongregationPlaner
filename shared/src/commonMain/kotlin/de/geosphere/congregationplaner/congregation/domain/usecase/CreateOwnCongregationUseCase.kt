package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import dev.zacsweers.metro.Inject

/**
 * Ein angemeldeter Nutzer legt im Login eine Versammlung im Kreis [circuitIds] an und wird ihr erster, freigegebener
 * Admin [firstAdmin] (ADR-0018). Das setzt eine bestätigte E-Mail-Adresse voraus; der Name muss im Kreis frei sein.
 */
@Inject
class CreateOwnCongregationUseCase(private val repository: CongregationMetadataRepository) {
    suspend operator fun invoke(
        circuitIds: List<String>,
        name: String,
        firstAdmin: NewCongregationAdmin,
    ): Result<CongregationMetadata> {
        if (!firstAdmin.emailVerified) return Result.failure(CongregationException.EmailNotVerified())
        return repository.createWithFreeName(circuitIds, name, createdBy = firstAdmin.uid, firstAdmin = firstAdmin)
    }
}
