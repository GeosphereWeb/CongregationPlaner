package de.geosphere.congregationplaner.notification.domain

import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.AppNotification
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.NotificationException
import de.geosphere.congregationplaner.notification.domain.permission.NotificationPermissionPolicy
import de.geosphere.congregationplaner.notification.domain.usecase.NotifyConfirmedChangedUseCase
import de.geosphere.congregationplaner.notification.domain.usecase.SendNotificationUseCase
import de.geosphere.congregationplaner.notification.testing.FakeNotificationRepository
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class NotificationUseCasesTest {
    private val repository = FakeNotificationRepository()
    private val send = SendNotificationUseCase(repository, NotificationPermissionPolicy(TalkSchedulePermissionPolicy()))
    private val notify = NotifyConfirmedChangedUseCase(send)

    private val helper = AppUser(
        uid = "helper",
        approved = true,
        roles = setOf(CongregationRole.TALK_HELPER),
        congregation = testCongregation,
    )
    private val slot = TalkSlot(date = LocalDate(2026, 11, 22), time = LocalTime(10, 0))
    private val talkNotification = AppNotification(
        category = NotificationCategory.TALK_PLANNING,
        title = "  Titel  ",
        body = " Text ",
        roles = setOf(CongregationRole.TALK_PLANNER),
    )

    @Test
    fun `send trims the texts and sends with the user as sender`() = runTest {
        assertTrue(send(helper, talkNotification).isSuccess)

        assertEquals(
            "helper" to talkNotification.copy(title = "Titel", body = "Text"),
            repository.sent.single(),
        )
    }

    @Test
    fun `send is denied without permission approval or congregation`() = runTest {
        val denied = listOf(
            helper.copy(roles = setOf(CongregationRole.TASK_PLANNER)),
            helper.copy(approved = false),
            helper.copy(congregation = null),
        )
        denied.forEach { user ->
            assertIs<NotificationException.NotAllowed>(send(user, talkNotification).exceptionOrNull())
        }
        assertTrue(repository.sent.isEmpty())
    }

    @Test
    fun `send rejects an empty or too long title a too long body and missing recipients`() = runTest {
        val invalid = listOf(
            talkNotification.copy(title = "  "),
            talkNotification.copy(title = "x".repeat(AppNotification.MAX_TITLE_LENGTH + 1)),
            talkNotification.copy(body = "x".repeat(AppNotification.MAX_BODY_LENGTH + 1)),
            talkNotification.copy(roles = emptySet()),
        )
        invalid.forEach { notification ->
            assertIs<NotificationException.Invalid>(send(helper, notification).exceptionOrNull())
        }
        assertTrue(send(helper, talkNotification.copy(roles = emptySet(), groups = setOf(EventAudience.ALL))).isSuccess)
    }

    @Test
    fun `a changed informed status is sent to planners and helpers`() = runTest {
        assertTrue(notify(helper, slot, slot.copy(confirmed = true)).isSuccess)
        assertTrue(notify(helper, slot.copy(confirmed = true), slot).isSuccess)
        // Ein neuer Termin, der gleich als informiert gespeichert wird, zählt als Änderung.
        assertTrue(notify(helper, null, slot.copy(confirmed = true)).isSuccess)

        assertEquals(
            listOf(
                AppNotification(
                    category = NotificationCategory.TALK_PLANNING,
                    title = "Redner informiert",
                    body = "Vortrag am 22.11.2026",
                    roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.TALK_HELPER),
                ),
                AppNotification(
                    category = NotificationCategory.TALK_PLANNING,
                    title = "Informiert zurückgenommen",
                    body = "Vortrag am 22.11.2026",
                    roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.TALK_HELPER),
                ),
            ),
            repository.sent.take(2).map { it.second },
        )
        assertEquals(3, repository.sent.size)
    }

    @Test
    fun `an unchanged informed status sends nothing and send failures are reported`() = runTest {
        assertTrue(notify(helper, slot, slot.copy(note = "x")).isSuccess)
        assertTrue(notify(helper, null, slot).isSuccess)
        assertTrue(repository.sent.isEmpty())

        repository.result = Result.failure(IllegalStateException("offline"))
        assertEquals("offline", notify(helper, slot, slot.copy(confirmed = true)).exceptionOrNull()?.message)
    }
}
