package de.geosphere.congregationplaner.talk.presentation.schedule

import de.geosphere.congregationplaner.presentation.navigation.germanWeekdayName
import de.geosphere.congregationplaner.talk.domain.model.SpeakerContact
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.user.domain.model.MessagePlaceholder
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import kotlinx.datetime.number

/** Der Name des angemeldeten Nutzers für `{MeinVorname}` und `{MeinNachname}`. */
data class MessageSender(val firstName: String = "", val lastName: String = "")

/** Eine ausgefüllte Vorlage hinter dem WhatsApp-Wischen: [name] der Vorlage, [uri] der fertige `wa.me`-Link. */
internal data class WhatsAppMessage(val name: String, val uri: String)

/**
 * Je Vorlage der WhatsApp-Link mit ausgefülltem Text, die Standardvorlage zuerst; leer, wenn sich der Redner nicht per
 * WhatsApp erreichen lässt.
 */
internal fun slotWhatsAppMessages(
    slot: TalkSlot,
    contact: SpeakerContact?,
    templates: List<MessageTemplate>,
    sender: MessageSender,
): List<WhatsAppMessage> {
    if (contact?.whatsAppUri == null) return emptyList()
    val values = slotMessageValues(slot, sender)
    return templates.mapNotNull { template ->
        contact.whatsAppUri(fillTemplate(template.text, values))?.let { WhatsAppMessage(template.name, it) }
    }
}

/** Ersetzt jeden bekannten Platzhalter; unbekannte Klammern bleiben stehen. */
internal fun fillTemplate(text: String, values: Map<MessagePlaceholder, String>): String =
    values.entries.fold(text) { filled, (placeholder, value) -> filled.replace(placeholder.key, value) }

/** Die Werte der Platzhalter für [slot]; was fehlt (z. B. der Vorname eines frei eingetragenen Redners), ist leer. */
internal fun slotMessageValues(slot: TalkSlot, sender: MessageSender): Map<MessagePlaceholder, String> = mapOf(
    MessagePlaceholder.FIRST_NAME to slot.speakerFirstName.orEmpty(),
    MessagePlaceholder.LAST_NAME to slot.speakerLastName.orEmpty(),
    MessagePlaceholder.WEEKDAY to germanWeekdayName(slot.date),
    MessagePlaceholder.DATE to
        "${slot.date.day.twoDigits()}.${slot.date.month.number.twoDigits()}.${slot.date.year}",
    MessagePlaceholder.TIME to "${slot.time.hour.twoDigits()}:${slot.time.minute.twoDigits()}",
    MessagePlaceholder.TALK to slotTalkText(slot).orEmpty(),
    MessagePlaceholder.TALK_NUMBER to slot.talkNumber?.toString().orEmpty(),
    MessagePlaceholder.TALK_TITLE to slot.talkTitle.orEmpty(),
    MessagePlaceholder.MY_FIRST_NAME to sender.firstName,
    MessagePlaceholder.MY_LAST_NAME to sender.lastName,
)

private fun Int.twoDigits(): String = toString().padStart(2, '0')
