package de.geosphere.congregationplaner.user.domain.model

/**
 * Eine eigene WhatsApp-Vorlage aus `…/members/{uid}/settings/messageTemplates`. Platzhalter wie `{Vorname}` füllt die
 * Vortragsplanung beim Öffnen von WhatsApp aus; in einer Liste ist die erste Vorlage der Standard.
 */
data class MessageTemplate(val name: String, val text: String) {
    companion object {
        const val MAX_TEMPLATES = 4
        const val MAX_NAME_LENGTH = 40
        const val MAX_TEXT_LENGTH = 1000
    }
}

/** Die Platzhalter, die beim Öffnen von WhatsApp ersetzt werden; die Werte stammen aus Termin, Redner und Nutzer. */
enum class MessagePlaceholder(val key: String, val description: String) {
    FIRST_NAME("{Vorname}", "Vorname des Redners"),
    LAST_NAME("{Nachname}", "Nachname des Redners"),
    WEEKDAY("{Wochentag}", "z. B. Sonntag"),
    DATE("{Datum}", "z. B. 15.11.2026"),
    TIME("{Uhrzeit}", "z. B. 10:00"),
    TALK("{Vortrag}", "Nr. und Titel"),
    TALK_NUMBER("{Vortragsnummer}", "nur die Nummer"),
    TALK_TITLE("{Vortragstitel}", "nur der Titel"),
    MY_FIRST_NAME("{MeinVorname}", "dein Vorname"),
    MY_LAST_NAME("{MeinNachname}", "dein Nachname"),
}

/** Gilt, solange der Nutzer keine eigenen Vorlagen gespeichert hat. */
val DEFAULT_MESSAGE_TEMPLATES = listOf(
    MessageTemplate(
        "Anfrage",
        "Hallo {Vorname}, ich bin für die Vortragsplanung zuständig. Könntest du am {Wochentag}, {Datum} um " +
            "{Uhrzeit} Uhr bei uns den Vortrag „{Vortrag}“ halten? Viele Grüße, {MeinVorname} {MeinNachname}",
    ),
    MessageTemplate(
        "Erinnerung",
        "Hallo {Vorname}, ich möchte dich an deinen Vortrag „{Vortrag}“ am {Wochentag}, {Datum} um {Uhrzeit} Uhr " +
            "erinnern. Wir freuen uns auf dich! Viele Grüße, {MeinVorname}",
    ),
    MessageTemplate(
        "Dank",
        "Hallo {Vorname}, vielen Dank für deinen Vortrag „{Vortrag}“ am {Datum}. Liebe Grüße, {MeinVorname}",
    ),
)

/** Eins bis [MessageTemplate.MAX_TEMPLATES] Vorlagen mit Name und Text in den erlaubten Längen. */
fun List<MessageTemplate>.isValidTemplateList(): Boolean = size in 1..MessageTemplate.MAX_TEMPLATES &&
    all { template ->
        template.name.trim().length in 1..MessageTemplate.MAX_NAME_LENGTH &&
            template.text.trim().length in 1..MessageTemplate.MAX_TEXT_LENGTH
    }

sealed class MessageTemplateException(message: String) : Exception(message) {
    class NotAllowed : MessageTemplateException("Vorlagen gibt es nur für Mitglieder mit eigenem Konto.")

    class Invalid :
        MessageTemplateException(
            "Bitte 1 bis ${MessageTemplate.MAX_TEMPLATES} Vorlagen mit Namen (bis ${MessageTemplate.MAX_NAME_LENGTH} " +
                "Zeichen) und Text (bis ${MessageTemplate.MAX_TEXT_LENGTH} Zeichen) angeben.",
        )
}
