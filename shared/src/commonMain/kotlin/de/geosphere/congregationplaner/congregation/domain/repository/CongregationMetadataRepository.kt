package de.geosphere.congregationplaner.congregation.domain.repository

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
import kotlinx.coroutines.flow.Flow

/**
 * Die Versammlungen eines Kreises. [circuitIds] sind die IDs von der Region bis zum Kreis (siehe
 * `CongregationRef.circuitIds` und `OrgLevel.collectionPath`).
 */
interface CongregationMetadataRepository {
    fun observeAll(circuitIds: List<String>): Flow<List<CongregationMetadata>>

    /** Die ID der Versammlung, die den Namen im Kreis trägt, oder `null`, wenn er frei ist. */
    suspend fun findNameOwner(circuitIds: List<String>, name: String): Result<String?>

    /**
     * Legt die Versammlung samt Namensdokument an; mit [firstAdmin] im selben atomaren Schreibzugriff auch dessen
     * Mitgliedsdokument (freigegeben, Rolle Versammlungsadmin). [congregation] hat noch keine ID.
     */
    suspend fun create(
        circuitIds: List<String>,
        congregation: CongregationMetadata,
        createdBy: String,
        firstAdmin: NewCongregationAdmin?,
    ): Result<CongregationMetadata>

    /** Speichert eine bestehende Versammlung; bei einem neuen Namen zieht das Namensdokument mit um. */
    suspend fun save(circuitIds: List<String>, congregation: CongregationMetadata): Result<CongregationMetadata>

    /** Löscht die Versammlung zusammen mit ihrem Namensdokument. */
    suspend fun delete(circuitIds: List<String>, congregationId: String): Result<Unit>
}
