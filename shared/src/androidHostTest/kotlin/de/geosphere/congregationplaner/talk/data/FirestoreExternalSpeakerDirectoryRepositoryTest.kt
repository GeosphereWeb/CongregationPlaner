package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalCircuit
import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker
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

class FirestoreExternalSpeakerDirectoryRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val firestore = mockk<FirebaseFirestore>()
    private val circuits = mockk<CollectionReference>()
    private val congregations = mockk<CollectionReference>()
    private val speakers = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreExternalSpeakerDirectoryRepository(firestore)

    init {
        every { firestore.collection(congregation.externalCircuitsPath) } returns circuits
        every { firestore.collection(congregation.externalCongregationsPath) } returns congregations
        every { firestore.collection(congregation.visitingSpeakersPath) } returns speakers
    }

    private fun CollectionReference.emit(vararg documents: DocumentSnapshot) {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.documents } returns documents.toList() }
        every { addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
    }

    @Test
    fun `circuits and congregations are mapped with defaults for missing fields`() = runBlocking {
        circuits.emit(
            mockk {
                every { id } returns "c1"
                every { getString("name") } returns "Nord"
            },
            mockk {
                every { id } returns "c2"
                every { getString("name") } returns null
            },
        )
        congregations.emit(
            mockk {
                every { id } returns "g1"
                every { getString("circuitId") } returns "c1"
                every { getString("name") } returns "Altdorf"
            },
        )

        assertEquals(
            listOf(ExternalCircuit("c1", "Nord"), ExternalCircuit("c2", "")),
            repository.observeCircuits(congregation).first(),
        )
        assertEquals(
            listOf(ExternalCongregation("g1", "c1", "Altdorf")),
            repository.observeCongregations(congregation).first(),
        )
        verify(exactly = 2) { registration.remove() }
    }

    @Test
    fun `speakers are mapped including phone, email and talk numbers`() = runBlocking {
        speakers.emit(
            mockk {
                every { id } returns "s1"
                every { getString("congregationId") } returns "g1"
                every { getString("lastName") } returns "Berg"
                every { getString("firstName") } returns "Lea"
                every { getString("phone") } returns "0170"
                every { getString("email") } returns "lea@example.org"
                every { this@mockk.get("talkNumbers") } returns listOf(12L, 45L, "x")
            },
            mockk {
                every { id } returns "s2"
                every { getString(any()) } returns null
                every { this@mockk.get("talkNumbers") } returns null
            },
        )

        assertEquals(
            listOf(
                VisitingSpeaker("s1", "g1", "Berg", "Lea", "0170", "lea@example.org", listOf(12, 45)),
                VisitingSpeaker("s2", "", ""),
            ),
            repository.observeSpeakers(congregation).first(),
        )
    }

    @Test
    fun `save creates a document with a generated id for a new speaker`() = runBlocking {
        val fields = slot<Any>()
        val reference = mockk<DocumentReference> { every { id } returns "auto1" }
        every { speakers.document() } returns reference
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)

        val saved = repository.saveSpeaker(
            congregation,
            VisitingSpeaker(congregationId = "g1", lastName = "Neu", talkNumbers = listOf(7, 9)),
        ).getOrThrow()

        assertEquals("auto1", saved.id)
        assertEquals(
            mapOf(
                "congregationId" to "g1",
                "lastName" to "Neu",
                "firstName" to "",
                "phone" to "",
                "email" to "",
                "talkNumbers" to listOf(7, 9),
            ),
            fields.captured,
        )
    }

    @Test
    fun `save overwrites the document of an existing circuit and congregation`() = runBlocking {
        val circuitReference = mockk<DocumentReference> { every { id } returns "c1" }
        val congregationReference = mockk<DocumentReference> { every { id } returns "g1" }
        every { circuits.document("c1") } returns circuitReference
        every { congregations.document("g1") } returns congregationReference
        every { circuitReference.set(mapOf("name" to "Nord")) } returns successTask<Void?>(null)
        every {
            congregationReference.set(mapOf("circuitId" to "c1", "name" to "Altdorf"))
        } returns successTask<Void?>(null)

        val circuit = ExternalCircuit("c1", "Nord")
        val altdorf = ExternalCongregation("g1", "c1", "Altdorf")
        assertEquals(circuit, repository.saveCircuit(congregation, circuit).getOrThrow())
        assertEquals(altdorf, repository.saveCongregation(congregation, altdorf).getOrThrow())
    }

    @Test
    fun `save and delete return failure when firestore rejects the write`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val reference = mockk<DocumentReference> { every { id } returns "c1" }
        every { circuits.document("c1") } returns reference
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.delete() } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.saveCircuit(congregation, ExternalCircuit("c1", "Nord")))
            assertFailure(error, repository.deleteCircuit(congregation, "c1"))
        }
    }

    @Test
    fun `delete removes the document of each level`() = runBlocking {
        listOf(circuits, congregations, speakers).forEach { collection ->
            val reference = mockk<DocumentReference>()
            every { collection.document("x1") } returns reference
            every { reference.delete() } returns successTask<Void?>(null)
        }

        assertEquals(Unit, repository.deleteCircuit(congregation, "x1").getOrThrow())
        assertEquals(Unit, repository.deleteCongregation(congregation, "x1").getOrThrow())
        assertEquals(Unit, repository.deleteSpeaker(congregation, "x1").getOrThrow())
    }

    @Test
    fun `android factory creates the firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreExternalSpeakerDirectoryRepository>(createExternalSpeakerDirectoryRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }
}
