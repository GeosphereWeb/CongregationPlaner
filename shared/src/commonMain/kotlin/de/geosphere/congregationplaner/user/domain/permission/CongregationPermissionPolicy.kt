package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für Versammlungs-Metadaten. Anlegen und Löschen dürfen nur globale Administratoren,
 * bearbeiten dürfen zusätzlich freigegebene Versammlungsadmins ihre eigene Versammlung.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class CongregationPermissionPolicy {

    // Darf der User überhaupt neue Versammlungen anlegen?
    fun canCreate(user: AppUser): Boolean = user.isGlobalAdmin

    // Darf der User diese spezifische Versammlung bearbeiten? Versammlungsadmins nur ihre eigene.
    fun canEdit(user: AppUser, congregation: CongregationMetadata): Boolean = user.isGlobalAdmin ||
        (
            user.approved &&
                user.role == UserRole.CONGREGATION_ADMIN &&
                congregation.id.isNotBlank() &&
                user.congregation?.congregationId == congregation.id
            )

    // Darf der User diese Versammlung löschen?
    @Suppress("UnusedParameter")
    fun canDelete(user: AppUser, congregation: CongregationMetadata): Boolean = user.isGlobalAdmin

    // Globale UI-Sichtbarkeit für Listenansichten, wo noch keine konkrete Versammlung gewählt ist
    fun canManageGeneral(user: AppUser): Boolean = user.isGlobalAdmin
}
