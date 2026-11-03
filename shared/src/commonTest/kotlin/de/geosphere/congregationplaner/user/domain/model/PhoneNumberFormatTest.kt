package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneNumberFormatTest {
    @Test
    fun `mobile numbers are split after the four digit prefix`() {
        assertEquals("0170 1234567", formatPhoneNumber("01701234567"))
        assertEquals("0151 12345678", formatPhoneNumber("015112345678"))
        assertEquals("0160 9876543", formatPhoneNumber(" 01609876543 "))
    }

    @Test
    fun `the big cities are split after the three digit area code`() {
        assertEquals("030 123456", formatPhoneNumber("030123456"))
        assertEquals("089 1234567", formatPhoneNumber("0891234567"))
    }

    @Test
    fun `international numbers with plus 49 keep the country code`() {
        assertEquals("+49 170 1234567", formatPhoneNumber("+491701234567"))
        assertEquals("+49 30 123456", formatPhoneNumber("+4930123456"))
    }

    @Test
    fun `numbers without a safely known prefix stay as typed`() {
        assertEquals("0201123456", formatPhoneNumber("0201123456"))
        assertEquals("+431234567", formatPhoneNumber("+431234567"))
        assertEquals("1234567", formatPhoneNumber("1234567"))
    }

    @Test
    fun `separators set by the user are kept`() {
        assertEquals("0201 123456", formatPhoneNumber("0201 123456"))
        assertEquals("0170/1234567", formatPhoneNumber("0170/1234567"))
    }

    @Test
    fun `short or empty input and too long results stay unchanged`() {
        assertEquals("", formatPhoneNumber(""))
        assertEquals("0170", formatPhoneNumber("0170"))
        val maxLength = "0170" + "1".repeat(MemberProfile.MAX_PHONE_LENGTH - 4)
        assertEquals(maxLength, formatPhoneNumber(maxLength))
    }
}
