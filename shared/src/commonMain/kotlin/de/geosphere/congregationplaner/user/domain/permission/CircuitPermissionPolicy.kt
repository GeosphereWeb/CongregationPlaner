package de.geosphere.congregationplaner.user.domain.permission

import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.user.domain.model.AppUser
import dev.zacsweers.metro.Inject

/**
 * Berechtigungen für die Kreisverwaltung. Schreibrechte haben nur globale Administratoren.
 * Hinweis: Die verbindliche Durchsetzung muss zusätzlich in den Firestore Security Rules erfolgen.
 */
@Inject
class CircuitPermissionPolicy {

    // Darf der User neue Kreise anlegen?
    fun canCreate(user: AppUser): Boolean = user.isGlobalAdmin

    // Darf der User diesen Kreis bearbeiten?
    @Suppress("UnusedParameter")
    fun canEdit(user: AppUser, circuit: Circuit): Boolean = user.isGlobalAdmin

    // Darf der User diesen Kreis löschen?
    @Suppress("UnusedParameter")
    fun canDelete(user: AppUser, circuit: Circuit): Boolean = user.isGlobalAdmin

    // Globale UI-Sichtbarkeit für Listenansichten, wo noch kein konkreter Kreis gewählt ist
    fun canManageGeneral(user: AppUser): Boolean = user.isGlobalAdmin
}
