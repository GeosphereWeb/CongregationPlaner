package de.geosphere.congregationplaner.data

import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.congregation.data.unavailableCongregationMetadataRepository
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.organization.data.unavailableOrgUnitRepository
import de.geosphere.congregationplaner.organization.data.unavailableOrganizationRepository
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.user.data.unavailableUserRepository
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
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
            repository.updateGlobalAdminProfile("uid-1", MemberProfile()).exceptionOrNull(),
        )
        assertFailsWith<UnsupportedOperationException> { repository.observeGlobalAdminProfile("uid-1").first() }
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
    fun `unavailable org unit repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableOrgUnitRepository("iOS")
        val unit = OrgUnit("r1", "EMEA")

        assertIs<UnsupportedOperationException>(repository.create(OrgLevel.REGION, emptyList(), unit).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.update(OrgLevel.REGION, emptyList(), unit).exceptionOrNull())
        assertIs<UnsupportedOperationException>(repository.delete(OrgLevel.REGION, emptyList(), unit).exceptionOrNull())
        assertFailsWith<UnsupportedOperationException> { repository.observe(OrgLevel.REGION, emptyList()).first() }
    }

    @Test
    fun `unavailable congregation repository reports the platform`(): Unit = runBlocking {
        val repository = unavailableCongregationMetadataRepository("iOS")

        assertIs<UnsupportedOperationException>(
            repository.save(congregation.circuitIds, CongregationMetadata(name = "Berlin")).exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(repository.delete(congregation.circuitIds, "609a").exceptionOrNull())
        assertIs<UnsupportedOperationException>(
            repository.findNameOwner(congregation.circuitIds, "Berlin").exceptionOrNull(),
        )
        assertIs<UnsupportedOperationException>(
            repository.create(congregation.circuitIds, CongregationMetadata(name = "Berlin"), "uid", null)
                .exceptionOrNull(),
        )
        assertFailsWith<UnsupportedOperationException> { repository.observeAll(congregation.circuitIds).first() }
    }
}
