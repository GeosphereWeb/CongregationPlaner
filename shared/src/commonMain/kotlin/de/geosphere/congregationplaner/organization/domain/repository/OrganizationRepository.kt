package de.geosphere.congregationplaner.organization.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation

interface OrganizationRepository {
    /**
     * Liest die Einheiten der Ebene [level] unterhalb von [parentIds]. Die Organisationsstruktur ist
     * bereits vor der Anmeldung lesbar, damit ein Nutzer seine Versammlung auswählen kann.
     */
    suspend fun listUnits(level: OrgLevel, parentIds: List<String>): Result<List<OrgUnit>>
}

/** Merkt sich die gewählte Versammlung lokal auf dem Gerät, damit sie nicht bei jedem Start gewählt werden muss. */
interface CongregationSelectionStore {
    fun load(): SelectedCongregation?

    fun save(selection: SelectedCongregation)

    fun clear()
}
