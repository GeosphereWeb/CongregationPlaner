package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AppUser
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
    fun `remove rejects blank ids and delegates otherwise`() = runBlocking {
        val repository = FakeUserRepository()
        val useCase = RemoveMemberUseCase(repository)

        assertIs<IllegalArgumentException>(useCase(testCongregation, "").exceptionOrNull())
        useCase(testCongregation, "1").getOrThrow()
        assertEquals(listOf("1"), repository.removedUids)
    }
}
