package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.permission.OrganizationPermissionPolicy
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/** Legt eine neue Einheit an (leere ID) oder speichert eine bestehende; nur für globale Admins. */
@Inject
class SaveOrgUnitUseCase(private val repository: OrgUnitRepository, private val policy: OrganizationPermissionPolicy) {
    suspend operator fun invoke(
        user: AppUser,
        level: OrgLevel,
        parentIds: List<String>,
        unit: OrgUnit,
    ): Result<OrgUnit> {
        require(level.isOrgUnit) { "$level is no organization unit." }
        if (!policy.canManage(user)) return Result.failure(IllegalStateException(MISSING_PERMISSION))
        val cleaned = unit.copy(
            name = unit.name.trim(),
            // Nur Kreise haben einen Kreisaufseher.
            overseerName = unit.overseerName.trim().takeIf { level == OrgLevel.CIRCUIT }.orEmpty(),
        )
        val error = when {
            cleaned.name.isEmpty() -> OrgUnitException.MissingName()
            maxOf(cleaned.name.length, cleaned.overseerName.length) > OrgUnit.MAX_NAME_LENGTH ->
                OrgUnitException.NameTooLong()
            else -> null
        }
        if (error != null) return Result.failure(error)
        return if (cleaned.id.isBlank()) {
            repository.create(level, parentIds, cleaned)
        } else {
            repository.update(level, parentIds, cleaned)
        }
    }
}
