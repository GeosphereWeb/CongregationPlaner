package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Nutzerverwaltung einer Versammlung.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class UserPermissionPolicy {

    // Darf der User die Mitglieder seiner Versammlung freigeben und Rollen/Berechtigungen vergeben?
    fun canManageUsers(user: AppUser): Boolean =
        user.isGlobalAdmin || (user.approved && user.role == UserRole.CONGREGATION_ADMIN)

    // Darf der User dieses Mitglied ändern? Das eigene Mitglied ist ausgenommen, damit sich kein Admin aussperrt.
    fun canEditMember(user: AppUser, member: AppUser): Boolean = canManageUsers(user) && member.uid != user.uid
}
