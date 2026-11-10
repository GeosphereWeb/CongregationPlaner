package de.geosphere.congregationplaner.importer.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportProblemsTest {
    @Test
    fun `at most twenty problems are shown`() {
        assertEquals(20, List(25) { it }.shownProblems().size)
        assertEquals(listOf(1, 2), listOf(1, 2).shownProblems())
    }
}
