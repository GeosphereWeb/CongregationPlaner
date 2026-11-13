package de.geosphere.congregationplaner.congregation.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.congregation.domain.model.CongregationException
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.CongregationName
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import de.geosphere.congregationplaner.congregation.domain.repository.CongregationMetadataRepository
import de.geosphere.congregationplaner.user.domain.model.toNameProfile
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

    /** Wie oben; der angemeldete [user] wird erster Admin, sein Anzeigename liefert Vor- und Nachnamen. */
    suspend operator fun invoke(
        circuitIds: List<String>,
        name: String,
        user: FirebaseUser,
    ): Result<CongregationMetadata> = invoke(circuitIds, name, user.toFirstAdmin())

    /** Der eingegebene Name, gekürzt auf die erlaubte Länge. */
    fun limitName(name: String): String = name.take(CongregationName.MAX_LENGTH)
}

private fun FirebaseUser.toFirstAdmin(): NewCongregationAdmin {
    val names = displayName.toNameProfile()
    return NewCongregationAdmin(
        uid = uid,
        email = email,
        displayName = displayName,
        firstName = names.firstName,
        lastName = names.lastName,
        emailVerified = emailVerified,
    )
}
