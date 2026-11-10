package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/** Warum die Admin-Rolle eines Mitglieds nicht entfernt werden darf. */
enum class AdminRoleLock {
    /** Der Admin sperrt sich nicht selbst aus. */
    OWN_ROLE,

    /** Eine Versammlung behält mindestens einen freigegebenen Admin. */
    LAST_ADMIN,
}

/**
 * Berechtigungen für die Nutzerverwaltung einer Versammlung.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class UserPermissionPolicy {

    // Darf der User die Mitglieder seiner Versammlung freigeben und Rollen vergeben? Nur Versammlungsadmins (ADR-0013).
    fun canManageUsers(user: AppUser): Boolean = user.hasRole(CongregationRole.CONGREGATION_ADMIN)

    // Darf der User die Mitglieder seiner Versammlung lesen (nicht ändern)? Das dürfen auch die Planer (ADR-0012).
    fun canViewMembers(user: AppUser): Boolean = canManageUsers(user) || memberReadingRoles.any(user::hasRole)

    // Die Rollen, die Mitglieder lesen dürfen; der Vortragsplanungs-Helfer braucht die Nummern der Redner.
    private val memberReadingRoles = setOf(
        CongregationRole.TALK_PLANNER,
        CongregationRole.TASK_PLANNER,
        CongregationRole.TALK_HELPER,
    )

    // Darf der User dieses Mitglied freigeben oder entfernen? Das eigene Mitglied ist ausgenommen, damit sich kein
    // Admin aussperrt.
    fun canEditMember(user: AppUser, member: AppUser): Boolean = canManageUsers(user) && member.uid != user.uid

    // Darf der User die Rollen dieses Mitglieds ändern? Anders als die Freigabe auch die eigenen; die Admin-Rolle
    // schützt zusätzlich [adminRoleLock].
    @Suppress("UnusedParameter")
    fun canEditRoles(user: AppUser, member: AppUser): Boolean = canManageUsers(user)

    /** Darf der User [role] bei [member] setzen oder entfernen? [members] sind alle Mitglieder der Versammlung. */
    fun canToggleRole(user: AppUser, member: AppUser, role: CongregationRole, members: List<AppUser>): Boolean =
        canEditRoles(user, member) &&
            (role != CongregationRole.CONGREGATION_ADMIN || adminRoleLock(user, member, members) == null)

    /** Warum die Admin-Rolle von [member] bestehen bleiben muss, sonst `null`. */
    fun adminRoleLock(user: AppUser, member: AppUser, members: List<AppUser>): AdminRoleLock? = when {
        CongregationRole.CONGREGATION_ADMIN !in member.roles -> null
        member.uid == user.uid -> AdminRoleLock.OWN_ROLE
        member.approved && members.count { it.hasRole(CongregationRole.CONGREGATION_ADMIN) } <= 1 ->
            AdminRoleLock.LAST_ADMIN
        else -> null
    }

    // Darf der User die persönlichen Angaben pflegen? Das betrifft keinen Zugang, daher auch beim eigenen Mitglied.
    @Suppress("UnusedParameter")
    fun canEditProfile(user: AppUser, member: AppUser): Boolean = canManageUsers(user)

    // Darf der User Personen ohne eigenes Konto (z. B. Kinder) erfassen?
    fun canCreateMemberWithoutAccount(user: AppUser): Boolean = canManageUsers(user)
}
