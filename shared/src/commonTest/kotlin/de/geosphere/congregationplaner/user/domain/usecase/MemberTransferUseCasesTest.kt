package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.talk.domain.model.MemberRef
import de.geosphere.congregationplaner.talk.domain.model.SlotPerson
import de.geosphere.congregationplaner.talk.domain.model.TalkScheduleException
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
import de.geosphere.congregationplaner.talk.domain.permission.TalkSchedulePermissionPolicy
import de.geosphere.congregationplaner.talk.domain.usecase.DeleteVisitingSpeakerUseCase
import de.geosphere.congregationplaner.talk.domain.usecase.MoveSlotsToMemberUseCase
import de.geosphere.congregationplaner.talk.testing.FakeExternalSpeakerDirectoryRepository
import de.geosphere.congregationplaner.talk.testing.FakeTalkScheduleRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberTransferUseCasesTest {
    private val adminPlanner = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN, CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val berg = VisitingSpeaker(
        "e1",
        "c1",
        "Berg",
        "Lea",
        mobilePhone = "0170 1234567",
        landlinePhone = "kaputt!",
        email = "lea@example.com",
        talkNumbers = listOf(12, 45),
    )
    private val bergSlot = TalkSlot(
        LocalDate(2026, 11, 15),
        LocalTime(10, 0),
        externalSpeakerId = "e1",
        speakerLastName = "Berg",
        speakerFirstName = "Lea",
        speakerCongregation = "Nord",
    )
    private val otherSlot = TalkSlot(LocalDate(2026, 11, 22), LocalTime(10, 0))

    private val users = FakeUserRepository()
    private val schedule = FakeTalkScheduleRepository(listOf(bergSlot, otherSlot))
    private val directory = FakeExternalSpeakerDirectoryRepository(speakers = listOf(berg))
    private val userPolicy = UserPermissionPolicy()
    private val talkPolicy = TalkSchedulePermissionPolicy()
    private val transferPolicy = MemberTransferPolicy(userPolicy, talkPolicy)
    private val moveSlots = MoveSlotsToMemberUseCase(schedule, talkPolicy)

    private val adopt = AdoptVisitingSpeakersUseCase(
        CreateMemberWithoutAccountUseCase(users, userPolicy),
        moveSlots,
        DeleteVisitingSpeakerUseCase(directory, talkPolicy),
        transferPolicy,
    )
    private val link = LinkMemberAccountUseCase(
        UpdateMemberProfileUseCase(users, userPolicy),
        moveSlots,
        RemoveMemberUseCase(users, userPolicy),
        transferPolicy,
    )

    @Test
    fun `an adopted speaker becomes a member with his slots and leaves the directory`() = runBlocking {
        val adoption = adopt(adminPlanner, listOf(berg)).getOrThrow()

        val member = adoption.adopted.single()
        val profile = users.createdMembers.single().profile
        assertEquals("Berg" to "Lea", profile.lastName to profile.firstName)
        assertEquals(MemberCategory.BROTHER, profile.category)
        assertTrue(profile.baptized && profile.isSpeaker)
        assertTrue(profile.mobilePhone.isNotEmpty())
        // The broken landline number is dropped instead of blocking the adoption.
        assertEquals("", profile.landlinePhone)
        assertEquals(listOf("lea@example.com"), profile.emails)
        assertEquals(berg.talkNumbers, profile.talkNumbers)
        assertEquals(member.uid, schedule.slots.value.first().speakerMemberId)
        assertEquals(listOf(bergSlot.date), schedule.savedSlots.map { it.date })
        assertEquals(listOf("e1"), directory.deletedIds)
        assertTrue(adoption.failures.isEmpty())
    }

    @Test
    fun `speakers that cannot become members are reported and stay in the directory`() = runBlocking {
        val nameless = berg.copy(id = "e2", firstName = "")

        val adoption = adopt(adminPlanner, listOf(nameless)).getOrThrow()

        assertTrue(adoption.adopted.isEmpty())
        assertTrue(adoption.failures.single().startsWith("Berg:"))
        assertTrue(directory.deletedIds.isEmpty())
    }

    @Test
    fun `a failed slot update keeps the speaker in the directory`() = runBlocking {
        schedule.saveResult = Result.failure(IllegalStateException("offline"))

        val adoption = adopt(adminPlanner, listOf(berg)).getOrThrow()

        assertEquals("Berg, Lea: offline", adoption.failures.single())
        assertTrue(directory.deletedIds.isEmpty())
    }

    @Test
    fun `transfers need both the admin and the planner role`(): Unit = runBlocking {
        val adminOnly = adminPlanner.copy(roles = setOf(CongregationRole.CONGREGATION_ADMIN))
        val plannerOnly = adminPlanner.copy(roles = setOf(CongregationRole.TALK_PLANNER))

        listOf(adminOnly, plannerOnly).forEach { user ->
            assertIs<MemberProfileException.NotAllowed>(adopt(user, listOf(berg)).exceptionOrNull())
            assertIs<MemberProfileException.NotAllowed>(link(user, account, person).exceptionOrNull())
        }
        assertIs<TalkScheduleException.NotAllowed>(moveSlots(adminOnly, slotPerson, ref).exceptionOrNull())
    }

    private val account = AppUser(uid = "acc", email = "max@example.com", congregation = testCongregation)
    private val person = AppUser(
        uid = "p1",
        hasAccount = false,
        congregation = testCongregation,
        profile = MemberProfile(lastName = "Muster", firstName = "Max", category = MemberCategory.BROTHER),
    )
    private val slotPerson = SlotPerson.Member("p1")
    private val ref = MemberRef("acc", "Muster", "Max")

    @Test
    fun `linking moves profile and slots to the account and removes the person`() = runBlocking {
        val chaired = otherSlot.copy(chairmanMemberId = "p1", chairmanLastName = "Muster", chairmanFirstName = "Max")
        schedule.slots.value = listOf(chaired)

        val linked = link(adminPlanner, account, person).getOrThrow()

        assertEquals(person.profile, linked.profile)
        assertEquals("acc", users.updatedProfiles.single().uid)
        assertEquals("acc", schedule.slots.value.single().chairmanMemberId)
        assertEquals(listOf("p1"), users.removedUids)
    }

    @Test
    fun `linking needs an account and a person without account`() = runBlocking {
        assertIs<IllegalArgumentException>(link(adminPlanner, person, account).exceptionOrNull())
        assertIs<IllegalArgumentException>(link(adminPlanner, account, account).exceptionOrNull())
        assertTrue(users.removedUids.isEmpty())
    }

    @Test
    fun `link candidates are the persons without account sorted by their label`() {
        val zora = person.copy(uid = "p2", profile = MemberProfile(lastName = "Zora", firstName = "Ben"))
        val anna = person.copy(uid = "p3", profile = MemberProfile(lastName = "Berg", firstName = "Anna"))

        // The label is "first name last name": Anna Berg, Ben Zora, Max Muster.
        assertEquals(
            listOf("p3", "p2", "p1"),
            FindLinkCandidatesUseCase()(listOf(zora, account, person, anna)).map { it.uid },
        )
    }

    @Test
    fun `a failure while reading the slots is reported`() = runBlocking {
        schedule.observeError = IllegalStateException("offline")

        assertEquals("offline", moveSlots(adminPlanner, slotPerson, ref).exceptionOrNull()?.message)
    }

    @Test
    fun `nothing is written when no slot names the person`() = runBlocking {
        assertEquals(0, moveSlots(adminPlanner, slotPerson, ref).getOrThrow())
        assertTrue(schedule.savedSlots.isEmpty())
    }
}
