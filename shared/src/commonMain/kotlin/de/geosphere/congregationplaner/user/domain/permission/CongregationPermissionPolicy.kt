package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für Versammlungs-Metadaten. Nicht freigegebene Nutzer erhalten nie Schreibrechte.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class CongregationPermissionPolicy {

    // Darf der User überhaupt neue Versammlungen anlegen?
    fun canCreate(user: AppUser): Boolean = user.isApprovedAdmin()

    // Darf der User diese spezifische Versammlung bearbeiten?
    @Suppress("UnusedParameter")
    fun canEdit(user: AppUser, congregation: CongregationMetadata): Boolean = user.isApprovedAdmin()

    // Darf der User diese Versammlung löschen?
    @Suppress("UnusedParameter")
    fun canDelete(user: AppUser, congregation: CongregationMetadata): Boolean = user.isApprovedAdmin()

    // Globale UI-Sichtbarkeit für Listenansichten, wo noch keine konkrete Versammlung gewählt ist
    fun canManageGeneral(user: AppUser): Boolean = user.isApprovedAdmin()

    private fun AppUser.isApprovedAdmin(): Boolean = approved && role == UserRole.ADMIN
}
