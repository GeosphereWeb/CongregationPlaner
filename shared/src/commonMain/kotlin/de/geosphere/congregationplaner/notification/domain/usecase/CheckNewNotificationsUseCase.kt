package de.geosphere.congregationplaner.notification.domain.usecase

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseAuthRepository
import de.geosphere.congregationplaner.event.domain.model.eventGroups
import de.geosphere.congregationplaner.notification.domain.model.NotificationCheck
import de.geosphere.congregationplaner.notification.domain.repository.NotificationRepository
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.first
import kotlin.time.Instant

/**
 * Holt die Benachrichtigungen nach [seenUntil] für das angemeldete, freigegebene Mitglied der gewählten Versammlung
 * (ADR-0035): alles an seine Gruppen und Rollen, ohne die eigenen Meldungen. Der neue Stand ist die Zeit der neuesten
 * gelesenen Benachrichtigung.
 *
 * Beim ersten Aufruf ([seenUntil] ist `null`) und solange niemand Berechtigtes angemeldet ist, gibt es nichts, und der
 * Stand springt auf [now]: Alte Benachrichtigungen kommen so nicht nachträglich auf einmal.
 */
@Inject
class CheckNewNotificationsUseCase(
    private val auth: FirebaseAuthRepository,
    private val selection: CongregationSelectionStore,
    private val users: UserRepository,
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(seenUntil: Instant?, now: Instant): Result<NotificationCheck> = runCatching {
        val uid = auth.currentUserId()
        val congregation = selection.load()?.ref
        val user = if (uid != null && congregation != null) users.observeUser(congregation, uid).first() else null
        val approvedUser = user?.takeIf { it.approved }
        if (congregation == null || approvedUser == null || seenUntil == null) {
            NotificationCheck(emptyList(), now)
        } else {
            val fetched = repository
                .fetchSince(congregation, approvedUser.profile.eventGroups(), approvedUser.roles, seenUntil)
                .getOrThrow()
            NotificationCheck(
                notifications = fetched.filter { it.senderUid != approvedUser.uid },
                seenUntil = fetched.maxOfOrNull { it.createdAt } ?: seenUntil,
            )
        }
    }
}
