package de.geosphere.congregationplaner.organization.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.OrgLevel
import de.geosphere.congregationplaner.organization.domain.model.OrgUnit
import de.geosphere.congregationplaner.organization.domain.model.OrgUnitException
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreOrgUnitRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val branches = mockk<CollectionReference>()
    private val circuits = mockk<CollectionReference>()
    private val reference = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreOrgUnitRepository(firestore)

    private val subregion = listOf("EMEA", "europe")
    private val branch = listOf("EMEA", "europe", "central-europe")

    init {
        every { firestore.collection("region/EMEA/subregion/europe/branch") } returns branches
        every { firestore.collection("region/EMEA/subregion/europe/branch/central-europe/circuit") } returns circuits
    }

    @Test
    fun `observe maps the documents of the level and removes the listener`() = runBlocking {
        val listener = slot<EventListener<QuerySnapshot>>()
        every { circuits.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot(document("c1", "Kreis Nord", "Max"), document("c2", null, null)), null)
            registration
        }

        assertEquals(
            listOf(OrgUnit("c1", "Kreis Nord", "Max"), OrgUnit("c2")),
            repository.observe(OrgLevel.CIRCUIT, branch).first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `create lets firestore generate the id and writes only the fields of the level`() = runBlocking {
        val fields = slot<Any>()
        every { branches.document() } returns reference
        every { reference.id } returns "generated-1"
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)

        val created = repository.create(OrgLevel.BRANCH, subregion, OrgUnit(name = "Mitte", overseerName = "x"))

        assertEquals(OrgUnit("generated-1", "Mitte", "x"), created.getOrThrow())
        assertEquals(mapOf("name" to "Mitte"), fields.captured)
    }

    @Test
    fun `update writes name and overseer of a circuit`() = runBlocking {
        every { circuits.document("c1") } returns reference
        every { reference.update(any<Map<String, Any>>()) } returns successTask<Void?>(null)
        val circuit = OrgUnit("c1", "Kreis Nord", "Max")

        assertEquals(circuit, repository.update(OrgLevel.CIRCUIT, branch, circuit).getOrThrow())
        verify { reference.update(mapOf<String, Any>("name" to "Kreis Nord", "overseer_name" to "Max")) }
    }

    @Test
    fun `writes report firestore failures and need an id for updates`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        every { branches.document() } returns reference
        every { branches.document("b1") } returns reference
        every { reference.id } returns "generated-1"
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.update(any<Map<String, Any>>()) } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.create(OrgLevel.BRANCH, subregion, OrgUnit(name = "Mitte")))
            assertFailure(error, repository.update(OrgLevel.BRANCH, subregion, OrgUnit("b1", "Mitte")))
            assertIs<IllegalArgumentException>(
                repository.update(OrgLevel.BRANCH, subregion, OrgUnit(name = "Mitte")).exceptionOrNull(),
            )
        }
    }

    @Test
    fun `delete removes an empty unit`() = runBlocking {
        stubChildren(isEmpty = true)
        every { reference.delete() } returns successTask<Void?>(null)

        assertTrue(repository.delete(OrgLevel.BRANCH, subregion, OrgUnit("b1", "Mitte")).isSuccess)
        verify { reference.delete() }
    }

    @Test
    fun `delete refuses units with children`() = runBlocking {
        stubChildren(isEmpty = false)

        val error = repository.delete(OrgLevel.BRANCH, subregion, OrgUnit("b1", "Mitte")).exceptionOrNull()

        assertIs<OrgUnitException.HasChildren>(error)
        assertTrue(error.message.orEmpty().contains("Mitte"))
        verify(exactly = 0) { reference.delete() }
    }

    @Test
    fun `android factory creates firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreOrgUnitRepository>(createOrgUnitRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun stubChildren(isEmpty: Boolean) {
        val children = mockk<CollectionReference>()
        val query = mockk<Query>()
        every { branches.document("b1") } returns reference
        every { reference.collection("circuit") } returns children
        every { children.limit(1) } returns query
        every { query.get() } returns successTask(mockk<QuerySnapshot> { every { this@mockk.isEmpty } returns isEmpty })
    }

    private fun snapshot(vararg documents: DocumentSnapshot): QuerySnapshot = mockk {
        every { this@mockk.documents } returns documents.toList()
    }

    private fun document(id: String, name: String?, overseerName: String?): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.id } returns id
        every { snapshot.get("name") } returns name
        every { snapshot.get("overseer_name") } returns overseerName
        return snapshot
    }
}
