package de.geosphere.congregationplaner.organization.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Die Organisation (Region bis Kreis) pflegen nur globale Administratoren; die Rules setzen das zusätzlich durch.
 */
@Inject
class OrganizationPermissionPolicy {
    fun canManage(user: AppUser): Boolean = user.isGlobalAdmin
}
