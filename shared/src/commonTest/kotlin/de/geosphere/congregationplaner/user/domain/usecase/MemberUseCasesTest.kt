package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.model.UserPermission
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberUseCasesTest {
    @Test
    fun `members are sorted with pending users first and then by name`() = runBlocking {
        val repository = FakeUserRepository(
            members = listOf(
                AppUser(uid = "1", displayName = "Zoe", approved = true),
                AppUser(uid = "2", email = "neu@example.com"),
                AppUser(uid = "3", displayName = "anna", approved = true),
            ),
        )

        val members = ObserveMembersUseCase(repository)(testCongregation).first()

        assertEquals(listOf("2", "3", "1"), members.map { it.uid })
        assertEquals(testCongregation, repository.requestedCongregation)
    }

    @Test
    fun `members without account never count as pending and are sorted by last name`() = runBlocking {
        val repository = FakeUserRepository(
            members = listOf(
                AppUser(uid = "1", approved = true, profile = MemberProfile(lastName = "Zander", firstName = "Anna")),
                AppUser(uid = "2", hasAccount = false, profile = MemberProfile(lastName = "Berg", firstName = "Tim")),
                AppUser(uid = "3", approved = true, profile = MemberProfile(lastName = "Berg", firstName = "Lea")),
                AppUser(uid = "4", email = "neu@example.com"),
            ),
        )

        val members = ObserveMembersUseCase(repository)(testCongregation).first()

        assertEquals(listOf("4", "3", "2", "1"), members.map { it.uid })
    }

    @Test
    fun `update rejects members without id or with unassignable role`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateMemberUseCase(repository)

        assertIs<IllegalArgumentException>(useCase(testCongregation, AppUser(uid = " ")).exceptionOrNull())
        assertIs<IllegalArgumentException>(
            useCase(testCongregation, AppUser(uid = "1", role = UserRole.NONE)).exceptionOrNull(),
        )
        assertTrue(repository.updatedMembers.isEmpty())

        val member = AppUser(uid = "1", approved = true, role = UserRole.CONGREGATION_ADMIN)
        assertEquals(member, useCase(testCongregation, member).getOrThrow())
        assertEquals(listOf(member), repository.updatedMembers)
    }

    @Test
    fun `update rejects access for members without account`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateMemberUseCase(repository)
        val member = AppUser(uid = "1", hasAccount = false)

        listOf(
            member.copy(approved = true),
            member.copy(role = UserRole.CONGREGATION_ADMIN),
            member.copy(permissions = UserPermission.defaults),
        ).forEach { invalid ->
            assertIs<MemberProfileException.AccessWithoutAccount>(useCase(testCongregation, invalid).exceptionOrNull())
        }
        assertTrue(repository.updatedMembers.isEmpty())

        assertEquals(member, useCase(testCongregation, member).getOrThrow())
    }

    @Test
    fun `profile update trims input and saves only the profile`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(
            uid = "1",
            profile = MemberProfile(lastName = " Muster ", firstName = " Max ", mobilePhone = " 0170 123 "),
        )

        val saved = UpdateMemberProfileUseCase(repository)(testCongregation, member).getOrThrow()

        val expected = member.copy(
            profile = MemberProfile(lastName = "Muster", firstName = "Max", mobilePhone = "0170 123"),
        )
        assertEquals(expected, saved)
        assertEquals(listOf(expected), repository.updatedProfiles)
    }

    @Test
    fun `profile update rejects invalid profiles`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateMemberProfileUseCase(repository)
        val member = AppUser(uid = "1")

        assertIs<IllegalArgumentException>(useCase(testCongregation, AppUser(uid = "")).exceptionOrNull())
        suspend fun errorFor(profile: MemberProfile, hasAccount: Boolean = true) =
            useCase(testCongregation, member.copy(hasAccount = hasAccount, profile = profile)).exceptionOrNull()

        assertIs<MemberProfileException.BaptizedChild>(errorFor(profile(MemberCategory.CHILD, baptized = true)))
        assertIs<MemberProfileException.AppointmentNotAllowed>(
            errorFor(profile(MemberCategory.SISTER, baptized = true, elder = true)),
        )
        assertIs<MemberProfileException.AppointmentNotAllowed>(errorFor(profile(MemberCategory.BROTHER, elder = true)))
        assertIs<MemberProfileException.SpeakerNotAllowed>(
            errorFor(MemberProfile(category = MemberCategory.SISTER, isSpeaker = true)),
        )
        assertIs<MemberProfileException.SpeakerNotAllowed>(errorFor(MemberProfile(category = null, isSpeaker = true)))
        assertIs<MemberProfileException.InvalidPhone>(errorFor(MemberProfile(landlinePhone = "030-abc")))
        assertIs<MemberProfileException.NameTooLong>(errorFor(MemberProfile(lastName = "x".repeat(101))))
        // Mitglieder mit Konto sind über ihr Login erkennbar; ohne Konto ist der Name Pflicht.
        assertIs<MemberProfileException.MissingName>(errorFor(MemberProfile(), hasAccount = false))
        assertTrue(repository.updatedProfiles.isEmpty())

        useCase(testCongregation, member).getOrThrow()
        val elder = member.copy(profile = profile(MemberCategory.BROTHER, baptized = true, elder = true))
        useCase(testCongregation, elder).getOrThrow()
        val speaker = member.copy(profile = MemberProfile(category = MemberCategory.BROTHER, isSpeaker = true))
        useCase(testCongregation, speaker).getOrThrow()
        assertEquals(listOf(member, elder, speaker), repository.updatedProfiles)
    }

    @Test
    fun `members without account need a name and are created through the repository`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = CreateMemberWithoutAccountUseCase(repository)

        assertIs<MemberProfileException.MissingName>(
            useCase(testCongregation, MemberProfile(lastName = "Muster", firstName = " ")).exceptionOrNull(),
        )
        assertTrue(repository.createdMembers.isEmpty())

        val created = useCase(
            testCongregation,
            MemberProfile(lastName = " Muster", firstName = "Lena ", category = MemberCategory.CHILD),
        ).getOrThrow()

        assertEquals(
            MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD),
            created.profile,
        )
        assertEquals(listOf(created), repository.createdMembers)
    }

    @Test
    fun `remove rejects blank ids and delegates otherwise`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = RemoveMemberUseCase(repository)

        assertIs<IllegalArgumentException>(useCase(testCongregation, "").exceptionOrNull())
        useCase(testCongregation, "1").getOrThrow()
        assertEquals(listOf("1"), repository.removedUids)
    }

    private fun profile(category: MemberCategory, baptized: Boolean = false, elder: Boolean = false) = MemberProfile(
        category = category,
        baptized = baptized,
        appointment = if (elder) Appointment.ELDER else Appointment.NONE,
    )
}
