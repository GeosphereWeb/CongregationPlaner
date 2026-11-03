package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberPrivateDetails
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirestoreMemberPrivateRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val document = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreMemberPrivateRepository(firestore)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")

    init {
        every { firestore.collection(congregation.memberPrivatePath) } returns collection
        every { collection.document("uid-1") } returns document
    }

    @Test
    fun `observe maps address and contacts and skips entries that are no map`() = runBlocking {
        val snapshot = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { getString("street") } returns "Hauptstr. 1"
            every { getString("postalCode") } returns "10115"
            every { getString("city") } returns "Berlin"
        }
        // Außerhalb des mockk-Blocks, weil dort `get` die DynamicCall-Funktion von MockK meint.
        every { snapshot.get("emergencyContacts") } returns listOf(
            mapOf("name" to "Anna", "relation" to "Ehefrau", "phone" to "0170"),
            "kaputt",
            mapOf("name" to "Ben", "phone" to "030"),
        )
        emit(snapshot)

        val details = repository.observe(congregation, "uid-1").first()

        assertEquals(
            MemberPrivateDetails(
                street = "Hauptstr. 1",
                postalCode = "10115",
                city = "Berlin",
                emergencyContacts = listOf(
                    EmergencyContact("Anna", "Ehefrau", "0170"),
                    EmergencyContact("Ben", "", "030"),
                ),
            ),
            details,
        )
        verify { registration.remove() }
    }

    @Test
    fun `observe emits null while nothing was saved`() = runBlocking {
        emit(mockk { every { exists() } returns false })

        assertNull(repository.observe(congregation, "uid-1").first())
    }

    @Test
    fun `save writes the fields and leaves out an empty relation`() = withMockedLog {
        val fields = slot<Any>()
        every { document.set(capture(fields)) } returns successTask<Void?>(null)
        val details = MemberPrivateDetails(
            street = "Hauptstr. 1",
            postalCode = "10115",
            city = "Berlin",
            emergencyContacts = listOf(EmergencyContact("Anna", "", "0170")),
        )

        runBlocking { repository.save(congregation, "uid-1", details).getOrThrow() }

        assertEquals(
            mapOf(
                "street" to "Hauptstr. 1",
                "postalCode" to "10115",
                "city" to "Berlin",
                "emergencyContacts" to listOf(mapOf("name" to "Anna", "phone" to "0170")),
            ),
            fields.captured,
        )
    }

    @Test
    fun `save and delete report failures`() = withMockedLog {
        val error = IllegalStateException("permission denied")
        every { document.set(any()) } returns failedTask<Void?>(error)
        every { document.delete() } returns failedTask<Void?>(error)

        runBlocking {
            assertFailure(error, repository.save(congregation, "uid-1", MemberPrivateDetails()))
            assertFailure(error, repository.delete(congregation, "uid-1"))
        }
    }

    private fun emit(snapshot: DocumentSnapshot) {
        val listener = slot<EventListener<DocumentSnapshot>>()
        every { document.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
    }
}
