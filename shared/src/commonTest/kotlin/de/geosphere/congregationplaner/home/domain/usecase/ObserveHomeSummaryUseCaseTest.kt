package de.geosphere.congregationplaner.home.domain.usecase

import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.EventHorizon
import de.geosphere.congregationplaner.congregation.testing.FakeCongregationMetadataRepository
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.event.domain.permission.EventTypePolicy
import de.geosphere.congregationplaner.event.domain.permission.SpecialEventPolicy
import de.geosphere.congregationplaner.event.domain.usecase.ObserveEventTypesUseCase
import de.geosphere.congregationplaner.event.domain.usecase.ObserveUpcomingEventsUseCase
import de.geosphere.congregationplaner.event.testing.FakeEventTypeRepository
import de.geosphere.congregationplaner.event.testing.FakeSpecialEventRepository
import de.geosphere.congregationplaner.home.domain.model.HomeEvent
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
    private val events = FakeSpecialEventRepository()
    private val eventTypes = FakeEventTypeRepository()
    private val congregations = FakeCongregationMetadataRepository(
        listOf(CongregationMetadata(id = testCongregation.congregationId, eventHorizon = EventHorizon(1))),
    )
    private val useCase = ObserveHomeSummaryUseCase(
        TalkSchedulePermissionPolicy(),
        UserPermissionPolicy(),
        ObserveTalkYearUseCase(schedule),
        ObserveMembersUseCase(users),
        ObserveHomeEventsUseCase(
            ObserveUpcomingEventsUseCase(events, SpecialEventPolicy()),
            ObserveScheduleEventsUseCase(schedule, congregations, EventTypePolicy()),
            ObserveEventTypesUseCase(eventTypes, EventTypePolicy()),
        ),
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
    fun `an approved member gets the upcoming events of his groups`() = runTest {
        val congress = SpecialEvent("1", "Kongress", today, LocalDate(2026, 11, 3))
        val eldersOnly = SpecialEvent("2", "Schulung", today, audience = setOf(EventAudience.ELDERS))
        events.events.value = listOf(congress, eldersOnly)

        assertEquals(listOf(HomeEvent("Kongress", today, LocalDate(2026, 11, 3))), summary(user()).events)
        assertEquals(setOf(EventAudience.ALL), events.requestedGroups)
    }

    @Test
    fun `an unapproved member gets no events and nothing is queried`() = runTest {
        events.observeError = IllegalStateException("must not be queried")

        assertTrue(summary(user(approved = false)).events.isEmpty())
    }

    @Test
    fun `a talk helper gets the talk block marked as helper`() = runTest {
        val result = summary(user(CongregationRole.TALK_HELPER))

        assertEquals(TalkSummary(asHelper = true), result.talk)
        assertNull(result.pendingApprovals)
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

    @Test
    fun `members get the event types for the icons and a failure only drops them`() = runTest {
        eventTypes.types.value = listOf(EventType("kk", "Kreiskongress", icon = "ic_cp_arena_duo"))
        events.events.value = listOf(SpecialEvent("1", "Kongress", today, eventTypeId = "kk"))

        assertEquals("ic_cp_arena_duo", summary(user()).events.single().icon)

        eventTypes.observeError = IllegalStateException("rules not deployed")
        assertNull(summary(user()).events.single().icon)
    }

    @Test
    fun `members get the slots with a week event within the horizon of the congregation`() = runTest {
        eventTypes.types.value = listOf(EventType("kk", "Kreiskongress", icon = "ic_cp_arena_duo"))
        schedule.slots.value = listOf(
            slot(daysFromToday(-1)) { it.copy(weekEventId = "kk", weekEventName = "Kongress") },
            slot(daysFromToday(7)) { it.copy(weekEventId = "kk", weekEventName = "Kongress") },
            slot(daysFromToday(14)),
            // One month after 2026-11-01 is 2026-12-01, the last day shown.
            slot(LocalDate(2026, 12, 1)) { it.copy(weekEventId = "x", weekEventName = "Besuch") },
            slot(LocalDate(2026, 12, 2)) { it.copy(weekEventId = "kk", weekEventName = "Kongress") },
        )
        events.events.value = listOf(SpecialEvent("1", "Dienst", daysFromToday(10)))

        assertEquals(
            listOf(
                HomeEvent("Kreiskongress", daysFromToday(7), icon = "ic_cp_arena_duo", fromSchedule = true),
                HomeEvent("Dienst", daysFromToday(10)),
                HomeEvent("Besuch", LocalDate(2026, 12, 1), fromSchedule = true),
            ),
            summary(user()).events,
        )
    }

    @Test
    fun `a failing schedule query keeps the special events`() = runTest {
        schedule.observeError = IllegalStateException("rules not deployed")
        events.events.value = listOf(SpecialEvent("1", "Dienst", today))

        assertEquals(listOf(HomeEvent("Dienst", today)), summary(user()).events)
    }

    @Test
    fun `an unapproved member gets no schedule events`() = runTest {
        schedule.slots.value = listOf(slot(today) { it.copy(weekEventId = "kk", weekEventName = "Kongress") })

        assertTrue(summary(user(approved = false)).events.isEmpty())
    }
}
