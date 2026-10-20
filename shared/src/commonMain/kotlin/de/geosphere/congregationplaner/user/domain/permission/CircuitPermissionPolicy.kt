package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Kreisverwaltung. Nicht freigegebene Nutzer erhalten nie Schreibrechte.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class CircuitPermissionPolicy {

    // Darf der User neue Kreise anlegen?
    fun canCreate(user: AppUser): Boolean = user.isApprovedAdmin()

    // Darf der User diesen Kreis bearbeiten?
    @Suppress("UnusedParameter")
    fun canEdit(user: AppUser, circuit: Circuit): Boolean = user.isApprovedAdmin()

    // Darf der User diesen Kreis löschen?
    @Suppress("UnusedParameter")
    fun canDelete(user: AppUser, circuit: Circuit): Boolean = user.isApprovedAdmin()

    // Globale UI-Sichtbarkeit für Listenansichten, wo noch kein konkreter Kreis gewählt ist
    fun canManageGeneral(user: AppUser): Boolean = user.isApprovedAdmin()

    private fun AppUser.isApprovedAdmin(): Boolean = approved && role == UserRole.ADMIN
}
