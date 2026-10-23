package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberProfileTest {
    @Test
    fun `only baptized brothers can have an appointment`() {
        assertTrue(MemberProfile(category = MemberCategory.BROTHER, baptized = true).canHaveAppointment)
        assertFalse(MemberProfile(category = MemberCategory.BROTHER, baptized = false).canHaveAppointment)
        assertFalse(MemberProfile(category = MemberCategory.SISTER, baptized = true).canHaveAppointment)
        assertFalse(MemberProfile(category = null, baptized = true).canHaveAppointment)
    }

    @Test
    fun `normalized drops baptism of children and appointments without prerequisites`() {
        val elder = MemberProfile(category = MemberCategory.BROTHER, baptized = true, appointment = Appointment.ELDER)

        assertEquals(elder, elder.normalized())
        assertEquals(
            MemberProfile(category = MemberCategory.CHILD, baptized = false, appointment = Appointment.NONE),
            elder.copy(category = MemberCategory.CHILD).normalized(),
        )
        assertEquals(
            MemberProfile(category = MemberCategory.SISTER, baptized = true, appointment = Appointment.NONE),
            elder.copy(category = MemberCategory.SISTER).normalized(),
        )
        assertEquals(Appointment.NONE, elder.copy(baptized = false).normalized().appointment)
    }

    @Test
    fun `label prefers the full name over display name or email or uid`() {
        val user = AppUser(uid = "uid-1", email = "max@example.com", displayName = "Maxi")

        assertEquals("Max Muster", user.copy(profile = MemberProfile(lastName = "Muster", firstName = "Max")).label)
        assertEquals("Muster", user.copy(profile = MemberProfile(lastName = "Muster")).label)
        assertEquals("Maxi", user.label)
        assertEquals("max@example.com", user.copy(displayName = " ").label)
        assertEquals("uid-1", AppUser(uid = "uid-1").label)
    }

    @Test
    fun `phone numbers allow digits and common separators only`() {
        assertTrue(MemberProfile.isValidPhone(""))
        assertTrue(MemberProfile.isValidPhone("+49 (030) 123/456-78"))
        assertFalse(MemberProfile.isValidPhone("0170 abc"))
        assertFalse(MemberProfile.isValidPhone("1".repeat(MemberProfile.MAX_PHONE_LENGTH + 1)))
    }

    @Test
    fun `unknown category and appointment values fall back to safe defaults`() {
        assertEquals(MemberCategory.SISTER, MemberCategory.fromValue("SISTER"))
        assertNull(MemberCategory.fromValue("UNCLE"))
        assertNull(MemberCategory.fromValue(null))
        assertEquals(Appointment.ELDER, Appointment.fromValue("ELDER"))
        assertEquals(Appointment.NONE, Appointment.fromValue("PIONEER"))
        assertEquals(Appointment.NONE, Appointment.fromValue(null))
    }
}
