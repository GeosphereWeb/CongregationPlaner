package de.geosphere.congregationplaner.talk.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import de.geosphere.congregationplaner.organization.domain.model.CountryRef
import de.geosphere.congregationplaner.talk.domain.model.Talk
import de.geosphere.congregationplaner.talk.domain.model.TalkTitleChange
import de.geosphere.congregationplaner.talk.domain.model.WeekEvent
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreReferenceDataRepositoriesTest {
    private val country = CountryRef("EMEA", "europe", "central-europe", "de")
    private val firestore = mockk<FirebaseFirestore>()
    private val talks = mockk<CollectionReference>()
    private val events = mockk<CollectionReference>()
    private val talkCatalog = FirestoreTalkCatalogRepository(firestore)
    private val weekEvents = FirestoreWeekEventRepository(firestore)

    init {
        every { firestore.collection("${country.documentPath}/talkCatalog") } returns talks
        every { firestore.collection("${country.documentPath}/weekEvents") } returns events
    }

    @Test
    fun `saveTalk writes title, active flag and title changes under the number`() = runBlocking {
        val fields = slot<Any>()
        val reference = mockk<DocumentReference>()
        every { talks.document("12") } returns reference
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)
        val talk = Talk(
            12,
            "Der Weg zum Glück",
            active = false,
            titleChanges = listOf(TalkTitleChange(LocalDate(2027, 1, 1), "Neuer Titel")),
        )

        assertEquals(talk, talkCatalog.save(country, talk).getOrThrow())
        assertEquals(
            mapOf(
                "title" to "Der Weg zum Glück",
                "active" to false,
                "titleChanges" to listOf(mapOf("validFrom" to "2027-01-01", "title" to "Neuer Titel")),
            ),
            fields.captured,
        )
    }

    @Test
    fun `deleteTalk removes the document of the number`() = runBlocking {
        val reference = mockk<DocumentReference>()
        every { talks.document("12") } returns reference
        every { reference.delete() } returns successTask<Void?>(null)

        assertEquals(Unit, talkCatalog.delete(country, 12).getOrThrow())
        verify { reference.delete() }
    }

    @Test
    fun `saveWeekEvent creates a document with a generated id for a new event`() = runBlocking {
        val fields = slot<Any>()
        val reference = mockk<DocumentReference> { every { id } returns "auto1" }
        every { events.document() } returns reference
        every { reference.set(capture(fields)) } returns successTask<Void?>(null)

        val saved = weekEvents.save(country, WeekEvent("", "Kongress", cancelsTalk = true)).getOrThrow()

        assertEquals(WeekEvent("auto1", "Kongress", cancelsTalk = true), saved)
        assertEquals(mapOf("name" to "Kongress", "cancelsTalk" to true), fields.captured)
    }

    @Test
    fun `saveWeekEvent overwrites the document of an existing event and deleteWeekEvent removes it`() = runBlocking {
        val reference = mockk<DocumentReference> { every { id } returns "ev1" }
        every { events.document("ev1") } returns reference
        every { reference.set(any()) } returns successTask<Void?>(null)
        every { reference.delete() } returns successTask<Void?>(null)
        val event = WeekEvent("ev1", "Besuch")

        assertEquals(event, weekEvents.save(country, event).getOrThrow())
        assertEquals(Unit, weekEvents.delete(country, "ev1").getOrThrow())
        verify { reference.delete() }
    }

    @Test
    fun `writes return failure when firestore rejects them`(): Unit = withMockedLog {
        val error = IllegalStateException("permission denied")
        val talkReference = mockk<DocumentReference>()
        val eventReference = mockk<DocumentReference> { every { id } returns "ev1" }
        every { talks.document("12") } returns talkReference
        every { events.document("ev1") } returns eventReference
        every { talkReference.set(any()) } returns failedTask<Void?>(error)
        every { talkReference.delete() } returns failedTask<Void?>(error)
        every { eventReference.set(any()) } returns failedTask<Void?>(error)
        every { eventReference.delete() } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, talkCatalog.save(country, Talk(12, "Titel")))
            assertFailure(error, talkCatalog.delete(country, 12))
            assertFailure(error, weekEvents.save(country, WeekEvent("ev1", "Besuch")))
            assertFailure(error, weekEvents.delete(country, "ev1"))
        }
    }
}
