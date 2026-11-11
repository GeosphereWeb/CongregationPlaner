package de.geosphere.congregationplaner.congregation.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.WriteBatch
import de.geosphere.congregationplaner.congregation.domain.model.CongregationMetadata
import de.geosphere.congregationplaner.congregation.domain.model.MeetingTimes
import de.geosphere.congregationplaner.congregation.domain.model.NewCongregationAdmin
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreCongregationMetadataRepositoryTest {
    private val circuit = listOf("EMEA", "europe", "central-europe", "de", "c1")
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val names = mockk<CollectionReference>()
    private val batch = mockk<WriteBatch>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreCongregationMetadataRepository(firestore)

    init {
        every { firestore.collection(congregationCollectionPath(circuit)) } returns collection
        every { firestore.collection(uniqueCongregationNamesCollectionPath(circuit)) } returns names
        every { firestore.batch() } returns batch
    }

    @Test
    fun `observeAll maps snapshot documents and removes listener on cancel`() = runBlocking {
        emitSnapshot(querySnapshot(document("609a", "Berlin", "19:00", 3L, "10:00", 7L)), null)

        assertEquals(
            listOf(CongregationMetadata("609a", "Berlin", LocalTime(19, 0), 3, LocalTime(10, 0), 7)),
            repository.observeAll(circuit).first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeAll uses defaults for missing fields`() = runBlocking {
        emitSnapshot(querySnapshot(document("1", null, null, null, null, null)), null)

        assertEquals(
            CongregationMetadata("1", "", LocalTime(0, 0), 1, LocalTime(0, 0), 1),
            repository.observeAll(circuit).first().single(),
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
            runBlocking { assertFailsWith<IllegalArgumentException> { repository.observeAll(circuit).first() } }
        }
    }

    @Test
    fun `observeAll fails for listener errors and empty snapshots`(): Unit = withMockedLog {
        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking { assertFailsWith<FirebaseFirestoreException> { repository.observeAll(circuit).first() } }

        emitSnapshot(null, null)
        runBlocking { assertFailsWith<IllegalStateException> { repository.observeAll(circuit).first() } }
    }

    @Test
    fun `observeAll maps yearly meeting times and skips malformed entries`() = runBlocking {
        val yearly = listOf("2027|4|18:30|6|09:30", "garbage")
        emitSnapshot(querySnapshot(document("1", "Berlin", "19:00", 3L, "10:00", 7L, yearly)), null)

        assertEquals(
            mapOf(2027 to MeetingTimes(4, LocalTime(18, 30), 6, LocalTime(9, 30))),
            repository.observeAll(circuit).first().single().yearlyMeetingTimes,
        )
    }

    @Test
    fun `create writes the congregation name document and first admin in one batch`() = runBlocking {
        val reference = mockk<DocumentReference>()
        val nameReference = mockk<DocumentReference>()
        val memberReference = mockk<DocumentReference>()
        val memberCollection = mockk<CollectionReference>()
        val fields = slot<Any>()
        val nameFields = slot<Any>()
        val memberFields = slot<Any>()
        every { collection.document() } returns reference
        every { reference.id } returns "new-id"
        every { reference.collection("members") } returns memberCollection
        every { memberCollection.document("uid-1") } returns memberReference
        every { names.document("grafing süd") } returns nameReference
        every { batch.set(reference, capture(fields)) } returns batch
        every { batch.set(nameReference, capture(nameFields)) } returns batch
        every { batch.set(memberReference, capture(memberFields)) } returns batch
        every { batch.commit() } returns successTask<Void?>(null)
        val admin = NewCongregationAdmin("uid-1", "max@example.com", "Max Muster", "Max", "Muster", true)

        val created = repository.create(circuit, CongregationMetadata(name = "Grafing Süd"), "uid-1", admin)
            .getOrThrow()

        assertEquals("new-id", created.id)
        assertEquals(
            mapOf(
                "name" to "Grafing Süd",
                "midweek_meeting_time" to "19:00",
                "midweek_meeting_weekday" to 3,
                "weekend_meeting_time" to "17:30",
                "weekend_meeting_weekday" to 7,
                "yearly_meeting_times" to emptyList<String>(),
                "createdBy" to "uid-1",
            ),
            fields.captured,
        )
        assertEquals(mapOf("congregationId" to "new-id"), nameFields.captured)
        val member = memberFields.captured as Map<*, *>
        assertEquals(true, member["approved"])
        assertEquals(listOf("CONGREGATION_ADMIN"), member["roles"])
        assertEquals("Max", member["firstName"])
        assertEquals("Muster", member["lastName"])
        verify(exactly = 1) { batch.commit() }
    }

    @Test
    fun `create without a first admin writes no member and reports failures`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val nameReference = mockk<DocumentReference>()
        every { collection.document() } returns reference
        every { reference.id } returns "new-id"
        every { names.document("nord") } returns nameReference
        every { batch.set(any<DocumentReference>(), any<Map<String, Any?>>()) } returns batch
        every { batch.commit() } returns successTask<Void?>(null)

        runBlocking {
            repository.create(circuit, CongregationMetadata(name = "Nord"), "global", null).getOrThrow()
        }
        verify(exactly = 2) { batch.set(any<DocumentReference>(), any<Map<String, Any?>>()) }

        val error = IllegalStateException("permission denied")
        every { batch.commit() } returns failedTask<Void?>(error)
        runBlocking {
            assertFailure(error, repository.create(circuit, CongregationMetadata(name = "Nord"), "global", null))
        }
    }

    @Test
    fun `findNameOwner reads the name document of the circuit`() = runBlocking {
        val nameReference = mockk<DocumentReference>()
        every { names.document("grafing süd") } returns nameReference
        every { nameReference.get(Source.SERVER) } returns successTask(
            mockk<DocumentSnapshot> {
                every { exists() } returns true
                every { getString("congregationId") } returns "k1"
            },
        )

        assertEquals("k1", repository.findNameOwner(circuit, "  Grafing   SÜD ").getOrThrow())

        every { nameReference.get(Source.SERVER) } returns successTask(
            mockk<DocumentSnapshot> { every { exists() } returns false },
        )
        assertNull(repository.findNameOwner(circuit, "Grafing Süd").getOrThrow())
    }

    @Test
    fun `save keeps the name document when the name stays`() = runBlocking {
        val reference = mockk<DocumentReference>()
        every { collection.document("609a") } returns reference
        every { reference.get(Source.SERVER) } returns currentName("Berlin")
        every { reference.update(any<Map<String, Any>>()) } returns successTask<Void?>(null)
        val congregation = CongregationMetadata(id = "609a", name = "BERLIN")

        assertEquals(congregation, repository.save(circuit, congregation).getOrThrow())
        verify { reference.update(any<Map<String, Any>>()) }
        verify(exactly = 0) { firestore.batch() }
    }

    @Test
    fun `save moves the name document along with a rename`() = runBlocking {
        val reference = mockk<DocumentReference>()
        val oldName = mockk<DocumentReference>()
        val newName = mockk<DocumentReference>()
        every { collection.document("609a") } returns reference
        every { reference.get(Source.SERVER) } returns currentName("Berlin")
        every { names.document("berlin") } returns oldName
        every { names.document("berlin-mitte") } returns newName
        every { batch.update(reference, any<Map<String, Any>>()) } returns batch
        every { batch.delete(oldName) } returns batch
        every { batch.set(newName, mapOf("congregationId" to "609a")) } returns batch
        every { batch.commit() } returns successTask<Void?>(null)

        repository.save(circuit, CongregationMetadata(id = "609a", name = "Berlin-Mitte")).getOrThrow()

        verify { batch.update(reference, any<Map<String, Any>>()) }
        verify { batch.delete(oldName) }
        verify { batch.set(newName, mapOf("congregationId" to "609a")) }
        verify(exactly = 1) { batch.commit() }
    }

    @Test
    fun `save needs an id and returns failures`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val error = IllegalStateException("permission denied")
        every { collection.document("609a") } returns reference
        every { reference.get(Source.SERVER) } returns currentName("Berlin")
        every { reference.update(any<Map<String, Any>>()) } returns failedTask<Void?>(error)

        runBlocking {
            assertIs<IllegalArgumentException>(
                repository.save(circuit, CongregationMetadata(name = "Berlin")).exceptionOrNull(),
            )
            assertFailure(error, repository.save(circuit, CongregationMetadata(id = "609a", name = "Berlin")))
        }
    }

    @Test
    fun `delete removes the congregation together with its name document`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val nameReference = mockk<DocumentReference>()
        every { collection.document("609a") } returns reference
        every { reference.get(Source.SERVER) } returns currentName("Grafing Süd")
        every { names.document("grafing süd") } returns nameReference
        every { batch.delete(any<DocumentReference>()) } returns batch
        every { batch.commit() } returns successTask<Void?>(null)

        runBlocking {
            assertTrue(repository.delete(circuit, "609a").isSuccess)
            assertIs<IllegalArgumentException>(repository.delete(circuit, "").exceptionOrNull())
        }
        verify { batch.delete(reference) }
        verify { batch.delete(nameReference) }
    }

    @Test
    fun `delete returns failure when firestore rejects the batch`() = withMockedLog {
        val reference = mockk<DocumentReference>()
        val error = IllegalStateException("permission denied")
        every { collection.document("609a") } returns reference
        every { reference.get(Source.SERVER) } returns currentName("Grafing Süd")
        every { names.document(any()) } returns mockk()
        every { batch.delete(any<DocumentReference>()) } returns batch
        every { batch.commit() } returns failedTask<Void?>(error)

        runBlocking { assertFailure(error, repository.delete(circuit, "609a")) }
    }

    private fun currentName(name: String) = successTask(
        mockk<DocumentSnapshot> { every { getString("name") } returns name },
    )

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
        yearlyMeetingTimes: Any? = null,
    ): DocumentSnapshot {
        val snapshot = mockk<DocumentSnapshot>()
        every { snapshot.id } returns id
        every { snapshot.get("name") } returns name
        every { snapshot.get("midweek_meeting_time") } returns midweekTime
        every { snapshot.get("midweek_meeting_weekday") } returns midweekWeekday
        every { snapshot.get("weekend_meeting_time") } returns weekendTime
        every { snapshot.get("weekend_meeting_weekday") } returns weekendWeekday
        every { snapshot.get("yearly_meeting_times") } returns yearlyMeetingTimes
        return snapshot
    }
}
