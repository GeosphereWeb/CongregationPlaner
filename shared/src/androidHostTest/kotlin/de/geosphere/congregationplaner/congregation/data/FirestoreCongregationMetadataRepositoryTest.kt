package de.geosphere.congregationplaner.congregation.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
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
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FirestoreCongregationMetadataRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreCongregationMetadataRepository(firestore)

    init {
        every { firestore.collection(CONGREGATION_COLLECTION_PATH) } returns collection
    }

    @Test
    fun `observeAll maps snapshot documents and removes listener on cancel`() = runBlocking {
        emitSnapshot(querySnapshot(document("609a", "Berlin", "19:00", 3L, "10:00", 7L)), null)

        assertEquals(
            listOf(CongregationMetadata("609a", "Berlin", LocalTime(19, 0), 3, LocalTime(10, 0), 7)),
            repository.observeAll().first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeAll uses defaults for missing fields`() = runBlocking {
        emitSnapshot(querySnapshot(document("1", null, null, null, null, null)), null)

        assertEquals(
            CongregationMetadata("1", "", LocalTime(0, 0), 1, LocalTime(0, 0), 1),
            repository.observeAll().first().single(),
        )
    }

    @Test
    fun `observeAll fails for invalid field values`() = withMockedLog {
        listOf(
            document("1", "Berlin", "19:00", 9L, "10:00", 7L),
            document("1", "Berlin", "19:00", 2.5, "10:00", 7L),
            document("1", "Berlin", "19:00", "Mo", "10:00", 7L),
            document("1", 42L, "19:00", 3L, "10:00", 7L),
            document("1", "Berlin", "25:99", 3L, "10:00", 7L),
            document("1", "Berlin", 1900L, 3L, "10:00", 7L),
        ).forEach { invalid ->
            emitSnapshot(querySnapshot(invalid), null)
            runBlocking { assertFailsWith<IllegalArgumentException> { repository.observeAll().first() } }
        }
    }

    @Test
    fun `observeAll fails for listener errors and empty snapshots`(): Unit = withMockedLog {
        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking { assertFailsWith<FirebaseFirestoreException> { repository.observeAll().first() } }

        emitSnapshot(null, null)
        runBlocking { assertFailsWith<IllegalStateException> { repository.observeAll().first() } }
    }

    @Test
    fun `save creates new document for congregation without id`() = runBlocking {
        val reference = mockk<DocumentReference>()
        val fields = slot<Any>()
        every { collection.document() } returns reference
        every { reference.id } returns "new-id"
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        val congregation = CongregationMetadata(name = "Berlin")

        assertEquals(congregation.copy(id = "new-id"), repository.save(congregation).getOrThrow())
        assertEquals(
            mapOf(
                "name" to "Berlin",
                "midweek_meeting_time" to "19:00",
                "midweek_meeting_weekday" to 3,
                "weekend_meeting_time" to "17:30",
                "weekend_meeting_weekday" to 7,
            ),
            fields.captured,
        )
    }

    @Test
    fun `save updates existing document`() = runBlocking {
        val reference = mockk<DocumentReference>()
        every { collection.document("609a") } returns reference
        every { reference.id } returns "609a"
        every { reference.update(any<Map<String, Any>>()) } returns successTask<Void?>(null)
        val congregation = CongregationMetadata(id = "609a", name = "Berlin")

        assertEquals(congregation, repository.save(congregation).getOrThrow())
        verify { reference.update(any<Map<String, Any>>()) }
    }

    @Test
    fun `save returns failure when firestore rejects the write`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val error = IllegalStateException("permission denied")
        every { collection.document() } returns reference
        every { reference.set(any()) } returns failedTask<Void?>(error)

        runBlocking { assertFailure(error, repository.save(CongregationMetadata(name = "Berlin"))) }
    }

    @Test
    fun `delete removes document and validates id`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        every { collection.document("609a") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        runBlocking {
            assertTrue(repository.delete("609a").isSuccess)
            assertIs<IllegalArgumentException>(repository.delete("").exceptionOrNull())
        }
        verify { reference.delete() }
    }

    @Test
    fun `delete returns failure when firestore rejects the delete`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val error = IllegalStateException("permission denied")
        every { collection.document("609a") } returns reference
        every { reference.delete() } returns failedTask<Void?>(error)

        runBlocking { assertFailure(error, repository.delete("609a")) }
    }

    @Test
    fun `android factory creates firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreCongregationMetadataRepository>(createCongregationMetadataRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
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

    @Suppress("LongParameterList")
    private fun document(
        id: String,
        name: Any?,
        midweekTime: Any?,
        midweekWeekday: Any?,
        weekendTime: Any?,
        weekendWeekday: Any?,
    ): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.id } returns id
        every { snapshot.get("name") } returns name
        every { snapshot.get("midweek_meeting_time") } returns midweekTime
        every { snapshot.get("midweek_meeting_weekday") } returns midweekWeekday
        every { snapshot.get("weekend_meeting_time") } returns weekendTime
        every { snapshot.get("weekend_meeting_weekday") } returns weekendWeekday
        return snapshot
    }
}
