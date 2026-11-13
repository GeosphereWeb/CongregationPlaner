package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.DEFAULT_MESSAGE_TEMPLATES
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.model.MessageTemplateException
import de.geosphere.congregationplaner.user.domain.model.isValidTemplateList
import de.geosphere.congregationplaner.user.domain.repository.MessageTemplateRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Nur ein Mitglied mit eigenem Konto hat ein Dokument, unter dem es Vorlagen speichern kann. */
internal val AppUser.canKeepMessageTemplates: Boolean get() = isMember && hasAccount && congregation != null

/**
 * Die Vorlagen des Nutzers, die erste ist der Standard. Ohne eigene (oder wenn sie sich nicht lesen lassen) gelten die
 * eingebauten [DEFAULT_MESSAGE_TEMPLATES].
 */
@Inject
class ObserveMessageTemplatesUseCase(private val repository: MessageTemplateRepository) {
    operator fun invoke(user: AppUser): Flow<List<MessageTemplate>> {
        val congregation = user.congregation
        if (!user.canKeepMessageTemplates || congregation == null) return flowOf(DEFAULT_MESSAGE_TEMPLATES)
        return repository.observeTemplates(congregation, user.uid)
            .map { it.ifEmpty { DEFAULT_MESSAGE_TEMPLATES } }
            .catch { emit(DEFAULT_MESSAGE_TEMPLATES) }
    }
}

/** Speichert die eigenen Vorlagen ohne Leerzeichen am Rand; die erste ist der Standard. */
@Inject
class SaveMessageTemplatesUseCase(private val repository: MessageTemplateRepository) {
    suspend operator fun invoke(user: AppUser, templates: List<MessageTemplate>): Result<List<MessageTemplate>> {
        val congregation = user.congregation
        val cleaned = templates.map { MessageTemplate(it.name.trim(), it.text.trim()) }
        return when {
            !user.canKeepMessageTemplates || congregation == null ->
                Result.failure(MessageTemplateException.NotAllowed())
            !cleaned.isValidTemplateList() -> Result.failure(MessageTemplateException.Invalid())
            else -> repository.saveTemplates(congregation, user.uid, cleaned).map { cleaned }
        }
    }
}
