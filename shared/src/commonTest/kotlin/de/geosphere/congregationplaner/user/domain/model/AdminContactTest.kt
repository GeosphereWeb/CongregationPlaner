package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AdminContactTest {
    private val admin = AppUser(
        uid = "a1",
        approved = true,
        roles = setOf(CongregationRole.CONGREGATION_ADMIN),
        profile = MemberProfile(firstName = "Anna", lastName = "Admin"),
    )

    @Test
    fun `approved congregation admin has a contact with the profile name`() {
        assertEquals(AdminContact("a1", "Anna", "Admin"), admin.adminContact())
    }

    @Test
    fun `unapproved admin and plain members have no contact`() {
        assertNull(admin.copy(approved = false).adminContact())
        assertNull(admin.copy(roles = emptySet()).adminContact())
        assertNull(admin.copy(roles = setOf(CongregationRole.TALK_PLANNER)).adminContact())
    }

    @Test
    fun `full name skips missing parts`() {
        assertEquals("Anna Admin", AdminContact("a", "Anna", "Admin").fullName)
        assertEquals("Admin", AdminContact("a", "", "Admin").fullName)
        assertEquals("", AdminContact("a", "", "").fullName)
    }
}
