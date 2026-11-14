package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.Gender
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemberRowTextTest {
    @Test
    fun `the login email is shown only for members with an account`() {
        assertEquals("Anmeldung: max@example.com", memberLoginEmail(AppUser(uid = "1", email = "max@example.com")))
        assertNull(memberLoginEmail(AppUser(uid = "2", email = " ")))
        assertNull(memberLoginEmail(AppUser(uid = "3")))
        assertNull(memberLoginEmail(AppUser(uid = "4", email = "kind@example.com", hasAccount = false)))
    }
    private val brother = MemberProfile(lastName = "Muster", firstName = "Max", gender = Gender.MALE)

    @Test
    fun `the subtitle shows the brother and his roles`() {
        val member = AppUser(
            uid = "1",
            approved = true,
            roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN),
            profile = brother.copy(baptized = true),
        )

        assertEquals("Bruder · Versammlungsadmin · Vortragsplaner", memberSubtitle(member))
    }

    @Test
    fun `a pending user waits for approval and gets the badge`() {
        val pending = AppUser(uid = "1", email = "neu@beispiel.de")

        assertEquals("Wartet auf Freigabe", memberSubtitle(pending))
        assertEquals("offen", memberBadge(pending))
    }

    @Test
    fun `an unbaptized person without account shows the gender and is never pending`() {
        val person = AppUser(
            uid = "1",
            hasAccount = false,
            profile = MemberProfile(lastName = "Muster", firstName = "Lena", gender = Gender.FEMALE),
        )

        assertEquals("Weiblich · Ohne Konto", memberSubtitle(person))
        assertNull(memberBadge(person))
    }

    @Test
    fun `an approved member without details is a member`() {
        val member = AppUser(uid = "1", approved = true)

        assertEquals("Mitglied", memberSubtitle(member))
        assertNull(memberBadge(member))
    }

    @Test
    fun `an approved sister without role is shown as sister`() {
        val sister = AppUser(
            uid = "1",
            approved = true,
            profile = MemberProfile(gender = Gender.FEMALE, baptized = true),
        )

        assertEquals("Schwester", memberSubtitle(sister))
        // Without a gender only the baptism is known.
        assertEquals("getauft", memberSubtitle(sister.copy(profile = MemberProfile(baptized = true))))
    }

    @Test
    fun `the own member is marked`() {
        val member = AppUser(uid = "1", approved = true, profile = brother)

        assertEquals("Max Muster (du)", memberTitle(member, isCurrentUser = true))
        assertEquals("Max Muster", memberTitle(member, isCurrentUser = false))
    }
}
