package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für Versammlungs-Metadaten. Anlegen und Löschen dürfen nur globale Administratoren. Den Namen ändern
 * globale Administratoren und die freigegebenen Versammlungsadmins der eigenen Versammlung; die Zusammenkunftszeiten
 * ändert nur der Versammlungsadmin (ADR-0013).
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class CongregationPermissionPolicy {

    // Darf der User überhaupt neue Versammlungen anlegen?
    fun canCreate(user: AppUser): Boolean = user.isGlobalAdmin

    // Darf der User diese Versammlung bearbeiten (Name)? Globale Admins jede, Versammlungsadmins nur ihre eigene.
    fun canEdit(user: AppUser, congregation: CongregationMetadata): Boolean =
        user.isGlobalAdmin || canEditMeetingTimes(user, congregation)

    // Darf der User die Zusammenkunftszeiten ändern? Nur der freigegebene Versammlungsadmin der eigenen Versammlung.
    fun canEditMeetingTimes(user: AppUser, congregation: CongregationMetadata): Boolean = user.approved &&
        CongregationRole.CONGREGATION_ADMIN in user.roles &&
        congregation.id.isNotBlank() &&
        user.congregation?.congregationId == congregation.id

    // Darf der User diese Versammlung löschen?
    @Suppress("UnusedParameter")
    fun canDelete(user: AppUser, congregation: CongregationMetadata): Boolean = user.isGlobalAdmin

    // Globale UI-Sichtbarkeit für Listenansichten, wo noch keine konkrete Versammlung gewählt ist
    fun canManageGeneral(user: AppUser): Boolean = user.isGlobalAdmin
}
