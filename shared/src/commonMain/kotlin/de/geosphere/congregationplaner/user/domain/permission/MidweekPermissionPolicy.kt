package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Planung unter der Woche. Die Rolle Versammlungsadmin allein berechtigt nicht (ADR-0012).
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class MidweekPermissionPolicy {
    fun canPlanMidweek(user: AppUser): Boolean = user.approved && CongregationRole.TASK_PLANNER in user.roles
}
