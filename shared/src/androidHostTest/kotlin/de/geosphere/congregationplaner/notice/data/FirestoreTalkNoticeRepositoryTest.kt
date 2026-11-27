package de.geosphere.congregationplaner.notice.data

import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import de.geosphere.congregationplaner.notice.domain.model.NoticeColorSet
import de.geosphere.congregationplaner.notice.domain.model.NoticeImage
import de.geosphere.congregationplaner.notice.domain.model.TalkNoticeSettings
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.talk.data.settingsPath
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirestoreTalkNoticeRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val settingsCollection = mockk<CollectionReference>()
    private val imagesCollection = mockk<CollectionReference>()
    private val settingsReference = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")
    private val repository = FirestoreTalkNoticeRepository(firestore)
    private val png = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10, 0, 127, -128, -1)

    init {
        every { firestore.collection(congregation.settingsPath) } returns settingsCollection
        every { settingsCollection.document("talkNotice") } returns settingsReference
        every { firestore.collection(congregation.noticeImagesPath) } returns imagesCollection
    }

    @Test
    fun `settings without a document are the defaults`() = runBlocking {
        emit()

        assertEquals(TalkNoticeSettings(), repository.observeSettings(congregation).first())
        verify { registration.remove() }
    }

    @Test
    fun `settings read the talkNotice document and ignore others`() = runBlocking {
        emit(settings("talkPlanning", "Falsch", "blue", null), settings("talkNotice", "Vorträge", "green", "img1"))

        assertEquals(
            TalkNoticeSettings("Vorträge", NoticeColorSet.GREEN, "img1"),
            repository.observeSettings(congregation).first(),
        )
    }

    @Test
    fun `an unknown color set falls back to blue`() = runBlocking {
        emit(settings("talkNotice", "T", "pink", null))

        assertEquals(
            TalkNoticeSettings("T", NoticeColorSet.BLUE, null),
            repository.observeSettings(congregation).first(),
        )
    }

    @Test
    fun `saveSettings writes the color set key and reports failures`(): Unit = withMockedLog {
        val fields = slot<Map<String, Any?>>()
        every { settingsReference.set(capture(fields)) } returns successTask<Void?>(null)
        val settings = TalkNoticeSettings("Vorträge", NoticeColorSet.PETROL, null)

        assertEquals(settings, runBlocking { repository.saveSettings(congregation, settings).getOrThrow() })
        assertEquals(
            mapOf("title" to "Vorträge", "colorSet" to "petrol", "defaultImageId" to null),
            fields.captured,
        )

        every { settingsReference.set(any()) } returns failedTask(IllegalStateException("denied"))
        assertTrue(runBlocking { repository.saveSettings(congregation, settings).isFailure })
    }

    @Test
    fun `loadImages reads name and exact bytes once`() = runBlocking {
        val snapshot = mockk<QuerySnapshot> { every { documents } returns listOf(image("a1", "Logo")) }
        every { imagesCollection.get() } returns successTask(snapshot)

        assertEquals(listOf(NoticeImage("a1", "Logo", png)), repository.loadImages(congregation).getOrThrow())
    }

    @Test
    fun `addImage stores name and blob and returns the new id`(): Unit = withMockedLog {
        val fields = slot<Map<String, Any?>>()
        val created = mockk<DocumentReference> { every { id } returns "new42" }
        every { imagesCollection.add(capture(fields)) } returns successTask(created)

        val image = runBlocking { repository.addImage(congregation, "Logo", png).getOrThrow() }

        assertEquals(NoticeImage("new42", "Logo", png), image)
        assertEquals("Logo", fields.captured["name"])
        assertEquals(Blob.fromBytes(png), fields.captured["png"])

        every { imagesCollection.add(any()) } returns failedTask(IllegalStateException("denied"))
        assertTrue(runBlocking { repository.addImage(congregation, "Logo", png).isFailure })
    }

    @Test
    fun `renameImage updates only the name`(): Unit = withMockedLog {
        val reference = mockk<DocumentReference>()
        every { imagesCollection.document("a1") } returns reference
        every { reference.update("name", "Neu") } returns successTask<Void?>(null)

        runBlocking { repository.renameImage(congregation, "a1", "Neu").getOrThrow() }

        verify { reference.update("name", "Neu") }
    }

    @Test
    fun `deleteImage removes the document`(): Unit = withMockedLog {
        val reference = mockk<DocumentReference>()
        every { imagesCollection.document("a1") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        runBlocking { repository.deleteImage(congregation, "a1").getOrThrow() }

        verify { reference.delete() }
    }

    private fun emit(vararg documents: DocumentSnapshot) {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> { every { this@mockk.documents } returns documents.toList() }
        every { settingsCollection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
    }

    private fun settings(id: String, title: String, colorSet: String, defaultImageId: String?): DocumentSnapshot =
        mockk {
            every { this@mockk.id } returns id
            every { getString("title") } returns title
            every { getString("colorSet") } returns colorSet
            every { getString("defaultImageId") } returns defaultImageId
        }

    private fun image(id: String, name: String): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.get("name") } returns name
        every { getBlob("png") } returns Blob.fromBytes(png)
    }
}
