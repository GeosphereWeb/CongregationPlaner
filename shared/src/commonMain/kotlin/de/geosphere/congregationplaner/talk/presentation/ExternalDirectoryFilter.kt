package de.geosphere.congregationplaner.talk.presentation

import de.geosphere.congregationplaner.talk.domain.model.ExternalDirectory

/** The name search of the congregation filter starts with this many letters. */
internal const val MIN_SEARCH_LENGTH = 2

/**
 * The filter of the speaker list. An empty set means "all"; several IDs show everything that belongs to any of them.
 * IDs that no longer exist are ignored.
 */
data class DirectoryFilter(val circuitIds: Set<String> = emptySet(), val congregationIds: Set<String> = emptySet()) {
    val isActive: Boolean get() = circuitIds.isNotEmpty() || congregationIds.isNotEmpty()
}

/** One entry of a filter menu. */
data class FilterOption(val id: String, val label: String, val selected: Boolean = false)

/** The directory reduced to the chosen circuits and congregations, with their circuits, congregations and speakers. */
internal fun ExternalDirectory.filtered(filter: DirectoryFilter): ExternalDirectory {
    val chosenCircuits = circuits.map { it.id }.filter { it in filter.circuitIds }.toSet()
    val chosenCongregations = congregations.filter { it.id in filter.congregationIds }
    val shownCongregations = congregations.filter {
        (chosenCircuits.isEmpty() || it.circuitId in chosenCircuits) &&
            (chosenCongregations.isEmpty() || chosenCongregations.any { chosen -> chosen.id == it.id })
    }
    val shownCircuitIds = when {
        chosenCircuits.isNotEmpty() -> chosenCircuits
        chosenCongregations.isNotEmpty() -> chosenCongregations.map { it.circuitId }.toSet()
        else -> circuits.map { it.id }.toSet()
    }
    val shownIds = shownCongregations.map { it.id }.toSet()
    return ExternalDirectory(
        circuits = circuits.filter { it.id in shownCircuitIds },
        congregations = shownCongregations,
        speakers = speakers.filter { it.congregationId in shownIds },
    )
}

/** Every circuit, marked if chosen. */
internal fun ExternalDirectory.circuitOptions(filter: DirectoryFilter): List<FilterOption> =
    circuits.map { FilterOption(it.id, it.name, it.id in filter.circuitIds) }

/**
 * The congregations to choose from: those of the chosen circuits, or all if no circuit is chosen (then the label names
 * the circuit, because congregation names can occur in several circuits). A [query] of at least [MIN_SEARCH_LENGTH]
 * letters narrows them to names containing it; chosen congregations stay in the list so they can be unchecked.
 */
internal fun ExternalDirectory.congregationOptions(filter: DirectoryFilter, query: String): List<FilterOption> {
    val circuitKnown = circuits.any { it.id in filter.circuitIds }
    val circuitNames = circuits.associate { it.id to it.name }
    val search = query.trim().takeIf { it.length >= MIN_SEARCH_LENGTH }
    return congregations
        .filter { !circuitKnown || it.circuitId in filter.circuitIds }
        .filter { search == null || it.name.contains(search, ignoreCase = true) || it.id in filter.congregationIds }
        .map {
            val label = if (circuitKnown) it.name else "${it.name} (${circuitNames[it.circuitId].orEmpty()})"
            FilterOption(it.id, label, it.id in filter.congregationIds)
        }
}

/** Text of a filter button: "Alle …", the single choice, or the number of choices. */
internal fun filterSummary(selectedLabels: List<String>, allLabel: String, pluralLabel: String): String =
    when (selectedLabels.size) {
        0 -> allLabel
        1 -> selectedLabels.single()
        else -> "${selectedLabels.size} $pluralLabel gewählt"
    }

/** The filter after (un)checking a circuit: chosen congregations that belong to none of the chosen circuits go. */
internal fun DirectoryFilter.toggleCircuit(directory: ExternalDirectory, circuitId: String): DirectoryFilter {
    val circuits = circuitIds.toggled(circuitId)
    val kept = congregationIds.filter { id ->
        circuits.isEmpty() || directory.congregations.firstOrNull { it.id == id }?.circuitId in circuits
    }
    return DirectoryFilter(circuits, kept.toSet())
}

internal fun DirectoryFilter.toggleCongregation(congregationId: String) =
    copy(congregationIds = congregationIds.toggled(congregationId))

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id
