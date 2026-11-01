package de.geosphere.congregationplaner.home.domain.usecase

import de.geosphere.congregationplaner.home.domain.model.HomeSummary
import de.geosphere.congregationplaner.home.domain.model.TalkSummary
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.ObserveTalkYearUseCase
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObserveHomeSummaryUseCaseTest {
    private val today = LocalDate(2026, 11, 1)

    private val schedule = FakeTalkScheduleRepository()
    private val users = FakeUserRepository()
    private val useCase = ObserveHomeSummaryUseCase(
        TalkSchedulePermissionPolicy(),
        UserPermissionPolicy(),
        ObserveTalkYearUseCase(schedule),
        ObserveMembersUseCase(users),
    )

    private fun user(vararg roles: CongregationRole, approved: Boolean = true) =
        AppUser(uid = "u", approved = approved, roles = roles.toSet(), congregation = testCongregation)

    private fun slot(date: LocalDate, block: (TalkSlot) -> TalkSlot = { it }) = block(TalkSlot(date, LocalTime(17, 30)))

    private fun TalkSlot.planned() =
        copy(talkNumber = 12, talkTitle = "Titel", speakerMemberId = "m", speakerLastName = "M")

    private fun daysFromToday(days: Int) = LocalDate.fromEpochDays(today.toEpochDays() + days)

    private suspend fun summary(user: AppUser) = useCase(user, today).first()

    @Test
    fun `a member without role gets no block and nothing is queried`() = runTest {
        schedule.observeError = IllegalStateException("must not be queried")

        assertEquals(HomeSummary(), summary(user()))
    }

    @Test
    fun `a talk planner gets the talk block only`() = runTest {
        val result = summary(user(CongregationRole.TALK_PLANNER))

        assertEquals(TalkSummary(), result.talk)
        assertNull(result.pendingApprovals)
    }

    @Test
    fun `an admin gets the approvals block only`() = runTest {
        users.members.value = listOf(
            AppUser(uid = "a", approved = false),
            AppUser(uid = "b", approved = false, hasAccount = false),
            AppUser(uid = "c", approved = true),
        )

        val result = summary(user(CongregationRole.CONGREGATION_ADMIN))

        assertNull(result.talk)
        assertEquals(1, result.pendingApprovals)
    }

    @Test
    fun `an admin without open approvals sees zero`() = runTest {
        assertEquals(0, summary(user(CongregationRole.CONGREGATION_ADMIN)).pendingApprovals)
    }

    @Test
    fun `a task planner gets no data block`() = runTest {
        assertEquals(HomeSummary(), summary(user(CongregationRole.TASK_PLANNER)))
    }

    @Test
    fun `a user who is not approved gets no block`() = runTest {
        schedule.observeError = IllegalStateException("must not be queried")

        val pending = user(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN, approved = false)

        val result = summary(pending)

        assertEquals(HomeSummary(), result)
    }

    @Test
    fun `a user without congregation gets no block`() = runTest {
        val withoutCongregation = AppUser(
            uid = "g",
            approved = true,
            roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN),
        )

        assertEquals(HomeSummary(), summary(withoutCongregation))
    }

    @Test
    fun `both blocks appear for both roles`() = runTest {
        val result = summary(user(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN))

        assertEquals(TalkSummary(), result.talk)
        assertEquals(0, result.pendingApprovals)
    }

    @Test
    fun `next slot and open dates come from the schedule`() = runTest {
        schedule.slots.value = listOf(
            slot(daysFromToday(-7)).planned(),
            slot(daysFromToday(7)).planned(),
            slot(daysFromToday(14)),
        )

        val talk = summary(user(CongregationRole.TALK_PLANNER)).talk

        assertEquals(daysFromToday(7), talk?.nextSlot?.date)
        assertEquals(listOf(daysFromToday(14)), talk?.openDates)
    }

    @Test
    fun `a slot beyond four weeks is not counted but can be the next one`() = runTest {
        schedule.slots.value = listOf(slot(daysFromToday(TalkSummary.HORIZON_DAYS)))

        val talk = summary(user(CongregationRole.TALK_PLANNER)).talk

        assertEquals(daysFromToday(TalkSummary.HORIZON_DAYS), talk?.nextSlot?.date)
        assertTrue(talk?.openDates.orEmpty().isEmpty())
    }

    @Test
    fun `cancelled slots are neither next nor open`() = runTest {
        schedule.slots.value = listOf(
            slot(daysFromToday(0)) { it.copy(talkCancelled = true) },
            slot(daysFromToday(7)).planned(),
        )

        val talk = summary(user(CongregationRole.TALK_PLANNER)).talk

        assertEquals(daysFromToday(7), talk?.nextSlot?.date)
        assertTrue(talk?.openDates.orEmpty().isEmpty())
    }

    @Test
    fun `a talk without speaker is open and an external speaker counts`() = runTest {
        schedule.slots.value = listOf(
            slot(daysFromToday(7)) { it.copy(talkNumber = 1, talkTitle = "T") },
            slot(daysFromToday(14)) {
                it.copy(talkNumber = 1, talkTitle = "T", externalSpeakerId = "e", speakerLastName = "E")
            },
        )

        val talk = summary(user(CongregationRole.TALK_PLANNER)).talk

        assertEquals(listOf(daysFromToday(7)), talk?.openDates)
    }

    @Test
    fun `slots of both years are combined once at the turn of the year`() = runTest {
        val december = LocalDate(2026, 12, 20)
        schedule.slots.value = listOf(slot(LocalDate(2026, 12, 27)), slot(LocalDate(2027, 1, 3)))

        val talk = useCase(user(CongregationRole.TALK_PLANNER), december).first().talk

        assertEquals(listOf(LocalDate(2026, 12, 27), LocalDate(2027, 1, 3)), talk?.openDates)
    }

    @Test
    fun `the schedule error reaches the collector`() = runTest {
        schedule.observeError = IllegalStateException("permission denied")

        val failure = runCatching { summary(user(CongregationRole.TALK_PLANNER)) }.exceptionOrNull()

        assertEquals("permission denied", failure?.message)
    }
}
