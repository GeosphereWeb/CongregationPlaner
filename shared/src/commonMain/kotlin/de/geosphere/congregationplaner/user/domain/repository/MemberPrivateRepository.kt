package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import kotlinx.coroutines.flow.Flow

/** Private Angaben eines Mitglieds (`…/congregation/{id}/memberPrivate/{uid}`, ADR-0015). */
interface MemberPrivateRepository {
    /** Beobachtet die Angaben; emittiert `null`, solange das Mitglied noch keine gespeichert hat. */
    fun observe(congregation: CongregationRef, uid: String): Flow<MemberPrivateDetails?>

    /** Ersetzt die Angaben des Mitglieds vollständig. */
    suspend fun save(congregation: CongregationRef, uid: String, details: MemberPrivateDetails): Result<Unit>

    /** Löscht die Angaben; gibt es keine, ist das kein Fehler. */
    suspend fun delete(congregation: CongregationRef, uid: String): Result<Unit>
}
