package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import de.geosphere.congregationplaner.user.domain.model.isValidTemplateList
import dev.zacsweers.metro.Inject

/** Eine Änderung an der Liste der eigenen WhatsApp-Vorlagen; [index] ist die Stelle in der Liste. */
sealed interface MessageTemplateEdit {
    data object Add : MessageTemplateEdit

    data class Change(val index: Int, val template: MessageTemplate) : MessageTemplateEdit

    data class Remove(val index: Int) : MessageTemplateEdit

    /** Die Vorlage wird Standard, indem sie an die erste Stelle rückt. */
    data class MakeDefault(val index: Int) : MessageTemplateEdit
}

/**
 * Wendet eine [MessageTemplateEdit] auf die Vorlagen an: höchstens [MessageTemplate.MAX_TEMPLATES], mindestens eine,
 * Name und Text auf ihre Länge gekürzt. Was die Regeln nicht erlauben, ändert nichts.
 */
@Inject
class EditMessageTemplatesUseCase {
    operator fun invoke(templates: List<MessageTemplate>, edit: MessageTemplateEdit): List<MessageTemplate> =
        when (edit) {
            MessageTemplateEdit.Add -> if (canAdd(templates)) templates + newTemplate(templates.size + 1) else templates
            is MessageTemplateEdit.Change ->
                templates.mapIndexed { i, current -> if (i == edit.index) edit.template.withinLimits() else current }
            is MessageTemplateEdit.Remove ->
                if (canRemove(templates)) templates.filterIndexed { i, _ -> i != edit.index } else templates
            is MessageTemplateEdit.MakeDefault -> templates.getOrNull(edit.index)
                ?.let { listOf(it) + templates.filterIndexed { i, _ -> i != edit.index } }
                ?: templates
        }

    fun canAdd(templates: List<MessageTemplate>): Boolean = templates.size < MessageTemplate.MAX_TEMPLATES

    fun canRemove(templates: List<MessageTemplate>): Boolean = templates.size > 1

    /** Lassen sich die Vorlagen so speichern (siehe [SaveMessageTemplatesUseCase])? */
    fun isValid(templates: List<MessageTemplate>): Boolean =
        templates.map { MessageTemplate(it.name.trim(), it.text.trim()) }.isValidTemplateList()

    private fun newTemplate(number: Int) = MessageTemplate("Vorlage $number", "Hallo {Vorname}, ")

    private fun MessageTemplate.withinLimits() =
        MessageTemplate(name.take(MessageTemplate.MAX_NAME_LENGTH), text.take(MessageTemplate.MAX_TEXT_LENGTH))
}
