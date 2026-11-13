package de.geosphere.congregationplaner.talk.domain.model

/** Fachliche Fehler beim Pflegen der Vortragsliste und der Wochenereignisse; die Meldungen zeigt die UI an. */
sealed class TalkReferenceException(message: String) : Exception(message) {
    class NotAllowed :
        TalkReferenceException("Nur globale Administratoren dürfen die Vortragsliste und die Wochenereignisse pflegen.")

    class InvalidTalk :
        TalkReferenceException(
            "Die Vortragsnummer muss zwischen 1 und ${Talk.MAX_NUMBER} liegen, jeder Titel 1 bis " +
                "${TalkSlot.MAX_TALK_TITLE_LENGTH} Zeichen haben; höchstens ${Talk.MAX_TITLE_CHANGES} " +
                "Titeländerungen, je Tag eine.",
        )

    class MissingNumber : TalkReferenceException("Bitte gib eine Vortragsnummer ein.")

    class NumberTaken(number: Int) : TalkReferenceException("Die Nummer $number gibt es schon.")

    class InvalidWeekEvent :
        TalkReferenceException(
            "Der Name des Wochenereignisses muss 1 bis ${TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH} Zeichen haben.",
        )
}
