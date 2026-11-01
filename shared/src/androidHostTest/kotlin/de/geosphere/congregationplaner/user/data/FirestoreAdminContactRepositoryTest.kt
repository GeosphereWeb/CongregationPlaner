package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.AdminContact
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirestoreAdminContactRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val document = mockk<DocumentReference>()
    private val repository = FirestoreAdminContactRepository(firestore)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "609a", "berlin")

    init {
        every { firestore.collection(congregation.adminContactsPath) } returns collection
        every { collection.document("a1") } returns document
    }

    @Test
    fun `getAdminContacts reads the contacts from the server`() = withMockedLog {
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(
                contactSnapshot("a1", "Anna", "Admin"),
                contactSnapshot("a2", null, "Ohne Vorname"),
            )
        }
        every { collection.get(Source.SERVER) } returns successTask(snapshot)

        val contacts = runBlocking { repository.getAdminContacts(congregation).getOrThrow() }

        assertEquals(listOf(AdminContact("a1", "Anna", "Admin"), AdminContact("a2", "", "Ohne Vorname")), contacts)
    }

    @Test
    fun `getAdminContacts reports failures`() = withMockedLog {
        val error = IllegalStateException("permission-denied")
        every { collection.get(Source.SERVER) } returns failedTask(error)

        runBlocking { assertFailure(error, repository.getAdminContacts(congregation)) }
    }

    @Test
    fun `putAdminContact stores only the name`() = withMockedLog {
        every { document.set(any()) } returns successTask<Void?>(null)

        runBlocking { repository.putAdminContact(congregation, AdminContact("a1", "Anna", "Admin")).getOrThrow() }

        verify { document.set(mapOf("firstName" to "Anna", "lastName" to "Admin")) }

        val error = IllegalStateException("permission-denied")
        every { document.set(any()) } returns failedTask(error)
        runBlocking { assertFailure(error, repository.putAdminContact(congregation, AdminContact("a1", "A", "B"))) }
    }

    @Test
    fun `deleteAdminContact deletes the document and reports failures`() = withMockedLog {
        every { document.delete() } returns successTask<Void?>(null)
        runBlocking { repository.deleteAdminContact(congregation, "a1").getOrThrow() }
        verify { document.delete() }

        val error = IllegalStateException("permission-denied")
        every { document.delete() } returns failedTask(error)
        runBlocking { assertFailure(error, repository.deleteAdminContact(congregation, "a1")) }
    }

    @Test
    fun `android factory creates firestore admin contact repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreAdminContactRepository>(createAdminContactRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun contactSnapshot(id: String, firstName: String?, lastName: String?): DocumentSnapshot = mockk {
        every { this@mockk.id } returns id
        every { getString("firstName") } returns firstName
        every { getString("lastName") } returns lastName
    }
}
