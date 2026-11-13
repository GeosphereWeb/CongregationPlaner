package de.geosphere.congregationplaner.user.domain.repository

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import kotlinx.coroutines.flow.Flow

/** Die eigenen WhatsApp-Vorlagen eines Mitglieds (`…/members/{uid}/settings/messageTemplates`). */
interface MessageTemplateRepository {
    /** Beobachtet die gespeicherten Vorlagen; eine leere Liste, solange keine gespeichert sind. */
    fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>>

    /** Ersetzt die gespeicherten Vorlagen. */
    suspend fun saveTemplates(
        congregation: CongregationRef,
        uid: String,
        templates: List<MessageTemplate>,
    ): Result<Unit>
}
