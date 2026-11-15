package de.geosphere.congregationplaner.notification.domain.permission

import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject

/**
 * Wer welche Art von Benachrichtigung senden darf (ADR-0035). Die Berechtigung gilt je Kategorie, damit sich später
 * einzelne Mitglieder auf bestimmte Kategorien beschränken lassen.
 * Hinweis: Die verbindliche Durchsetzung erfolgt zusätzlich in den Firestore Security Rules.
 */
@Inject
class NotificationPermissionPolicy(private val talkPolicy: TalkSchedulePermissionPolicy) {
    fun canSend(user: AppUser, category: NotificationCategory): Boolean = when (category) {
        // Wer „Informiert“ ändern darf, meldet die Änderung.
        NotificationCategory.TALK_PLANNING -> talkPolicy.canConfirmSpeaker(user)
        NotificationCategory.ANNOUNCEMENT -> user.hasRole(CongregationRole.NOTIFICATION_SENDER)
    }
}
