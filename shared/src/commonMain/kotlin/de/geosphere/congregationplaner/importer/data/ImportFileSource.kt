package de.geosphere.congregationplaner.importer.data

import de.geosphere.congregationplaner.importer.domain.model.ImportTable

/** Lets the user pick a spreadsheet and reads it. Only the desktop app can; elsewhere [isAvailable] is `false`. */
interface ImportFileSource {
    val isAvailable: Boolean

    /** Asks for an `.xlsx` file and parses its first sheet; `null` if the user cancelled. */
    suspend fun pickTable(title: String): Result<ImportTable?>
}

/** Platform choice of the file source, like the repository factories (the import is a desktop feature). */
expect fun createImportFileSource(): ImportFileSource

internal object UnavailableImportFileSource : ImportFileSource {
    override val isAvailable: Boolean = false

    override suspend fun pickTable(title: String): Result<ImportTable?> =
        Result.failure(UnsupportedOperationException("Der Import ist nur in der Desktop-App verfügbar."))
}
