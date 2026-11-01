package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Nutzerverwaltung einer Versammlung.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class UserPermissionPolicy {

    // Darf der User die Mitglieder seiner Versammlung freigeben und Rollen vergeben? Nur Versammlungsadmins (ADR-0013).
    fun canManageUsers(user: AppUser): Boolean = user.approved && CongregationRole.CONGREGATION_ADMIN in user.roles

    // Darf der User die Mitglieder seiner Versammlung lesen (nicht ändern)? Das dürfen auch die Planer (ADR-0012).
    fun canViewMembers(user: AppUser): Boolean = canManageUsers(user) ||
        (user.approved && user.roles.any { it == CongregationRole.TALK_PLANNER || it == CongregationRole.TASK_PLANNER })

    // Darf der User dieses Mitglied ändern? Das eigene Mitglied ist ausgenommen, damit sich kein Admin aussperrt.
    fun canEditMember(user: AppUser, member: AppUser): Boolean = canManageUsers(user) && member.uid != user.uid

    // Darf der User die Rollen dieses Mitglieds ändern? Anders als die Freigabe auch die eigenen; die eigene
    // Admin-Rolle bleibt dabei bestehen (siehe UserManagementViewModel), damit sich kein Admin aussperrt.
    @Suppress("UnusedParameter")
    fun canEditRoles(user: AppUser, member: AppUser): Boolean = canManageUsers(user)

    // Darf der User die persönlichen Angaben pflegen? Das betrifft keinen Zugang, daher auch beim eigenen Mitglied.
    @Suppress("UnusedParameter")
    fun canEditProfile(user: AppUser, member: AppUser): Boolean = canManageUsers(user)

    // Darf der User Personen ohne eigenes Konto (z. B. Kinder) erfassen?
    fun canCreateMemberWithoutAccount(user: AppUser): Boolean = canManageUsers(user)
}
