package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.domain.model.ImportTable

/**
 * Lets the user pick a spreadsheet to read and choose where to save one. The Android and desktop apps can; elsewhere
 * [isAvailable] is `false`.
 */
interface ImportFileSource {
    val isAvailable: Boolean

    /** Asks for an `.xlsx` file and parses its first sheet; `null` if the user cancelled. */
    suspend fun pickTable(title: String): Result<ImportTable?>

    /** Asks where to save [table] as an `.xlsx` file, proposing [fileName]; `false` if the user cancelled. */
    suspend fun saveTable(fileName: String, table: ImportTable): Result<Boolean>
}

/** Platform choice of the file source, like the repository factories. */
expect fun createImportFileSource(): ImportFileSource

internal object UnavailableImportFileSource : ImportFileSource {
    override val isAvailable: Boolean = false

    override suspend fun pickTable(title: String): Result<ImportTable?> = unavailable()

    override suspend fun saveTable(fileName: String, table: ImportTable): Result<Boolean> = unavailable()

    private fun <T> unavailable(): Result<T> =
        Result.failure(UnsupportedOperationException("Excel-Dateien gibt es nur in der Android- und Desktop-App."))
}
