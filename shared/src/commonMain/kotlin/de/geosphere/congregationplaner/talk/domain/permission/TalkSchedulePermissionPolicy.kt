package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Vortragsplanung einer Versammlung. Lesen und Planen sind zunächst dasselbe Recht.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class TalkSchedulePermissionPolicy {
    // Auch Versammlungsadmins brauchen die Rolle TALK_PLANNER; die Admin-Rolle allein berechtigt nicht zum Planen.
    fun canPlanTalks(user: AppUser): Boolean = user.approved && CongregationRole.TALK_PLANNER in user.roles
}
