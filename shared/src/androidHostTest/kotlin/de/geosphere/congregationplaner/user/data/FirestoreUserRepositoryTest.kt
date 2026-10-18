package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.auth.FirebaseUser
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.UserRole
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class FirestoreUserRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val document = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreUserRepository(firestore)

    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = "User"
        override val idToken: String? = null
    }

    init {
        every { firestore.collection(USERS_COLLECTION_PATH) } returns collection
        every { collection.document("uid-1") } returns document
    }

    @Test
    fun `getOrCreateUser reads existing user from server`() = runBlocking {
        every { document.get(Source.SERVER) } returns successTask(userSnapshot(approved = true, role = "ADMIN"))

        assertEquals(
            AppUser("uid-1", "user@example.com", "User", approved = true, role = UserRole.ADMIN),
            repository.getOrCreateUser(authUser).getOrThrow(),
        )
    }

    @Test
    fun `getOrCreateUser creates unapproved user when document is missing`() = runBlocking {
        val missing = mockk<DocumentSnapshot> { every { exists() } returns false }
        val fields = slot<Any>()
        every { document.get(Source.SERVER) } returns successTask(missing)
        every { document.set(capture(fields)) } returns successTask<Void?>(null)

        val user = repository.getOrCreateUser(authUser).getOrThrow()

        assertEquals(
            AppUser("uid-1", "user@example.com", "User", approved = false, role = UserRole.SPEAKING_ASSISTANT),
            user,
        )
        assertEquals(
            mapOf(
                "email" to "user@example.com",
                "displayName" to "User",
                "approved" to false,
                "role" to "SPEAKING_ASSISTANT",
            ),
            fields.captured,
        )
    }

    @Test
    fun `getOrCreateUser returns failure when firestore fails`() = withMockedLog {
        val error = IllegalStateException("offline")
        every { document.get(Source.SERVER) } returns failedTask(error)

        runBlocking { assertFailure(error, repository.getOrCreateUser(authUser)) }
    }

    @Test
    fun `observeUser emits mapped user and removes listener`() = runBlocking {
        emitSnapshot(userSnapshot(approved = null, role = null), null)

        assertEquals(
            AppUser("uid-1", "user@example.com", "User", approved = false, role = UserRole.NONE),
            repository.observeUser("uid-1").first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeUser emits null for missing documents`() = runBlocking {
        emitSnapshot(null, null)
        assertNull(repository.observeUser("uid-1").first())

        emitSnapshot(mockk { every { exists() } returns false }, null)
        assertNull(repository.observeUser("uid-1").first())
    }

    @Test
    fun `observeUser fails for listener errors and unparsable documents`(): Unit = withMockedLog {
        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking { assertFailsWith<FirebaseFirestoreException> { repository.observeUser("uid-1").first() } }

        val broken = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { id } returns "uid-1"
            every { getString("email") } throws RuntimeException("Field 'email' is not a String")
        }
        emitSnapshot(broken, null)
        runBlocking { assertFailsWith<RuntimeException> { repository.observeUser("uid-1").first() } }
    }

    @Test
    fun `android factory creates firestore repository`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<FirestoreUserRepository>(createUserRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun emitSnapshot(snapshot: DocumentSnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<DocumentSnapshot>>()
        every { document.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, error)
            registration
        }
    }

    private fun userSnapshot(approved: Boolean?, role: String?): DocumentSnapshot = mockk {
        every { exists() } returns true
        every { id } returns "uid-1"
        every { getString("email") } returns "user@example.com"
        every { getString("displayName") } returns "User"
        every { getBoolean("approved") } returns approved
        every { getString("role") } returns role
    }
}
