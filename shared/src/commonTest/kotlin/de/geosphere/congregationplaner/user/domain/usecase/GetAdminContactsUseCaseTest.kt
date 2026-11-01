package de.geosphere.congregationplaner.user.domain.usecase

import de.geosphere.congregationplaner.user.domain.model.AdminContact
import de.geosphere.congregationplaner.user.testing.FakeAdminContactRepository
import de.geosphere.congregationplaner.user.testing.testCongregation
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetAdminContactsUseCaseTest {
    private val repository = FakeAdminContactRepository()
    private val useCase = GetAdminContactsUseCase(repository)

    @Test
    fun `contacts are sorted by name`() = runBlocking {
        repository.contacts = listOf(AdminContact("2", "Bernd", "Beispiel"), AdminContact("1", "anna", "Admin"))

        assertEquals(listOf("1", "2"), useCase(testCongregation).getOrThrow().map { it.uid })
    }

    @Test
    fun `no contacts is an empty list`() = runBlocking {
        assertEquals(emptyList(), useCase(testCongregation).getOrThrow())
    }

    @Test
    fun `failure is passed on`() = runBlocking {
        repository.getResult = Result.failure(IllegalStateException("denied"))

        assertTrue(useCase(testCongregation).isFailure)
    }
}
