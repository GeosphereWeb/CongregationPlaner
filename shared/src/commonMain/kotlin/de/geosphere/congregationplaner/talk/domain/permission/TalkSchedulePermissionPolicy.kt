package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Vortragsplanung einer Versammlung. Lesen und Planen sind zunächst dasselbe Recht.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class TalkSchedulePermissionPolicy {
    // Auch Versammlungsadmins brauchen PLAN_TALKS; die Rolle allein berechtigt nicht zum Planen.
    fun canPlanTalks(user: AppUser): Boolean =
        user.isGlobalAdmin || (user.approved && UserPermission.PLAN_TALKS in user.permissions)
}
