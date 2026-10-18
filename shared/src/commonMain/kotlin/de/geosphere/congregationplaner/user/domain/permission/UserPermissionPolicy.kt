package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Nutzerverwaltung.
 */
@Inject
class UserPermissionPolicy {

    // Darf der User andere Nutzer freigeben und Rollen vergeben?
    fun canManageUsers(user: AppUser): Boolean = user.approved && user.role == UserRole.ADMIN
}
