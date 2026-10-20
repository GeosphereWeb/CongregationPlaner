package de.geosphere.congregationplaner.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.circuit.data.unavailableCircuitRepository
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.congregation.data.unavailableCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.data.unavailableOrganizationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.user.data.unavailableUserRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
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
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")

    @Test
    fun `unavailable user repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableUserRepository("iOS")

        val failure = repository.getOrCreateUser(authUser, congregation).exceptionOrNull()
        assertIs<UnsupportedOperationException>(failure)
        assertEquals("Firestore is not configured for iOS.", failure.message)
        assertIs<UnsupportedOperationException>(repository.isGlobalAdmin("uid-1").exceptionOrNull())
        assertIs<UnsupportedOperationException>(
            repository.updateMember(congregation, AppUser(uid = "uid-1")).exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(repository.removeMember(congregation, "uid-1").exceptionOrNull())
        assertFailsWith<UnsupportedOperationException> { repository.observeUser(congregation, "uid-1").first() }
        assertFailsWith<UnsupportedOperationException> { repository.observeMembers(congregation).first() }
    }

    @Test
    fun `unavailable organization repository reports the platform`(): Unit = runBlocking {
        val failure = unavailableOrganizationRepository("iOS").listUnits(OrgLevel.REGION, emptyList()).exceptionOrNull()

        assertIs<UnsupportedOperationException>(failure)
        assertEquals("Firestore is not configured for iOS.", failure.message)
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
