package de.geosphere.congregationplaner.talk.presentation.directory

import de.geosphere.congregationplaner.presentation.state.UiStateStore

/** Grenzt die Liste auf Kreise und Versammlungen ein; jede Auswahl gilt sofort. */
class DirectoryFilterHolder(private val store: UiStateStore<ExternalDirectoryUiState>) {
    /** Wählt einen Kreis an oder ab; Versammlungen anderer Kreise entfallen. */
    fun toggleCircuit(circuitId: String) =
        store.update { it.copy(filter = it.filter.toggleCircuit(it.directory, circuitId)) }

    fun toggleCongregation(congregationId: String) =
        store.update { it.copy(filter = it.filter.toggleCongregation(congregationId)) }

    fun clearCircuits() = store.update { it.copy(filter = it.filter.copy(circuitIds = emptySet())) }

    fun clearCongregations() =
        store.update { it.copy(filter = it.filter.copy(congregationIds = emptySet()), congregationQuery = "") }

    /** Die Suche nach dem Namen einer Versammlung; sie grenzt die Auswahl ab dem zweiten Buchstaben ein. */
    fun searchCongregations(query: String) = store.update { it.copy(congregationQuery = query) }
}
