package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Vortragsplanung einer Versammlung: Vortragsplaner planen, Vortragsplanungs-Helfer sehen die
 * Planung und markieren Redner als informiert (ADR-0023).
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class TalkSchedulePermissionPolicy {
    // Auch Versammlungsadmins brauchen die Rolle TALK_PLANNER; die Admin-Rolle allein berechtigt nicht zum Planen.
    fun canPlanTalks(user: AppUser): Boolean = user.hasRole(CongregationRole.TALK_PLANNER)

    /** Termine, Rednerliste und Vortragshistorie sehen: Planer und Helfer. */
    fun canViewTalkPlanning(user: AppUser): Boolean = canPlanTalks(user) || user.hasRole(CongregationRole.TALK_HELPER)

    /** Einen Redner als informiert markieren (nur das Feld `confirmed` eines Termins): Planer und Helfer. */
    fun canConfirmSpeaker(user: AppUser): Boolean = canViewTalkPlanning(user)
}
