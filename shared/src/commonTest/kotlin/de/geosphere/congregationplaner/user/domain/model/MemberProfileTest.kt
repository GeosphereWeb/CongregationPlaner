package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MemberProfileTest {
    private val elder = MemberProfile(gender = Gender.MALE, baptized = true, appointment = Appointment.ELDER)

    @Test
    fun `only baptized brothers can have an appointment`() {
        assertTrue(MemberProfile(gender = Gender.MALE, baptized = true).canHaveAppointment)
        assertFalse(MemberProfile(gender = Gender.MALE, baptized = false).canHaveAppointment)
        assertFalse(MemberProfile(gender = Gender.FEMALE, baptized = true).canHaveAppointment)
        assertFalse(MemberProfile(gender = null, baptized = true).canHaveAppointment)
    }

    @Test
    fun `brother and sister follow from gender and baptism`() {
        assertTrue(MemberProfile(gender = Gender.MALE, baptized = true).isBrother)
        assertFalse(MemberProfile(gender = Gender.MALE).isBrother)
        assertFalse(MemberProfile(gender = Gender.FEMALE, baptized = true).isBrother)
        assertTrue(MemberProfile(gender = Gender.FEMALE, baptized = true).isSister)
        assertFalse(MemberProfile(gender = Gender.FEMALE).isSister)
        assertFalse(MemberProfile(baptized = true).isSister)
    }

    @Test
    fun `only elders and ministerial servants can be speakers and normalizing drops the flag for everyone else`() {
        val speaker = elder.copy(isSpeaker = true)

        assertFalse(MemberProfile().isSpeaker)
        assertTrue(elder.canBeSpeaker)
        assertTrue(elder.copy(appointment = Appointment.MINISTERIAL_SERVANT).canBeSpeaker)
        assertFalse(elder.copy(appointment = Appointment.NONE).canBeSpeaker)
        assertFalse(elder.copy(gender = Gender.FEMALE).canBeSpeaker)
        assertFalse(elder.copy(baptized = false).canBeSpeaker)

        assertTrue(speaker.normalized().isSpeaker)
        assertFalse(speaker.copy(appointment = Appointment.NONE).normalized().isSpeaker)
        assertFalse(speaker.copy(gender = Gender.FEMALE).normalized().isSpeaker)
        assertFalse(speaker.copy(gender = null).normalized().isSpeaker)
        assertFalse(speaker.copy(baptized = false).normalized().isSpeaker)
    }

    @Test
    fun `normalizing sorts the talks of a speaker and drops them for everyone else`() {
        val speaker = elder.copy(isSpeaker = true, talkNumbers = listOf(45, 12, 45))

        assertEquals(listOf(12, 45), speaker.normalized().talkNumbers)
        assertTrue(speaker.copy(isSpeaker = false).normalized().talkNumbers.isEmpty())
        assertTrue(speaker.copy(gender = Gender.FEMALE).normalized().talkNumbers.isEmpty())
    }

    @Test
    fun `only baptized brothers with an appointment can be chairmen`() {
        assertTrue(elder.canBeChairman)
        assertTrue(elder.copy(appointment = Appointment.MINISTERIAL_SERVANT).canBeChairman)
        assertFalse(elder.copy(appointment = Appointment.NONE).canBeChairman)
        assertFalse(elder.copy(gender = Gender.FEMALE).canBeChairman)
        assertFalse(elder.copy(baptized = false).canBeChairman)
    }

    @Test
    fun `normalized drops appointments without prerequisites and keeps the baptism`() {
        assertEquals(elder, elder.normalized())
        assertEquals(
            MemberProfile(gender = Gender.FEMALE, baptized = true, appointment = Appointment.NONE),
            elder.copy(gender = Gender.FEMALE).normalized(),
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
    fun `unknown gender and appointment values fall back to safe defaults`() {
        assertEquals(Gender.FEMALE, Gender.fromValue("FEMALE"))
        assertNull(Gender.fromValue("SISTER"))
        assertNull(Gender.fromValue(null))
        assertEquals(Appointment.ELDER, Appointment.fromValue("ELDER"))
        assertEquals(Appointment.NONE, Appointment.fromValue("PIONEER"))
        assertEquals(Appointment.NONE, Appointment.fromValue(null))
    }
}
