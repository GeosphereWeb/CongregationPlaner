package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.MessageSender
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WhatsAppMessage
import de.geosphere.congregationplaner.talk.domain.model.talkLabel
import de.geosphere.congregationplaner.user.domain.model.MessagePlaceholder
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import dev.zacsweers.metro.Inject
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

private val WEEKDAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")

/**
 * Je Vorlage der WhatsApp-Link mit ausgefülltem Text, die Standardvorlage zuerst; leer, wenn sich der Redner nicht per
 * WhatsApp erreichen lässt. Unbekannte Platzhalter bleiben stehen, fehlende Werte (z. B. der Vorname eines frei
 * eingetragenen Redners) sind leer.
 */
@Inject
class BuildWhatsAppMessagesUseCase {
    operator fun invoke(
        slot: TalkSlot,
        contact: SpeakerContact?,
        templates: List<MessageTemplate>,
        sender: MessageSender,
    ): List<WhatsAppMessage> {
        if (contact?.whatsAppUri == null) return emptyList()
        val values = values(slot, sender)
        return templates.mapNotNull { template ->
            contact.whatsAppUri(fill(template.text, values))?.let { WhatsAppMessage(template.name, it) }
        }
    }

    internal fun fill(text: String, values: Map<MessagePlaceholder, String>): String =
        values.entries.fold(text) { filled, (placeholder, value) -> filled.replace(placeholder.key, value) }

    internal fun values(slot: TalkSlot, sender: MessageSender): Map<MessagePlaceholder, String> = mapOf(
        MessagePlaceholder.FIRST_NAME to slot.speakerFirstName.orEmpty(),
        MessagePlaceholder.LAST_NAME to slot.speakerLastName.orEmpty(),
        MessagePlaceholder.WEEKDAY to WEEKDAYS[slot.date.dayOfWeek.isoDayNumber - 1],
        MessagePlaceholder.DATE to
            "${slot.date.day.twoDigits()}.${slot.date.month.number.twoDigits()}.${slot.date.year}",
        MessagePlaceholder.TIME to "${slot.time.hour.twoDigits()}:${slot.time.minute.twoDigits()}",
        MessagePlaceholder.TALK to slot.talkLabel().orEmpty(),
        MessagePlaceholder.TALK_NUMBER to slot.talkNumber?.toString().orEmpty(),
        MessagePlaceholder.TALK_TITLE to slot.talkTitle.orEmpty(),
        MessagePlaceholder.MY_FIRST_NAME to sender.firstName,
        MessagePlaceholder.MY_LAST_NAME to sender.lastName,
    )
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
