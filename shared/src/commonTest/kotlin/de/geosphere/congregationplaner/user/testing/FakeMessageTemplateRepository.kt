package de.geosphere.congregationplaner.user.testing

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-Memory-[MessageTemplateRepository]; [saveResult] stellt einen Fehler beim Speichern ein. */
internal class FakeMessageTemplateRepository(templates: List<MessageTemplate> = emptyList()) :
    MessageTemplateRepository {
    val templates = MutableStateFlow(templates)
    var saveResult: Result<Unit> = Result.success(Unit)
    val saved = mutableListOf<Pair<String, List<MessageTemplate>>>()

    override fun observeTemplates(congregation: CongregationRef, uid: String): Flow<List<MessageTemplate>> = templates

    override suspend fun saveTemplates(
        congregation: CongregationRef,
        uid: String,
        templates: List<MessageTemplate>,
    ): Result<Unit> = saveResult.onSuccess {
        saved += uid to templates
        this.templates.value = templates
    }
}
