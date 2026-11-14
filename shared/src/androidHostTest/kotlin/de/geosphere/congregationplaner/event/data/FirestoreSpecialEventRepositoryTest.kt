package de.geosphere.congregationplaner.event.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.event.domain.model.EventAudience
import de.geosphere.congregationplaner.event.domain.model.SpecialEvent
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreSpecialEventRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreSpecialEventRepository(firestore)
    private val congress = SpecialEvent(
        id = "ev1",
        title = "Kongress",
        startDate = LocalDate(2027, 6, 11),
        endDate = LocalDate(2027, 6, 13),
        audience = setOf(EventAudience.SISTERS, EventAudience.BROTHERS),
    )

    init {
        every { firestore.collection(congregation.eventsPath) } returns collection
    }

    @Test
    fun `observeAll maps the events and skips those without a valid start`() = runBlocking {
        emit(
            document("ev1", "Kongress", "2027-06-11", "2027-06-13", listOf("BROTHERS", "SISTERS", "UNKNOWN", 7)),
            document("ev2", "Besuch", "2027-03-02", null, listOf("ALL")),
            document("ev3", "Kaputt", "kein Datum", "2027-03-02", listOf("ALL")),
        )

        assertEquals(
            listOf(
                congress,
                SpecialEvent("ev2", "Besuch", LocalDate(2027, 3, 2), audience = setOf(EventAudience.ALL)),
            ),
            repository.observeAll(congregation).first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeFor queries the events of the groups`() = runBlocking {
        val query = mockk<Query>()
        every { collection.whereArrayContainsAny("audience", listOf("ALL", "SISTERS")) } returns query
        val listener = slot<EventListener<QuerySnapshot>>()
        val document = document("ev1", "Kongress", "2027-06-11", "2027-06-13", listOf("BROTHERS", "SISTERS"))
        val snapshot = mockk<QuerySnapshot> { every { documents } returns listOf(document) }
        every { query.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }

        assertEquals(
            listOf(congress),
            repository.observeFor(congregation, setOf(EventAudience.SISTERS, EventAudience.ALL)).first(),
        )
    }

    @Test
    fun `save writes the fields under a new id or the existing one`() = runBlocking {
        val fields = slot<Any>()
        val created = mockk<DocumentReference> { every { id } returns "auto1" }
        val existing = mockk<DocumentReference> { every { id } returns "ev1" }
        every { collection.document() } returns created
        every { collection.document("ev1") } returns existing
        every { created.set(capture(fields)) } returns successTask<Void?>(null)
        every { existing.set(any()) } returns successTask<Void?>(null)

        assertEquals(congress.copy(id = "auto1"), repository.save(congregation, congress.copy(id = "")).getOrThrow())
        assertEquals(
            mapOf(
                "title" to "Kongress",
                "startDate" to "2027-06-11",
                "endDate" to "2027-06-13",
                "audience" to listOf("BROTHERS", "SISTERS"),
            ),
            fields.captured,
        )
        assertEquals(congress, repository.save(congregation, congress).getOrThrow())
        verify { existing.set(any()) }
    }

    @Test
    fun `delete removes the document and writes report failures`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val reference = mockk<DocumentReference> { every { id } returns "ev1" }
        every { collection.document("ev1") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        runBlocking { assertEquals(Unit, repository.delete(congregation, "ev1").getOrThrow()) }

        every { reference.delete() } returns failedTask<Void?>(error)
        every { reference.set(any()) } returns failedTask<Void?>(error)
        runBlocking {
            assertFailure(error, repository.delete(congregation, "ev1"))
            assertFailure(error, repository.save(congregation, congress))
        }
    }

    private fun emit(vararg documents: DocumentSnapshot) {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.documents } returns documents.toList() }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
    }

    private fun document(
        id: String,
        title: String,
        start: String?,
        end: String?,
        audience: List<Any>,
    ): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("title") } returns title
        every { getString("startDate") } returns start
        every { getString("endDate") } returns end
        every { this@mockk.get("audience") } returns audience
    }
}
