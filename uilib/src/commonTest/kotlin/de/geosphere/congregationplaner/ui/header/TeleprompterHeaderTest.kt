package de.geosphere.congregationplaner.ui.header

import kotlin.test.Test
import kotlin.test.assertEquals

class TeleprompterHeaderTest {

    @Test
    fun `no previous lines need no fading`() {
        assertEquals(emptyList(), fadeAlphas(0))
    }

    @Test
    fun `the latest previous line is the most visible`() {
        assertEquals(listOf(0.55f), fadeAlphas(1))
        assertEquals(listOf(0.3f, 0.55f), fadeAlphas(2))
    }
}
