package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual fun createMessageTemplateRepository(): MessageTemplateRepository = object : MessageTemplateRepository {
    override fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>> = flow {
        throw UnsupportedOperationException("Firestore is not configured for iOS.")
    }

    override suspend fun saveTemplates(
        congregation: CongregationRef,
        uid: String,
        templates: List<MessageTemplate>,
    ): Result<Unit> = Result.failure(UnsupportedOperationException("Firestore is not configured for iOS."))
}
