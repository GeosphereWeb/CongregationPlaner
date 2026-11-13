package de.geosphere.congregationplaner.user.data

import de.geosphere.congregationplaner.auth.domain.repository.FirebaseUser
import de.geosphere.congregationplaner.organization.domain.model.CongregationRef
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import de.geosphere.congregationplaner.user.domain.model.toNameProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class NameProfileTest {
    @Test
    fun `the display name is split at the last space`() {
        assertEquals(MemberProfile(firstName = "Max", lastName = "Muster"), "Max Muster".toNameProfile())
        assertEquals(MemberProfile(firstName = "Anna Maria", lastName = "Müller"), "Anna Maria Müller".toNameProfile())
        assertEquals(MemberProfile(firstName = "Max", lastName = "Muster"), "  Max   Muster ".toNameProfile())
    }

    @Test
    fun `a single word is the first name and a missing name stays empty`() {
        assertEquals(MemberProfile(firstName = "Madonna"), "Madonna".toNameProfile())
        assertEquals(MemberProfile(), null.toNameProfile())
        assertEquals(MemberProfile(), "   ".toNameProfile())
    }

    @Test
    fun `names are cut to the maximum length`() {
        val long = "x".repeat(MemberProfile.MAX_NAME_LENGTH + 20)

        val profile = "$long $long".toNameProfile()

        assertEquals(MemberProfile.MAX_NAME_LENGTH, profile.firstName.length)
        assertEquals(MemberProfile.MAX_NAME_LENGTH, profile.lastName.length)
    }

    @Test
    fun `a new member takes the names from the auth display name`() {
        val authUser = object : FirebaseUser {
            override val uid = "uid-1"
            override val email = "max@example.com"
            override val displayName = "Max Muster"
            override val idToken: String? = null
        }
        val congregation = CongregationRef("r", "s", "b", "de", "c", "k")

        val user = authUser.toNewAppUser(congregation)

        assertEquals("Max", user.profile.firstName)
        assertEquals("Muster", user.profile.lastName)
        assertEquals(false, user.approved)
    }
}
