package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalCongregation
import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory

/** The filter of the speaker list; `null` means "all". An ID that no longer exists is ignored. */
data class DirectoryFilter(val circuitId: String? = null, val congregationId: String? = null) {
    val isActive: Boolean get() = circuitId != null || congregationId != null
}

/** One entry of a filter menu; [id] `null` is the "all" entry. */
data class FilterOption(val id: String?, val label: String)

/** The directory reduced to the chosen circuit and/or congregation, with its circuits, congregations and speakers. */
internal fun ExternalDirectory.filtered(filter: DirectoryFilter): ExternalDirectory {
    val congregation = congregations.firstOrNull { it.id == filter.congregationId }
    val circuitId = circuits.firstOrNull { it.id == filter.circuitId }?.id ?: congregation?.circuitId
    val shownCongregations = congregations.filter {
        (circuitId == null || it.circuitId == circuitId) && (congregation == null || it.id == congregation.id)
    }
    val shownIds = shownCongregations.map { it.id }.toSet()
    return ExternalDirectory(
        circuits = circuits.filter { circuitId == null || it.id == circuitId },
        congregations = shownCongregations,
        speakers = speakers.filter { it.congregationId in shownIds },
    )
}

/** "Alle Kreise" plus every circuit. */
internal fun ExternalDirectory.circuitOptions(): List<FilterOption> =
    listOf(FilterOption(null, "Alle Kreise")) + circuits.map { FilterOption(it.id, it.name) }

/**
 * "Alle Versammlungen" plus the congregations of the chosen circuit, or all congregations if no circuit is chosen. In
 * that case the label names the circuit, because congregation names can occur in several circuits.
 */
internal fun ExternalDirectory.congregationOptions(circuitId: String?): List<FilterOption> {
    val circuitKnown = circuits.any { it.id == circuitId }
    val shown: List<ExternalCongregation> =
        if (circuitKnown) congregations.filter { it.circuitId == circuitId } else congregations
    val circuitNames = circuits.associate { it.id to it.name }
    return listOf(FilterOption(null, "Alle Versammlungen")) + shown.map {
        FilterOption(it.id, if (circuitKnown) it.name else "${it.name} (${circuitNames[it.circuitId].orEmpty()})")
    }
}

/** The filter after choosing a circuit: a chosen congregation of another circuit is dropped. */
internal fun DirectoryFilter.withCircuit(directory: ExternalDirectory, circuitId: String?): DirectoryFilter {
    val congregation = directory.congregations.firstOrNull { it.id == congregationId }
    val keep = circuitId == null || congregation?.circuitId == circuitId
    return DirectoryFilter(circuitId, congregationId.takeIf { keep })
}
