package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.ContactAttempt
import de.geosphere.congregationplaner.talk.domain.model.ContactKind
import de.geosphere.congregationplaner.talk.domain.model.SpeakerRequest
import de.geosphere.congregationplaner.talk.domain.model.TalkSlot
import de.geosphere.congregationplaner.talk.domain.repository.ScheduleWrite
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreSpeakerRequestRepositoryTest {
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val firestore = mockk<FirebaseFirestore>()
    private val slots = mockk<CollectionReference>()
    private val requests = mockk<CollectionReference>()
    private val request = SpeakerRequest(
        id = "r1",
        createdOn = LocalDate(2026, 11, 10),
        note = "Frühjahr",
        contactAttempts = listOf(
            ContactAttempt(LocalDate(2026, 11, 12), ContactKind.VOICEMAIL, "zurückrufen"),
            ContactAttempt(LocalDate(2026, 11, 20), ContactKind.CALL),
        ),
        externalSpeakerId = "e1",
        speakerLastName = "Extern",
        speakerFirstName = "Eva",
        speakerCongregation = "Nord",
        talkNumber = 12,
        talkTitle = "Der Weg zum Glück",
    )

    init {
        every { firestore.collection(congregation.talkSchedulePath) } returns slots
        every { firestore.collection(congregation.talkRequestsPath) } returns requests
    }

    @Test
    fun `observeRequests maps requests with contact attempts and skips invalid documents`(): Unit = withMockedLog {
        val invalid = mockk<DocumentSnapshot>(relaxed = true) {
            every { id } returns "bad"
            every { getString("createdOn") } returns "2026-02-31"
        }
        val listener = slot<EventListener<QuerySnapshot>>()
        every { requests.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(
                mockk { every { documents } returns listOf(invalid, requestDocument()) },
                null,
            )
            mockk<ListenerRegistration>(relaxed = true)
        }

        val read = runBlocking { FirestoreSpeakerRequestRepository(firestore).observeRequests(congregation).first() }

        assertEquals(listOf(request), read)
    }

    @Test
    fun `commit writes slots and requests and deletes in one batch`() = runBlocking {
        val batch = mockk<WriteBatch>()
        val slotRef = mockk<DocumentReference>()
        val oldSlotRef = mockk<DocumentReference>()
        val requestRef = mockk<DocumentReference>()
        val oldRequestRef = mockk<DocumentReference>()
        every { firestore.batch() } returns batch
        every { slots.document("2026-12-06") } returns slotRef
        every { slots.document("2026-11-29") } returns oldSlotRef
        every { requests.document("r1") } returns requestRef
        every { requests.document("r0") } returns oldRequestRef
        val requestFields = slot<Any>()
        every { batch.set(requestRef, capture(requestFields)) } returns batch
        every { batch.set(slotRef, any()) } returns batch
        every { batch.delete(any()) } returns batch
        every { batch.commit() } returns successTask<Void?>(null)

        val result = FirestoreSpeakerRequestRepository(firestore).commit(
            congregation,
            ScheduleWrite(
                saveSlots = listOf(TalkSlot(LocalDate(2026, 12, 6), LocalTime(10, 0), requestId = "r1")),
                deleteSlotDates = listOf(LocalDate(2026, 11, 29)),
                saveRequests = listOf(request),
                deleteRequestIds = listOf("r0"),
            ),
        )

        assertTrue(result.isSuccess)
        verify { batch.set(slotRef, any()) }
        verify { batch.delete(oldSlotRef) }
        verify { batch.delete(oldRequestRef) }
        verify(exactly = 1) { batch.commit() }
        val written = requestFields.captured as Map<*, *>
        assertEquals("2026-11-10", written["createdOn"])
        assertEquals(
            listOf(
                mapOf("date" to "2026-11-12", "kind" to "VOICEMAIL", "note" to "zurückrufen"),
                mapOf("date" to "2026-11-20", "kind" to "CALL", "note" to null),
            ),
            written["contactAttempts"],
        )
        assertEquals(12, written["talkNumber"])
    }

    private fun requestDocument(): DocumentSnapshot = mockk {
        every { id } returns "r1"
        every { getString("createdOn") } returns "2026-11-10"
        every { getString("note") } returns "Frühjahr"
        every { this@mockk.get("contactAttempts") } returns listOf(
            mapOf("date" to "2026-11-12", "kind" to "VOICEMAIL", "note" to "zurückrufen"),
            mapOf("date" to "2026-11-20", "kind" to "CALL", "note" to null),
        )
        every { getString("speakerMemberId") } returns null
        every { getString("speakerLastName") } returns "Extern"
        every { getString("speakerFirstName") } returns "Eva"
        every { getString("externalSpeakerId") } returns "e1"
        every { getString("speakerCongregation") } returns "Nord"
        every { getLong("talkNumber") } returns 12L
        every { getString("talkTitle") } returns "Der Weg zum Glück"
    }
}
