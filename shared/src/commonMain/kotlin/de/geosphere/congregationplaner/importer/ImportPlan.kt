package de.geosphere.congregationplaner.importer

enum class ImportKind { CREATE, UPDATE, SKIP }

/** What an import run will do with one [item]: create it, update an existing one, or leave it untouched. */
data class PlanEntry<T>(val item: T, val kind: ImportKind)

/** A source row that cannot be imported; [row] is the 1-based row number in the file. */
data class ImportError(val row: Int, val message: String)

/** The preview of an import step: nothing is written before the user confirms it. */
data class ImportPlan<T>(val entries: List<PlanEntry<T>>, val errors: List<ImportError>) {
    fun count(kind: ImportKind): Int = entries.count { it.kind == kind }
}

internal fun <T> kindFor(current: T?, wanted: T): ImportKind = when (current) {
    null -> ImportKind.CREATE
    wanted -> ImportKind.SKIP
    else -> ImportKind.UPDATE
}
