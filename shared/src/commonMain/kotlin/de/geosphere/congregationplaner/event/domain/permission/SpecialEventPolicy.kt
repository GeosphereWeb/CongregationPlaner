package de.geosphere.congregationplaner.event.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Besondere Ereignisse pflegt der Versammlungsadmin seiner Versammlung.
 * Hinweis: Die verbindliche Durchsetzung erfolgt zusätzlich in den Firestore Security Rules.
 */
@Inject
class SpecialEventPolicy {
    fun canManage(user: AppUser): Boolean = user.hasRole(CongregationRole.CONGREGATION_ADMIN)
}
