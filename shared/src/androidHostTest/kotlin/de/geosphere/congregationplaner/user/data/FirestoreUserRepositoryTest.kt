package de.geosphere.congregationplaner.user.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.testing.assertFailure
import de.geosphere.congregationplaner.testing.failedTask
import de.geosphere.congregationplaner.testing.successTask
import de.geosphere.congregationplaner.testing.withMockedLog
import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.Appointment
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.EmergencyContact
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FirestoreUserRepositoryTest {
    private val firestore = mockk<FirebaseFirestore>()
    private val collection = mockk<CollectionReference>()
    private val document = mockk<DocumentReference>()
    private val adminDocument = mockk<DocumentReference>()
    private val registration = mockk<ListenerRegistration>(relaxed = true)
    private val repository = FirestoreUserRepository(firestore)
    private val congregation = CongregationRef("EMEA", "europe", "central-europe", "de", "609a", "berlin")

    private val authUser = object : FirebaseUser {
        override val uid: String = "uid-1"
        override val email: String? = "user@example.com"
        override val displayName: String? = "User"
        override val idToken: String? = null
    }

    init {
        every { firestore.collection(congregation.membersPath) } returns collection
        every { collection.document("uid-1") } returns document
        every { firestore.collection(ADMINS_COLLECTION_PATH) } returns mockk {
            every { document("uid-1") } returns adminDocument
        }
    }

    @Test
    fun `getOrCreateUser reads existing member from server`() = runBlocking {
        every { document.get(Source.SERVER) } returns successTask(
            memberSnapshot(approved = true, roles = listOf("CONGREGATION_ADMIN", "BOSS", 1)),
        )

        assertEquals(
            AppUser(
                "uid-1",
                "user@example.com",
                "User",
                approved = true,
                roles = setOf(CongregationRole.CONGREGATION_ADMIN),
                congregation = congregation,
            ),
            repository.getOrCreateUser(authUser, congregation).getOrThrow(),
        )
    }

    @Test
    fun `getOrCreateUser creates an unapproved member without roles when the document is missing`() = runBlocking {
        val missing = mockk<DocumentSnapshot> { every { exists() } returns false }
        val fields = slot<Any>()
        every { document.get(Source.SERVER) } returns successTask(missing)
        every { document.set(capture(fields)) } returns successTask<Void?>(null)

        val user = repository.getOrCreateUser(authUser, congregation).getOrThrow()

        assertEquals(
            AppUser(
                "uid-1",
                "user@example.com",
                "User",
                approved = false,
                congregation = congregation,
                profile = MemberProfile(firstName = "User"),
            ),
            user,
        )
        // Der Anzeigename „User“ ist ein einzelnes Wort und wird zum Vornamen.
        assertEquals(
            mapOf(
                "email" to "user@example.com",
                "displayName" to "User",
                "approved" to false,
                "roles" to emptyList<String>(),
                "hasAccount" to true,
            ) + defaultProfileFields + mapOf("firstName" to "User"),
            fields.captured,
        )
    }

    @Test
    fun `getOrCreateUser maps profile fields and the account flag`() = runBlocking {
        every { document.get(Source.SERVER) } returns successTask(
            memberSnapshot(
                approved = true,
                role = "MEMBER",
                permissions = null,
                profile = mapOf(
                    "lastName" to "Muster",
                    "firstName" to "Max",
                    "category" to "BROTHER",
                    "baptized" to true,
                    "appointment" to "MINISTERIAL_SERVANT",
                    "mobilePhone" to "0170 123",
                    "landlinePhone" to "030 456",
                    "isSpeaker" to true,
                    // Firestore delivers integers as Long; entries that are no number are skipped.
                    "talkNumbers" to listOf(12L, "x", 45L),
                    "emails" to listOf("max@beispiel.de"),
                    "street" to "Hauptstr. 1",
                    "postalCode" to "10115",
                    "city" to "Berlin",
                    // Entries that are no map are skipped; a missing relation is empty.
                    "emergencyContacts" to listOf(
                        mapOf("name" to "Anna", "relation" to "Ehefrau", "phones" to listOf("0170 1", 7)),
                        "kaputt",
                        mapOf("name" to "Ben", "phones" to listOf("030 2")),
                    ),
                    "hasAccount" to false,
                ),
            ),
        )

        val user = repository.getOrCreateUser(authUser, congregation).getOrThrow()

        assertFalse(user.hasAccount)
        assertEquals(
            MemberProfile(
                lastName = "Muster",
                firstName = "Max",
                category = MemberCategory.BROTHER,
                baptized = true,
                appointment = Appointment.MINISTERIAL_SERVANT,
                mobilePhone = "0170 123",
                landlinePhone = "030 456",
                isSpeaker = true,
                talkNumbers = listOf(12, 45),
                emails = listOf("max@beispiel.de"),
                street = "Hauptstr. 1",
                postalCode = "10115",
                city = "Berlin",
                emergencyContacts = listOf(
                    EmergencyContact("Anna", "Ehefrau", listOf("0170 1")),
                    EmergencyContact("Ben", "", listOf("030 2")),
                ),
            ),
            user.profile,
        )
    }

    @Test
    fun `createMemberWithoutAccount stores a person with generated id and without access`() = runBlocking {
        val generated = mockk<DocumentReference>()
        val fields = slot<Any>()
        every { collection.document() } returns generated
        every { generated.id } returns "generated-1"
        every { generated.set(capture(fields)) } returns successTask<Void?>(null)
        val profile = MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD)

        val member = repository.createMemberWithoutAccount(congregation, profile).getOrThrow()

        assertEquals(
            AppUser(
                uid = "generated-1",
                approved = false,
                congregation = congregation,
                hasAccount = false,
                profile = profile,
            ),
            member,
        )
        assertEquals(
            mapOf(
                "email" to null,
                "displayName" to null,
                "approved" to false,
                "roles" to emptyList<String>(),
                "hasAccount" to false,
            ) + defaultProfileFields + mapOf("lastName" to "Muster", "firstName" to "Lena", "category" to "CHILD"),
            fields.captured,
        )
    }

    @Test
    fun `updateMemberProfile writes only profile fields`() = withMockedLog {
        val fields = slot<Map<String, Any?>>()
        every { document.update(capture(fields)) } returns successTask<Void?>(null)
        val profile = MemberProfile(
            lastName = "Muster",
            category = MemberCategory.BROTHER,
            baptized = true,
            appointment = Appointment.ELDER,
            isSpeaker = true,
            talkNumbers = listOf(12, 45),
        )
        val member = AppUser(uid = "uid-1", approved = true, profile = profile)

        runBlocking { assertEquals(member, repository.updateMemberProfile(congregation, member).getOrThrow()) }
        assertEquals(
            defaultProfileFields + mapOf(
                "lastName" to "Muster",
                "category" to "BROTHER",
                "baptized" to true,
                "appointment" to "ELDER",
                "isSpeaker" to true,
                "talkNumbers" to listOf(12, 45),
            ),
            fields.captured,
        )

        val error = IllegalStateException("not-found")
        every { document.update(any<Map<String, Any?>>()) } returns failedTask(error)
        runBlocking { assertFailure(error, repository.updateMemberProfile(congregation, member)) }
    }

    @Test
    fun `updateOwnProfile writes only the own contact fields`() = withMockedLog {
        val fields = slot<Map<String, Any?>>()
        every { document.update(capture(fields)) } returns successTask<Void?>(null)
        val profile = MemberProfile(
            lastName = "Muster",
            firstName = "Max",
            category = MemberCategory.BROTHER,
            mobilePhone = "0170",
            landlinePhone = "030 1",
            city = "Berlin",
            emergencyContacts = listOf(
                EmergencyContact("Anna", "Ehefrau", listOf("0171")),
                EmergencyContact("Ben", "", listOf("030 2")),
            ),
        )
        val member = AppUser(uid = "uid-1", approved = true, profile = profile)

        runBlocking { assertEquals(member, repository.updateOwnProfile(congregation, member).getOrThrow()) }
        assertEquals(
            mapOf(
                "lastName" to "Muster",
                "firstName" to "Max",
                "mobilePhone" to "0170",
                "landlinePhone" to "030 1",
                "emails" to emptyList<String>(),
                "street" to "",
                "postalCode" to "",
                "city" to "Berlin",
                // An empty relation is left out; the rules allow it only as an optional field.
                "emergencyContacts" to listOf(
                    mapOf("name" to "Anna", "relation" to "Ehefrau", "phones" to listOf("0171")),
                    mapOf("name" to "Ben", "phones" to listOf("030 2")),
                ),
            ),
            fields.captured,
        )

        val error = IllegalStateException("denied")
        every { document.update(any<Map<String, Any?>>()) } returns failedTask(error)
        runBlocking { assertFailure(error, repository.updateOwnProfile(congregation, member)) }
    }

    @Test
    fun `getOrCreateUser returns failure when firestore fails`() = withMockedLog {
        val error = IllegalStateException("offline")
        every { document.get(Source.SERVER) } returns failedTask(error)

        runBlocking { assertFailure(error, repository.getOrCreateUser(authUser, congregation)) }
    }

    @Test
    fun `isGlobalAdmin checks the admins document`() = runBlocking {
        every { adminDocument.get(Source.SERVER) } returns successTask(mockk { every { exists() } returns true })
        assertTrue(repository.isGlobalAdmin("uid-1").getOrThrow())

        every { adminDocument.get(Source.SERVER) } returns successTask(mockk { every { exists() } returns false })
        assertFalse(repository.isGlobalAdmin("uid-1").getOrThrow())
    }

    @Test
    fun `observeGlobalAdminProfile maps the admin document and emits an empty profile without one`() = runBlocking {
        emitAdminSnapshot(
            mockk {
                every { exists() } returns true
                every { getString("lastName") } returns "Admin"
                every { getString("firstName") } returns "Anna"
                every { getString("mobilePhone") } returns "0170"
                every { getString("landlinePhone") } returns null
                every { this@mockk.get("emails") } returns listOf("a@b.de", 5)
            },
            null,
        )
        assertEquals(
            MemberProfile(lastName = "Admin", firstName = "Anna", mobilePhone = "0170", emails = listOf("a@b.de")),
            repository.observeGlobalAdminProfile("uid-1").first(),
        )
        verify { registration.remove() }

        emitAdminSnapshot(mockk { every { exists() } returns false }, null)
        assertEquals(MemberProfile(), repository.observeGlobalAdminProfile("uid-1").first())

        emitAdminSnapshot(null, null)
        assertEquals(MemberProfile(), repository.observeGlobalAdminProfile("uid-1").first())
    }

    @Test
    fun `observeGlobalAdminProfile fails for listener errors`(): Unit = withMockedLog {
        emitAdminSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))

        runBlocking {
            assertFailsWith<FirebaseFirestoreException> { repository.observeGlobalAdminProfile("uid-1").first() }
        }
    }

    @Test
    fun `updateGlobalAdminProfile writes names phones and e-mails into the admin document`() = withMockedLog {
        val fields = slot<Map<String, Any>>()
        every { adminDocument.update(capture(fields)) } returns successTask<Void?>(null)
        val profile = MemberProfile(
            lastName = "Admin",
            firstName = "Anna",
            mobilePhone = "0170",
            emails = listOf("a@b.de"),
            street = "Hauptstr. 1",
            isSpeaker = true,
        )

        runBlocking { repository.updateGlobalAdminProfile("uid-1", profile).getOrThrow() }
        assertEquals(
            mapOf(
                "lastName" to "Admin",
                "firstName" to "Anna",
                "mobilePhone" to "0170",
                "landlinePhone" to "",
                "emails" to listOf("a@b.de"),
            ),
            fields.captured,
        )

        val error = IllegalStateException("denied")
        every { adminDocument.update(any<Map<String, Any>>()) } returns failedTask(error)
        runBlocking { assertFailure(error, repository.updateGlobalAdminProfile("uid-1", profile)) }
    }

    @Test
    fun `observeUser emits mapped member and removes listener`() = runBlocking {
        emitSnapshot(memberSnapshot(approved = null, role = null, permissions = null), null)

        assertEquals(
            AppUser(
                "uid-1",
                "user@example.com",
                "User",
                approved = false,
                congregation = congregation,
            ),
            repository.observeUser(congregation, "uid-1").first(),
        )
        verify { registration.remove() }
    }

    @Test
    fun `observeUser emits null for missing documents`() = runBlocking {
        emitSnapshot(null, null)
        assertNull(repository.observeUser(congregation, "uid-1").first())

        emitSnapshot(mockk { every { exists() } returns false }, null)
        assertNull(repository.observeUser(congregation, "uid-1").first())
    }

    @Test
    fun `observeUser fails for listener errors and unparsable documents`(): Unit = withMockedLog {
        emitSnapshot(null, mockk<FirebaseFirestoreException>(relaxed = true))
        runBlocking {
            assertFailsWith<FirebaseFirestoreException> { repository.observeUser(congregation, "uid-1").first() }
        }

        val broken = mockk<DocumentSnapshot> {
            every { exists() } returns true
            every { id } returns "uid-1"
            every { getString("email") } throws RuntimeException("Field 'email' is not a String")
        }
        emitSnapshot(broken, null)
        runBlocking { assertFailsWith<RuntimeException> { repository.observeUser(congregation, "uid-1").first() } }
    }

    @Test
    fun `observeMembers emits mapped members and fails for listener errors`(): Unit = withMockedLog {
        val listener = slot<EventListener<QuerySnapshot>>()
        val snapshot = mockk<QuerySnapshot> {
            every { documents } returns listOf(memberSnapshot(approved = true, role = "MEMBER", permissions = null))
        }
        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, null)
            registration
        }
        runBlocking {
            assertEquals(listOf("uid-1"), repository.observeMembers(congregation).first().map { it.uid })
        }

        every { collection.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(null, mockk<FirebaseFirestoreException>(relaxed = true))
            registration
        }
        runBlocking { assertFailsWith<FirebaseFirestoreException> { repository.observeMembers(congregation).first() } }
    }

    @Test
    fun `updateMember writes only approval and roles`() = runBlocking {
        val fields = slot<Map<String, Any>>()
        every { document.update(capture(fields)) } returns successTask<Void?>(null)
        val member =
            AppUser(
                uid = "uid-1",
                email = "ignored",
                approved = true,
                roles = setOf(CongregationRole.CONGREGATION_ADMIN),
            )

        assertEquals(member, repository.updateMember(congregation, member).getOrThrow())
        assertEquals(
            mapOf(
                "approved" to true,
                "roles" to listOf("CONGREGATION_ADMIN"),
            ),
            fields.captured,
        )
    }

    @Test
    fun `removeMember deletes the member document and reports failures`() = withMockedLog {
        every { document.delete() } returns successTask<Void?>(null)
        runBlocking { repository.removeMember(congregation, "uid-1").getOrThrow() }
        verify { document.delete() }

        val error = IllegalStateException("permission-denied")
        every { document.delete() } returns failedTask(error)
        runBlocking { assertFailure(error, repository.removeMember(congregation, "uid-1")) }
    }

    @Test
    fun `android factory creates firestore repository with admin contacts`() {
        mockkStatic(FirebaseFirestore::class)
        try {
            every { FirebaseFirestore.getInstance() } returns mockk(relaxed = true)
            assertIs<AdminContactSyncingUserRepository>(createUserRepository())
        } finally {
            unmockkStatic(FirebaseFirestore::class)
        }
    }

    private fun emitAdminSnapshot(snapshot: DocumentSnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<DocumentSnapshot>>()
        every { adminDocument.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, error)
            registration
        }
    }

    private fun emitSnapshot(snapshot: DocumentSnapshot?, error: FirebaseFirestoreException?) {
        val listener = slot<EventListener<DocumentSnapshot>>()
        every { document.addSnapshotListener(capture(listener)) } answers {
            listener.captured.onEvent(snapshot, error)
            registration
        }
    }

    private fun memberSnapshot(
        approved: Boolean?,
        role: String? = null,
        permissions: List<Any>? = null,
        profile: Map<String, Any> = emptyMap(),
        roles: List<Any>? = null,
    ): DocumentSnapshot = mockk {
        every { exists() } returns true
        every { id } returns "uid-1"
        // Später definierte, konkretere Stubs haben in MockK Vorrang vor diesen Profil-Defaults.
        every { getString(any()) } answers { profile[firstArg()] as? String }
        every { getBoolean(any()) } answers { profile[firstArg()] as? Boolean }
        every { this@mockk.get("permissions") } returns permissions
        every { contains("roles") } returns (roles != null)
        every { this@mockk.get("roles") } returns roles
        every { this@mockk.get("emails") } returns profile["emails"]
        every { this@mockk.get("emergencyContacts") } returns profile["emergencyContacts"]
        every { this@mockk.get("talkNumbers") } returns profile["talkNumbers"]
        every { getString("email") } returns "user@example.com"
        every { getString("displayName") } returns "User"
        every { getString("role") } returns role
        every { getBoolean("approved") } returns approved
    }

    private val defaultProfileFields = mapOf(
        "lastName" to "",
        "firstName" to "",
        "category" to null,
        "baptized" to false,
        "appointment" to "NONE",
        "mobilePhone" to "",
        "landlinePhone" to "",
        "isSpeaker" to false,
        "talkNumbers" to emptyList<Int>(),
        "emails" to emptyList<String>(),
        "street" to "",
        "postalCode" to "",
        "city" to "",
        "emergencyContacts" to emptyList<Map<String, Any>>(),
    )
}
