package de.geosphere.congregationplaner.user.data.mappers

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole

/** Leitet die Rollen aus den alten Feldern `role` und `permissions` ab (bis ADR-0012 vollständig umgesetzt ist). */
internal fun legacyRoles(role: UserRole, permissions: Set<UserPermission>): Set<CongregationRole> = buildSet {
    if (role == UserRole.CONGREGATION_ADMIN) add(CongregationRole.CONGREGATION_ADMIN)
    if (UserPermission.PLAN_TALKS in permissions) add(CongregationRole.TALK_PLANNER)
}

internal fun AppUser.withLegacyRoles(): AppUser = copy(roles = legacyRoles(role, permissions))
