package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.MemberProfileException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MemberUseCasesTest {
    private val policy = UserPermissionPolicy()
    private val admin = AppUser(
        uid = "admin",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )

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
    fun `approval needs an id an editable member and an account`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = SetMemberApprovalUseCase(repository, policy)

        assertIs<IllegalArgumentException>(useCase(admin, AppUser(uid = " "), true).exceptionOrNull())
        // Der Admin sperrt sich nicht selbst aus.
        assertIs<MemberProfileException.NotAllowed>(useCase(admin, admin, false).exceptionOrNull())
        assertIs<MemberProfileException.AccessWithoutAccount>(
            useCase(admin, AppUser(uid = "1", hasAccount = false), true).exceptionOrNull(),
        )
        assertTrue(repository.updatedMembers.isEmpty())

        val saved = useCase(admin, AppUser(uid = "1"), true).getOrThrow()
        assertTrue(saved.approved)
        assertEquals(listOf(saved), repository.updatedMembers)
    }

    @Test
    fun `roles are granted and removed while the needed admin role stays`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = SetMemberRoleUseCase(repository, policy)
        val otherAdmin = AppUser(uid = "2", approved = true, roles = setOf(CongregationRole.CONGREGATION_ADMIN))
        val members = listOf(admin, otherAdmin)
        val adminRole = CongregationRole.CONGREGATION_ADMIN

        val planner = useCase(admin, AppUser(uid = "1"), CongregationRole.TALK_PLANNER, true, members).getOrThrow()
        assertEquals(setOf(CongregationRole.TALK_PLANNER), planner.roles)
        assertEquals(emptySet(), useCase(admin, otherAdmin, adminRole, false, members).getOrThrow().roles)

        assertIs<MemberProfileException.NotAllowed>(useCase(admin, admin, adminRole, false, members).exceptionOrNull())
        assertIs<MemberProfileException.NotAllowed>(
            useCase(admin, otherAdmin, adminRole, false, listOf(otherAdmin)).exceptionOrNull(),
        )
        assertIs<MemberProfileException.AccessWithoutAccount>(
            useCase(admin, AppUser(uid = "3", hasAccount = false), adminRole, true, members).exceptionOrNull(),
        )
        assertEquals(2, repository.updatedMembers.size)
    }

    @Test
    fun `only an approved congregation admin changes members`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(uid = "1")
        val notAllowed = listOf(
            admin.copy(roles = setOf(CongregationRole.TALK_PLANNER)),
            admin.copy(approved = false),
            admin.copy(congregation = null),
        )

        notAllowed.forEach { user ->
            listOf(
                SetMemberApprovalUseCase(repository, policy)(user, member, true).exceptionOrNull(),
                SetMemberRoleUseCase(repository, policy)(
                    user,
                    member,
                    CongregationRole.TALK_PLANNER,
                    true,
                    listOf(member),
                ).exceptionOrNull(),
                UpdateMemberProfileUseCase(repository, policy)(user, member).exceptionOrNull(),
                CreateMemberWithoutAccountUseCase(repository, policy)(user, named()).exceptionOrNull(),
                RemoveMemberUseCase(repository, policy)(user, member).exceptionOrNull(),
            ).forEach { assertIs<MemberProfileException.NotAllowed>(it) }
        }
        assertTrue(repository.updatedMembers.isEmpty())
        assertTrue(repository.updatedProfiles.isEmpty())
        assertTrue(repository.createdMembers.isEmpty())
        assertTrue(repository.removedUids.isEmpty())
    }

    @Test
    fun `profile update trims input and saves only the profile`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(
            uid = "1",
            profile = MemberProfile(lastName = " Muster ", firstName = " Max ", mobilePhone = " 0170 123 "),
        )

        val saved = UpdateMemberProfileUseCase(repository, policy)(admin, member).getOrThrow()

        val expected = member.copy(
            profile = MemberProfile(lastName = "Muster", firstName = "Max", mobilePhone = "0170 123"),
        )
        assertEquals(expected, saved)
        assertEquals(listOf(expected), repository.updatedProfiles)
    }

    @Test
    fun `profile update rejects invalid profiles`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateMemberProfileUseCase(repository, policy)
        val member = AppUser(uid = "1")

        assertIs<IllegalArgumentException>(useCase(admin, AppUser(uid = "")).exceptionOrNull())
        suspend fun errorFor(profile: MemberProfile, hasAccount: Boolean = true) =
            useCase(admin, member.copy(hasAccount = hasAccount, profile = profile)).exceptionOrNull()

        assertIs<MemberProfileException.BaptizedChild>(errorFor(profile(MemberCategory.CHILD, baptized = true)))
        assertIs<MemberProfileException.AppointmentNotAllowed>(
            errorFor(profile(MemberCategory.SISTER, baptized = true, elder = true)),
        )
        assertIs<MemberProfileException.AppointmentNotAllowed>(errorFor(profile(MemberCategory.BROTHER, elder = true)))
        assertIs<MemberProfileException.SpeakerNotAllowed>(
            errorFor(MemberProfile(category = MemberCategory.SISTER, isSpeaker = true)),
        )
        assertIs<MemberProfileException.SpeakerNotAllowed>(errorFor(MemberProfile(category = null, isSpeaker = true)))
        val brother = MemberProfile(category = MemberCategory.BROTHER, isSpeaker = true)
        assertIs<MemberProfileException.InvalidTalks>(
            errorFor(brother.copy(isSpeaker = false, talkNumbers = listOf(1))),
        )
        assertIs<MemberProfileException.InvalidTalks>(errorFor(brother.copy(talkNumbers = listOf(0))))
        assertIs<MemberProfileException.InvalidTalks>(errorFor(brother.copy(talkNumbers = listOf(10_000))))
        assertIs<MemberProfileException.InvalidTalks>(errorFor(brother.copy(talkNumbers = (1..201).toList())))
        assertIs<MemberProfileException.InvalidPhone>(errorFor(MemberProfile(landlinePhone = "030-abc")))
        assertIs<MemberProfileException.NameTooLong>(errorFor(MemberProfile(lastName = "x".repeat(101))))
        // Mitglieder mit Konto sind über ihr Login erkennbar; ohne Konto ist der Name Pflicht.
        assertIs<MemberProfileException.MissingName>(errorFor(MemberProfile(), hasAccount = false))
        assertTrue(repository.updatedProfiles.isEmpty())

        useCase(admin, member).getOrThrow()
        val elder = member.copy(profile = profile(MemberCategory.BROTHER, baptized = true, elder = true))
        useCase(admin, elder).getOrThrow()
        val speaker = member.copy(profile = MemberProfile(category = MemberCategory.BROTHER, isSpeaker = true))
        useCase(admin, speaker).getOrThrow()
        val withTalks = speaker.copy(profile = speaker.profile.copy(talkNumbers = listOf(45, 12, 12)))
        useCase(admin, withTalks).getOrThrow()
        assertEquals(listOf(12, 45), repository.updatedProfiles.last().profile.talkNumbers)
        assertEquals(listOf(member, elder, speaker), repository.updatedProfiles.dropLast(1))
    }

    @Test
    fun `members without account need a name and are created through the repository`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = CreateMemberWithoutAccountUseCase(repository, policy)

        assertIs<MemberProfileException.MissingName>(
            useCase(admin, MemberProfile(lastName = "Muster", firstName = " ")).exceptionOrNull(),
        )
        assertTrue(repository.createdMembers.isEmpty())

        val created = useCase(
            admin,
            MemberProfile(lastName = " Muster", firstName = "Lena ", category = MemberCategory.CHILD),
        ).getOrThrow()

        assertEquals(
            MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD),
            created.profile,
        )
        assertEquals(listOf(created), repository.createdMembers)
    }

    @Test
    fun `remove rejects blank ids`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = RemoveMemberUseCase(repository, policy)

        assertIs<IllegalArgumentException>(useCase(admin, AppUser(uid = "")).exceptionOrNull())
        useCase(admin, AppUser(uid = "1")).getOrThrow()
        assertEquals(listOf("1"), repository.removedUids)
    }

    @Test
    fun `a global admin without a member document saves the own profile in the admin document`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateOwnProfileUseCase(repository)
        // Auch mit einer lokal gemerkten Versammlung: gespeichert wird nie im Mitgliedsdokument.
        val globalAdmins = listOf(
            AppUser(uid = "1", approved = true, isGlobalAdmin = true, isMember = false),
            AppUser(
                uid = "1",
                approved = true,
                congregation = testCongregation,
                isGlobalAdmin = true,
                isMember = false,
            ),
        )

        globalAdmins.forEach { globalAdmin ->
            val saved = useCase(globalAdmin, named(mobilePhone = " 0170 123456 ")).getOrThrow()
            assertEquals("Muster", saved.profile.lastName)
            assertEquals("0170 123456", saved.profile.mobilePhone)
        }

        assertEquals(2, repository.updatedGlobalAdminProfiles.size)
        assertEquals("1", repository.updatedGlobalAdminProfiles.first().first)
        assertEquals("Muster", repository.updatedGlobalAdminProfiles.first().second.lastName)
        assertTrue(repository.updatedOwnProfiles.isEmpty())
    }

    @Test
    fun `a global admin profile needs both names and reports storage failures`(): Unit = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateOwnProfileUseCase(repository)
        val globalAdmin = AppUser(uid = "1", approved = true, isGlobalAdmin = true, isMember = false)

        assertIs<MemberProfileException.MissingName>(
            useCase(globalAdmin, MemberProfile(lastName = "Muster")).exceptionOrNull(),
        )
        assertTrue(repository.updatedGlobalAdminProfiles.isEmpty())

        repository.updateGlobalAdminProfileResult = Result.failure(IllegalStateException("denied"))
        assertIs<IllegalStateException>(useCase(globalAdmin, named()).exceptionOrNull())
    }

    @Test
    fun `a global admin who is a member saves in the member document`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation, isGlobalAdmin = true)

        UpdateOwnProfileUseCase(repository)(member, named()).getOrThrow()

        assertEquals(1, repository.updatedOwnProfiles.size)
        assertTrue(repository.updatedGlobalAdminProfiles.isEmpty())
    }

    @Test
    fun `own profile keeps up to three e-mails and drops empty ones`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateOwnProfileUseCase(repository)
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation)

        val saved = useCase(member, named(emails = listOf(" max@beispiel.de ", "", "m.m@firma.org"))).getOrThrow()
        assertEquals(listOf("max@beispiel.de", "m.m@firma.org"), saved.profile.emails)

        assertIs<MemberProfileException.InvalidEmail>(
            useCase(member, named(emails = listOf("keine-adresse"))).exceptionOrNull(),
        )
        assertIs<MemberProfileException.TooManyEmails>(
            useCase(member, named(emails = listOf("a@b.de", "c@d.de", "e@f.de", "g@h.de"))).exceptionOrNull(),
        )
        assertEquals(1, repository.updatedOwnProfiles.size)
    }

    @Test
    fun `own profile keeps valid phones and rejects invalid ones`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateOwnProfileUseCase(repository)
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation)

        val saved = useCase(member, named(mobilePhone = " 0170 123456 ", landlinePhone = "+49 (30) 1234-56"))
            .getOrThrow()
        assertEquals("0170 123456", saved.profile.mobilePhone)
        assertEquals("+49 (30) 1234-56", saved.profile.landlinePhone)

        assertIs<MemberProfileException.InvalidPhone>(
            useCase(member, named(mobilePhone = "0170-abc")).exceptionOrNull(),
        )
        assertEquals(1, repository.updatedOwnProfiles.size)
    }

    @Test
    fun `own name needs a congregation and both names within the limit`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = UpdateOwnProfileUseCase(repository)
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation)

        assertIs<IllegalStateException>(useCase(member.copy(congregation = null), named()).exceptionOrNull())
        assertIs<MemberProfileException.MissingName>(useCase(member, named(lastName = " ")).exceptionOrNull())
        assertIs<MemberProfileException.MissingName>(useCase(member, named(firstName = "")).exceptionOrNull())
        val tooLong = "x".repeat(MemberProfile.MAX_NAME_LENGTH + 1)
        assertIs<MemberProfileException.NameTooLong>(useCase(member, named(lastName = tooLong)).exceptionOrNull())
        assertTrue(repository.updatedOwnProfiles.isEmpty())
    }

    @Test
    fun `own profile is trimmed and keeps the fields of the admin`() = runBlocking {
        val repository = FakeUserRepository()
        val profile = MemberProfile(category = MemberCategory.BROTHER, mobilePhone = "0170", isSpeaker = true)
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation, profile = profile)
        // The form must not be able to change the fields of the admin.
        val form = profile.copy(lastName = " Muster ", firstName = " Max ", category = MemberCategory.SISTER)

        val saved = UpdateOwnProfileUseCase(repository)(member, form.copy(isSpeaker = false)).getOrThrow()

        assertEquals(profile.copy(lastName = "Muster", firstName = "Max"), saved.profile)
        assertEquals(listOf(saved), repository.updatedOwnProfiles)
    }

    @Test
    fun `address and contacts are trimmed and empty contacts dropped`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation)
        val form = named(
            street = " Hauptstr. 1 ",
            postalCode = " 10115",
            city = "Berlin ",
            emergencyContacts = listOf(
                EmergencyContact(" Anna Muster ", " Ehefrau ", listOf("01701234567", "", " 0201123456 ")),
                EmergencyContact(),
            ),
        )

        val saved = UpdateOwnProfileUseCase(repository)(member, form).getOrThrow().profile

        assertEquals("Hauptstr. 1, 10115, Berlin", listOf(saved.street, saved.postalCode, saved.city).joinToString())
        val contact = saved.emergencyContacts.single()
        assertEquals(EmergencyContact("Anna Muster", "Ehefrau", listOf("0170 1234567", "0201 123456")), contact)
        assertEquals("Essen", contact.areaName)
    }

    @Test
    fun `invalid incomplete or too many contacts and long addresses are rejected`(): Unit = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(uid = "1", approved = true, congregation = testCongregation)
        val useCase = UpdateOwnProfileUseCase(repository)
        val contact = EmergencyContact("Anna", "", listOf("0170"))
        suspend fun errorFor(form: MemberProfile) = useCase(member, form).exceptionOrNull()

        assertIs<MemberProfileException.IncompleteContact>(
            errorFor(named(emergencyContacts = listOf(EmergencyContact("Anna")))),
        )
        assertIs<MemberProfileException.InvalidContact>(
            errorFor(named(emergencyContacts = listOf(contact.copy(phones = listOf("abc"))))),
        )
        assertIs<MemberProfileException.TooManyPhones>(
            errorFor(
                named(emergencyContacts = listOf(contact.copy(phones = listOf("0170", "030 1", "0201 2", "089 3")))),
            ),
        )
        assertIs<MemberProfileException.TooManyContacts>(errorFor(named(emergencyContacts = List(6) { contact })))
        assertIs<MemberProfileException.AddressTooLong>(errorFor(named(postalCode = "12345678901")))
        assertTrue(repository.updatedOwnProfiles.isEmpty())

        useCase(member, named(emergencyContacts = List(MemberProfile.MAX_EMERGENCY_CONTACTS) { contact })).getOrThrow()
    }

    @Test
    fun `the admin saves address and contacts of a member`() = runBlocking {
        val repository = FakeUserRepository()
        val member = AppUser(
            uid = "1",
            profile = MemberProfile(
                city = " Berlin ",
                emergencyContacts = listOf(EmergencyContact("Anna", "", listOf("0170"))),
            ),
        )

        val saved = UpdateMemberProfileUseCase(repository, policy)(admin, member).getOrThrow()

        assertEquals("Berlin", saved.profile.city)
        assertEquals(listOf(saved), repository.updatedProfiles)
    }

    private fun named(
        lastName: String = "Muster",
        firstName: String = "Max",
        mobilePhone: String = "",
        landlinePhone: String = "",
        emails: List<String> = emptyList(),
        street: String = "",
        postalCode: String = "",
        city: String = "",
        emergencyContacts: List<EmergencyContact> = emptyList(),
    ) = MemberProfile(
        lastName = lastName,
        firstName = firstName,
        mobilePhone = mobilePhone,
        landlinePhone = landlinePhone,
        emails = emails,
        street = street,
        postalCode = postalCode,
        city = city,
        emergencyContacts = emergencyContacts,
    )
    private fun profile(category: MemberCategory, baptized: Boolean = false, elder: Boolean = false) = MemberProfile(
        category = category,
        baptized = baptized,
        appointment = if (elder) Appointment.ELDER else Appointment.NONE,
    )
}
