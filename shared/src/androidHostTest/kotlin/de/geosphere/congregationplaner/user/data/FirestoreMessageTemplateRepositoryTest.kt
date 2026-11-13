package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.MessageTemplate
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FirestoreMessageTemplateRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val document = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration> { every { remove() } just runs }
    private val repository = FirestoreMessageTemplateRepository(firestore)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    init {
        every { firestore.collection("${congregation.membersPath}/u1/settings") } returns collection
        every { collection.document("messageTemplates") } returns document
    }

    @Test
    fun `observeTemplates maps the list and skips broken entries`() = runBlocking {
        val snapshot = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { this@mockk.get("templates") } returns listOf(
                mapOf("name" to "Kurz", "text" to "Hallo {Vorname}"),
                mapOf("name" to "Ohne Text"),
                "kein Eintrag",
            )
        }
        emitSnapshot(snapshot, null)

        assertEquals(
            listOf(MessageTemplate("Kurz", "Hallo {Vorname}")),
            repository.observeTemplates(congregation, "u1").first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeTemplates is empty without a document and fails for listener errors`(): Unit = withMockedLog {
        emitSnapshot(mockk { every { exists() } returns false }, null)
        runBlocking { assertEquals(emptyList(), repository.observeTemplates(congregation, "u1").first()) }

        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking {
            assertFailsWith<FirebaseFirestoreException> { repository.observeTemplates(congregation, "u1").first() }
        }
    }

    @Test
    fun `saveTemplates stores the list and reports failures`(): Unit = withMockedLog {
        every { document.set(any()) } returns successTask<Void?>(null)
        runBlocking {
            repository.saveTemplates(congregation, "u1", listOf(MessageTemplate("Kurz", "Hallo"))).getOrThrow()
        }
        verify { document.set(mapOf("templates" to listOf(mapOf("name" to "Kurz", "text" to "Hallo")))) }

        val error = IllegalStateException("permission-denied")
        every { document.set(any()) } returns failedTask(error)
        runBlocking {
            assertFailure(error, repository.saveTemplates(congregation, "u1", listOf(MessageTemplate("A", "B"))))
        }
    }

    private fun emitSnapshot(snapshot: DocumentSnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<DocumentSnapshot>>()
        every { document.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, error)
            registration
        }
    }
}
