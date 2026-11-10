package de.geosphere.congregationplaner.importer.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportTableTest {
    @Test
    fun textIsTrimmed() {
        assertEquals("Max", ImportRow(2, mapOf("Name" to "  Max ")).text("Name"))
    }

    @Test
    fun blankAndMissingCellsAreNull() {
        val row = ImportRow(2, mapOf("Name" to "   "))

        assertNull(row.text("Name"))
        assertNull(row.text("Unknown"))
    }

    @Test
    fun rowKeepsItsSheetRowNumberForErrorMessages() {
        assertEquals(7, ImportRow(7, emptyMap()).number)
    }
}
