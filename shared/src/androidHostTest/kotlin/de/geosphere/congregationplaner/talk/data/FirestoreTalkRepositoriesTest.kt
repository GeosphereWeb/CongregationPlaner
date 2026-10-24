package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ExternalSpeaker
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FirestoreTalkRepositoriesTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val reference = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val slot = TalkSlot(
        LocalDate(2026, 10, 25),
        LocalTime(17, 30),
        "kongress",
        "Kongress",
        true,
        12,
        "Der Weg zum Glück",
        "m1",
        "Muster",
        "Max",
        "c1",
        "Alt",
        "Anton",
    )

    init {
        every { firestore.collection(congregation.talkSchedulePath) } returns collection
        every { firestore.collection(WEEK_EVENTS_COLLECTION_PATH) } returns collection
        every { firestore.collection(TALK_CATALOG_COLLECTION_PATH) } returns collection
        every { collection.document("2026-10-25") } returns reference
    }

    @Test
    fun `observeYear queries the id range of the year and maps slots`() = runBlocking {
        val lower = slot<Any>()
        val upper = slot<Any>()
        val query = mockk<Query>()
        val ranged = mockk<Query>()
        every { collection.whereGreaterThanOrEqualTo(any<FieldPath>(), capture(lower)) } returns query
        every { query.whereLessThanOrEqualTo(any<FieldPath>(), capture(upper)) } returns ranged
        emitSnapshot(ranged, querySnapshot(slotDocument("2026-10-25"), plainDocument("2026-11-01")), null)

        val slots = FirestoreTalkScheduleRepository(firestore).observeYear(congregation, 2026).first()

        assertEquals(
            listOf(slot, TalkSlot(LocalDate(2026, 11, 1), LocalTime(10, 0))),
            slots,
        )
        assertEquals("2026-01-01", lower.captured)
        assertEquals("2026-12-31", upper.captured)
        verify { registration.remove() }
    }

    @Test
    fun `observeYear skips invalid documents and fails on listener errors`(): Unit = withMockedLog {
        val query = mockk<Query>()
        val ranged = mockk<Query>()
        every { collection.whereGreaterThanOrEqualTo(any<FieldPath>(), any()) } returns query
        every { query.whereLessThanOrEqualTo(any<FieldPath>(), any()) } returns ranged
        val invalidId = mockk<DocumentSnapshot> { every { id } returns "2026-02-31" }
        val missingTime = mockk<DocumentSnapshot> {
            every { id } returns "2026-11-01"
            every { getString("time") } returns null
        }
        emitSnapshot(ranged, querySnapshot(invalidId, missingTime, slotDocument("2026-10-25")), null)
        val repository = FirestoreTalkScheduleRepository(firestore)

        runBlocking { assertEquals(listOf(slot), repository.observeYear(congregation, 2026).first()) }

        emitSnapshot(ranged, null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking {
            assertFailsWith<FirebaseFirestoreException> { repository.observeYear(congregation, 2026).first() }
        }
    }

    @Test
    fun `observeYear maps an external speaker and saveSlot writes the snapshot`() = runBlocking {
        val query = mockk<Query>()
        val ranged = mockk<Query>()
        every { collection.whereGreaterThanOrEqualTo(any<FieldPath>(), any()) } returns query
        every { query.whereLessThanOrEqualTo(any<FieldPath>(), any()) } returns ranged
        val external = mockk<DocumentSnapshot> {
            every { id } returns "2026-10-25"
            every { getString("time") } returns "17:30"
            every { getBoolean("talkCancelled") } returns false
            every { getString("weekEventId") } returns null
            every { getString("weekEventName") } returns null
            every { getLong("talkNumber") } returns null
            every { getString("talkTitle") } returns null
            every { getString("speakerMemberId") } returns null
            every { getString("speakerLastName") } returns "Extern"
            every { getString("speakerFirstName") } returns "Eva"
            every { getString("chairmanMemberId") } returns null
            every { getString("chairmanLastName") } returns null
            every { getString("chairmanFirstName") } returns null
            every { getString("externalSpeakerId") } returns "e1"
            every { getString("speakerCongregation") } returns "Nord"
        }
        emitSnapshot(ranged, querySnapshot(external), null)
        val repository = FirestoreTalkScheduleRepository(firestore)

        val read = repository.observeYear(congregation, 2026).first().single()
        assertEquals(ExternalSpeaker("e1", "Extern", "Eva", "Nord"), read.externalSpeaker)

        val fields = slot<Any>()
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        repository.saveSlot(congregation, read)
        val written = fields.captured as Map<*, *>
        assertEquals("e1", written["externalSpeakerId"])
        assertEquals("Nord", written["speakerCongregation"])
    }

    @Test
    fun `saveSlot writes the document under the date`() = runBlocking {
        val fields = slot<Any>()
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)

        assertEquals(slot, FirestoreTalkScheduleRepository(firestore).saveSlot(congregation, slot).getOrThrow())
        assertEquals(
            mapOf(
                "time" to "17:30",
                "talkCancelled" to true,
                "weekEventId" to "kongress",
                "weekEventName" to "Kongress",
                "talkNumber" to 12,
                "talkTitle" to "Der Weg zum Glück",
                "speakerMemberId" to "m1",
                "speakerLastName" to "Muster",
                "speakerFirstName" to "Max",
                "chairmanMemberId" to "c1",
                "chairmanLastName" to "Alt",
                "chairmanFirstName" to "Anton",
                "externalSpeakerId" to null,
                "speakerCongregation" to null,
            ),
            fields.captured,
        )
    }

    @Test
    fun `saveSlot and deleteSlot return failure when firestore rejects the write`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        every { reference.set(any()) } returns failedTask<Void?>(error)
        every { reference.delete() } returns failedTask<Void?>(error)
        val repository = FirestoreTalkScheduleRepository(firestore)

        runBlocking {
            assertFailure(error, repository.saveSlot(congregation, slot))
            assertFailure(error, repository.deleteSlot(congregation, slot.date))
        }
    }

    @Test
    fun `deleteSlot removes the document of the day`() = runBlocking {
        every { reference.delete() } returns successTask<Void?>(null)

        assertEquals(Unit, FirestoreTalkScheduleRepository(firestore).deleteSlot(congregation, slot.date).getOrThrow())
        verify { reference.delete() }
    }

    @Test
    fun `week events are mapped with defaults for missing fields`() = runBlocking {
        val kongress = mockk<DocumentSnapshot> {
            every { id } returns "kongress"
            every { getString("name") } returns "Kongress"
            every { getBoolean("cancelsTalk") } returns true
        }
        val besuch = mockk<DocumentSnapshot> {
            every { id } returns "besuch"
            every { getString("name") } returns null
            every { getBoolean("cancelsTalk") } returns null
        }
        emitSnapshot(collection, querySnapshot(kongress, besuch), null)

        val events = FirestoreWeekEventRepository(firestore).observeWeekEvents().first()

        assertEquals(listOf(WeekEvent("kongress", "Kongress", true), WeekEvent("besuch", "", false)), events)
    }

    @Test
    fun `talk catalog maps documents and skips ids that are not a talk number`(): Unit = withMockedLog {
        val active = catalogDocument("12", "Der Weg zum Glück", true)
        val paused = catalogDocument("7", "Pausiert", false)
        val defaults = catalogDocument("9", null, null)
        val notANumber = catalogDocument("abc", "Ungültig", true)
        val zero = catalogDocument("0", "Null", true)
        emitSnapshot(collection, querySnapshot(active, paused, defaults, notANumber, zero), null)

        val talks = runBlocking { FirestoreTalkCatalogRepository(firestore).observeTalks().first() }

        assertEquals(
            listOf(Talk(12, "Der Weg zum Glück", true), Talk(7, "Pausiert", false), Talk(9, "", true)),
            talks,
        )
    }

    @Test
    fun `android factories create the firestore repositories`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreTalkScheduleRepository>(createTalkScheduleRepository())
            assertIs<FirestoreWeekEventRepository>(createWeekEventRepository())
            assertIs<FirestoreTalkCatalogRepository>(createTalkCatalogRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun emitSnapshot(target: Any, snapshot: QuerySnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<QuerySnapshot>>()
        val answer = {
            listener.captured.onEvent(snapshot, error)
            registration
        }
        when (target) {
            is Query -> every { target.addSnapshotListener(capture(listener)) } answers { answer() }
            is CollectionReference -> every { target.addSnapshotListener(capture(listener)) } answers { answer() }
        }
    }

    private fun catalogDocument(id: String, title: String?, active: Boolean?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("title") } returns title
        every { getBoolean("active") } returns active
    }

    private fun querySnapshot(vararg documents: DocumentSnapshot): QuerySnapshot = mockk {
        every { this@mockk.documents } returns documents.toList()
    }

    private fun slotDocument(id: String): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("time") } returns "17:30"
        every { getBoolean("talkCancelled") } returns true
        every { getString("weekEventId") } returns "kongress"
        every { getString("weekEventName") } returns "Kongress"
        every { getLong("talkNumber") } returns 12L
        every { getString("talkTitle") } returns "Der Weg zum Glück"
        every { getString("speakerMemberId") } returns "m1"
        every { getString("speakerLastName") } returns "Muster"
        every { getString("speakerFirstName") } returns "Max"
        every { getString("chairmanMemberId") } returns "c1"
        every { getString("chairmanLastName") } returns "Alt"
        every { getString("chairmanFirstName") } returns "Anton"
        every { getString("externalSpeakerId") } returns null
        every { getString("speakerCongregation") } returns null
    }

    // Ein Termin ohne Ereignis; fehlende Felder bekommen ihre Vorgaben.
    private fun plainDocument(id: String): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("time") } returns "10:00"
        every { getBoolean("talkCancelled") } returns null
        every { getString("weekEventId") } returns null
        every { getString("weekEventName") } returns null
        every { getLong("talkNumber") } returns null
        every { getString("talkTitle") } returns null
        every { getString("speakerMemberId") } returns null
        every { getString("speakerLastName") } returns null
        every { getString("speakerFirstName") } returns null
        every { getString("chairmanMemberId") } returns null
        every { getString("chairmanLastName") } returns null
        every { getString("chairmanFirstName") } returns null
        every { getString("externalSpeakerId") } returns null
        every { getString("speakerCongregation") } returns null
    }
}
