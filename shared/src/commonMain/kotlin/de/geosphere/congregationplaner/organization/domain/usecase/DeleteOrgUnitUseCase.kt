package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Löscht eine leere Einheit; nur für globale Admins. */
@Inject
class DeleteOrgUnitUseCase(
    private val repository: OrgUnitRepository,
    private val policy: OrganizationPermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit> {
        require(level.isOrgUnit) { "$level is no organization unit." }
        if (!policy.canManage(user)) return Result.failure(IllegalStateException(MISSING_PERMISSION))
        if (unit.id.isBlank()) return Result.failure(IllegalArgumentException("Die Einheit hat keine ID."))
        return repository.delete(level, parentIds, unit)
    }
}
