package de.geosphere.congregationplaner.notification.domain.usecase

import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.NotificationException
import de.geosphere.congregationplaner.notification.domain.permission.NotificationPermissionPolicy
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import dev.zacsweers.metro.Inject
import kotlinx.datetime.number

/** Sendet eine Benachrichtigung an die eigene Versammlung, wenn der Nutzer die Kategorie senden darf. */
@Inject
class SendNotificationUseCase(
    private val repository: NotificationRepository,
    private val policy: NotificationPermissionPolicy,
) {
    suspend operator fun invoke(user: AppUser, notification: AppNotification): Result<Unit> {
        val congregation = user.congregation
        val cleaned = notification.copy(title = notification.title.trim(), body = notification.body.trim())
        return when {
            congregation == null || !policy.canSend(user, notification.category) ->
                Result.failure(NotificationException.NotAllowed())
            !cleaned.isValid() -> Result.failure(NotificationException.Invalid())
            else -> repository.send(congregation, user.uid, cleaned)
        }
    }

    private fun AppNotification.isValid(): Boolean = title.length in 1..AppNotification.MAX_TITLE_LENGTH &&
        body.length <= AppNotification.MAX_BODY_LENGTH &&
        (groups.isNotEmpty() || roles.isNotEmpty())
}

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

/**
 * Meldet Vortragsplanern und Helfern, dass sich „Informiert“ eines Termins geändert hat: von [previous] (`null` für
 * einen neuen Termin) zu [saved]. Ohne Änderung wird nichts gesendet.
 */
@Inject
class NotifyConfirmedChangedUseCase(private val send: SendNotificationUseCase) {
    suspend operator fun invoke(user: AppUser, previous: TalkSlot?, saved: TalkSlot): Result<Unit> {
        if ((previous?.confirmed ?: false) == saved.confirmed) return Result.success(Unit)
        val date = saved.date
        val dateText = "${twoDigits(date.day)}.${twoDigits(date.month.number)}.${date.year}"
        return send(
            user,
            AppNotification(
                category = NotificationCategory.TALK_PLANNING,
                title = if (saved.confirmed) "Redner informiert" else "Informiert zurückgenommen",
                body = "Vortrag am $dateText",
                roles = TALK_PLANNING_ROLES,
            ),
        )
    }

    companion object {
        /** Empfänger der Meldungen zur Vortragsplanung; die Rules lassen für die Kategorie nur genau diese zu. */
        val TALK_PLANNING_ROLES = setOf(CongregationRole.TALK_PLANNER, CongregationRole.TALK_HELPER)
    }
}
