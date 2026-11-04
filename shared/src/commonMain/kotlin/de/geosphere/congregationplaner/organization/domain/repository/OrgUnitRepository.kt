package de.geosphere.congregationplaner.organization.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import kotlinx.coroutines.flow.Flow

/**
 * Pflege der Organisationsebenen Region bis Kreis ([OrgLevel.isOrgUnit]) durch den globalen Admin. [parentIds] sind die
 * IDs der übergeordneten Einheiten von [OrgLevel.REGION] abwärts (siehe [OrgLevel.collectionPath]).
 */
interface OrgUnitRepository {
    fun observe(level: OrgLevel, parentIds: List<String>): Flow<List<OrgUnit>>

    /** Legt die Einheit mit einer von Firestore erzeugten ID an und liefert sie mit dieser ID zurück. */
    suspend fun create(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit>

    suspend fun update(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<OrgUnit>

    /**
     * Löscht die Einheit. Firestore löscht Unterdokumente nicht mit; hat sie noch Einträge der nächsten Ebene, schlägt
     * das mit `OrgUnitException.HasChildren` fehl.
     */
    suspend fun delete(level: OrgLevel, parentIds: List<String>, unit: OrgUnit): Result<Unit>
}
