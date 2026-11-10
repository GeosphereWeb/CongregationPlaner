package de.geosphere.congregationplaner.user.presentation.management

import de.geosphere.congregationplaner.user.domain.model.AppUser
import de.geosphere.congregationplaner.user.domain.model.CongregationRole
import de.geosphere.congregationplaner.user.domain.model.MemberCategory
import de.geosphere.congregationplaner.user.domain.model.MemberProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MemberRowTextTest {
    private val brother = MemberProfile(lastName = "Muster", firstName = "Max", category = MemberCategory.BROTHER)

    @Test
    fun `the subtitle shows category baptism and roles`() {
        val member = AppUser(
            uid = "1",
            approved = true,
            roles = setOf(CongregationRole.TALK_PLANNER, CongregationRole.CONGREGATION_ADMIN),
            profile = brother.copy(baptized = true),
        )

        assertEquals("Bruder · getauft · Versammlungsadmin · Vortragsplaner", memberSubtitle(member))
    }

    @Test
    fun `a pending user waits for approval and gets the badge`() {
        val pending = AppUser(uid = "1", email = "neu@beispiel.de")

        assertEquals("Wartet auf Freigabe", memberSubtitle(pending))
        assertEquals("offen", memberBadge(pending))
    }

    @Test
    fun `a child without account is shown as such and never pending`() {
        val child = AppUser(
            uid = "1",
            hasAccount = false,
            profile = MemberProfile(lastName = "Muster", firstName = "Lena", category = MemberCategory.CHILD),
        )

        assertEquals("Kind · Ohne Konto", memberSubtitle(child))
        assertNull(memberBadge(child))
    }

    @Test
    fun `an approved member without details is a member`() {
        val member = AppUser(uid = "1", approved = true)

        assertEquals("Mitglied", memberSubtitle(member))
        assertNull(memberBadge(member))
    }

    @Test
    fun `an approved sister without role shows only category and baptism`() {
        val sister = AppUser(
            uid = "1",
            approved = true,
            profile = MemberProfile(category = MemberCategory.SISTER, baptized = true),
        )

        assertEquals("Schwester · getauft", memberSubtitle(sister))
    }

    @Test
    fun `the own member is marked`() {
        val member = AppUser(uid = "1", approved = true, profile = brother)

        assertEquals("Max Muster (du)", memberTitle(member, isCurrentUser = true))
        assertEquals("Max Muster", memberTitle(member, isCurrentUser = false))
    }
}
