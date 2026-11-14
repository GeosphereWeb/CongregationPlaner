package de.geosphere.congregationplaner.talk.domain.permission

import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Berechtigung für die Pflege der Vortragsliste und der Wochenereignisse eines Landes; sie gelten für alle
 * Versammlungen des Landes, deshalb dürfen sie nur globale Administratoren ändern.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class TalkReferenceDataPolicy {
    fun canManage(user: AppUser): Boolean = user.isGlobalAdmin
}
