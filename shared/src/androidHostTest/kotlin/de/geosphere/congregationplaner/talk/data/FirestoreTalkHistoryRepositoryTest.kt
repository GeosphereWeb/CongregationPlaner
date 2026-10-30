package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkHistoryEntry
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirestoreTalkHistoryRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreTalkHistoryRepository(firestore)

    init {
        every { firestore.collection(congregation.talkHistoryPath) } returns collection
    }

    private fun document(id: String, heldOn: Any?, plannedOn: Any? = null, speakers: Any? = null) =
        mockk<DocumentSnapshot> {
            every { this@mockk.id } returns id
            every { this@mockk.get("heldOn") } returns heldOn
            every { this@mockk.get("plannedOn") } returns plannedOn
            every { this@mockk.get("speakers") } returns speakers
        }

    @Test
    fun `observeAll maps documents and skips invalid ones`(): Unit = withMockedLog {
        val documents = listOf(
            document(
                "7",
                listOf("2026-10-25", "2026-03-01"),
                listOf("2026-10-25"),
                mapOf("2026-10-25" to "Muster, Max", "not a date" to "x", "2026-03-01" to 5),
            ),
            document("6", listOf("2026-03-01"), "not a list"),
            document("x", listOf("2026-03-01")),
            document("8", listOf("not a date")),
            document("9", emptyList<String>()),
            document("10", null),
        )
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.documents } returns documents }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }

        val entries = runBlocking { repository.observeAll(congregation).first() }

        val march = LocalDate(2026, 3, 1)
        val october = LocalDate(2026, 10, 25)
        val expected = TalkHistoryEntry(7, listOf(march, october), listOf(october), mapOf(october to "Muster, Max"))
        assertEquals(listOf(expected), entries)
    }

    @Test
    fun `save writes the dates as iso strings under the talk number`() = runBlocking {
        val fields = slot<Any>()
        val reference = mockk<DocumentReference>()
        every { collection.document("7") } returns reference
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        val march = LocalDate(2026, 3, 1)
        val october = LocalDate(2026, 10, 25)
        val entry = TalkHistoryEntry(7, listOf(march, october), listOf(october), mapOf(october to "Muster, Max"))

        assertEquals(entry, repository.save(congregation, entry).getOrThrow())
        val expected = mapOf(
            "heldOn" to listOf("2026-03-01", "2026-10-25"),
            "plannedOn" to listOf("2026-10-25"),
            "speakers" to mapOf("2026-10-25" to "Muster, Max"),
        )
        assertEquals(expected, fields.captured)
    }

    @Test
    fun `delete removes the document of the talk`() = runBlocking {
        val reference = mockk<DocumentReference>()
        every { collection.document("7") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        assertEquals(Unit, repository.delete(congregation, 7).getOrThrow())
    }

    @Test
    fun `save and delete return failure when firestore rejects the write`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val reference = mockk<DocumentReference>()
        every { collection.document("7") } returns reference
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.delete() } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.save(congregation, TalkHistoryEntry(7, listOf(LocalDate(2026, 3, 1)))))
            assertFailure(error, repository.delete(congregation, 7))
        }
    }

    @Test
    fun `android factory creates the firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreTalkHistoryRepository>(createTalkHistoryRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }
}
