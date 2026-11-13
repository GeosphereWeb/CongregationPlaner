package de.geosphere.congregationplaner.organization.presentation.selection

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VerificationHintTest {
    @Test
    fun `the hint names the recipient and the newest link`() {
        val hint = verificationHint("Musterstadt", " max@example.com ")

        assertTrue("E-Mail an max@example.com geschickt" in hint)
        assertTrue("„Musterstadt“" in hint)
        assertTrue("neuesten Mail" in hint)
    }

    @Test
    fun `without an address the hint leaves the recipient out`() {
        assertFalse(" an " in verificationHint("Musterstadt", null).substringBefore("geschickt"))
        assertFalse(" an " in verificationHint("Musterstadt", "  ").substringBefore("geschickt"))
    }
}
