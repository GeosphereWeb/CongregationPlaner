package de.geosphere.congregationplaner.organization.domain.usecase

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.organization.domain.repository.OrgUnitRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.permission.OrganizationPermissionPolicy
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Die Einheiten einer Ebene, nach Namen sortiert. */
@Inject
class ObserveOrgUnitsUseCase(private val repository: OrgUnitRepository) {
    operator fun invoke(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>> {
        require(level.isOrgUnit) { "$level is no organization unit." }
        return repository.observe(level, parentIds).map { units -> units.sortedBy { it.displayName.lowercase() } }
    }
}

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

private const val MISSING_PERMISSION = "Dafür fehlt dir die Berechtigung."
