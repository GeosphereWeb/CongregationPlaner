package de.geosphere.congregationplaner.event.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.event.domain.model.EventType
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
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

class FirestoreEventTypeRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreEventTypeRepository(firestore)
    private val congress = EventType("kk", "Kreiskongress", cancelsTalk = true, icon = "ic_cp_arena_duo")

    init {
        every { firestore.collection(congregation.eventTypesPath) } returns collection
    }

    @Test
    fun `observe maps the documents with defaults for missing fields`() = runBlocking {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(
                document("kk", "Kreiskongress", true, "ic_cp_arena_duo"),
                document("x", null, null, null),
            )
        }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }

        assertEquals(listOf(congress, EventType("x")), repository.observeEventTypes(congregation).first())
        verify { registration.remove() }
    }

    @Test
    fun `save creates a document with a generated id or overwrites an existing one`() = runBlocking {
        val fields = slot<Any>()
        val created = mockk<DocumentReference> { every { id } returns "auto1" }
        val existing = mockk<DocumentReference> { every { id } returns "kk" }
        every { collection.document() } returns created
        every { collection.document("kk") } returns existing
        every { created.set(capture(fields)) } returns successTask<Void?>(null)
        every { existing.set(any()) } returns successTask<Void?>(null)

        assertEquals(congress.copy(id = "auto1"), repository.save(congregation, congress.copy(id = "")).getOrThrow())
        assertEquals(
            mapOf("name" to "Kreiskongress", "cancelsTalk" to true, "icon" to "ic_cp_arena_duo"),
            fields.captured,
        )
        assertEquals(congress, repository.save(congregation, congress).getOrThrow())
        verify { existing.set(any()) }
    }

    @Test
    fun `delete removes the document and failures are reported`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val reference = mockk<DocumentReference> { every { id } returns "kk" }
        every { collection.document("kk") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        runBlocking { assertEquals(Unit, repository.delete(congregation, "kk").getOrThrow()) }

        every { reference.delete() } returns failedTask<Void?>(error)
        every { reference.set(any()) } returns failedTask<Void?>(error)
        runBlocking {
            assertFailure(error, repository.delete(congregation, "kk"))
            assertFailure(error, repository.save(congregation, congress))
        }
    }

    @Test
    fun `the android factory creates the firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreEventTypeRepository>(createEventTypeRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun document(id: String, name: String?, cancelsTalk: Boolean?, icon: String?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("name") } returns name
        every { getBoolean("cancelsTalk") } returns cancelsTalk
        every { getString("icon") } returns icon
    }
}
