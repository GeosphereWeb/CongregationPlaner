package de.geosphere.congregationplaner.organization.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreOrganizationRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val repository = FirestoreOrganizationRepository(firestore)

    @Test
    fun `listUnits maps documents of the level collection`() = runBlocking {
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(unit("europe", "Europa"), unit("africa", null))
        }
        every { firestore.collection("region/EMEA/subregion") } returns mockk {
            every { get() } returns successTask(snapshot)
        }

        assertEquals(
            listOf(OrgUnit("europe", "Europa"), OrgUnit("africa")),
            repository.listUnits(OrgLevel.SUBREGION, listOf("EMEA")).getOrThrow(),
        )
    }

    @Test
    fun `listUnits returns failure when firestore fails`() = withMockedLog {
        val error = IllegalStateException("permission-denied")
        every { firestore.collection("region") } returns mockk { every { get() } returns failedTask(error) }

        runBlocking { assertFailure(error, repository.listUnits(OrgLevel.REGION, emptyList())) }
    }

    private fun unit(id: String, name: String?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.get("name") } returns name
    }
}
