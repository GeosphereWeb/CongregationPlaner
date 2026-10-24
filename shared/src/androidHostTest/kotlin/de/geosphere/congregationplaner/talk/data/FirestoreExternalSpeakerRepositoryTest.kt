package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
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

class FirestoreExternalSpeakerRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreExternalSpeakerRepository(firestore)

    init {
        every { firestore.collection(congregation.externalSpeakersPath) } returns collection
    }

    @Test
    fun `observeAll maps documents with defaults for missing fields`() = runBlocking {
        val full = mockk<DocumentSnapshot> {
            every { id } returns "e1"
            every { getString("lastName") } returns "Extern"
            every { getString("firstName") } returns "Eva"
            every { getString("congregationName") } returns "Nord"
        }
        val empty = mockk<DocumentSnapshot> {
            every { id } returns "e2"
            every { getString("lastName") } returns null
            every { getString("firstName") } returns null
            every { getString("congregationName") } returns null
        }
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { documents } returns listOf(full, empty) }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }

        val speakers = repository.observeAll(congregation).first()

        val expected = listOf(ExternalSpeaker("e1", "Extern", "Eva", "Nord"), ExternalSpeaker("e2", "", "", ""))
        assertEquals(expected, speakers)
        verify { registration.remove() }
    }

    @Test
    fun `save creates a document with a generated id for a new speaker`() = runBlocking {
        val fields = slot<Any>()
        val reference = mockk<DocumentReference> { every { id } returns "auto1" }
        every { collection.document() } returns reference
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)

        val saved = repository.save(congregation, ExternalSpeaker("", "Extern", "Eva", "Nord")).getOrThrow()

        assertEquals(ExternalSpeaker("auto1", "Extern", "Eva", "Nord"), saved)
        val expectedFields = mapOf("lastName" to "Extern", "firstName" to "Eva", "congregationName" to "Nord")
        assertEquals(expectedFields, fields.captured)
    }

    @Test
    fun `save overwrites the document of an existing speaker`() = runBlocking {
        val reference = mockk<DocumentReference> { every { id } returns "e1" }
        every { collection.document("e1") } returns reference
        every { reference.set(any()) } returns successTask<Void?>(null)
        val speaker = ExternalSpeaker("e1", "Extern", "Eva", "Nord")

        assertEquals(speaker, repository.save(congregation, speaker).getOrThrow())
        verify { reference.set(any()) }
    }

    @Test
    fun `save and delete return failure when firestore rejects the write`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val reference = mockk<DocumentReference> { every { id } returns "e1" }
        every { collection.document("e1") } returns reference
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.delete() } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.save(congregation, ExternalSpeaker("e1", "Extern", "Eva", "Nord")))
            assertFailure(error, repository.delete(congregation, "e1"))
        }
    }

    @Test
    fun `delete removes the document`() = runBlocking {
        val reference = mockk<DocumentReference>()
        every { collection.document("e1") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        assertEquals(Unit, repository.delete(congregation, "e1").getOrThrow())
        verify { reference.delete() }
    }

    @Test
    fun `android factory creates the firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreExternalSpeakerRepository>(createExternalSpeakerRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }
}
