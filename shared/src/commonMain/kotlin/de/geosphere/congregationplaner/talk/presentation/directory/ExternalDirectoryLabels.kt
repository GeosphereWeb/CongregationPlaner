package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory
import de.geosphere.congregationplaner.talk.domain.model.VisitingSpeaker

internal fun ExternalDirectory.congregationsIn(circuitId: String): List<ExternalCongregation> =
    congregations.filter { it.circuitId == circuitId }

internal fun ExternalDirectory.speakersIn(congregationId: String): List<VisitingSpeaker> =
    speakers.filter { it.congregationId == congregationId }

internal fun congregationCountLabel(count: Int): String = if (count == 1) "1 Versammlung" else "$count Versammlungen"

internal fun speakerCountLabel(count: Int): String = "$count Redner"

internal fun VisitingSpeaker.displayName(): String =
    listOf(lastName, firstName).filter(String::isNotBlank).joinToString(", ")

internal fun ExternalDirectory.circuitName(circuitId: String): String =
    circuits.firstOrNull { it.id == circuitId }?.name.orEmpty()

/** Name der Versammlung mit ihrem Kreis, weil Namen in mehreren Kreisen vorkommen können. */
internal fun ExternalDirectory.congregationLabel(congregation: ExternalCongregation): String =
    "${congregation.name} (${circuitName(congregation.circuitId)})"

internal fun ExternalDirectory.congregationLabel(congregationId: String): String =
    congregations.firstOrNull { it.id == congregationId }?.let(::congregationLabel).orEmpty()

internal fun ExternalDirectory.circuitFilterSummary(filter: DirectoryFilter): String =
    filterSummary(circuitOptions(filter).filter { it.selected }.map { it.label }, "Alle Kreise", "Kreise")

internal fun ExternalDirectory.congregationFilterSummary(filter: DirectoryFilter): String = filterSummary(
    congregationOptions(filter, "").filter { it.selected }.map { it.label },
    "Alle Versammlungen",
    "Versammlungen",
)
