package de.geosphere.congregationplaner.circuit.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.circuit.domain.model.Circuit
import de.geosphere.congregationplaner.circuit.domain.model.CircuitException
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
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreCircuitRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val reference = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreCircuitRepository(firestore)

    init {
        every { firestore.collection(CIRCUIT_COLLECTION_PATH) } returns collection
        every { collection.document("609a") } returns reference
    }

    @Test
    fun `observeAll maps snapshot documents and removes listener on cancel`() = runBlocking {
        emitSnapshot(querySnapshot(document("609a", "Nord", "Max"), document("610b", null, null)), null)

        assertEquals(
            listOf(Circuit("609a", "Nord", "Max"), Circuit("610b", "", "")),
            repository.observeAll().first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeAll fails for invalid fields, listener errors and empty snapshots`(): Unit = withMockedLog {
        emitSnapshot(querySnapshot(document("1", 42L, null)), null)
        runBlocking { assertFailsWith<IllegalArgumentException> { repository.observeAll().first() } }

        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking { assertFailsWith<FirebaseFirestoreException> { repository.observeAll().first() } }

        emitSnapshot(null, null)
        runBlocking { assertFailsWith<IllegalStateException> { repository.observeAll().first() } }
    }

    @Test
    fun `create writes document under the chosen id`() = runBlocking {
        val fields = slot<Any>()
        every { reference.get() } returns successTask(existingDocument(false))
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        val circuit = Circuit("609a", "Nord", "Max")

        assertEquals(circuit, repository.create(circuit).getOrThrow())
        assertEquals(mapOf("name" to "Nord", "overseer_name" to "Max"), fields.captured)
    }

    @Test
    fun `create refuses to overwrite an existing circuit`() = runBlocking {
        every { reference.get() } returns successTask(existingDocument(true))

        assertIs<CircuitException.AlreadyExists>(repository.create(Circuit("609a", "Nord")).exceptionOrNull())
        verify(exactly = 0) { reference.set(any()) }
    }

    @Test
    fun `create and update return failure when firestore rejects the write`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        every { reference.get() } returns successTask(existingDocument(false))
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.update(any<Map<String, Any>>()) } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.create(Circuit("609a", "Nord")))
            assertFailure(error, repository.update(Circuit("609a", "Nord")))
            assertIs<IllegalArgumentException>(repository.create(Circuit("", "Nord")).exceptionOrNull())
        }
    }

    @Test
    fun `update writes fields of existing document`() = runBlocking {
        every { reference.update(any<Map<String, Any>>()) } returns successTask<Void?>(null)
        val circuit = Circuit("609a", "Nord")

        assertEquals(circuit, repository.update(circuit).getOrThrow())
        verify { reference.update(mapOf<String, Any>("name" to "Nord", "overseer_name" to "")) }
    }

    @Test
    fun `delete removes circuit without congregations`() = runBlocking {
        stubCongregations(isEmpty = true)
        every { reference.delete() } returns successTask<Void?>(null)

        assertTrue(repository.delete("609a").isSuccess)
        verify { reference.delete() }
    }

    @Test
    fun `delete refuses circuits with congregations and validates id`(): Unit = withMockedLog {
        stubCongregations(isEmpty = false)

        runBlocking {
            assertIs<CircuitException.HasCongregations>(repository.delete("609a").exceptionOrNull())
            assertIs<IllegalArgumentException>(repository.delete("").exceptionOrNull())
        }
        verify(exactly = 0) { reference.delete() }
    }

    @Test
    fun `android factory creates firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreCircuitRepository>(createCircuitRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun stubCongregations(isEmpty: Boolean) {
        val congregations = mockk<CollectionReference>()
        val query = mockk<Query>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.isEmpty } returns isEmpty }
        every { reference.collection(CONGREGATION_SUBCOLLECTION) } returns congregations
        every { congregations.limit(1) } returns query
        every { query.get() } returns successTask(snapshot)
    }

    private fun emitSnapshot(snapshot: QuerySnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<QuerySnapshot>>()
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, error)
            registration
        }
    }

    private fun querySnapshot(vararg documents: DocumentSnapshot): QuerySnapshot = mockk {
        every { this@mockk.documents } returns documents.toList()
    }

    private fun existingDocument(exists: Boolean): DocumentSnapshot = mockk {
        every { this@mockk.exists() } returns exists
    }

    private fun document(id: String, name: Any?, overseerName: Any?): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.id } returns id
        every { snapshot.get("name") } returns name
        every { snapshot.get("overseer_name") } returns overseerName
        return snapshot
    }
}
