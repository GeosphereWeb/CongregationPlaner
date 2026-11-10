package de.geosphere.congregationplaner.importer.domain.model

/** The result of writing a plan: what was written, what was left alone and what failed (with the reason). */
data class ImportReport(
    val created: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val failures: List<String> = emptyList(),
) {
    val description: String
        get() = "$created angelegt, $updated geändert, $skipped unverändert, ${failures.size} Fehler"

    operator fun plus(other: ImportReport) = ImportReport(
        created + other.created,
        updated + other.updated,
        skipped + other.skipped,
        failures + other.failures,
    )
}
