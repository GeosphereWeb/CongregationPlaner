package de.geosphere.congregationplaner.notification.domain.model

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/**
 * Art einer Benachrichtigung (ADR-0035). Sie bestimmt, wer sie senden darf, und auf Android den
 * Benachrichtigungskanal.
 */
enum class NotificationCategory {
    /** Änderungen an der Vortragsplanung, z. B. „Redner informiert“; an Vortragsplaner und Helfer. */
    TALK_PLANNING,
}

/**
 * Eine Benachrichtigung an die Mitglieder einer Versammlung aus `…/congregation/{id}/notifications/{id}`.
 * Empfänger sind alle freigegebenen Mitglieder, die zu einer der [groups] gehören oder eine der [roles] haben.
 */
data class AppNotification(
    val category: NotificationCategory,
    val title: String,
    val body: String = "",
    val groups: Set<EventAudience> = emptySet(),
    val roles: Set<CongregationRole> = emptySet(),
) {
    companion object {
        const val MAX_TITLE_LENGTH = 100
        const val MAX_BODY_LENGTH = 500
    }
}

/** Fachliche Fehler beim Senden einer Benachrichtigung. */
sealed class NotificationException(message: String) : Exception(message) {
    class NotAllowed : NotificationException("Du darfst diese Benachrichtigung nicht senden.")

    class Invalid : NotificationException("Die Benachrichtigung braucht einen Titel, einen kurzen Text und Empfänger.")
}
