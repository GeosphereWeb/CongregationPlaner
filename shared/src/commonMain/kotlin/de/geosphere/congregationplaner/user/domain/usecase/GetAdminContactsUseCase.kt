package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.domain.repository.AdminContactRepository
import dev.zacsweers.metro.Inject

/** Die Admins der Versammlung, an die sich ein noch nicht freigegebener Nutzer wenden kann. */
@Inject
class GetAdminContactsUseCase(private val repository: AdminContactRepository) {
    suspend operator fun invoke(congregation: CongregationRef): Result<List<AdminContact>> =
        repository.getAdminContacts(congregation).map { contacts -> contacts.sortedBy { it.fullName.lowercase() } }
}
