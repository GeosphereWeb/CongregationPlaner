package de.geosphere.congregationplaner.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.circuit.data.unavailableCircuitRepository
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.congregation.data.unavailableCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.user.data.unavailableUserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class UnavailableRepositoriesTest {
    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = null
        override val displayName: String? = null
        override val idToken: String? = null
    }

    @Test
    fun `unavailable user repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableUserRepository("iOS")

        val failure = repository.getOrCreateUser(authUser).exceptionOrNull()
        assertIs<UnsupportedOperationException>(failure)
        assertEquals("Firestore is not configured for iOS.", failure.message)
        assertFailsWith<UnsupportedOperationException> { repository.observeUser("uid-1").first() }
    }

    @Test
    fun `unavailable congregation repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableCongregationMetadataRepository("iOS")

        assertIs<UnsupportedOperationException>(
            repository.save(CongregationMetadata(name = "Berlin")).exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(repository.delete("609a").exceptionOrNull())
        assertFailsWith<UnsupportedOperationException> { repository.observeAll().first() }
    }

    @Test
    fun `unavailable circuit repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableCircuitRepository("iOS")

        assertIs<UnsupportedOperationException>(repository.create(Circuit(id = "609a")).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.update(Circuit(id = "609a")).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete("609a").exceptionOrNull())
        assertFailsWith<UnsupportedOperationException> { repository.observeAll().first() }
    }
}
