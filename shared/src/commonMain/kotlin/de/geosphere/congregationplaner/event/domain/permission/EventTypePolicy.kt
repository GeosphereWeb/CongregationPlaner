package de.geosphere.congregationplaner.event.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Ereignisarten (ADR-0036) sehen alle freigegebenen Mitglieder; der Versammlungsadmin pflegt sie, der Vortragsplaner
 * darf beim Import fehlende anlegen. Hinweis: Die verbindliche Durchsetzung erfolgt zusätzlich in den Security Rules.
 */
@Inject
class EventTypePolicy {
    fun canView(user: AppUser): Boolean = user.approved && user.isMember && user.congregation != null

    fun canManage(user: AppUser): Boolean = user.hasRole(CongregationRole.CONGREGATION_ADMIN)

    fun canCreate(user: AppUser): Boolean = canManage(user) || user.hasRole(CongregationRole.TALK_PLANNER)
}
