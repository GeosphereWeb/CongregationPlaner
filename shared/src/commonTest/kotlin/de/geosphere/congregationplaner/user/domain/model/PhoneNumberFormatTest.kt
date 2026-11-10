package de.geosphere.congregationplaner.user.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneNumberFormatTest {
    @Test
    fun `mobile numbers are split after the four digit prefix`() {
        assertEquals("0170 1234567", formatPhoneNumber("01701234567"))
        assertEquals("0151 12345678", formatPhoneNumber("015112345678"))
        assertEquals("0160 9876543", formatPhoneNumber(" 01609876543 "))
    }

    @Test
    fun `landline numbers are split after the area code of any length`() {
        assertEquals("030 123456", formatPhoneNumber("030123456"))
        assertEquals("0201 123456", formatPhoneNumber("0201123456"))
        assertEquals("0331 234567", formatPhoneNumber("0331234567"))
        assertEquals("03381 12345", formatPhoneNumber("0338112345"))
        assertEquals("039999 1234", formatPhoneNumber("0399991234"))
    }

    @Test
    fun `the longest matching area code wins`() {
        assertEquals("02129 12345", formatPhoneNumber("0212912345"))
        assertEquals("0212 123456", formatPhoneNumber("0212123456"))
    }

    @Test
    fun `international numbers with plus 49 keep the country code`() {
        assertEquals("+49 170 1234567", formatPhoneNumber("+491701234567"))
        assertEquals("+49 30 123456", formatPhoneNumber("+4930123456"))
        assertEquals("+49 201 123456", formatPhoneNumber("+49201123456"))
    }

    @Test
    fun `numbers without a known prefix stay as typed`() {
        assertEquals("08001234567", formatPhoneNumber("08001234567"))
        assertEquals("+431234567", formatPhoneNumber("+431234567"))
        assertEquals("1234567", formatPhoneNumber("1234567"))
    }

    @Test
    fun `separators set by the user are kept`() {
        assertEquals("0201 12 34 56", formatPhoneNumber("0201 12 34 56"))
        assertEquals("0170/1234567", formatPhoneNumber("0170/1234567"))
    }

    @Test
    fun `short or empty input and too long results stay unchanged`() {
        assertEquals("", formatPhoneNumber(""))
        assertEquals("0170", formatPhoneNumber("0170"))
        assertEquals("030", formatPhoneNumber("030"))
        val maxLength = "0170" + "1".repeat(MemberProfile.MAX_PHONE_LENGTH - 4)
        assertEquals(maxLength, formatPhoneNumber(maxLength))
    }

    @Test
    fun `mobile and landline numbers are recognised with or without separators`() {
        assertEquals(PhoneKind.MOBILE, phoneKind("0170 1234567"))
        assertEquals(PhoneKind.MOBILE, phoneKind("+49 151 12345678"))
        assertEquals(PhoneKind.LANDLINE, phoneKind("0201123456"))
        assertEquals(PhoneKind.LANDLINE, phoneKind("+49 (30) 123-456"))
        assertEquals(PhoneKind.UNKNOWN, phoneKind("0800 1234567"))
        assertEquals(PhoneKind.UNKNOWN, phoneKind("+43 1 234567"))
        assertEquals(PhoneKind.UNKNOWN, phoneKind(""))
    }

    @Test
    fun `the area name comes from the longest matching area code of a landline number`() {
        assertEquals("Essen", areaName("0201 123456"))
        assertEquals("Berlin", areaName("+4930123456"))
        assertEquals("Haan Rheinl", areaName("0212912345"))
        assertEquals("Solingen", areaName("0212123456"))
        assertNull(areaName("0170 1234567"))
        assertNull(areaName("0800 1234567"))
    }

    @Test
    fun `the area code list holds all active codes of the federal network agency`() {
        assertEquals(5200, GERMAN_AREA_CODES.size)
        assertTrue(listOf("30", "201", "212", "2129", "331", "3381", "39999").all { it in GERMAN_AREA_CODES })
        assertTrue("33052" !in GERMAN_AREA_CODES, "inaktive Kennzahl Leegebruch darf nicht enthalten sein")
    }

    @Test
    fun `an international number has a plus and no separators`() {
        assertEquals("+491701234567", internationalNumber("0170 1234567"))
        assertEquals("+491701234567", internationalNumber("+49 170 1234567"))
        assertEquals("+491701234567", internationalNumber("0049 170 1234567"))
        assertEquals("+436601234567", internationalNumber("+43 (660) 1234567"))
        assertEquals("+4989123456", internationalNumber("089/123456"))
    }

    @Test
    fun `a number without a prefix or too short has no international form`() {
        assertNull(internationalNumber("1234567"))
        assertNull(internationalNumber("0123"))
        assertNull(internationalNumber(""))
        assertNull(internationalNumber("abc"))
    }
}
