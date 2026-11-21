package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.domain.model.ImportRow
import de.geosphere.congregationplaner.importer.domain.model.ImportTable
import kotlin.test.Test
import kotlin.test.assertEquals

/** On Android, the shared [ZipArchive] round-trips a workbook written by [Xlsx]. */
class AndroidZipArchiveTest {
    @Test
    fun writtenWorkbookIsReadBackUnchanged() {
        val table = ImportTable(listOf("Name", "Nummer"), listOf(ImportRow(2, mapOf("Name" to "Süd", "Nummer" to "3"))))

        assertEquals(table, Xlsx.readFirstSheet(ZipArchive.unzip(ZipArchive.zip(Xlsx.write(table)))))
    }
}
