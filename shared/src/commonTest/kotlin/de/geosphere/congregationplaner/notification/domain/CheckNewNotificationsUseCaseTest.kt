package de.geosphere.congregationplaner.notification.domain

import de.geosphere.congregationplaner.auth.testing.FakeAuthRepository
import de.geosphere.congregationplaner.auth.testing.FakeAuthUser
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.notification.domain.model.NotificationCategory
import de.geosphere.congregationplaner.notification.domain.model.NotificationCheck
import de.geosphere.congregationplaner.notification.domain.model.ReceivedNotification
import de.geosphere.congregationplaner.notification.domain.usecase.CheckNewNotificationsUseCase
import de.geosphere.congregationplaner.notification.testing.FakeNotificationRepository
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.Gender
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class CheckNewNotificationsUseCaseTest {
    private val now = Instant.parse("2026-11-15T12:00:00Z")
    private val seenUntil = Instant.parse("2026-11-15T11:00:00Z")
    private val planner = AppUser(
        uid = "planner",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
        profile = MemberProfile(gender = Gender.MALE, baptized = true),
    )
    private val auth = FakeAuthRepository(FakeAuthUser(uid = "planner"))
    private val selection = FakeSelectionStore(SelectedCongregation(testCongregation, "Berlin"))
    private val users = FakeUserRepository(planner)
    private val repository = FakeNotificationRepository()
    private val check = CheckNewNotificationsUseCase(auth, selection, users, repository)

    private fun received(id: String, sender: String, minute: Int) = ReceivedNotification(
        id = id,
        category = NotificationCategory.TALK_PLANNING,
        title = "Redner informiert",
        body = "Vortrag am 22.11.2026",
        senderUid = sender,
        createdAt = Instant.parse("2026-11-15T11:${minute.toString().padStart(2, '0')}:00Z"),
    )

    @Test
    fun `fetches for the own groups and roles skips own notifications and moves to the newest`() = runTest {
        repository.fetchResult = Result.success(
            listOf(received("a", "helper", 10), received("b", "planner", 20), received("c", "helper", 15)),
        )

        val result = check(seenUntil, now).getOrThrow()

        assertEquals(
            NotificationCheck(
                listOf(received("a", "helper", 10), received("c", "helper", 15)),
                Instant.parse("2026-11-15T11:20:00Z"),
            ),
            result,
        )
        assertEquals(
            Triple(setOf(EventAudience.ALL, EventAudience.BROTHERS), setOf(CongregationRole.TALK_PLANNER), seenUntil),
            repository.fetches.single(),
        )
    }

    @Test
    fun `nothing new keeps the last state`() = runTest {
        assertEquals(NotificationCheck(emptyList(), seenUntil), check(seenUntil, now).getOrThrow())
    }

    @Test
    fun `the first check and anyone not allowed start at now without asking`() = runTest {
        val nothing = NotificationCheck(emptyList(), now)
        assertEquals(nothing, check(null, now).getOrThrow())

        users.user.value = planner.copy(approved = false)
        assertEquals(nothing, check(seenUntil, now).getOrThrow())

        users.user.value = null
        assertEquals(nothing, check(seenUntil, now).getOrThrow())

        users.user.value = planner
        selection.selection = null
        assertEquals(nothing, check(seenUntil, now).getOrThrow())

        selection.selection = SelectedCongregation(testCongregation, "Berlin")
        auth.user = null
        assertEquals(nothing, check(seenUntil, now).getOrThrow())

        assertTrue(repository.fetches.isEmpty())
    }

    @Test
    fun `a failed query is reported and keeps nothing`() = runTest {
        repository.fetchResult = Result.failure(IllegalStateException("offline"))

        assertEquals("offline", check(seenUntil, now).exceptionOrNull()?.message)
    }

    private class FakeSelectionStore(var selection: SelectedCongregation?) : CongregationSelectionStore {
        override fun load(): SelectedCongregation? = selection

        override fun save(selection: SelectedCongregation) {
            this.selection = selection
        }

        override fun clear() {
            selection = null
        }
    }
}
