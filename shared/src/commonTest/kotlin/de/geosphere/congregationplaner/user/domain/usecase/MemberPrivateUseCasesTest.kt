package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateException
import de.geosphere.congregationplaner.user.domain.permission.UserPermissionPolicy
import de.geosphere.congregationplaner.user.testing.FakeMemberPrivateRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberPrivateUseCasesTest {
    private val member = AppUser(uid = "m1", approved = true, congregation = testCongregation)
    private val admin = AppUser(
        uid = "a1",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        congregation = testCongregation,
    )
    private val planner = AppUser(
        uid = "p1",
        approved = true,
        roles = setOf(CongregationRole.TALK_PLANNER),
        congregation = testCongregation,
    )
    private val stored = MemberPrivateDetails(
        street = "Hauptstr. 1",
        postalCode = "10115",
        city = "Berlin",
        emergencyContacts = listOf(EmergencyContact("Anna Muster", "Ehefrau", listOf("0170 1"))),
    )

    @Test
    fun `own details are empty until something was saved`() = runBlocking {
        val repository = FakeMemberPrivateRepository()

        assertEquals(MemberPrivateDetails(), ObserveOwnPrivateDetailsUseCase(repository)(member).first())
        assertEquals(testCongregation, repository.requestedCongregation)
        assertTrue(ObserveOwnPrivateDetailsUseCase(repository)(member.copy(congregation = null)).toList().isEmpty())
    }

    @Test
    fun `saving trims the input and drops empty contacts`() = runBlocking {
        val repository = FakeMemberPrivateRepository()
        val input = MemberPrivateDetails(
            street = " Hauptstr. 1 ",
            postalCode = " 10115",
            city = "Berlin ",
            emergencyContacts = listOf(
                EmergencyContact(" Anna Muster ", " Ehefrau ", listOf(" 0170 1 ")),
                EmergencyContact(),
            ),
        )

        val saved = SaveOwnPrivateDetailsUseCase(repository)(member, input).getOrThrow()

        assertEquals(stored, saved)
        assertEquals(stored, repository.details.value["m1"])
        assertEquals(listOf("m1"), repository.savedUids)
    }

    @Test
    fun `saving rejects incomplete invalid or too many contacts and long addresses`() = runBlocking {
        val repository = FakeMemberPrivateRepository()
        val save = SaveOwnPrivateDetailsUseCase(repository)
        val contact = EmergencyContact("Anna", "", listOf("0170"))

        assertIs<MemberPrivateException.IncompleteContact>(
            save(member, MemberPrivateDetails(emergencyContacts = listOf(EmergencyContact("Anna")))).exceptionOrNull(),
        )
        assertIs<MemberPrivateException.InvalidContact>(
            save(
                member,
                MemberPrivateDetails(emergencyContacts = listOf(contact.copy(phones = listOf("abc")))),
            ).exceptionOrNull(),
        )
        assertIs<MemberPrivateException.TooManyContacts>(
            save(member, MemberPrivateDetails(emergencyContacts = List(6) { contact })).exceptionOrNull(),
        )
        assertIs<MemberPrivateException.AddressTooLong>(
            save(member, MemberPrivateDetails(postalCode = "12345678901")).exceptionOrNull(),
        )
        assertIs<IllegalStateException>(save(member.copy(congregation = null), stored).exceptionOrNull())
        assertTrue(repository.savedUids.isEmpty())
    }

    @Test
    fun `a contact keeps up to three formatted numbers and drops empty ones`(): Unit = runBlocking {
        val repository = FakeMemberPrivateRepository()
        val save = SaveOwnPrivateDetailsUseCase(repository)
        val input = MemberPrivateDetails(
            emergencyContacts = listOf(EmergencyContact("Anna", "", listOf("01701234567", "", " 0201123456 "))),
        )

        val saved = save(member, input).getOrThrow()

        assertEquals(listOf("0170 1234567", "0201 123456"), saved.emergencyContacts.single().phones)
        assertEquals("Essen", saved.emergencyContacts.single().areaName)
        val four = EmergencyContact("Anna", "", listOf("0170", "030 1", "0201 2", "089 3"))
        assertIs<MemberPrivateException.TooManyPhones>(
            save(member, MemberPrivateDetails(emergencyContacts = listOf(four))).exceptionOrNull(),
        )
    }

    @Test
    fun `five contacts are allowed`() = runBlocking {
        val repository = FakeMemberPrivateRepository()
        val five = MemberPrivateDetails(emergencyContacts = List(5) { EmergencyContact("K$it", "", listOf("0$it")) })

        assertEquals(five, SaveOwnPrivateDetailsUseCase(repository)(member, five).getOrThrow())
    }

    @Test
    fun `only a congregation admin sees the details of another member`() = runBlocking {
        val repository = FakeMemberPrivateRepository(mapOf("m1" to stored))
        val observe = ObserveMemberPrivateDetailsUseCase(repository, UserPermissionPolicy())

        assertEquals(stored, observe(admin, member).first())
        assertTrue(observe(planner, member).toList().isEmpty())
        assertTrue(observe(admin, member.copy(hasAccount = false)).toList().isEmpty())
        assertNull(observe(admin, member.copy(uid = "other")).first())
    }
}
