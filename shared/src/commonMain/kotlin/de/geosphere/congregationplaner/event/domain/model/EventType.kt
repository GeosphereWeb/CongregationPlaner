package de.geosphere.congregationplaner.event.domain.model

/**
 * Eine Ereignisart der Versammlung aus `…/congregation/{id}/eventTypes/{id}` (ADR-0036), z. B. „Kreiskongress“.
 * Termine und besondere Ereignisse verweisen per [id] darauf. [icon] ist der Name einer Icon-Ressource wie
 * `ic_cp_arena_duo`; die Wochenereignisse des Landes dienen als Vorlage.
 */
data class EventType(
    val id: String = "",
    val name: String = "",
    val cancelsTalk: Boolean = false,
    val icon: String = DEFAULT_ICON,
) {
    companion object {
        const val MAX_NAME_LENGTH = 100
        const val DEFAULT_ICON = "ic_cp_square"

        /** Wie in den Firestore-Regeln: nur Icons des App-Satzes. */
        val ICON_PATTERN = Regex("^ic_cp_[a-z0-9_]{1,93}$")
    }
}

/** Fachliche Fehler beim Pflegen der Ereignisarten; die Nachrichten werden direkt in der UI angezeigt. */
sealed class EventTypeException(message: String) : Exception(message) {
    class NotAllowed : EventTypeException("Dafür fehlt dir die Berechtigung.")

    class InvalidName :
        EventTypeException("Bitte gib einen Namen mit höchstens ${EventType.MAX_NAME_LENGTH} Zeichen ein.")

    class InvalidIcon : EventTypeException("Das gewählte Icon ist ungültig.")
}
