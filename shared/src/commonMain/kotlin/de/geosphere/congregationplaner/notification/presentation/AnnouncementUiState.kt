package de.geosphere.congregationplaner.notification.presentation

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.user.domain.model.CongregationRole

/** Die Mitteilung, die gerade verfasst wird. */
data class AnnouncementDraft(
    val title: String = "",
    val body: String = "",
    val groups: Set<EventAudience> = emptySet(),
    val roles: Set<CongregationRole> = emptySet(),
) {
    val hasRecipients: Boolean get() = groups.isNotEmpty() || roles.isNotEmpty()
    val isComplete: Boolean get() = title.isNotBlank() && hasRecipients

    /** Texte auf die erlaubte Länge gekürzt, damit die Eingabe nie mehr annimmt, als gesendet werden darf. */
    fun limited(): AnnouncementDraft = copy(
        title = title.take(AppNotification.MAX_TITLE_LENGTH),
        body = body.take(AppNotification.MAX_BODY_LENGTH),
    )

    /** „Alle“ schließt alle übrigen Empfänger aus; wer eine Gruppe wählt, wählt „Alle“ ab. */
    fun toggled(group: EventAudience): AnnouncementDraft = when {
        group == EventAudience.ALL && group !in groups -> copy(groups = setOf(EventAudience.ALL), roles = emptySet())
        group in groups -> copy(groups = groups - group)
        else -> copy(groups = groups - EventAudience.ALL + group)
    }

    /** Eine Rolle wählt „Alle“ ab, wie eine Gruppe. */
    fun toggled(role: CongregationRole): AnnouncementDraft = when (role) {
        in roles -> copy(roles = roles - role)
        else -> copy(groups = groups - EventAudience.ALL, roles = roles + role)
    }

    fun toNotification(): AppNotification =
        AppNotification(NotificationCategory.ANNOUNCEMENT, title = title, body = body, groups = groups, roles = roles)
}

data class AnnouncementUiState(
    val canSend: Boolean = false,
    val draft: AnnouncementDraft = AnnouncementDraft(),
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    /** Bestätigung nach dem Senden; verschwindet bei der nächsten Eingabe. */
    val sentMessage: String? = null,
) {
    val canSubmit: Boolean get() = canSend && !isSending && draft.isComplete
}
