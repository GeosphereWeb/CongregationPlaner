package de.geosphere.congregationplaner.importer

/**
 * One row of an imported sheet. [number] is the 1-based row number in the file (the header is row 1), so error
 * messages can point at the line the user sees in Excel.
 */
data class ImportRow(val number: Int, private val cells: Map<String, String>) {
    /** The trimmed text of the column [header], or `null` if the column is missing or blank. */
    fun text(header: String): String? = cells[header]?.trim()?.takeIf { it.isNotEmpty() }
}

/** The first sheet of an imported file: header names (blank headers dropped) and the data rows below them. */
data class ImportTable(val headers: List<String>, val rows: List<ImportRow>)
