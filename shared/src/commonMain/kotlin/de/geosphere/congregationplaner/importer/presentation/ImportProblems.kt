package de.geosphere.congregationplaner.importer.presentation

private const val MAX_SHOWN_PROBLEMS = 20

/** Die ersten Probleme eines Imports; mehr passen nicht sinnvoll auf den Bildschirm. */
internal fun <T> List<T>.shownProblems(): List<T> = take(MAX_SHOWN_PROBLEMS)
