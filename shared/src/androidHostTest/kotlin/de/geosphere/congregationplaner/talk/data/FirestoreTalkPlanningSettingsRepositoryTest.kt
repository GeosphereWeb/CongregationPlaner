package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.domain.model.TalkPlanningSettings
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
import kotlin.test.assertTrue

class FirestoreTalkPlanningSettingsRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val reference = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreTalkPlanningSettingsRepository(firestore)

    init {
        every { firestore.collection(congregation.settingsPath) } returns collection
        every { collection.document("talkPlanning") } returns reference
    }

    @Test
    fun `observe reads the talk planning document and ignores other documents and invalid dates`() = runBlocking {
        emit(document("other", "2020-01-01", null), document("talkPlanning", "2025-11-02", "kein Datum"))

        assertEquals(
            TalkPlanningSettings(speakerFreeSince = LocalDate(2025, 11, 2)),
            repository.observe(congregation).first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observe without the document has no cut-off dates`() = runBlocking {
        emit()

        assertEquals(TalkPlanningSettings(), repository.observe(congregation).first())
    }

    @Test
    fun `save writes both dates as text and reports failures`(): Unit = withMockedLog {
        val fields = slot<Map<String, Any?>>()
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        val settings = TalkPlanningSettings(talkFreeSince = LocalDate(2024, 1, 14))

        assertEquals(settings, runBlocking { repository.save(congregation, settings).getOrThrow() })
        assertEquals(mapOf("speakerFreeSince" to null, "talkFreeSince" to "2024-01-14"), fields.captured)

        every { reference.set(any()) } returns failedTask(IllegalStateException("denied"))
        assertTrue(runBlocking { repository.save(congregation, settings).isFailure })
    }

    private fun emit(vararg documents: DocumentSnapshot) {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.documents } returns documents.toList() }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
    }

    private fun document(id: String, speaker: String?, talk: String?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("speakerFreeSince") } returns speaker
        every { getString("talkFreeSince") } returns talk
    }
}
