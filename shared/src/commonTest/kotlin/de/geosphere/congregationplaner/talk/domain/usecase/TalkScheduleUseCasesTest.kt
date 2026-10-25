package de.geosphere.congregationplaner.talk.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.Speaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.testing.FakeTalkCatalogRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.talk.testing.FakeWeekEventRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.usecase.ObserveMembersUseCase
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TalkScheduleUseCasesTest {
    private val repository = FakeTalkScheduleRepository()
    private val policy = TalkSchedulePermissionPolicy()
    private val save = SaveTalkSlotUseCase(repository, policy)
    private val delete = DeleteTalkSlotUseCase(repository, policy)

    private val admin = AppUser(
        uid = "1",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        congregation = testCongregation,
    )
    private val member = AppUser(uid = "2", approved = true, congregation = testCongregation)

    private val sunday = LocalDate(2026, 10, 25)
    private fun slot(date: LocalDate = sunday) = TalkSlot(date = date, time = LocalTime(17, 30))

    @Test
    fun `slot id is the iso date`() {
        assertEquals("2026-10-25", slot().id)
        assertEquals("2028-02-29", slot(LocalDate(2028, 2, 29)).id)
    }

    @Test
    fun `planner saves a slot in his own congregation`() = runBlocking {
        val result = save(admin, slot())

        assertTrue(result.isSuccess)
        assertEquals(listOf(slot()), repository.savedSlots)
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `save is denied without permission or congregation`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(save(member, slot()).exceptionOrNull())
        assertIs<TalkScheduleException.NotAllowed>(save(admin.copy(congregation = null), slot()).exceptionOrNull())
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `save accepts any weekday because the meeting day can move at short notice`() = runBlocking {
        assertTrue(save(admin, slot(LocalDate(2026, 10, 24))).isSuccess)
    }

    @Test
    fun `save checks the week event and trims its name`() = runBlocking {
        val invalid = listOf(
            slot().copy(weekEventId = "kongress", weekEventName = " "),
            slot().copy(weekEventId = "kongress", weekEventName = null),
            slot().copy(weekEventId = null, weekEventName = "Kongress"),
            slot().copy(weekEventId = "kongress", weekEventName = "x".repeat(TalkSlot.MAX_WEEK_EVENT_NAME_LENGTH + 1)),
        )
        invalid.forEach { assertIs<TalkScheduleException.InvalidWeekEvent>(save(admin, it).exceptionOrNull()) }

        save(admin, slot().copy(weekEventId = "kongress", weekEventName = " Kongress "))

        assertEquals("Kongress", repository.savedSlots.single().weekEventName)
    }

    @Test
    fun `save checks number and title of the talk together and trims the title`() = runBlocking {
        val invalid = listOf(
            slot().copy(talkNumber = 0, talkTitle = "Titel"),
            slot().copy(talkNumber = 12, talkTitle = " "),
            slot().copy(talkNumber = 12, talkTitle = null),
            slot().copy(talkNumber = null, talkTitle = "Titel"),
            slot().copy(talkNumber = 12, talkTitle = "x".repeat(TalkSlot.MAX_TALK_TITLE_LENGTH + 1)),
        )
        invalid.forEach { assertIs<TalkScheduleException.InvalidTalk>(save(admin, it).exceptionOrNull()) }
        assertTrue(repository.savedSlots.isEmpty())

        save(admin, slot().copy(talkNumber = 12, talkTitle = " Titel "))

        assertEquals(12, repository.savedSlots.single().talkNumber)
        assertEquals("Titel", repository.savedSlots.single().talkTitle)
    }

    @Test
    fun `save checks the three speaker fields together and trims the names`() = runBlocking {
        val invalid = listOf(
            slot().copy(speakerMemberId = "m1", speakerLastName = null, speakerFirstName = "Max"),
            slot().copy(speakerMemberId = "m1", speakerLastName = " ", speakerFirstName = "Max"),
            slot().copy(speakerMemberId = "m1", speakerLastName = "Muster", speakerFirstName = null),
            slot().copy(speakerMemberId = " ", speakerLastName = "Muster", speakerFirstName = "Max"),
            slot().copy(speakerMemberId = null, speakerLastName = "Muster", speakerFirstName = null),
            slot().copy(speakerMemberId = null, speakerLastName = null, speakerFirstName = "Max"),
            slot().copy(
                speakerMemberId = "m1",
                speakerLastName = "x".repeat(TalkSlot.MAX_PERSON_NAME_LENGTH + 1),
                speakerFirstName = "Max",
            ),
        )
        invalid.forEach { assertIs<TalkScheduleException.InvalidSpeaker>(save(admin, it).exceptionOrNull()) }
        assertTrue(repository.savedSlots.isEmpty())

        save(admin, slot().copy(speakerMemberId = "m1", speakerLastName = " Muster ", speakerFirstName = ""))

        assertEquals(Speaker("m1", "Muster", ""), repository.savedSlots.single().speaker)
    }

    @Test
    fun `speakers are the sorted members marked as speaker with a last name`() = runBlocking {
        fun member(uid: String, last: String, first: String, speaker: Boolean) =
            AppUser(uid = uid, profile = MemberProfile(lastName = last, firstName = first, isSpeaker = speaker))
        val users = FakeUserRepository(
            members = listOf(
                member("1", "zander", "Anna", true),
                member("2", "Berg", "Tim", false),
                member("3", "Berg", "Lea", true),
                member("4", "", "Ohne", true),
                member("5", "Berg", "adam", true),
            ),
        )

        val speakers = ObserveTalkMembersUseCase(ObserveMembersUseCase(users))(admin).first().speakers

        assertEquals(listOf("5", "3", "1"), speakers.map { it.memberId })
        assertEquals("Berg, adam", speakers.first().displayName)
        assertEquals("Muster", Speaker("x", "Muster", " ").displayName)
        assertEquals(testCongregation, users.requestedCongregation)
        val withoutCongregation = admin.copy(congregation = null)
        assertTrue(ObserveTalkMembersUseCase(ObserveMembersUseCase(users))(withoutCongregation).toList().isEmpty())
    }

    @Test
    fun `save accepts an external speaker with congregation and trims the fields`() = runBlocking {
        val external = slot().copy(
            externalSpeakerId = "e1",
            speakerLastName = " Extern ",
            speakerFirstName = " Eva ",
            speakerCongregation = " Nord ",
        )

        save(admin, external)

        val saved = repository.savedSlots.single()
        assertEquals(ExternalSpeaker("e1", "Extern", "Eva", "Nord"), saved.externalSpeaker)
        assertNull(saved.speaker)
    }

    @Test
    fun `save rejects inconsistent speakers`() = runBlocking {
        val external = slot().copy(
            externalSpeakerId = "e1",
            speakerLastName = "Extern",
            speakerFirstName = "Eva",
            speakerCongregation = "Nord",
        )
        val invalid = listOf(
            external.copy(speakerMemberId = "m1"),
            external.copy(speakerCongregation = null),
            external.copy(speakerCongregation = " "),
            external.copy(speakerCongregation = "x".repeat(TalkSlot.MAX_PERSON_NAME_LENGTH + 1)),
            external.copy(speakerLastName = null),
            external.copy(externalSpeakerId = null),
            slot().copy(
                speakerMemberId = "m1",
                speakerLastName = "Muster",
                speakerFirstName = "Max",
                speakerCongregation = "Nord",
            ),
        )

        invalid.forEach { assertIs<TalkScheduleException.InvalidSpeaker>(save(admin, it).exceptionOrNull()) }
        assertTrue(repository.savedSlots.isEmpty())
    }

    @Test
    fun `a member is never speaker and chairman of the same slot`() = runBlocking {
        val sameMember = slot().copy(
            speakerMemberId = "m1",
            speakerLastName = "Muster",
            speakerFirstName = "Max",
            chairmanMemberId = "m1",
            chairmanLastName = "Muster",
            chairmanFirstName = "Max",
        )

        assertIs<TalkScheduleException.SpeakerIsChairman>(save(admin, sameMember).exceptionOrNull())
        assertTrue(repository.savedSlots.isEmpty())

        val different = sameMember.copy(chairmanMemberId = "m2")
        assertTrue(save(admin, different).isSuccess)
        // Ein externer Redner ist kein Mitglied und kann daher nie mit dem Vorsitzenden zusammenfallen.
        val external = different.copy(
            speakerMemberId = null,
            externalSpeakerId = "m2",
            speakerCongregation = "Nord",
            chairmanMemberId = "m2",
        )
        assertTrue(save(admin, external).isSuccess)
    }

    @Test
    fun `chairmen are the sorted brothers with an appointment and a last name`() = runBlocking {
        fun member(uid: String, last: String, category: MemberCategory?, appointment: Appointment) = AppUser(
            uid = uid,
            profile = MemberProfile(
                lastName = last,
                firstName = "X",
                category = category,
                baptized = true,
                appointment = appointment,
            ),
        )
        val users = FakeUserRepository(
            members = listOf(
                member("1", "Zander", MemberCategory.BROTHER, Appointment.ELDER),
                member("2", "Berg", MemberCategory.BROTHER, Appointment.MINISTERIAL_SERVANT),
                member("3", "Alt", MemberCategory.BROTHER, Appointment.NONE),
                member("4", "Schwester", MemberCategory.SISTER, Appointment.ELDER),
                member("5", "", MemberCategory.BROTHER, Appointment.ELDER),
            ),
        )

        val chairmen = ObserveTalkMembersUseCase(ObserveMembersUseCase(users))(admin).first().chairmen

        assertEquals(listOf("2", "1"), chairmen.map { it.memberId })
    }

    @Test
    fun `save checks the three chairman fields together and trims the names`() = runBlocking {
        val invalid = listOf(
            slot().copy(chairmanMemberId = "c1", chairmanLastName = null, chairmanFirstName = "Max"),
            slot().copy(chairmanMemberId = "c1", chairmanLastName = " ", chairmanFirstName = "Max"),
            slot().copy(chairmanMemberId = "c1", chairmanLastName = "Muster", chairmanFirstName = null),
            slot().copy(chairmanMemberId = " ", chairmanLastName = "Muster", chairmanFirstName = "Max"),
            slot().copy(chairmanMemberId = null, chairmanLastName = "Muster", chairmanFirstName = null),
            slot().copy(
                chairmanMemberId = "c1",
                chairmanLastName = "x".repeat(TalkSlot.MAX_PERSON_NAME_LENGTH + 1),
                chairmanFirstName = "Max",
            ),
        )
        invalid.forEach { assertIs<TalkScheduleException.InvalidChairman>(save(admin, it).exceptionOrNull()) }
        assertTrue(repository.savedSlots.isEmpty())

        save(admin, slot().copy(chairmanMemberId = "c1", chairmanLastName = " Muster ", chairmanFirstName = " Max "))

        assertEquals(MemberRef("c1", "Muster", "Max"), repository.savedSlots.single().chairman)
        assertEquals(null, slot().chairman)
    }

    @Test
    fun `talk catalog is sorted by number and keeps inactive talks`() = runBlocking {
        val catalog = FakeTalkCatalogRepository(listOf(Talk(30, "C"), Talk(2, "B", active = false), Talk(10, "A")))

        val talks = ObserveTalkCatalogUseCase(catalog)().first()

        assertEquals(listOf(2, 10, 30), talks.map { it.number })
        assertEquals(false, talks.first().active)
    }

    @Test
    fun `repository failure is passed on`() = runBlocking {
        val failure = IllegalStateException("offline")
        repository.saveResult = Result.failure(failure)

        assertEquals(failure, save(admin, slot()).exceptionOrNull())
    }

    @Test
    fun `delete requires the permission`() = runBlocking {
        assertIs<TalkScheduleException.NotAllowed>(delete(member, sunday).exceptionOrNull())
        assertTrue(repository.deletedDates.isEmpty())

        assertTrue(delete(admin, sunday).isSuccess)
        assertEquals(listOf(sunday), repository.deletedDates)
    }

    @Test
    fun `move saves the new day first and then deletes the old one`() = runBlocking {
        val moved = slot(LocalDate(2026, 10, 24))

        val result = MoveTalkSlotUseCase(save, delete)(admin, from = sunday, slot = moved)

        assertEquals(moved, result.getOrThrow())
        assertEquals(listOf(moved), repository.savedSlots)
        assertEquals(listOf(sunday), repository.deletedDates)
    }

    @Test
    fun `move on the same day only saves and a failed save deletes nothing`() = runBlocking {
        val move = MoveTalkSlotUseCase(save, delete)

        assertTrue(move(admin, sunday, slot()).isSuccess)
        assertTrue(repository.deletedDates.isEmpty())

        repository.saveResult = Result.failure(IllegalStateException("offline"))
        assertTrue(move(admin, from = LocalDate(2026, 10, 18), slot = slot()).isFailure)
        assertTrue(repository.deletedDates.isEmpty())
    }

    @Test
    fun `move reports a failed delete of the old day`() = runBlocking {
        repository.deleteResult = Result.failure(IllegalStateException("offline"))

        val result = MoveTalkSlotUseCase(save, delete)(admin, from = sunday, slot = slot(LocalDate(2026, 10, 24)))

        assertEquals("offline", result.exceptionOrNull()?.message)
    }

    @Test
    fun `observe returns nothing for a user without congregation`() = runBlocking {
        val slots = ObserveTalkYearUseCase(repository)(member.copy(congregation = null), 2026).toList()

        assertTrue(slots.isEmpty())
        assertNull(repository.requestedYear)
    }

    @Test
    fun `week events are sorted by name ignoring case`() = runBlocking {
        val events = FakeWeekEventRepository(listOf(WeekEvent("b", "kongress"), WeekEvent("a", "Besuch")))

        val sorted = ObserveWeekEventsUseCase(events)().first()

        assertEquals(listOf("a", "b"), sorted.map { it.id })
    }

    @Test
    fun `observe reads the year of the users congregation`() = runBlocking {
        repository.slots.value = listOf(slot())

        val slots = ObserveTalkYearUseCase(repository)(admin, 2026).first()

        assertEquals(listOf(slot()), slots)
        assertEquals(2026, repository.requestedYear)
        assertEquals(testCongregation, repository.requestedCongregation)
    }
}
