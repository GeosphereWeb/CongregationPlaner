package de.geosphere.congregationplaner.notification.domain

import de.geosphere.congregationplaner.auth.testing.FakeAuthRepository
import de.geosphere.congregationplaner.auth.testing.FakeAuthUser
import de.geosphere.congregationplaner.notification.domain.usecase.CheckSpeakerRemindersUseCase
import de.geosphere.congregationplaner.notification.domain.usecase.SpeakerReminderCheck
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.SelectedCongregation
import de.geosphere.congregationplaner.organization.domain.repository.CongregationSelectionStore
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.repository.TalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckSpeakerRemindersUseCaseTest {
    // Sonntag, 22.11.2026; die Woche beginnt am Montag, 16.11.
    private val sunday = LocalDate(2026, 11, 22)
    private val planned = TalkSlot(
        date = sunday,
        time = LocalTime(10, 0),
        speakerMemberId = "m1",
        speakerLastName = "Muster",
        speakerFirstName = "Max",
    )
    private val helper = AppUser(
        uid = "helper",
        approved = true,
        roles = setOf(CongregationRole.TALK_HELPER),
        congregation = testCongregation,
    )
    private val auth = FakeAuthRepository(FakeAuthUser(uid = "helper"))
    private val selection = Selection(SelectedCongregation(testCongregation, "Berlin"))
    private val users = FakeUserRepository(helper)
    private val schedule = FakeTalkScheduleRepository(listOf(planned))
    private val check = CheckSpeakerRemindersUseCase(auth, selection, users, schedule, TalkSchedulePermissionPolicy())

    private fun at(date: LocalDate, hour: Int = 9) = LocalDateTime(date, LocalTime(hour, 0))

    @Test
    fun `reminds from monday to the talk day once a day from eight`() = runTest {
        val monday = LocalDate(2026, 11, 16)

        assertEquals(SpeakerReminderCheck(listOf(planned), remindNow = true), check(at(monday), null).getOrThrow())
        assertTrue(check(at(sunday), remindedOn = LocalDate(2026, 11, 21)).getOrThrow().remindNow)
        // Heute schon erinnert oder noch vor 8 Uhr: offen, aber keine neue Erinnerung.
        assertFalse(check(at(monday), remindedOn = monday).getOrThrow().remindNow)
        assertFalse(check(at(monday, hour = 7), null).getOrThrow().remindNow)
        assertEquals(listOf(planned), check(at(monday, hour = 7), null).getOrThrow().open)
    }

    @Test
    fun `no reminder before the week after the talk or once the speaker is informed`() = runTest {
        assertEquals(nothing, check(at(LocalDate(2026, 11, 15)), null).getOrThrow())
        assertEquals(nothing, check(at(LocalDate(2026, 11, 23)), null).getOrThrow())

        schedule.slots.value = listOf(planned.copy(confirmed = true))
        assertEquals(nothing, check(at(sunday), null).getOrThrow())
    }

    @Test
    fun `slots without speaker or with a cancelled talk need no reminder`() = runTest {
        schedule.slots.value = listOf(
            planned.copy(speakerMemberId = null, speakerLastName = null, speakerFirstName = null),
            planned.copy(date = LocalDate(2026, 11, 21), talkCancelled = true),
        )

        assertEquals(nothing, check(at(sunday), null).getOrThrow())
    }

    @Test
    fun `only planners and helpers are reminded`() = runTest {
        val planner = helper.copy(roles = setOf(CongregationRole.TALK_PLANNER))
        users.user.value = planner
        assertTrue(check(at(sunday), null).getOrThrow().remindNow)

        users.user.value = helper.copy(roles = setOf(CongregationRole.TASK_PLANNER))
        assertEquals(nothing, check(at(sunday), null).getOrThrow())

        users.user.value = helper.copy(approved = false)
        assertEquals(nothing, check(at(sunday), null).getOrThrow())

        users.user.value = helper
        selection.selection = null
        assertEquals(nothing, check(at(sunday), null).getOrThrow())

        selection.selection = SelectedCongregation(testCongregation, "Berlin")
        auth.user = null
        assertEquals(nothing, check(at(sunday), null).getOrThrow())
    }

    @Test
    fun `the week may cross the turn of the year`() = runTest {
        val newYearSunday = planned.copy(date = LocalDate(2027, 1, 3))
        val byYear = ByYear(listOf(newYearSunday))
        val crossing = CheckSpeakerRemindersUseCase(auth, selection, users, byYear, TalkSchedulePermissionPolicy())

        val result = crossing(at(LocalDate(2026, 12, 28)), null).getOrThrow()

        assertEquals(listOf(newYearSunday), result.open)
        assertEquals("03.01.2027: Muster, Max ist noch nicht informiert.", result.body)
        assertEquals("Redner informieren", result.title)
    }

    @Test
    fun `a failed load is reported`() = runTest {
        schedule.observeError = IllegalStateException("offline")

        assertEquals("offline", check(at(sunday), null).exceptionOrNull()?.message)
    }

    private val nothing = SpeakerReminderCheck(emptyList(), remindNow = false)

    /** Liefert je Jahr nur dessen Termine, wie die echte Datenbank. */
    private class ByYear(
        slots: List<TalkSlot>,
        private val all: FakeTalkScheduleRepository = FakeTalkScheduleRepository(slots),
    ) : TalkScheduleRepository by all {
        override fun observeYear(congregation: CongregationRef, year: Int): Flow<List<TalkSlot>> =
            all.observeYear(congregation, year).map { slots -> slots.filter { it.date.year == year } }
    }

    private class Selection(var selection: SelectedCongregation?) : CongregationSelectionStore {
        override fun load(): SelectedCongregation? = selection

        override fun save(selection: SelectedCongregation) {
            this.selection = selection
        }

        override fun clear() {
            selection = null
        }
    }
}
