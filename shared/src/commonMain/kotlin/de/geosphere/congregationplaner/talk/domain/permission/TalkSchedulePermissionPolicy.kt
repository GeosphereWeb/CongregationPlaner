package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Vortragsplanung einer Versammlung. Lesen und Planen sind zunächst dasselbe Recht.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class TalkSchedulePermissionPolicy {
    // Versammlungsadmins brauchen PLAN_TALKS nicht, weil bestehende Admins die Berechtigung nicht besitzen.
    fun canPlanTalks(user: AppUser): Boolean = user.isGlobalAdmin ||
        (
            user.approved &&
                (user.role == UserRole.CONGREGATION_ADMIN || UserPermission.PLAN_TALKS in user.permissions)
            )
}
