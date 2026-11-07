package de.geosphere.congregationplaner.talk.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalDirectoryLabelsTest {
    @Test
    fun `the congregation count uses the singular for one`() {
        assertEquals("0 Versammlungen", congregationCountLabel(0))
        assertEquals("1 Versammlung", congregationCountLabel(1))
        assertEquals("2 Versammlungen", congregationCountLabel(2))
    }
}
