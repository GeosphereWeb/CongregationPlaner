package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.UserRole
import de.geosphere.congregationplaner.user.domain.repository.UserRepository
import de.geosphere.congregationplaner.user.testing.FakeAdminContactRepository
import de.geosphere.congregationplaner.user.testing.FakeUserRepository
import de.geosphere.congregationplaner.user.testing.testAuthUser
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdminContactSyncingUserRepositoryTest {
    private val log = mutableListOf<String>()
    private val contacts = FakeAdminContactRepository(log = log)
    private val users = FakeUserRepository()

    // Protokolliert die Aufrufe des eigentlichen Repositorys in derselben Liste wie die Kontakte.
    private val repository: UserRepository = object : UserRepository by users {
        override suspend fun updateMember(congregation: CongregationRef, member: AppUser) =
            users.updateMember(congregation, member).also { log += "update ${member.uid}" }

        override suspend fun updateMemberProfile(congregation: CongregationRef, member: AppUser) =
            users.updateMemberProfile(congregation, member).also { log += "profile ${member.uid}" }

        override suspend fun removeMember(congregation: CongregationRef, uid: String) =
            users.removeMember(congregation, uid).also { log += "remove $uid" }
    }.withAdminContacts(contacts)

    private val admin = AppUser(
        uid = "a1",
        approved = true,
        role = UserRole.CONGREGATION_ADMIN,
        profile = MemberProfile(firstName = "Anna", lastName = "Admin"),
    )
    private val member = AppUser(uid = "m1", approved = true, role = UserRole.MEMBER)

    @Test
    fun `promoting a member saves the member before publishing the contact`() = runBlocking {
        repository.updateMember(testCongregation, admin).getOrThrow()

        assertEquals(listOf("update a1", "put a1 Anna Admin"), log)
    }

    @Test
    fun `demoting an admin removes the contact before saving the member`() = runBlocking {
        repository.updateMember(testCongregation, admin.copy(role = UserRole.MEMBER)).getOrThrow()

        assertEquals(listOf("delete a1", "update a1"), log)
    }

    @Test
    fun `an admin who is not approved yet has no contact`() = runBlocking {
        repository.updateMember(testCongregation, admin.copy(approved = false)).getOrThrow()

        assertEquals(listOf("delete a1", "update a1"), log)
    }

    @Test
    fun `member is not saved when the contact cannot be removed`() = runBlocking {
        contacts.deleteResult = Result.failure(IllegalStateException("denied"))

        val result = repository.updateMember(testCongregation, member)

        assertEquals("denied", result.exceptionOrNull()?.message)
        assertEquals(listOf("delete m1"), log)
    }

    @Test
    fun `failing to publish the contact is reported after the member was saved`() = runBlocking {
        contacts.putResult = Result.failure(IllegalStateException("offline"))

        val result = repository.updateMember(testCongregation, admin)

        assertEquals("offline", result.exceptionOrNull()?.message)
        assertEquals(listOf("update a1", "put a1 Anna Admin"), log)
    }

    @Test
    fun `failing member update does not publish the contact`() = runBlocking {
        users.updateResult = Result.failure(IllegalStateException("denied"))

        val result = repository.updateMember(testCongregation, admin)

        assertTrue(result.isFailure)
        assertEquals(listOf("update a1"), log)
    }

    @Test
    fun `renaming an admin republishes the contact`() = runBlocking {
        repository.updateMemberProfile(testCongregation, admin).getOrThrow()

        assertEquals(listOf("profile a1", "put a1 Anna Admin"), log)
    }

    @Test
    fun `editing a plain member profile leaves the contacts alone`() = runBlocking {
        repository.updateMemberProfile(testCongregation, member).getOrThrow()

        assertEquals(listOf("profile m1"), log)
    }

    @Test
    fun `failing profile update does not publish the contact`() = runBlocking {
        users.updateProfileResult = Result.failure(IllegalStateException("denied"))

        assertTrue(repository.updateMemberProfile(testCongregation, admin).isFailure)
        assertEquals(listOf("profile a1"), log)
    }

    @Test
    fun `failing to republish after a rename is reported`() = runBlocking {
        contacts.putResult = Result.failure(IllegalStateException("offline"))

        assertEquals("offline", repository.updateMemberProfile(testCongregation, admin).exceptionOrNull()?.message)
    }

    @Test
    fun `removing a member removes the contact first`() = runBlocking {
        repository.removeMember(testCongregation, "a1").getOrThrow()

        assertEquals(listOf("delete a1", "remove a1"), log)
    }

    @Test
    fun `member stays when its contact cannot be removed`() = runBlocking {
        contacts.deleteResult = Result.failure(IllegalStateException("denied"))

        assertTrue(repository.removeMember(testCongregation, "a1").isFailure)
        assertEquals(listOf("delete a1"), log)
    }

    @Test
    fun `signing in as admin publishes the contact`() = runBlocking {
        users.getOrCreateResult = Result.success(admin)

        repository.getOrCreateUser(testAuthUser("a1"), testCongregation).getOrThrow()

        assertEquals(listOf("put a1 Anna Admin"), log)
    }

    @Test
    fun `signing in as admin still works when the contact cannot be published`() = runBlocking {
        users.getOrCreateResult = Result.success(admin)
        contacts.putResult = Result.failure(IllegalStateException("offline"))

        assertEquals(admin, repository.getOrCreateUser(testAuthUser("a1"), testCongregation).getOrThrow())
    }

    @Test
    fun `signing in as plain member publishes nothing`() = runBlocking {
        users.getOrCreateResult = Result.success(member)

        repository.getOrCreateUser(testAuthUser("m1"), testCongregation).getOrThrow()

        assertEquals(emptyList(), log)
    }
}
